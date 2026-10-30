package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * Read-only FIFO sale result. It does not post a ledger event.
 * Buy charges are stored on the lot. When a unit price was recorded, the
 * FIFO cost removed on a sale is quantity times that unit price (LotEngine).
 * Sale charges are subtracted from proceeds. The last parcel keeps the
 * leftover paise. An opening or statement row with no price is unknown cost:
 * the gain stays null and is never presented as zero.
 */
object SaleReview {
    const val FEE_ORDER =
        "Oldest units are sold first. Buy charges are added to the lot's stored cost. " +
            "When a unit price was recorded, the FIFO cost of a sale is quantity times that unit price. " +
            "Sale charges are subtracted from the proceeds. The last parcel keeps any leftover paise " +
            "so the net proceeds add up."
    const val UNKNOWN_COST = "Need what you paid. The gain is not shown as zero."

    data class Line(
        val acquiredOn: LocalDate,
        val soldOn: LocalDate,
        val quantityScaled: Long,
        val cost: MoneyScaled?,
        val proceeds: MoneyScaled,
        val gain: MoneyScaled?,
        val longTerm: Boolean,
        val unknownCost: Boolean,
    )

    data class Result(
        val holdingId: HoldingId,
        val ownerId: OwnerId,
        val holdingName: String,
        val feeOrder: String,
        val lines: List<Line>,
        val knownGain: MoneyScaled?,
        val unknownCost: Boolean,
        val warning: String?,
        val errors: List<String>,
    )

    fun of(holding: FinancialHolding, events: List<FinancialEvent>): Result {
        val foreign = events.any { it.holdingId == holding.holdingId && it.ownerId != holding.ownerId }
        val mine = events.filter { it.holdingId == holding.holdingId && it.ownerId == holding.ownerId }
        val folded = LotEngine.fold(holding.holdingId, holding.category, mine)
        val unknownIds = mine.filter(::missingCostSource).map { it.id }.toSet()
        val lines = folded.disposals.map { disposal ->
            val unknown = disposal.purchaseEventId in unknownIds
            Line(
                acquiredOn = disposal.acquiredOn,
                soldOn = disposal.soldOn,
                quantityScaled = disposal.quantityScaled,
                cost = if (unknown) null else disposal.cost,
                proceeds = disposal.proceeds,
                gain = if (unknown) null else disposal.gain,
                longTerm = disposal.longTerm,
                unknownCost = unknown,
            )
        }
        val unknownCost = lines.any { it.unknownCost }
        val knownGain = if (unknownCost || lines.isEmpty()) null else
            lines.fold(MoneyScaled(0)) { sum, line -> sum + requireNotNull(line.gain) }
        val errors = buildList {
            addAll(folded.errors)
            if (foreign) add("Another person's events were left out.")
        }
        return Result(
            holding.holdingId, holding.ownerId, holding.name, FEE_ORDER, lines,
            knownGain, unknownCost, if (unknownCost) UNKNOWN_COST else null, errors,
        )
    }

    private fun missingCostSource(event: FinancialEvent): Boolean {
        if (event.kind != FinancialEventKind.OPENING_BALANCE &&
            event.kind != FinancialEventKind.SNAPSHOT_ADJUSTMENT
        ) return false
        val price = event.unitPrice
        if (price != null && price.raw > 0L) return false
        if (price == null && event.totalAmount.raw > 0L) return false
        return true
    }
}
