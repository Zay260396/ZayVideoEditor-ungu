package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.DemoTimeline
import com.zay.videoeditor.data.TimelineClip
import com.zay.videoeditor.ui.theme.NavyCard
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.sin

/**
 * Timeline multi-layer: penggaris waktu, 4 lapisan (Video, Teks, Musik, Efek),
 * klip yang bisa dipilih, dan playhead yang bisa digeser dengan mengetuk.
 * Data klip masih contoh; dihubungkan ke video sungguhan pada TAHAP 3-4.
 */
@Composable
fun TimelineView(
    clips: List<TimelineClip>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    playhead: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    dpPerSec: Float = 34f,
    totalSec: Float = DemoTimeline.TOTAL_SECONDS,
    /** Mulai menyeret item [id] (dipakai untuk mencatat undo). */
    onDragStart: (Int) -> Unit = {},
    /** Geser seluruh item [id] sebesar delta detik (teks, stiker, musik). */
    onDragBody: (Int, Float) -> Unit = { _, _ -> },
    /** Tarik tepi item: (id, tepiKiri, delta detik). Video, teks, stiker. */
    onDragEdge: (Int, Boolean, Float) -> Unit = { _, _, _ -> },
    /** Hapus item [id] lewat tombol X di atas item yang terpilih. */
    onDeleteItem: (Int) -> Unit = {},
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val startCb = rememberUpdatedState(onDragStart)
    val bodyCb = rememberUpdatedState(onDragBody)
    val edgeCb = rememberUpdatedState(onDragEdge)
    val seekCb = rememberUpdatedState(onSeek)
    val playheadNow = rememberUpdatedState(playhead)
    val pxPerSecNow = with(density) { dpPerSec.dp.toPx() }
    val tracks = DemoTimeline.tracks
    val rulerH = 26.dp
    val trackH = 42.dp
    val shape = RoundedCornerShape(20.dp)

    // Item yang waktunya bertumpuk di satu lapisan ditaruh di baris (lane) berbeda, maksimal 3 baris per lapisan.
    val lanes = assignLanes(clips)
    val laneCounts = tracks.indices.map { ti ->
        (clips.filter { it.track == ti }.maxOfOrNull { lanes[it.id] ?: 0 } ?: 0) + 1
    }
    val trackTops = tracks.indices.map { ti -> trackH * laneCounts.take(ti).sum() }
    val bodyH = trackH * laneCounts.sum()

    Row(
        modifier
            .clip(shape)
            .background(NavyCard.copy(alpha = 0.7f))
            .border(1.dp, StrokeSoft, shape),
    ) {
        // Kolom ikon lapisan
        Column(Modifier.width(44.dp)) {
            Spacer(Modifier.height(rulerH))
            tracks.forEachIndexed { ti, t ->
                Box(Modifier.height(trackH * laneCounts[ti]).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(t.icon, t.name, tint = t.colors.first(), modifier = Modifier.size(20.dp))
                }
            }
        }

        Box(Modifier.weight(1f).horizontalScroll(scroll)) {
            Box(
                Modifier
                    .width((totalSec * dpPerSec).dp + 24.dp)
                    .height(rulerH + bodyH)
                    .pointerInput(dpPerSec, totalSec) {
                        detectTapGestures { offset ->
                            val pxPerSec = with(density) { dpPerSec.dp.toPx() }
                            onSeek((offset.x / pxPerSec).coerceIn(0f, totalSec))
                        }
                    },
            ) {
                // Penggaris waktu
                (0..totalSec.toInt() step 5).forEach { s ->
                    Text(
                        "${s}s",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .offset(x = (s * dpPerSec).dp + 3.dp, y = 6.dp),
                    )
                }

                // Klip
                clips.forEach { item ->
                    val track = tracks[item.track]
                    val selected = item.id == selectedId
                    val clipShape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .offset(
                                x = (item.start * dpPerSec).dp,
                                y = rulerH + trackTops[item.track] + trackH * (lanes[item.id] ?: 0) + 3.dp,
                            )
                            .width((item.length * dpPerSec).dp)
                            .height(trackH - 6.dp)
                            .clip(clipShape)
                            .background(Brush.horizontalGradient(track.colors))
                            .then(
                                if (selected) Modifier.border(2.dp, Color.White, clipShape) else Modifier,
                            )
                            .clickable { onSelect(item.id) }
                            .then(
                                if (selected && (item.track == 1 || item.track == 2 || (item.track == 3 && item.id < 1_000_000))) {
                                    Modifier.pointerInput(item.id, pxPerSecNow) {
                                        detectDragGestures(
                                            onDragStart = { startCb.value(item.id) },
                                        ) { change, drag ->
                                            change.consume()
                                            bodyCb.value(item.id, drag.x / pxPerSecNow)
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (track.isAudio) {
                            Canvas(Modifier.matchParentSize()) {
                                val barW = 3.dp.toPx()
                                val gap = 3.dp.toPx()
                                var x = 0f
                                var i = 0
                                while (x < size.width) {
                                    val h = (0.25f + 0.6f * abs(sin(i * 0.7f))) * size.height
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.35f),
                                        topLeft = Offset(x, (size.height - h) / 2f),
                                        size = Size(barW, h),
                                        cornerRadius = CornerRadius(barW / 2f),
                                    )
                                    x += barW + gap
                                    i++
                                }
                            }
                        }
                        Text(
                            item.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Item terpilih: pegangan tepi (perpanjang/perpendek) dan tombol X untuk menghapus
                clips.firstOrNull { it.id == selectedId && (it.track <= 2 || it.id < 1_000_000) }?.let { item ->
                    val itemTop = rulerH + trackTops[item.track] + trackH * (lanes[item.id] ?: 0) + 3.dp
                    val itemH = trackH - 6.dp
                    if (item.track == 0 || item.track == 1 || item.track == 2 || item.track == 3) {
                        listOf(true, false).forEach { isLeft ->
                            Box(
                                Modifier
                                    .offset(
                                        x = if (isLeft) (item.start * dpPerSec).dp - 2.dp else ((item.start + item.length) * dpPerSec).dp - 14.dp,
                                        y = itemTop,
                                    )
                                    .width(16.dp)
                                    .height(itemH)
                                    .pointerInput(item.id, isLeft, pxPerSecNow) {
                                        detectDragGestures(
                                            onDragStart = { startCb.value(item.id) },
                                        ) { change, drag ->
                                            change.consume()
                                            edgeCb.value(item.id, isLeft, drag.x / pxPerSecNow)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .width(10.dp)
                                        .height(itemH)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .width(2.dp)
                                            .height(16.dp)
                                            .background(Color(0xFF2B1B6E)),
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        Modifier
                            .offset(
                                x = ((item.start + item.length / 2f) * dpPerSec).dp - 13.dp,
                                y = itemTop - 11.dp,
                            )
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(NeonPink)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { onDeleteItem(item.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, "Hapus", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Geser di penggaris waktu untuk menggeser garis putih (scrub)
                Box(
                    Modifier
                        .width((totalSec * dpPerSec).dp + 24.dp)
                        .height(rulerH)
                        .pointerInput(pxPerSecNow, totalSec) {
                            detectDragGestures(
                                onDragStart = { o -> seekCb.value((o.x / pxPerSecNow).coerceIn(0f, totalSec)) },
                            ) { change, _ ->
                                change.consume()
                                seekCb.value((change.position.x / pxPerSecNow).coerceIn(0f, totalSec))
                            }
                        },
                )

                // Playhead
                Box(
                    Modifier
                        .offset(x = (playhead * dpPerSec).dp - 1.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White),
                )
                // Pegangan lebar di sekitar garis putih: tahan lalu geser kiri/kanan
                Box(
                    Modifier
                        .offset(x = (playhead * dpPerSec).dp - 14.dp)
                        .width(28.dp)
                        .fillMaxHeight()
                        .pointerInput(pxPerSecNow, totalSec) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                seekCb.value((playheadNow.value + drag.x / pxPerSecNow).coerceIn(0f, totalSec))
                            }
                        },
                )
                Box(
                    Modifier
                        .offset(x = (playhead * dpPerSec).dp - 6.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(NeonPink),
                )
            }
        }
    }
}

/** Nomor baris (0..2) tiap item di lapisannya: item yang waktunya bertumpuk dipindah ke baris berikutnya. */
private fun assignLanes(clips: List<TimelineClip>): Map<Int, Int> {
    val result = HashMap<Int, Int>()
    for ((_, group) in clips.groupBy { it.track }) {
        val laneEnds = ArrayList<Float>()
        for (item in group.sortedBy { it.start }) {
            var lane = laneEnds.indexOfFirst { it <= item.start + 0.001f }
            if (lane < 0) {
                laneEnds.add(item.start + item.length)
                lane = laneEnds.size - 1
            } else {
                laneEnds[lane] = item.start + item.length
            }
            result[item.id] = minOf(lane, 2)
        }
    }
    return result
}
