package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OfficialQuotePolicyTest {
    private val now = LocalDate.parse("2026-10-10")
    private fun quote(date: String, id: String = "INF000000001") =
        RecordedOfficialQuote(id, MoneyScaled(125_000), LocalDate.parse(date),
            OfficialMarketSource.AMFI)

    @Test fun quoteMustCarryExplicitDateSourceAndExactHoldingIdentity() {
        assertEquals(QuoteCondition.CURRENT,
            OfficialQuotePolicy.condition(quote("2026-10-09"), "INF000000001", now))
        assertEquals(QuoteCondition.IDENTITY_MISMATCH,
            OfficialQuotePolicy.condition(quote("2026-10-09"), "OTHER", now))
    }

    @Test fun oldQuoteIsLabelledStaleAndFutureQuoteRefused() {
        assertEquals(QuoteCondition.STALE,
            OfficialQuotePolicy.condition(quote("2026-10-01"), "INF000000001", now))
        assertEquals(QuoteCondition.FUTURE_REJECTED,
            OfficialQuotePolicy.condition(quote("2026-10-11"), "INF000000001", now))
    }

    @Test fun missingIdentityAndNonpositivePriceNeverBecomeMarketValues() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordedOfficialQuote("", MoneyScaled(100), now, OfficialMarketSource.NSE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecordedOfficialQuote("ISIN", MoneyScaled(0), now, OfficialMarketSource.BSE)
        }
    }
}
