package `in`.growurdhan.domain.vault

/**
 * Subcategory-specific funded-intake rules distilled from frozen
 * src/components/modals/AddHoldingModal.tsx, src/types.ts.
 *
 * The opt-in BASIC onboarding path deliberately creates a zero-unit/unknown-cost
 * holding and does not require these financed-position fields. The ADVANCED path
 * must not imply a complete product-specific form when mandatory data is missing.
 * Monetary balances and units are still separate reviewed ledger operations.
 */
object OriginalHoldingFormPolicy {
    data class RequiredField(val key: String, val label: String)

    fun required(category: String, subCategory: String, facts: Map<String, String?>): List<RequiredField> {
        ManualAssetOnboarding.requireCanonicalSubCategory(category, subCategory)
        fun field(key: String, label: String) = RequiredField(key, label)
        val needed = mutableListOf<RequiredField>()
        when {
            category == "LIABILITY" -> {
                needed += field("monthly_emi_scaled", "Monthly EMI (INR)")
                needed += field("remaining_emi_count", "Remaining EMI months")
                needed += field("nextEmiDate", "Next EMI due date")
            }
            category == "SPECIALISED" && subCategory == "AIF" -> {
                needed += field("altHoldMode", "AIF holding mode")
                if (facts["altHoldMode"] == "LUMPSUM") {
                    needed += field("aifCategory", "AIF category")
                    needed += field("commitmentMinor", "Committed capital (scaled INR)")
                }
            }
            category == "REAL_ESTATE" && subCategory in setOf("REIT", "InvIT") -> {
                needed += field("altHoldMode", "REIT/InvIT holding mode")
            }
            category == "FIXED_INCOME" && subCategory == "NPS" ->
                needed += field("npsTier", "NPS tier")
            category == "FIXED_INCOME" && subCategory == "Bank FD" ->
                needed += field("compounding", "Deposit compounding/payout")
            category == "GOLD_SGB" && subCategory == "Physical gold (24K)" ->
                needed += field("goldPurity", "Gold purity")
            category == "MUTUAL_FUND" ->
                needed += field("mfHeldAs", "Folio or demat holding")
        }
        return needed
    }

    fun missing(category: String, subCategory: String, facts: Map<String, String?>): List<RequiredField> =
        required(category, subCategory, facts).filter { facts[it.key].isNullOrBlank() }

    /** Units and money must be recorded by explicit ledger review, not inferred here. */
    fun usesSingleMonetaryBalance(category: String, subCategory: String, facts: Map<String, String?>): Boolean =
        category in setOf("CASH_BANK", "LIABILITY") ||
            (category == "FIXED_INCOME" && subCategory in setOf(
                "PPF", "EPF", "NPS", "Sukanya Samriddhi (SSY)", "Bank FD",
                "SCSS", "NSC", "KVP", "POMIS", "Post Office RD",
                "RBI Floating Rate Bond", "Other",
            )) ||
            (category == "SPECIALISED" && subCategory == "AIF" &&
                facts["altHoldMode"] == "LUMPSUM")
}
