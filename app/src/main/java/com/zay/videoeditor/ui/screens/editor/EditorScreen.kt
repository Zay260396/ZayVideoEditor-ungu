package com.zay.videoeditor.ui.screens.editor

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zay.videoeditor.R
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.data.OverlayKind
import com.zay.videoeditor.data.OverlayStyle
import com.zay.videoeditor.data.ProjectStore
import com.zay.videoeditor.data.TimelineClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.IconCircleButton
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.components.ToolTile
import com.zay.videoeditor.ui.components.ZayBackground
import com.zay.videoeditor.ui.components.showSoon
import com.zay.videoeditor.fx.ClipFx
import com.zay.videoeditor.overlay.FxLayers
import com.zay.videoeditor.ui.theme.Navy
import com.zay.videoeditor.ui.theme.NavyCard
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import java.util.Locale

enum class EditorPanel(val label: String, val icon: ImageVector) {
    Trim("Potong", Icons.Rounded.ContentCut),
    Music("Musik", Icons.Rounded.MusicNote),
    Teks("Teks", Icons.Rounded.TextFields),
    Effects("Efek", Icons.Rounded.AutoAwesome),
    Transition("Transisi", Icons.Rounded.SwapHoriz),
    Sticker("Stiker", Icons.Rounded.EmojiEmotions),
    Ratio("Rasio", Icons.Rounded.AspectRatio),
}

