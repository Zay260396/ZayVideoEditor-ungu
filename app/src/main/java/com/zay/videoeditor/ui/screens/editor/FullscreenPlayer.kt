package com.zay.videoeditor.ui.screens.editor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.R
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.IconCircleButton
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun clock(sec: Float): String {
    val s = sec.toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

/**
 * Pratinjau layar penuh: menyembunyikan bilah sistem, memutar ke lanskap untuk rasio lebar,
 * dan memakai ExoPlayer yang sama dengan editor ([exo]). Ketuk layar untuk menampilkan/menyembunyikan kontrol.
 * Tombol kembali atau ikon keluar menutupnya.
 */
@Composable
fun FullscreenPlayer(
    exo: ExoPlayer,
    ratio: Float,
    fill: Boolean,
    isPlaying: Boolean,
    playhead: Float,
    total: Float,
    overlays: List<Overlay>,
    overlayTime: () -> Float,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val filterKey = remember { FilterCache() }
    val hostView = LocalView.current
    var showControls by remember { mutableStateOf(true) }

    BackHandler(onBack = onClose)

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, hostView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        if (activity != null && ratio >= 1f) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previousOrientation
        }
    }

    // Sembunyikan kontrol otomatis setelah 3 detik saat video diputar.
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    val safeTotal = maxOf(total, 0.1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val boxRatio = maxWidth / maxHeight
            val frameW = if (boxRatio > ratio) maxHeight * ratio else maxWidth
            val frameH = if (boxRatio > ratio) maxHeight else maxWidth / ratio
            Box(
                Modifier
                    .size(frameW, frameH)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showControls = !showControls },
            ) {
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.zay_player_view, null, false) as PlayerView)
                            .also { it.player = exo }
                    },
                    modifier = Modifier.matchParentSize(),
                    update = { view ->
                        if (view.player !== exo) view.player = exo
                        // 0 = pas (seluruh video terlihat), 4 = isi (dipotong memenuhi bingkai)
                        view.resizeMode = if (fill) 4 else 0
                        val pc = EditorSession.clips.getOrNull(clipIndexAt(EditorSession.clips, playhead).first)
                        view.applyClipFilter(pc?.filter ?: 0, pc?.filterStrength ?: 0f, filterKey)
                    },
                )
                TransitionDip(playhead, Modifier.matchParentSize())
                // Teks dan stiker, digambar oleh renderer yang sama dengan pratinjau dan ekspor.
                OverlayLayer(
                    overlays = overlays,
                    time = overlayTime,
                    selectedId = -1,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        if (showControls) {
            // Tombol keluar di kanan atas
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp),
            ) {
                IconCircleButton(Icons.Rounded.FullscreenExit, onClose, tint = Color.White)
            }

            // Bilah kontrol bawah
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(clock(playhead), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = playhead.coerceIn(0f, safeTotal),
                    onValueChange = onSeek,
                    valueRange = 0f..safeTotal,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = NeonPurple,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text(clock(total), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
