package `in`.growurdhan.core.model

/**
 * Read-only projection of the encrypted M24 holding extension. Null cost is
 * genuinely unknown; do not turn an imported value into a fabricated BUY.
 */
data class HoldingProfileSnapshot(
    val holdingId: HoldingId,
    val ownerId: OwnerId,
    val name: String,
    val assetCategory: String,
    val subCategory: String? = null,
    val detailsJson: String = "{}",
    val notes: String? = null,
    val location: String? = null,
    val symbolOrFolio: String? = null,
    val isin: String? = null,
    val acquiredOn: String? = null,
    val priceAsOf: String? = null,
    val priceSource: String? = null,
    val verifiedCostPerUnit: MoneyScaled? = null,
    val archived: Boolean = false,
)

/** Only the fields explicitly supplied by the user may change. Ownership is immutable. */
data class HoldingProfileEdit(
    val holdingId: HoldingId,
    val ownerId: OwnerId,
    val name: String,
    val subCategory: String? = null,
    /** An object containing selected v9 HoldingDetails fields; unknown originals are retained. */
    val detailsJson: String = "{}",
    val notes: String? = null,
    val location: String? = null,
    val symbolOrFolio: String? = null,
    val isin: String? = null,
    val acquiredOn: String? = null,
    /** Optional explicitly dated custom price; does not imply a purchase. */
    val customPrice: MoneyScaled? = null,
    val priceAsOf: String? = null,
    val priceSource: String? = null,
    /** Null leaves unknown/existing acquisition-cost status unchanged. */
    val verifiedCostPerUnit: MoneyScaled? = null,
    /** Archive is a reversible visibility marker, NEVER deletion of a financial row. */
    val archived: Boolean = false,
)
