

```markdown
# NotePad Pro 📝

تطبيق ملاحظات احترافي وعصري لنظام Android، مبني بـ **Jetpack Compose** و **Kotlin**، يوفر تجربة مستخدم متقدمة مع دعم كامل للعربية و RTL.

---

## ✨ المميزات الرئيسية

### 📝 التحرير المتقدم
- محرر نصوص قوي مع دعم البحث والاستبدال
- undo/redo غير محدود
- معاينة مباشرة للنصوص
- حفظ تلقائي للملاحظات

### 🎨 التصميم والواجهة
- تصميم عصري يستخدم Material Design 3
- أوضاع عرض متعددة (ليلي/نهاري/نظام)
- دعم كامل للعربية و RTL
- واجهة سلسة وسريعة جداً

### 🔧 خيارات متقدمة
- **تخصيص الخطوط**: حجم، نوع (Default/Serif/Mono)، ارتفاع السطر
- **معالجة النصوص**: تنسيق، تنظيف، ترتيب، تحويل أرقام
- **ترميز الملفات**: UTF-8، UTF-16، Windows-1256
- **تنسيق الأسطر**: LF أو CRLF (ويندوز)

### 📁 إدارة الملفات
- إنشاء وحذف وإعادة تسمية الملاحظات
- فتح ملفات من التخزين
- حفظ ملفات بصيغة TXT
- البحث في الملاحظات

### 🔍 البحث والاستبدال
- بحث عادي وتعابير نمطية (Regex)
- مطابقة الحالة (case-sensitive)
- استبدال فردي أو جماعي
- عداد النتائج

### 🛠️ أدوات النصوص المرفقة
- **ترتيب الأسطر**: أبجدي عكسي، عكس الترتيب
- **التنظيف**: دمج المسافات، حذف التشكيل، توحيد الألف والياء
- **تحويل الأرقام**: عربي ↔ إنجليزي
- **تحويل الأحرف**: أحرف كبيرة/صغيرة
- **إدراج**: التاريخ، الوقت، خط فاصل

### 📊 شريط الحالة
- عداد الكلمات والأحرف والأسطر
- رقم السطر والعمود الحالي
- مؤشر الحفظ
- تقدير وقت القراءة

---

## 🛠️ المتطلبات والمعايير

| المتطلب | القيمة |
|--------|--------|
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |
| **Compile SDK** | 34 |
| **Java Version** | 17 |
| **Kotlin** | 2.0.20 |
| **Gradle** | 8.5.2 |
| **Android Studio** | 2024.1+ |

---

## 🏗️ بنية المشروع الكاملة

```
NotePadPro/
│
├── gradle/                              # ملفات Gradle Wrapper
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       ├── gradle-wrapper.properties
│       ├── gradlew
│       └── gradlew.bat
│
├── app/                                 # 🎯 وحدة التطبيق الرئيسية
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml      # ملف الإظهار
│   │   │   │
│   │   │   ├── java/com/notepadpro/     # 📦 الكود المصدري
│   │   │   │   ├── MainActivity.kt      # النشاط الرئيسي
│   │   │   │   ├── NotePadApp.kt        # واجهة المستخدم (Compose)
│   │   │   │   ├── NoteViewModel.kt     # إدارة الحالة
│   │   │   │   ├── Models.kt            # نماذج البيانات
│   │   │   │   └── TextTools.kt         # أدوات معالجة النصوص
│   │   │   │
│   │   │   └── res/                     # 🎨 الموارد
│   │   │       ├── mipmap/              # الأيقونات
│   │   │       │   └── ic_luncher.xml
│   │   │       └── values/              # القيم والثوابت
│   │   │           ├── colors.xml
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   │   │
│   │   ├── test/                        # اختبارات الوحدة
│   │   └── androidTest/                 # اختبارات الجهاز
│   │
│   ├── build/                           # مخرجات البناء
│   ├── build.gradle.kts                 # ملف البناء
│   ├── .classpath                       # ملف IDE
│   └── .project
│
├── build.gradle.kts                     # بناء جذري
├── settings.gradle.kts                  # إعدادات المشروع
├── gradle.properties                    # خصائص Gradle
├── .gitignore
└── README.md
```

---

## 📋 شرح الملفات الرئيسية

### 🎯 MainActivity.kt
**النشاط الرئيسي - نقطة دخول التطبيق**

```kotlin
class MainActivity : ComponentActivity() {
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NotePadApp(vm) }
    }

    override fun onStop() {
        super.onStop()
        vm.persistNow()
    }
}
```

**المسؤوليات:**
- تهيئة البيئة الأساسية
- ربط واجهة Compose
- حفظ البيانات عند الخروج

---

### 🎨 NotePadApp.kt
**واجهة المستخدم الرئيسية - الكود الأساسي**

```kotlin
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
```

**المكونات الرئيسية:**
- `Main()` - الواجهة الرئيسية مع Drawer و Scaffold
- `Editor()` - محرر النصوص
- `FindBar()` - بحث واستبدال
- `StatusBar()` - شريط الحالة
- `NotesPane()` - قائمة الملاحظات الجانبية
- `SettingsPane()` - إعدادات التطبيق
- `SectionTitle()` - عنوان القسم

**الألوان المخصصة:**
```kotlin
// الوضع الليلي (Dark)
primary = Color(0xFFD4A847)          // ذهبي
background = Color(0xFF0F0E0C)       // أسود عميق
surface = Color(0xFF1A1916)          // رمادي داكن

