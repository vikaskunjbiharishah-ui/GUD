package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Data provenance is mandatory; an undated or guessed number is not a quote. */
enum class OfficialMarketSource { NSE, BSE, AMFI }

data class RecordedOfficialQuote(
    val identity: String,
    val unitPrice: MoneyScaled,
    val closeDate: LocalDate,
    val source: OfficialMarketSource,
) {
    init {
        require(identity.isNotBlank() && identity.length <= 64)
        require(unitPrice.raw > 0) { "Price must be positive and explicitly sourced." }
    }
}

enum class QuoteCondition { CURRENT, STALE, FUTURE_REJECTED, IDENTITY_MISMATCH }

/**
 * A pure validation boundary for future official-source adapters.
 * This does NOT fetch or store prices and cannot produce a live valuation.
 */
object OfficialQuotePolicy {
    fun condition(
        quote: RecordedOfficialQuote,
        expectedIdentity: String,
        today: LocalDate,
        staleAfterDays: Long = 7L,
    ): QuoteCondition {
        require(staleAfterDays >= 0 && staleAfterDays <= 365)
        if (quote.identity != expectedIdentity) return QuoteCondition.IDENTITY_MISMATCH
        val age = ChronoUnit.DAYS.between(quote.closeDate, today)
        if (age < 0) return QuoteCondition.FUTURE_REJECTED
        if (age > staleAfterDays) return QuoteCondition.STALE
        return QuoteCondition.CURRENT
    }
}
