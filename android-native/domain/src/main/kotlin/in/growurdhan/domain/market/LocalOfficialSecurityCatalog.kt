package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate

/**
 * Read-only search over rows supplied by a VERIFIED, locally cached NSE/BSE/AMFI
 * adapter. Does NOT make HTTP requests or claim a live price. A source label in
 * an arbitrary JSON payload is not sufficient verification; the M30 adapter
 * must establish official file provenance before constructing these rows.
 */
data class CachedListedSecurity(
    val isin: String,
    val name: String,
    val category: String,
    val source: OfficialMarketSource,
    val closeDate: LocalDate,
    val closePrice: MoneyScaled,
    val symbol: String? = null,
    val amfiCode: String? = null,
) {
    init {
        require(Regex("[A-Z0-9]{12}").matches(isin)) {
            "Only a twelve-character uppercase security identity is accepted."
        }
        require(name.trim().length in 1..200 && name.none { it.isISOControl() })
        require(category in setOf("EQUITY", "MUTUAL_FUND"))
        require(closePrice.raw > 0L)
        require(symbol == null || (symbol.length <= 64 &&
            symbol.none { it.isISOControl() }))
        require(amfiCode == null || Regex("[0-9]{1,16}").matches(amfiCode))
    }

    fun condition(today: LocalDate): QuoteCondition = OfficialQuotePolicy.condition(
        RecordedOfficialQuote(isin, closePrice, closeDate, source), isin, today)
}

object LocalOfficialSecurityCatalog {
    /**
     * Source rows only; no network or guessed price. Reject future and invalid
     * dates, keep stale dated identities visible with a warning, limit results.
     */
    fun search(
        rows: List<CachedListedSecurity>,
        category: String,
        query: String,
        today: LocalDate,
        limit: Int = 20,
    ): List<CachedListedSecurity> {
        require(limit in 1..100)
        if (category !in setOf("EQUITY", "MUTUAL_FUND")) return emptyList()
        val q = query.trim().take(100)
        if (q.length < 2) return emptyList()
        return rows.asSequence()
            .filter { it.category == category && it.condition(today) !=
                QuoteCondition.FUTURE_REJECTED }
            .filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.isin.contains(q, ignoreCase = true) ||
                    it.symbol?.contains(q, ignoreCase = true) == true ||
                    it.amfiCode?.contains(q, ignoreCase = true) == true
            }
            // A cache may contain many dates for the same ISIN/source;
            // display the newest usable row rather than an arbitrary stale row.
            .sortedByDescending { it.closeDate }
            .distinctBy { it.isin to it.source }
            .take(limit)
            .toList()
    }
}
