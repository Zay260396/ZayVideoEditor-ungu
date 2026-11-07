package com.zay.videoeditor.ui.screens.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.data.ProjectStore
import java.io.File

/** Pindah ke detik [sec] pada timeline gabungan (semua klip berurutan). */
fun Player.seekToGlobal(clips: List<ImportedClip>, sec: Float) {
    if (clips.isEmpty()) return
    var acc = 0f
    for (i in clips.indices) {
        val end = acc + clips[i].durationSec
        if (sec < end || i == clips.lastIndex) {
            val local = (sec - acc).coerceIn(0f, clips[i].durationSec)
            seekTo(i, (local * 1000f).toLong())
            return
        }
        acc = end
    }
}

/** Posisi pemutar saat ini pada timeline gabungan, dalam detik. */
fun Player.globalPositionSec(clips: List<ImportedClip>): Float {
    var acc = 0f
    val idx = minOf(currentMediaItemIndex, clips.size)
    for (i in 0 until idx) acc += clips[i].durationSec
    return acc + currentPosition / 1000f
}

/**
 * Salin video ke penyimpanan aplikasi (supaya proyek tetap utuh), lalu baca durasi dan nama file.
 * Mengembalikan null bila video tidak bisa dibaca. Panggil dari thread latar belakang.
 */
fun readImportedClip(context: Context, sourceUri: Uri): ImportedClip? {
    val name: String? = try {
        context.contentResolver
            .query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }
    val ext = name?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 } ?: "mp4"
    val dest = File(ProjectStore.mediaDir(context), "${System.currentTimeMillis()}_${EditorSession.nextId()}.$ext")
    try {
        val input = context.contentResolver.openInputStream(sourceUri) ?: return null
        input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
    } catch (e: Exception) {
        dest.delete()
        return null
    }
    val uri = Uri.fromFile(dest)

    val retriever = MediaMetadataRetriever()
    val seconds: Float = try {
        retriever.setDataSource(context, uri)
        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        ms / 1000f
    } catch (e: Exception) {
        0f
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            // abaikan
        }
    }
    if (seconds <= 0f) {
        dest.delete()
        return null
    }
    return ImportedClip(EditorSession.nextId(), uri, name ?: "Klip", seconds)
}

/** Item pemutar yang hanya memutar bagian klip yang dipakai (hasil potong/belah). */
fun ImportedClip.toMediaItem(): MediaItem {
    val clipping = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs((trimStartSec * 1000f).toLong())
        .setEndPositionMs(
            if (trimEndSec >= sourceDurationSec - 0.01f) C.TIME_END_OF_SOURCE else (trimEndSec * 1000f).toLong(),
        )
        .build()
    return MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clipping)
        .build()
}
