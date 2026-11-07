package com.zay.videoeditor.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.zay.videoeditor.ui.theme.NeonAmber
import com.zay.videoeditor.ui.theme.NeonBlue
import com.zay.videoeditor.ui.theme.NeonMint
import com.zay.videoeditor.ui.theme.NeonOrange
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple

/**
 * DATA CONTOH untuk tampilan (TAHAP 1-2).
 * Akan diganti data proyek sungguhan yang disimpan di perangkat pada TAHAP 4.
 */
data class ProjectItem(
    val id: Int,
    val name: String,
    val duration: String,
    val resolution: String,
    val kind: String, // "Video" atau "Draf"
    val favorite: Boolean,
    val colors: List<Color>,
    /** Path video pertama proyek + posisi frame (detik) untuk thumbnail asli. Null = pakai gradasi. */
    val thumbPath: String? = null,
    val thumbSec: Float = 0f,
)

data class TimelineClip(
    val id: Int,
    val track: Int,
    val start: Float,
    val length: Float,
    val label: String,
)

data class TrackInfo(
    val name: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val isAudio: Boolean = false,
)

object DemoData {
    val projects = listOf(
        ProjectItem(1, "Liburan Seru", "00:45", "1080p", "Video", true, listOf(Color(0xFFFF8A4D), Color(0xFF7B2FFF))),
        ProjectItem(2, "Kota Malam", "01:20", "4K", "Video", false, listOf(NeonBlue, Color(0xFF2B1B6E))),
        ProjectItem(3, "Kucing Lucu", "00:32", "1080p", "Video", true, listOf(NeonAmber, Color(0xFFE0663D))),
        ProjectItem(4, "Bunga Indah", "00:28", "720p", "Draf", false, listOf(NeonPink, Color(0xFF6A3DFF))),
        ProjectItem(5, "Senja Hari Ini", "00:50", "1080p", "Video", false, listOf(Color(0xFFFF7A59), Color(0xFF3A1B6E))),
        ProjectItem(6, "Alam Pegunungan", "01:15", "4K", "Draf", false, listOf(NeonMint, NeonBlue)),
    )
}

object DemoTimeline {
    const val TOTAL_SECONDS = 45f

    val tracks = listOf(
        TrackInfo("Video", Icons.Rounded.Visibility, listOf(NeonBlue, NeonPurple)),
        TrackInfo("Teks", Icons.Rounded.TextFields, listOf(NeonPurple, Color(0xFF7B4DFF))),
        TrackInfo("Musik", Icons.Rounded.MusicNote, listOf(Color(0xFF2E9BFF), Color(0xFF1F6FE0)), isAudio = true),
        TrackInfo("Efek", Icons.Rounded.Layers, listOf(NeonPink, Color(0xFFB83A9A))),
    )

    val clips = listOf(
        TimelineClip(1, 0, 0f, 15f, "Klip 1"),
        TimelineClip(2, 0, 15.5f, 14.5f, "Klip 2"),
        TimelineClip(3, 0, 30.5f, 14.5f, "Klip 3"),
        TimelineClip(4, 1, 5f, 20f, "Selamat Menonton"),
        TimelineClip(5, 2, 1f, 44f, "Musik Latar"),
        TimelineClip(6, 3, 10f, 12f, "Efek Cahaya"),
    )
}

/** Sesi editor sederhana yang dipakai bersama oleh Editor dan Ekspor. Diganti ViewModel pada TAHAP 4. */
object EditorSession {
    // Urutan indeks DIPERTAHANKAN (0..2) agar proyek lama yang tersimpan tetap benar; pilihan baru ditambah di belakang.
    val resolutions = listOf("720P", "1080P", "4K", "2K", "480P")

    /** Urutan tampil di layar, dari terkecil ke terbesar (berisi indeks pada [resolutions]). */
    val resolutionOrder = listOf(4, 0, 1, 3, 2)
    var resolutionIndex by mutableStateOf(1)

    /** Video di timeline (TAHAP 3-4). Kosong = timeline masih contoh. */
    val clips = mutableStateListOf<ImportedClip>()

    val totalSeconds: Float
        get() = clips.fold(0f) { acc, c -> acc + c.durationSec }

    // ---- Rasio layar untuk media sosial (TAHAP 4c) ----
    val aspects = listOf(
        AspectOption("16:9", 16f / 9f, "Lanskap", "YouTube, TV, Facebook"),
        AspectOption("9:16", 9f / 16f, "Vertikal", "TikTok, Reels, Shorts, Status WA"),
        AspectOption("1:1", 1f, "Persegi", "Feed Instagram, Facebook"),
        AspectOption("4:5", 4f / 5f, "Potret", "Feed Instagram dan Facebook"),
        AspectOption("4:3", 4f / 3f, "Klasik", "Facebook, presentasi"),
        AspectOption("21:9", 21f / 9f, "Sinematik", "Film, bingkai lebar"),
    )
    var aspectIndex by mutableStateOf(0)

