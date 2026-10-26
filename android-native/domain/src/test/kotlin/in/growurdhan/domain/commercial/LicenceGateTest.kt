package `in`.growurdhan.domain.commercial

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LicenceGateTest {
    private val now = Instant.parse("2026-10-26T12:00:00Z")

    @Test fun playInstallStartsAThirtyDayTrialAndNoOtherPrice() {
        val first = LicenceGate.resolve(emptyList(), null, installedFromPlay = true, debugTrialAllowed = false, now)
        assertTrue(first.persistTrialStart)
        assertEquals("trial", first.state.plan)
        assertEquals(30, first.state.daysLeft)
        assertFalse(first.state.subscribed)
        assertFalse(first.state.sideloadBlocked)
        val again = LicenceGate.resolve(emptyList(), first.state.trialStartedAt, true, false, now)
        assertFalse(again.persistTrialStart)
        val ended = LicenceGate.resolve(
            emptyList(),
            now.minusSeconds(40L * 86_400).toString(),
            installedFromPlay = true,
            debugTrialAllowed = false,
            now,
        )
        assertEquals("expired", ended.state.plan)
        assertFalse(ended.state.subscribed)
    }

    @Test fun sideloadStaysLockedUntilTheAnnualSubscriptionIsLive() {
        val locked = LicenceGate.resolve(emptyList(), null, installedFromPlay = false, debugTrialAllowed = false, now)
        assertTrue(locked.state.sideloadBlocked)
        assertEquals("expired", locked.state.plan)
        assertFalse(locked.persistTrialStart)
        val live = PlayReceipts.evidence("gud_annual", PlayReceipts.STATE_PURCHASED, true, "{}", true, now)
        val open = LicenceGate.resolve(listOf(live), null, installedFromPlay = false, debugTrialAllowed = false, now)
        assertTrue(open.state.subscribed)
        assertFalse(open.state.sideloadBlocked)
        assertEquals("annual", open.state.plan)
        assertFalse(open.persistTrialStart)
    }

    @Test fun oldSkuPendingAndStoredJsonDoNotUnlock() {
        val old = PlayReceipts.evidence("gud_lifetime", PlayReceipts.STATE_PURCHASED, true, "{}", true, now)
        val pending = PlayReceipts.evidence("gud_annual", PlayReceipts.STATE_PENDING, true, "{}", true, now)
        val stored = PlayReceipts.evidence("gud_annual", PlayReceipts.STATE_PURCHASED, true, "{}", false, now)
        val priced = PlayReceipts.evidence(
            "gud_annual",
            PlayReceipts.STATE_PURCHASED,
            true,
            """{"expiryTimeMillis":"1900000000000"}""",
            true,
            now,
        )
        assertFalse(LicenceGate.resolve(listOf(old, pending, stored), null, true, false, now).state.subscribed)
        val year = LicenceGate.resolve(listOf(priced), null, true, false, now)
        assertTrue(year.state.subscribed)
        assertEquals(Instant.ofEpochMilli(1_900_000_000_000).toString(), year.state.renewsAt)
        assertNull(LicenceGate.resolve(listOf(old), null, true, false, now).state.renewsAt)
    }
}
