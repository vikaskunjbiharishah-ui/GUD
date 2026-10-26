package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.math.BigInteger
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import java.time.LocalDate
import java.util.Locale

/**
 * Reference inputs: src/lib/interest.ts, Jul–Sep 2026 small-savings rates
 * and FY 2025-26 EPF rate. Not a rate history or authoritative tax service.
 * Money and quantities remain scaled integers; no floating-point balances.
 */
enum class SavingsKind { SGB, PPF, EPF, SSY, FD, SCSS, POMIS, NSC, KVP, RD, FRSB, BOND, OTHER }
/**
 * These are financial projections, NOT automatically posted ledger events. A
 * mutable amount entered by the user must be verified before catch-up is shown.
 */
enum class SavingsInterestMode { COMPOUND, PAYOUT }
data class SavingsInterestCredit(
    val postedOn: LocalDate,
    val amount: MoneyScaled,
    val estimated: Boolean,
)
data class SavingsInterestReview(
    val credits: List<SavingsInterestCredit>,
    /** Present only for products where interest compounds into the holding. */
    val projectedBalance: MoneyScaled?,
    val assumption: String,
)

object SavingsInterestEngine {
    private val publishedBps = mapOf(
        SavingsKind.SGB to 250, SavingsKind.PPF to 710, SavingsKind.EPF to 825,
        SavingsKind.SSY to 820, SavingsKind.SCSS to 820, SavingsKind.POMIS to 740,
        SavingsKind.NSC to 770, SavingsKind.KVP to 750, SavingsKind.RD to 670,
        SavingsKind.FRSB to 805,
    )
    fun defaultRateBps(kind: SavingsKind): Int? = publishedBps[kind]

    fun kindForSubcategory(subcategory: String): SavingsKind? {
        val sub = subcategory.lowercase()
        fun has(word: String) = Regex("\\b" + Regex.escape(word) + "\\b").containsMatchIn(sub)
        return when {
            "sovereign gold" in sub || has("sgb") -> SavingsKind.SGB
            has("ppf") || "public provident" in sub -> SavingsKind.PPF
            has("epf") || has("vpf") || "provident fund" in sub -> SavingsKind.EPF
            "sukanya" in sub || has("ssy") -> SavingsKind.SSY
            has("fd") || "fixed deposit" in sub || "term deposit" in sub -> SavingsKind.FD
            has("scss") || "senior citizen savings" in sub -> SavingsKind.SCSS
            "pomis" in sub || "monthly income" in sub -> SavingsKind.POMIS
            has("nsc") || "national savings certificate" in sub -> SavingsKind.NSC
            has("kvp") || "kisan vikas" in sub -> SavingsKind.KVP
            has("rd") || "recurring deposit" in sub -> SavingsKind.RD
            "floating rate" in sub || "frsb" in sub -> SavingsKind.FRSB
            "bond" in sub || "goi" in sub -> SavingsKind.BOND
            else -> null
        }
    }

    fun wholeMonths(from: LocalDate, to: LocalDate): Int {
        if (to < from) return 0
        var months = (to.year - from.year) * 12 + to.monthValue - from.monthValue
        if (to.dayOfMonth < from.dayOfMonth) months--
        return months.coerceAtLeast(0)
    }

    fun fyEndDatesAfter(after: LocalDate, asOf: LocalDate): List<LocalDate> {
        if (after >= asOf) return emptyList()
        var year = after.year
        if (after.monthValue > 3 || after == LocalDate.of(after.year, 3, 31)) year++
        val dates = mutableListOf<LocalDate>()
        while (year <= asOf.year && dates.size <= 80) {
            val end = LocalDate.of(year, 3, 31)
            if (end > after && end <= asOf) dates += end
            year++
        }
        return dates
    }

    fun monthsHeldInFy(acquired: LocalDate?, fyEnd: LocalDate): Int {
        require(fyEnd.monthValue == 3 && fyEnd.dayOfMonth == 31)
        val start = LocalDate.of(fyEnd.year - 1, 4, 1)
        val from = if (acquired != null && acquired > start) acquired else start
        if (from > fyEnd) return 0
        return if (from <= start) 12 else wholeMonths(from, fyEnd).coerceAtMost(12)
    }

