package `in`.growurdhan.app.presentation

import `in`.growurdhan.app.billing.AnnualPlay
import `in`.growurdhan.domain.commercial.PlayReceipts
import `in`.growurdhan.domain.commercial.PurchaseVerificationEvidence
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenceViewModelTest {
    @Test fun debugTrialIsNotWrittenUntilPlayAnswers() {
        val clock = MemoryClock()
        val play = HoldPlay()
        val vm = LicenceViewModel(clock, Facts(fromPlay = false, debug = true), play)
        assertEquals("trial", vm.state.value.licence.plan)
        assertTrue(vm.state.value.checking)
        assertNull(clock.saved)
        play.release()
        assertFalse(clock.saved.isNullOrBlank())
        assertFalse(vm.state.value.checking)
        assertTrue(vm.state.value.licence.daysLeft in 29..30)
    }

    @Test fun aLiveAnnualSubscriptionDoesNotInventATrialOrAcceptAnOldSku() {
        val clock = MemoryClock()
        val play = HoldPlay(hold = false)
        val now = Instant.now()
        play.evidence = listOf(
            PlayReceipts.evidence("gud_lifetime", PlayReceipts.STATE_PURCHASED, true, "{}", true, now),
            PlayReceipts.evidence("gud_annual", PlayReceipts.STATE_PURCHASED, true, "{}", true, now),
        )
        val vm = LicenceViewModel(clock, Facts(fromPlay = true, debug = false), play)
        assertTrue(vm.state.value.licence.subscribed)
        assertEquals("annual", vm.state.value.licence.plan)
        assertNull(clock.saved)
        vm.purchase()
        assertEquals("The ₹99 yearly plan is already on this account.", vm.state.value.error)
    }

    @Test fun sideloadCannotBuyAndATrialCopySurfacesThePlayError() {
        val play = HoldPlay(hold = false)
        val locked = LicenceViewModel(MemoryClock(), Facts(fromPlay = false, debug = false), play)
        assertTrue(locked.state.value.licence.sideloadBlocked)
        locked.purchase()
        assertEquals("This copy cannot buy the ₹99 yearly plan.", locked.state.value.error)

        play.buyError = "Install GrowUrDhan from Google Play to buy a licence."
        val trial = LicenceViewModel(MemoryClock(), Facts(fromPlay = false, debug = true), play)
        trial.purchase()
        assertEquals("Install GrowUrDhan from Google Play to buy a licence.", trial.state.value.error)
        assertFalse(trial.state.value.busy)
    }

    @Test fun restoreWithNoLivePurchaseStaysUnsubscribed() {
        val play = HoldPlay(hold = false)
        val now = Instant.now()
        play.evidence = listOf(
            PlayReceipts.evidence("gud_annual", PlayReceipts.STATE_PURCHASED, true, "{}", false, now),
        )
        val vm = LicenceViewModel(MemoryClock(), Facts(fromPlay = true, debug = false), play)
        assertFalse(vm.state.value.licence.subscribed)
        vm.refresh()
        assertFalse(vm.state.value.licence.subscribed)
        play.evidence = emptyList()
        vm.refresh()
        assertFalse(vm.state.value.licence.subscribed)
    }

    private class MemoryClock : TrialMemory {
        var saved: String? = null
        override fun read(): String? = saved
        override fun write(iso: String) { saved = iso }
    }

    private class Facts(override val fromPlay: Boolean, val debug: Boolean) : InstallFacts {
        override val debugTrialAllowed: Boolean get() = debug
    }

    private class HoldPlay(var hold: Boolean = true) : AnnualPlay {
        var evidence: List<PurchaseVerificationEvidence> = emptyList()
        var buyError: String? = null
        private var pending: ((List<PurchaseVerificationEvidence>) -> Unit)? = null
        override fun load(onResult: (List<PurchaseVerificationEvidence>) -> Unit) {
            if (hold) pending = onResult else onResult(evidence)
        }
        fun release() {
            hold = false
            pending?.invoke(evidence)
            pending = null
        }
        override fun purchase(onFinished: (String?) -> Unit) { onFinished(buyError) }
    }
}