// الوضع النهاري (Light)
primary = Color(0xFFA67A12)          // بني ذهبي
background = Color(0xFFF3F1EC)       # أبيض كريمي
surface = Color(0xFFFFFFFF)          # أبيض نقي
```

---

### 📊 NoteViewModel.kt
**إدارة حالة البيانات - الكود الأساسي**

```kotlin
class NoteViewModel(app: Application) : AndroidViewModel(app) {
    var notes by mutableStateOf(listOf<Note>())
    var currentId by mutableStateOf("")
    var settings by mutableStateOf(Settings())
    var field by mutableStateOf(TextFieldValue(""))
    
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    
    val current: Note get() = notes.firstOrNull { it.id == currentId } ?: notes.first()
}
```

**الوظائف الأساسية:**

| الوظيفة | الوصف |
|-------|-------|
| `onChange(v)` | معالج تغيير النص مع undo/redo |
| `undo()` / `redo()` | تراجع وإعادة |
| `newNote()` | إنشاء ملاحظة جديدة |
| `switchTo(id)` | التبديل بين الملاحظات |
| `deleteNote(id)` | حذف ملاحظة |
| `rename(name)` | إعادة تسمية الملاحظة |
| `findNext()` | البحث التالي |
| `replaceOne()` / `replaceAll()` | الاستبدال |
| `applyTool()` | تطبيق أدوات النص |
| `persistNow()` | حفظ البيانات |
| `importFile()` | فتح ملف خارجي |
| `outputBytes()` | تحويل للـ bytes للحفظ |

---

### 🗂️ Models.kt
**نماذج البيانات - البيانات الأساسية**

```kotlin
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "ملف جديد",
    val text: String = "",
    val updated: Long = System.currentTimeMillis(),
    val dirty: Boolean = false
)

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

