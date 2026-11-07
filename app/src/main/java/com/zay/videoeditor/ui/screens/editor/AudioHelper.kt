package com.zay.videoeditor.ui.screens.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zay.videoeditor.data.AudioClip
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.data.ProjectStore
import java.io.File
import kotlin.math.abs

/** Baca durasi file audio dalam detik. 0 bila gagal. Panggil dari thread latar belakang. */
fun probeDurationSec(context: Context, uri: Uri): Float {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        ms / 1000f
    } catch (e: Exception) {
        0f
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            // abaikan
        }
    }
}

/** Salin audio pilihan pengguna ke penyimpanan aplikasi lalu bentuk klip audio. Null bila gagal. */
fun readImportedAudio(context: Context, sourceUri: Uri, startSec: Float): AudioClip? {
    val name: String? = try {
        context.contentResolver
            .query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }
    val ext = name?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 } ?: "m4a"
    val dest = File(ProjectStore.mediaDir(context), "audio_${System.currentTimeMillis()}_${EditorSession.nextId()}.$ext")
    try {
        val input = context.contentResolver.openInputStream(sourceUri) ?: return null
        input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
    } catch (e: Exception) {
        dest.delete()
        return null
    }
    val uri = Uri.fromFile(dest)
    val seconds = probeDurationSec(context, uri)
    if (seconds <= 0f) {
        dest.delete()
        return null
    }
    return AudioClip(
        id = EditorSession.nextId(),
        uri = uri,
        name = name?.substringBeforeLast('.') ?: "Musik",
        durationSec = seconds,
        startSec = startSec,
        volume = 1f,
        isVoice = false,
    )
}

/** Bentuk klip audio dari file rekaman suara. Null bila file kosong atau rusak. */
fun readRecordedAudio(context: Context, file: File, startSec: Float, number: Int): AudioClip? {
    val uri = Uri.fromFile(file)
    val seconds = probeDurationSec(context, uri)
    if (seconds <= 0f) {
        file.delete()
        return null
    }
    return AudioClip(
        id = EditorSession.nextId(),
        uri = uri,
        name = "Suara $number",
        durationSec = seconds,
        startSec = startSec,
        volume = 1f,
        isVoice = true,
    )
}

/** Perekam suara dari mikrofon ke file .m4a di penyimpanan aplikasi. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    val isRecording: Boolean
        get() = recorder != null

    @Suppress("DEPRECATION")
    fun start(): Boolean {
        val f = File(ProjectStore.mediaDir(context), "voice_${System.currentTimeMillis()}.m4a")
        var r: MediaRecorder? = null
        return try {
            r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = f
            true
        } catch (e: Exception) {
            try {
                r?.release()
            } catch (e2: Exception) {
                // abaikan
            }
            recorder = null
            file = null
            f.delete()
            false
        }
    }

    /** Hentikan rekaman. Mengembalikan file hasil, atau null bila gagal. */
    fun stop(): File? {
        val r = recorder ?: return null
        val f = file
        recorder = null
        file = null
        return try {
            r.stop()
            r.release()
            f
        } catch (e: Exception) {
            try {
                r.release()
            } catch (e2: Exception) {
                // abaikan
            }
            f?.delete()
            null
        }
    }

    /** Batalkan dan buang rekaman yang sedang berjalan. */
    fun cancel() {
        stop()?.delete()
    }
}

/**
 * Memutar klip audio (musik/suara) bersamaan dengan video di pratinjau.
 * Satu ExoPlayer per klip audio; [sync] dipanggil berkala dengan posisi playhead.
 */
class AudioMixer(private val context: Context) {
    private val players = mutableMapOf<Int, ExoPlayer>()

    fun sync(clips: List<AudioClip>, playheadSec: Float, playing: Boolean) {
        val ids = clips.map { it.id }.toSet()
        val gone = players.keys.filter { it !in ids }
        for (id in gone) {
            players.remove(id)?.release()
        }
        for (c in clips) {
            val p = players.getOrPut(c.id) {
                ExoPlayer.Builder(context).build().also {
                    it.setMediaItem(MediaItem.fromUri(c.uri))
                    it.prepare()
                }
            }
            p.volume = c.volume.coerceIn(0f, 1f)
            val local = playheadSec - c.startSec
            val inside = local >= 0f && local < c.durationSec
            if (playing && inside) {
                if (!p.playWhenReady || p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(((c.trimStartSec + local) * 1000f).toLong())
                    p.play()
                } else if (p.playbackState == Player.STATE_READY &&
                    abs(p.currentPosition / 1000f - (c.trimStartSec + local)) > 0.4f
                ) {
                    p.seekTo(((c.trimStartSec + local) * 1000f).toLong())
                }
            } else if (p.playWhenReady) {
                p.pause()
            }
        }
    }

    fun pauseAll() {
        for (p in players.values) p.pause()
    }

    fun release() {
        for (p in players.values) p.release()
        players.clear()
    }
}
