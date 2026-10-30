package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate

/** XIRR for Insights. A missing rate is unknown, never zero. */
object InsightsReturns {
    const val UNKNOWN = "XIRR is unknown from this book."

    fun note(events: List<FinancialEvent>, currentValue: MoneyScaled, asOf: LocalDate): String {
        val rate = ReturnEngine.xirr(ReturnEngine.flows(events, currentValue, asOf)) ?: return UNKNOWN
        return "XIRR $rate%. Estimate, not advice."
    }
}
