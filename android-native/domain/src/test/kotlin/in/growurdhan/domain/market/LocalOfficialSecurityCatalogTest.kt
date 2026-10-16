package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalOfficialSecurityCatalogTest {
    private val now = LocalDate.parse("2026-10-16")
    private val equity = CachedListedSecurity(
        "INE000000001", "Synthetic Exchange Security", "EQUITY",
        OfficialMarketSource.NSE, LocalDate.parse("2026-10-15"),
        MoneyScaled(1_345_000), symbol = "SYNTH",
    )
    private val fund = CachedListedSecurity(
        "INF000000001", "Synthetic Mutual Scheme", "MUTUAL_FUND",
        OfficialMarketSource.AMFI, LocalDate.parse("2026-10-15"),
        MoneyScaled(138_000), amfiCode = "123456",
    )

    @Test fun officialRowsAreSearchedOnlyWithinRequestedCategory() {
        val rows = listOf(equity, fund)
        assertEquals(listOf(equity), LocalOfficialSecurityCatalog.search(rows,
            "EQUITY", "SYN", now))
        assertEquals(listOf(fund), LocalOfficialSecurityCatalog.search(rows,
            "MUTUAL_FUND", "123456", now))
        assertTrue(LocalOfficialSecurityCatalog.search(rows, "EQUITY",
            "mutual", now).isEmpty())
        assertTrue(LocalOfficialSecurityCatalog.search(rows, "EQUITY", "S", now).isEmpty())
        assertEquals(QuoteCondition.CURRENT, equity.condition(now))
    }

    @Test fun futurePricesCannotAppearAndOldCloseIsAlwaysMarkedStale() {
        val future = equity.copy(closeDate = now.plusDays(1))
        val old = equity.copy(closeDate = now.minusDays(30), source = OfficialMarketSource.BSE)
        val found = LocalOfficialSecurityCatalog.search(listOf(future, old),
            "EQUITY", "Synthetic", now)
        assertEquals(listOf(old), found)
        assertEquals(QuoteCondition.STALE, old.condition(now))
    }

    @Test fun invalidIdentityOrZeroPriceFailsBeforeAnySelection() {
        assertThrows(IllegalArgumentException::class.java) {
            equity.copy(isin = "bad")
        }
        assertThrows(IllegalArgumentException::class.java) {
            equity.copy(closePrice = MoneyScaled(0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            equity.copy(amfiCode = "not-a-code")
        }
        assertTrue(LocalOfficialSecurityCatalog.search(emptyList(), "EQUITY",
            "synthetic", now).isEmpty())
    }

    @Test fun identicalOfficialSourceIdentitySelectsMostRecentNonfutureDate() {
        val older = equity.copy(closeDate = now.minusDays(22),
            closePrice = MoneyScaled(9_900_000))
        val future = equity.copy(closeDate = now.plusDays(2))
        val latest = LocalOfficialSecurityCatalog.search(
            listOf(older, future, equity), "EQUITY", "SYNTH", now,
        )
        assertEquals(listOf(equity), latest)
        assertEquals(QuoteCondition.CURRENT, latest.single().condition(now))
    }
}
