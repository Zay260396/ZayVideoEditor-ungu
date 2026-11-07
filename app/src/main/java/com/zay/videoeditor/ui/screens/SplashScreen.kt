package com.zay.videoeditor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.components.ZayBackground
import com.zay.videoeditor.ui.components.ZayLogo
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val finished by rememberUpdatedState(onFinished)

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow",
    )

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        launch { alpha.animateTo(1f, tween(700)) }
        delay(1900)
        finished()
    }

    ZayBackground {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(190.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonPurple.copy(alpha = 0.55f), NeonPink.copy(alpha = 0.15f), Color.Transparent),
                            ),
                        ),
                )
                ZayLogo(
                    96.dp,
                    Modifier
                        .scale(scale.value)
                        .alpha(alpha.value),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "ZAY",
                modifier = Modifier.alpha(alpha.value),
                color = TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
            )
            Text(
                "VIDEO EDITOR",
                modifier = Modifier.alpha(alpha.value),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Create • Edit • Inspire",
                modifier = Modifier.alpha(alpha.value),
                color = TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}