    /** false = Pas (seluruh video terlihat, ada bidang kosong); true = Isi (dipotong memenuhi bingkai). */
    var fillFrame by mutableStateOf(true)

    val aspect: AspectOption
        get() = aspects[aspectIndex.coerceIn(0, aspects.size - 1)]

    /** Ukuran video hasil (lebar, tinggi) menurut resolusi dan rasio terpilih, selalu genap. */
    fun outputSize(): Pair<Int, Int> {
        val short = when (resolutionIndex) {
            0 -> 720
            2 -> 2160
            3 -> 1440
            4 -> 480
            else -> 1080
        }
        val r = aspect.ratio
        val w: Float
        val h: Float
        if (r >= 1f) {
            h = short.toFloat()
            w = short * r
        } else {
            w = short.toFloat()
            h = short / r
        }
        return Pair((w / 2f).toInt() * 2, (h / 2f).toInt() * 2)
    }

    // ---- Audio: musik dan rekaman suara (TAHAP 5a) ----
    val audioClips = mutableStateListOf<AudioClip>()

    val audioEndSec: Float
        get() = audioClips.fold(0f) { acc, a -> maxOf(acc, a.startSec + a.durationSec) }

    fun addAudio(clip: AudioClip) {
        audioClips.add(clip)
    }

    fun deleteAudio(id: Int) {
        val i = audioClips.indexOfFirst { it.id == id }
        if (i >= 0) audioClips.removeAt(i)
    }

    fun setAudioVolume(id: Int, volume: Float) {
        val i = audioClips.indexOfFirst { it.id == id }
        if (i >= 0) audioClips[i] = audioClips[i].copy(volume = volume)
    }

    /** Membelah audio di [atSec] (waktu timeline) menjadi dua klip. False bila titik terlalu dekat tepi. */
    fun splitAudio(id: Int, atSec: Float): Boolean {
        val i = audioClips.indexOfFirst { it.id == id }
        if (i < 0) return false
        val a = audioClips[i]
        val local = atSec - a.startSec
        if (local < 0.3f || local > a.durationSec - 0.3f) return false
        audioClips[i] = a.copy(durationSec = local)
        audioClips.add(
            i + 1,
            a.copy(
                id = nextId(),
                startSec = atSec,
                trimStartSec = a.trimStartSec + local,
                durationSec = a.durationSec - local,
            ),
        )
        return true
    }

    /** Menggeser tepi audio. Tepi kiri: [deltaSec] > 0 memotong awal. Tepi kanan: [deltaSec] > 0 memperpanjang. */
    fun trimAudio(id: Int, leftEdge: Boolean, deltaSec: Float) {
        val i = audioClips.indexOfFirst { it.id == id }
        if (i < 0) return
        val a = audioClips[i]
        if (a.durationSec <= 0.31f && deltaSec > 0f && leftEdge) return
        if (leftEdge) {
            val minD = -minOf(a.trimStartSec, a.startSec)
            val maxD = maxOf(minD, a.durationSec - 0.3f)
            val d = deltaSec.coerceIn(minD, maxD)
            audioClips[i] = a.copy(
                startSec = a.startSec + d,
                trimStartSec = a.trimStartSec + d,
                durationSec = a.durationSec - d,
            )
        } else {
            val maxDur = maxOf(0.3f, a.sourceDurationSec - a.trimStartSec)
            val nd = (a.durationSec + deltaSec).coerceIn(0.3f, maxDur)
            audioClips[i] = a.copy(durationSec = nd)
        }
    }

    fun moveAudio(id: Int, startSec: Float) {
        val i = audioClips.indexOfFirst { it.id == id }
        if (i >= 0) audioClips[i] = audioClips[i].copy(startSec = maxOf(0f, startSec))
    }

    // ---- Teks dan stiker di atas video (TAHAP 5b) ----
    val overlays = mutableStateListOf<Overlay>()

    val overlayEndSec: Float
        get() = overlays.fold(0f) { acc, o -> maxOf(acc, o.endSec) }

    fun addOverlay(overlay: Overlay) {
        overlays.add(overlay)
    }

    fun updateOverlay(id: Int, change: (Overlay) -> Overlay) {
        val i = overlays.indexOfFirst { it.id == id }
        if (i >= 0) overlays[i] = change(overlays[i])
    }

    fun deleteOverlay(id: Int) {
        val i = overlays.indexOfFirst { it.id == id }
        if (i >= 0) overlays.removeAt(i)
    }

    /** Mengembalikan id salinan (digeser sedikit supaya terlihat), atau null bila tidak ditemukan. */
    fun duplicateOverlay(id: Int): Int? {
        val i = overlays.indexOfFirst { it.id == id }
        if (i < 0) return null
        val o = overlays[i]
        val copy = o.copy(id = nextId(), x = (o.x + 0.05f).coerceAtMost(1f), y = (o.y + 0.05f).coerceAtMost(1f))
        overlays.add(copy)
        return copy.id
    }