private fun formatTime(sec: Float): String {
    val m = (sec / 60f).toInt()
    val s = sec - m * 60f
    return String.format(Locale.US, "%02d:%04.1f", m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onBack: () -> Unit, onExport: () -> Unit) {
    val context = LocalContext.current
    val soon: (String) -> Unit = { stage -> showSoon(context, stage) }

    var panel by rememberSaveable { mutableStateOf<EditorPanel?>(null) }
    var selectedClip by rememberSaveable { mutableStateOf(EditorSession.clips.firstOrNull()?.id ?: -1) }
    var playhead by rememberSaveable { mutableStateOf(0f) }
    var resMenu by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val importedClips = EditorSession.clips
    val imported = importedClips.isNotEmpty()

    // Pemutar video
    val exo = remember { ExoPlayer.Builder(context).build() }
    val mixer = remember { AudioMixer(context) }
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0f) }
    var recordStartSec by remember { mutableStateOf(0f) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exo.addListener(listener)
        val owner = context as? ComponentActivity
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_STOP) {
                    exo.pause()
                    mixer.pauseAll()
                    if (recorder.isRecording) {
                        recorder.cancel()
                        recording = false
                    }
                    ProjectStore.saveCurrent(context)
                }
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            exo.removeListener(listener)
            ProjectStore.saveCurrent(context)
            recorder.cancel()
            mixer.release()
            exo.release()
        }
    }

    // Muat ulang daftar putar setiap kali isi timeline berubah (impor, belah, hapus, dll.)
    LaunchedEffect(importedClips.toList()) {
        delay(120) // jeda singkat supaya menyeret tepi klip tidak memuat ulang pemutar di tiap gerakan
        if (importedClips.isEmpty()) {
            exo.clearMediaItems()
        } else {
            exo.setMediaItems(importedClips.map { it.toMediaItem() })
            exo.prepare()
            exo.seekToGlobal(importedClips, playhead)
        }
    }

    // Simpan otomatis setiap kali timeline atau resolusi berubah
    LaunchedEffect(
        importedClips.toList(),
        EditorSession.resolutionIndex,
        EditorSession.aspectIndex,
        EditorSession.fillFrame,
        EditorSession.audioClips.toList(),
        EditorSession.overlays.toList(),
    ) {
        ProjectStore.saveCurrent(context)
    }

    // Playhead mengikuti pemutar saat video diputar
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playhead = exo.globalPositionSec(importedClips)
            mixer.sync(EditorSession.audioClips.toList(), playhead, true)
            delay(100)
        }
        if (importedClips.isNotEmpty()) {
            playhead = exo.globalPositionSec(importedClips)
        }
        mixer.pauseAll()
    }

    // Waktu untuk teks/stiker di pratinjau: dibaca tiap frame saat diputar supaya animasi mulus.
    // Dibaca hanya saat menggambar ([overlayTime]), jadi tidak menyusun ulang seluruh layar.
    var overlayClock by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withFrameNanos { }
            overlayClock = exo.globalPositionSec(importedClips)
        }
    }
    val overlayTime: () -> Float = { if (isPlaying) overlayClock else playhead }

    // Sinkronkan pemutar audio saat daftar audio atau volumenya berubah
    LaunchedEffect(EditorSession.audioClips.toList()) {
        mixer.sync(EditorSession.audioClips.toList(), playhead, isPlaying)
    }

    // Penghitung waktu saat merekam suara
    LaunchedEffect(recording) {
        while (recording) {
            delay(250)
            recordSeconds += 0.25f
        }
    }

    val togglePlay: () -> Unit = {
        if (importedClips.isEmpty()) {
            Toast.makeText(context, "Tambahkan video dulu lewat tombol + Media", Toast.LENGTH_SHORT).show()
        } else if (exo.isPlaying) {
            exo.pause()
        } else {
            if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0, 0L)
            exo.play()
        }
    }

    // Pemilih video dari galeri
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            Toast.makeText(context, "Menyalin video ke proyek, tunggu sebentar...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val wasEmpty = EditorSession.clips.isEmpty()
                var failed = 0
                for (uri in uris) {
                    val clip = withContext(Dispatchers.IO) { readImportedClip(context, uri) }
                    if (clip != null) EditorSession.addClip(clip) else failed++
                }
                if (wasEmpty && EditorSession.clips.isNotEmpty()) {
                    playhead = 0f
                    selectedClip = EditorSession.clips.first().id
                }
                if (failed > 0) {
                    Toast.makeText(context, "$failed video tidak bisa dibaca", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val total = if (imported) EditorSession.totalSeconds else 0f
    val clips: List<TimelineClip> = if (imported) {
        var acc = 0f
        importedClips.map { c ->
            val item = TimelineClip(c.id, 0, acc, c.durationSec, c.name)
            acc += c.durationSec
            item
        }
    } else {
        emptyList()
    }
    val videoStarts = clips.filter { it.track == 0 }.map { it.start }.sorted()
    val selected = clips.firstOrNull { it.id == selectedClip }

    // ---- Filter dan transisi (TAHAP 5c) ----
    val previewClip: ImportedClip? = importedClips.getOrNull(clipIndexAt(importedClips, playhead).first)
    // Bila yang terpilih bukan klip video (mis. teks atau musik), efek dan transisi dipasang ke klip di garis putih.
    val selectedClipData: ImportedClip? = importedClips.firstOrNull { it.id == selectedClip } ?: previewClip
    val filterKey = remember { FilterCache() }
    val effectTimeline: List<TimelineClip> = if (imported) {
        val out = mutableListOf<TimelineClip>()
        importedClips.forEachIndexed { k, c ->
            val base = clips.getOrNull(k) ?: return@forEachIndexed
            if (c.filter > 0) {
                out.add(TimelineClip(c.id + 1_000_000, 3, base.start, base.length, ClipFx.filters[c.filter].name))
            }
            if (c.transition > 0 && k < importedClips.lastIndex) {
                val d = minOf(c.transitionDur, c.durationSec, importedClips[k + 1].durationSec)
                out.add(
                    TimelineClip(
                        c.id + 2_000_000,
                        3,
                        maxOf(0f, base.start + base.length - d / 2f),
                        d,
                        ClipFx.transitionNames[c.transition],
                    ),
                )
            }
        }
        out
    } else {
        emptyList()
    }

    // ---- Operasi edit (TAHAP 4) ----
    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    // ---- Audio (TAHAP 5a) ----
    val selectedAudio = EditorSession.audioClips.firstOrNull { it.id == selectedClip }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val startAt = playhead
            toast("Menyalin audio ke proyek, tunggu sebentar...")
            scope.launch {
                val clip = withContext(Dispatchers.IO) { readImportedAudio(context, uri, startAt) }
                if (clip != null) {
                    EditorSession.addAudio(clip)
                    selectedClip = clip.id
                } else {
                    toast("Audio tidak bisa dibaca")
                }
            }
        }
    }

    val startRecording: () -> Unit = {
        if (recorder.start()) {
            recordStartSec = playhead
            recordSeconds = 0f
            recording = true
            if (importedClips.isNotEmpty()) {
                if (exo.playbackState == Player.STATE_ENDED) {
                    exo.seekTo(0, 0L)
                    playhead = 0f
                    recordStartSec = 0f
                }
                exo.play()
            }
        } else {
            toast("Tidak bisa memulai rekaman")
        }
    }

    val stopRecording: () -> Unit = {
        recording = false
        exo.pause()
        val file = recorder.stop()
        val startAt = recordStartSec
        val number = EditorSession.audioClips.count { it.isVoice } + 1
        if (file == null) {
            toast("Rekaman gagal atau terlalu singkat")
        } else {
            scope.launch {
                val clip = withContext(Dispatchers.IO) { readRecordedAudio(context, file, startAt, number) }
                if (clip != null) {
                    EditorSession.addAudio(clip)
                    selectedClip = clip.id
                    toast("Rekaman suara ditambahkan")
                } else {
                    toast("Rekaman gagal disimpan")
                }
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else toast("Izin mikrofon diperlukan untuk merekam suara")
    }

    val onToggleRecord: () -> Unit = {
        if (recording) {
            stopRecording()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val audioTimeline: List<TimelineClip> = EditorSession.audioClips.map { a ->
        TimelineClip(a.id, 2, a.startSec, a.durationSec, a.name)
    }

    // ---- Teks dan stiker (TAHAP 5b): lapisan Teks (indeks 1) di timeline ----
    val selectedOverlay: Overlay? = EditorSession.overlays.firstOrNull { it.id == selectedClip }
    val overlayTimeline: List<TimelineClip> = EditorSession.overlays.map { o ->
        if (o.kind == OverlayKind.Effect) {
            TimelineClip(o.id, 3, o.startSec, o.durationSec, FxLayers.nameOf(o.content))
        } else {
            TimelineClip(o.id, 1, o.startSec, o.durationSec, o.content)
        }
    }

    val overlayAdd: (Overlay) -> Unit = { draft ->
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else {
            val start = playhead.coerceIn(0f, maxOf(0f, total - OverlayStyle.MIN_DURATION))
            val dur = minOf(OverlayStyle.DEFAULT_DURATION, maxOf(total - start, OverlayStyle.MIN_DURATION))
            val o = draft.copy(id = EditorSession.nextId(), startSec = start, durationSec = dur)
            EditorSession.addOverlay(o)
            selectedClip = o.id
            panel = null
            if (o.anim != 0) {
                // Geser garis putih sedikit ke dalam supaya animasi masuk terlihat di pratinjau.
                playhead = start + minOf(0.8f, dur / 2f)
                exo.seekToGlobal(importedClips, playhead)
            }
        }
    }
    val overlayChange: (Overlay) -> Unit = { updated ->
        EditorSession.updateOverlay(updated.id) { updated }
    }
    val overlayDuplicate: () -> Unit = {
        selectedOverlay?.let { o ->
            EditorSession.duplicateOverlay(o.id)?.let { selectedClip = it }
        }
    }
    val overlayDelete: () -> Unit = {
        selectedOverlay?.let { EditorSession.deleteOverlay(it.id) }
        selectedClip = EditorSession.clips.firstOrNull()?.id ?: -1
        panel = null
    }
    val overlayDrag: (Float, Float) -> Unit = { dx, dy ->
        val id = selectedClip
        EditorSession.updateOverlay(id) { it.copy(x = (it.x + dx).coerceIn(0f, 1f), y = (it.y + dy).coerceIn(0f, 1f)) }
    }
    // Memilih teks/stiker di timeline: bila garis putih di luar rentangnya, pindah ke dalam rentang agar terlihat.
    val onSelectItem: (Int) -> Unit = { id ->
        selectedClip = id
        EditorSession.overlays.firstOrNull { it.id == id }?.let { o ->
            if (!o.isActiveAt(playhead)) {
                playhead = o.startSec + minOf(0.6f, o.durationSec / 2f)
                if (imported) exo.seekToGlobal(importedClips, playhead)
            }
        }
    }

    val afterEdit: () -> Unit = {
        val list = EditorSession.clips
        if (list.isEmpty()) {
            selectedClip = -1
            playhead = 0f
        } else {
            if (list.none { it.id == selectedClip }) selectedClip = list.first().id
            playhead = playhead.coerceIn(0f, EditorSession.totalSeconds)
        }
    }

    val onSplit: () -> Unit = {
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else if (selected == null) {
            toast("Pilih klip dulu di timeline")
        } else {
            val local = playhead - selected.start
            if (local < 0.3f || local > selected.length - 0.3f) {
                toast("Geser garis putih ke dalam klip terpilih, lalu coba lagi")
            } else {
                EditorSession.splitClip(selected.id, local)
                panel = null
                afterEdit()
            }
        }
    }

    // ---- Efek lapisan (salju, hujan, dll.) ----
    val fxAdd: (String) -> Unit = { key ->
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else {
            val cur = selectedOverlay?.takeIf { it.kind == OverlayKind.Effect }
            if (cur != null) {
                EditorSession.updateOverlay(cur.id) { it.copy(content = key) }
            } else {
                val start = playhead.coerceIn(0f, maxOf(0f, total - OverlayStyle.MIN_DURATION))
                val dur = minOf(4f, maxOf(total - start, OverlayStyle.MIN_DURATION))
                val o = Overlay(
                    id = EditorSession.nextId(),
                    kind = OverlayKind.Effect,
                    content = key,
                    startSec = start,
                    durationSec = dur,
                    size = 70f,
                )
                EditorSession.addOverlay(o)
                selectedClip = o.id
            }
        }
    }

    // ---- Geser, perpanjang, dan hapus langsung di timeline ----
    val timelineDragStart: (Int) -> Unit = { id ->
        // Catat satu langkah undo di awal seretan klip video
        if (importedClips.any { it.id == id }) EditorSession.updateClip(id, true) { it }
    }
    val timelineDragBody: (Int, Float) -> Unit = { id, d ->
        val ov = EditorSession.overlays.firstOrNull { it.id == id }
        val au = EditorSession.audioClips.firstOrNull { it.id == id }
        if (ov != null) {
            val maxStart = maxOf(0f, maxOf(total, ov.endSec) - ov.durationSec)
            EditorSession.updateOverlay(id) { it.copy(startSec = (it.startSec + d).coerceIn(0f, maxStart)) }
        } else if (au != null) {
            EditorSession.moveAudio(id, (au.startSec + d).coerceIn(0f, maxOf(0f, total - 0.5f)))
        }
    }
    val timelineDragEdge: (Int, Boolean, Float) -> Unit = { id, left, d ->
        val ov = EditorSession.overlays.firstOrNull { it.id == id }
        val vc = importedClips.firstOrNull { it.id == id }
        if (EditorSession.audioClips.any { it.id == id }) {
            EditorSession.trimAudio(id, left, d)
        } else if (ov != null) {
            val minD = OverlayStyle.MIN_DURATION
            if (left) {
                val newStart = (ov.startSec + d).coerceIn(0f, ov.endSec - minD)
                EditorSession.updateOverlay(id) { it.copy(startSec = newStart, durationSec = it.endSec - newStart) }
            } else {
                val maxEnd = maxOf(total, ov.startSec + minD)
                val newDur = (ov.durationSec + d).coerceIn(minD, maxOf(minD, maxEnd - ov.startSec))
                EditorSession.updateOverlay(id) { it.copy(durationSec = newDur) }
            }
        } else if (vc != null) {
            val minD = 0.3f
            if (left) {
                val ns = (vc.trimStartSec + d).coerceIn(0f, vc.trimEndSec - minD)
                EditorSession.updateClip(id, false) { it.copy(trimStartSec = ns) }
            } else {
                val ne = (vc.trimEndSec + d).coerceIn(vc.trimStartSec + minD, vc.sourceDurationSec)
                EditorSession.updateClip(id, false) { it.copy(trimEndSec = ne) }
            }
        }
    }
    val timelineDelete: (Int) -> Unit = { id ->
        if (EditorSession.overlays.any { it.id == id }) {
            EditorSession.deleteOverlay(id)
        } else if (EditorSession.audioClips.any { it.id == id }) {
            EditorSession.deleteAudio(id)
        } else if (importedClips.any { it.id == id }) {
            EditorSession.deleteClip(id)
        }
        selectedClip = EditorSession.clips.firstOrNull()?.id ?: -1
        panel = null
        afterEdit()
    }

    val onDelete: () -> Unit = {
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else if (selected == null) {
            toast("Pilih klip dulu di timeline")
        } else {
            val idx = importedClips.indexOfFirst { it.id == selected.id }
            EditorSession.deleteClip(selected.id)
            val list = EditorSession.clips
            selectedClip = list.getOrNull(minOf(idx, list.size - 1))?.id ?: -1
            panel = null
            afterEdit()
        }
    }

    val onDuplicate: () -> Unit = {
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else if (selected == null) {
            toast("Pilih klip dulu di timeline")
        } else {
            val newId = EditorSession.duplicateClip(selected.id)
            if (newId != null) selectedClip = newId
            panel = null
            afterEdit()
        }
    }

    val onMove: (Int) -> Unit = { delta ->
        if (!imported) {
            toast("Tambahkan video dulu lewat tombol + Media")
        } else if (selected == null) {
            toast("Pilih klip dulu di timeline")
        } else if (EditorSession.moveClip(selected.id, delta)) {
            panel = null
            afterEdit()
        } else {
            toast("Klip sudah di ujung")
        }
    }

    val onUndo: () -> Unit = {
        if (EditorSession.undo()) afterEdit() else toast("Tidak ada yang bisa dibatalkan")
    }
    val onRedo: () -> Unit = {
        if (EditorSession.redo()) afterEdit() else toast("Tidak ada yang bisa diulang")
    }

    Box(Modifier.fillMaxSize()) {
    ZayBackground {
        Column(Modifier.fillMaxSize()) {
            // Bilah atas
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    "ZAY EDITOR",
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box {
                    val chipShape = RoundedCornerShape(12.dp)
                    Row(
                        Modifier
                            .clip(chipShape)
                            .background(NavyCardHigh)
                            .border(1.dp, StrokeSoft, chipShape)
                            .clickable { resMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            EditorSession.resolutions[EditorSession.resolutionIndex],
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = resMenu, onDismissRequest = { resMenu = false }) {
                        EditorSession.resolutionOrder.forEach { i ->
                            val r = EditorSession.resolutions[i]
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    EditorSession.resolutionIndex = i
                                    resMenu = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                GradientButton(
                    text = "Export",
                    onClick = onExport,
                    height = 38.dp,
                    horizontalPadding = 18.dp,
                )
            }

            // Pratinjau: pemutar video sungguhan; gradasi saat belum ada video
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
            val ratio = EditorSession.aspect.ratio
            val frameW = minOf(maxWidth, 250.dp * ratio)
            val frameH = frameW / ratio
            Box(
                Modifier
                    .size(frameW, frameH)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2B1B6E), Color(0xFF8A2BE2), Color(0xFFFF7A59)),
                        ),
                    )
                    .border(1.dp, StrokeSoft, RoundedCornerShape(22.dp))
                    .clickable { togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                if (imported) {
                    AndroidView(
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(R.layout.zay_player_view, null, false) as PlayerView)
                                .also { it.player = exo }
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                // Gerakan klip (Animasi): dibaca saat menggambar supaya mulus tanpa menyusun ulang layar
                                val clipsNow = EditorSession.clips
                                val (ci, local) = clipIndexAt(clipsNow, overlayTime())
                                val c = clipsNow.getOrNull(ci)
                                val mo = if (c != null && c.motion > 0) {
                                    ClipFx.motionAt(c.motion, c.motionStrength, local, c.durationSec)
                                } else {
                                    null
                                }
                                if (mo != null) {
                                    scaleX = if (mo.flip) -mo.scale else mo.scale
                                    scaleY = mo.scale
                                    translationX = mo.tx * size.width / 2f
                                    translationY = -mo.ty * size.height / 2f
                                    rotationZ = mo.rot
                                } else {
                                    scaleX = 1f
                                    scaleY = 1f
                                    translationX = 0f
                                    translationY = 0f
                                    rotationZ = 0f
                                }
                            },
                        update = { view ->
                            // 0 = pas (seluruh video terlihat), 4 = isi (dipotong memenuhi bingkai)
                            view.resizeMode = if (EditorSession.fillFrame) 4 else 0
                            view.applyClipFilter(previewClip?.filter ?: 0, previewClip?.filterStrength ?: 0f, filterKey)
                            // Saat layar penuh aktif, pemutar dipakai tampilan layar penuh.
                            if (fullscreen) {
                                if (view.player != null) view.player = null
                            } else if (view.player !== exo) {
                                view.player = exo
                            }
                        },
                    )
                }
                if (imported) {
                    TransitionDip(playhead, Modifier.matchParentSize())
                }
                OverlayLayer(
                    overlays = EditorSession.overlays,
                    time = overlayTime,
                    selectedId = selectedClip,
                    modifier = Modifier.matchParentSize(),
                    dragEnabled = selectedOverlay != null && selectedOverlay.kind != OverlayKind.Effect && selectedOverlay.isActiveAt(playhead),
                    onDrag = overlayDrag,
                )
                if (!isPlaying) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Icon(
                    Icons.Rounded.Fullscreen,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(22.dp)
                        .clickable {
                            if (imported) fullscreen = true else toast("Tambahkan video dulu lewat tombol + Media")
                        },
                )
            }
            }

            // Kontrol transport
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatTime(playhead)} / ${formatTime(total)}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                IconCircleButton(
                    Icons.Rounded.SkipPrevious,
                    onClick = {
                        playhead = videoStarts.lastOrNull { it < playhead - 0.1f } ?: 0f
                        if (imported) exo.seekToGlobal(importedClips, playhead)
                    },
                    size = 38.dp,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .clickable { togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconCircleButton(
                    Icons.Rounded.SkipNext,
                    onClick = {
                        playhead = videoStarts.firstOrNull { it > playhead + 0.1f } ?: total
                        if (imported) exo.seekToGlobal(importedClips, playhead)
                    },
                    size = 38.dp,
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    IconCircleButton(Icons.AutoMirrored.Rounded.Undo, onUndo, size = 36.dp)
                    Spacer(Modifier.width(6.dp))
                    IconCircleButton(Icons.AutoMirrored.Rounded.Redo, onRedo, size = 36.dp)
                }
            }

            // Info klip terpilih + tombol tambah media
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected != null) {
                        "Terpilih: ${selected.label} • ${String.format(Locale.US, "%.1f", selected.length)} dtk"
                    } else if (selectedAudio != null) {
                        "Terpilih: ${selectedAudio.name} • ${String.format(Locale.US, "%.1f", selectedAudio.durationSec)} dtk"
                    } else if (selectedOverlay != null) {
                        if (selectedOverlay.kind == OverlayKind.Effect) {
                            "Terpilih: Efek ${FxLayers.nameOf(selectedOverlay.content)} • geser atau tarik tepinya di timeline"
                        } else {
                            val what = if (selectedOverlay.kind == OverlayKind.Text) "Teks" else "Stiker"
                            "Terpilih: $what ${selectedOverlay.content} • seret di pratinjau untuk memindah"
                        }
                    } else {
                        if (imported) "Ketuk klip untuk memilih" else "Belum ada video. Ketuk + Media."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                if (recording) {
                    NeonChip(
                        text = "Berhenti rekam",
                        selected = true,
                        onClick = stopRecording,
                    )
                } else {
                    NeonChip(
                        text = "Rasio ${EditorSession.aspect.label}",
                        selected = true,
                        onClick = { panel = EditorPanel.Ratio },
                    )
                }
                Spacer(Modifier.width(8.dp))
                GradientButton(
                    text = "Media",
                    icon = Icons.Rounded.Add,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    height = 34.dp,
                    horizontalPadding = 14.dp,
                )
            }

            // Timeline multi-layer
            TimelineView(
                clips = clips + overlayTimeline + audioTimeline + effectTimeline,
                selectedId = selectedClip,
                onSelect = { id -> onSelectItem(if (id >= 1_000_000) id % 1_000_000 else id) },
                playhead = playhead,
                onSeek = { t ->
                    playhead = t.coerceIn(0f, total)
                    if (imported) exo.seekToGlobal(importedClips, playhead)
                },
                onDragStart = timelineDragStart,
                onDragBody = timelineDragBody,
                onDragEdge = timelineDragEdge,
                onDeleteItem = timelineDelete,
                totalSec = maxOf(total, EditorSession.audioEndSec, EditorSession.overlayEndSec, 10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )

            Spacer(Modifier.weight(1f))

            // Bilah alat
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EditorPanel.entries.filter { it != EditorPanel.Ratio }.forEach { p ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCard)
                            .border(1.dp, StrokeSoft, RoundedCornerShape(14.dp))
                            .clickable { panel = p }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(p.icon, p.label, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text(p.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }

    }

        // Pratinjau layar penuh (menutupi seluruh layar, termasuk area bilah sistem)
        if (fullscreen && imported) {
            FullscreenPlayer(
                exo = exo,
                ratio = EditorSession.aspect.ratio,
                fill = EditorSession.fillFrame,
                isPlaying = isPlaying,
                playhead = playhead,
                total = total,
                overlays = EditorSession.overlays,
                overlayTime = overlayTime,
                onTogglePlay = togglePlay,
                onSeek = { sec ->
                    playhead = sec
                    exo.seekToGlobal(importedClips, sec)
                },
                onClose = { fullscreen = false },
            )
        }
    }

    val current = panel
    if (current != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            sheetState = sheetState,
            containerColor = Navy,
            contentColor = TextPrimary,
            scrimColor = Color.Black.copy(alpha = 0.4f),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(current.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        current.label,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconCircleButton(Icons.Rounded.Close, { panel = null }, size = 36.dp)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (current) {
                        EditorPanel.Trim -> TrimPanel(onSplit, onDelete, onDuplicate, { onMove(-1) }, { onMove(1) })
                        EditorPanel.Music -> MusicPanel(
                            audioClips = EditorSession.audioClips,
                            selectedAudio = selectedAudio,
                            recording = recording,
                            recordSeconds = recordSeconds,
                            onAddMusic = { audioPicker.launch(arrayOf("audio/*")) },
                            onToggleRecord = onToggleRecord,
                            onSelect = { selectedClip = it },
                            onVolume = { v ->
                                selectedAudio?.let { EditorSession.setAudioVolume(it.id, v) }
                            },
                            onMoveToPlayhead = {
                                selectedAudio?.let { EditorSession.moveAudio(it.id, playhead) }
                            },
                            onSplit = {
                                val a = selectedAudio
                                if (a == null) {
                                    toast("Pilih musik dulu di timeline")
                                } else if (!EditorSession.splitAudio(a.id, playhead)) {
                                    toast("Geser garis putih ke dalam musik terpilih, lalu coba lagi")
                                }
                            },
                            onDelete = {
                                selectedAudio?.let { EditorSession.deleteAudio(it.id) }
                                selectedClip = EditorSession.clips.firstOrNull()?.id ?: -1
                            },
                        )
                        EditorPanel.Teks -> TextPanel(
                            editing = selectedOverlay?.takeIf { it.kind == OverlayKind.Text },
                            totalSec = total,
                            playhead = playhead,
                            onAdd = overlayAdd,
                            onChange = overlayChange,
                            onDuplicate = overlayDuplicate,
                            onDelete = overlayDelete,
                            onNew = { selectedClip = -1 },
                        )
                        EditorPanel.Effects -> EffectsPanel(
                            clip = selectedClipData,
                            clipLabel = "Klip ${importedClips.indexOfFirst { it.id == selectedClipData?.id } + 1}",
                            fxOverlay = selectedOverlay?.takeIf { it.kind == OverlayKind.Effect },
                            onMotion = { m ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, true) { it.copy(motion = m) }
                                }
                            },
                            onMotionStrength = { v ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, false) { it.copy(motionStrength = v) }
                                }
                            },
                            onAddFx = fxAdd,
                            onFxIntensity = { v ->
                                selectedOverlay?.takeIf { it.kind == OverlayKind.Effect }?.let { o ->
                                    EditorSession.updateOverlay(o.id) { it.copy(size = (v * 100f).coerceIn(10f, 100f)) }
                                }
                            },
                            onFilter = { f ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, true) { it.copy(filter = f) }
                                }
                            },
                            onStrength = { v ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, false) { it.copy(filterStrength = v) }
                                }
                            },
                            onApplyAll = {
                                selectedClipData?.let { src ->
                                    EditorSession.clips.toList().forEach { c ->
                                        EditorSession.updateClip(c.id, false) {
                                            it.copy(filter = src.filter, filterStrength = src.filterStrength)
                                        }
                                    }
                                    toast("Filter diterapkan ke semua klip")
                                }
                            },
                        )
                        EditorPanel.Transition -> TransitionPanel(
                            clips = importedClips.toList(),
                            selectedId = selectedClipData?.id ?: -1,
                            onType = { t ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, true) { it.copy(transition = t) }
                                }
                            },
                            onDuration = { d ->
                                selectedClipData?.let { c ->
                                    EditorSession.updateClip(c.id, false) { it.copy(transitionDur = d) }
                                }
                            },
                        )
                        EditorPanel.Sticker -> StickerPanel(
                            editing = selectedOverlay?.takeIf { it.kind == OverlayKind.Sticker },
                            totalSec = total,
                            playhead = playhead,
                            onAdd = overlayAdd,
                            onChange = overlayChange,
                            onDuplicate = overlayDuplicate,
                            onDelete = overlayDelete,
                            onNew = { selectedClip = -1 },
                        )
                        EditorPanel.Ratio -> RatioPanel()
                    }
                }
            }
        }
    }
}

