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

/** Goldens from src/lib/kosha.test.ts section W and src/lib/costBasis.ts. */
class CostBasisTest {
    private val owner = OwnerId("f1")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun holding(
        id: String,
        category: String,
        qty: Long,
        sub: String = "",
        name: String = "Synthetic",
        acquired: String? = null,
    ) = FinancialHolding(
        HoldingId(id), owner, name, category, rupees(1), qty * QUANTITY_SCALE,
        subCategory = sub, acquiredOn = acquired?.let(::date),
    )
    private fun ev(
        id: String, kind: FinancialEventKind, on: String, qty: Long, price: Long, holding: String,
        supersedes: String? = null,
    ) = FinancialEvent(
        id, HoldingId(holding), owner, kind, date(on),
        qty * QUANTITY_SCALE, rupees(price), rupees(qty * price),
        supersedesEventId = supersedes,
    )

    @Test fun typedPurchasePriceSupersedesTheStatementParcel() {
        val opening = ev("ob_rel", FinancialEventKind.OPENING_BALANCE, "2026-08-31", 139, 1285, "h_rel")
        assertEquals("ob_rel", CostBasis.earliestActiveParcel(listOf(opening), HoldingId("h_rel"))?.id)
        val reliance = holding("h_rel", "EQUITY", 999, name = "RELIANCE INDUSTRIES LIMITED EQUITY SHARES")
        val paid = CostBasis.planInvestedCorrection(reliance, listOf(opening), rupees(850), "2021-03-15")
        requireNotNull(paid)
        assertEquals(139 * QUANTITY_SCALE, paid.quantityScaled)
        assertEquals(850L, paid.unitPrice.rupees().longValueExact())
        assertEquals(date("2021-03-15"), paid.date)
        assertEquals("ob_rel", paid.supersedesEventId)
        val folded = LotEngine.fold(
            HoldingId("h_rel"), "EQUITY", listOf(opening, paid.event("cor_rel")),
        )
        assertEquals(118_150L, folded.investedCost.rupees().longValueExact())
        assertEquals(139 * QUANTITY_SCALE, folded.quantityScaled)

        assertNull(CostBasis.planInvestedCorrection(
            reliance, listOf(opening), rupees(1285), "2026-08-31",
        ))
        assertEquals(date("2026-08-31"), CostBasis.planInvestedCorrection(
            reliance, listOf(opening), rupees(850), "",
        )?.date)
        assertEquals(date("2021-03-15"), CostBasis.planInvestedCorrection(
            reliance, listOf(opening), rupees(1285), "2021-03-15",
        )?.date)
        assertNull(CostBasis.planInvestedCorrection(reliance, listOf(opening), MoneyScaled(0), "2021-03-15"))
    }

    @Test fun snapshotWithoutAContributionStaysUnknownUntilCorrected() {
        val snap = ev("s_aif", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-08-31", 25000, 170, "h_aif3")
        val aif = holding("h_aif3", "SPECIALISED", 25000, "AIF", "INDIA DISCOVERY FUND II - CLASS B1")
        val paid = CostBasis.planInvestedCorrection(aif, listOf(snap), rupees(140), "not-a-date")
        requireNotNull(paid)
        assertEquals(date("2026-08-31"), paid.date)
        assertEquals(140L, paid.unitPrice.rupees().longValueExact())
        val folded = LotEngine.fold(HoldingId("h_aif3"), "SPECIALISED", listOf(snap, paid.event("c_aif")))
        assertEquals(25_000 * QUANTITY_SCALE, folded.quantityScaled)
        assertEquals(3_500_000L, folded.investedCost.rupees().longValueExact())

        val same = rupees(170) * 25_000
        assertTrue(CostBasis.isUnknownInvested(aif, listOf(snap), same, same))
        assertFalse(CostBasis.isUnknownInvested(
            aif, listOf(snap, paid.event("c_aif")), rupees(140) * 25_000, same,
        ))
        assertFalse(CostBasis.isUnknownInvested(aif, listOf(snap, paid.event("c_aif")), same, same))
        assertTrue(CostBasis.isUnknownInvested(aif, listOf(snap), same, MoneyScaled(same.raw + 10_000)))
        assertFalse(CostBasis.isUnknownInvested(aif, listOf(snap), same, MoneyScaled(same.raw + 10_001)))
        assertFalse(CostBasis.isUnknownInvested(aif, emptyList(), MoneyScaled(0), same))
        assertFalse(CostBasis.isUnknownInvested(
            holding("loan", "LIABILITY", 1, name = "Car loan"), emptyList(), same, same,
        ))
    }

    @Test fun balanceProductsAndTheEarliestParcel() {
        val ppf = holding("h_ppf", "FIXED_INCOME", 1, "PPF", "PPF SBI")
        val balance = rupees(500_000)
        assertFalse(CostBasis.isUnknownInvested(ppf, emptyList(), balance, balance))
        val nps = holding("h_nps", "FIXED_INCOME", 1, "NPS", "NPS Tier I")
        assertTrue(CostBasis.isUnknownInvested(nps, emptyList(), balance, balance))
        val storedOnly = holding("bare", "EQUITY", 10, acquired = "2020-01-02")
        val planned = CostBasis.planInvestedCorrection(storedOnly, emptyList(), rupees(40), "")
        requireNotNull(planned)
        assertEquals(10 * QUANTITY_SCALE, planned.quantityScaled)
        assertEquals(date("2020-01-02"), planned.date)
        assertNull(planned.supersedesEventId)
        assertNull(CostBasis.planInvestedCorrection(
            holding("empty", "EQUITY", 0), emptyList(), rupees(40), "2020-01-02",
        ))

        val later = ev("b", FinancialEventKind.BUY, "2024-02-01", 5, 10, "h")
        val earlier = ev("a", FinancialEventKind.BUY, "2024-01-01", 5, 10, "h")
        val sameDay = ev("c", FinancialEventKind.BUY, "2024-01-01", 5, 10, "h")
        val sale = ev("s", FinancialEventKind.SELL, "2023-01-01", 5, 10, "h")
        assertEquals("a", CostBasis.earliestActiveParcel(listOf(later, sameDay, earlier, sale), HoldingId("h"))?.id)
        val replaced = ev("cor", FinancialEventKind.CORRECTION, "2024-05-01", 5, 8, "h", supersedes = "a")
        assertEquals("cor", CostBasis.earliestActiveParcel(listOf(earlier, replaced), HoldingId("h"))?.id)
        assertEquals(CostBasis.UNKNOWN_INVESTED_LABEL, "Need what you paid")
    }
}

private operator fun MoneyScaled.times(units: Long) = MoneyScaled(Math.multiplyExact(raw, units))
