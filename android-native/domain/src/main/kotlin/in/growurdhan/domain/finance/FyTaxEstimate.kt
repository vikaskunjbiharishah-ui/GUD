package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * FY equity-tax arithmetic from src/context/KoshaContext.tsx.
 * Slab and property gains are summed and not taxed: those rates are not a
 * single published figure. Nothing here is a filing total.
 */
object FyTaxEstimate {
    const val LABEL = "estimate, not advice"
    /** Exact sentence for FY 2024-25. Do not replace it with a computed rupee tax. */
    const val RATE_CHANGED_NOTE = "Rates changed during this year; not estimated"

    data class Summary(
        val financialYear: String,
        val assessmentYear: String,
        val ruleNotes: String,
        val provisional: Boolean,
        val label: String,
        val ltcgEquityGain: MoneyScaled,
        val ltcgExemption: MoneyScaled,
        val ltcgTaxable: MoneyScaled,
        val ltcgTax: MoneyScaled?,
        val stcgEquityGain: MoneyScaled,
        val stcgTax: MoneyScaled?,
        val debtSlabGain: MoneyScaled,
        val realEstateGain: MoneyScaled,
        val sgbExemptGain: MoneyScaled,
        val dividend: MoneyScaled,
        val interest: MoneyScaled,
        /** Null means unknown. It must not be shown as ₹0. */
        val estimatedTax: MoneyScaled?,
        val rateNote: String?,
        val errors: List<String>,
    ) {
        fun lines(): List<String> {
            val rows = mutableListOf(
                label,
                "FY $financialYear · $assessmentYear",
            )
            if (ruleNotes.isNotBlank()) rows += ruleNotes
            if (provisional) rows += "These rules are marked provisional."
            if (rateNote != null) rows += rateNote
            rows += "Equity long-term gain ${MoneyEngine.formatRupees(ltcgEquityGain.raw)}"
            rows += "Equity short-term gain ${MoneyEngine.formatRupees(stcgEquityGain.raw)}"
            val tax = estimatedTax
            val longTax = ltcgTax
            val shortTax = stcgTax
            if (rateNote == null && tax != null && longTax != null && shortTax != null) {
                rows += "Equity long-term exemption ${MoneyEngine.formatRupees(ltcgExemption.raw)}"
                rows += "Estimated equity long-term tax ${MoneyEngine.formatRupees(longTax.raw)}"
                rows += "Estimated equity short-term tax ${MoneyEngine.formatRupees(shortTax.raw)}"
                rows += "Estimated tax ${MoneyEngine.formatRupees(tax.raw)}. Equity rates only."
            }
            rows += "Debt and slab gains ${MoneyEngine.formatRupees(debtSlabGain.raw)} are not estimated."
            rows += "Property gains ${MoneyEngine.formatRupees(realEstateGain.raw)} are not given one rate here."
            rows += "SGB certificate redemption gain ${MoneyEngine.formatRupees(sgbExemptGain.raw)}"
            rows += "Dividends ${MoneyEngine.formatRupees(dividend.raw)}. Interest ${MoneyEngine.formatRupees(interest.raw)}. Not in the estimated tax."
            if (errors.isNotEmpty()) rows += "This estimate is incomplete."
            return rows
        }
    }

