package com.akrishna87.expiro.data

import java.time.LocalDate
import java.time.YearMonth

/** Finds an expiry date in text read from a product pack (OCR output). Dates are read as day/month/year. */
object DateParser {
    private val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    private const val MON = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC"
    private const val SEP = """\s*[./\-,]?\s*"""

    private val numericFull = Regex("""(?<!\d)(\d{1,2})\s*[./\-]\s*(\d{1,2})\s*[./\-]\s*(\d{4}|\d{2})(?!\d)""")
    private val dayMonthNameYear = Regex("""(?<!\d)(\d{1,2})$SEP($MON)[A-Z]*\.?$SEP(\d{4}|\d{2})(?!\d)""")
    private val monthNameYear = Regex("""(?<![A-Z\d])($MON)[A-Z]*\.?$SEP(\d{4}|\d{2})(?!\d)""")
    private val monthNumYear = Regex("""(?<!\d)(\d{1,2})\s*[./\-]\s*(\d{4})(?!\d)""")
    private val keywords = Regex("""EXP|USE\s*BY|BEST\s*BEFORE|BBE|\bBB\b|BEST\s*BY|EXPIRY|VALID""")

    private data class Hit(val date: LocalDate, val range: IntRange) {
        val start get() = range.first
    }

    fun findExpiry(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
        val t = text.uppercase().replace('\n', ' ')
        val hits = mutableListOf<Hit>()

        for (m in numericFull.findAll(t)) {
            var d = m.groupValues[1].toInt()
            var mo = m.groupValues[2].toInt()
            if (mo > 12 && d <= 12) { val x = d; d = mo; mo = x } // US style month/day
            toDate(year(m.groupValues[3]), mo, d)?.let { hits += Hit(it, m.range) }
        }
        for (m in dayMonthNameYear.findAll(t)) {
            toDate(year(m.groupValues[3]), months.indexOf(m.groupValues[2]) + 1, m.groupValues[1].toInt())
                ?.let { hits += Hit(it, m.range) }
        }
        // Month + year only ("BB 11.2027"): ignore when it is just part of a full date found above.
        val fullRanges = hits.map { it.range }
        fun insideFull(r: IntRange) = fullRanges.any { it.first <= r.last && r.first <= it.last }
        for (m in monthNameYear.findAll(t)) {
            if (insideFull(m.range)) continue
            monthEnd(year(m.groupValues[2]), months.indexOf(m.groupValues[1]) + 1)?.let { hits += Hit(it, m.range) }
        }
        for (m in monthNumYear.findAll(t)) {
            if (insideFull(m.range)) continue
            monthEnd(m.groupValues[2].toInt(), m.groupValues[1].toInt())?.let { hits += Hit(it, m.range) }
        }

        val plausible = hits.filter { it.date.year in (today.year - 1)..(today.year + 15) }
        if (plausible.isEmpty()) return null

        // Prefer dates written right after EXP / USE BY / BEST BEFORE; otherwise the latest date
        // (a manufacturing date is always earlier than the expiry date).
        val labelled = plausible.filter { hit ->
            keywords.containsMatchIn(t.substring(maxOf(0, hit.start - 25), hit.start))
        }
        return (labelled.ifEmpty { plausible }).maxOf { it.date }
    }

    private fun year(s: String): Int = if (s.length == 2) 2000 + s.toInt() else s.toInt()

    private fun toDate(y: Int, m: Int, d: Int): LocalDate? =
        try { LocalDate.of(y, m, d) } catch (e: Exception) { null }

    private fun monthEnd(y: Int, m: Int): LocalDate? =
        try { YearMonth.of(y, m).atEndOfMonth() } catch (e: Exception) { null }
}

/** Reads expiry dates and product numbers from GS1 barcodes (medicine packs, trade packs). */
object Gs1 {
    private val fixedLength = mapOf(
        "00" to 18, "01" to 14, "02" to 14, "11" to 6, "12" to 6, "13" to 6, "15" to 6, "16" to 6, "17" to 6, "20" to 2,
    )
    private val variableLength = setOf("10", "21", "22", "30", "37")
    private const val GS = '\u001d'

    fun parseExpiry(raw: String): LocalDate? {
        val text = raw.trim()
        Regex("""\(17\)(\d{6})""").find(text)?.let { return toDate(it.groupValues[1]) }
        Regex("""\(15\)(\d{6})""").find(text)?.let { return toDate(it.groupValues[1]) }
        if (text.length < 18 || !text.startsWith("01")) return null

        var i = 0
        var bestBefore: LocalDate? = null
        while (i + 2 <= text.length) {
            val ai = text.substring(i, i + 2)
            val len = fixedLength[ai]
            if (len != null) {
                if (i + 2 + len > text.length) break
                val value = text.substring(i + 2, i + 2 + len)
                if (!value.all(Char::isDigit)) break
                if (ai == "17") return toDate(value)
                if (ai == "15") bestBefore = toDate(value)
                i += 2 + len
            } else if (ai in variableLength) {
                val end = text.indexOf(GS, i + 2)
                if (end < 0) break
                i = end + 1
            } else break
        }
        return bestBefore
    }

    /** The product number (as EAN/UPC digits) if the code is a GS1 element string, else null. */
    fun productNumber(raw: String): String? {
        val text = raw.trim()
        val gtin = Regex("""\(01\)(\d{14})""").find(text)?.groupValues?.get(1)
            ?: if (text.length >= 18 && text.startsWith("01") && text.substring(2, 16).all(Char::isDigit)) {
                text.substring(2, 16)
            } else return null
        return gtin.trimStart('0').padStart(8, '0')
    }

    private fun toDate(yymmdd: String): LocalDate? = try {
        val y = 2000 + yymmdd.substring(0, 2).toInt()
        val m = yymmdd.substring(2, 4).toInt()
        val d = yymmdd.substring(4, 6).toInt()
        if (d == 0) java.time.YearMonth.of(y, m).atEndOfMonth() else LocalDate.of(y, m, d)
    } catch (e: Exception) {
        null
    }
}
