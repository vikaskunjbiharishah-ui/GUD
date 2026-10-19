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

    /**
     * Detail keys the original add-holding form shows without "show all".
     * Equity depository IDs and non-liquid bank IFSC match AddHoldingModal.tsx.
     */
    fun preferredDetailKeys(category: String, subCategory: String): Set<String> = when (category) {
        "MUTUAL_FUND" -> setOf("mfPlan", "mfOption", "mfHeldAs", "dpId", "clientId", "boId")
        "EQUITY" -> setOf("depository", "dpId", "clientId", "boId")
        "GOLD_SGB" -> setOf("goldHoldMode", "goldPurity", "goldHallmark", "earlyExitDate", "faceValueMinor")
        "FIXED_INCOME", "PPF", "EPF", "FIXED_DEPOSIT", "BOND" ->
            if (subCategory == "NPS")
                setOf("npsTier", "npsCra", "npsPfm", "annualContributionMinor")
            else setOf("annualContributionMinor", "compounding", "tdsApplicable",
                "rateType", "originalTenorMonths", "contributionOn")
        "REAL_ESTATE" -> setOf("areaSqFt", "ownership", "ownershipPercent", "linkedHoldingId",
            "altHoldMode", "monthlyIncomeMinor", "incomeDay", "incomeStart", "incomeEnd")
        "SPECIALISED", "NPS" -> setOf("npsTier", "npsCra", "npsPfm",
            "aifCategory", "commitmentMinor", "capitalCalledMinor", "strategyName",
            "custodian", "altHoldMode", "ifsc")
        "CASH_BANK" -> if (subCategory.contains("liquid", ignoreCase = true)) emptySet()
            else setOf("ifsc")
        "LIABILITY" -> setOf("linkedHoldingId", "rateType", "compounding", "originalTenorMonths")
        else -> emptySet()
    }

    /** Same identifier cleanup as AddHoldingModal submit / IFSC typing. Does not invent values. */
    fun normalizeStoredDetail(key: String, value: String): String = when (key) {
        "dpId", "ifsc" -> value.replace(Regex("\\s"), "").uppercase()
        "clientId", "boId" -> value.replace(Regex("\\s"), "")
        else -> value
    }

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
