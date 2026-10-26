package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts for src/lib/holdingView.ts. */
class HoldingViewTest {
    private val owner = OwnerId("f1")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun holding(
        id: String, category: String, name: String, sub: String = "",
        price: Long = 1, cost: Long? = null, qty: Long = 1, mode: String? = null,
    ) = FinancialHolding(
        HoldingId(id), owner, name, category, rupees(price), qty * QUANTITY_SCALE,
        openingCostPerUnit = cost?.let(::rupees), subCategory = sub, goldHoldMode = mode,
    )

    @Test fun bucketsLabelsAndNamesMatchTauri() {
        assertEquals("Updated to match statement", HoldingView.eventLabel(FinancialEventKind.SNAPSHOT_ADJUSTMENT))
        assertEquals("Opening from statement", HoldingView.eventLabel(FinancialEventKind.OPENING_BALANCE))
        assertEquals("Bought", HoldingView.eventLabel(FinancialEventKind.BUY))
        assertEquals("Loan recorded", HoldingView.eventLabel(FinancialEventKind.VALUATION_UPDATE, "Loan recorded — outstanding at start"))
        assertEquals("Value updated", HoldingView.eventLabel(FinancialEventKind.VALUATION_UPDATE))
        assertEquals("Loan repayment", HoldingView.eventLabel(FinancialEventKind.REPAYMENT))

        assertEquals("SHARES", HoldingView.bucketOf(holding("eq", "EQUITY", "Reliance", "Large Cap")))
        assertEquals("FUNDS", HoldingView.bucketOf(holding("mf", "MUTUAL_FUND", "HDFC Flexi Cap", "Flexi Cap")))
        assertEquals("GOLD", HoldingView.bucketOf(holding("sgb", "GOLD_SGB", "SGB 2020", "SGB")))
        assertEquals("GOVT", HoldingView.bucketOf(holding("ppf", "FIXED_INCOME", "PPF", "Govt Small Savings (EEE)")))
        assertEquals("BANK", HoldingView.bucketOf(holding("fd", "FIXED_INCOME", "HDFC Bank FD", "Bank FD")))
        assertTrue(HoldingView.looksLikeSgb("2.5% SGB 2021-22 SERIES II 2029-LOAN CODE 30433"))
        assertEquals("GOLD", HoldingView.bucketOf(holding("tag", "EQUITY", "2.5% SGB 2021-22 SERIES II")))

        val chips = HoldingView.orderBuckets(setOf("SHARES", "FUNDS", "SPECIAL"))
        assertEquals(listOf("SHARES", "FUNDS", "SPECIAL"), chips.filter { !it.empty }.map { it.id })
        assertEquals(listOf("GOLD", "GOVT", "PROPERTY", "BANK", "LOAN"), chips.filter { it.empty }.map { it.id })
        assertTrue(chips.single { it.id == "LOAN" }.empty)
        assertFalse(chips.single { it.id == "SHARES" }.empty)
        assertEquals("Share", HoldingView.plainCategory("EQUITY"))

        assertEquals("Edelweiss Multi Cap Fund - Regular Plan Growth", HoldingView.displayName("MURG - Edelweiss Multi Cap Fund - Regular Plan Growth"))
        assertEquals("ICICI Prudential Active Momentum Fund", HoldingView.displayName("3572 - ICICI Prudential Active Momentum Fund"))
        assertEquals("ICICI Prudential Multi-Asset Fund", HoldingView.displayName("15 - ICICI Prudential Multi-Asset Fund"))
        assertEquals("Parag Parikh Flexi Cap Fund", HoldingView.displayName("001ZG - Parag Parikh Flexi Cap Fund"))
        assertEquals("HDFC Bank Ltd", HoldingView.displayName("HDFC Bank Ltd"))
        assertEquals("Sovereign Gold Bond 2018-19 Series VI", HoldingView.displayName("Sovereign Gold Bond 2018-19 Series VI"))
        assertEquals("AVAP", HoldingView.displayName("AVAP"))
        assertEquals("", HoldingView.displayName(""))
        assertEquals(
            "NIPPON INDIA MF-NIPPON INDIA ETF NIFTY 1D RATE",
            HoldingView.displayName("NIPPON LIFE INDIA AM LTD#NIPPON INDIA MF-NIPPON INDIA ETF NIFTY 1D RATE"),
        )
        assertEquals("Edelweiss Multi Cap Fund", HoldingView.displayName("AMC#MURG - Edelweiss Multi Cap Fund"))

        assertEquals("Edelweiss", HoldingView.inferAmc("MURG - Edelweiss Multi Cap Fund - Regular Plan Growth"))
        assertEquals("ICICI Prudential", HoldingView.inferAmc("3572 - ICICI Prudential Active Momentum Fund"))
        assertEquals("Nippon India", HoldingView.inferAmc("MCAG - NIPPON INDIA MNC FUND"))
        assertEquals("WhiteOak Capital", HoldingView.inferAmc("101G - WhiteOak Capital Fund"))
        assertEquals("HDFC", HoldingView.inferAmc("HDFC Flexi Cap Fund"))
        assertEquals("ICICI Prudential", HoldingView.accountTitle("Mutual Fund Folios", listOf(AccountLine("X", "ICICI Prudential")), "MF_FOLIO"))
        assertEquals("Edelweiss", HoldingView.accountTitle("Mutual Fund Folios", listOf(AccountLine("MURG - Edelweiss Multi Cap Fund")), "MF_FOLIO"))
        assertEquals("RAISE SECURITIES PRIVATE LIMITED", HoldingView.accountTitle("RAISE SECURITIES PRIVATE LIMITED", listOf(AccountLine("HDFC Bank Ltd")), "DEMAT"))
        assertEquals("Mutual fund folio", HoldingView.accountTitle("Mutual Fund Folios", emptyList(), "MF_FOLIO"))
    }

