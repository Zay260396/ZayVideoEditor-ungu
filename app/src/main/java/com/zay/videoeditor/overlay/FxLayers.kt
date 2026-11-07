package com.zay.videoeditor.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.zay.videoeditor.data.Overlay
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Satu jenis efek lapisan (overlay visual) untuk panel Efek > Overlay. */
class FxDef(val key: String, val name: String, val emoji: String, val top: Int, val bottom: Int)

/**
 * Efek visual yang digambar di atas video (salju, hujan, bintang, vinyet, dll.).
 * Digambar oleh OverlayRenderer, jadi pratinjau dan ekspor identik. Semua gerak dihitung dari waktu [t]
 * dan nomor partikel (acak tetap), tanpa menyimpan keadaan, sehingga bisa digambar di waktu mana pun.
 * Kekuatan efek = [Overlay.size] / 100.
 */
object FxLayers {
    val items: List<FxDef> = listOf(
        FxDef("snow", "Salju", "❄", 0xFFBFE3FF.toInt(), 0xFF3A5FA8.toInt()),
        FxDef("rain", "Hujan", "🌧", 0xFF9FB4FF.toInt(), 0xFF2B1B6E.toInt()),
        FxDef("stars", "Bintang", "✨", 0xFFFFE680.toInt(), 0xFF7B2FFF.toInt()),
        FxDef("dust", "Debu Film", "🎞", 0xFFCCCCCC.toInt(), 0xFF444444.toInt()),
        FxDef("vignette", "Vinyet", "◐", 0xFF777788.toInt(), 0xFF101018.toInt()),
        FxDef("cinema", "Sinema", "🎬", 0xFF333344.toInt(), 0xFF000000.toInt()),
        FxDef("hearts", "Hati", "💖", 0xFFFF8AC0.toInt(), 0xFFB83A9A.toInt()),
        FxDef("confetti", "Konfeti", "🎉", 0xFFFFC46B.toInt(), 0xFFFF4DA6.toInt()),
        FxDef("leak", "Cahaya Bocor", "🌅", 0xFFFFB46B.toInt(), 0xFFFF4D8A.toInt()),
        FxDef("flash", "Kilat", "⚡", 0xFFFFFFFF.toInt(), 0xFF7FA8FF.toInt()),
        FxDef("bubbles", "Gelembung", "🫧", 0xFF9FE8FF.toInt(), 0xFF2B7FE0.toInt()),
    )

    fun nameOf(key: String): String = items.firstOrNull { it.key == key }?.name ?: "Efek"

    private fun rnd(i: Int, salt: Int): Float {
        val x = sin(i * 12.9898 + salt * 78.233) * 43758.5453
        return (x - floor(x)).toFloat()
    }

    private fun mod(a: Float, b: Float): Float {
        val r = a % b
        return if (r < 0f) r + b else r
    }

