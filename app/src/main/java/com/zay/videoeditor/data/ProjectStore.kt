package com.zay.videoeditor.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.zay.videoeditor.ui.theme.NeonAmber
import com.zay.videoeditor.ui.theme.NeonBlue
import com.zay.videoeditor.ui.theme.NeonMint
import com.zay.videoeditor.ui.theme.NeonOrange
import com.zay.videoeditor.ui.theme.NeonPink
import com.zay.videoeditor.ui.theme.NeonPurple
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Penyimpanan proyek di perangkat (TAHAP 4b).
 * - Proyek: filesDir/projects/<id>.json
 * - Salinan video: filesDir/media/  (supaya proyek tetap bisa dibuka walau file asli dipindah/dihapus)
 */
object ProjectStore {
    /** Daftar proyek untuk Beranda dan halaman Proyek. */
    val projects = mutableStateListOf<ProjectItem>()

    private val palettes = listOf(
        listOf(NeonOrange, NeonPurple),
        listOf(NeonBlue, Color(0xFF2B1B6E)),
        listOf(NeonAmber, Color(0xFFE0663D)),
        listOf(NeonPink, Color(0xFF6A3DFF)),
        listOf(NeonMint, NeonBlue),
    )

    private fun projectDir(context: Context): File = File(context.filesDir, "projects").also { it.mkdirs() }

    fun mediaDir(context: Context): File = File(context.filesDir, "media").also { it.mkdirs() }

    private fun fileOf(context: Context, id: Int): File = File(projectDir(context), "$id.json")

    private fun newId(): Int = (System.currentTimeMillis() / 1000L).toInt()

    private fun defaultName(): String =
        "Proyek " + SimpleDateFormat("d MMM HH.mm", Locale("id", "ID")).format(Date())

