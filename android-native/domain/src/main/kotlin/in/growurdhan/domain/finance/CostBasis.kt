package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Manual cost basis from src/lib/costBasis.ts.
 * A holdings statement has units and a market price, not what was paid.
 * The correction supersedes the opening parcel. Quantity stays.
 */
object CostBasis {
    const val UNKNOWN_INVESTED_LABEL = "Need what you paid"
    private const val RUPEE = 10_000L
    private val parcelKinds = setOf(
        FinancialEventKind.BUY,
        FinancialEventKind.OPENING_BALANCE,
        FinancialEventKind.SNAPSHOT_ADJUSTMENT,
        FinancialEventKind.CORRECTION,
    )
    private val balanceIsTheCost = setOf(
        SavingsKind.PPF, SavingsKind.EPF, SavingsKind.SSY, SavingsKind.FD,
        SavingsKind.SCSS, SavingsKind.POMIS, SavingsKind.NSC, SavingsKind.KVP,
        SavingsKind.RD, SavingsKind.FRSB,
    )

    fun earliestActiveParcel(events: List<FinancialEvent>, holdingId: HoldingId): FinancialEvent? {
        val mine = events.filter { it.holdingId == holdingId }
        val superseded = mine.mapNotNull { it.supersedesEventId }.toSet()
        return mine.filter {
            it.id !in superseded && it.kind in parcelKinds && it.quantityScaled > 0
        }.sortedWith(compareBy({ it.date }, { it.id })).firstOrNull()
    }

    /**
     * Append-only plan. Returns null when the price is blank, there are no
     * units, the date cannot be placed, or nothing would change.
     */
    fun planInvestedCorrection(
        holding: FinancialHolding,
        events: List<FinancialEvent>,
        unitPrice: MoneyScaled,
        acquiredOn: String,
    ): InvestedCorrection? {
        if (unitPrice.raw <= 0L) return null
        val parcel = earliestActiveParcel(events, holding.holdingId)
        val qty = parcel?.quantityScaled ?: holding.storedQuantityScaled
        if (qty <= 0L) return null
        val dated = parseIso(acquiredOn) ?: parcel?.date ?: holding.acquiredOn ?: return null
        val total = MoneyScaled(timesQuantity(qty, unitPrice.raw))
        val samePrice = parcel != null && (parcel.unitPrice?.raw ?: 0L) == unitPrice.raw
        val sameDate = parcel != null && parcel.date == dated
        if (samePrice && sameDate) return null
        return InvestedCorrection(
            holding.holdingId, holding.ownerId, dated, qty, unitPrice, total,
            parcel?.id,
            "Purchase date you typed — not the day you edited this. The statement had no buy price.",
        )
    }

    fun isUnknownInvested(
        holding: FinancialHolding,
        events: List<FinancialEvent>,
        cost: MoneyScaled,
        value: MoneyScaled,
    ): Boolean {
        if (holding.category == "LIABILITY" || balanceIsCost(holding)) return false
        if (cost.raw <= 0L || value.raw <= 0L) return false
        val drift = (cost.raw - value.raw).let { if (it < 0) -it else it }
        if (drift > RUPEE) return false
        return events.none {
            it.holdingId == holding.holdingId &&
                (it.kind == FinancialEventKind.BUY || it.kind == FinancialEventKind.CORRECTION)
        }
    }

    private fun balanceIsCost(holding: FinancialHolding): Boolean {
        if (holding.category != "FIXED_INCOME") return false
        val blob = "${holding.subCategory} ${holding.name}".lowercase()
        if (Regex("\\bnps\\b").containsMatchIn(blob)) return false
        val kind = SavingsInterestEngine.kindForSubcategory(blob) ?: return false
        return kind in balanceIsTheCost
    }

    private fun parseIso(raw: String): LocalDate? {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(raw)) return null
        return try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

data class InvestedCorrection(
    val holdingId: HoldingId,
    val ownerId: OwnerId,
    val date: LocalDate,
    val quantityScaled: Long,
    val unitPrice: MoneyScaled,
    val totalAmount: MoneyScaled,
    val supersedesEventId: String?,
    val notes: String,
) {
    fun event(id: String): FinancialEvent = FinancialEvent(
        id, holdingId, ownerId, FinancialEventKind.CORRECTION, date,
        quantityScaled, unitPrice, totalAmount, supersedesEventId = supersedesEventId,
    )
}
