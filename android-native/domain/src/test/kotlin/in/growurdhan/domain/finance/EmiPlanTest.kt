package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section O. */
class EmiPlanTest {
    private fun date(value: String) = LocalDate.parse(value)
    private fun rupees(value: Long) = MoneyScaled(value * 10_000)
    private val loan = LoanEmi(
        HoldingId("loan1"), OwnerId("f1"), "HDFC Car Loan", "LIABILITY",
        outstanding = rupees(612_000), monthlyEmi = rupees(18_500), remainingCount = 6,
        nextDue = date("2026-07-05"), status = "PAYING",
    )

    @Test fun catchUpPostsOnlyDatesAlreadyDue() {
        assertEquals(date("2026-02-28"), EmiPlan.addMonths(date("2026-01-31"), 1))
        assertEquals(date("2024-02-29"), EmiPlan.addMonths(date("2024-01-31"), 1))
        assertEquals(
            listOf("2026-03-05", "2026-04-05", "2026-05-05"),
            EmiPlan.dueDates(date("2026-03-05"), 3, date("2026-05-10")).map { it.toString() },
        )
        assertEquals(emptyList<LocalDate>(), EmiPlan.dueDates(date("2026-10-05"), 12, date("2026-09-09")))

        val plan = EmiPlan.plan(loan, date("2026-09-09"))
        requireNotNull(plan)
        assertEquals(listOf("2026-07-05", "2026-08-05", "2026-09-05"), plan.events.map { it.date.toString() })
        assertEquals(3, plan.remainingCount)
        assertEquals(612_000L - 18_500L * 3, plan.outstanding.rupees().longValueExact())
        assertEquals(date("2026-10-05"), plan.nextDue)
        assertEquals("PAYING", plan.status)
        assertEquals("Monthly EMI (automatic)", plan.events.first().notes)
        assertEquals(0L, plan.events.first().amount.raw - rupees(18_500).raw)

        assertNull(EmiPlan.plan(loan.copy(status = "PAUSED"), date("2026-09-09")))
        val skipped = EmiPlan.plan(loan, date("2026-09-09"), setOf(date("2026-07-05")))
        requireNotNull(skipped)
        assertEquals(listOf("2026-08-05", "2026-09-05"), skipped.events.map { it.date.toString() })
        assertEquals(date("2026-10-05"), EmiPlan.resumeDate(date("2026-07-05"), date("2026-09-09")))
        assertEquals(date("2026-11-05"), EmiPlan.resumeDate(date("2026-11-05"), date("2026-09-09")))

        val last = EmiPlan.plan(
            loan.copy(outstanding = rupees(10_000), remainingCount = 2),
            date("2026-09-09"),
        )
        requireNotNull(last)
        assertEquals(1, last.events.size)
        assertEquals(10_000L, last.events.single().amount.rupees().longValueExact())
        assertEquals(0L, last.outstanding.raw)
        assertEquals("STOPPED", last.status)
        assertNull(last.nextDue)

        val implied = EmiPlan.plan(loan.copy(status = null), date("2026-07-05"))
        requireNotNull(implied)
        assertEquals(listOf("2026-07-05"), implied.events.map { it.date.toString() })
        assertNull(EmiPlan.plan(loan.copy(category = "EQUITY"), date("2026-09-09")))

        val applied = EmiPlan.apply(loan, plan, "2026-09-09T00:00:00Z")
        assertEquals(plan.outstanding, applied.outstanding)
        assertEquals("EMI catch-up", applied.priceSource)
        assertEquals(3, applied.remainingCount)
    }
}