enum class ThemeMode { System, Dark, Light }
enum class DirMode { Auto, Rtl, Ltr }
enum class FontKind { Default, Serif, Mono }
enum class ReadEnc(val charset: Charset) {
    Utf8(Charsets.UTF_8),
    Win1256(Charset.forName("windows-1256")),
    Utf16(Charsets.UTF_16LE)
}
```

**نظام التخزين:**
```kotlin
object Storage {
    fun load(ctx: Context): Snapshot { ... }
    fun save(ctx: Context, notes, current, settings) { ... }
}
// يستخدم SharedPreferences مع JSON
```

---

### 🔧 TextTools.kt
**أدوات معالجة النصوص - مجموعات الأدوات**

```kotlin
object TextTools {
    val groups: List<Pair<String, List<Tool>>> = listOf(
        "الأسطر" to listOf(
            Tool("ترتيب أ ← ي") { /* ترتيب أبجدي */ },
            Tool("عكس الأسطر") { /* عكس الترتيب */ },
            Tool("حذف الأسطر الفارغة") { /* تنظيف */ },
            // ... المزيد
        ),
        "التنظيف" to listOf(
            Tool("دمج المسافات") { /* توحيد المسافات */ },
            Tool("حذف التشكيل") { /* إزالة التشكيل */ },
            Tool("توحيد الألف والياء") { /* توحيد */ },
            // ... المزيد
        ),
        "الحروف والأرقام" to listOf(
            Tool("أرقام عربية ١٢٣") { /* تحويل */ },
            Tool("ABC أحرف كبيرة") { /* uppercase */ },
            // ... المزيد
        )
    )
}
```

**الأدوات المتاحة:**

**الأسطر:**
- ترتيب أبجدي / عكسي
- عكس ترتيب الأسطر
- حذف الأسطر الفارغة والمكررة
- قص المسافات الزائدة
- ترقيم الأسطر
- دمج في فقرة

**التنظيف:**
- دمج المسافات المتعددة
- حذف التشكيل (الفتحة، الضمة، إلخ)
- حذف التطويل (ـ)
- توحيد الألف والياء

**الحروف والأرقام:**
- تحويل أرقام عربي ↔ إنجليزي
- أحرف كبيرة / صغيرة
- Title Case

---

## 📦 المكتبات المستخدمة

```gradle
// AndroidX
androidx.core:core-ktx:1.13.1
androidx.activity:activity-compose:1.9.2
androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6

// Jetpack Compose BOM
androidx.compose:compose-bom:2024.09.03

// Compose Components
androidx.compose.ui:ui
androidx.compose.foundation:foundation
androidx.compose.material3:material3
androidx.compose.material:material-icons-core
```

---

## 🚀 البدء السريع

### الخطوة 1: استنساخ المشروع
```bash
git clone https://github.com/sakalalalahalibo/NotePad-Pro.git
cd NotePad-Pro
```

### الخطوة 2: فتح في Android Studio
```
File → Open → اختر مجلد المشروع
```

### الخطوة 3: انتظر المزامنة
Gradle سيزامن تلقائياً جميع المكتبات

### الخطوة 4: تشغيل
```bash
./gradlew installDebug
```

أو اضغط **Shift + F10** في Android Studio

---

## 🔧 أوامر Gradle

```bash
# البناء
./gradlew build
./gradlew assembleDebug
./gradlew assembleRelease

# التشغيل
./gradlew installDebug
./gradlew runDebug

# التنظيف
./gradlew clean
./gradlew cleanBuildCache

# الاختبارات
./gradlew test
./gradlew connectedAndroidTest
```

---

## 🎯 معلومات التطبيق

| المعلومة | القيمة |
|---------|--------|
| **Application ID** | com.notepadpro |
| **Version Code** | 1 |
| **Version Name** | 1.0 |
| **Label** | NotePad Pro |
| **RTL Support** | ✅ Yes |
| **Backup** | ✅ Enabled |

---

## 💾 نظام التخزين

- **محلي**: SharedPreferences مع JSON
- **صيغة**: JSON مشفر في SharedPreferences
- **الحفظ**: تلقائي عند مغادرة التطبيق أو بعد التعديل
- **الترميز المدعوم**: UTF-8, UTF-16, Windows-1256

---

## 🤝 المساهمة

1. Fork المشروع
2. إنشاء فرع للميزة (`git checkout -b feature/amazing`)
3. Commit التغييرات (`git commit -m 'Add feature'`)
4. Push للفرع (`git push origin feature/amazing`)
5. فتح Pull Request

---

## 📞 التواصل

- **المطور**: [sakalalalahalibo](https://github.com/sakalalalahalibo)
- **المشروع**: [NotePad-Pro](https://github.com/sakalalalahalibo/NotePad-Pro)
- **Issues**: [فتح مشكلة](https://github.com/sakalalalahalibo/NotePad-Pro/issues)

---

## 📝 الترخيص

مشروع مفتوح المصدر

---

## 🙏 شكر خاص

- فريق Google Android
- فريق Jetpack Compose
- فريق Kotlin

---

**⭐ إذا أعجبك المشروع، أضف نجمة!**

```