    @Test fun loanGoldAndUpcomingMatchTauri() {
        val loan = holding("h_loan", "LIABILITY", "HDFC Car Loan", price = 612_000, cost = 850_000)
        val figures = HoldingView.metrics(loan)
        assertEquals(612_000L, figures.value.rupees().longValueExact())
        assertEquals(850_000L, figures.cost.rupees().longValueExact())
        assertEquals(0L, figures.gain.raw)
        assertTrue(HoldingView.isOpen(loan))
        assertFalse(HoldingView.isOpen(loan.copy(unitPrice = MoneyScaled(0))))

        val certificate = holding("sgb", "GOLD_SGB", "SGB 2018", "Sovereign Gold Bond", price = 7_000, qty = 10, mode = "CERTIFICATE")
        assertEquals(rupees(6_500), HoldingView.priceOf(certificate, ibjaPerGram = rupees(6_500), benchmark = rupees(6_200)))
        assertEquals("grams", HoldingView.unitWord(certificate))
        assertEquals("shares", HoldingView.unitWord(holding("esop", "SPECIALISED", "Grant", "ESOP")))
        assertEquals("", HoldingView.unitWord(holding("ppf", "FIXED_INCOME", "PPF", "PPF")))

        val coming = HoldingView.upcoming(
            listOf(HoldingCard(holding("sgb", "GOLD_SGB", "SGB 2020", "SGB"), nextCoupon = date("2099-11-15"), maturity = date("2099-11-15"))),
            listOf(GoalReminder("g1", "College", date("2099-06-01"))),
            date("2026-01-01"),
        )
        assertTrue(coming.any { it.kind == "SGB_COUPON" })
        assertTrue(coming.any { it.kind == "GOAL" && it.title == "Goal · College" })
        assertTrue(coming.any { it.kind == "MATURITY" })

        val emi = HoldingView.upcoming(
            listOf(HoldingCard(loan, nextCoupon = date("2026-10-05"))), emptyList(), date("2026-09-16"),
        )
        assertEquals("EMI", emi.single().kind)
        assertTrue(emi.single().title.contains("HDFC Car Loan"))
        assertNull(HoldingView.upcoming(
            listOf(HoldingCard(loan, nextCoupon = date("2026-10-05"), emiStopped = true)), emptyList(), date("2026-09-16"),
        ).firstOrNull())

        val reit = holding("h_reit", "REAL_ESTATE", "Embassy Office Parks REIT", "REIT", qty = 100)
        val income = HoldingView.upcoming(
            listOf(HoldingCard(
                reit, monthlyIncome = rupees(15_000), incomeDay = 15,
                incomeStart = date("2026-01-10"), incomeEnd = date("2026-03-20"),
            )),
            emptyList(), date("2026-01-01"),
        )
        assertEquals("2026-01-15", income.single { it.kind == "INCOME" }.date.toString())
    }
}
