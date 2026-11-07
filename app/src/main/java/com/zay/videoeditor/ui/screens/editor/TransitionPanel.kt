package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.fx.ClipFx
import com.zay.videoeditor.ui.components.NeonSlider
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.components.ToolTile
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import java.util.Locale

// Urutan sama dengan ClipFx.transitionNames
private val transitionIcons: List<ImageVector> = listOf(
    Icons.Rounded.Block,
    Icons.Rounded.BlurOn,
    Icons.Rounded.FlashOn,
    Icons.Rounded.SwapHoriz,
    Icons.Rounded.ZoomIn,
    Icons.Rounded.Autorenew,
)

/** Panel Transisi (TAHAP 5c): transisi dipasang antara klip terpilih dan klip sesudahnya. */
@Composable
fun TransitionPanel(
    clips: List<ImportedClip>,
    selectedId: Int,
    onType: (Int) -> Unit,
    onDuration: (Float) -> Unit,
) {
    val idx = clips.indexOfFirst { it.id == selectedId }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when {
            idx < 0 -> {
                Text("Pilih klip video di timeline, lalu pilih transisi.", color = TextSecondary, fontSize = 13.sp)
            }
            idx >= clips.lastIndex -> {
                Text(
                    "Klip terakhir tidak punya klip sesudahnya. Pilih klip sebelumnya untuk memasang transisi.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
            else -> {
                val clip = clips[idx]
                Text(
                    "Transisi antara Klip ${idx + 1} dan Klip ${idx + 2}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                SimpleGrid(ClipFx.transitionNames.indices.toList(), columns = 4) { i ->
                    ToolTile(
                        icon = transitionIcons[i],
                        label = ClipFx.transitionNames[i],
                        selected = clip.transition == i,
                        onClick = { onType(i) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (clip.transition != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Durasi", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp))
                        NeonSlider(clip.transitionDur, onDuration, Modifier.weight(1f), valueRange = 0.2f..2f)
                        Text(
                            String.format(Locale.US, "%.1f dtk", clip.transitionDur),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.width(60.dp),
                        )
                    }
                    Text(
                        "Pratinjau menampilkan versi sederhana (memudar). Hasil ekspor memakai gerakan penuh.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
