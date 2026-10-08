@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.notepadpro

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD4A847), onPrimary = Color(0xFF0F0E0C),
    primaryContainer = Color(0xFF3A2F14), onPrimaryContainer = Color(0xFFE8C97A),
    secondaryContainer = Color(0xFF3A2F14), onSecondaryContainer = Color(0xFFE8C97A),
    background = Color(0xFF0F0E0C), onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF1A1916), onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF242220), onSurfaceVariant = Color(0xFF9A9488),
    outline = Color(0xFF2E2C28), outlineVariant = Color(0xFF2E2C28), error = Color(0xFFE05C5C)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFA67A12), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3B8), onPrimaryContainer = Color(0xFF5B4208),
    secondaryContainer = Color(0xFFF3E3B8), onSecondaryContainer = Color(0xFF5B4208),
    background = Color(0xFFF3F1EC), onBackground = Color(0xFF201D17),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF201D17),
    surfaceVariant = Color(0xFFECE9E2), onSurfaceVariant = Color(0xFF5D574A),
    outline = Color(0xFFD9D4C8), outlineVariant = Color(0xFFD9D4C8), error = Color(0xFFB3261E)
)

@Composable
fun NotePadApp(vm: NoteViewModel) {
    val dark = when (vm.settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Main(vm) }
    }
}

private fun Context.displayName(uri: Uri): String =
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        }
    }.getOrNull()?.substringBeforeLast('.') ?: "ملف"

@Composable
private fun Main(vm: NoteViewModel) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val snack = remember { SnackbarHostState() }
    val focus = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    var showTools by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.events.collect { m ->
            val r = snack.showSnackbar(
                message = m.text,
                actionLabel = m.actionLabel,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) m.action?.invoke()
        }
    }
    LaunchedEffect(vm.focusTick) { if (vm.focusTick > 0) runCatching { focus.requestFocus() } }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(vm.outputBytes()) }
            }.onSuccess { vm.markSaved() }.onFailure { vm.toast("تعذّر الحفظ") }
        }
    }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching {
                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                vm.importFile(ctx.displayName(uri), vm.decode(bytes))
            }.onFailure { vm.toast("تعذّر قراءة الملف") }
        }
    }
    val doSave = {
        if (vm.field.text.isEmpty()) vm.toast("الملف فارغ") else saver.launch(vm.suggestedName())
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = cs.surface) {
                NotesPane(vm) { scope.launch { drawer.close() } }
            }
        }
    ) {
        Scaffold(
            containerColor = cs.background,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "الملفات") }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = vm.current.name,
                                onValueChange = vm::rename,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary),
                                modifier = Modifier.weight(1f, fill = false).widthIn(min = 60.dp)
                            )
                            Text(".txt", color = cs.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.showFind = !vm.showFind }) { Icon(Icons.Default.Search, "بحث") }
                        Button(onClick = { doSave() }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("حفظ .txt") }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "المزيد") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("فتح ملف") }, onClick = { menu = false; opener.launch(arrayOf("*/*")) })
                                DropdownMenuItem(text = { Text("ملف جديد") }, onClick = { menu = false; vm.newNote() })
                                DropdownMenuItem(text = { Text("الإعدادات") }, onClick = { menu = false; showSettings = true })
                            }
                        }
                    }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
                if (vm.showFind) FindBar(vm)
                Editor(vm, focus, Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(onClick = { vm.undo() }) { Text("↶ تراجع") }
                    TextButton(onClick = { vm.redo() }) { Text("↷ إعادة") }
                    TextButton(onClick = { showTools = true }) { Text("أدوات النص") }
                    TextButton(onClick = { vm.selectAll() }) { Text("تحديد الكل") }
                    TextButton(onClick = {
                        if (vm.field.text.isEmpty()) vm.toast("لا يوجد نص")
                        else { clipboard.setText(AnnotatedString(vm.field.text)); vm.toast("تم نسخ النص") }
                    }) { Text("نسخ الكل") }
                }
                StatusBar(vm)
            }
        }
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, containerColor = cs.surface) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "تُطبَّق على النص المحدد، وإن لم تحدد شيئاً فعلى كل النص.",
                    color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                )
                TextTools.groups.forEach { (title, tools) ->
                    SectionTitle(title)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tools.forEach { t ->
                            AssistChip(onClick = { vm.applyTool(t.fn); showTools = false }, label = { Text(t.label) })
                        }
                    }
                }
                SectionTitle("إدراج")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { vm.insertDate(); showTools = false }, label = { Text("التاريخ") })
                    AssistChip(onClick = { vm.insertTime(); showTools = false }, label = { Text("الوقت") })
                    AssistChip(onClick = { vm.insertSeparator(); showTools = false }, label = { Text("خط فاصل") })
                    AssistChip(onClick = { vm.clearAll(); showTools = false }, label = { Text("مسح كل النص") })
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = cs.surface) {
            SettingsPane(vm)
        }
    }
}

