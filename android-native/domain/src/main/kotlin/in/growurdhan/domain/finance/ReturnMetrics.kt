package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import java.util.Locale
import kotlin.math.pow

/**
 * How a holding's return is explained, from src/lib/returnMetrics.ts.
 * Absolute return is shown whenever the invested amount is known.
 * Annual return is only for one amount left untouched for at least a year.
 */
object ReturnMetrics {
    const val BUCKET_HINT =
        "Abs. return = (current − invested) ÷ invested. Annual return is N/A if invested changed."

    private val why = mapOf(
        "SGB" to "Absolute return compares today’s value with the amount invested in this holding.",
        "REIT" to "Absolute return compares today’s value with the amount invested in this holding.",
        "SIF" to "Absolute return compares today’s value with the amount invested in this holding.",
        "AIF" to "Absolute return compares today’s value with the amount invested in this holding.",
        "PPF" to "Absolute return compares today’s value with the amount invested in this holding.",
        "FD" to "Absolute return compares today’s value with the amount invested in this holding.",
        "MF" to "Absolute return compares today’s value with the amount invested in this holding.",
        "EQUITY" to "Absolute return compares today’s value with the amount invested in this holding.",
        "OTHER" to "Absolute return compares today’s value with the amount invested in this holding.",
    )
    private const val STATEMENT =
        "This is the change in value versus the invested value recorded in Kosha."
    private const val UNKNOWN =
        "The statement printed today’s market value, not what you paid. Return cannot be computed until you add invested."
    private const val CLOSED =
        "This holding is no longer on the latest statement. Quantity is zero."
    private val annualWhy = mapOf(
        "ok" to "Annual return is the yearly rate if this same amount sat from purchase until today.",
        "changed" to "N/A — invested changed after purchase (SIP, extra buy or sale). Annual return only works on one amount left untouched.",
        "under_year" to "N/A — held for under a year.",
        "unknown" to "N/A — add what you paid first.",
        "closed" to "N/A — this holding is closed.",
        "none" to "N/A — not enough of a purchase date to annualise.",
    )

    fun absoluteReturnPercent(invested: MoneyScaled, current: MoneyScaled): Double? {
        if (invested.raw <= 0L) return null
        return (current.raw - invested.raw).toDouble() / invested.raw.toDouble() * 100.0
    }

    /** CAGR for one untouched amount. Null under a year, from src/lib/growth.ts. */
    fun annualReturnPercent(invested: MoneyScaled, current: MoneyScaled, days: Long): Double? {
        if (invested.raw <= 0L || current.raw <= 0L || days < 365) return null
        val ratio = current.raw.toDouble() / invested.raw.toDouble()
        if (ratio <= 0.0 || !ratio.isFinite()) return null
        return (ratio.pow(365.0 / days) - 1.0) * 100.0
    }

    fun formatPercent(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        val sign = if (value > 0.0) "+" else ""
        return sign + String.format(Locale.US, "%.1f", value) + "%"
    }

    fun isStatementOnly(events: List<FinancialEvent>): Boolean {
        var opening = false
        for (event in events) {
            if (event.kind == FinancialEventKind.BUY || event.kind == FinancialEventKind.SELL) return false
            if (event.kind == FinancialEventKind.OPENING_BALANCE) opening = true
        }
        return opening
    }

    fun investedHasChanged(events: List<FinancialEvent>, holding: FinancialHolding): Boolean {
        val mine = events.filter { it.holdingId == holding.holdingId }
        if (mine.any { it.kind == FinancialEventKind.SELL }) return true
        if (mine.isEmpty()) return false
        val open = LotEngine.fold(holding.holdingId, holding.category, mine).openLots
        return open.count { it.remainingQtyScaled > 0L } > 1
    }