    fun summarize(
        holdings: List<FinancialHolding>,
        events: List<FinancialEvent>,
        financialYear: String,
        asOf: LocalDate,
        fmvByHolding: Map<HoldingId, MoneyScaled> = emptyMap(),
    ): Summary {
        val anchor = anchorOf(financialYear)
        val assessmentYear = LotEngine.assessmentYear(anchor)
        val rules = IndianTaxRules.forAssessmentYear(assessmentYear)
        if (rules == null) {
            return blank(
                financialYear, assessmentYear,
                listOf("Tax rules for $assessmentYear are not available. This year cannot be computed."),
            )
        }
        val byId = holdings.associateBy { it.holdingId }
        val errors = mutableListOf<String>()
        var ltcg = 0L
        var stcg = 0L
        var debt = 0L
        var property = 0L
        var sgb = 0L
        for (holding in holdings) {
            val rows = events.filter { it.holdingId == holding.holdingId }
            if (rows.any { it.ownerId != holding.ownerId }) {
                errors += "A ledger row does not match the holding owner."
                continue
            }
            val folded = LotEngine.fold(
                holding.holdingId, holding.category, rows.filter { it.date <= asOf },
            )
            errors += folded.errors
            val fmv = fmvByHolding[holding.holdingId]
            for (disposal in folded.disposals) {
                if (LotEngine.fiscalYear(disposal.soldOn) != financialYear) continue
                val cost = IndianTaxRules.grandfatheredCost(
                    rules, disposal.acquiredOn, disposal.cost, disposal.proceeds, fmv,
                )
                val gain = Math.subtractExact(disposal.proceeds.raw, cost.raw)
                when (IndianTaxRules.classify(
                    holding.category, holding.subCategory, disposal.longTerm, holding.goldHoldMode,
                )) {
                    GainClass.LTCG_EQUITY -> ltcg = Math.addExact(ltcg, gain)
                    GainClass.STCG_EQUITY -> stcg = Math.addExact(stcg, gain)
                    GainClass.DEBT_SLAB -> debt = Math.addExact(debt, gain)
                    GainClass.REAL_ESTATE_LTCG -> property = Math.addExact(property, gain)
                    GainClass.SGB_EXEMPT -> sgb = Math.addExact(sgb, gain)
                }
            }
        }
        val orphan = events.filter { it.holdingId !in byId && it.date <= asOf &&
            LotEngine.fiscalYear(it.date) == financialYear &&
            it.kind in setOf(FinancialEventKind.DIVIDEND, FinancialEventKind.INTEREST, FinancialEventKind.SELL)
        }
        if (orphan.isNotEmpty()) errors += "A sale or income row has no holding."
        var dividend = 0L
        var interest = 0L
        for (event in events) {
            val holding = byId[event.holdingId] ?: continue
            if (event.ownerId != holding.ownerId) continue
            if (event.date > asOf || LotEngine.fiscalYear(event.date) != financialYear) continue
            val cash = cashOf(event)
            when (event.kind) {
                FinancialEventKind.DIVIDEND -> dividend = Math.addExact(dividend, cash)
                FinancialEventKind.INTEREST -> interest = Math.addExact(interest, cash)
                else -> Unit
            }
        }
        if (!rules.singleEquityRate) {
            // s.112A(2) proviso: ₹1,25,000 covers both date buckets together.
            // The Act does not say which bucket is reduced first. One rate on
            // every sale in FY 2024-25 would be wrong, so tax stays unknown.
            return filled(
                financialYear, assessmentYear, rules, ltcg, stcg, debt, property, sgb,
                dividend, interest, errors,
                exemption = 0L, ltcgTaxable = 0L, ltcgTax = null, stcgTax = null,
                rateNote = RATE_CHANGED_NOTE,
            )
        }
        val exemption = Math.multiplyExact(rules.ltcgExemptionRupees, 10_000L)
        val ltcgTaxable = if (ltcg > exemption) Math.subtractExact(ltcg, exemption) else 0L
        val ltcgTax = IndianTaxRules.applyRate(MoneyScaled(ltcgTaxable), rules.ltcgEquityBps)
        val stcgBase = if (stcg > 0L) stcg else 0L
        val stcgTax = IndianTaxRules.applyRate(MoneyScaled(stcgBase), rules.stcgEquityBps)
        return filled(
            financialYear, assessmentYear, rules, ltcg, stcg, debt, property, sgb,
            dividend, interest, errors,
            exemption = exemption, ltcgTaxable = ltcgTaxable, ltcgTax = ltcgTax, stcgTax = stcgTax,
            rateNote = null,
        )
    }

    private fun filled(
        financialYear: String,
        assessmentYear: String,
        rules: TaxRuleSet,
        ltcg: Long,
        stcg: Long,
        debt: Long,
        property: Long,
        sgb: Long,
        dividend: Long,
        interest: Long,
        errors: List<String>,
        exemption: Long,
        ltcgTaxable: Long,
        ltcgTax: MoneyScaled?,
        stcgTax: MoneyScaled?,
        rateNote: String?,
    ) = Summary(
        financialYear = financialYear,
        assessmentYear = assessmentYear,
        ruleNotes = rules.notes,
        provisional = rules.provisional,
        label = LABEL,
        ltcgEquityGain = MoneyScaled(ltcg),
        ltcgExemption = MoneyScaled(exemption),
        ltcgTaxable = MoneyScaled(ltcgTaxable),
        ltcgTax = ltcgTax,
        stcgEquityGain = MoneyScaled(stcg),
        stcgTax = stcgTax,
        debtSlabGain = MoneyScaled(debt),
        realEstateGain = MoneyScaled(property),
        sgbExemptGain = MoneyScaled(sgb),
        dividend = MoneyScaled(dividend),
        interest = MoneyScaled(interest),
        estimatedTax = if (ltcgTax == null || stcgTax == null) null else ltcgTax + stcgTax,
        rateNote = rateNote,
        errors = errors,
    )

    private fun cashOf(event: FinancialEvent): Long {
        if (event.totalAmount.raw != 0L) return event.totalAmount.raw
        val price = event.unitPrice ?: return 0L
        return timesQuantity(kotlin.math.abs(event.quantityScaled), price.raw)
    }

    private fun anchorOf(financialYear: String): LocalDate {
        val match = Regex("""^(\d{4})-(\d{2})$""").matchEntire(financialYear)
            ?: throw IllegalArgumentException("Financial year must look like 2025-26.")
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        require(end == (start + 1) % 100) { "Financial year is not a consecutive pair." }
        val anchor = LocalDate.of(start, 4, 1)
        require(LotEngine.fiscalYear(anchor) == financialYear)
        return anchor
    }

    private fun blank(financialYear: String, assessmentYear: String, errors: List<String>) = Summary(
        financialYear = financialYear,
        assessmentYear = assessmentYear,
        ruleNotes = "",
        provisional = false,
        label = LABEL,
        ltcgEquityGain = MoneyScaled(0),
        ltcgExemption = MoneyScaled(0),
        ltcgTaxable = MoneyScaled(0),
        ltcgTax = null,
        stcgEquityGain = MoneyScaled(0),
        stcgTax = null,
        debtSlabGain = MoneyScaled(0),
        realEstateGain = MoneyScaled(0),
        sgbExemptGain = MoneyScaled(0),
        dividend = MoneyScaled(0),
        interest = MoneyScaled(0),
        estimatedTax = null,
        rateNote = null,
        errors = errors,
    )
}

data class OwnerFyTax(
    val ownerId: OwnerId,
    val summary: FyTaxEstimate.Summary,
)
