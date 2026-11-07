package com.zay.videoeditor.fx

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import com.zay.videoeditor.data.ImportedClip

/** Satu filter warna. [m] = matriks warna 4x5 (20 angka, offset dalam skala 0..1) pada kekuatan penuh. */
class FilterDef(val name: String, val top: Color, val bottom: Color, val m: FloatArray)

/** Keadaan transisi pada satu saat: [type] jenis, [amount] 0..1 (0 = normal, 1 = penuh), [isIn] true = klip sedang masuk. */
data class TransitionState(val type: Int, val amount: Float, val isIn: Boolean)

/**
 * Filter warna dan transisi antar klip.
 * Satu sumber kebenaran untuk pratinjau (ColorMatrix + lapisan gelap/terang) dan ekspor (efek Media3).
 */
object ClipFx {

    // ---------------------------------------------------------------- matriks warna

    private fun ident() = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private fun sat(s: Float): FloatArray {
        val a = (1f - s) * 0.2126f
        val b = (1f - s) * 0.7152f
        val c = (1f - s) * 0.0722f
        return floatArrayOf(
            a + s, b, c, 0f, 0f,
            a, b + s, c, 0f, 0f,
            a, b, c + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private fun scale(r: Float, g: Float, b: Float) = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private fun offs(r: Float, g: Float, b: Float) = floatArrayOf(
        1f, 0f, 0f, 0f, r,
        0f, 1f, 0f, 0f, g,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f,
    )

    private fun contrast(c: Float): FloatArray {
        val o = 0.5f * (1f - c)
        return floatArrayOf(
            c, 0f, 0f, 0f, o,
            0f, c, 0f, 0f, o,
            0f, 0f, c, 0f, o,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** Terapkan [first] dulu, lalu [second]. */
    private fun then(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += second[i * 5 + k] * first[k * 5 + j]
                out[i * 5 + j] = sum
            }
            var off = second[i * 5 + 4]
            for (k in 0 until 4) off += second[i * 5 + k] * first[k * 5 + 4]
            out[i * 5 + 4] = off
        }
        return out
    }

    private fun chain(vararg steps: FloatArray): FloatArray {
        var acc = ident()
        for (s in steps) acc = then(acc, s)
        return acc
    }

    /** Indeks 0 = Asli (tanpa filter). */
    val filters: List<FilterDef> = listOf(
        FilterDef("Asli", Color(0xFF6A6F8F), Color(0xFF2B2F4A), ident()),
        FilterDef("Vintage", Color(0xFFE8B98A), Color(0xFF8A5A44), chain(sat(0.7f), scale(1.08f, 1.0f, 0.82f), contrast(0.92f), offs(0.04f, 0.02f, 0f))),
        FilterDef("Dreamy", Color(0xFF9FD8FF), Color(0xFFD9A6FF), chain(contrast(0.88f), sat(1.15f), scale(1.04f, 1.0f, 1.1f), offs(0.07f, 0.05f, 0.09f))),
        FilterDef("Neon", Color(0xFFFF4DA6), Color(0xFF7B2FFF), chain(sat(1.9f), contrast(1.2f), scale(1.05f, 0.95f, 1.15f))),
        FilterDef("Hangat", Color(0xFFFFC46B), Color(0xFFE0663D), chain(scale(1.12f, 1.02f, 0.86f), sat(1.1f))),
        FilterDef("Dingin", Color(0xFF7FD3FF), Color(0xFF2B4FA0), chain(scale(0.88f, 1.0f, 1.14f), sat(1.05f))),
        FilterDef("Film", Color(0xFF7FA8C9), Color(0xFF2B3A55), chain(contrast(1.12f), sat(0.85f), scale(0.95f, 1.02f, 1.05f), offs(0f, 0.01f, 0.03f))),
        FilterDef("Glow", Color(0xFFFFFFFF), Color(0xFF4DA6FF), chain(contrast(0.92f), offs(0.09f, 0.09f, 0.09f), sat(1.1f))),
        FilterDef(
            "Sepia", Color(0xFFD9B88F), Color(0xFF6B4A2E),
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
        FilterDef("Mono", Color(0xFFCCCCCC), Color(0xFF222222), sat(0f)),
        FilterDef("Drama", Color(0xFFB0B0C0), Color(0xFF101018), chain(contrast(1.35f), sat(1.2f), offs(-0.05f, -0.05f, -0.05f))),
        FilterDef("Cerah", Color(0xFFFFE680), Color(0xFF4DD2A0), chain(offs(0.05f, 0.05f, 0.05f), sat(1.3f), contrast(1.08f))),
        FilterDef("Pudar", Color(0xFFD8D8E8), Color(0xFF8A8AA8), chain(contrast(0.8f), sat(0.75f), offs(0.08f, 0.08f, 0.08f))),
        FilterDef("Senja", Color(0xFFFFA05A), Color(0xFF8A2BE2), chain(scale(1.15f, 1.0f, 0.8f), sat(1.3f), offs(0.03f, 0.01f, 0f))),
        FilterDef("Musim Gugur", Color(0xFFE0883D), Color(0xFF6B3A1E), chain(sat(1.2f), scale(1.12f, 1.0f, 0.78f), contrast(1.05f))),
        FilterDef("Pastel", Color(0xFFFFD1E8), Color(0xFFBFD8FF), chain(contrast(0.8f), sat(0.85f), offs(0.1f, 0.1f, 0.12f))),
        FilterDef("Tajam", Color(0xFFE0E0F0), Color(0xFF202030), chain(contrast(1.5f), sat(1.1f))),
        FilterDef("Tosca", Color(0xFF7FF0E0), Color(0xFF1F7FA0), chain(scale(0.85f, 1.08f, 1.1f), sat(1.15f))),
        FilterDef("Rose", Color(0xFFFFB0C8), Color(0xFFB04A7A), chain(scale(1.1f, 0.92f, 1.02f), offs(0.05f, 0f, 0.03f), sat(1.1f))),
        FilterDef("Matcha", Color(0xFFB5E08A), Color(0xFF3A7A4A), chain(scale(0.92f, 1.08f, 0.9f), sat(0.95f))),
        FilterDef("Cyber", Color(0xFFFF4DD2), Color(0xFF2B4FFF), chain(sat(1.6f), contrast(1.2f), scale(1.12f, 0.88f, 1.2f))),
        FilterDef("Noir", Color(0xFFAAAAAA), Color(0xFF000000), chain(sat(0f), contrast(1.4f), offs(-0.04f, -0.04f, -0.04f))),
        FilterDef("Retro 80", Color(0xFFFF8AE0), Color(0xFF4A2BB0), chain(sat(1.3f), scale(1.1f, 0.9f, 1.15f), offs(0.06f, 0f, 0.06f), contrast(0.95f))),
        FilterDef("Emas", Color(0xFFFFD86B), Color(0xFF8A5A1E), chain(scale(1.18f, 1.05f, 0.7f), sat(1.25f), contrast(1.05f))),
        FilterDef(
            "Negatif", Color(0xFF301060), Color(0xFFE0F0A0),
            floatArrayOf(
                -1f, 0f, 0f, 0f, 1f,
                0f, -1f, 0f, 0f, 1f,
                0f, 0f, -1f, 0f, 1f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    /** Matriks 4x5 untuk filter [index] pada kekuatan [strength] (0..1): campuran antara asli dan filter penuh. */
    fun matrix(index: Int, strength: Float): FloatArray {
        val f = filters.getOrNull(index) ?: return ident()
        val s = strength.coerceIn(0f, 1f)
        val id = ident()
        return FloatArray(20) { i -> id[i] + (f.m[i] - id[i]) * s }
    }

    /** Untuk pratinjau di TextureView. Null bila klip tanpa filter. */
    fun previewFilter(index: Int, strength: Float): ColorMatrixColorFilter? {
        if (index <= 0) return null
        val m = matrix(index, strength)
        for (i in intArrayOf(4, 9, 14, 19)) m[i] = m[i] * 255f // ColorMatrix Android: offset skala 0..255
        return ColorMatrixColorFilter(ColorMatrix(m))
    }

    /** Matriks 4x5 menjadi matriks RGB 4x4 (urutan kolom) milik Media3. Alpha video = 1 sehingga offset masuk kolom ke-4. */
    private fun toRgb4(a: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (row in 0 until 4) {
            for (col in 0 until 3) out[col * 4 + row] = a[row * 5 + col]
            out[3 * 4 + row] = a[row * 5 + 3] + a[row * 5 + 4]
        }
        return out
    }

    // ---------------------------------------------------------------- gerakan klip (tab Animasi)

    /** Indeks 0 = tanpa gerakan. */
    val motionNames = listOf("Tanpa", "Getar", "Denyut", "Zoom Masuk", "Zoom Keluar", "Goyang", "Melayang", "Cermin")

    /**
     * Gerakan pada satu saat. [scale] pembesaran, [tx] dan [ty] geseran dalam satuan NDC (2 = lebar/tinggi bingkai,
     * ty positif = ke atas), [rot] derajat searah jarum jam, [flip] cermin kiri-kanan.
     */
    class Motion(val scale: Float, val tx: Float, val ty: Float, val rot: Float, val flip: Boolean)

    /** Gerakan klip jenis [type] pada detik lokal [t] dari klip berdurasi [dur], kekuatan [k] 0..1. */
    fun motionAt(type: Int, k: Float, t: Float, dur: Float): Motion? {
        val p = if (dur > 0.01f) (t / dur).coerceIn(0f, 1f) else 0f
        val pi2 = 6.2831855f
        return when (type) {
            1 -> { // Getar
                val a = 0.015f + 0.03f * k
                Motion(1.02f + a * 1.2f, a * sin(t * 47f), a * kotlin.math.cos(t * 39f), 0.6f * k * sin(t * 31f), false)
            }
            2 -> { // Denyut
                val pulse = kotlin.math.abs(sin(t * 3.1415927f * 1.5f))
                Motion(1f + (0.03f + 0.1f * k) * pulse, 0f, 0f, 0f, false)
            }
            3 -> Motion(1f + (0.1f + 0.4f * k) * p, 0f, 0f, 0f, false) // Zoom Masuk
            4 -> Motion(1f + (0.1f + 0.4f * k) * (1f - p), 0f, 0f, 0f, false) // Zoom Keluar
            5 -> { // Goyang
                val amp = 2f + 6f * k
                Motion(1.03f + 0.035f * amp, 0f, 0f, amp * sin(t * pi2 * 0.5f), false)
            }
            6 -> Motion(1.1f, 0.03f * sin(t * pi2 * 0.35f), 0.04f * sin(t * pi2 * 0.5f), 0f, false) // Melayang
            7 -> Motion(1f, 0f, 0f, 0f, true) // Cermin
            else -> null
        }
    }

    // ---------------------------------------------------------------- transisi

    /** Indeks 0 = tanpa transisi. */
    val transitionNames = listOf("Tanpa", "Fade", "Cahaya", "Geser", "Zoom", "Putar")

    const val T_NONE = 0
    const val T_FADE = 1
    const val T_LIGHT = 2
    const val T_SLIDE = 3
    const val T_ZOOM = 4
    const val T_SPIN = 5

    private fun half(d: Float, a: ImportedClip, b: ImportedClip): Float =
        minOf(d / 2f, a.durationSec / 2f, b.durationSec / 2f)

    /**
     * Keadaan transisi klip ke-[i] pada detik lokal [t] (dihitung dari awal klip).
     * Transisi dipasang di sambungan: separuh durasi di ujung klip kiri, separuh di awal klip kanan.
     */
    fun stateAt(clips: List<ImportedClip>, i: Int, t: Float): TransitionState? {
        if (i !in clips.indices) return null
        val c = clips[i]
        var best: TransitionState? = null

        if (c.transition != T_NONE && i + 1 < clips.size) {
            val h = half(c.transitionDur, c, clips[i + 1])
            val start = c.durationSec - h
            if (h > 0.01f && t > start) {
                val a = ((t - start) / h).coerceIn(0f, 1f)
                best = TransitionState(c.transition, a, false)
            }
        }
        if (i > 0) {
            val p = clips[i - 1]
            if (p.transition != T_NONE) {
                val h = half(p.transitionDur, p, c)
                if (h > 0.01f && t < h) {
                    val a = (1f - t / h).coerceIn(0f, 1f)
                    if (best == null || a > best.amount) best = TransitionState(p.transition, a, true)
                }
            }
        }
        return best?.takeIf { it.amount > 0.001f }
    }

    private fun smooth(a: Float): Float = a * a * (3f - 2f * a)

    /** Untuk pratinjau: warna dan transparansi lapisan yang menutupi video. Null bila tidak ada transisi. */
    fun previewDip(clips: List<ImportedClip>, i: Int, t: Float): Pair<Color, Float>? {
        val s = stateAt(clips, i, t) ?: return null
        val color = if (s.type == T_LIGHT) Color.White else Color.Black
        return color to smooth(s.amount)
    }

    // ---------------------------------------------------------------- efek ekspor (Media3)

    /**
     * Daftar efek Media3 untuk klip ke-[i]: filter warna, lalu gerak transisi, lalu pudar/kilat transisi.
     * Waktu transisi dihitung dari frame pertama yang diterima efek, jadi tidak bergantung pada
     * apakah Media3 memberi waktu mulai dari 0 atau dari awal timeline.
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    fun buildEffects(clips: List<ImportedClip>, i: Int, width: Int, height: Int): List<Effect> {
        val c = clips[i]
        val list = mutableListOf<Effect>()

        if (c.filter > 0) {
            val m4 = toRgb4(matrix(c.filter, c.filterStrength))
            list.add(
                object : RgbMatrix {
                    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = m4
                },
            )
        }

        if (c.motion > 0) {
            val arM = width.toFloat() / height.toFloat()
            var baseMotion = -1L
            list.add(
                object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        if (baseMotion < 0L) baseMotion = presentationTimeUs
                        val t = (presentationTimeUs - baseMotion) / 1_000_000f
                        val m = Matrix()
                        val mo = motionAt(c.motion, c.motionStrength, t, c.durationSec) ?: return m
                        m.postScale(if (mo.flip) -mo.scale else mo.scale, mo.scale)
                        if (mo.rot != 0f) {
                            m.postScale(arM, 1f)
                            m.postRotate(-mo.rot)
                            m.postScale(1f / arM, 1f)
                        }
                        m.postTranslate(mo.tx, mo.ty)
                        return m
                    }
                },
            )
        }

        val hasIn = i > 0 && clips[i - 1].transition != T_NONE
        val hasOut = c.transition != T_NONE && i + 1 < clips.size
        if (hasIn || hasOut) {
            val ar = width.toFloat() / height.toFloat()

            // Gerak (geser, zoom, putar)
            var baseGeo = -1L
            list.add(
                object : MatrixTransformation {
                    override fun getMatrix(presentationTimeUs: Long): Matrix {
                        if (baseGeo < 0L) baseGeo = presentationTimeUs
                        val t = (presentationTimeUs - baseGeo) / 1_000_000f
                        val m = Matrix()
                        val st = stateAt(clips, i, t) ?: return m
                        val s = smooth(st.amount)
                        val sign = if (st.isIn) 1f else -1f
                        when (st.type) {
                            T_SLIDE -> m.postTranslate(sign * 2f * s, 0f)
                            T_ZOOM -> {
                                val z = 1f + 1.5f * s
                                m.postScale(z, z)
                            }
                            T_SPIN -> {
                                m.postScale(ar, 1f)
                                m.postRotate(sign * -90f * s)
                                m.postScale(1f / ar, 1f)
                                val z = 1f - 0.6f * s
                                m.postScale(z, z)
                            }
                            else -> {}
                        }
                        return m
                    }
                },
            )

            // Pudar ke gelap atau kilat putih
            var baseColor = -1L
            list.add(
                object : RgbMatrix {
                    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
                        if (baseColor < 0L) baseColor = presentationTimeUs
                        val t = (presentationTimeUs - baseColor) / 1_000_000f
                        val out = floatArrayOf(
                            1f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, 0f, 0f, 1f,
                        )
                        val st = stateAt(clips, i, t) ?: return out
                        if (st.type == T_SLIDE) return out
                        val s = smooth(st.amount)
                        val keep = 1f - s
                        out[0] = keep
                        out[5] = keep
                        out[10] = keep
                        if (st.type == T_LIGHT) {
                            out[12] = s
                            out[13] = s
                            out[14] = s
                        }
                        return out
                    }
                },
            )
        }
        return list
    }
}
