package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate

/**
 * How a holding is shown, from src/lib/holdingView.ts.
 * Names are cleaned for the screen only. Stored names stay unchanged.
 */
object HoldingView {
    val buckets = listOf(
        FamilyBucket("SHARES", "Shares", "Stocks in your demat account"),
        FamilyBucket("FUNDS", "Mutual funds", "Folios with CAMS or KFintech"),
        FamilyBucket("GOLD", "Gold", "Sovereign gold bonds and physical gold"),
        FamilyBucket("GOVT", "Govt schemes", "PPF, EPF, NPS, Sukanya"),
        FamilyBucket("PROPERTY", "Property", "Home, land, REITs"),
        FamilyBucket("BANK", "Bank & deposits", "Savings and fixed deposits"),
        FamilyBucket("SPECIAL", "AIF & others", "PMS, AIF, unlisted"),
        FamilyBucket("LOAN", "Loans", "What the family owes"),
    )
    private val amcPrefixes = listOf(
        "Aditya Birla Sun Life", "Baroda BNP Paribas", "Mahindra Manulife", "WhiteOak Capital",
        "Motilal Oswal", "Canara Robeco", "ICICI Prudential", "Nippon India", "Parag Parikh",
        "Mirae Asset", "JM Financial", "Bank of India", "Edelweiss", "WhiteOak", "Bandhan",
        "Sundaram", "Franklin", "Invesco", "Kotak", "HDFC", "Axis", "Quant", "PPFAS", "Groww",
        "Tata", "HSBC", "Navi", "DSP", "UTI", "SBI", "LIC", "ITI",
    ).sortedByDescending { it.length }

    fun orderBuckets(present: Set<String>): List<FamilyBucket> {
        val filled = buckets.filter { it.id in present }.map { it.copy(empty = false) }
        val unused = buckets.filter { it.id !in present }.map { it.copy(empty = true) }
        return filled + unused
    }

    fun looksLikeSgb(name: String?): Boolean {
        val upper = (name ?: "").uppercase()
        return Regex("\\bSGB\\b").containsMatchIn(upper) || "SOVEREIGN GOLD" in upper
    }

    fun bucketOf(holding: FinancialHolding): String {
        if (holding.category == "GOLD_SGB" || looksLikeSgb(holding.name)) return "GOLD"
        return when (holding.category) {
            "EQUITY" -> "SHARES"
            "MUTUAL_FUND" -> "FUNDS"
            "REAL_ESTATE" -> "PROPERTY"
            "CASH_BANK" -> "BANK"
            "LIABILITY" -> "LOAN"
            "SPECIALISED" -> "SPECIAL"
            "FIXED_INCOME" -> {
                val text = "${holding.subCategory} ${holding.name}".lowercase()
                if (Regex("\\bfd\\b|deposit|\\brd\\b").containsMatchIn(text)) "BANK" else "GOVT"
            }
            else -> "FUNDS"
        }
    }

    fun plainCategory(category: String): String = when (category) {
        "EQUITY" -> "Share"
        "MUTUAL_FUND" -> "Mutual fund"
        "GOLD_SGB" -> "Gold"
        "FIXED_INCOME" -> "Govt / deposit"
        "REAL_ESTATE" -> "Property"
        "SPECIALISED" -> "AIF / PMS"
        "CASH_BANK" -> "Bank"
        "LIABILITY" -> "Loan"
        else -> category
    }

    fun eventLabel(kind: FinancialEventKind, notes: String = ""): String = when (kind) {
        FinancialEventKind.OPENING_BALANCE -> "Opening from statement"
        FinancialEventKind.SNAPSHOT_ADJUSTMENT -> "Updated to match statement"
        FinancialEventKind.BUY -> "Bought"
        FinancialEventKind.SELL -> "Sold"
        FinancialEventKind.BONUS -> "Bonus shares"
        FinancialEventKind.SPLIT -> "Share split"
        FinancialEventKind.DIVIDEND -> "Dividend"
        FinancialEventKind.INTEREST -> "Interest"
        FinancialEventKind.VALUATION_UPDATE ->
            if (Regex("loan", RegexOption.IGNORE_CASE).containsMatchIn(notes)) "Loan recorded" else "Value updated"
        FinancialEventKind.REPAYMENT -> "Loan repayment"
        FinancialEventKind.CORRECTION -> "Buy price you entered"
    }

