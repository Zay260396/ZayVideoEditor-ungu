package com.zay.videoeditor.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Ikon mahkota (header Beranda dan tab Template di konsep resmi). Warna diatur lewat tint pada Icon. */
val CrownIcon: ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 18f)
        lineTo(2f, 7f)
        lineTo(7.5f, 12f)
        lineTo(12f, 4f)
        lineTo(16.5f, 12f)
        lineTo(22f, 7f)
        lineTo(21f, 18f)
        close()
        moveTo(3f, 20f)
        lineTo(21f, 20f)
        lineTo(21f, 22f)
        lineTo(3f, 22f)
        close()
    }
}.build()
