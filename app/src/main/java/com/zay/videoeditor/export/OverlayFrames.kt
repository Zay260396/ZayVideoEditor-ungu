package com.zay.videoeditor.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.overlay.OverlayRenderer

/**
 * Sumber gambar teks/stiker untuk ekspor: tiap frame video, Media3 meminta bitmap bingkai penuh
 * ([width] x [height] = ukuran video hasil) untuk waktu [presentationTimeUs] pada timeline gabungan.
 * Gambarnya dibuat oleh [OverlayRenderer], kode yang sama dengan pratinjau.
 *
 * Catatan teknis: BitmapOverlay hanya mengunggah ulang tekstur bila instance Bitmap berganti, jadi dua bitmap
 * dipakai bergantian (tanpa alokasi per frame) dan bitmap yang sama dikembalikan selama isi gambar tidak berubah.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class OverlayFrames(
    private val width: Int,
    private val height: Int,
    private val overlays: List<Overlay>,
) : BitmapOverlay() {

    private val buffers = arrayOfNulls<Bitmap>(2)
    private var nextBuffer = 0
    private var blank: Bitmap? = null
    private var last: Bitmap? = null
    private var lastKey: Int? = null

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val t = presentationTimeUs / 1_000_000f
        val key = OverlayRenderer.frameKey(overlays, t)
        val cached = last
        if (cached != null && key == lastKey) return cached

        val result: Bitmap
        if (key == null) {
            // Tidak ada teks/stiker di waktu ini: bitmap transparan 1x1 sudah cukup.
            val b = blank ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            blank = b
            result = b
        } else {
            val i = nextBuffer
            nextBuffer = 1 - nextBuffer
            val b = buffers[i] ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            buffers[i] = b
            b.eraseColor(Color.TRANSPARENT)
            OverlayRenderer.draw(Canvas(b), width, height, overlays, t)
            result = b
        }
        last = result
        lastKey = key
        return result
    }
}
