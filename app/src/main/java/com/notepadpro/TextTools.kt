package com.notepadpro

import java.text.Collator
import java.util.Locale

class Tool(val label: String, val fn: (String) -> String)

object TextTools {
    private const val AR = "٠١٢٣٤٥٦٧٨٩"
    private val collator: Collator = Collator.getInstance(Locale("ar"))

    private fun lines(f: (List<String>) -> List<String>): (String) -> String =
        { t -> f(t.split("\n")).joinToString("\n") }

    val groups: List<Pair<String, List<Tool>>> = listOf(
        "الأسطر" to listOf(
            Tool("ترتيب أ ← ي", lines { l -> l.sortedWith(Comparator { a, b -> collator.compare(a, b) }) }),
            Tool("ترتيب ي ← أ", lines { l -> l.sortedWith(Comparator { a, b -> collator.compare(b, a) }) }),
            Tool("عكس الأسطر", lines { l -> l.reversed() }),
            Tool("حذف الأسطر الفارغة", lines { l -> l.filter { it.isNotBlank() } }),
            Tool("حذف المكرر", lines { l -> l.distinct() }),
            Tool("قص مسافات الأسطر", lines { l -> l.map { it.trim() } }),
            Tool("ترقيم الأسطر", lines { l -> l.mapIndexed { i, s -> "${i + 1}. $s" } }),
            Tool("دمج في فقرة") { t -> t.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") }
        ),
        "التنظيف" to listOf(
            Tool("دمج المسافات") { t -> t.replace(Regex("[ \\t\\u00A0]+"), " ").replace(Regex(" ?\\n ?"), "\n") },
            Tool("حذف التشكيل") { t -> t.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "") },
            Tool("حذف التطويل") { t -> t.replace("\u0640", "") },
            Tool("توحيد الألف والياء") { t -> t.replace(Regex("[أإآٱ]"), "ا").replace('ى', 'ي') }
        ),
        "الحروف والأرقام" to listOf(
            Tool("أرقام عربية ١٢٣") { t ->
                buildString { for (c in t) append(if (c in '0'..'9') AR[c - '0'] else c) }
            },
            Tool("أرقام إنجليزية 123") { t ->
                buildString {
                    for (c in t) append(
                        when (c) {
                            in '٠'..'٩' -> '0' + (c - '٠')
                            in '۰'..'۹' -> '0' + (c - '۰')
                            else -> c
                        }
                    )
                }
            },
            Tool("ABC أحرف كبيرة") { t -> t.uppercase() },
            Tool("abc أحرف صغيرة") { t -> t.lowercase() },
            Tool("Title Case") { t ->
                Regex("(^|\\s)(\\S)").replace(t.lowercase()) { m -> m.groupValues[1] + m.groupValues[2].uppercase() }
            }
        )
    )
}