    private fun formatDuration(sec: Float): String {
        val total = sec.toInt()
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    // ---------------- Daftar ----------------

    fun refresh(context: Context) {
        val files = projectDir(context).listFiles()?.filter { it.extension == "json" } ?: emptyList()
        val items = files
            .sortedByDescending { it.lastModified() }
            .mapNotNull { readItem(it) }
        projects.clear()
        projects.addAll(items)
    }

    private fun readItem(file: File): ProjectItem? {
        return try {
            val o = JSONObject(file.readText())
            val id = o.getInt("id")
            val clips = o.optJSONArray("clips") ?: JSONArray()
            var total = 0f
            var thumbPath: String? = null
            var thumbSec = 0f
            for (i in 0 until clips.length()) {
                val c = clips.getJSONObject(i)
                total += (c.getDouble("trimEnd") - c.getDouble("trimStart")).toFloat()
                if (thumbPath == null) {
                    val p = c.optString("path", "")
                    if (p.isNotEmpty() && File(p).exists()) {
                        thumbPath = p
                        thumbSec = c.optDouble("trimStart", 0.0).toFloat()
                    }
                }
            }
            val resIdx = o.optInt("resolution", 1).coerceIn(0, EditorSession.resolutions.size - 1)
            ProjectItem(
                id = id,
                name = o.optString("name", "Proyek"),
                duration = formatDuration(total),
                resolution = EditorSession.resolutions[resIdx],
                kind = if (clips.length() > 0) "Video" else "Draf",
                favorite = o.optBoolean("favorite", false),
                colors = palettes[Math.floorMod(id, palettes.size)],
                thumbPath = thumbPath,
                thumbSec = thumbSec,
            )
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- Buka / simpan ----------------

    /** Mulai proyek kosong yang baru. File baru dibuat saat video pertama ditambahkan. */
    fun openNew() {
        EditorSession.start(newId(), defaultName(), 1, false, emptyList())
    }

    /** Buka proyek tersimpan. False bila file proyek tidak bisa dibaca. */
    fun open(context: Context, id: Int): Boolean {
        return try {
            val o = JSONObject(fileOf(context, id).readText())
            val list = mutableListOf<ImportedClip>()
            val arr = o.optJSONArray("clips") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val file = File(c.getString("path"))
                if (!file.exists()) continue
                val source = c.getDouble("source").toFloat()
                list.add(
                    ImportedClip(
                        id = EditorSession.nextId(),
                        uri = Uri.fromFile(file),
                        name = c.optString("name", "Klip"),
                        sourceDurationSec = source,
                        trimStartSec = c.getDouble("trimStart").toFloat(),
                        trimEndSec = c.getDouble("trimEnd").toFloat(),
                        filter = c.optInt("filter", 0).coerceIn(0, com.zay.videoeditor.fx.ClipFx.filters.size - 1),
                        filterStrength = c.optDouble("fstrength", 0.7).toFloat(),
                        transition = c.optInt("trans", 0).coerceIn(0, com.zay.videoeditor.fx.ClipFx.transitionNames.size - 1),
                        transitionDur = c.optDouble("transDur", 0.6).toFloat(),
                        motion = c.optInt("motion", 0).coerceIn(0, com.zay.videoeditor.fx.ClipFx.motionNames.size - 1),
                        motionStrength = c.optDouble("mstrength", 0.5).toFloat(),
                    ),
                )
            }
            val audios = mutableListOf<AudioClip>()
            val aArr = o.optJSONArray("audios") ?: JSONArray()
            for (i in 0 until aArr.length()) {
                val a = aArr.getJSONObject(i)
                val af = File(a.getString("path"))
                if (!af.exists()) continue
                audios.add(
                    AudioClip(
                        id = EditorSession.nextId(),
                        uri = Uri.fromFile(af),
                        name = a.optString("name", "Audio"),
                        durationSec = a.getDouble("duration").toFloat(),
                        startSec = a.optDouble("start", 0.0).toFloat(),
                        volume = a.optDouble("volume", 1.0).toFloat(),
                        isVoice = a.optBoolean("voice", false),
                        trimStartSec = a.optDouble("trim", 0.0).toFloat(),
                        sourceDurationSec = a.optDouble("source", a.getDouble("duration")).toFloat(),
                    ),
                )
            }
            val overlays = mutableListOf<Overlay>()
            val oArr = o.optJSONArray("overlays") ?: JSONArray()
            for (i in 0 until oArr.length()) {
                val parsed = overlayFromJson(oArr.getJSONObject(i)) ?: continue
                overlays.add(parsed)
            }
            EditorSession.start(
                id = o.getInt("id"),
                name = o.optString("name", defaultName()),
                resolution = o.optInt("resolution", 1).coerceIn(0, EditorSession.resolutions.size - 1),
                fav = o.optBoolean("favorite", false),
                list = list,
                aspect = o.optInt("aspect", 0),
                fill = o.optBoolean("fill", true),
                audio = audios,
                overlayList = overlays,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Simpan proyek yang sedang dibuka di editor. */
    fun saveCurrent(context: Context) {
        val id = EditorSession.projectId
        if (id == 0) return
        val file = fileOf(context, id)
        if (EditorSession.clips.isEmpty() && EditorSession.audioClips.isEmpty() &&
            EditorSession.overlays.isEmpty() && !file.exists()
        ) {
            return
        }
        try {
            val arr = JSONArray()
            for (c in EditorSession.clips.toList()) {
                arr.put(
                    JSONObject().apply {
                        put("path", c.uri.path ?: "")
                        put("name", c.name)
                        put("source", c.sourceDurationSec.toDouble())
                        put("trimStart", c.trimStartSec.toDouble())
                        put("trimEnd", c.trimEndSec.toDouble())
                        put("filter", c.filter)
                        put("fstrength", c.filterStrength.toDouble())
                        put("trans", c.transition)
                        put("transDur", c.transitionDur.toDouble())
                        put("motion", c.motion)
                        put("mstrength", c.motionStrength.toDouble())
                    },
                )
            }
            val aArr = JSONArray()
            for (a in EditorSession.audioClips.toList()) {
                aArr.put(
                    JSONObject().apply {
                        put("path", a.uri.path ?: "")
                        put("name", a.name)
                        put("duration", a.durationSec.toDouble())
                        put("start", a.startSec.toDouble())
                        put("volume", a.volume.toDouble())
                        put("voice", a.isVoice)
                        put("trim", a.trimStartSec.toDouble())
                        put("source", a.sourceDurationSec.toDouble())
                    },
                )
            }
            val oArr = JSONArray()
            for (ov in EditorSession.overlays.toList()) oArr.put(overlayToJson(ov))
            val o = JSONObject().apply {
                put("overlays", oArr)
                put("audios", aArr)
                put("id", id)
                put("name", EditorSession.projectName)
                put("resolution", EditorSession.resolutionIndex)
                put("favorite", EditorSession.favorite)
                put("aspect", EditorSession.aspectIndex)
                put("fill", EditorSession.fillFrame)
                put("clips", arr)
            }
            file.writeText(o.toString())
        } catch (e: Exception) {
            // gagal menyimpan: abaikan, percobaan berikutnya akan menimpa
        }
    }

    // ---------------- Teks dan stiker (TAHAP 5b) ----------------

    private fun overlayToJson(o: Overlay): JSONObject = JSONObject().apply {
        put("kind", o.kind.name)
        put("content", o.content)
        put("start", o.startSec.toDouble())
        put("duration", o.durationSec.toDouble())
        put("x", o.x.toDouble())
        put("y", o.y.toDouble())
        put("size", o.size.toDouble())
        put("rotation", o.rotation.toDouble())
        put("color", o.color)
        put("font", o.font)
        put("bold", o.bold)
        put("italic", o.italic)
        put("anim", o.anim)
    }

    /** Baca satu overlay dari JSON. Nilai di luar batas dirapikan; null bila data rusak. */
    private fun overlayFromJson(j: JSONObject): Overlay? {
        return try {
            val kind = try {
                OverlayKind.valueOf(j.getString("kind"))
            } catch (e: Exception) {
                return null
            }
            val sizeMin = when (kind) {
                OverlayKind.Text -> OverlayStyle.TEXT_SIZE_MIN
                OverlayKind.Sticker -> OverlayStyle.STICKER_SIZE_MIN
                OverlayKind.Effect -> 10f
            }
            val sizeMax = when (kind) {
                OverlayKind.Text -> OverlayStyle.TEXT_SIZE_MAX
                OverlayKind.Sticker -> OverlayStyle.STICKER_SIZE_MAX
                OverlayKind.Effect -> 100f
            }
            Overlay(
                id = EditorSession.nextId(),
                kind = kind,
                content = j.optString("content", ""),
                startSec = j.optDouble("start", 0.0).toFloat().coerceAtLeast(0f),
                durationSec = j.optDouble("duration", OverlayStyle.DEFAULT_DURATION.toDouble()).toFloat()
                    .coerceAtLeast(OverlayStyle.MIN_DURATION),
                x = j.optDouble("x", 0.5).toFloat().coerceIn(-0.5f, 1.5f),
                y = j.optDouble("y", 0.5).toFloat().coerceIn(-0.5f, 1.5f),
                size = j.optDouble("size", 48.0).toFloat().coerceIn(sizeMin, sizeMax),
                rotation = j.optDouble("rotation", 0.0).toFloat().coerceIn(-360f, 360f),
                color = j.optInt("color", -1),
                font = j.optInt("font", 1).coerceIn(0, OverlayStyle.fontFamilies.size - 1),
                bold = j.optBoolean("bold", true),
                italic = j.optBoolean("italic", false),
                anim = j.optInt("anim", 0).coerceIn(0, OverlayStyle.animNames.size - 1),
            )
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- Kelola ----------------

    fun setFavorite(context: Context, id: Int, value: Boolean) {
        try {
            val f = fileOf(context, id)
            val o = JSONObject(f.readText())
            o.put("favorite", value)
            f.writeText(o.toString())
        } catch (e: Exception) {
            // abaikan
        }
        refresh(context)
    }

    /** Ganti nama proyek yang tersimpan di perangkat. */
    fun rename(context: Context, id: Int, newName: String) {
        val name = newName.trim()
        if (name.isEmpty()) return
        try {
            val f = fileOf(context, id)
            val o = JSONObject(f.readText())
            o.put("name", name)
            f.writeText(o.toString())
        } catch (e: Exception) {
            // abaikan
        }
        refresh(context)
    }

    /** Hapus proyek beserta salinan video yang tidak dipakai proyek lain. */
    fun delete(context: Context, id: Int) {
        val target = fileOf(context, id)
        val mine = pathsOf(target)
        target.delete()
        val others = (projectDir(context).listFiles() ?: emptyArray())
            .filter { it.extension == "json" }
            .flatMap { pathsOf(it) }
            .toSet()
        for (p in mine) {
            if (p !in others) File(p).delete()
        }
        refresh(context)
    }

    private fun pathsOf(file: File): List<String> {
        return try {
            val o = JSONObject(file.readText())
            val out = mutableListOf<String>()
            for (key in listOf("clips", "audios")) {
                val arr = o.optJSONArray(key) ?: continue
                for (i in 0 until arr.length()) out.add(arr.getJSONObject(i).getString("path"))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }
}
