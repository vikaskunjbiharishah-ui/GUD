package `in`.growurdhan.domain.commercial

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayEntitlementPolicyTest {
    private val now = Instant.parse("2026-10-10T12:00:00Z")
    private fun evidence(
        id: String = PlayProduct.LIFETIME.productId,
        state: ObservedPurchaseState = ObservedPurchaseState.PURCHASED,
        verified: Boolean = true, acknowledged: Boolean = true,
        expiry: Instant? = null,
    ) = PurchaseVerificationEvidence(id, state, verified, acknowledged, now, expiry)

    @Test fun pendingUnknownCancelledOrUnverifiedPurchasesNeverUnlock() {
        for (state in listOf(ObservedPurchaseState.PENDING,
            ObservedPurchaseState.UNKNOWN, ObservedPurchaseState.CANCELLED)) {
            assertFalse(PlayEntitlementPolicy.evaluate(listOf(
                evidence(state = state)), now).lifetime)
        }
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(verified = false)), now).lifetime)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(acknowledged = false)), now).lifetime)
    }

    @Test fun verifiedAcknowledgedLifetimeCanUnlockButSupportIsSeparate() {
        assertEquals(EntitlementDecision(true, false),
            PlayEntitlementPolicy.evaluate(listOf(evidence()), now))
        assertEquals(EntitlementDecision(true, true),
            PlayEntitlementPolicy.evaluate(listOf(
                evidence(), evidence(
                    id = PlayProduct.ANNUAL_SUPPORT.productId,
                    expiry = now.plusSeconds(86_400),
                )), now))
    }

    @Test fun supportOnlyCannotReplaceLifetimeAndExpiredSubscriptionIsDenied() {
        assertEquals(EntitlementDecision(false, false),
            PlayEntitlementPolicy.evaluate(listOf(
                evidence(id = PlayProduct.ANNUAL_SUPPORT.productId,
                    expiry = now.plusSeconds(86_400))), now))
        assertEquals(EntitlementDecision(true, false),
            PlayEntitlementPolicy.evaluate(listOf(
                evidence(), evidence(id = PlayProduct.ANNUAL_SUPPORT.productId,
                    expiry = now.minusSeconds(1))), now))
    }

    @Test fun oldOrFutureOrUnknownProductObservationsNeverUnlock() {
        val old = evidence().copy(checkedAt = now.minusSeconds(90_000))
        val future = evidence().copy(checkedAt = now.plusSeconds(1))
        val wrong = evidence(id = "not_a_growurdhan_play_sku")
        for (receipt in listOf(old, future, wrong)) {
            assertFalse(PlayEntitlementPolicy.evaluate(listOf(receipt), now).lifetime)
        }
    }
}
