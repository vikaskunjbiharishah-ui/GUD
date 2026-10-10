package `in`.growurdhan.domain.vault

import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.QuantityScaled

/**
 * Opt-in, manually tracked assets. No financial position or acquisition cost is
 * guessed at enrolment. NPS/EPF/PPF here are personal ledger categories, NOT
 * verified government-statement or PRAN integrations.
 */
object ManualAssetOnboarding {
    val supportedCategories = listOf(
        "EQUITY", "MUTUAL_FUND", "NPS", "PPF", "EPF",
        "FIXED_DEPOSIT", "BOND", "GOLD", "REAL_ESTATE", "CASH", "OTHER",
    )
    const val MAX_MANUAL_HOLDINGS_PER_OWNER = 2_000

    fun create(
        ownerId: OwnerId, name: String, category: String, id: HoldingId,
        existing: List<Holding>,
    ): Holding {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        require(category in supportedCategories) { "Select an approved asset category." }
        require(clean.length in 1..200 && clean.none { it.isISOControl() }) {
            "Asset nickname must be 1–200 printable characters."
        }
        require(!Regex("^[A-Z]{5}[0-9]{4}[A-Z]$", RegexOption.IGNORE_CASE).matches(clean)) {
            "A PAN cannot be used as the holding name."
        }
        require(existing.count { it.ownerId == ownerId } < MAX_MANUAL_HOLDINGS_PER_OWNER)
        require(existing.none {
            it.ownerId == ownerId && it.assetCategory == category &&
                it.name.equals(clean, ignoreCase = true)
        }) { "This family member already tracks an asset with that category and name." }
        return Holding(
            id, ownerId, null, clean, category,
            QuantityScaled(0), MoneyScaled(0),
        )
    }
}
