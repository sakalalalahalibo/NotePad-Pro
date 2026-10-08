package com.notepadpro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "ملف جديد",
    val text: String = "",
    val updated: Long = System.currentTimeMillis(),
    val dirty: Boolean = false
)

enum class ThemeMode { System, Dark, Light }
enum class DirMode { Auto, Rtl, Ltr }
enum class FontKind { Default, Serif, Mono }
enum class ReadEnc(val charset: Charset) {
    Utf8(Charsets.UTF_8),
    Win1256(Charset.forName("windows-1256")),
    Utf16(Charsets.UTF_16LE)
}

data class Settings(
    val theme: ThemeMode = ThemeMode.System,
    val fontSize: Int = 18,
    val lineHeight: Float = 1.8f,
    val dir: DirMode = DirMode.Auto,
    val font: FontKind = FontKind.Default,
    val saveBom: Boolean = false,
    val crlf: Boolean = false,
    val readEnc: ReadEnc = ReadEnc.Utf8
)

data class Snapshot(val notes: List<Note>, val current: String?, val settings: Settings)

object Storage {
    private const val PREFS = "notepad_pro"
    private const val KEY = "data"

    private inline fun <reified T : Enum<T>> enumOr(v: String, d: T): T =
        runCatching { enumValueOf<T>(v) }.getOrDefault(d)

    fun load(ctx: Context): Snapshot {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return Snapshot(emptyList(), null, Settings())
        return runCatching {
            val o = JSONObject(raw)
            val s = o.optJSONObject("settings") ?: JSONObject()
            val d = Settings()
            val settings = Settings(
                theme = enumOr(s.optString("theme"), d.theme),
                fontSize = s.optInt("fontSize", d.fontSize),
                lineHeight = s.optDouble("lineHeight", d.lineHeight.toDouble()).toFloat(),
                dir = enumOr(s.optString("dir"), d.dir),
                font = enumOr(s.optString("font"), d.font),
                saveBom = s.optBoolean("saveBom", d.saveBom),
                crlf = s.optBoolean("crlf", d.crlf),
                readEnc = enumOr(s.optString("readEnc"), d.readEnc)
            )
            val arr = o.optJSONArray("notes") ?: JSONArray()
            val notes = (0 until arr.length()).map {
                val n = arr.getJSONObject(it)
                Note(
                    id = n.getString("id"),
                    name = n.optString("name", "ملف جديد"),
                    text = n.optString("text"),
                    updated = n.optLong("updated"),
                    dirty = n.optBoolean("dirty")
                )
            }
            Snapshot(notes, o.optString("cur").ifEmpty { null }, settings)
        }.getOrDefault(Snapshot(emptyList(), null, Settings()))
    }

    fun save(ctx: Context, notes: List<Note>, current: String, s: Settings) {
        val o = JSONObject()
            .put("cur", current)
            .put(
                "settings", JSONObject()
                    .put("theme", s.theme.name).put("fontSize", s.fontSize)
                    .put("lineHeight", s.lineHeight.toDouble()).put("dir", s.dir.name)
                    .put("font", s.font.name).put("saveBom", s.saveBom)
                    .put("crlf", s.crlf).put("readEnc", s.readEnc.name)
            )
            .put("notes", JSONArray(notes.map {
                JSONObject().put("id", it.id).put("name", it.name).put("text", it.text)
                    .put("updated", it.updated).put("dirty", it.dirty)
            }))
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, o.toString()).apply()
    }
}
