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
    /** Canonical original-v9 taxonomy for all NEW native registrations. */
    val canonicalCategories = listOf(
        "EQUITY", "MUTUAL_FUND", "GOLD_SGB", "FIXED_INCOME",
        "REAL_ESTATE", "SPECIALISED", "CASH_BANK", "LIABILITY",
    )
    /** Exact source labels: GUD-Android-Gmail src/types.ts ASSET_SUBCATEGORIES. */
    val subCategories: Map<String, List<String>> = mapOf(
        "EQUITY" to listOf("Large cap", "Mid cap", "Small cap",
            "Multi & flexi cap", "Sector & thematic", "Other"),
        "MUTUAL_FUND" to listOf("Equity fund", "Debt fund", "Hybrid fund",
            "Index / ETF", "ELSS (tax saving)", "Other"),
        "GOLD_SGB" to listOf("Sovereign Gold Bond", "Physical gold (24K)",
            "Digital gold", "Other"),
        "FIXED_INCOME" to listOf("PPF", "EPF", "NPS", "Sukanya Samriddhi (SSY)",
            "Bank FD", "SCSS", "NSC", "KVP", "POMIS", "Post Office RD",
            "RBI Floating Rate Bond", "Bond / GOI", "Other"),
        "REAL_ESTATE" to listOf("Residential", "Commercial", "Land",
            "REIT", "InvIT", "Other"),
        "SPECIALISED" to listOf("PMS", "AIF", "Unlisted equity", "ESOP", "Other"),
        "CASH_BANK" to listOf("Savings account", "Current account",
            "Liquid / overnight", "Other"),
        "LIABILITY" to listOf("Home loan", "Car loan", "Education loan",
            "Personal loan", "Other"),
    )
    fun requireCanonicalSubCategory(category: String, subCategory: String) {
        require(category in canonicalCategories &&
            subCategory in (subCategories[category] ?: emptyList())) {
            "Choose a valid category and its matching subcategory."
        }
    }
    /** Legacy aliases remain importable/readable; new forms use canonicalCategories. */
    val supportedCategories = canonicalCategories + listOf(
        "NPS", "PPF", "EPF", "FIXED_DEPOSIT", "BOND", "GOLD", "CASH", "OTHER",
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
