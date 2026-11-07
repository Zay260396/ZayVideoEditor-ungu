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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.data.OverlayKind
import com.zay.videoeditor.data.OverlayStyle
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.LabelText
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.components.NeonSlider
import com.zay.videoeditor.ui.components.PanelTabs
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

private val categories = listOf("Populer", "Emoji", "Alam", "Dekorasi")

// ️ memaksa tampilan emoji berwarna untuk karakter yang bisa tampil sebagai simbol hitam-putih.
private val stickers = listOf(
    listOf("😀", "😍", "🔥", "💜", "✨", "🎉", "👍", "💯", "🌈", "⭐", "😎", "🥳"),
    listOf("😂", "🥰", "😭", "😡", "🤯", "😴", "🤔", "🙌", "👏", "🙏", "💪", "👀"),
    listOf("🌸", "🌙", "☀️", "🌊", "🍀", "🦋", "🌺", "⚡", "❄️", "🌴", "🍃", "🌟"),
    listOf("💖", "💫", "🎈", "🎀", "🎵", "🎬", "📸", "🏆", "👑", "💎", "🎶", "🎁"),
)

/**
 * Panel Stiker. Dua mode seperti [TextPanel]:
 * - [editing] == null: pilih stiker lalu "Tambah Stiker" ([onAdd]; id, waktu mulai, dan durasi diisi pemanggil).
 * - [editing] != null: stiker terpilih; mengetuk emoji lain menggantinya, perubahan langsung lewat [onChange].
 */
@Composable
fun StickerPanel(
    editing: Overlay?,
    totalSec: Float,
    playhead: Float,
    onAdd: (Overlay) -> Unit,
    onChange: (Overlay) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onNew: () -> Unit,
) {
    val key = editing?.id ?: 0
    val startCategory = editing?.let { e -> stickers.indexOfFirst { e.content in it } }?.takeIf { it >= 0 } ?: 0
    var category by rememberSaveable(key) { mutableStateOf(startCategory) }
    var emoji by rememberSaveable(key) { mutableStateOf(editing?.content ?: stickers[0][0]) }
    var size by rememberSaveable(key) { mutableStateOf(editing?.size ?: 160f) }
    var animIndex by rememberSaveable(key) { mutableStateOf(editing?.anim ?: 0) }

    fun build(base: Overlay?): Overlay {
        val style = base ?: Overlay(
            id = 0,
            kind = OverlayKind.Sticker,
            content = emoji,
            startSec = 0f,
            durationSec = OverlayStyle.DEFAULT_DURATION,
        )
        return style.copy(content = emoji, size = size, anim = animIndex)
    }

    fun push() {
        if (editing != null) onChange(build(editing))
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (editing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mengedit stiker terpilih",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                NeonChip("+ Stiker baru", false, onNew)
            }
        }

        PanelTabs(categories, category) { category = it }

        SimpleGrid(stickers[category].indices.toList(), columns = 6, spacing = 8.dp) { i ->
            val value = stickers[category][i]
            val isSelected = emoji == value
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(NavyCardHigh.copy(alpha = 0.9f))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, BrandGradient, shape)
                        } else {
                            Modifier.border(1.dp, StrokeSoft, shape)
                        },
                    )
                    .clickable {
                        emoji = value
                        push()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(value, fontSize = 26.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ukuran", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp))
            NeonSlider(
                size,
                {
                    size = it
                    push()
                },
                Modifier.weight(1f),
                valueRange = OverlayStyle.STICKER_SIZE_MIN..OverlayStyle.STICKER_SIZE_MAX,
            )
            Text("${size.toInt()}", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        }

        LabelText("Animasi masuk")
        AnimChips(animIndex) {
            animIndex = it
            push()
        }

        if (editing == null) {
            LabelText("Stiker muncul di tengah pada posisi garis putih selama 3 detik. Seret di pratinjau untuk memindahkannya.")
            GradientButton(
                text = "Tambah Stiker",
                onClick = { onAdd(build(null)) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OverlayEditControls(
                overlay = editing,
                totalSec = totalSec,
                playhead = playhead,
                onChange = onChange,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
            )
        }
    }
}
