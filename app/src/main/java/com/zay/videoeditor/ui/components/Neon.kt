package com.zay.videoeditor.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.theme.Navy
import com.zay.videoeditor.ui.theme.NavyCard
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NavyDeep
import com.zay.videoeditor.ui.theme.NeonBlue
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF5B9BFF), Color(0xFF2B4FD9)))
val CoolGradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple))
val FullGradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple, NeonPink))

/** Pemberitahuan jujur untuk fitur yang belum dibangun pada tahap saat ini. */
fun showSoon(context: Context, stage: String) {
    Toast.makeText(context, "Fitur ini aktif di $stage", Toast.LENGTH_SHORT).show()
}

@Composable
fun ZayBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyDeep)))
            .drawBehind {
                val c1 = Offset(size.width * 0.1f, size.height * 0.05f)
                val r1 = size.width * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF3A6BD8).copy(alpha = 0.30f), Color.Transparent),
                        center = c1,
                        radius = r1,
                    ),
                    radius = r1,
                    center = c1,
                )
                val c2 = Offset(size.width * 0.95f, size.height * 0.9f)
                val r2 = size.width * 0.7f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2B5FD9).copy(alpha = 0.18f), Color.Transparent),
                        center = c2,
                        radius = r2,
                    ),
                    radius = r2,
                    center = c2,
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            content = content,
        )
    }
}

@Composable
fun ZayLogo(size: Dp, modifier: Modifier = Modifier) {
    // Logo ZAY asli dari gambar konsep (latar transparan)
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.zay.videoeditor.R.drawable.zay_logo),
        contentDescription = "ZAY",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    brush: Brush = BrandGradient,
    height: Dp = 52.dp,
    horizontalPadding: Dp = 24.dp,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .height(height)
            .shadow(10.dp, shape, ambientColor = NeonPurple, spotColor = NeonPurple)
            .clip(shape)
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (height < 46.dp) 14.sp else 16.sp)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    corner: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    val borderBrush = Brush.linearGradient(
        listOf(Color(0xFFBFD2F2).copy(alpha = 0.45f), Color(0xFF4A8BFF).copy(alpha = 0.15f), Color(0xFFBFD2F2).copy(alpha = 0.3f)),
    )
    val base = modifier
        .clip(shape)
        .background(NavyCard.copy(alpha = 0.85f))
        .border(1.dp, borderBrush, shape)
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(clickable.padding(contentPadding), content = content)
}

@Composable
fun NeonChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val styled = if (selected) {
        modifier.clip(shape).background(BrandGradient)
    } else {
        modifier.clip(shape).background(NavyCardHigh).border(1.dp, StrokeSoft, shape)
    }
    Box(styled.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PanelTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { i, t -> NeonChip(t, selected == i, { onSelect(i) }) }
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    size: Dp = 40.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(NavyCardHigh.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Ubin alat berisi ikon dan label. Dipakai untuk panel Potong dan Transisi. */
@Composable
fun ToolTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val outline = if (selected) {
        Modifier.border(2.dp, BrandGradient, shape)
    } else {
        Modifier.border(1.dp, StrokeSoft, shape)
    }
    Column(
        modifier
            .clip(shape)
            .background(NavyCardHigh.copy(alpha = 0.9f))
            .then(outline)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (selected) BrandGradient else Brush.linearGradient(listOf(NavyCard, NavyCard))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun LabelText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun NeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = NeonPurple,
            inactiveTrackColor = NavyCardHigh,
        ),
    )
}

@Composable
fun <T> SimpleGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    cell: @Composable (T) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { item ->
                    Box(Modifier.weight(1f)) { cell(item) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun zayFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonPurple,
    unfocusedBorderColor = StrokeSoft,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NeonPink,
    focusedLabelColor = NeonPurple,
    unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary,
    focusedLeadingIconColor = TextSecondary,
    unfocusedLeadingIconColor = TextSecondary,
)
