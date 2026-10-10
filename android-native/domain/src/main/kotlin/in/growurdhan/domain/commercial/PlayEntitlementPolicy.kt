package `in`.growurdhan.domain.commercial

import java.time.Instant
import java.time.temporal.ChronoUnit

/** Exact owner-approved Play product IDs; never derive entitlement from UI price text. */
enum class PlayProduct(val productId: String) {
    LIFETIME("gud_lifetime"),
    ANNUAL_SUPPORT("gud_support_annual"),
}

enum class ObservedPurchaseState { PENDING, PURCHASED, CANCELLED, UNKNOWN }

/**
 * This is a domain contract for a future trusted Play verification adapter.
 * A UI-controlled boolean or an unverified local preference MUST NOT be fed
 * into it. It deliberately does not call Google Play or grant live licences.
 */
data class PurchaseVerificationEvidence(
    val productId: String,
    val state: ObservedPurchaseState,
    val verifiedByTrustedProvider: Boolean,
    val acknowledged: Boolean,
    val checkedAt: Instant,
    val expiresAt: Instant? = null,
)

data class EntitlementDecision(
    val lifetime: Boolean,
    val supportActive: Boolean,
)

/** Fail-closed rules; UI/remote response alone never counts as a purchase. */
object PlayEntitlementPolicy {
    fun evaluate(
        evidence: List<PurchaseVerificationEvidence>,
        now: Instant,
    ): EntitlementDecision {
        val eligible = evidence.filter { receipt ->
            receipt.verifiedByTrustedProvider &&
                receipt.state == ObservedPurchaseState.PURCHASED &&
                receipt.acknowledged &&
                !receipt.checkedAt.isAfter(now) &&
                !receipt.checkedAt.isBefore(now.minus(1, ChronoUnit.DAYS)) &&
                (receipt.expiresAt == null || receipt.expiresAt.isAfter(now))
        }
        val lifetime = eligible.any {
            it.productId == PlayProduct.LIFETIME.productId && it.expiresAt == null
        }
        val support = lifetime && eligible.any {
            it.productId == PlayProduct.ANNUAL_SUPPORT.productId && it.expiresAt != null
        }
        return EntitlementDecision(lifetime, support)
    }
}
