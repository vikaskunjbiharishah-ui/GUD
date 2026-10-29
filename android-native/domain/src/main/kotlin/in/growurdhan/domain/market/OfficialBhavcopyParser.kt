package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

/**
 * Official NSE/BSE UDiFF bhavcopy rows. Prices come only from a close column
 * in the file. A missing, zero, or future close is skipped, never replaced.
 */
object OfficialBhavcopyParser {
    private const val MAX_ROWS = 20_000
    private val isinPattern = Regex("^[A-Z]{2}[A-Z0-9]{9}[0-9]$")
    private val nseSeries = setOf("EQ", "BE", "SM", "ST", "GB")
    private val months = mapOf(
        "JAN" to "01", "FEB" to "02", "MAR" to "03", "APR" to "04",
        "MAY" to "05", "JUN" to "06", "JUL" to "07", "AUG" to "08",
        "SEP" to "09", "OCT" to "10", "NOV" to "11", "DEC" to "12",
    )

    data class Parsed(val rows: List<CachedListedSecurity>, val understood: Boolean)

    fun parse(csv: String, exchange: OfficialMarketSource, today: LocalDate): Parsed {
        require(exchange == OfficialMarketSource.NSE || exchange == OfficialMarketSource.BSE)
        require(csv.length <= 12_000_000) { "Bhavcopy is too large." }
        require(!csv.contains("<html", ignoreCase = true)) { "Received HTML instead of a bhavcopy." }
        val lines = csv.lineSequence().map { it.trim('\uFEFF', ' ', '\r') }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return Parsed(emptyList(), false)
        val header = splitCsv(lines.first()).map { it.trim().trim('"').uppercase(Locale.US) }
        fun idx(name: String) = header.indexOf(name)
        val isinIdx = idx("ISIN")
        val closeIdx = idx("CLSPRIC").let { if (it >= 0) it else idx("CLOSE") }
        if (isinIdx < 0 || closeIdx < 0) return Parsed(emptyList(), false)
        val symbolIdx = idx("TCKRSYMB").let { if (it >= 0) it else idx("SYMBOL") }
        val nameIdx = idx("FININSTRMNM")
        val seriesIdx = idx("SCTYSRS").let { if (it >= 0) it else idx("SERIES") }
        val typeIdx = idx("FININSTRMTP")
        val dateIdx = listOf(idx("TRADDT"), idx("BIZDT"), idx("TIMESTAMP")).firstOrNull { it >= 0 } ?: -1
        val rows = ArrayList<CachedListedSecurity>()
        val seen = HashSet<String>()
        for (line in lines.drop(1)) {
            val cols = splitCsv(line)
            if (cols.size <= maxOf(isinIdx, closeIdx)) continue
            val isin = raw(cols, isinIdx).uppercase(Locale.US)
            if (!isinPattern.matches(isin)) continue
            val series = raw(cols, seriesIdx).uppercase(Locale.US)
            if (exchange == OfficialMarketSource.NSE && series.isNotEmpty() && series !in nseSeries) continue
            if (exchange == OfficialMarketSource.BSE && typeIdx >= 0) {
                val kind = raw(cols, typeIdx).uppercase(Locale.US)
                if (kind.isNotEmpty() && kind != "STK") continue
            }
            val price = runCatching {
                MoneyScaled.fromRupees(BigDecimal(raw(cols, closeIdx).replace(",", "")))
            }.getOrNull()?.takeIf { it.raw > 0 } ?: continue
            val day = parseDay(raw(cols, dateIdx)) ?: continue
            if (day.isAfter(today)) continue
            val symbol = raw(cols, symbolIdx).uppercase(Locale.US).ifEmpty { null }
            val name = raw(cols, nameIdx).ifEmpty { symbol }
            if (name.isNullOrBlank() || name.length > 200 || name.any { it.isISOControl() }) continue
            if (symbol != null && (symbol.length > 64 || symbol.any { it.isISOControl() })) continue
            if (!seen.add(isin)) continue
            rows += CachedListedSecurity(
                isin = isin,
                name = name,
                category = if (series == "GB") "GOLD_SGB" else "EQUITY",
                source = exchange,
                closeDate = day,
                closePrice = price,
                symbol = symbol,
            )
            require(rows.size <= MAX_ROWS) { "Too many bhavcopy rows." }
        }
        return Parsed(rows, true)
    }

    private fun raw(cols: List<String>, index: Int): String =
        if (index >= 0 && index < cols.size) cols[index].trim().trim('"') else ""

    private fun parseDay(raw: String): LocalDate? {
        val t = raw.trim()
        if (t.length >= 10 && t[4] == '-' && t[7] == '-') {
            return runCatching { LocalDate.parse(t.take(10)) }.getOrNull()
        }
        val parts = t.uppercase(Locale.ENGLISH).split('-')
        if (parts.size < 3) return null
        val dd = parts[0].filter { it.isDigit() }.padStart(2, '0')
        val mon = months[parts[1].take(3)] ?: return null
        val yyyy = parts[2].filter { it.isDigit() }.take(4)
        if (dd.length != 2 || yyyy.length != 4) return null
        return runCatching { LocalDate.parse("$yyyy-$mon-$dd") }.getOrNull()
    }

    private fun splitCsv(line: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var quoted = false
        for (ch in line) {
            when {
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    out += cur.toString()
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        out += cur.toString()
        return out
    }
}
