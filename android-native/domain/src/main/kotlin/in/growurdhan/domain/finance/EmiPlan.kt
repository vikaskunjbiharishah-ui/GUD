package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * Loan EMI catch-up from src/lib/emi.ts.
 * On open, post every EMI dated today or earlier while the loan is paying.
 * There is no server clock. A paused loan posts nothing.
 */
object EmiPlan {
    fun addMonths(date: LocalDate, months: Int): LocalDate {
        val shifted = date.withDayOfMonth(1).plusMonths(months.toLong())
        val day = minOf(date.dayOfMonth, shifted.lengthOfMonth())
        return shifted.withDayOfMonth(day)
    }

    fun dueDates(next: LocalDate?, remaining: Int, asOf: LocalDate): List<LocalDate> {
        val start = next ?: return emptyList()
        if (remaining <= 0) return emptyList()
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (dates.size < remaining && !cursor.isAfter(asOf)) {
            dates.add(cursor)
            cursor = addMonths(cursor, 1)
        }
        return dates
    }

    fun resumeDate(next: LocalDate?, asOf: LocalDate): LocalDate {
        val start = next ?: return asOf
        if (!start.isBefore(asOf)) return start
        var cursor = start
        var guard = 0
        while (cursor.isBefore(asOf) && guard++ < 360) cursor = addMonths(cursor, 1)
        return cursor
    }

    fun plan(loan: LoanEmi, asOf: LocalDate, alreadyPosted: Set<LocalDate> = emptySet()): EmiCatchUp? {
        if (loan.category != "LIABILITY") return null
        if (effectiveStatus(loan) != "PAYING") return null
        val emi = loan.monthlyEmi ?: return null
        if (emi.raw <= 0L || loan.nextDue == null) return null
        val remaining = loan.remainingCount ?: 0
        if (remaining <= 0) return null
        val dates = dueDates(loan.nextDue, remaining, asOf).filter { it !in alreadyPosted }
        if (dates.isEmpty()) return null
        var outstanding = loan.outstanding.raw
        val events = mutableListOf<EmiPosting>()
        for (date in dates) {
            if (outstanding <= 0L) break
            val pay = minOf(outstanding, emi.raw)
            outstanding = Math.subtractExact(outstanding, pay)
            events += EmiPosting(loan.id, loan.ownerId, loan.name, date, MoneyScaled(pay), "Monthly EMI (automatic)")
        }
        if (events.isEmpty()) return null
        val left = maxOf(0, remaining - events.size)
        val finished = outstanding <= 0L || left == 0
        return EmiCatchUp(
            events = events,
            nextDue = if (finished) null else addMonths(events.last().date, 1),
            remainingCount = left,
            outstanding = MoneyScaled(outstanding),
            status = if (finished) "STOPPED" else "PAYING",
        )
    }

    fun apply(loan: LoanEmi, plan: EmiCatchUp, postedAt: String): LoanEmi = loan.copy(
        outstanding = plan.outstanding,
        remainingCount = plan.remainingCount,
        nextDue = plan.nextDue,
        status = plan.status,
        priceSource = "EMI catch-up",
        priceUpdatedAt = postedAt,
    )

    private fun effectiveStatus(loan: LoanEmi): String? {
        if (!loan.status.isNullOrBlank()) return loan.status
        if (loan.monthlyEmi != null && loan.monthlyEmi.raw > 0L && loan.nextDue != null) return "PAYING"
        return null
    }
}

data class LoanEmi(
    val id: HoldingId,
    val ownerId: OwnerId,
    val name: String,
    val category: String,
    val outstanding: MoneyScaled,
    val monthlyEmi: MoneyScaled? = null,
    val remainingCount: Int? = null,
    val nextDue: LocalDate? = null,
    val status: String? = null,
    val priceSource: String? = null,
    val priceUpdatedAt: String? = null,
)

data class EmiPosting(
    val holdingId: HoldingId,
    val ownerId: OwnerId,
    val name: String,
    val date: LocalDate,
    val amount: MoneyScaled,
    val notes: String,
)

data class EmiCatchUp(
    val events: List<EmiPosting>,
    val nextDue: LocalDate?,
    val remainingCount: Int,
    val outstanding: MoneyScaled,
    val status: String,
)