    /** TS interestMinor truncates the scaled result, never rounds upward. */
    fun annualAccrual(principal: MoneyScaled, rateBps: Int, months: Int): MoneyScaled {
        require(rateBps in 0..100_000 && months in 0..12)
        if (principal.raw <= 0 || rateBps == 0 || months == 0) return MoneyScaled(0)
        return MoneyScaled((
            BigInteger.valueOf(principal.raw) * BigInteger.valueOf(rateBps.toLong()) *
                BigInteger.valueOf(months.toLong()) / BigInteger.valueOf(120_000L)
            ).toCheckedLong())
    }

    /** TS periodInterest; a payout is not compounded into the investment. */
    fun coupon(principal: MoneyScaled, rateBps: Int, payoutsPerYear: Int): MoneyScaled {
        require(rateBps in 0..100_000 && payoutsPerYear in 1..12)
        if (principal.raw <= 0 || rateBps == 0) return MoneyScaled(0)
        return MoneyScaled((
            BigInteger.valueOf(principal.raw) * BigInteger.valueOf(rateBps.toLong()) /
                BigInteger.valueOf(10_000L * payoutsPerYear)
            ).toCheckedLong())
    }

    /** TS steppedDates: iterative month clamping with an optional maturity cap. */
    fun payoutDates(
        acquired: LocalDate, stepMonths: Int, asOf: LocalDate, maturity: LocalDate? = null,
    ): List<LocalDate> {
        require(stepMonths in 1..12)
        val dates = mutableListOf<LocalDate>()
        var cursor = LoanEmiEngine.addCalendarMonths(acquired, stepMonths.toLong())
        repeat(480) {
            if (cursor > asOf || (maturity != null && cursor > maturity)) return dates
            dates += cursor
            if (maturity != null && cursor == maturity) return dates
            cursor = LoanEmiEngine.addCalendarMonths(cursor, stepMonths.toLong())
        }
        return dates
    }

    /**
     * Matches src/lib/interest.ts compoundQuarterlyMinor for a fixed
     * principal and a single FY interval: one final rounding, not a
     * rounded payment every quarter. Rate is still an assumption.
     */
    fun quarterlyCompoundAccrual(
        principal: MoneyScaled, rateBps: Int, months: Int,
    ): MoneyScaled {
        require(rateBps in 0..100_000 && months in 0..12)
        if (principal.raw <= 0 || rateBps == 0 || months == 0) return MoneyScaled(0)
        val factor = (1.0 + rateBps / 40_000.0).pow(months / 3.0)
        require(factor.isFinite() && factor > 0.0)
        val after = BigDecimal.valueOf(principal.raw).multiply(BigDecimal.valueOf(factor))
            .setScale(0, RoundingMode.HALF_UP)
            .toBigIntegerExact().toCheckedLong()
        return MoneyScaled(maxOf(0L, Math.subtractExact(after, principal.raw)))
    }

