package `in`.growurdhan.domain.commercial

import java.time.Instant

/**
 * Maps a Play Billing purchase into domain evidence.
 * Purchase state codes are Play's: 1 purchased, 2 pending. Anything else is unknown.
 * This object does not call Google Play.
 */
object PlayReceipts {
    const val STATE_PURCHASED = 1
    const val STATE_PENDING = 2

    private val expiryMillis = Regex(""""expiryTimeMillis"\s*:\s*"?(\d+)"""")

    fun evidence(
        productId: String,
        purchaseState: Int,
        acknowledged: Boolean,
        originalJson: String,
        fromBillingClient: Boolean,
        checkedAt: Instant,
    ): PurchaseVerificationEvidence {
        val expiry = expiryOf(originalJson)
        return PurchaseVerificationEvidence(
            productId = productId,
            state = when (purchaseState) {
                STATE_PURCHASED -> ObservedPurchaseState.PURCHASED
                STATE_PENDING -> ObservedPurchaseState.PENDING
                else -> ObservedPurchaseState.UNKNOWN
            },
            verifiedByTrustedProvider = fromBillingClient,
            acknowledged = acknowledged,
            checkedAt = checkedAt,
            expiresAt = expiry,
            liveStoreQuery = fromBillingClient && expiry == null,
        )
    }

    private fun expiryOf(json: String): Instant? {
        val millis = expiryMillis.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        if (millis <= 0L) return null
        return Instant.ofEpochMilli(millis)
    }
}
