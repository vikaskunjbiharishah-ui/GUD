package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts sections A, B, F and I. */
class TaxonomyTest {
    private fun holding(name: String, category: String, sub: String) = FinancialHolding(
        HoldingId("h"), OwnerId("f1"), name, category, MoneyScaled(10_000), 100_000_000L, subCategory = sub,
    )
    private fun place(name: String, category: String, sub: String) =
        Taxonomy.classify(holding(name, category, sub)).code

    @Test fun exposureAndWrapperStayIndependent() {
        assertEquals(8, Taxonomy.exposures.size)
        assertEquals(11, Taxonomy.wrappers.size)
        assertEquals("Debt & fixed income / Government", "${Taxonomy.exposureLabel("DEBT")} / ${Taxonomy.levelLabel("DEBT", "GOVERNMENT")}")

        assertEquals("EQUITY/LARGE_CAP/DIRECT", place("Reliance Industries", "EQUITY", "Large Cap Conglomerate"))
        assertEquals("EQUITY/MULTI_FLEXI/MF", place("HDFC Flexi Cap Fund", "MUTUAL_FUND", "Flexi Cap Fund"))
        assertEquals("EQUITY/LARGE_CAP/INDEX_ETF", place("Nifty 50 Index Fund", "MUTUAL_FUND", "Index Fund"))
        assertEquals("EQUITY/LARGE_CAP/SIF", place("Quant SIF Equity Long-Short", "SPECIALISED", "SIF Strategy"))
        assertEquals("DEBT/GOVERNMENT/MF", place("SBI Gilt Fund", "MUTUAL_FUND", "Gilt Fund"))
        assertEquals("DEBT/DURATION/SIF", place("Edelweiss SIF Debt Long-Short", "SPECIALISED", "SIF"))
        assertEquals("DEBT/DEPOSITS/DEPOSIT", place("HDFC Bank FD", "FIXED_INCOME", "Bank Term Deposit"))

        val sgb = Taxonomy.classify(holding("SGB 2019-20 Series V", "GOLD_SGB", "RBI Sovereign Gold Bond"))
        assertEquals("GOLD", sgb.exposure)
        assertEquals("DIRECT", sgb.wrapper)
        assertEquals("PROPERTY", Taxonomy.classify(holding("Embassy Office Parks REIT", "EQUITY", "REIT")).exposure)
        assertEquals("PROPERTY", Taxonomy.classify(holding("IndiGrid InvIT", "EQUITY", "InvIT")).exposure)
        assertEquals("INVIT", Taxonomy.classify(holding("IndiGrid InvIT", "EQUITY", "InvIT")).level)
        assertEquals("GOLD/GOLD/PHYSICAL", place("Physical Gold Bars", "GOLD_SGB", "Physical 24 Karat Bullion"))
        assertEquals("DEBT/SMALL_SAVINGS/SMALL_SAVINGS", place("PPF Account", "FIXED_INCOME", "Govt Small Savings (EEE)"))
        assertEquals("RETIREMENT", Taxonomy.classify(holding("EPF Balance", "FIXED_INCOME", "Retirement Provident Fund")).wrapper)
        assertEquals("CASH", Taxonomy.classify(holding("Liquid Contingency", "CASH_BANK", "Liquid Contingency Fund")).exposure)
        assertEquals("LIABILITY/HOME_LOAN/DIRECT", place("SBI Home Loan", "LIABILITY", "Home Loan Overdraft"))
        assertEquals("PROTECTION/PURE/INSURANCE", place("HDFC Click 2 Protect Term Plan", "SPECIALISED", "Term Insurance"))
        assertEquals("PRIVATE/ESOP/DIRECT", place("Employer ESOP Grant", "SPECIALISED", "ESOP"))

        val forced = Taxonomy.classify(
            holding("Weird Name", "EQUITY", ""),
            explicit = ExplicitPlacement("GOLD", "SILVER", "PHYSICAL"),
        )
        assertEquals("GOLD/SILVER/PHYSICAL", forced.code)
        assertFalse(forced.inferred)
        assertTrue(Taxonomy.classify(holding("Reliance", "EQUITY", "Large Cap")).inferred)
    }
}
