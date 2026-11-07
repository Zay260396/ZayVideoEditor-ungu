package com.zay.videoeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZayColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    secondary = NeonBlue,
    onSecondary = Color.White,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
    surfaceVariant = NavyCardHigh,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSoft,
)

@Composable
fun ZayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZayColorScheme,
        typography = ZayTypography,
        content = content,
    )
}
