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

/** Goldens from src/lib/kosha.test.ts sections S, T and W for src/lib/growth.ts. */
class GrowthModelTest {
    private val owner = OwnerId("f1")
    private val you = GrowthMember(owner, "Vikas")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun holding(
        id: String, category: String, name: String, qty: Long, cost: Long, price: Long, sub: String = "",
    ) = FinancialHolding(
        HoldingId(id), owner, name, category, rupees(price), qty * QUANTITY_SCALE,
        openingCostPerUnit = rupees(cost), subCategory = sub,
    )
    private fun ev(id: String, kind: FinancialEventKind, on: String, qty: Long, price: Long, holding: String, total: Long = kotlin.math.abs(qty) * price) =
        FinancialEvent(
            id, HoldingId(holding), owner, kind, date(on),
            qty * QUANTITY_SCALE, rupees(price), rupees(total),
        )

    @Test fun equityBookAndSkippedGhostMatchTauri() {
        val share = holding("h_g", "EQUITY", "Reliance", 10, 100, 150)
        val opening = ev("op1", FinancialEventKind.OPENING_BALANCE, "2024-09-10", 10, 100, "h_g")
        val book = GrowthModel.build(listOf(share), listOf(opening), listOf(you), date("2026-09-10"))
        assertEquals(1_500L, book.kpis.assets.rupees().longValueExact())
        assertEquals(1_000L, book.kpis.invested.rupees().longValueExact())
        assertEquals(500L, book.kpis.absoluteGain.rupees().longValueExact())
        assertEquals(1_500L, book.kpis.netWorth.rupees().longValueExact())
        assertEquals(50.0, book.kpis.absoluteReturnPercent)
        assertEquals("SHARES", book.buckets.single().id)
        assertEquals("#38bdf8", book.buckets.single().color)
        assertEquals("EQUITY", book.exposures.single().id)
        assertEquals("Vikas", book.members.single().label)
        assertEquals("2026-27", book.currentFy)
        assertEquals(25, book.months.size)
        assertEquals(1_000L, book.months.first().invested.rupees().longValueExact())
        assertEquals(1_000L, book.months.last().invested.rupees().longValueExact())

        val ghost = holding("gh0", "MUTUAL_FUND", "Gone", 10, 10, 12)
        val live = holding("st1", "MUTUAL_FUND", "Live fund", 100, 10, 12)
        val withoutGhost = GrowthModel.build(
            listOf(ghost, live),
            listOf(
                ev("gh_ob", FinancialEventKind.OPENING_BALANCE, "2026-01-31", 5, 10, "gh0"),
                ev("gh_adj", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-02-28", -5, 10, "gh0"),
                ev("st_ob", FinancialEventKind.OPENING_BALANCE, "2026-01-31", 100, 10, "st1"),
            ),
            listOf(you), date("2026-09-10"),
        )
        assertFalse(withoutGhost.holdings.any { it.id == "gh0" })
        assertTrue(withoutGhost.holdings.any { it.id == "st1" })
        assertEquals(1_200L, withoutGhost.kpis.assets.rupees().longValueExact())

        val loan = holding("loan", "LIABILITY", "HDFC Car Loan", 1, 850_000, 612_000)
        val withLoan = GrowthModel.build(listOf(share, loan), listOf(opening), listOf(you), date("2026-09-10"))
        assertEquals(612_000L, withLoan.kpis.liabilities.rupees().longValueExact())
        assertEquals(1_500L - 612_000L, withLoan.kpis.netWorth.rupees().longValueExact())
        assertFalse(withLoan.buckets.any { it.id == "LOAN" })
    }

    @Test fun unknownCostIsLeftOutOfTheReturn() {
        val aif = holding("h_aif3", "SPECIALISED", "INDIA DISCOVERY FUND II - CLASS B1", 25_000, 170, 170, "AIF")
        val book = GrowthModel.build(
            listOf(aif),
            listOf(ev("s_aif", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-08-31", 25_000, 170, "h_aif3")),
            listOf(GrowthMember(owner, "You")),
            date("2026-09-16"),
        )
        assertEquals(0L, book.kpis.invested.raw)
        assertEquals(1, book.kpis.unknownInvestedCount)
        assertNull(book.kpis.absoluteReturnPercent)
        assertEquals(4_250_000L, book.kpis.assets.rupees().longValueExact())
        assertEquals(0L, book.holdings.single().cost.raw)
        assertTrue(book.holdings.single().unknownInvested)
    }

    @Test fun incomeAndMonthlyInvestedMatchTauri() {
        val share = holding("h_g", "EQUITY", "Reliance", 10, 100, 150)
        val dividend = ev("dv1", FinancialEventKind.DIVIDEND, "2026-05-10", 1, 5_000, "h_g", total = 5_000)
        val years = GrowthModel.incomeByFinancialYear(listOf(dividend), emptyList())
        assertEquals("2026-27", years.single().fy)
        assertEquals(5_000L, years.single().dividend.rupees().longValueExact())

        val rent = ev("rent", FinancialEventKind.INTEREST, "2025-08-01", 1, 8_000, "house", total = 8_000)
        val house = holding("house", "REAL_ESTATE", "Flat", 1, 1, 1)
        val split = GrowthModel.incomeByFinancialYear(listOf(dividend, rent), listOf(house))
        assertEquals(listOf("2025-26", "2026-27"), split.map { it.fy })
        assertEquals(8_000L, split.first().rental.rupees().longValueExact())
        assertEquals(0L, split.first().interest.raw)

        val book = GrowthModel.build(
            listOf(share),
            listOf(ev("op1", FinancialEventKind.OPENING_BALANCE, "2026-01-15", 10, 100, "h_g"), dividend),
            listOf(you), date("2026-09-10"),
        )
        assertEquals(5_000L, book.kpis.incomeThisFy.rupees().longValueExact())
        assertEquals(5_000L, book.months.last().income.rupees().longValueExact())

        val sold = GrowthModel.cumulativeByMonth(listOf(
            ev("buy", FinancialEventKind.BUY, "2026-01-10", 10, 100, "h_g"),
            ev("sell", FinancialEventKind.SELL, "2026-02-10", 10, 100, "h_g"),
        ), date("2026-03-01"))
        assertEquals(0L, sold.last().invested.raw)
    }
}
