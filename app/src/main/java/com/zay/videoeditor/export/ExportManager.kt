package com.zay.videoeditor.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import com.zay.videoeditor.data.AudioClip
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.fx.ClipFx
import com.zay.videoeditor.ui.screens.editor.toMediaItem
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Pilihan pengguna dari layar Ekspor. */
data class ExportConfig(
    val videoBitrate: Int,
    val saveToGallery: Boolean,
)

sealed interface ExportState {
    object Idle : ExportState

    /** [progress] 0..1. [stage] teks singkat untuk pengguna. */
    data class Running(val progress: Float, val stage: String) : ExportState

    /** [uri] bisa dibuka/dibagikan. [inGallery] true bila tersimpan di Galeri (Movies/ZAY Video Editor). */
    data class Done(val uri: Uri, val inGallery: Boolean, val sizeMb: Float, val note: String) : ExportState

    data class Failed(val message: String) : ExportState
}

/**
 * Mengekspor proyek (EditorSession) menjadi MP4 H.264 + AAC.
 * - Video: semua klip di timeline berurutan, dipotong sesuai trim, diskalakan ke rasio dan resolusi pilihan
 *   (mode Pas = letterbox, mode Isi = crop) lewat efek Presentation.
 * - Audio: suara asli klip + semua musik/rekaman suara (volume dan posisi mulai dipertahankan), dicampur Transformer.
 * Status disimpan global supaya tetap ada walau layar Ekspor ditutup atau dibuat ulang.
 */
@androidx.annotation.OptIn(UnstableApi::class)
object ExportManager {

    var state by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val progressHolder = ProgressHolder()

    private var transformer: Transformer? = null
    private var outputFile: File? = null
    private var tempFiles: List<File> = emptyList()

    @Volatile
    private var cancelled = false

    private val poll = object : Runnable {
        override fun run() {
            val t = transformer ?: return
            if (state !is ExportState.Running) return
            val s = t.getProgress(progressHolder)
            if (s == Transformer.PROGRESS_STATE_AVAILABLE) {
                state = ExportState.Running(progressHolder.progress / 100f, "Mengekspor video...")
            }
            mainHandler.postDelayed(this, 300)
        }
    }

    val isRunning: Boolean
        get() = state is ExportState.Running

    /** Kembali ke keadaan awal (setelah selesai atau gagal). Tidak berefek saat ekspor berjalan. */
    fun dismiss() {
        if (state !is ExportState.Running) state = ExportState.Idle
    }

    /** Hentikan ekspor yang sedang berjalan dan buang file setengah jadi. Panggil dari thread utama. */
    fun cancel() {
        cancelled = true
        mainHandler.removeCallbacks(poll)
        try {
            transformer?.cancel()
        } catch (e: Exception) {
            // abaikan
        }
        transformer = null
        outputFile?.delete()
        outputFile = null
        deleteTemps()
        state = ExportState.Idle
    }