    /**
     * Review catch-up for income-paying and balance-compounding products. It
     * honors reported maturity and posted dates, but cannot model PPF/SSY
     * daily balances without the actual contribution transaction history.
     * If original/accrued balance is a verified manual input, never overwrite.
     */
    fun review(
        kind: SavingsKind,
        mode: SavingsInterestMode,
        startingBalance: MoneyScaled,
        rateBps: Int,
        acquiredOn: LocalDate,
        asOf: LocalDate,
        maturityOn: LocalDate? = null,
        postedDates: Set<LocalDate> = emptySet(),
        manuallyVerified: Boolean = false,
    ): SavingsInterestReview? {
        require(rateBps in 0..100_000)
        if (startingBalance.raw <= 0 || rateBps <= 0 || acquiredOn > asOf) return null
        if (mode == SavingsInterestMode.COMPOUND && manuallyVerified) return null
        val horizon = if (maturityOn != null && maturityOn < asOf) maturityOn else asOf
        if (horizon < acquiredOn) return null
        val dates: List<LocalDate> = when (mode) {
            SavingsInterestMode.COMPOUND ->
                fyEndDatesAfter(acquiredOn, horizon)
            SavingsInterestMode.PAYOUT ->
                if (kind == SavingsKind.FRSB) {
                    buildList {
                        for (year in acquiredOn.year..horizon.year) {
                            for (month in listOf(1, 7)) {
                                val day = LocalDate.of(year, month, 1)
                                if (day > acquiredOn && day <= horizon) add(day)
                            }
                        }
                    }
                } else {
                    val interval = when (kind) {
                        SavingsKind.POMIS -> 1
                        SavingsKind.SCSS -> 3
                        SavingsKind.FD -> 3
                        SavingsKind.SGB, SavingsKind.FRSB, SavingsKind.BOND -> 6
                        else -> 12
                    }
                    payoutDates(acquiredOn, interval, horizon, maturityOn)
                }
        }
        if (dates.isEmpty()) return null
        var projected = startingBalance.raw
        val results = mutableListOf<SavingsInterestCredit>()
        for (date in dates) {
            val accrual = if (mode == SavingsInterestMode.PAYOUT) {
                val frequency = when (kind) {
                    SavingsKind.POMIS -> 12
                    SavingsKind.SCSS, SavingsKind.FD -> 4
                    SavingsKind.SGB, SavingsKind.FRSB, SavingsKind.BOND -> 2
                    else -> 1
                }
                coupon(startingBalance, rateBps, frequency)
            } else {
                val months = monthsHeldInFy(acquiredOn, date)
                if (kind == SavingsKind.RD || kind == SavingsKind.FD) {
                    quarterlyCompoundAccrual(MoneyScaled(projected), rateBps, months)
                } else annualAccrual(MoneyScaled(projected), rateBps, months)
            }
            if (mode == SavingsInterestMode.COMPOUND) {
                projected = Math.addExact(projected, accrual.raw)
            }
            if (accrual.raw > 0 && date !in postedDates) {
                results += SavingsInterestCredit(date, accrual, estimated = true)
            }
        }
        if (results.isEmpty()) return null
        return SavingsInterestReview(
            credits = results,
            projectedBalance = if (mode == SavingsInterestMode.COMPOUND)
                MoneyScaled(projected) else null,
            assumption = "Historical balances and rate changes are not fully known; confirm against the passbook.",
        )
    }

    /** src/lib/interest.ts interestKindOf. NPS is not a small-savings product. */
    fun interestKindOf(holding: FinancialHolding, statedRate: Double? = null): SavingsKind? {
        if (holding.category == "LIABILITY") return null
        val sub = holding.subCategory.lowercase()
        val sgbName = Regex("\\bSGB\\b").containsMatchIn(holding.name.uppercase()) ||
            "SOVEREIGN GOLD" in holding.name.uppercase()
        if (holding.category == "GOLD_SGB") {
            if (Regex("physical|digital").containsMatchIn(sub)) return null
            if (Regex("\\bother\\b").containsMatchIn(sub) && !sgbName) return null
            if (Regex("sovereign|\\bsgb\\b").containsMatchIn(sub) || sgbName || sub.isEmpty()) return SavingsKind.SGB
            return null
        }
        if (sgbName && !Regex("physical|digital").containsMatchIn(sub)) return SavingsKind.SGB
        if (holding.category != "FIXED_INCOME") return null
        val blob = "${holding.subCategory} ${holding.name}"
        if (Regex("\\bnps\\b").containsMatchIn(blob.lowercase())) return null
        kindForSubcategory(blob)?.let { return it }
        if (sub == "other" && (statedRate ?: 0.0) > 0.0) return SavingsKind.OTHER
        return null
    }

