package com.zay.videoeditor.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
import com.zay.videoeditor.ui.components.zayFieldColors
import com.zay.videoeditor.ui.theme.NavyCard
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NeonAmber
import com.zay.videoeditor.ui.theme.NeonBlue
import com.zay.videoeditor.ui.theme.NeonMint
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

// Urutan harus sama dengan OverlayStyle.fontFamilies: serif, sans-serif, monospace, cursive.
private val composeFonts = listOf(
    FontFamily.Serif,
    FontFamily.SansSerif,
    FontFamily.Monospace,
    FontFamily.Cursive,
)

private val colorChoices = listOf(
    Color.White,
    Color.Black,
    NeonPurple,
    NeonPink,
    NeonBlue,
    NeonMint,
    NeonAmber,
)

/**
 * Panel Teks. Dua mode:
 * - [editing] == null: mode tambah. Tombol "Tambahkan ke Video" memanggil [onAdd] dengan teks baru
 *   (id, waktu mulai, dan durasi diisi oleh pemanggil).
 * - [editing] != null: mode edit teks terpilih. Setiap perubahan langsung dikirim lewat [onChange].
 */
@Composable
fun TextPanel(
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
    var tab by rememberSaveable { mutableStateOf(0) }
    var text by rememberSaveable(key) { mutableStateOf(editing?.content ?: "ZAY VIDEO EDITOR") }
    var fontIndex by rememberSaveable(key) { mutableStateOf(editing?.font ?: 1) }
    var colorIndex by rememberSaveable(key) {
        mutableStateOf(
            editing?.let { e -> colorChoices.indexOfFirst { it.toArgb() == e.color }.coerceAtLeast(0) } ?: 0,
        )
    }
    var animIndex by rememberSaveable(key) { mutableStateOf(editing?.anim ?: 0) }
    var bold by rememberSaveable(key) { mutableStateOf(editing?.bold ?: true) }
    var italic by rememberSaveable(key) { mutableStateOf(editing?.italic ?: false) }
    var textSize by rememberSaveable(key) { mutableStateOf(editing?.size ?: 48f) }

    /** Overlay dari pilihan sekarang. [base] = teks yang sedang diedit (mempertahankan posisi dan waktu). */
    fun build(base: Overlay?): Overlay {
        val style = base ?: Overlay(
            id = 0,
            kind = OverlayKind.Text,
            content = "",
            startSec = 0f,
            durationSec = OverlayStyle.DEFAULT_DURATION,
            y = 0.5f,
        )
        return style.copy(
            content = text,
            size = textSize,
            color = colorChoices[colorIndex].toArgb(),
            font = fontIndex,
            bold = bold,
            italic = italic,
            anim = animIndex,
        )
    }

    // Di mode edit, setiap perubahan langsung diterapkan ke teks terpilih.
    fun push() {
        if (editing != null) onChange(build(editing))
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (editing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mengedit teks terpilih",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                NeonChip("+ Teks baru", false, onNew)
            }
        }

        // Pratinjau teks (menggunakan pilihan font, warna, gaya, dan ukuran)
        Box(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NavyCardHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text.ifBlank { "Teks" },
                color = colorChoices[colorIndex],
                fontFamily = composeFonts[fontIndex],
                fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                fontSize = (textSize * 0.6f).sp,
                lineHeight = (textSize * 0.72f).sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                push()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teks") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = zayFieldColors(),
        )

        PanelTabs(listOf("Font", "Warna", "Animasi", "Gaya"), tab) { tab = it }

        when (tab) {
            0 -> SimpleGrid(composeFonts.indices.toList(), columns = 4, spacing = 8.dp) { i ->
                val selected = fontIndex == i
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(NavyCardHigh.copy(alpha = 0.9f))
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, BrandGradient, shape)
                            } else {
                                Modifier.border(1.dp, StrokeSoft, shape)
                            },
                        )
                        .clickable {
                            fontIndex = i
                            push()
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Aa", color = TextPrimary, fontSize = 22.sp, fontFamily = composeFonts[i], fontWeight = FontWeight.Bold)
                    Text(OverlayStyle.fontNames[i], color = TextSecondary, fontSize = 11.sp)
                }
            }

            1 -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                colorChoices.forEachIndexed { i, c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (colorIndex == i) {
                                    Modifier.border(3.dp, BrandGradient, CircleShape)
                                } else {
                                    Modifier.border(1.dp, StrokeSoft, CircleShape)
                                },
                            )
                            .clickable {
                                colorIndex = i
                                push()
                            },
                    )
                }
            }

            2 -> AnimChips(animIndex) {
                animIndex = it
                push()
            }

            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StyleToggle(Icons.Rounded.FormatBold, bold) {
                    bold = !bold
                    push()
                }
                StyleToggle(Icons.Rounded.FormatItalic, italic) {
                    italic = !italic
                    push()
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ukuran Teks", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(88.dp))
            NeonSlider(
                textSize,
                {
                    textSize = it
                    push()
                },
                Modifier.weight(1f),
                valueRange = OverlayStyle.TEXT_SIZE_MIN..OverlayStyle.TEXT_SIZE_MAX,
            )
            Text("${textSize.toInt()}", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
        }

        if (editing == null) {
            LabelText("Teks muncul di posisi garis putih selama 3 detik. Durasi dan posisi bisa diubah setelah ditambahkan.")
            GradientButton(
                text = "Tambahkan ke Video",
                onClick = { if (text.isNotBlank()) onAdd(build(null)) },
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

@Composable
private fun StyleToggle(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(BrandGradient)
                } else {
                    Modifier.background(NavyCard).border(1.dp, StrokeSoft, shape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
