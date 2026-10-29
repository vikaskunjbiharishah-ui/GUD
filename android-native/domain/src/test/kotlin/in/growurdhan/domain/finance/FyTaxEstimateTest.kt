package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Hand-checked against IndianTaxRules AY26-27 (12.5% LTCG above ₹1,25,000,
 * 20% STCG) and the exemption order in src/context/KoshaContext.tsx.
 * Slab and property tax are left at zero on purpose.
 */
class FyTaxEstimateTest {
    private val owner = OwnerId("f1")
    private val other = OwnerId("f2")
    private val asOf = LocalDate.parse("2026-03-31")

    private fun rupees(value: Long) = MoneyScaled(Math.multiplyExact(value, 10_000))
    private fun units(value: Long) = Math.multiplyExact(value, QUANTITY_SCALE)
    private fun holding(
        id: String,
        category: String = "EQUITY",
        sub: String = "Shares",
        mode: String? = null,
        ownerId: OwnerId = owner,
    ) = FinancialHolding(
        HoldingId(id), ownerId, "Synthetic", category, rupees(1),
        subCategory = sub, goldHoldMode = mode,
    )
    private fun trade(
        id: String, holding: String, kind: FinancialEventKind, on: String,
        qty: Long, price: Long, ownerId: OwnerId = owner,
    ) = FinancialEvent(
        id, HoldingId(holding), ownerId, kind, LocalDate.parse(on),
        units(qty), rupees(price), rupees(Math.multiplyExact(qty, price)),
    )

    @Test fun equityLongTermNetsLossesThenAppliesThePublishedExemption() {
        val summary = FyTaxEstimate.summarize(
            holdings = listOf(holding("h1")),
            events = listOf(
                trade("b", "h1", FinancialEventKind.BUY, "2020-04-01", 2000, 100),
                trade("s1", "h1", FinancialEventKind.SELL, "2025-05-01", 1000, 400),
                trade("s2", "h1", FinancialEventKind.SELL, "2025-05-02", 1000, 50),
            ),
            financialYear = "2025-26",
            asOf = asOf,
        )
        assertEquals(FyTaxEstimate.LABEL, summary.label)
        assertEquals("AY26-27", summary.assessmentYear)
        assertEquals(rupees(250_000), summary.ltcgEquityGain)
        assertEquals(rupees(125_000), summary.ltcgExemption)
        assertEquals(rupees(125_000), summary.ltcgTaxable)
        assertEquals(rupees(15_625), summary.ltcgTax)
        assertEquals(rupees(15_625), summary.estimatedTax)
        assertEquals(0L, summary.stcgTax.raw)
        assertTrue(summary.lines().first() == "estimate, not advice")
        assertTrue(summary.lines().any { it.contains(MoneyEngine.formatRupees(summary.estimatedTax.raw)) })
    }

    @Test fun shortTermEquityUsesThePublishedStcgRateAndIgnoresSlabGains() {
        val summary = FyTaxEstimate.summarize(
            holdings = listOf(holding("eq"), holding("debt", "MUTUAL_FUND", "Gilt Fund")),
            events = listOf(
                trade("b1", "eq", FinancialEventKind.BUY, "2025-04-10", 100, 100),
                trade("s1", "eq", FinancialEventKind.SELL, "2025-06-01", 100, 200),
                trade("b2", "debt", FinancialEventKind.BUY, "2025-04-10", 10, 100),
                trade("s2", "debt", FinancialEventKind.SELL, "2025-06-01", 10, 150),
                FinancialEvent(
                    "div", HoldingId("eq"), owner, FinancialEventKind.DIVIDEND,
                    LocalDate.parse("2025-05-15"), totalAmount = rupees(25),
                ),
            ),
            financialYear = "2025-26",
            asOf = asOf,
        )
        assertEquals(rupees(10_000), summary.stcgEquityGain)
        assertEquals(rupees(2_000), summary.stcgTax)
        assertEquals(rupees(500), summary.debtSlabGain)
        assertEquals(rupees(2_000), summary.estimatedTax)
        assertEquals(rupees(25), summary.dividend)
    }

    @Test fun january2018ValueReplacesTheLowerActualCost() {
        val id = HoldingId("old")
        val summary = FyTaxEstimate.summarize(
            holdings = listOf(holding("old")),
            events = listOf(
                trade("b", "old", FinancialEventKind.BUY, "2015-06-01", 1, 100),
                trade("s", "old", FinancialEventKind.SELL, "2025-06-01", 1, 900),
            ),
            financialYear = "2025-26",
            asOf = asOf,
            fmvByHolding = mapOf(id to rupees(400)),
        )
        assertEquals(rupees(500), summary.ltcgEquityGain)
        assertEquals(0L, summary.estimatedTax.raw)
    }

    @Test fun unpublishedYearAndCrossOwnerRowsDoNotInventTax() {
        val missing = FyTaxEstimate.summarize(
            holdings = listOf(holding("h")),
            events = listOf(trade("s", "h", FinancialEventKind.SELL, "2038-05-01", 1, 10)),
            financialYear = "2038-39",
            asOf = LocalDate.parse("2039-03-31"),
        )
        assertEquals(0L, missing.estimatedTax.raw)
        assertTrue(missing.errors.single().contains("not available"))
        assertEquals(FyTaxEstimate.LABEL, missing.lines().first())

        val crossed = FyTaxEstimate.summarize(
            holdings = listOf(holding("h")),
            events = listOf(
                trade("b", "h", FinancialEventKind.BUY, "2020-04-01", 1, 100, owner),
                trade("s", "h", FinancialEventKind.SELL, "2025-05-01", 1, 500, other),
            ),
            financialYear = "2025-26",
            asOf = asOf,
        )
        assertEquals(0L, crossed.ltcgEquityGain.raw)
        assertEquals(0L, crossed.estimatedTax.raw)
        assertTrue(crossed.errors.isNotEmpty())
        assertTrue(crossed.lines().any { it == "This estimate is incomplete." })
    }
}
