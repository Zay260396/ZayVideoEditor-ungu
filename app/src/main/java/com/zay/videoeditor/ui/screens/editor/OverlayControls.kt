package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.data.OverlayStyle
import com.zay.videoeditor.overlay.OverlayRenderer
import com.zay.videoeditor.ui.components.LabelText
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.components.NeonSlider
import com.zay.videoeditor.ui.theme.TextPrimary
import java.util.Locale

/**
 * Lapisan teks/stiker di atas pratinjau video. Menggambar lewat [OverlayRenderer], kode yang sama dengan ekspor.
 * [time] dibaca saat menggambar (bukan saat komposisi) supaya animasi mulus tanpa menyusun ulang layar.
 * Bila [dragEnabled], seret di mana saja pada bingkai memindahkan overlay terpilih lewat [onDrag]
 * (perubahan dalam pecahan lebar/tinggi bingkai).
 */
@Composable
fun OverlayLayer(
    overlays: List<Overlay>,
    time: () -> Float,
    selectedId: Int,
    modifier: Modifier = Modifier,
    dragEnabled: Boolean = false,
    onDrag: (Float, Float) -> Unit = { _, _ -> },
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    Canvas(
        modifier.pointerInput(dragEnabled) {
            if (dragEnabled) {
                detectDragGestures { change, amount ->
                    change.consume()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w > 0f && h > 0f) currentOnDrag(amount.x / w, amount.y / h)
                }
            }
        },
    ) {
        val t = time()
        val w = size.width.toInt()
        val h = size.height.toInt()
        val list = overlays.toList()
        if (w > 0 && h > 0 && list.isNotEmpty()) {
            val stroke = 2.dp.toPx()
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                OverlayRenderer.draw(native, w, h, list, t)
                val selected = list.firstOrNull { it.id == selectedId }
                if (selected != null && selected.isActiveAt(t)) {
                    OverlayRenderer.drawSelectionBox(native, w, h, selected, stroke)
                }
            }
        }
    }
}

/** Pilihan animasi masuk (Tanpa, Fade, Naik, Zoom, Ketik, Pantul), dua baris tiga chip. */
@Composable
fun AnimChips(anim: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OverlayStyle.animNames.chunked(3).forEachIndexed { row, names ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, name ->
                    val index = row * 3 + i
                    NeonChip(name, anim == index, { onSelect(index) })
                }
            }
        }
    }
}

/** Kontrol yang sama untuk teks dan stiker terpilih: durasi, putar, posisi cepat, mulai di garis putih, duplikat, hapus. */
@Composable
fun OverlayEditControls(
    overlay: Overlay,
    totalSec: Float,
    playhead: Float,
    onChange: (Overlay) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val maxDur = maxOf(OverlayStyle.MIN_DURATION + 0.5f, totalSec - overlay.startSec)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelText(
            "Tampil mulai ${one(overlay.startSec)} dtk, selama ${one(overlay.durationSec)} dtk",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelText("Durasi", Modifier.width(64.dp))
            NeonSlider(
                overlay.durationSec.coerceIn(OverlayStyle.MIN_DURATION, maxDur),
                { onChange(overlay.copy(durationSec = it)) },
                Modifier.weight(1f),
                valueRange = OverlayStyle.MIN_DURATION..maxDur,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelText("Putar", Modifier.width(64.dp))
            NeonSlider(
                overlay.rotation.coerceIn(-180f, 180f),
                { onChange(overlay.copy(rotation = it)) },
                Modifier.weight(1f),
                valueRange = -180f..180f,
            )
            Text(
                "${overlay.rotation.toInt()}°",
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End,
            )
        }
        Text(
            "Seret teks/stiker di pratinjau untuk memindahkannya, atau pakai posisi cepat:",
            color = com.zay.videoeditor.ui.theme.TextSecondary,
            fontSize = 12.sp,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonChip("Atas", false, { onChange(overlay.copy(x = 0.5f, y = 0.15f)) })
            NeonChip("Tengah", false, { onChange(overlay.copy(x = 0.5f, y = 0.5f)) })
            NeonChip("Bawah", false, { onChange(overlay.copy(x = 0.5f, y = 0.85f)) })
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonChip(
                "Mulai di garis putih",
                false,
                { onChange(overlay.copy(startSec = playhead.coerceIn(0f, maxOf(0f, totalSec - OverlayStyle.MIN_DURATION)))) },
            )
            NeonChip("Duplikat", false, onDuplicate)
            NeonChip("Hapus", false, onDelete)
        }
    }
}

private fun one(sec: Float): String = String.format(Locale.US, "%.1f", sec)
