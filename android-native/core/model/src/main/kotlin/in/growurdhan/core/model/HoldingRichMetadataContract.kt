package `in`.growurdhan.core.model

/**
 * Original React v9 Holding scalar fields preserved in SQLCipher holding_profile.
 * This typed allowlist prevents dynamic SQL column selection from arbitrary JSON.
 * Financial money values are scaled by 1e4; rates are integer basis points.
 */
object HoldingRichMetadataContract {
    val moneyColumns = setOf(
        "benchmark_price_scaled", "issue_price_scaled", "monthly_emi_scaled",
        "fmv_31jan2018_scaled",
    )
    val rateColumns = setOf("coupon_rate_bps", "interest_rate_bps")
    val countColumns = setOf("remaining_emi_count")
    val dateColumns = setOf("next_coupon_date", "maturity_date")
    val textColumns = setOf("institution_name", "account_number_masked", "emi_status",
        "exposure_node", "l2_node", "wrapper", "tag")
    val jsonColumns = setOf("allocation_split_json", "tags_json")
    val editableColumns: Set<String> = moneyColumns + rateColumns + countColumns +
        dateColumns + textColumns + jsonColumns
}