    fun profileOf(holding: FinancialHolding): InstrumentProfile {
        val text = "${holding.name} ${holding.subCategory}".lowercase()
        val key = when {
            holding.category == "GOLD_SGB" || Regex("\\bsgb\\b|sovereign gold").containsMatchIn(text) -> "SGB"
            holding.category == "REAL_ESTATE" || Regex("\\breit\\b|\\binvit\\b").containsMatchIn(text) -> "REIT"
            Regex("\\bsif\\b|specialised investment").containsMatchIn(text) -> "SIF"
            holding.category == "SPECIALISED" || Regex("\\baif\\b|\\bpms\\b|category ii|cat ii").containsMatchIn(text) -> "AIF"
            holding.category == "FIXED_INCOME" &&
                Regex("\\bppf\\b|\\bepf\\b|\\bvpf\\b|provident|sukanya|\\bssy\\b").containsMatchIn(text) -> "PPF"
            holding.category == "FIXED_INCOME" || holding.category == "CASH_BANK" ||
                Regex("\\bfd\\b|fixed deposit|term deposit|post office|\\btd\\b|recurring").containsMatchIn(text) -> "FD"
            holding.category == "MUTUAL_FUND" -> "MF"
            holding.category == "EQUITY" -> "EQUITY"
            else -> "OTHER"
        }
        return InstrumentProfile(key, "Absolute return", "Annual return", why.getValue(key))
    }

    fun holdingReturn(holding: FinancialHolding, events: List<FinancialEvent>, asOf: LocalDate): HoldingReturn {
        val own = events.filter { it.holdingId == holding.holdingId }
        val profile = profileOf(holding)
        val figures = figuresOf(holding, own)
        fun annual(reason: String, pct: Double? = null) = AnnualDisplay(
            pct, if (reason == "ok") formatPercent(pct) else "N/A", reason, annualWhy.getValue(reason),
        )
        if (holding.category != "LIABILITY" && figures.quantityScaled <= 0L) {
            return HoldingReturn(
                profile.copy(primaryLabel = "Closed", why = CLOSED),
                null, "Closed", false, annual("closed"),
            )
        }
        if (holding.category != "LIABILITY" &&
            CostBasis.isUnknownInvested(holding, own, figures.cost, figures.value)
        ) {
            return HoldingReturn(
                profile.copy(why = UNKNOWN), null, CostBasis.UNKNOWN_INVESTED_LABEL, true, annual("unknown"),
            )
        }
        val absolute = absoluteReturnPercent(figures.cost, figures.value)
        val labelled = if (isStatementOnly(own)) profile.copy(why = STATEMENT) else profile
        val shown = if (investedHasChanged(own, holding)) {
            annual("changed")
        } else {
            val start = holding.acquiredOn ?: CostBasis.earliestActiveParcel(own, holding.holdingId)?.date
            when {
                start == null -> annual("none")
                LotEngine.calendarDays(start, asOf) < 365 -> annual("under_year")
                else -> {
                    val pct = annualReturnPercent(figures.cost, figures.value, LotEngine.calendarDays(start, asOf))
                    if (pct == null) annual("none") else annual("ok", pct)
                }
            }
        }
        return HoldingReturn(labelled, absolute, formatPercent(absolute), false, shown)
    }

    private fun figuresOf(holding: FinancialHolding, own: List<FinancialEvent>): HoldingMetrics {
        if (holding.category == "LIABILITY" || own.isEmpty()) return HoldingView.metrics(holding)
        val folded = LotEngine.fold(holding.holdingId, holding.category, own)
        val price = HoldingView.priceOf(holding)
        val value = if (folded.quantityScaled > 0L) {
            MoneyScaled(timesQuantity(folded.quantityScaled, price.raw))
        } else MoneyScaled(0)
        return HoldingMetrics(folded.quantityScaled, price, value, folded.investedCost, MoneyScaled(0))
    }
}

data class InstrumentProfile(
    val key: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val why: String,
)

data class AnnualDisplay(
    val percent: Double?,
    val text: String,
    val reason: String,
    val why: String,
)

data class HoldingReturn(
    val profile: InstrumentProfile,
    val primaryPercent: Double?,
    val primaryText: String,
    val unknownInvested: Boolean,
    val annual: AnnualDisplay,
)
