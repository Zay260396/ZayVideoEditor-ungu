package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.AudioClip
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.LabelText
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import java.util.Locale

private fun fmt(sec: Float): String {
    val total = sec.toInt()
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

private val volumeSteps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/** Panel audio: tambah musik dari file, rekam suara, atur volume, geser, hapus. */
@Composable
fun MusicPanel(
    audioClips: List<AudioClip>,
    selectedAudio: AudioClip?,
    recording: Boolean,
    recordSeconds: Float,
    onAddMusic: () -> Unit,
    onToggleRecord: () -> Unit,
    onSelect: (Int) -> Unit,
    onVolume: (Float) -> Unit,
    onMoveToPlayhead: () -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Musik dan suara ditaruh di posisi garis putih. Geser garis putih dulu ke titik yang diinginkan.",
            color = TextSecondary,
            fontSize = 13.sp,
        )

        GradientButton(
            text = "Tambah Musik dari File",
            icon = Icons.Rounded.MusicNote,
            onClick = onAddMusic,
            modifier = Modifier.fillMaxWidth(),
        )

        GradientButton(
            text = if (recording) "Berhenti Merekam  ${fmt(recordSeconds)}" else "Rekam Suara",
            icon = if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
            onClick = onToggleRecord,
            modifier = Modifier.fillMaxWidth(),
            brush = if (recording) {
                Brush.linearGradient(listOf(Color(0xFFFF4D4D), Color(0xFFFF4DC4)))
            } else {
                Brush.linearGradient(listOf(Color(0xFF2E9BFF), Color(0xFF9D4DFF)))
            },
        )
        Text(
            "Saat merekam, video ikut diputar. Pakai headset supaya suara video tidak terekam.",
            color = TextSecondary,
            fontSize = 12.sp,
        )

        LabelText("Audio di proyek (${audioClips.size})")
        if (audioClips.isEmpty()) {
            Text("Belum ada musik atau suara.", color = TextSecondary, fontSize = 13.sp)
        }
        audioClips.forEach { clip ->
            val isSel = selectedAudio?.id == clip.id
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(NavyCardHigh.copy(alpha = 0.9f))
                    .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else StrokeSoft, shape)
                    .clickable { onSelect(clip.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (clip.isVoice) Icons.Rounded.Mic else Icons.Rounded.MusicNote,
                    null,
                    tint = Color.White,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        clip.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        "Mulai ${fmt(clip.startSec)} • ${fmt(clip.durationSec)} • Volume ${(clip.volume * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (selectedAudio != null) {
            LabelText("Volume")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                volumeSteps.forEach { v ->
                    val label = if (v == 0f) "Bisu" else "${(v * 100).toInt()}%"
                    NeonChip(label, selectedAudio.volume == v, { onVolume(v) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonChip("Belah", false, onSplit)
                NeonChip("Pindah ke garis putih", false, onMoveToPlayhead)
                NeonChip("Hapus", false, onDelete)
            }
        }
    }
}
