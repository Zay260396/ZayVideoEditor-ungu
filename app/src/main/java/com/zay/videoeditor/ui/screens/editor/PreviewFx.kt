package com.zay.videoeditor.ui.screens.editor

import android.graphics.Paint
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.ui.PlayerView
import com.zay.videoeditor.data.EditorSession
import com.zay.videoeditor.data.ImportedClip
import com.zay.videoeditor.fx.ClipFx

/** Indeks klip dan detik lokal di dalam klip itu untuk detik [sec] pada timeline gabungan. */
fun clipIndexAt(clips: List<ImportedClip>, sec: Float): Pair<Int, Float> {
    if (clips.isEmpty()) return 0 to 0f
    var acc = 0f
    for (i in clips.indices) {
        val end = acc + clips[i].durationSec
        if (sec < end || i == clips.lastIndex) {
            return i to (sec - acc).coerceIn(0f, clips[i].durationSec)
        }
        acc = end
    }
    return 0 to 0f
}

/** Catatan filter terakhir yang dipasang ke pratinjau; dibuat ulang bila TextureView berganti. */
class FilterCache {
    var key: Int = 0
    var view: TextureView? = null
}

/**
 * Pasang filter warna klip ke pratinjau (TextureView pemutar). [cache] menyimpan filter terakhir
 * supaya lapisan tidak dibuat ulang terus-menerus.
 */
fun PlayerView.applyClipFilter(filter: Int, strength: Float, cache: FilterCache) {
    val tv = videoSurfaceView as? TextureView ?: return
    if (cache.view !== tv) {
        cache.view = tv
        cache.key = -1
    }
    val key = if (filter <= 0) 0 else filter * 1000 + (strength * 100f).toInt() + 1
    if (key == cache.key) return
    cache.key = key
    if (filter <= 0) {
        tv.setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint()
        paint.colorFilter = ClipFx.previewFilter(filter, strength)
        tv.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
}

/** Lapisan gelap atau terang di atas video saat berada di sambungan transisi (pratinjau sederhana). */
@Composable
fun TransitionDip(timeSec: Float, modifier: Modifier = Modifier) {
    val clips = EditorSession.clips
    val (i, t) = clipIndexAt(clips, timeSec)
    val dip = ClipFx.previewDip(clips, i, t) ?: return
    Box(modifier.background(dip.first.copy(alpha = dip.second)))
}
