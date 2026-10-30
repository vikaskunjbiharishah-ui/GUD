package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class InsightsReturnsTest {
    private fun rupees(value: Long) = MoneyScaled(value * 10_000L)

    @Test fun knownFlowPrintsTheRateAndAnUnknownFlowIsNotZero() {
        val holding = HoldingId("synthetic")
        val owner = OwnerId("synthetic-owner")
        val buy = FinancialEvent(
            "buy", holding, owner, FinancialEventKind.BUY, LocalDate.parse("2025-01-01"),
            totalAmount = rupees(1_000),
        )
        val known = InsightsReturns.note(listOf(buy), rupees(1_100), LocalDate.parse("2026-01-01"))
        assertEquals("XIRR 10.01%. Estimate, not advice.", known)
        val unknown = InsightsReturns.note(emptyList(), rupees(1_100), LocalDate.parse("2026-01-01"))
        assertEquals(InsightsReturns.UNKNOWN, unknown)
        assertFalse(unknown.contains("0%"))
        assertFalse(unknown.contains("₹"))
    }
}
