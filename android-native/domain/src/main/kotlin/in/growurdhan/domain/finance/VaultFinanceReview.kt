package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingProfileSnapshot
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * Reads only rates, dates and balances already stored on the holding.
 * A missing rate is not replaced with a published default. Nothing here
 * writes a ledger event; posting still needs a separate confirmation.
 */
object VaultFinanceReview {
    data class InterestLine(
        val ownerId: OwnerId,
        val name: String,
        val plan: InterestCatchUp,
    )

    data class EmiLine(
        val ownerId: OwnerId,
        val name: String,
        val plan: EmiCatchUp,
    )

    fun interest(
        holdings: List<FinancialHolding>,
        profiles: List<HoldingProfileSnapshot>,
        asOf: LocalDate,
        ownerId: OwnerId? = null,
    ): List<InterestLine> {
        val byId = profiles.associateBy { it.holdingId }
        return holdings.mapNotNull { holding ->
            if (ownerId != null && holding.ownerId != ownerId) return@mapNotNull null
            if (holding.category == "LIABILITY") return@mapNotNull null
            val profile = byId[holding.holdingId] ?: return@mapNotNull null
            val rateBps = jsonLong(profile.richMetadataJson, "interest_rate_bps")
            val couponBps = jsonLong(profile.richMetadataJson, "coupon_rate_bps")
            if (rateBps == null && couponBps == null) return@mapNotNull null
            val acquired = dateOrNull(profile.acquiredOn)
            val priced = holding.copy(
                acquiredOn = acquired ?: holding.acquiredOn,
                openingCostPerUnit = profile.verifiedCostPerUnit ?: holding.openingCostPerUnit,
                subCategory = profile.subCategory ?: holding.subCategory,
            )
            val terms = SchemeTerms(
                holding = priced,
                balance = MoneyScaled(timesQuantity(holding.storedQuantityScaled, holding.unitPrice.raw)),
                ratePercent = rateBps?.let { it / 100.0 },
                couponRatePercent = couponBps?.let { it / 100.0 },
                issuePrice = jsonLong(profile.richMetadataJson, "issue_price_scaled")?.let(::MoneyScaled),
                priceSource = profile.priceSource.orEmpty(),
                valuedOn = dateOrNull(profile.priceAsOf),
                maturityOn = dateOrNull(jsonString(profile.richMetadataJson, "maturity_date")),
                compounding = jsonString(profile.detailsJson, "compounding"),
            )
            val plan = SavingsInterestEngine.planInterest(terms, asOf) ?: return@mapNotNull null
            InterestLine(holding.ownerId, holding.name, plan)
        }
    }

    fun emi(
        holdings: List<FinancialHolding>,
        profiles: List<HoldingProfileSnapshot>,
        asOf: LocalDate,
        ownerId: OwnerId? = null,
    ): List<EmiLine> {
        val byId = profiles.associateBy { it.holdingId }
        return holdings.mapNotNull { holding ->
            if (ownerId != null && holding.ownerId != ownerId) return@mapNotNull null
            if (holding.category != "LIABILITY") return@mapNotNull null
            val profile = byId[holding.holdingId] ?: return@mapNotNull null
            val emi = jsonLong(profile.richMetadataJson, "monthly_emi_scaled") ?: return@mapNotNull null
            val remaining = jsonLong(profile.richMetadataJson, "remaining_emi_count") ?: return@mapNotNull null
            val next = dateOrNull(
                jsonString(profile.instrumentMetadataJson, "nextEmiDate")
                    ?: jsonString(profile.detailsJson, "nextEmiDate"),
            ) ?: return@mapNotNull null
            val outstanding = timesQuantity(holding.storedQuantityScaled, holding.unitPrice.raw)
            if (outstanding <= 0L || emi <= 0L || remaining <= 0L) return@mapNotNull null
            val loan = LoanEmi(
                id = holding.holdingId,
                ownerId = holding.ownerId,
                name = holding.name,
                category = holding.category,
                outstanding = MoneyScaled(outstanding),
                monthlyEmi = MoneyScaled(emi),
                remainingCount = remaining.toInt(),
                nextDue = next,
                status = engineStatus(jsonString(profile.richMetadataJson, "emi_status")),
            )
            val plan = EmiPlan.plan(loan, asOf) ?: return@mapNotNull null
            EmiLine(holding.ownerId, holding.name, plan)
        }
    }

    /** Stored words are "Paying", "Paused", "Stopped". The engine uses uppercase. */
    internal fun engineStatus(stored: String?): String? = when (stored?.trim()?.lowercase()) {
        null, "" -> null
        "paying" -> "PAYING"
        "paused" -> "PAUSED"
        "stopped" -> "STOPPED"
        else -> stored
    }

    private fun dateOrNull(text: String?): LocalDate? =
        text?.takeIf { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it) }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun jsonLong(json: String, key: String): Long? =
        Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    private fun jsonString(json: String, key: String): String? =
        Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
}
