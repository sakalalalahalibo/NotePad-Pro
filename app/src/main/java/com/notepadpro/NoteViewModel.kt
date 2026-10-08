package com.notepadpro

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Msg(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    var notes by mutableStateOf(listOf<Note>()); private set
    var currentId by mutableStateOf(""); private set
    var settings by mutableStateOf(Settings()); private set
    var field by mutableStateOf(TextFieldValue("")); private set
    var focusTick by mutableIntStateOf(0); private set

    var showFind by mutableStateOf(false)
    var findText by mutableStateOf("")
    var replaceText by mutableStateOf("")
    var matchCase by mutableStateOf(false)
    var useRegex by mutableStateOf(false)

    val events = MutableSharedFlow<Msg>(extraBufferCapacity = 8)

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastPush = 0L
    private var saveJob: Job? = null

    val current: Note get() = notes.firstOrNull { it.id == currentId } ?: notes.first()

    init {
        val snap = Storage.load(app)
        notes = snap.notes.ifEmpty { listOf(Note()) }
        settings = snap.settings
        currentId = snap.current?.takeIf { id -> notes.any { it.id == id } } ?: notes.first().id
        field = TextFieldValue(current.text)
    }

    fun toast(text: String) { events.tryEmit(Msg(text)) }

    // ---------- persistence ----------
    private fun schedule() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { delay(400); persistNow() }
    }

    fun persistNow() = Storage.save(getApplication(), notes, currentId, settings)
    override fun onCleared() { persistNow() }

    fun updateSettings(f: (Settings) -> Settings) { settings = f(settings); schedule() }

    // ---------- editing ----------
    private fun pushUndo(old: TextFieldValue, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || undoStack.isEmpty() || now - lastPush > 700) {
            undoStack.addLast(old)
            if (undoStack.size > 100) undoStack.removeFirst()
        }
        lastPush = now
    }

    private fun syncText() {
        val now = System.currentTimeMillis()
        notes = notes.map { if (it.id == currentId) it.copy(text = field.text, dirty = true, updated = now) else it }
        schedule()
    }

    fun onChange(v: TextFieldValue) {
        if (v.text != field.text) {
            pushUndo(field)
            redoStack.clear()
            field = v
            syncText()
        } else {
            field = v
        }
    }

    private fun restore(v: TextFieldValue) { field = v; syncText(); focusTick++ }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return toast("لا شيء للتراجع")
        redoStack.addLast(field); restore(prev)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return toast("لا شيء للإعادة")
        undoStack.addLast(field); restore(next)
    }

    private fun replaceRange(start: Int, end: Int, new: String, select: Boolean) {
        pushUndo(field, force = true)
        redoStack.clear()
        val t = field.text.replaceRange(start, end, new)
        field = TextFieldValue(t, if (select) TextRange(start, start + new.length) else TextRange(start + new.length))
        syncText(); focusTick++
    }

    fun selectAll() { field = field.copy(selection = TextRange(0, field.text.length)); focusTick++ }

    fun applyTool(fn: (String) -> String) {
        val text = field.text
        val sel = field.selection
        val whole = sel.collapsed
        val s = if (whole) 0 else sel.min
        val e = if (whole) text.length else sel.max
        if (s == e) return toast("لا يوجد نص")
        val old = text.substring(s, e)
        val new = fn(old)
        if (new == old) return toast("لا تغيير")
        replaceRange(s, e, new, true)
        toast("تم التطبيق على " + if (whole) "كل النص" else "التحديد")
    }

    fun insert(t: String) = replaceRange(field.selection.min, field.selection.max, t, false)
    fun insertDate() = insert(SimpleDateFormat("EEEE d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn")).format(Date()))
    fun insertTime() = insert(SimpleDateFormat("HH:mm", Locale.forLanguageTag("ar-u-nu-latn")).format(Date()))
    fun insertSeparator() = insert("\n" + "─".repeat(30) + "\n")

    fun clearAll() {
        if (field.text.isEmpty()) return
        replaceRange(0, field.text.length, "", false)
        events.tryEmit(Msg("تم مسح النص", "تراجع") { undo() })
    }

    // ---------- find & replace ----------
    private fun buildRegex(): Regex? {
        if (findText.isEmpty()) return null
        val opts = mutableSetOf(RegexOption.MULTILINE)
        if (!matchCase) opts.add(RegexOption.IGNORE_CASE)
        return runCatching { Regex(if (useRegex) findText else Regex.escape(findText), opts) }.getOrNull()
    }

    private fun matches(): List<MatchResult> =
        buildRegex()?.findAll(field.text)?.filter { it.value.isNotEmpty() }?.toList() ?: emptyList()

    /** -1 = invalid regex */
    fun matchCount(): Int {
        if (findText.isEmpty()) return 0
        if (buildRegex() == null) return -1
        return matches().size
    }

    private fun replacementFor(r: Regex, input: String): String =
        r.replace(input, if (useRegex) replaceText else Regex.escapeReplacement(replaceText))

    fun findNext(forward: Boolean) {
        val m = matches()
        if (m.isEmpty()) return toast("لا توجد نتائج")
        val sel = field.selection
        val hit = if (forward) {
            m.firstOrNull { it.range.first >= sel.max } ?: m.first()
        } else {
            m.lastOrNull { it.range.last + 1 <= sel.min } ?: m.last()
        }
        field = field.copy(selection = TextRange(hit.range.first, hit.range.last + 1))
        focusTick++
    }

    fun replaceOne() {
        val r = buildRegex() ?: return
        val sel = field.selection
        if (!sel.collapsed) {
            val seg = field.text.substring(sel.min, sel.max)
            if (r.matchEntire(seg) != null) replaceRange(sel.min, sel.max, replacementFor(r, seg), false)
        }
        findNext(true)
    }

    fun replaceAll() {
        val r = buildRegex() ?: return
        val n = matches().size
        if (n == 0) return toast("لا توجد نتائج")
        replaceRange(0, field.text.length, replacementFor(r, field.text), false)
        toast("تم استبدال $n موضع")
    }

    // ---------- files ----------
    fun newNote(name: String = "ملف جديد", text: String = "") {
        val n = Note(name = name, text = text)
        notes = notes + n
        switchTo(n.id)
    }

    fun switchTo(id: String) {
        currentId = id
        field = TextFieldValue(current.text)
        undoStack.clear(); redoStack.clear()
        schedule()
    }

    fun deleteNote(id: String) {
        val removed = notes.firstOrNull { it.id == id } ?: return
        val idx = notes.indexOf(removed)
        var rest = notes - removed
        if (rest.isEmpty()) rest = listOf(Note())
        notes = rest
        if (currentId == id) switchTo(rest[idx.coerceAtMost(rest.size - 1)].id)
        schedule()
        events.tryEmit(Msg("حُذف «${removed.name}»", "تراجع") { notes = notes + removed; switchTo(removed.id) })
    }

    fun rename(name: String) {
        notes = notes.map { if (it.id == currentId) it.copy(name = name) else it }
        schedule()
    }

    fun importFile(name: String, text: String) {
        val c = current
        if (c.text.isEmpty() && !c.dirty) {
            notes = notes.map { if (it.id == currentId) it.copy(name = name, text = text, updated = System.currentTimeMillis()) else it }
            field = TextFieldValue(text)
            undoStack.clear(); redoStack.clear()
            schedule()
        } else newNote(name, text)
        toast("تم فتح «$name»")
    }

    fun decode(b: ByteArray): String {
        var bytes = b
        when (settings.readEnc) {
            ReadEnc.Utf8 -> if (b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte())
                bytes = b.copyOfRange(3, b.size)
            ReadEnc.Utf16 -> if (b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte())
                bytes = b.copyOfRange(2, b.size)
            else -> {}
        }
        return String(bytes, settings.readEnc.charset)
    }

    fun outputBytes(): ByteArray {
        var t = field.text.replace("\r\n", "\n")
        if (settings.crlf) t = t.replace("\n", "\r\n")
        val body = t.toByteArray(Charsets.UTF_8)
        return if (settings.saveBom) byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body else body
    }

    fun suggestedName(): String {
        val n = current.name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().ifEmpty { "ملف" }
        return if (n.endsWith(".txt", ignoreCase = true)) n else "$n.txt"
    }

    fun markSaved() {
        notes = notes.map { if (it.id == currentId) it.copy(dirty = false) else it }
        schedule()
        toast("تم الحفظ")
    }
}
