package com.zay.videoeditor.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.DemoTimeline
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.export.ExportConfig
import com.zay.videoeditor.export.ExportManager
import com.zay.videoeditor.export.ExportState
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.IconCircleButton
import com.zay.videoeditor.ui.components.LabelText
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.components.SimpleGrid
import com.zay.videoeditor.ui.components.ZayBackground
import com.zay.videoeditor.ui.components.showSoon
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NeonAmber
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary
import java.util.Locale

private data class ResOption(val label: String, val size: String, val baseMbps: Float, val badge: String?)

private val resOptions = listOf(
    ResOption("720p", "1280×720", 5f, null),
    ResOption("1080p", "1920×1080", 10f, "Disarankan"),
    ResOption("4K", "3840×2160", 35f, "Ultra"),
    // Ditambah di belakang agar indeks proyek lama tetap cocok; urutan tampil diatur EditorSession.resolutionOrder.
    ResOption("2K", "2560×1440", 18f, "Tajam"),
    ResOption("480p", "854×480", 2.5f, "Hemat"),
)

@Composable
fun ExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zay_prefs", Context.MODE_PRIVATE) }
    var saveToGallery by remember { mutableStateOf(prefs.getBoolean("save_to_gallery", true)) }
    var fpsIndex by rememberSaveable { mutableStateOf(1) }
    var qualityIndex by rememberSaveable { mutableStateOf(1) }

    val fpsOptions = listOf(24, 30, 60)
    val qualityLabels = listOf("Hemat", "Seimbang", "Tinggi")
    val qualityFactors = listOf(0.7f, 1f, 1.4f)

    val res = resOptions[EditorSession.resolutionIndex]
    val outSizeNow = EditorSession.outputSize()
    val refPixels = when (EditorSession.resolutionIndex) {
        0 -> 1280f * 720f
        2 -> 3840f * 2160f
        3 -> 2560f * 1440f
        4 -> 854f * 480f
        else -> 1920f * 1080f
    }
    val pixelFactor = (outSizeNow.first.toFloat() * outSizeNow.second.toFloat()) / refPixels
    val mbps = res.baseMbps * qualityFactors[qualityIndex] * pixelFactor
    val bitrate = (mbps * 1_000_000f).toInt().coerceIn(1_000_000, 60_000_000)

    val exportState = ExportManager.state
    val running = exportState is ExportState.Running

    // Layar tetap menyala selama ekspor; buang hasil lama saat layar dibuka.
    val hostView = LocalView.current
    DisposableEffect(running) {
        hostView.keepScreenOn = running
        onDispose { hostView.keepScreenOn = false }
    }
    LaunchedEffect(Unit) { ExportManager.dismiss() }
    BackHandler(enabled = running) {
        Toast.makeText(context, "Ekspor sedang berjalan. Tekan Batal untuk menghentikan.", Toast.LENGTH_SHORT).show()
    }
    val totalSec = if (EditorSession.clips.isEmpty()) DemoTimeline.TOTAL_SECONDS else EditorSession.totalSeconds
    val estimatedMb = mbps * totalSec / 8f
    val estimateText = String.format(Locale.forLanguageTag("id-ID"), "%.1f MB", estimatedMb)

    ZayBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, {
                    if (running) {
                        Toast.makeText(context, "Ekspor sedang berjalan. Tekan Batal untuk menghentikan.", Toast.LENGTH_SHORT).show()
                    } else {
                        onBack()
                    }
                })
                Text(
                    "Ekspor",
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(40.dp))
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val outSize = EditorSession.outputSize()
                Text(
                    "Rasio ${EditorSession.aspect.label} • ${outSize.first} × ${outSize.second} px • ${EditorSession.aspect.platforms}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                // Pratinjau proyek (gradasi pengganti; frame sungguhan di TAHAP 3)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2B1B6E), Color(0xFF8A2BE2), Color(0xFFFF7A59)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Text("Pilih Resolusi", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                SimpleGrid(EditorSession.resolutionOrder, columns = 3, spacing = 10.dp) { index ->
                    run {
                        val option = resOptions[index]
                        val selected = EditorSession.resolutionIndex == index
                        val shape = RoundedCornerShape(18.dp)
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
                                .clickable { EditorSession.resolutionIndex = index }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(option.label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(option.size, color = TextSecondary, fontSize = 11.sp)
                            if (option.badge != null) {
                                Text(
                                    option.badge,
                                    color = if (option.label == "4K") NeonAmber else NeonPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                if (res.label == "4K") {
                    Text(
                        "4K hanya berhasil jika HP mendukung. Bila tidak, ukuran hasil diturunkan otomatis oleh encoder HP.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }

                LabelText("Frame rate")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fpsOptions.forEachIndexed { i, fps ->
                        NeonChip("$fps fps", fpsIndex == i, { fpsIndex = i })
                    }
                }

                Text(
                    "Frame rate hasil mengikuti video sumber. Pilihan fps akan diterapkan di versi berikutnya.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )

                LabelText("Kualitas")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualityLabels.forEachIndexed { i, label ->
                        NeonChip(label, qualityIndex == i, { qualityIndex = i })
                    }
                }

                // Simpan ke Galeri
                val rowShape = RoundedCornerShape(18.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(NavyCardHigh.copy(alpha = 0.6f))
                        .border(1.dp, StrokeSoft, rowShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Simpan ke Galeri", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Perkiraan ukuran: $estimateText", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = saveToGallery,
                        onCheckedChange = {
                            saveToGallery = it
                            prefs.edit().putBoolean("save_to_gallery", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = NavyCardHigh,
                        ),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Check, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    Text("MP4 • tanpa watermark", color = TextSecondary, fontSize = 12.sp)
                }
                Text(
                    "Hasil memuat video, suara asli klip, musik, dan rekaman suara sesuai pengaturan di editor.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }

            when (val st = exportState) {
                is ExportState.Running -> ExportProgressCard(st, onCancel = { ExportManager.cancel() })
                is ExportState.Done -> ExportDoneCard(
                    st,
                    onPlay = { openVideo(context, st.uri) },
                    onShare = { shareVideo(context, st.uri) },
                    onClose = { ExportManager.dismiss() },
                )
                is ExportState.Failed -> ExportFailedCard(
                    st.message,
                    onRetry = { ExportManager.dismiss() },
                )
                ExportState.Idle -> GradientButton(
                    text = "Export Video 🚀",
                    onClick = {
                        if (EditorSession.clips.isEmpty()) {
                            Toast.makeText(context, "Tambahkan video dulu di editor", Toast.LENGTH_SHORT).show()
                        } else {
                            ExportManager.start(context, ExportConfig(bitrate, saveToGallery))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kartu status ekspor

@Composable
private fun ExportCardShell(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(shape)
            .background(NavyCardHigh.copy(alpha = 0.95f))
            .border(1.dp, StrokeSoft, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun OutlineAction(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(NavyCardHigh)
            .border(1.dp, StrokeSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExportProgressCard(state: ExportState.Running, onCancel: () -> Unit) {
    ExportCardShell {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.stage,
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${(state.progress * 100f).toInt().coerceIn(0, 100)}%",
                color = NeonPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        LinearProgressIndicator(
            progress = { state.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = NeonPurple,
            trackColor = NavyCardHigh,
        )
        Text(
            "Jangan tutup aplikasi dan biarkan layar menyala sampai selesai.",
            color = TextSecondary,
            fontSize = 12.sp,
        )
        OutlineAction("Batal", null, onCancel, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExportDoneCard(state: ExportState.Done, onPlay: () -> Unit, onShare: () -> Unit, onClose: () -> Unit) {
    ExportCardShell {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Check, null, tint = NeonPurple, modifier = Modifier.size(22.dp))
            Text(
                "Ekspor selesai • " + String.format(Locale.forLanguageTag("id-ID"), "%.1f MB", state.sizeMb),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(state.note, color = TextSecondary, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineAction("Putar", Icons.Rounded.PlayArrow, onPlay, Modifier.weight(1f))
            OutlineAction("Bagikan", Icons.Rounded.Share, onShare, Modifier.weight(1f))
        }
        GradientButton(
            text = "Selesai",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            height = 46.dp,
        )
    }
}

@Composable
private fun ExportFailedCard(message: String, onRetry: () -> Unit) {
    ExportCardShell {
        Text("Ekspor gagal", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary, fontSize = 12.sp)
        GradientButton(
            text = "Coba lagi",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            height = 46.dp,
        )
    }
}

private fun openVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi pemutar video", Toast.LENGTH_SHORT).show()
    }
}

private fun shareVideo(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Bagikan video"))
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak bisa membagikan video", Toast.LENGTH_SHORT).show()
    }
}
