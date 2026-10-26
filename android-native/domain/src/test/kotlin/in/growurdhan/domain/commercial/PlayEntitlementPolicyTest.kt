package `in`.growurdhan.domain.commercial

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayEntitlementPolicyTest {
    private val now = Instant.parse("2026-10-10T12:00:00Z")
    private fun evidence(
        id: String = PlayProduct.ANNUAL.productId,
        state: ObservedPurchaseState = ObservedPurchaseState.PURCHASED,
        verified: Boolean = true, acknowledged: Boolean = true,
        expiry: Instant? = now.plusSeconds(86_400),
    ) = PurchaseVerificationEvidence(id, state, verified, acknowledged, now, expiry)

    @Test fun pendingUnknownCancelledOrUnverifiedPurchasesNeverUnlock() {
        for (state in listOf(ObservedPurchaseState.PENDING,
            ObservedPurchaseState.UNKNOWN, ObservedPurchaseState.CANCELLED)) {
            assertFalse(PlayEntitlementPolicy.evaluate(listOf(
                evidence(state = state)), now).subscribed)
        }
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(verified = false)), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(acknowledged = false)), now).subscribed)
    }

    @Test fun aCurrentAnnualSubscriptionIsTheOnlyUnlock() {
        assertTrue(PlayEntitlementPolicy.evaluate(listOf(evidence()), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(expiry = null)), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(expiry = now.minusSeconds(1))), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(id = "gud_lifetime", expiry = null)), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(id = "gud_gmail")), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            evidence(id = "gud_support_annual")), now).subscribed)
    }

    @Test fun aLivePlayQueryWithoutAnExpiryStillMeansTheYearIsCurrent() {
        val live = evidence(expiry = null).copy(liveStoreQuery = true)
        assertTrue(PlayEntitlementPolicy.evaluate(listOf(live), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            live.copy(productId = "gud_lifetime")), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            live.copy(expiresAt = now.minusSeconds(1))), now).subscribed)
        assertFalse(PlayEntitlementPolicy.evaluate(listOf(
            live.copy(liveStoreQuery = false)), now).subscribed)
    }

    @Test fun oldOrFutureOrUnknownProductObservationsNeverUnlock() {
        val old = evidence().copy(checkedAt = now.minusSeconds(90_000))
        val future = evidence().copy(checkedAt = now.plusSeconds(1))
        val wrong = evidence(id = "not_a_growurdhan_play_sku")
        for (receipt in listOf(old, future, wrong)) {
            assertFalse(PlayEntitlementPolicy.evaluate(listOf(receipt), now).subscribed)
        }
    }
}