    fun unitWord(holding: FinancialHolding, altHoldMode: String? = null): String {
        if (holding.category == "GOLD_SGB") return "grams"
        if (holding.category == "EQUITY") return "shares"
        if (holding.category == "SPECIALISED" &&
            Regex("unlisted|esop", RegexOption.IGNORE_CASE).containsMatchIn(holding.subCategory)
        ) return "shares"
        if (!usesUnits(holding, altHoldMode)) return ""
        return "units"
    }

    fun isCertificateSgb(holding: FinancialHolding): Boolean {
        if (holding.category != "GOLD_SGB") return false
        if (Regex("physical|digital|other").containsMatchIn(holding.subCategory.lowercase())) return false
        return holding.goldHoldMode == "CERTIFICATE"
    }

    fun priceOf(holding: FinancialHolding, ibjaPerGram: MoneyScaled? = null, benchmark: MoneyScaled? = null): MoneyScaled {
        if (isCertificateSgb(holding)) {
            if (ibjaPerGram != null && ibjaPerGram.raw > 0L) return ibjaPerGram
            if (benchmark != null && benchmark.raw > 0L) return benchmark
        }
        return holding.unitPrice
    }

    /** A loan's value is what is still owed. The stored cost is the original amount, not a share price. */
    fun metrics(holding: FinancialHolding, price: MoneyScaled = priceOf(holding)): HoldingMetrics {
        if (holding.category == "LIABILITY") {
            val outstanding = holding.unitPrice
            val sanctioned = holding.openingCostPerUnit
            return HoldingMetrics(
                quantityScaled = 0L,
                price = outstanding,
                value = outstanding,
                cost = if (sanctioned != null && sanctioned.raw > 0L) sanctioned else outstanding,
                gain = MoneyScaled(0),
            )
        }
        val qty = holding.storedQuantityScaled
        val value = if (qty > 0L) MoneyScaled(timesQuantity(qty, price.raw)) else MoneyScaled(0)
        val cost = holding.openingCostPerUnit?.takeIf { it.raw > 0L && qty > 0L }
            ?.let { MoneyScaled(timesQuantity(qty, it.raw)) } ?: MoneyScaled(0)
        return HoldingMetrics(qty, price, value, cost, MoneyScaled(Math.subtractExact(value.raw, cost.raw)))
    }

    fun isOpen(holding: FinancialHolding): Boolean =
        if (holding.category == "LIABILITY") holding.unitPrice.raw > 0L
        else holding.storedQuantityScaled > 0L

    fun upcoming(cards: List<HoldingCard>, goals: List<GoalReminder>, asOf: LocalDate): List<UpcomingItem> {
        val items = mutableListOf<UpcomingItem>()
        for (card in cards) {
            val holding = card.holding
            val name = displayName(holding.name)
            var couponShown = false
            val coupon = card.nextCoupon
            if (holding.category == "LIABILITY" && coupon != null && coupon >= asOf && !card.emiStopped) {
                items += UpcomingItem("e_${holding.holdingId.raw}", coupon, "EMI due · $name", "EMI", holding.holdingId.raw)
            } else if (holding.category != "LIABILITY" && coupon != null && coupon >= asOf) {
                couponShown = true
                val title = if (holding.category == "GOLD_SGB") "Gold bond interest · $name" else "Interest · $name"
                items += UpcomingItem("c_${holding.holdingId.raw}", coupon, title, "SGB_COUPON", holding.holdingId.raw)
            }
            if (!couponShown && holding.category != "LIABILITY") {
                val income = SavingsInterestEngine.nextMonthlyIncomeDate(
                    SchemeTerms(
                        holding, holding.unitPrice, monthlyIncome = card.monthlyIncome,
                        incomeDay = card.incomeDay, incomeStart = card.incomeStart, incomeEnd = card.incomeEnd,
                    ),
                    asOf,
                )
                if (income != null) {
                    items += UpcomingItem("i_${holding.holdingId.raw}", income, "Monthly income · $name", "INCOME", holding.holdingId.raw)
                }
            }
            val maturity = card.maturity
            if (maturity != null && maturity >= asOf) {
                val label = when (holding.category) {
                    "GOLD_SGB" -> "Gold bond matures"
                    "FIXED_INCOME" -> "Matures"
                    "LIABILITY" -> "Loan ends"
                    else -> "Lock-in ends"
                }
                items += UpcomingItem("m_${holding.holdingId.raw}", maturity, "$label · $name", "MATURITY", holding.holdingId.raw)
            }
        }
        for (goal in goals) {
            if (goal.target >= asOf) {
                items += UpcomingItem("g_${goal.id}", goal.target, "Goal · ${goal.name}", "GOAL", null)
            }
        }
        return items.sortedWith(compareBy({ it.date }, { it.title })).take(24)
    }

