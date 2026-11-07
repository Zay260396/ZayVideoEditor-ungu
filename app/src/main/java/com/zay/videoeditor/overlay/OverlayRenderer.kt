package com.zay.videoeditor.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.zay.videoeditor.data.Overlay
import com.zay.videoeditor.data.OverlayKind
import com.zay.videoeditor.data.OverlayStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Renderer teks dan stiker. SATU kode ini dipakai pratinjau (Compose Canvas) dan ekspor (Bitmap untuk
 * Media3 OverlayEffect), jadi hasil ekspor sama persis dengan yang terlihat di pratinjau.
 *
 * Semua ukuran dihitung dari sisi pendek bingkai ([REF_SHORT_SIDE] = 1080 px acuan), sehingga tampilan
 * sama pada resolusi berapa pun. Kanvas yang diberikan berukuran [w] x [h] = bingkai video penuh.
 */
object OverlayRenderer {
    const val REF_SHORT_SIDE = 1080f
    private const val INTRO_SEC = 0.5f
    private const val OUTRO_SEC = 0.4f
    private const val BOX_WIDTH_FRACTION = 0.9f

    private class Anim(val alpha: Float, val scale: Float, val dy: Float, val chars: Int)

    private val staticAnim = Anim(1f, 1f, 0f, -1)

    private val typefaces = HashMap<Int, Typeface>()

    // ------------------------------------------------------------------------------------------
    // Animasi

    private fun easeOut(p: Float): Float {
        val q = 1f - p
        return 1f - q * q * q
    }

