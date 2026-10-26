package `in`.growurdhan.domain.commercial

import java.time.Instant
import java.time.temporal.ChronoUnit

/** The only owner-approved Play product. A UI price is not a purchase. */
enum class PlayProduct(val productId: String) {
    ANNUAL("gud_annual"),
}

enum class ObservedPurchaseState { PENDING, PURCHASED, CANCELLED, UNKNOWN }

/**
 * A purchase observation. `verifiedByTrustedProvider` may be set only by the
 * Play Billing client in this process. A preference, button, or old SKU is not
 * a subscription. `liveStoreQuery` means BillingClient just returned this row.
 * The device library often omits expiry; a live row is current, a stored row
 * without an expiry is not.
 */
data class PurchaseVerificationEvidence(
    val productId: String,
    val state: ObservedPurchaseState,
    val verifiedByTrustedProvider: Boolean,
    val acknowledged: Boolean,
    val checkedAt: Instant,
    val expiresAt: Instant? = null,
    val liveStoreQuery: Boolean = false,
)

data class EntitlementDecision(val subscribed: Boolean)

/** Fail-closed. Only a current, verified gud_annual subscription counts. */
object PlayEntitlementPolicy {
    fun evaluate(
        evidence: List<PurchaseVerificationEvidence>,
        now: Instant,
    ): EntitlementDecision {
        val subscribed = evidence.any { receipt ->
            val periodOpen = when {
                receipt.expiresAt != null -> receipt.expiresAt.isAfter(now)
                receipt.liveStoreQuery -> true
                else -> false
            }
            receipt.productId == PlayProduct.ANNUAL.productId &&
                receipt.verifiedByTrustedProvider &&
                receipt.state == ObservedPurchaseState.PURCHASED &&
                receipt.acknowledged &&
                !receipt.checkedAt.isAfter(now) &&
                !receipt.checkedAt.isBefore(now.minus(1, ChronoUnit.DAYS)) &&
                periodOpen
        }
        return EntitlementDecision(subscribed)
    }
}