    /**
     * Catch-up from src/lib/interest.ts. A payout is cash and does not change
     * the holding. A typed "Manual Verified" balance is not rebuilt.
     * When the stored balance is still the deposit, earlier 31 March credits
     * are projected. A balance that has already moved is only credited forward.
     */
    fun planInterest(
        terms: SchemeTerms,
        asOf: LocalDate,
        alreadyPosted: Set<LocalDate> = emptySet(),
    ): InterestCatchUp? {
        val spec = interestSpec(terms) ?: return null
        val acquired = terms.holding.acquiredOn
        val locked = Regex("Manual Verified").containsMatchIn(terms.priceSource)
        val deposited = investedOf(terms.holding)
        val projectFromDeposit = spec.mode == SavingsInterestMode.COMPOUND &&
            !locked && acquired != null && deposited > 0L && spec.principal.raw == deposited
        val dates = if (spec.mode == SavingsInterestMode.PAYOUT) {
            payoutSchedule(terms, spec, asOf).filter { it !in alreadyPosted }
        } else if (locked) {
            return null
        } else if (projectFromDeposit) {
            fyEndDatesAfter(acquired!!, asOf).filter { terms.maturityOn == null || it <= terms.maturityOn }
        } else {
            val after = laterDate(terms.valuedOn, acquired) ?: return null
            fyEndDatesAfter(after, asOf).filter {
                it !in alreadyPosted && (terms.maturityOn == null || it <= terms.maturityOn)
            }
        }
        if (dates.isEmpty()) return null
        var principal = if (projectFromDeposit) deposited else spec.principal.raw
        val events = mutableListOf<PlannedInterest>()
        for (date in dates) {
            if (!projectFromDeposit && date in alreadyPosted) continue
            val contrib = if (spec.mode == SavingsInterestMode.COMPOUND) contributionThisFy(terms, spec.kind, date) else null
            var base = principal
            if (contrib != null && contrib.earnsThisYear) base = Math.addExact(base, contrib.amount.raw)
            val amount = when {
                spec.mode == SavingsInterestMode.PAYOUT ->
                    coupon(MoneyScaled(principal), spec.rateBps, spec.periodsPerYear)
                spec.quarterlyCompound ->
                    quarterlyCompoundAccrual(MoneyScaled(base), spec.rateBps, monthsHeldInFy(acquired, date))
                else -> annualAccrual(MoneyScaled(base), spec.rateBps, monthsHeldInFy(acquired, date))
            }
            if (amount.raw <= 0L) {
                if (spec.mode == SavingsInterestMode.COMPOUND && contrib != null && !contrib.earnsThisYear) {
                    principal = Math.addExact(principal, contrib.amount.raw)
                }
                continue
            }
            if (date !in alreadyPosted) {
                events += PlannedInterest(date, amount, interestNote(spec, date))
            }
            if (spec.mode == SavingsInterestMode.COMPOUND) {
                principal = Math.addExact(base, amount.raw)
                if (contrib != null && !contrib.earnsThisYear) principal = Math.addExact(principal, contrib.amount.raw)
            }
        }
        if (events.isEmpty()) return null
        val grown = if (spec.mode == SavingsInterestMode.COMPOUND) MoneyScaled(principal) else null
        if (grown != null && grown.raw == terms.balance.raw) return null
        return InterestCatchUp(events, grown)
    }

    fun planMonthlyIncome(
        terms: SchemeTerms,
        asOf: LocalDate,
        alreadyPosted: Set<LocalDate> = emptySet(),
    ): List<PlannedInterest>? {
        val amount = terms.monthlyIncome ?: return null
        if (amount.raw <= 0L) return null
        val start = terms.incomeStart ?: terms.holding.acquiredOn ?: return null
        val dates = monthlyIncomeDates(start, terms.incomeDay ?: 1, asOf, terms.incomeEnd)
            .filter { it !in alreadyPosted }
        if (dates.isEmpty()) return null
        return dates.map { PlannedInterest(it, amount, "Monthly income on $it") }
    }

    fun nextMonthlyIncomeDate(terms: SchemeTerms, asOf: LocalDate): LocalDate? {
        val amount = terms.monthlyIncome ?: return null
        if (amount.raw <= 0L) return null
        val start = terms.incomeStart ?: terms.holding.acquiredOn ?: return null
        val horizon = LoanEmiEngine.addCalendarMonths(asOf, 36)
        return monthlyIncomeDates(start, terms.incomeDay ?: 1, horizon, terms.incomeEnd)
            .firstOrNull { it >= asOf }
    }

    private data class Spec(
        val kind: SavingsKind,
        val mode: SavingsInterestMode,
        val rateBps: Int,
        val ratePercent: Double,
        val principal: MoneyScaled,
        val periodsPerYear: Int,
        val stepMonths: Int,
        val quarterlyCompound: Boolean,
    )

