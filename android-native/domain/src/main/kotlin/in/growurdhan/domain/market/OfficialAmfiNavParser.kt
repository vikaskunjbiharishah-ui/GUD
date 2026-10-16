package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * Pure, bounded decoder for AMFI's published semicolon NAV report format.
 * Does NOT establish source authenticity: callers must download exclusively
 * from an AMFI official HTTPS URL, with no redirects or identity parameters.
 * Some AMFI reports use newer formats: unknown formats must fail closed.
 */
object OfficialAmfiNavParser {
    private const val MAX_ROWS = 50_000
    private val navDate = DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT)
    private val validIsin = Regex("INF[A-Z0-9]{9}")
    private val navPattern = Regex("[0-9]{1,11}(\\.[0-9]{1,4})?")
    private val numericCode = Regex("[0-9]{1,16}")

    fun parse(text: String, today: LocalDate): List<CachedListedSecurity> {
        require(text.length <= 12_000_000) { "Official report is too large." }
        require(!text.contains("<html", ignoreCase = true)) {
            "Received HTML instead of an official AMFI NAV report."
        }
        val results = ArrayList<CachedListedSecurity>()
        var sawHeader = false
        for (line in text.lineSequence()) {
            if (line.contains("Scheme Code", ignoreCase = true) &&
                line.contains("Net Asset Value", ignoreCase = true)) {
                sawHeader = true
                continue
            }
            val parts = line.split(';')
            if (parts.size != 6) continue // house/section headers
            val code = parts[0].trim()
            val fundName = parts[3].trim()
            val nav = parts[4].trim()
            val date = parts[5].trim()
            if (!numericCode.matches(code) || fundName.isBlank() || fundName.length > 200 ||
                !navPattern.matches(nav)) continue
            val day = runCatching { LocalDate.parse(date, navDate) }.getOrNull() ?: continue
            if (day.isAfter(today) || day.isBefore(today.minusYears(3))) continue
            val price = runCatching {
                MoneyScaled.fromRupees(BigDecimal(nav))
            }.getOrNull()?.takeIf { it.raw > 0 } ?: continue
            for (part in listOf(parts[1], parts[2])) {
                val isin = part.trim().uppercase(Locale.ROOT)
                if (validIsin.matches(isin)) {
                    results.add(CachedListedSecurity(
                        isin = isin, name = fundName, category = "MUTUAL_FUND",
                        source = OfficialMarketSource.AMFI, closeDate = day,
                        closePrice = price, amfiCode = code,
                    ))
                    require(results.size <= MAX_ROWS) { "Too many NAV rows." }
                }
            }
        }
        require(sawHeader && results.isNotEmpty()) {
            "Missing supported AMFI NAV heading or valid schemes; source changed."
        }
        return results.distinctBy { it.isin to it.closeDate }
    }
}
