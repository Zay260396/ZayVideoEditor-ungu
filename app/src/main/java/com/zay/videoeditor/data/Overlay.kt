package com.zay.videoeditor.data

/** Jenis lapisan di atas video (TAHAP 5b). */
enum class OverlayKind { Text, Sticker, Effect }

/**
 * Teks atau stiker di atas video. Semua nilai bebas-resolusi supaya pratinjau dan ekspor identik:
 * - [x], [y]: titik tengah, pecahan 0..1 dari lebar/tinggi bingkai.
 * - [size]: ukuran huruf/stiker dalam "piksel referensi" pada sisi pendek bingkai 1080 px
 *   (di 720p otomatis lebih kecil, di 4K otomatis lebih besar).
 * - [startSec], [durationSec]: waktu tampil pada timeline gabungan.
 * Untuk jenis Effect (efek lapisan seperti salju/hujan), [content] adalah kunci efek (lihat FxLayers) dan [size] adalah
 * kekuatan 0..100; posisi dan gaya teks tidak dipakai.
 * Untuk stiker, [content] adalah emoji-nya; gaya teks ([font], [bold], [italic], [color]) diabaikan kecuali [anim].
 */
data class Overlay(
    val id: Int,
    val kind: OverlayKind,
    val content: String,
    val startSec: Float,
    val durationSec: Float,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = 48f,
    val rotation: Float = 0f,
    val color: Int = -1, // putih (0xFFFFFFFF)
    val font: Int = 1,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val anim: Int = 0,
) {
    val endSec: Float
        get() = startSec + durationSec

    fun isActiveAt(t: Float): Boolean = t >= startSec && t < endSec
}

/** Pilihan gaya yang dipakai bersama panel dan renderer. Urutan indeks = nilai yang disimpan di proyek. */
object OverlayStyle {
    /** Nama keluarga font Android; indeks sama dengan pilihan font di panel Teks. */
    val fontFamilies = listOf("serif", "sans-serif", "monospace", "cursive")
    val fontNames = listOf("Classic", "Modern", "Kode", "Cute")
    val animNames = listOf("Tanpa", "Fade", "Naik", "Zoom", "Ketik", "Pantul")

    const val TEXT_SIZE_MIN = 16f
    const val TEXT_SIZE_MAX = 160f
    const val STICKER_SIZE_MIN = 40f
    const val STICKER_SIZE_MAX = 400f
    const val MIN_DURATION = 0.5f
    const val DEFAULT_DURATION = 3f
}
