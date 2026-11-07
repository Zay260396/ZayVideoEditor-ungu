package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.fx.ClipFx
import com.zay.videoeditor.overlay.FxLayers
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.NeonSlider
import com.zay.videoeditor.ui.components.PanelTabs
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.components.ToolTile
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

// Urutan sama dengan ClipFx.motionNames
private val motionIcons: List<ImageVector> = listOf(
    Icons.Rounded.Block,
    Icons.Rounded.Vibration,
    Icons.Rounded.Favorite,
    Icons.Rounded.ZoomIn,
    Icons.Rounded.ZoomOut,
    Icons.Rounded.RotateRight,
    Icons.Rounded.Cloud,
    Icons.Rounded.Flip,
)

/**
 * Panel Efek (TAHAP 5c).
 * - Filter: 25 filter warna per klip (pratinjau dan ekspor).
 * - Overlay: 11 efek lapisan (salju, hujan, bintang, dll.) yang bisa dipindah dan dipanjangkan di timeline.
 * - Animasi: 7 gerakan klip (getar, denyut, zoom, goyang, melayang, cermin).
 */
@Composable
fun EffectsPanel(
    clip: ImportedClip?,
    clipLabel: String,
    fxOverlay: Overlay?,
    onFilter: (Int) -> Unit,
    onStrength: (Float) -> Unit,
    onApplyAll: () -> Unit,
    onMotion: (Int) -> Unit,
    onMotionStrength: (Float) -> Unit,
    onAddFx: (String) -> Unit,
    onFxIntensity: (Float) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Filter", "Overlay", "Animasi")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PanelTabs(tabs, tab) { tab = it }

        when (tab) {
            0 -> FilterTab(clip, clipLabel, onFilter, onStrength, onApplyAll)
            1 -> OverlayTab(fxOverlay, onAddFx, onFxIntensity)
            else -> MotionTab(clip, clipLabel, onMotion, onMotionStrength)
        }
    }
}

@Composable
private fun FilterTab(
    clip: ImportedClip?,
    clipLabel: String,
    onFilter: (Int) -> Unit,
    onStrength: (Float) -> Unit,
    onApplyAll: () -> Unit,
) {
    if (clip == null) {
        Text("Tambahkan video dulu lewat tombol + Media.", color = TextSecondary, fontSize = 13.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Filter untuk $clipLabel", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        SimpleGrid(ClipFx.filters.indices.toList(), columns = 4) { i ->
            val item = ClipFx.filters[i]
            val isSelected = clip.filter == i
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.clickable { onFilter(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(item.top, item.bottom)))
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, BrandGradient, shape)
                            } else {
                                Modifier.border(1.dp, StrokeSoft, shape)
                            },
                        ),
                ) {}
                Text(
                    item.name,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        if (clip.filter > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Atur Intensitas", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(104.dp))
                NeonSlider(clip.filterStrength, onStrength, Modifier.weight(1f))
                Text("${(clip.filterStrength * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(44.dp))
            }
        }

        GradientButton(
            text = "Terapkan ke Semua Klip",
            onClick = onApplyAll,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OverlayTab(
    fxOverlay: Overlay?,
    onAddFx: (String) -> Unit,
    onFxIntensity: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            if (fxOverlay == null) {
                "Ketuk efek untuk menambahkannya di posisi garis putih. Geser atau tarik tepinya di timeline untuk mengatur waktu dan panjangnya."
            } else {
                "Efek terpilih: ${FxLayers.nameOf(fxOverlay.content)}. Ketuk efek lain untuk menggantinya, atau tekan X di timeline untuk menghapus."
            },
            color = TextSecondary,
            fontSize = 12.sp,
        )

        SimpleGrid(FxLayers.items.indices.toList(), columns = 4) { i ->
            val item = FxLayers.items[i]
            val isSelected = fxOverlay?.content == item.key
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.clickable { onAddFx(item.key) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(Color(item.top), Color(item.bottom))))
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, BrandGradient, shape)
                            } else {
                                Modifier.border(1.dp, StrokeSoft, shape)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, fontSize = 24.sp)
                }
                Text(
                    item.name,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        if (fxOverlay != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Atur Intensitas", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(104.dp))
                NeonSlider(fxOverlay.size / 100f, onFxIntensity, Modifier.weight(1f))
                Text("${fxOverlay.size.toInt()}%", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(44.dp))
            }
        }
    }
}

@Composable
private fun MotionTab(
    clip: ImportedClip?,
    clipLabel: String,
    onMotion: (Int) -> Unit,
    onMotionStrength: (Float) -> Unit,
) {
    if (clip == null) {
        Text("Tambahkan video dulu lewat tombol + Media.", color = TextSecondary, fontSize = 13.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Gerakan untuk $clipLabel", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        SimpleGrid(ClipFx.motionNames.indices.toList(), columns = 4) { i ->
            ToolTile(
                icon = motionIcons[i],
                label = ClipFx.motionNames[i],
                selected = clip.motion == i,
                onClick = { onMotion(i) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (clip.motion > 0 && clip.motion != 7) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kekuatan", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(104.dp))
                NeonSlider(clip.motionStrength, onMotionStrength, Modifier.weight(1f))
                Text("${(clip.motionStrength * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(44.dp))
            }
        }
    }
}
