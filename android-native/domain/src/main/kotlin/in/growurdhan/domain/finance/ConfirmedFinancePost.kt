package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * Turns a confirmed interest or EMI review into ledger events.
 * A zero amount is refused. A missing holding is not guessed.
 * The id is the holding and the date, so a second confirm is the same event.
 */
object ConfirmedFinancePost {
    fun interestEvents(line: VaultFinanceReview.InterestLine): List<FinancialEvent> {
        val holdingId = line.holdingId ?: return emptyList()
        return line.plan.events.map { planned ->
            event(holdingId, line.ownerId, planned.date, planned.amount, FinancialEventKind.INTEREST)
        }
    }

    fun emiEvents(line: VaultFinanceReview.EmiLine): List<FinancialEvent> =
        line.plan.events.map { posting ->
            event(posting.holdingId, posting.ownerId, posting.date, posting.amount, FinancialEventKind.REPAYMENT)
        }

    fun onlyNew(events: List<FinancialEvent>, existingIds: Set<String>): List<FinancialEvent> =
        events.filter { it.id !in existingIds }

    private fun event(
        holdingId: HoldingId,
        ownerId: OwnerId,
        date: LocalDate,
        amount: MoneyScaled,
        kind: FinancialEventKind,
    ): FinancialEvent {
        require(amount.raw > 0L) { "A zero amount is not posted." }
        val prefix = if (kind == FinancialEventKind.INTEREST) "int" else "emi"
        return FinancialEvent(
            id = "$prefix:${holdingId.raw}:${date}",
            holdingId = holdingId,
            ownerId = ownerId,
            kind = kind,
            date = date,
            totalAmount = amount,
        )
    }
}
