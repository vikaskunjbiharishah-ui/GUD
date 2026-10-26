package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section Q. */
class EodPricesTest {
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)

    @Test fun closeReplacesPriceAndLeavesQuantity() {
        assertTrue(EodPrices.isEodSource("NSE close"))
        assertTrue(EodPrices.isEodSource("AMFI NAV"))
        assertFalse(EodPrices.isEodSource("CAS Statement Snapshot"))
        assertFalse(EodPrices.isEodSource(null))

        val tcs = EodPrices.listed(CatalogRow(
            isin = "ine467b01029", name = "TCS", symbol = "TCS", nse = "TCS", bse = "TCS",
            amfi = "", priceRupees = 2208.0, date = "2026-09-09", source = "NSE close",
        ))
        requireNotNull(tcs)
        assertEquals(22_080_000L, tcs.close.raw)
        assertEquals("INE467B01029", tcs.isin)
        assertEquals("EQUITY", tcs.category)
        assertNull(tcs.amfiCode)
        assertNull(EodPrices.listed(CatalogRow("SHORT", "X", 10.0, "2026-09-09", "NSE close")))
        assertNull(EodPrices.listed(CatalogRow("INE467B01029", "TCS", 0.0, "2026-09-09", "NSE close")))
        assertEquals("MUTUAL_FUND", EodPrices.listed(CatalogRow(
            "INF879O01027", "Flexi", 89.3946, "2026-09-09", "AMFI NAV", category = "MUTUAL_FUND",
        ))?.category)
        assertEquals("EQUITY", EodPrices.listed(CatalogRow(
            "INE000000000", "Flat", 1.0, "2026-09-09", "NSE close", category = "REAL_ESTATE",
        ))?.category)

        val before = HeldPrice("h_tcs", "EQUITY", 1000L * 100_000_000L, rupees(2000), isin = "INE467B01029")
        val priced = EodPrices.apply(listOf(before), listOf(tcs))
        assertEquals(1, priced.updated)
        assertEquals(2208L, priced.holdings[0].price.rupees().longValueExact())
        assertEquals(before.quantityScaled, priced.holdings[0].quantityScaled)
        assertEquals("2026-09-09T16:00:00+05:30", priced.holdings[0].priceDate)
        assertFalse(priced.holdings[0].stale)

        val again = EodPrices.apply(priced.holdings, listOf(tcs))
        assertEquals(0, again.updated)
        assertEquals(priced.holdings[0].priceDate, again.holdings[0].priceDate)

        val fromCas = EodPrices.apply(
            listOf(before.copy(price = rupees(2208), priceSource = "CAS Statement Snapshot")),
            listOf(tcs),
        )
        assertEquals(1, fromCas.updated)

        val loan = HeldPrice("loan", "LIABILITY", 1, rupees(612_000), isin = "INE467B01029")
        val flat = HeldPrice("flat", "REAL_ESTATE", 1, rupees(1), subCategory = "Residential", isin = "INE000000000")
        val reit = HeldPrice("reit", "REAL_ESTATE", 100, rupees(300), subCategory = "REIT", isin = "INE000000001")
        val reitClose = EodPrices.listed(CatalogRow("INE000000001", "Embassy", 400.0, "2026-09-09", "NSE close"))!!
        val skipped = EodPrices.apply(listOf(loan, flat, reit), listOf(tcs, reitClose))
        assertEquals(1, skipped.updated)
        assertEquals(612_000L, skipped.holdings[0].price.rupees().longValueExact())
        assertEquals(1L, skipped.holdings[1].price.rupees().longValueExact())
        assertEquals(400L, skipped.holdings[2].price.rupees().longValueExact())
    }

    @Test fun searchPrefersGrowthOverIdcw() {
        val tcs = EodPrices.listed(CatalogRow(
            "INE467B01029", "TCS", 2208.0, "2026-09-09", "NSE close", symbol = "TCS", nse = "TCS",
        ))!!
        assertEquals("INE467B01029", EodPrices.search(listOf(tcs), "tcs", "EQUITY").single().isin)
        assertEquals(0, EodPrices.search(listOf(tcs), "tcs", "MUTUAL_FUND").size)
        assertEquals(0, EodPrices.search(listOf(tcs), "t").size)

        val funds = listOf(
            ListedClose("INF879O01118", "Parag Parikh Liquid Fund — Direct Plan · Daily Reinvestment of IDCW", null, null, null, null, "MUTUAL_FUND", MoneyScaled(10_007_489), "2026-09-10", "AMFI NAV"),
            ListedClose("INF879O01027", "Parag Parikh Flexi Cap Fund — Direct Plan · Growth", null, null, null, null, "MUTUAL_FUND", MoneyScaled(893_946), "2026-09-09", "AMFI NAV"),
            ListedClose("INF879O01068", "Parag Parikh Liquid Fund — Direct Plan · Growth", null, null, null, null, "MUTUAL_FUND", MoneyScaled(15_705_718), "2026-09-10", "AMFI NAV"),
        )
        assertEquals("INF879O01027", EodPrices.search(funds, "parag pari", "MUTUAL_FUND").first().isin)
        assertEquals("INF879O01068", EodPrices.search(funds, "parag pari", "MUTUAL_FUND")[1].isin)
    }
}
