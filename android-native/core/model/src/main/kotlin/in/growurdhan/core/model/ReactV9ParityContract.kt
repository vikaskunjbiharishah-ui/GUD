package `in`.growurdhan.core.model

/**
 * Source-frozen catalogue from GUD-Android-Gmail src/types.ts (schema v9).
 *
 * This is a preservation contract, NOT a statement that UI or legacy restore
 * is implemented. M24 stores all HoldingDetails attributes as opaque JSON
 * in holding_profile.details_json; M25 must validate and map them explicitly.
 * Keep original unknown/acquisition values NULL, never synthesize cost.
 */
enum class ReactV9AssetCategory {
    EQUITY, MUTUAL_FUND, GOLD_SGB, FIXED_INCOME, REAL_ESTATE, SPECIALISED, CASH_BANK, LIABILITY
}
enum class ReactV9LedgerEventType {
    OPENING_BALANCE, SNAPSHOT_ADJUSTMENT, BUY, SELL, BONUS, SPLIT, DIVIDEND, INTEREST, VALUATION_UPDATE, REPAYMENT, CORRECTION
}
object ReactV9HoldingDetailsContract {
    const val SOURCE_SCHEMA_VERSION = 9
    val fields: Set<String> = setOf(
        "goldHoldMode",
        "goldPurity",
        "goldHallmark",
        "earlyExitDate",
        "annualContributionMinor",
        "contributionOn",
        "compounding",
        "tdsApplicable",
        "npsTier",
        "npsCra",
        "npsPfm",
        "faceValueMinor",
        "areaSqFt",
        "ownership",
        "ownershipPercent",
        "aifCategory",
        "commitmentMinor",
        "capitalCalledMinor",
        "ifsc",
        "strategyName",
        "custodian",
        "originalTenorMonths",
        "rateType",
        "linkedHoldingId",
        "mfPlan",
        "mfOption",
        "mfHeldAs",
        "depository",
        "dpId",
        "clientId",
        "boId",
        "altHoldMode",
        "monthlyIncomeMinor",
        "incomeDay",
        "incomeStart",
        "incomeEnd",
    )
    /** Nothing in M24 silently drops a field from the reference schema. */
    fun requireAllKeysPreservable(keys: Set<String>) {
        require(keys.all { it in fields }) {
            "Unknown original HoldingDetails attribute: preserve in legacy_field_extension for review."
        }
    }
}
/** Null is unknown cost; zero is a verified actual zero-cost position. */
data class VerifiedAcquisitionCost(val scaledCost: MoneyScaled?) {
    val known: Boolean get() = scaledCost != null
}
