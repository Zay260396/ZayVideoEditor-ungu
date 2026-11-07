package com.zay.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zay.videoeditor.ui.theme.NavyCard
import com.zay.videoeditor.ui.theme.StrokeSoft
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

/** Cache kecil thumbnail (kunci = path + detik) supaya daftar proyek tidak membaca video berulang kali. */
private val thumbCache = object : android.util.LruCache<String, android.graphics.Bitmap>(24) {}

private fun loadThumb(path: String, sec: Float): android.graphics.Bitmap? {
    val key = "$path@$sec"
    thumbCache.get(key)?.let { return it }
    val r = android.media.MediaMetadataRetriever()
    return try {
        r.setDataSource(path)
        val frame = r.getFrameAtTime((sec * 1_000_000f).toLong(), android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        val maxSide = 480
        val scale = maxSide.toFloat() / maxOf(frame.width, frame.height)
        val bmp = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                frame,
                (frame.width * scale).toInt().coerceAtLeast(1),
                (frame.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            frame
        }
        thumbCache.put(key, bmp)
        bmp
    } catch (e: Exception) {
        null
    } finally {
        try {
            r.release()
        } catch (e: Exception) {
            // abaikan
        }
    }
}

/** Thumbnail proyek: frame video asli bila tersedia, kalau tidak gradasi seperti sebelumnya. */
@Composable
fun ProjectThumb(item: ProjectItem, modifier: Modifier = Modifier, showDuration: Boolean = true) {
    val thumb: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, item.thumbPath, item.thumbSec) {
        val path = item.thumbPath
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { loadThumb(path, item.thumbSec)?.asImageBitmap() }
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(item.colors)),
        contentAlignment = Alignment.Center,
    ) {
        val img = thumb
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(Icons.Rounded.Image, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        }
        if (showDuration) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Kartu proyek untuk grid 2 kolom (halaman Proyek). */
@Composable
fun ProjectCard(item: ProjectItem, onClick: () -> Unit, modifier: Modifier = Modifier, onMore: () -> Unit = {}) {
    GlassCard(modifier, onClick, contentPadding = 8.dp) {
        ProjectThumb(item, Modifier.fillMaxWidth().height(110.dp), showDuration = false)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${item.duration} • ${item.resolution}", color = TextSecondary, fontSize = 11.sp)
            }
            Icon(
                Icons.Rounded.MoreVert,
                null,
                tint = TextSecondary,
                modifier = Modifier.size(30.dp).clickable(onClick = onMore).padding(6.dp),
            )
        }
    }
}

/** Thumbnail kecil dengan judul untuk baris "Proyek Saya" di Beranda. */
@Composable
fun ProjectMini(item: ProjectItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProjectThumb(item, Modifier.fillMaxWidth().height(78.dp))
        Text(
            "${item.name}.mp4",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

