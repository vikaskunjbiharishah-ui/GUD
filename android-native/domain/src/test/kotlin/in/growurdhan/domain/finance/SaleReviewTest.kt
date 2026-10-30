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

/**
 * Hand-checked against LotEngine. Buy 10 at ₹100 with ₹10 charges stores ₹1,010
 * on the lot. The recorded unit price is ₹100, so selling 4 removes ₹400 of
 * FIFO cost. Sale charges of ₹4 come off the ₹600 proceeds, leaving ₹596.
 * Gain is ₹196. That is the engine order, not a second formula.
 */
class SaleReviewTest {
    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")
    private val holdingId = HoldingId("synthetic-equity")
    private fun rupees(value: Long) = MoneyScaled(value * 10_000L)
    private fun units(count: Long) = Math.multiplyExact(count, QUANTITY_SCALE)

    private fun holding() = FinancialHolding(
        holdingId, owner, "Synthetic equity", "EQUITY", rupees(150), units(6),
    )

    @Test fun fifoSaleUsesRecordedUnitPriceAndSubtractsSaleCharges() {
        val buy = FinancialEvent(
            "buy", holdingId, owner, FinancialEventKind.BUY, LocalDate.parse("2026-01-01"),
            units(10), rupees(100), rupees(1_000), rupees(10),
        )
        val sell = FinancialEvent(
            "sell", holdingId, owner, FinancialEventKind.SELL, LocalDate.parse("2026-06-01"),
            units(4), rupees(150), rupees(600), rupees(4),
        )
        val review = SaleReview.of(holding(), listOf(sell, buy))
        val line = review.lines.single()
        assertEquals(4_000_000L, line.cost!!.raw)
        assertEquals(5_960_000L, line.proceeds.raw)
        assertEquals(1_960_000L, line.gain!!.raw)
        assertEquals(1_960_000L, review.knownGain!!.raw)
        assertFalse(review.unknownCost)
        assertNull(review.warning)
        assertTrue(review.feeOrder.contains("Oldest units are sold first."))
        assertTrue(review.feeOrder.contains("last parcel"))
        assertEquals(LocalDate.parse("2026-01-01"), line.acquiredOn)
        assertFalse(line.longTerm)
    }

    @Test fun openingWithoutAPriceDoesNotShowAZeroGain() {
        val opening = FinancialEvent(
            "open", holdingId, owner, FinancialEventKind.OPENING_BALANCE, LocalDate.parse("2026-01-01"),
            units(10), MoneyScaled(0), MoneyScaled(0),
        )
        val sell = FinancialEvent(
            "sell", holdingId, owner, FinancialEventKind.SELL, LocalDate.parse("2026-06-01"),
            units(4), rupees(150), rupees(600),
        )
        val review = SaleReview.of(holding(), listOf(opening, sell))
        val line = review.lines.single()
        assertTrue(line.unknownCost)
        assertNull(line.cost)
        assertNull(line.gain)
        assertNull(review.knownGain)
        assertEquals(SaleReview.UNKNOWN_COST, review.warning)
        assertEquals(CostBasis.UNKNOWN_INVESTED_LABEL, "Need what you paid")
        assertTrue(review.warning!!.startsWith(CostBasis.UNKNOWN_INVESTED_LABEL))
    }

    @Test fun anotherOwnersSaleIsLeftOut() {
        val buy = FinancialEvent(
            "buy", holdingId, owner, FinancialEventKind.BUY, LocalDate.parse("2026-01-01"),
            units(10), rupees(100), rupees(1_000),
        )
        val foreign = FinancialEvent(
            "sell-other", holdingId, other, FinancialEventKind.SELL, LocalDate.parse("2026-06-01"),
            units(4), rupees(150), rupees(600),
        )
        val review = SaleReview.of(holding(), listOf(buy, foreign))
        assertTrue(review.lines.isEmpty())
        assertTrue(review.errors.any { it.contains("left out") })
        assertNull(review.knownGain)
    }
}