    fun displayName(name: String?): String {
        val trimmed = (name ?: "").trim()
        if (trimmed.isEmpty()) return ""
        val hash = trimmed.indexOf('#')
        val afterHash = if (hash >= 0 && hash < trimmed.length - 3) trimmed.substring(hash + 1).trim() else trimmed
        val match = Regex("""^([A-Z0-9]{1,8})\s*-\s+(.+)$""").matchEntire(afterHash) ?: return afterHash
        return match.groupValues[2].trim().ifEmpty { afterHash }
    }

    fun inferAmc(name: String?): String? {
        val shown = displayName(name).trim()
        if (shown.isEmpty()) return null
        val lower = shown.lowercase()
        return amcPrefixes.firstOrNull { lower.startsWith(it.lowercase()) }
    }

    fun accountTitle(custodian: String?, holdings: List<AccountLine>, accountType: String? = null): String {
        holdings.firstOrNull { !it.amc.isNullOrBlank() }?.amc?.trim()?.let { return it }
        val name = (custodian ?: "").trim()
        if (name.isNotEmpty() && !Regex("^(mutual fund folios?|mf folios?|cas)$", RegexOption.IGNORE_CASE).matches(name)) {
            return name
        }
        for (line in holdings) inferAmc(line.name)?.let { return it }
        if (accountType == "MF_FOLIO") return "Mutual fund folio"
        return name.ifEmpty { "Account" }
    }

    private fun usesUnits(holding: FinancialHolding, altHoldMode: String?): Boolean {
        if (holding.category == "EQUITY" || holding.category == "MUTUAL_FUND" || holding.category == "GOLD_SGB") {
            return true
        }
        val sub = holding.subCategory
        if (holding.category == "REAL_ESTATE") {
            if (!Regex("\\breit\\b|\\binvit\\b", RegexOption.IGNORE_CASE).containsMatchIn(sub)) return false
            return altHoldMode != "LUMPSUM"
        }
        if (holding.category == "SPECIALISED") {
            if (Regex("unlisted|esop", RegexOption.IGNORE_CASE).containsMatchIn(sub)) return true
            if (Regex("\\baif\\b", RegexOption.IGNORE_CASE).containsMatchIn(sub)) return altHoldMode == "DEMAT"
            return false
        }
        if (holding.category == "FIXED_INCOME") return Regex("bond|goi").containsMatchIn(sub.lowercase())
        return false
    }
}

data class FamilyBucket(val id: String, val title: String, val hint: String, val empty: Boolean = false)
data class HoldingMetrics(
    val quantityScaled: Long,
    val price: MoneyScaled,
    val value: MoneyScaled,
    val cost: MoneyScaled,
    val gain: MoneyScaled,
)
data class HoldingCard(
    val holding: FinancialHolding,
    val nextCoupon: LocalDate? = null,
    val maturity: LocalDate? = null,
    val emiStopped: Boolean = false,
    val monthlyIncome: MoneyScaled? = null,
    val incomeDay: Int? = null,
    val incomeStart: LocalDate? = null,
    val incomeEnd: LocalDate? = null,
)
data class GoalReminder(val id: String, val name: String, val target: LocalDate)
data class UpcomingItem(val id: String, val date: LocalDate, val title: String, val kind: String, val holdingId: String?)
data class AccountLine(val name: String, val amc: String? = null)
