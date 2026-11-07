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
import androidx.compose.material.icons.outlined.ContentCut as OutlinedContentCut
import androidx.compose.material.icons.outlined.Image as OutlinedImage
import androidx.compose.material.icons.outlined.Layers as OutlinedLayers
import androidx.compose.material.icons.outlined.MusicNote as OutlinedMusicNote
import androidx.compose.material.icons.outlined.Star as OutlinedStar
import androidx.compose.material.icons.outlined.TextFields as OutlinedTextFields
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.ProjectStore
import com.zay.videoeditor.ui.components.ProjectMini
import com.zay.videoeditor.ui.components.ProjectThumb
import com.zay.videoeditor.ui.components.ZayLogo
import com.zay.videoeditor.ui.theme.Platinum
import com.zay.videoeditor.ui.theme.PlatinumDeep
import com.zay.videoeditor.ui.theme.SapphireBlue
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

private data class HomeTool(val label: String, val icon: ImageVector)

private val homeTools = listOf(
    HomeTool("Potong", Icons.Outlined.OutlinedContentCut),
    HomeTool("Musik", Icons.Outlined.OutlinedMusicNote),
    HomeTool("Teks", Icons.Outlined.OutlinedTextFields),
    HomeTool("Efek", Icons.Outlined.OutlinedStar),
    HomeTool("Transisi", Icons.Outlined.OutlinedLayers),
    HomeTool("Stiker", Icons.Outlined.OutlinedImage),
)

@Composable
fun HomeScreen(
    onNewProject: () -> Unit,
    onOpenProject: (Int) -> Unit,
    onSeeAll: () -> Unit,
    onSettings: () -> Unit,
    onTemplate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ===== Hero: foto biru memudar ke latar, tanpa bingkai =====
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            Image(
                painter = painterResource(id = com.zay.videoeditor.R.drawable.zay_hero_blue),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(300.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(0f to Color.Transparent, 0.6f to Color.Black),
                            blendMode = BlendMode.DstIn,
                        )
                        drawRect(
                            brush = Brush.verticalGradient(0.5f to Color.Black, 0.98f to Color.Transparent),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ZayLogo(34.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ZAY",
                            color = TextPrimary,
                            fontFamily = FontFamily.Serif,
                            fontSize = 19.sp,
                            letterSpacing = 7.sp,
                        )
                        Text(
                            "VIDEO STUDIO",
                            color = Platinum,
                            fontSize = 8.sp,
                            letterSpacing = 4.sp,
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x8C05070F))
                            .border(1.dp, Platinum.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = Platinum, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(28.dp).height(1.dp).background(Platinum))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "CREATE · EDIT · INSPIRE",
                        color = Platinum,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    buildAnnotatedString {
                        append("Turn your moments\ninto ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Platinum)) {
                            append("masterpieces")
                        }
                    },
                    color = TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontSize = 30.sp,
                    lineHeight = 37.sp,
                )
            }
        }

        // ===== Dua tombol utama: Video Baru (platinum) + Template (garis platinum) =====
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val pill = RoundedCornerShape(29.dp)
            Row(
                Modifier
                    .weight(1.3f)
                    .height(58.dp)
                    .shadow(16.dp, pill, ambientColor = SapphireBlue, spotColor = SapphireBlue)
                    .clip(pill)
                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDCE6F7), Color(0xFF9FB4D6))))
                    .clickable(onClick = onNewProject),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color(0xFF0A1020), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "VIDEO BARU",
                    color = Color(0xFF0A1020),
                    fontSize = 12.5.sp,
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(pill)
                    .background(Color(0xE60E1320))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Platinum, Platinum.copy(alpha = 0.15f), PlatinumDeep)),
                        pill,
                    )
                    .clickable(onClick = onTemplate),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Dashboard, null, tint = Platinum, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "TEMPLATE",
                    color = Platinum,
                    fontSize = 12.5.sp,
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ===== Lanjutkan mengedit (proyek terakhir) =====
        val last = ProjectStore.projects.firstOrNull()
        if (last != null) {
            val cardShape = RoundedCornerShape(22.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(listOf(Platinum.copy(alpha = 0.10f), Color.White.copy(alpha = 0.025f))),
                    )
                    .border(1.dp, Platinum.copy(alpha = 0.22f), cardShape)
                    .clickable { onOpenProject(last.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.width(92.dp).height(66.dp), contentAlignment = Alignment.Center) {
                    ProjectThumb(last, Modifier.fillMaxSize(), showDuration = false)
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x8C05070F))
                            .border(1.dp, Platinum, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Platinum, modifier = Modifier.size(16.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("LANJUTKAN MENGEDIT", color = Platinum, fontSize = 9.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        last.name,
                        color = TextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                    Text("${last.duration} • ${last.resolution}", color = TextSecondary, fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Platinum, modifier = Modifier.size(20.dp))
            }
        }

        // ===== Proyek Saya =====
        Column(
            Modifier.padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Proyek Saya",
                    color = TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "LIHAT SEMUA",
                    color = Platinum,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            val recent = ProjectStore.projects.take(3)
            if (recent.isEmpty()) {
                Text("Belum ada proyek. Ketuk Video Baru untuk mulai.", color = TextSecondary, fontSize = 13.sp)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recent.forEach { item ->
                        ProjectMini(item, onClick = { onOpenProject(item.id) }, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - recent.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ===== Alat: enam lingkaran bergaris platinum =====
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            homeTools.forEach { tool ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNewProject)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Platinum.copy(alpha = 0.08f))
                            .border(1.dp, Platinum.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tool.icon, null, tint = Platinum, modifier = Modifier.size(21.dp))
                    }
                    Text(
                        tool.label,
                        color = TextPrimary.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