    private fun bounce(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val a = x - 1.5f / d1
                n1 * a * a + 0.75f
            }
            x < 2.5f / d1 -> {
                val a = x - 2.25f / d1
                n1 * a * a + 0.9375f
            }
            else -> {
                val a = x - 2.625f / d1
                n1 * a * a + 0.984375f
            }
        }
    }

    private fun animOf(o: Overlay, t: Float): Anim {
        val local = max(0f, t - o.startSec)
        val remain = max(0f, o.endSec - t)
        val p = (local / INTRO_SEC).coerceIn(0f, 1f)
        return when (o.anim) {
            1 -> Anim(min(p, (remain / OUTRO_SEC).coerceIn(0f, 1f)), 1f, 0f, -1) // Fade
            2 -> Anim(easeOut(p), 1f, (1f - easeOut(p)) * 0.08f, -1) // Naik
            3 -> Anim(easeOut(p), 0.3f + 0.7f * easeOut(p), 0f, -1) // Zoom
            4 -> { // Ketik (stiker: memudar masuk)
                if (o.kind == OverlayKind.Text) {
                    val n = o.content.codePointCount(0, o.content.length)
                    val dur = (n * 0.08f).coerceIn(0.4f, 2f)
                    val k = ceil(n * (local / dur).coerceIn(0f, 1f)).toInt()
                    Anim(1f, 1f, 0f, k)
                } else {
                    Anim(easeOut(p), 1f, 0f, -1)
                }
            }
            5 -> Anim( // Pantul
                (local / 0.1f).coerceIn(0f, 1f),
                0.2f + 0.8f * bounce((local / 0.7f).coerceIn(0f, 1f)),
                0f,
                -1,
            )
            else -> staticAnim
        }
    }

    // ------------------------------------------------------------------------------------------
    // Ukuran dan font

    private fun typefaceOf(o: Overlay): Typeface {
        val style = when {
            o.bold && o.italic -> Typeface.BOLD_ITALIC
            o.bold -> Typeface.BOLD
            o.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val idx = o.font.coerceIn(0, OverlayStyle.fontFamilies.size - 1)
        val key = idx * 4 + style
        return synchronized(typefaces) {
            typefaces.getOrPut(key) { Typeface.create(OverlayStyle.fontFamilies[idx], style) }
        }
    }

    private fun shadowColorFor(color: Int): Int {
        val lum = (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
        return if (lum < 0.25f) Color.argb(150, 255, 255, 255) else Color.argb(150, 0, 0, 0)
    }

    private fun textLayout(o: Overlay, text: String, w: Int, h: Int): StaticLayout {
        val unit = min(w, h) / REF_SHORT_SIDE
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = max(1f, o.size * unit)
        paint.color = o.color
        paint.typeface = typefaceOf(o)
        val r = paint.textSize * 0.05f
        paint.setShadowLayer(r, 0f, r * 0.5f, shadowColorFor(o.color))
        val maxW = max(1, (w * BOX_WIDTH_FRACTION).toInt())
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, maxW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
    }

    private fun stickerPaint(o: Overlay, w: Int, h: Int): Paint {
        val unit = min(w, h) / REF_SHORT_SIDE
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = max(1f, o.size * unit)
        paint.textAlign = Paint.Align.CENTER
        return paint
    }

    /** Lebar dan tinggi (piksel bingkai) objek yang tampak penuh, tanpa animasi. */
    fun measure(o: Overlay, w: Int, h: Int): Pair<Float, Float> {
        if (o.kind == OverlayKind.Effect) return Pair(w.toFloat(), h.toFloat())
        if (o.kind == OverlayKind.Sticker) {
            val p = stickerPaint(o, w, h)
            val fm = p.fontMetrics
            return Pair(max(p.measureText(o.content), 1f), fm.descent - fm.ascent)
        }
        val layout = textLayout(o, o.content.ifEmpty { " " }, w, h)
        var widest = 0f
        for (i in 0 until layout.lineCount) widest = max(widest, layout.getLineWidth(i))
        return Pair(max(widest, 1f), layout.height.toFloat())
    }

    // ------------------------------------------------------------------------------------------
    // Menggambar

    /** Gambar semua overlay yang aktif pada waktu [t] (detik, timeline gabungan). Urutan daftar = urutan tumpukan. */
    fun draw(canvas: Canvas, w: Int, h: Int, overlays: List<Overlay>, t: Float) {
        if (w <= 0 || h <= 0) return
        for (o in overlays) {
            if (o.isActiveAt(t)) drawOne(canvas, w, h, o, t)
        }
    }

    private fun drawOne(canvas: Canvas, w: Int, h: Int, o: Overlay, t: Float) {
        if (o.kind == OverlayKind.Effect) {
            FxLayers.draw(canvas, w, h, o, t)
            return
        }
        val a = animOf(o, t)
        if (a.alpha <= 0.003f || a.scale <= 0.001f) return

        val text: String? = if (o.kind == OverlayKind.Text) {
            val full = o.content
            val shown = if (a.chars < 0) {
                full
            } else {
                val n = full.codePointCount(0, full.length)
                full.substring(0, full.offsetByCodePoints(0, min(a.chars, n)))
            }
            if (shown.isEmpty()) return
            shown
        } else {
            null
        }

        val checkpoint = canvas.save()
        canvas.translate(o.x * w, (o.y + a.dy) * h)
        canvas.rotate(o.rotation)
        canvas.scale(a.scale, a.scale)

        if (text != null) {
            val layout = textLayout(o, text, w, h)
            val boxW = layout.width.toFloat()
            val boxH = layout.height.toFloat()
            if (a.alpha < 0.999f) {
                val pad = layout.paint.textSize * 0.4f
                canvas.saveLayerAlpha(
                    -boxW / 2f - pad, -boxH / 2f - pad, boxW / 2f + pad, boxH / 2f + pad,
                    (a.alpha * 255f).toInt(),
                )
            }
            canvas.translate(-boxW / 2f, -boxH / 2f)
            layout.draw(canvas)
        } else {
            val paint = stickerPaint(o, w, h)
            val fm = paint.fontMetrics
            if (a.alpha < 0.999f) {
                val half = paint.textSize
                canvas.saveLayerAlpha(-half, -half, half, half, (a.alpha * 255f).toInt())
            }
            canvas.drawText(o.content, 0f, -(fm.ascent + fm.descent) / 2f, paint)
        }
        canvas.restoreToCount(checkpoint)
    }

    /** Bingkai putus-putus di sekeliling overlay terpilih (hanya untuk pratinjau, tidak ikut ekspor). */
    fun drawSelectionBox(canvas: Canvas, w: Int, h: Int, o: Overlay, strokePx: Float) {
        if (w <= 0 || h <= 0 || o.kind == OverlayKind.Effect) return
        val size = measure(o, w, h)
        val pad = strokePx * 3f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokePx
        paint.color = Color.WHITE
        paint.pathEffect = DashPathEffect(floatArrayOf(strokePx * 4f, strokePx * 3f), 0f)
        val checkpoint = canvas.save()
        canvas.translate(o.x * w, o.y * h)
        canvas.rotate(o.rotation)
        canvas.drawRect(
            -size.first / 2f - pad, -size.second / 2f - pad,
            size.first / 2f + pad, size.second / 2f + pad,
            paint,
        )
        canvas.restoreToCount(checkpoint)
    }

    /**
     * Kunci keadaan gambar pada waktu [t]: dua waktu dengan kunci sama menghasilkan gambar sama persis,
     * jadi ekspor tidak perlu menggambar ulang. Null = tidak ada overlay aktif (bingkai kosong).
     */
    fun frameKey(overlays: List<Overlay>, t: Float): Int? {
        var key = 1
        var any = false
        for (o in overlays) {
            if (!o.isActiveAt(t)) continue
            any = true
            if (o.kind == OverlayKind.Effect) {
                // efek lapisan bergerak tiap frame: kunci berubah tiap 1/30 detik
                key = 31 * key + o.hashCode()
                key = 31 * key + (t * 30f).toInt()
                continue
            }
            val a = animOf(o, t)
            key = 31 * key + o.hashCode()
            key = 31 * key + (a.alpha * 255f).toInt()
            key = 31 * key + (a.scale * 1000f).toInt()
            key = 31 * key + (a.dy * 1000f).toInt()
            key = 31 * key + a.chars
        }
        return if (any) key else null
    }
}