    /** Mulai ekspor. Panggil dari thread utama. */
    fun start(context: Context, config: ExportConfig) {
        if (isRunning) return
        val app = context.applicationContext

        // Ambil salinan data proyek sekarang supaya aman walau pengguna mengubah proyek.
        val clips = EditorSession.clips.toList()
        val audios = EditorSession.audioClips.toList()
        val overlays = EditorSession.overlays.toList()
        val size = EditorSession.outputSize()
        val fill = EditorSession.fillFrame

        if (clips.isEmpty()) {
            state = ExportState.Failed("Belum ada video di timeline. Tambahkan video dulu di editor.")
            return
        }

        // Cek ruang penyimpanan (perkiraan kasar: bitrate video + audio x durasi; x2 bila disalin ke Galeri).
        val totalSec = clips.fold(0f) { acc, c -> acc + c.durationSec }
        val oneCopy = (config.videoBitrate.toLong() + 192_000L) * (totalSec + 1f).toLong() / 8L
        val needBytes = (if (config.saveToGallery) oneCopy * 2L else oneCopy) + 50L * 1024L * 1024L
        val freeBytes = try {
            android.os.StatFs(app.filesDir.absolutePath).availableBytes
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
        if (freeBytes < needBytes) {
            val needMb = needBytes / (1024L * 1024L)
            val freeMb = freeBytes / (1024L * 1024L)
            state = ExportState.Failed("Ruang penyimpanan kurang: dibutuhkan sekitar $needMb MB, tersedia $freeMb MB. Kosongkan ruang lalu coba lagi.")
            return
        }

        cancelled = false
        state = ExportState.Running(0f, "Menyiapkan...")

        worker.execute {
            val temps = mutableListOf<File>()
            try {
                val composition = buildComposition(app, clips, audios, overlays, size, fill, temps)
                mainHandler.post {
                    if (cancelled) {
                        temps.forEach { it.delete() }
                    } else {
                        launchTransformer(app, composition, config, temps)
                    }
                }
            } catch (e: Exception) {
                temps.forEach { it.delete() }
                mainHandler.post {
                    if (!cancelled) {
                        state = ExportState.Failed("Gagal menyiapkan ekspor: ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------

    private fun launchTransformer(app: Context, composition: Composition, config: ExportConfig, temps: List<File>) {
        tempFiles = temps
        val dir = File(app.filesDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "ZAY_$stamp.mp4")
        if (out.exists()) out.delete()
        outputFile = out

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                mainHandler.removeCallbacks(poll)
                transformer = null
                deleteTemps()
                state = ExportState.Running(1f, "Menyimpan hasil...")
                worker.execute { finish(app, out, config.saveToGallery) }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                mainHandler.removeCallbacks(poll)
                transformer = null
                deleteTemps()
                out.delete()
                outputFile = null
                val detail = exportException.message ?: "tidak diketahui"
                state = ExportState.Failed("Ekspor gagal (kode ${exportException.errorCode}): $detail")
            }
        }

        try {
            val encoderFactory = DefaultEncoderFactory.Builder(app)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(config.videoBitrate).build(),
                )
                .setEnableFallback(true)
                .build()
            val t = Transformer.Builder(app)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(listener)
                .build()
            transformer = t
            t.start(composition, out.absolutePath)
            mainHandler.postDelayed(poll, 300)
        } catch (e: Exception) {
            transformer = null
            deleteTemps()
            out.delete()
            outputFile = null
            state = ExportState.Failed("Tidak bisa memulai ekspor: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun deleteTemps() {
        tempFiles.forEach { it.delete() }
        tempFiles = emptyList()
    }

    /** Dipanggil di thread latar belakang setelah Transformer selesai. */
    private fun finish(app: Context, out: File, saveToGallery: Boolean) {
        if (!out.exists() || out.length() <= 0L) {
            state = ExportState.Failed("File hasil kosong. Coba ekspor lagi.")
            return
        }
        val sizeMb = out.length() / (1024f * 1024f)
        outputFile = null

        if (saveToGallery) {
            val galleryUri = copyToGallery(app, out)
            if (galleryUri != null) {
                out.delete()
                state = ExportState.Done(galleryUri, true, sizeMb, "Tersimpan di Galeri (folder Movies/ZAY Video Editor).")
                return
            }
        }
        val fileUri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", out)
        val note = if (saveToGallery) {
            "Tidak bisa menyalin ke Galeri, video disimpan di penyimpanan aplikasi. Gunakan Bagikan untuk mengirimnya."
        } else {
            "Video disimpan di penyimpanan aplikasi. Gunakan Bagikan atau Putar."
        }
        state = ExportState.Done(fileUri, false, sizeMb, note)
    }

    /** Salin ke MediaStore (Android 10+). Null bila gagal atau Android lebih lama. */
    private fun copyToGallery(app: Context, file: File): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = app.contentResolver
        var uri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ZAY Video Editor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            uri = resolver.insert(collection, values)
            if (uri == null) return null
            val stream = resolver.openOutputStream(uri)
            if (stream == null) {
                resolver.delete(uri, null, null)
                return null
            }
            stream.use { os -> file.inputStream().use { input -> input.copyTo(os) } }
            val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            return uri
        } catch (e: Exception) {
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null)
                } catch (e2: Exception) {
                    // abaikan
                }
            }
            return null
        }
    }

    // ------------------------------------------------------------------------------------------
    // Menyusun Composition

    private fun buildComposition(
        app: Context,
        clips: List<ImportedClip>,
        audios: List<AudioClip>,
        overlays: List<Overlay>,
        size: Pair<Int, Int>,
        fill: Boolean,
        temps: MutableList<File>,
    ): Composition {
        val width = size.first
        val height = size.second
        val layout = if (fill) Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP else Presentation.LAYOUT_SCALE_TO_FIT
        val total = clips.fold(0f) { acc, c -> acc + c.durationSec }

        // Suara asli klip ikut diekspor hanya bila SEMUA klip punya trek suara.
        val keepOriginalAudio = clips.all { hasAudioTrack(app, it.uri) }

        val videoItems = clips.mapIndexed { index, c ->
            val videoEffects = mutableListOf<Effect>(Presentation.createForWidthAndHeight(width, height, layout))
            videoEffects.addAll(ClipFx.buildEffects(clips, index, width, height))
            val effects = Effects(
                emptyList<AudioProcessor>(),
                videoEffects,
            )
            EditedMediaItem.Builder(c.toMediaItem())
                .setRemoveAudio(!keepOriginalAudio)
                .setEffects(effects)
                .build()
        }

        val sequences = mutableListOf(EditedMediaItemSequence(videoItems))

        for (a in audios) {
            val start = maxOf(0f, a.startSec)
            if (start >= total - 0.05f) continue // mulai setelah video selesai
            val dur = minOf(a.durationSec, total - start)
            if (dur <= 0.05f) continue

            val mediaBuilder = MediaItem.Builder().setUri(a.uri)
            if (a.trimStartSec > 0.01f || dur < a.sourceDurationSec - 0.05f) {
                mediaBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs((a.trimStartSec * 1000f).toLong())
                        .setEndPositionMs(((a.trimStartSec + dur) * 1000f).toLong())
                        .build(),
                )
            }
            val volume = a.volume.coerceIn(0f, 1f)
            val processors: List<AudioProcessor> =
                if (volume < 0.999f) listOf<AudioProcessor>(GainAudioProcessor(volume)) else emptyList<AudioProcessor>()
            val audioItem = EditedMediaItem.Builder(mediaBuilder.build())
                .setRemoveVideo(true)
                .setEffects(Effects(processors, emptyList<Effect>()))
                .build()

            val items = mutableListOf<EditedMediaItem>()
            if (start > 0.05f) {
                // Jeda hening sebelum audio supaya audio mulai tepat di posisinya pada timeline.
                val fmt = probeAudioFormat(app, a.uri)
                val wav = File(app.cacheDir, "silence_${System.nanoTime()}.wav")
                writeSilentWav(wav, start, fmt.first, fmt.second)
                temps.add(wav)
                items.add(
                    EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(wav)))
                        .setRemoveVideo(true)
                        .build(),
                )
            }
            items.add(audioItem)
            sequences.add(EditedMediaItemSequence(items))
        }

        val builder = Composition.Builder(sequences)

        // Teks dan stiker (TAHAP 5b): satu OverlayEffect di tingkat Composition (bukan per klip), supaya waktu
        // yang diterima = waktu timeline gabungan, sama dengan waktu di pratinjau. Bitmap digambar oleh OverlayRenderer.
        val visible = overlays.filter { it.startSec < total - 0.05f && it.durationSec > 0f }
        if (visible.isNotEmpty()) {
            val frames = OverlayFrames(width, height, visible)
            val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(frames))
            builder.setEffects(
                Effects(emptyList<AudioProcessor>(), listOf<Effect>(overlayEffect)),
            )
        }
        return builder.build()
    }

    private fun hasAudioTrack(context: Context, uri: Uri): Boolean {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
        } catch (e: Exception) {
            false
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
                // abaikan
            }
        }
    }

    /** (sampleRate, channelCount) trek audio pertama; bawaan 44100 Hz stereo bila gagal dibaca. */
    private fun probeAudioFormat(context: Context, uri: Uri): Pair<Int, Int> {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(context, uri, null)
            var result = Pair(44100, 2)
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    val rate = if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) f.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
                    val ch = if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
                    result = Pair(rate, ch.coerceIn(1, 2))
                    break
                }
            }
            result
        } catch (e: Exception) {
            Pair(44100, 2)
        } finally {
            try {
                ex.release()
            } catch (e: Exception) {
                // abaikan
            }
        }
    }

    /** Tulis file WAV PCM 16-bit berisi keheningan sepanjang [durationSec]. */
    private fun writeSilentWav(file: File, durationSec: Float, sampleRate: Int, channels: Int) {
        val bytesPerFrame = 2 * channels
        val frames = (durationSec * sampleRate).toLong()
        val dataSize = frames * bytesPerFrame
        file.outputStream().buffered().use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt((36L + dataSize).toInt())
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1.toShort()) // PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * bytesPerFrame)
            header.putShort(bytesPerFrame.toShort())
            header.putShort(16.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize.toInt())
            out.write(header.array())
            val zeros = ByteArray(64 * 1024)
            var remaining = dataSize
            while (remaining > 0L) {
                val n = minOf(remaining, zeros.size.toLong()).toInt()
                out.write(zeros, 0, n)
                remaining -= n
            }
        }
    }
}

/** Pengatur volume (gain) untuk audio PCM 16-bit. Dipakai agar volume tiap musik/suara ikut hasil ekspor. */
@androidx.annotation.OptIn(UnstableApi::class)
class GainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val count = inputBuffer.remaining()
        if (count == 0) return
        val out = replaceOutputBuffer(count)
        while (inputBuffer.remaining() >= 2) {
            val sample = inputBuffer.getShort().toInt()
            val scaled = (sample * gain).toInt().coerceIn(-32768, 32767)
            out.putShort(scaled.toShort())
        }
        out.flip()
    }
}
