package com.zay.videoeditor.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.ZayBackground
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NeonBlue
import com.zay.videoeditor.ui.theme.NeonOrange
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val colors: List<Color>,
)

private val pages = listOf(
    OnboardPage(
        Icons.Rounded.ContentCut,
        "Edit Video Lengkap",
        "Potong, gabungkan, dan atur klip dengan timeline multi-layer yang mudah dipakai di HP.",
        listOf(NeonPurple, NeonBlue),
    ),
    OnboardPage(
        Icons.Rounded.AutoAwesome,
        "Efek, Teks & Musik",
        "Tambahkan filter, transisi, stiker, teks beranimasi, dan musik favoritmu.",
        listOf(NeonPink, NeonPurple),
    ),
    OnboardPage(
        Icons.Rounded.Layers,
        "Ekspor Tanpa Watermark",
        "Simpan hasil editmu dalam MP4 720p, 1080p, hingga 4K. Gratis, tanpa watermark.",
        listOf(NeonOrange, NeonPink),
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.size - 1

    ZayBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone) {
                    Text("Lewati", color = TextSecondary)
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                OnboardPageContent(pages[index])
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .height(8.dp)
                            .size(width = width, height = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(listOf(NeonPurple, NeonPink))
                                } else {
                                    Brush.horizontalGradient(listOf(NavyCardHigh, NavyCardHigh))
                                },
                            ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            GradientButton(
                text = if (isLast) "Mulai Sekarang" else "Lanjut",
                onClick = {
                    if (isLast) {
                        onDone()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Box(
                Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(page.colors.first().copy(alpha = 0.45f), Color.Transparent),
                        ),
                    ),
            )
            Box(
                Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(Brush.linearGradient(page.colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(page.icon, null, tint = Color.White, modifier = Modifier.size(76.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            page.title,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            page.body,
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
