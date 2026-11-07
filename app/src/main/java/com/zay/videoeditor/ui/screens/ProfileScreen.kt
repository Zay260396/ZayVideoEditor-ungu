package com.zay.videoeditor.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.components.BrandGradient
import com.zay.videoeditor.ui.components.FullGradient
import com.zay.videoeditor.ui.components.GlassCard
import com.zay.videoeditor.ui.components.showSoon
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zay_prefs", Context.MODE_PRIVATE) }
    var autoSave by remember { mutableStateOf(prefs.getBoolean("auto_save", true)) }
    var hqPreview by remember { mutableStateOf(prefs.getBoolean("hq_preview", false)) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profil & Pengaturan", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FullGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Kreator ZAY", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Akun lokal di perangkat ini", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BrandGradient)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("Gratis • Tanpa watermark", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        SettingsGroup("Umum") {
            SettingsRow(Icons.Rounded.Language, "Bahasa", "Indonesia") { showSoon(context, "Tahap 7") }
            SettingsRow(Icons.Rounded.Palette, "Tema", "Neon Gelap", showChevron = false) {}
            SettingsRow(Icons.Rounded.Videocam, "Kualitas ekspor bawaan", "1080p") { showSoon(context, "Tahap 6") }
        }

        SettingsGroup("Editor") {
            SettingsSwitchRow(Icons.Rounded.Star, "Simpan otomatis", "Proyek disimpan saat berpindah halaman", autoSave) {
                autoSave = it
                prefs.edit().putBoolean("auto_save", it).apply()
            }
            SettingsSwitchRow(Icons.Rounded.Videocam, "Pratinjau kualitas tinggi", "Lebih tajam, lebih berat di HP lama", hqPreview) {
                hqPreview = it
                prefs.edit().putBoolean("hq_preview", it).apply()
            }
        }

        SettingsGroup("Penyimpanan") {
            SettingsRow(Icons.Rounded.Storage, "Folder hasil ekspor", "Movies/ZAY") { showSoon(context, "Tahap 6") }
            SettingsRow(Icons.Rounded.Storage, "Hapus cache", "Bersihkan file sementara") {
                val ok = try {
                    context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                    true
                } catch (e: Exception) {
                    false
                }
                Toast.makeText(
                    context,
                    if (ok) "Cache dibersihkan. Proyek dan video Anda aman." else "Gagal membersihkan cache",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        SettingsGroup("Tentang") {
            SettingsRow(Icons.Rounded.Info, "Versi aplikasi", "0.11.3 (Alat tanpa kotak)", showChevron = false) {}
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NavyCardHigh.copy(alpha = 0.6f))
                .border(1.dp, StrokeSoft, shape),
        ) { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (showChevron) {
            Icon(Icons.Rounded.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NeonPink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = NavyCardHigh,
            ),
        )
    }
}
