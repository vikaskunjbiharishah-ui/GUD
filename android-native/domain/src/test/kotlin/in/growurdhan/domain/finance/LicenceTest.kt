package `in`.growurdhan.domain.finance

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** One paid plan: ₹99 a year. The 30-day trial is not a second product. */
class LicenceTest {
    private val now = Instant.parse("2026-09-19T00:00:00Z")
    private fun ago(days: Long) = Instant.ofEpochMilli(now.toEpochMilli() - days * 86_400_000L).toString()

    @Test fun oneAnnualPriceAndWhatItTurnsOn() {
        assertEquals(30, Licence.TRIAL_DAYS)
        assertEquals("gud_annual", Licence.SKU_ANNUAL)
        assertEquals(99, Licence.PRICE_ANNUAL)
        assertEquals("₹99", Licence.formatInr(99))
        assertEquals("₹12,345", Licence.formatInr(12345))

        val trial = Licence.derive(false, ago(2), now)
        assertEquals("trial", trial.plan)
        assertEquals(28, trial.daysLeft)
        assertEquals("expired", Licence.derive(false, ago(40), now).plan)
        assertEquals(0, Licence.derive(false, ago(40), now).daysLeft)
        assertEquals("annual", Licence.derive(true, null, now).plan)
        assertEquals(30, Licence.daysRemaining(now.toString(), now))
        assertEquals(1, Licence.daysRemaining(ago(30), now.minusMillis(1)))
        assertEquals(0, Licence.daysRemaining(ago(30), now))

        val expired = LicenceState("expired", subscribed = false, daysLeft = 0, sideloadBlocked = false)
        assertFalse(Licence.appUnlocked(expired))
        assertFalse(Licence.gmailSyncAllowed(expired))
        assertFalse(Licence.enhancementsEnabled(expired))
        assertTrue(Licence.canPurchaseAnnual(expired))

        val year = LicenceState("annual", subscribed = true, daysLeft = 0, sideloadBlocked = false, playAccountBound = true)
        assertTrue(Licence.appUnlocked(year))
        assertTrue(Licence.gmailSyncAllowed(year))
        assertTrue(Licence.enhancementsEnabled(year))
        assertFalse(Licence.canPurchaseAnnual(year))

        val sideload = LicenceState("trial", subscribed = false, daysLeft = 30, sideloadBlocked = true, trialStartedAt = now.toString())
        assertFalse(Licence.appUnlocked(sideload))
        assertFalse(Licence.canPurchaseAnnual(sideload))
        assertTrue(Licence.appUnlocked(year.copy(sideloadBlocked = true)))

        val trialState = LicenceState("trial", subscribed = false, daysLeft = 20, sideloadBlocked = false, trialStartedAt = now.toString(), source = "play")
        assertTrue(Licence.inTrial(trialState))
        assertTrue(Licence.gmailSyncAllowed(trialState))
        assertTrue(Licence.enhancementsEnabled(trialState))
        assertTrue(Licence.canPurchaseAnnual(trialState))
    }
}