    private fun interestSpec(terms: SchemeTerms): Spec? {
        val kind = interestKindOf(terms.holding, terms.ratePercent) ?: return null
        val stated = if (kind == SavingsKind.SGB) terms.couponRatePercent else terms.ratePercent
        val rateBps = if (stated != null && stated > 0.0) java.lang.Math.round(stated * 100.0).toInt()
            else defaultRateBps(kind) ?: return null
        if (rateBps <= 0) return null
        if (kind == SavingsKind.FD && terms.compounding == null) return null
        val periods = periodsFor(kind, terms.compounding)
        val ratePercent = rateBps / 100.0
        if (kind == SavingsKind.SGB) {
            val issue = terms.issuePrice ?: return null
            val principal = timesQuantity(terms.holding.storedQuantityScaled, issue.raw)
            if (principal <= 0L) return null
            return Spec(kind, SavingsInterestMode.PAYOUT, rateBps, ratePercent, MoneyScaled(principal), 2, 6, false)
        }
        if (kind == SavingsKind.BOND) {
            val face = terms.issuePrice?.takeIf { it.raw > 0L } ?: terms.balance
            if (face.raw <= 0L) return null
            val principal = timesQuantity(terms.holding.storedQuantityScaled, face.raw)
            if (principal <= 0L) return null
            return Spec(kind, SavingsInterestMode.PAYOUT, rateBps, ratePercent, MoneyScaled(principal), periods.first, periods.second, false)
        }
        if (terms.balance.raw <= 0L && kind != SavingsKind.FD) return null
        if (kind in setOf(SavingsKind.SCSS, SavingsKind.POMIS, SavingsKind.FRSB)) {
            if (terms.balance.raw <= 0L) return null
            return Spec(kind, SavingsInterestMode.PAYOUT, rateBps, ratePercent, terms.balance, periods.first, periods.second, false)
        }
        if (kind == SavingsKind.FD) {
            val cumulative = terms.compounding == "CUMULATIVE"
            val deposited = investedOf(terms.holding)
            val principal = if (!cumulative && deposited > 0L) deposited else terms.balance.raw
            if (principal <= 0L) return null
            return Spec(
                kind, if (cumulative) SavingsInterestMode.COMPOUND else SavingsInterestMode.PAYOUT,
                rateBps, ratePercent, MoneyScaled(principal), periods.first, periods.second, cumulative,
            )
        }
        if (terms.balance.raw <= 0L) return null
        val rd = kind == SavingsKind.RD
        return Spec(
            kind, SavingsInterestMode.COMPOUND, rateBps, ratePercent, terms.balance,
            if (rd) 4 else 1, periods.second, rd,
        )
    }

    private fun periodsFor(kind: SavingsKind, compounding: String?): Pair<Int, Int> = when (kind) {
        SavingsKind.FD -> when (compounding) {
            "MONTHLY" -> 12 to 1
            "HALF_YEARLY" -> 2 to 6
            "YEARLY" -> 1 to 12
            else -> 4 to 3
        }
        SavingsKind.POMIS -> 12 to 1
        SavingsKind.SCSS, SavingsKind.RD -> 4 to 3
        else -> 2 to 6
    }

    private fun payoutSchedule(terms: SchemeTerms, spec: Spec, asOf: LocalDate): List<LocalDate> {
        val acquired = terms.holding.acquiredOn ?: return emptyList()
        if (spec.kind == SavingsKind.FRSB) {
            val out = mutableListOf<LocalDate>()
            for (year in acquired.year..asOf.year + 1) {
                for (month in listOf(1, 7)) {
                    val day = LocalDate.of(year, month, 1)
                    if (day <= acquired || day > asOf) continue
                    if (terms.maturityOn != null && day > terms.maturityOn) continue
                    out += day
                }
            }
            return out
        }
        return payoutDates(acquired, spec.stepMonths, asOf, terms.maturityOn)
    }

    private fun contributionThisFy(terms: SchemeTerms, kind: SavingsKind, fyEnd: LocalDate): FyContribution? {
        if (terms.compounding != null) return null
        if (kind != SavingsKind.PPF && kind != SavingsKind.SSY) return null
        val amount = terms.annualContribution ?: return null
        val whenPaid = terms.contributionOn ?: return null
        if (amount.raw <= 0L) return null
        val start = LocalDate.of(fyEnd.year - 1, 4, 1)
        if (whenPaid < start || whenPaid > fyEnd) return null
        return FyContribution(amount, earnsThisYear = whenPaid.dayOfMonth <= 5)
    }