    // ---- Proyek yang sedang dibuka (TAHAP 4b) ----
    var projectId: Int = 0
    var projectName: String = ""
    var favorite: Boolean = false

    fun start(
        id: Int,
        name: String,
        resolution: Int,
        fav: Boolean,
        list: List<ImportedClip>,
        aspect: Int = 0,
        fill: Boolean = true,
        audio: List<AudioClip> = emptyList(),
        overlayList: List<Overlay> = emptyList(),
    ) {
        audioClips.clear()
        audioClips.addAll(audio)
        overlays.clear()
        overlays.addAll(overlayList)
        aspectIndex = aspect.coerceIn(0, aspects.size - 1)
        fillFrame = fill
        undoStack.clear()
        redoStack.clear()
        projectId = id
        projectName = name
        favorite = fav
        resolutionIndex = resolution
        clips.clear()
        clips.addAll(list)
    }

    private var idCounter = 0

    fun nextId(): Int {
        idCounter += 1
        return idCounter
    }

    // ---- Undo / redo (TAHAP 4) ----
    private val undoStack = ArrayDeque<List<ImportedClip>>()
    private val redoStack = ArrayDeque<List<ImportedClip>>()

    /** Dipanggil SEBELUM mengubah [clips] supaya perubahan bisa dibatalkan. */
    private fun snapshot() {
        undoStack.addLast(clips.toList())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(clips.toList())
        val prev = undoStack.removeLast()
        clips.clear()
        clips.addAll(prev)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(clips.toList())
        val next = redoStack.removeLast()
        clips.clear()
        clips.addAll(next)
        return true
    }

    // ---- Operasi klip (TAHAP 4) ----
    fun addClip(clip: ImportedClip) {
        snapshot()
        clips.add(clip)
    }

    fun deleteClip(id: Int) {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return
        snapshot()
        clips.removeAt(i)
    }

    /** Ubah filter/transisi satu klip. [record] true = bisa dibatalkan lewat undo (false untuk geseran slider). */
    fun updateClip(id: Int, record: Boolean, change: (ImportedClip) -> ImportedClip) {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return
        if (record) snapshot()
        clips[i] = change(clips[i])
    }

    /** Mengembalikan id salinan, atau null bila klip tidak ditemukan. */
    fun duplicateClip(id: Int): Int? {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return null
        snapshot()
        val copy = clips[i].copy(id = nextId())
        clips.add(i + 1, copy)
        return copy.id
    }

    /** Geser klip ke kiri (delta = -1) atau kanan (delta = 1). False bila sudah di ujung. */
    fun moveClip(id: Int, delta: Int): Boolean {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return false
        val j = i + delta
        if (j < 0 || j >= clips.size) return false
        snapshot()
        val c = clips.removeAt(i)
        clips.add(j, c)
        return true
    }

    /** Belah klip pada [localSec] detik dari awal klip (waktu timeline). */
    fun splitClip(id: Int, localSec: Float): Boolean {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return false
        val c = clips[i]
        if (localSec <= 0.1f || localSec >= c.durationSec - 0.1f) return false
        snapshot()
        val mid = c.trimStartSec + localSec
        clips[i] = c.copy(trimEndSec = mid, transition = 0)
        clips.add(i + 1, c.copy(id = nextId(), trimStartSec = mid))
        return true
    }
}

/** Musik atau rekaman suara. Mulai di [startSec] pada timeline dan berdurasi [durationSec]. */
data class AudioClip(
    val id: Int,
    val uri: Uri,
    val name: String,
    val durationSec: Float,
    val startSec: Float = 0f,
    val volume: Float = 1f,
    val isVoice: Boolean = false,
    /** Titik awal yang diputar di dalam file audio (detik); > 0 bila bagian awal dipotong. */
    val trimStartSec: Float = 0f,
    /** Panjang file audio asli (detik), batas perpanjangan saat tepi digeser. */
    val sourceDurationSec: Float = durationSec,
)

data class AspectOption(
    val label: String,
    val ratio: Float, // lebar / tinggi
    val title: String,
    val platforms: String,
)

/** Satu video di timeline. Bagian yang dipakai adalah [trimStartSec]..[trimEndSec] dari file asli. */
data class ImportedClip(
    val id: Int,
    val uri: Uri,
    val name: String,
    val sourceDurationSec: Float,
    val trimStartSec: Float = 0f,
    val trimEndSec: Float = sourceDurationSec,
    /** Filter warna (0 = asli) dan kekuatannya 0..1 (TAHAP 5c). */
    val filter: Int = 0,
    val filterStrength: Float = 0.7f,
    /** Transisi di sambungan antara klip ini dan klip berikutnya (0 = tanpa) dan durasinya dalam detik. */
    val transition: Int = 0,
    val transitionDur: Float = 0.6f,
    /** Gerakan klip (Getar, Denyut, Zoom, dll.; 0 = tanpa) dan kekuatannya 0..1. */
    val motion: Int = 0,
    val motionStrength: Float = 0.5f,
) {
    val durationSec: Float
        get() = trimEndSec - trimStartSec
}