@Composable
private fun TrimPanel(
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    val tools = listOf(
        Triple("Belah", Icons.Rounded.ContentCut, onSplit),
        Triple("Hapus", Icons.Rounded.Delete, onDelete),
        Triple("Duplikat", Icons.Rounded.ContentCopy, onDuplicate),
        Triple("Geser kiri", Icons.AutoMirrored.Rounded.ArrowBack, onMoveLeft),
        Triple("Geser kanan", Icons.AutoMirrored.Rounded.ArrowForward, onMoveRight),
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Pilih klip di timeline. Untuk Belah, geser garis putih ke titik potong di dalam klip.",
            color = TextSecondary,
            fontSize = 13.sp,
        )
        SimpleGrid(tools, columns = 3) { (label, icon, action) ->
            ToolTile(icon, label, selected = false, onClick = action, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RatioPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val out = EditorSession.outputSize()
        Text(
            "Pilih rasio sesuai tujuan unggahan. Hasil ekspor: ${out.first} × ${out.second} px.",
            color = TextSecondary,
            fontSize = 13.sp,
        )
        EditorSession.aspects.forEachIndexed { index, opt ->
            val isSel = index == EditorSession.aspectIndex
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(NavyCardHigh.copy(alpha = 0.9f))
                    .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else StrokeSoft, shape)
                    .clickable {
                        EditorSession.aspectIndex = index
                        EditorSession.fillFrame = true
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    val r = opt.ratio
                    val mw = if (r >= 1f) 32.dp else 32.dp * r
                    val mh = if (r >= 1f) 32.dp / r else 32.dp
                    Box(
                        Modifier
                            .size(mw, mh)
                            .border(2.dp, if (isSel) Color.White else TextSecondary, RoundedCornerShape(4.dp)),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${opt.label}  ${opt.title}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(opt.platforms, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Text("Cara bingkai video", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonChip("Pas (semua terlihat)", !EditorSession.fillFrame, { EditorSession.fillFrame = false })
            NeonChip("Isi (zoom, dipotong)", EditorSession.fillFrame, { EditorSession.fillFrame = true })
        }
    }
}