    private fun investedOf(holding: FinancialHolding): Long {
        val cost = holding.openingCostPerUnit ?: return 0L
        if (holding.storedQuantityScaled <= 0L || cost.raw <= 0L) return 0L
        return timesQuantity(holding.storedQuantityScaled, cost.raw)
    }

    private fun laterDate(a: LocalDate?, b: LocalDate?): LocalDate? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    private fun interestNote(spec: Spec, date: LocalDate): String {
        val rate = formatRate(spec.ratePercent)
        val fy = fyLabel(date)
        return when (spec.kind) {
            SavingsKind.SGB -> "SGB half-year coupon on $date ($rate% on the issue price)"
            SavingsKind.PPF -> "PPF interest for FY $fy ($rate%)"
            SavingsKind.EPF -> "EPF interest for FY $fy ($rate%, passbook date 31 March)"
            SavingsKind.SSY -> "SSY interest for FY $fy ($rate%)"
            SavingsKind.FD -> if (spec.mode == SavingsInterestMode.COMPOUND)
                "FD interest for FY $fy ($rate%, compounded quarterly)"
            else "FD interest paid on $date ($rate%)"
            SavingsKind.SCSS -> "SCSS quarterly interest on $date ($rate%)"
            SavingsKind.POMIS -> "POMIS monthly interest on $date ($rate%)"
            SavingsKind.NSC -> "NSC accrual for FY $fy ($rate%, paid only at maturity)"
            SavingsKind.KVP -> "KVP accrual for FY $fy ($rate%, paid only at maturity)"
            SavingsKind.RD -> "RD interest for FY $fy ($rate%, quarterly compound on the balance entered)"
            SavingsKind.FRSB -> "RBI floating-rate bond interest on $date ($rate%)"
            SavingsKind.BOND -> "Bond coupon on $date ($rate% on face value)"
            SavingsKind.OTHER -> "Interest for FY $fy ($rate%)"
        }
    }

    private fun formatRate(percent: Double): String {
        if (percent % 1.0 == 0.0) return percent.toInt().toString()
        return String.format(Locale.US, "%.2f", percent).trimEnd('0').trimEnd('.')
    }

    private fun fyLabel(date: LocalDate): String {
        val endYear = if (date.monthValue > 3) date.year + 1 else date.year
        return "${endYear - 1}-${(endYear % 100).toString().padStart(2, '0')}"
    }

    private fun monthlyIncomeDates(
        start: LocalDate, day: Int, asOf: LocalDate, end: LocalDate?,
    ): List<LocalDate> {
        val payday = day.coerceIn(1, 28)
        var cursor = LocalDate.of(start.year, start.monthValue, minOf(payday, start.lengthOfMonth()))
        if (cursor < start) cursor = cursor.plusMonths(1).withDayOfMonth(payday)
        val out = mutableListOf<LocalDate>()
        repeat(480) {
            if (cursor > asOf || (end != null && cursor > end)) return out
            out += cursor
            cursor = cursor.plusMonths(1).withDayOfMonth(payday)
        }
        return out
    }
}

private data class FyContribution(val amount: MoneyScaled, val earnsThisYear: Boolean)

/** Inputs that src/lib/interest.ts reads off a holding and its details. */
data class SchemeTerms(
    val holding: FinancialHolding,
    val balance: MoneyScaled,
    val ratePercent: Double? = null,
    val couponRatePercent: Double? = null,
    val issuePrice: MoneyScaled? = null,
    val priceSource: String = "",
    val valuedOn: LocalDate? = null,
    val maturityOn: LocalDate? = null,
    val compounding: String? = null,
    val annualContribution: MoneyScaled? = null,
    val contributionOn: LocalDate? = null,
    val monthlyIncome: MoneyScaled? = null,
    val incomeDay: Int? = null,
    val incomeStart: LocalDate? = null,
    val incomeEnd: LocalDate? = null,
)

data class PlannedInterest(val date: LocalDate, val amount: MoneyScaled, val notes: String)

data class InterestCatchUp(val events: List<PlannedInterest>, val grownBalance: MoneyScaled?)
