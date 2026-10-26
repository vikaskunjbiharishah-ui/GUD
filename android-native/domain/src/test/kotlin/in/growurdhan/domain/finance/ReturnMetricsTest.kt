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

/** Goldens from src/lib/kosha.test.ts section T and the percent helpers in growth.ts. */
class ReturnMetricsTest {
    private val owner = OwnerId("f1")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun holding(
        id: String, category: String, name: String, qty: Long, cost: Long, price: Long, sub: String = "",
        acquired: String? = null,
    ) = FinancialHolding(
        HoldingId(id), owner, name, category, rupees(price), qty * QUANTITY_SCALE,
        openingCostPerUnit = rupees(cost), subCategory = sub, acquiredOn = acquired?.let(::date),
    )
    private fun ev(id: String, kind: FinancialEventKind, on: String, qty: Long, price: Long, holding: String) =
        FinancialEvent(
            id, HoldingId(holding), owner, kind, date(on),
            qty * QUANTITY_SCALE, rupees(price), rupees(kotlin.math.abs(qty) * price),
        )

    @Test fun absoluteAnnualAndProfilesMatchTauri() {
        assertEquals(50.0, ReturnMetrics.absoluteReturnPercent(rupees(1_000), rupees(1_500)))
        assertEquals("+12.5%", ReturnMetrics.formatPercent(12.54))
        assertEquals("—", ReturnMetrics.formatPercent(null))
        assertNull(ReturnMetrics.annualReturnPercent(rupees(100), rupees(150), 200))
        val year = ReturnMetrics.annualReturnPercent(rupees(100), rupees(121), 365)
        requireNotNull(year)
        assertEquals(21.0, kotlin.math.round(year))

        val sip = holding("mf1", "MUTUAL_FUND", "Flexi", 100, 10, 12)
        val sipEvents = listOf(
            ev("s1", FinancialEventKind.BUY, "2025-01-01", 10, 10, "mf1"),
            ev("s2", FinancialEventKind.BUY, "2025-02-01", 10, 10, "mf1"),
            ev("s3", FinancialEventKind.BUY, "2025-03-01", 10, 10, "mf1"),
        )
        assertEquals("Absolute return", ReturnMetrics.profileOf(sip).primaryLabel)
        assertEquals("MF", ReturnMetrics.profileOf(sip).key)
        assertTrue(ReturnMetrics.investedHasChanged(sipEvents, sip))
        val sipReturn = ReturnMetrics.holdingReturn(sip, sipEvents, date("2026-09-10"))
        assertEquals("N/A", sipReturn.annual.text)
        assertEquals("changed", sipReturn.annual.reason)
        assertEquals("+20.0%", sipReturn.primaryText)

        val lump = holding("mf2", "MUTUAL_FUND", "Lump", 100, 10, 15)
        val opened = ev("l1", FinancialEventKind.OPENING_BALANCE, "2024-01-01", 100, 10, "mf2")
        val lumpReturn = ReturnMetrics.holdingReturn(lump, listOf(opened), date("2026-09-10"))
        assertEquals("ok", lumpReturn.annual.reason)
        assertEquals("+16.2%", lumpReturn.annual.text)
        assertEquals("+50.0%", lumpReturn.primaryText)
        assertTrue(lumpReturn.profile.why.contains("recorded in Kosha"))

        val fresh = ReturnMetrics.holdingReturn(
            lump, listOf(ev("l2", FinancialEventKind.OPENING_BALANCE, "2026-06-01", 100, 10, "mf2")),
            date("2026-09-10"),
        )
        assertEquals("under_year", fresh.annual.reason)

        assertEquals("EQUITY", ReturnMetrics.profileOf(holding("eq1", "EQUITY", "Reliance", 10, 100, 120)).key)
        assertEquals("SGB", ReturnMetrics.profileOf(holding("sg1", "GOLD_SGB", "SGB 2020", 10, 5000, 5500)).key)
        assertEquals("REIT", ReturnMetrics.profileOf(holding("re", "EQUITY", "Embassy Office Parks REIT", 10, 300, 320)).key)
        assertEquals("SIF", ReturnMetrics.profileOf(holding("sif", "SPECIALISED", "Quant SIF Equity", 1, 100, 110, "SIF")).key)
        assertEquals("AIF", ReturnMetrics.profileOf(holding("a1", "SPECIALISED", "Cat II AIF", 1, 100, 150)).key)
        assertEquals("Absolute return", ReturnMetrics.profileOf(holding("a1", "SPECIALISED", "Cat II AIF", 1, 100, 150)).primaryLabel)

        val ppf = holding("pp1", "FIXED_INCOME", "PPF", 1, 100_000, 110_000)
        assertEquals("PPF", ReturnMetrics.profileOf(ppf).key)
        val ppfReturn = ReturnMetrics.holdingReturn(ppf, emptyList(), date("2026-09-10"))
        assertEquals("+10.0%", ppfReturn.primaryText)
        assertEquals("none", ppfReturn.annual.reason)
        assertFalse(ppfReturn.unknownInvested)
    }

    @Test fun closedUnknownAndStatementHistoryMatchTauri() {
        assertTrue(ReturnMetrics.isStatementOnly(listOf(
            ev("ob", FinancialEventKind.OPENING_BALANCE, "2026-01-31", 100, 10, "mf2"),
        )))
        assertFalse(ReturnMetrics.isStatementOnly(listOf(
            ev("b", FinancialEventKind.BUY, "2025-01-01", 10, 10, "mf1"),
        )))

        val stillListed = holding("st1", "MUTUAL_FUND", "Scheme", 100, 10, 12)
        val statement = ReturnMetrics.holdingReturn(
            stillListed, listOf(ev("st_ob", FinancialEventKind.OPENING_BALANCE, "2026-01-31", 100, 10, "st1")),
            date("2026-09-10"),
        )
        assertEquals("Absolute return", statement.profile.primaryLabel)
        assertTrue(statement.profile.why.contains("recorded in Kosha"))

        val closed = holding("cl1", "MUTUAL_FUND", "Closed scheme", 10, 10, 12)
        val closedReturn = ReturnMetrics.holdingReturn(closed, listOf(
            ev("cl_ob", FinancialEventKind.OPENING_BALANCE, "2026-01-31", 10, 10, "cl1"),
            ev("cl_adj", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-02-28", -10, 10, "cl1"),
        ), date("2026-09-10"))
        assertEquals("Closed", closedReturn.primaryText)
        assertEquals("Closed", closedReturn.profile.primaryLabel)
        assertEquals("closed", closedReturn.annual.reason)

        val aif = holding("h_aif3", "SPECIALISED", "INDIA DISCOVERY FUND II - CLASS B1", 25000, 170, 170, "AIF")
        val unknown = ReturnMetrics.holdingReturn(
            aif, listOf(ev("s_aif", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-08-31", 25000, 170, "h_aif3")),
            date("2026-09-16"),
        )
        assertEquals(CostBasis.UNKNOWN_INVESTED_LABEL, unknown.primaryText)
        assertTrue(unknown.unknownInvested)
        assertEquals("unknown", unknown.annual.reason)
        assertEquals(ReturnMetrics.BUCKET_HINT, ReturnMetrics.BUCKET_HINT)
    }
}
