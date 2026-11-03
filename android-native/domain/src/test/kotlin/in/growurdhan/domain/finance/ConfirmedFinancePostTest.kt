package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfirmedFinancePostTest {
    private val owner = OwnerId("synthetic-owner")
    private val holding = HoldingId("synthetic-ppf")
    private val day = LocalDate.parse("2027-03-31")

    @Test fun interestUsesTheEngineAmountAndNotZero() {
        val line = VaultFinanceReview.InterestLine(
            owner, "Public Provident Fund",
            InterestCatchUp(listOf(PlannedInterest(day, MoneyScaled(1_377_400_000L), "PPF interest")), null),
            holding,
        )
        val posted = ConfirmedFinancePost.interestEvents(line)
        assertEquals(FinancialEventKind.INTEREST, posted.single().kind)
        assertEquals(1_377_400_000L, posted.single().totalAmount.raw)
        assertEquals("int:synthetic-ppf:2027-03-31", posted.single().id)
        assertEquals(posted, ConfirmedFinancePost.interestEvents(line))
        assertTrue(ConfirmedFinancePost.onlyNew(posted, setOf(posted.single().id)).isEmpty())
        assertEquals(emptyList<FinancialEvent>(), ConfirmedFinancePost.interestEvents(line.copy(holdingId = null)))
        assertThrows(IllegalArgumentException::class.java) {
            ConfirmedFinancePost.interestEvents(line.copy(plan = InterestCatchUp(
                listOf(PlannedInterest(day, MoneyScaled(0), "none")), null,
            )))
        }
    }

    @Test fun emiUsesTheInstalmentAndNotZero() {
        val line = VaultFinanceReview.EmiLine(
            owner, "Synthetic car loan",
            EmiCatchUp(
                listOf(EmiPosting(HoldingId("synthetic-loan"), owner, "Synthetic car loan", LocalDate.parse("2026-07-05"), MoneyScaled(185_000_000L), "Monthly EMI")),
                LocalDate.parse("2026-08-05"), 5, MoneyScaled(612_000L * 10_000), "PAYING",
            ),
        )
        val posted = ConfirmedFinancePost.emiEvents(line)
        assertEquals(FinancialEventKind.REPAYMENT, posted.single().kind)
        assertEquals(185_000_000L, posted.single().totalAmount.raw)
        assertEquals("emi:synthetic-loan:2026-07-05", posted.single().id)
        assertThrows(IllegalArgumentException::class.java) {
            ConfirmedFinancePost.emiEvents(line.copy(plan = line.plan.copy(events = listOf(
                line.plan.events.single().copy(amount = MoneyScaled(0)),
            ))))
        }
    }
}
