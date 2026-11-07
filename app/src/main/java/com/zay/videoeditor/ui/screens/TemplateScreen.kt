package com.zay.videoeditor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.components.PanelTabs
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.components.showSoon
import com.zay.videoeditor.ui.theme.Platinum
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

private data class TemplateItem(val title: String, val tag: String, val colors: List<Color>, val height: Dp)

private val templates = listOf(
    TemplateItem("Vlog Harian", "Vlog", listOf(Color(0xFF4AA3FF), Color(0xFF10265E)), 200.dp),
    TemplateItem("Beat Neon", "Musik", listOf(Color(0xFF7A8CFF), Color(0xFF1A1F4D)), 176.dp),
    TemplateItem("Promosi Produk", "Bisnis", listOf(Color(0xFF9FB4D6), Color(0xFF2A3A5C)), 200.dp),
    TemplateItem("Liburan Cepat", "Vlog", listOf(Color(0xFF3FC7D9), Color(0xFF123A6B)), 176.dp),
    TemplateItem("Lirik Estetik", "Musik", listOf(Color(0xFF5B6FE0), Color(0xFF150F3A)), 200.dp),
    TemplateItem("Slide Katalog", "Bisnis", listOf(Color(0xFF6FA0E8), Color(0xFF0E1A38)), 176.dp),
)

@Composable
fun TemplateScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tabs = listOf("Semua", "Vlog", "Musik", "Bisnis")
    var tab by rememberSaveable { mutableStateOf(0) }
    val shown = templates.filter { tab == 0 || it.tag == tabs[tab] }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        Text("Template", color = TextPrimary, fontFamily = FontFamily.Serif, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text("Mulai lebih cepat dengan kerangka siap pakai", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        PanelTabs(tabs, tab) { tab = it }
        Spacer(Modifier.height(16.dp))

        // ===== Banner unggulan =====
        val bannerShape = RoundedCornerShape(26.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(bannerShape)
                .background(Color(0xFF0B1226))
                .border(1.dp, Platinum.copy(alpha = 0.3f), bannerShape),
        ) {
            Image(
                painter = painterResource(id = com.zay.videoeditor.R.drawable.zay_hero_blue),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(200.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(0f to Color.Transparent, 0.6f to Color.Black),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
            Column(Modifier.padding(start = 20.dp, top = 22.dp, bottom = 18.dp).fillMaxHeight()) {
                Text("PILIHAN UNGGULAN", color = Platinum, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        append("Cinematic\n")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Platinum)) { append("Moments") }
                    },
                    color = TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDCE6F7), Color(0xFF9FB4D6))))
                        .clickable { showSoon(context, "Tahap 5") }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "GUNAKAN",
                        color = Color(0xFF0A1020),
                        fontSize = 11.5.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        null,
                        tint = Color(0xFF0A1020),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ===== Kartu template =====
        SimpleGrid(shown, columns = 2, spacing = 12.dp) { item ->
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(item.height)
                    .clip(shape)
                    .background(Brush.linearGradient(item.colors))
                    .border(1.dp, Platinum.copy(alpha = 0.25f), shape)
                    .clickable { showSoon(context, "Tahap 5") },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color(0xD905070F),
                            ),
                        ),
                )
                Text(
                    item.tag.uppercase(),
                    color = Platinum,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x8C05070F))
                        .border(1.dp, Platinum.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x8005070F))
                        .border(1.dp, Platinum, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Platinum, modifier = Modifier.size(20.dp))
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        null,
                        tint = Platinum,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