    fun draw(canvas: Canvas, w: Int, h: Int, o: Overlay, t: Float) {
        val u = min(w, h) / 1080f
        val k = (o.size / 100f).coerceIn(0.1f, 1f)
        val local = max(0f, t - o.startSec)
        val remain = max(0f, o.endSec - t)
        val fade = min(1f, min(local / 0.4f, remain / 0.4f)).coerceIn(0f, 1f)
        if (fade <= 0.002f) return
        val wf = w.toFloat()
        val hf = h.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (o.content) {
            "snow" -> {
                val n = (30 + 90 * k).toInt()
                paint.color = Color.WHITE
                for (i in 0 until n) {
                    val r = (1.5f + rnd(i, 3) * 4.5f) * u
                    val speed = (40f + rnd(i, 4) * 90f) * u
                    val x = mod(rnd(i, 1) * wf + sin(t * 0.8f + i) * 25f * u, wf)
                    val y = mod(rnd(i, 2) * hf + t * speed, hf)
                    paint.alpha = ((0.5f + 0.5f * rnd(i, 5)) * 255f * fade).toInt().coerceIn(0, 255)
                    canvas.drawCircle(x, y, r, paint)
                }
            }
            "rain" -> {
                val n = (60 + 160 * k).toInt()
                paint.color = Color.rgb(191, 217, 255)
                paint.strokeWidth = max(1f, 2f * u)
                paint.alpha = (140f * fade).toInt().coerceIn(0, 255)
                for (i in 0 until n) {
                    val x = rnd(i, 1) * wf
                    val speed = (1400f + rnd(i, 2) * 600f) * u
                    val y = mod(rnd(i, 3) * hf + t * speed, hf)
                    val len = (24f + rnd(i, 4) * 30f) * u
                    canvas.drawLine(x, y, x - len * 0.15f, y - len, paint)
                }
            }
            "stars" -> {
                val n = (14 + 30 * k).toInt()
                paint.color = Color.rgb(255, 242, 176)
                val path = Path()
                for (i in 0 until n) {
                    val cx = rnd(i, 1) * wf
                    val cy = rnd(i, 2) * hf
                    val s = (6f + rnd(i, 3) * 14f) * u
                    val a = 0.5f + 0.5f * sin(t * (2f + rnd(i, 4) * 3f) + i * 1.7f)
                    paint.alpha = (a * 255f * fade).toInt().coerceIn(0, 255)
                    val q = s * 0.18f
                    path.reset()
                    path.moveTo(cx, cy - s)
                    path.lineTo(cx + q, cy - q)
                    path.lineTo(cx + s, cy)
                    path.lineTo(cx + q, cy + q)
                    path.lineTo(cx, cy + s)
                    path.lineTo(cx - q, cy + q)
                    path.lineTo(cx - s, cy)
                    path.lineTo(cx - q, cy - q)
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
            "dust" -> {
                val f = floor(t * 10f).toInt()
                val n = (8 + 30 * k).toInt()
                for (i in 0 until n) {
                    val x = rnd(i, f * 7 + 1) * wf
                    val y = rnd(i, f * 7 + 2) * hf
                    val r = (0.8f + rnd(i, f * 7 + 3) * 2.2f) * u
                    paint.color = if (rnd(i, f * 7 + 4) > 0.5f) Color.WHITE else Color.rgb(40, 40, 40)
                    paint.alpha = (150f * fade).toInt().coerceIn(0, 255)
                    canvas.drawCircle(x, y, r, paint)
                }
                if (rnd(f, 98) > 0.7f) {
                    paint.color = Color.WHITE
                    paint.alpha = (70f * fade).toInt().coerceIn(0, 255)
                    paint.strokeWidth = max(1f, 1.5f * u)
                    val x = rnd(f, 97) * wf
                    canvas.drawLine(x, 0f, x + (rnd(f, 96) - 0.5f) * 10f * u, hf, paint)
                }
            }
            "vignette" -> {
                val radius = hypot(wf, hf) / 2f
                val edge = Color.argb((200f * k * fade).toInt().coerceIn(0, 255), 0, 0, 0)
                paint.shader = RadialGradient(
                    wf / 2f, hf / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, edge),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            "cinema" -> {
                val bar = hf * 0.13f * k * fade
                paint.color = Color.BLACK
                canvas.drawRect(0f, 0f, wf, bar, paint)
                canvas.drawRect(0f, hf - bar, wf, hf, paint)
            }
            "hearts" -> {
                val n = (8 + 16 * k).toInt()
                paint.textAlign = Paint.Align.CENTER
                for (i in 0 until n) {
                    val sz = (26f + rnd(i, 3) * 40f) * u
                    paint.textSize = sz
                    val period = hf + sz * 2f
                    val x = rnd(i, 1) * wf + sin(t * 1.3f + i) * 20f * u
                    val y = hf + sz - mod(rnd(i, 2) * period + t * (90f + rnd(i, 4) * 120f) * u, period)
                    paint.alpha = (220f * fade).toInt().coerceIn(0, 255)
                    canvas.drawText("💖", x, y, paint)
                }
            }
            "confetti" -> {
                val colors = intArrayOf(
                    Color.rgb(255, 77, 166), Color.rgb(255, 196, 107), Color.rgb(77, 166, 255),
                    Color.rgb(123, 255, 183), Color.rgb(176, 123, 255),
                )
                val n = (40 + 80 * k).toInt()
                for (i in 0 until n) {
                    val x = mod(rnd(i, 1) * wf + sin(t * 2f + i) * 30f * u, wf)
                    val y = mod(rnd(i, 2) * hf + t * (150f + rnd(i, 3) * 200f) * u, hf)
                    val a = (6f + rnd(i, 4) * 8f) * u
                    val b = (10f + rnd(i, 5) * 10f) * u
                    paint.color = colors[i % colors.size]
                    paint.alpha = (255f * fade).toInt().coerceIn(0, 255)
                    val cp = canvas.save()
                    canvas.translate(x, y)
                    canvas.rotate(t * (100f + rnd(i, 6) * 200f) + i * 37f)
                    canvas.drawRect(-a / 2f, -b / 2f, a / 2f, b / 2f, paint)
                    canvas.restoreToCount(cp)
                }
            }
            "leak" -> {
                val p = mod(local * 0.15f, 1.6f) - 0.3f
                val a = (120f * k * fade).toInt().coerceIn(0, 255)
                paint.shader = LinearGradient(
                    (p - 0.35f) * wf, 0f, (p + 0.35f) * wf, hf * 0.4f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(a, 255, 140, 60), Color.argb(a, 255, 60, 140), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 0.6f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }
            "flash" -> {
                val ph = mod(local, 1.6f)
                val a = (1f - ph / 0.25f).coerceIn(0f, 1f)
                if (a > 0.01f) {
                    paint.color = Color.WHITE
                    paint.alpha = (170f * k * a * fade).toInt().coerceIn(0, 255)
                    canvas.drawRect(0f, 0f, wf, hf, paint)
                }
            }
            "bubbles" -> {
                val n = (10 + 20 * k).toInt()
                for (i in 0 until n) {
                    val r = (8f + rnd(i, 3) * 26f) * u
                    val period = hf + 2f * r
                    val x = rnd(i, 1) * wf + sin(t * 1.1f + i) * 22f * u
                    val y = hf + r - mod(rnd(i, 2) * period + t * (70f + rnd(i, 4) * 90f) * u, period)
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    paint.alpha = (40f * fade).toInt().coerceIn(0, 255)
                    canvas.drawCircle(x, y, r, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = max(1f, 2f * u)
                    paint.alpha = (170f * fade).toInt().coerceIn(0, 255)
                    canvas.drawCircle(x, y, r, paint)
                }
            }
            else -> {}
        }
    }
}
