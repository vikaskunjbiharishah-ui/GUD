package `in`.growurdhan.domain.commercial

import `in`.growurdhan.domain.finance.Licence
import `in`.growurdhan.domain.finance.LicenceState
import java.time.Instant

data class LicenceResolution(
    val state: LicenceState,
    val persistTrialStart: Boolean,
)

/**
 * Trial clock plus Play evidence. A sideload does not start a trial.
 * Debug builds may pass debugTrialAllowed so a studio install can use the
 * same 30 days. That flag does not invent a subscription.
 */
object LicenceGate {
    fun resolve(
        evidence: List<PurchaseVerificationEvidence>,
        trialStartedAt: String?,
        installedFromPlay: Boolean,
        debugTrialAllowed: Boolean,
        now: Instant,
    ): LicenceResolution {
        val subscribed = PlayEntitlementPolicy.evaluate(evidence, now).subscribed
        val trustedInstall = installedFromPlay || debugTrialAllowed
        val sideloadBlocked = !trustedInstall && !subscribed
        val persistTrialStart = trustedInstall && !subscribed && trialStartedAt.isNullOrBlank()
        val clock = when {
            sideloadBlocked -> null
            persistTrialStart -> now.toString()
            else -> trialStartedAt
        }
        val derived = Licence.derive(subscribed, clock, now)
        val renewsAt = if (!subscribed) null else evidence
            .filter { it.productId == PlayProduct.ANNUAL.productId && it.expiresAt != null }
            .maxByOrNull { it.expiresAt!! }
            ?.expiresAt
            ?.toString()
        return LicenceResolution(
            state = LicenceState(
                plan = derived.plan,
                subscribed = subscribed,
                daysLeft = derived.daysLeft,
                sideloadBlocked = sideloadBlocked,
                trialStartedAt = clock,
                trialEndsAt = derived.trialEndsAt,
                renewsAt = renewsAt,
                playAccountBound = subscribed,
                source = "play",
            ),
            persistTrialStart = persistTrialStart,
        )
    }
}
