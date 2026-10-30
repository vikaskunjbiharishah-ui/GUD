package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PriceProvenance { OFFICIAL_CLOSE, MANUAL, MISSING }

data class PricedClose(
    val amount: MoneyScaled?,
    val asOf: LocalDate?,
    val provenance: PriceProvenance,
    val stale: Boolean,
    val label: String,
)

/**
 * A missing official close stays missing. It is not stored or shown as zero.
 * A manual price is labelled as typed by the user and can be stale.
 */
object ManualPrice {
    const val MISSING_LABEL = "No close is saved. It is not zero."
    const val MANUAL_LABEL = "Manual price you typed."

    fun resolve(
        official: MoneyScaled?,
        officialDate: LocalDate?,
        manual: MoneyScaled?,
        manualDate: LocalDate?,
        today: LocalDate,
        staleAfterDays: Long = 7,
    ): PricedClose {
        require(staleAfterDays >= 0)
        if (usable(manual) && manualDate != null && !manualDate.isAfter(today)) {
            val stale = ChronoUnit.DAYS.between(manualDate, today) > staleAfterDays
            val whenText = if (stale) "Stale as of $manualDate." else "As of $manualDate."
            return PricedClose(manual, manualDate, PriceProvenance.MANUAL, stale, "$MANUAL_LABEL $whenText")
        }
        if (!usable(official) || officialDate == null || officialDate.isAfter(today)) {
            return PricedClose(null, null, PriceProvenance.MISSING, true, MISSING_LABEL)
        }
        val stale = ChronoUnit.DAYS.between(officialDate, today) > staleAfterDays
        val label = if (stale) "Official close is stale as of $officialDate."
            else "Official close as of $officialDate."
        return PricedClose(official, officialDate, PriceProvenance.OFFICIAL_CLOSE, stale, label)
    }

    private fun usable(amount: MoneyScaled?): Boolean = amount != null && amount.raw > 0L
}