@Composable
private fun SectionTitle(t: String) =
    Text(t, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))

@Composable
private fun Editor(vm: NoteViewModel, focus: FocusRequester, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val s = vm.settings
    val style = TextStyle(
        color = cs.onSurface,
        fontSize = s.fontSize.sp,
        lineHeight = (s.fontSize * s.lineHeight).sp,
        fontFamily = when (s.font) {
            FontKind.Default -> FontFamily.Default
            FontKind.Serif -> FontFamily.Serif
            FontKind.Mono -> FontFamily.Monospace
        },
        textDirection = when (s.dir) {
            DirMode.Auto -> TextDirection.Content
            DirMode.Rtl -> TextDirection.Rtl
            DirMode.Ltr -> TextDirection.Ltr
        },
        textAlign = TextAlign.Start
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val h = maxHeight
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            BasicTextField(
                value = vm.field,
                onValueChange = vm::onChange,
                textStyle = style,
                cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = h).focusRequester(focus).padding(16.dp),
                decorationBox = { inner ->
                    Box {
                        if (vm.field.text.isEmpty()) {
                            Text("ابدأ الكتابة هنا…", style = style.copy(color = cs.onSurfaceVariant))
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun FindBar(vm: NoteViewModel) {
    val cs = MaterialTheme.colorScheme
    val count = remember(vm.findText, vm.matchCase, vm.useRegex, vm.field.text) { vm.matchCount() }
    Surface(color = cs.surfaceVariant) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.findText, onValueChange = { vm.findText = it }, singleLine = true,
                    placeholder = { Text("ابحث عن…") }, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vm.replaceText, onValueChange = { vm.replaceText = it }, singleLine = true,
                    placeholder = { Text("استبدل بـ…") }, modifier = Modifier.weight(1f)
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.Center) {
                TextButton(onClick = { vm.findNext(true) }) { Text("التالي") }
                TextButton(onClick = { vm.findNext(false) }) { Text("السابق") }
                TextButton(onClick = { vm.replaceOne() }) { Text("استبدال") }
                TextButton(onClick = { vm.replaceAll() }) { Text("استبدال الكل") }
                FilterChip(selected = vm.matchCase, onClick = { vm.matchCase = !vm.matchCase }, label = { Text("مطابقة الحالة") })
                FilterChip(selected = vm.useRegex, onClick = { vm.useRegex = !vm.useRegex }, label = { Text("تعبير نمطي") })
                Text(
                    when {
                        count < 0 -> "تعبير غير صالح"
                        vm.findText.isEmpty() -> ""
                        count == 0 -> "لا نتائج"
                        else -> "$count نتيجة"
                    },
                    color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { vm.showFind = false }) { Icon(Icons.Default.Close, "إغلاق") }
            }
        }
    }
}

@Composable
private fun StatusBar(vm: NoteViewModel) {
    val cs = MaterialTheme.colorScheme
    val text = vm.field.text
    val words = remember(text) { val t = text.trim(); if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size }
    val lines = remember(text) { if (text.isEmpty()) 0 else text.count { it == '\n' } + 1 }
    val before = text.substring(0, vm.field.selection.start.coerceIn(0, text.length))
    val line = before.count { it == '\n' } + 1
    val col = before.length - (before.lastIndexOf('\n') + 1) + 1
    val dirty = vm.current.dirty
    Surface(color = cs.surface) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val st = MaterialTheme.typography.labelSmall
            Text(if (dirty) "● لم يُحفظ كملف" else "تم الحفظ", style = st, color = if (dirty) cs.primary else cs.onSurfaceVariant)
            Text("كلمات $words", style = st, color = cs.onSurfaceVariant)
            Text("أحرف ${text.length}", style = st, color = cs.onSurfaceVariant)
            Text("أسطر $lines", style = st, color = cs.onSurfaceVariant)
            Text("قراءة ${(words + 199) / 200} د", style = st, color = cs.onSurfaceVariant)
            Text("سطر $line · عمود $col", style = st, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotesPane(vm: NoteViewModel, onPick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var q by remember { mutableStateOf("") }
    val df = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("ar-u-nu-latn")) }
    val shown = vm.notes
        .filter { q.isBlank() || (it.name + " " + it.text).contains(q, ignoreCase = true) }
        .sortedByDescending { it.updated }
    Column(Modifier.fillMaxHeight().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ملفاتي", style = MaterialTheme.typography.titleMedium, color = cs.primary, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = { vm.newNote(); onPick() }) { Icon(Icons.Default.Add, "ملف جديد") }
        }
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("ابحث في الملفات…") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shown, key = { it.id }) { n ->
                val active = n.id == vm.currentId
                Surface(
                    onClick = { vm.switchTo(n.id); onPick() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) cs.primaryContainer else cs.surfaceVariant
                ) {
                    Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.name.ifEmpty { "بدون اسم" }, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = if (active) cs.onPrimaryContainer else cs.onSurface
                            )
                            Text(
                                n.text.take(60).replace('\n', ' '), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp, color = cs.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(df.format(Date(n.updated)), fontSize = 11.sp, color = cs.onSurfaceVariant)
                                if (n.dirty) Text("● لم يُحفظ", fontSize = 11.sp, color = cs.primary)
                            }
                        }
                        IconButton(onClick = { vm.deleteNote(n.id) }) { Icon(Icons.Default.Delete, "حذف", tint = cs.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(title: String, options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun SettingsPane(vm: NoteViewModel) {
    val s = vm.settings
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
        ChipRow("المظهر", listOf("النظام" to ThemeMode.System, "داكن" to ThemeMode.Dark, "فاتح" to ThemeMode.Light), s.theme) {
            vm.updateSettings { c -> c.copy(theme = it) }
        }
        SectionTitle("حجم الخط")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.updateSettings { c -> c.copy(fontSize = (c.fontSize - 1).coerceAtLeast(12)) } }) { Text("−") }
            Text("${s.fontSize}")
            OutlinedButton(onClick = { vm.updateSettings { c -> c.copy(fontSize = (c.fontSize + 1).coerceAtMost(40)) } }) { Text("+") }
        }
        ChipRow("ارتفاع السطر", listOf("ضيق" to 1.4f, "عادي" to 1.7f, "مريح" to 1.9f, "واسع" to 2.3f), s.lineHeight) {
            vm.updateSettings { c -> c.copy(lineHeight = it) }
        }
        ChipRow("الخط", listOf("افتراضي" to FontKind.Default, "سيريف" to FontKind.Serif, "ثابت العرض" to FontKind.Mono), s.font) {
            vm.updateSettings { c -> c.copy(font = it) }
        }
        ChipRow("اتجاه الكتابة", listOf("تلقائي" to DirMode.Auto, "من اليمين" to DirMode.Rtl, "من اليسار" to DirMode.Ltr), s.dir) {
            vm.updateSettings { c -> c.copy(dir = it) }
        }
        ChipRow("ترميز الحفظ", listOf("UTF-8" to false, "UTF-8 مع BOM (للمفكرة)" to true), s.saveBom) {
            vm.updateSettings { c -> c.copy(saveBom = it) }
        }
        ChipRow("نهاية السطر", listOf("LF" to false, "CRLF (ويندوز)" to true), s.crlf) {
            vm.updateSettings { c -> c.copy(crlf = it) }
        }
        ChipRow(
            "ترميز قراءة الملفات",
            listOf("UTF-8" to ReadEnc.Utf8, "عربي قديم (Windows-1256)" to ReadEnc.Win1256, "UTF-16" to ReadEnc.Utf16),
            s.readEnc
        ) { vm.updateSettings { c -> c.copy(readEnc = it) } }
    }
}
