package `in`.growurdhan.domain.finance

import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

/**
 * Commercial licence from src/lib/license.ts.
 * One Play subscription: ₹99 a year. The trial is 30 days.
 * There is no lifetime licence and no separate Gmail product.
 * Charging through Play stays on the device.
 */
object Licence {
    const val TRIAL_DAYS = 30
    const val SKU_ANNUAL = "gud_annual"
    const val PRICE_ANNUAL = 99
    private const val DAY_MS = 86_400_000L

    fun daysRemaining(trialStartedAt: String, now: Instant): Int {
        val start = runCatching { Instant.parse(trialStartedAt) }.getOrNull() ?: return 0
        val end = start.toEpochMilli() + TRIAL_DAYS * DAY_MS
        val left = (end - now.toEpochMilli()).toDouble() / DAY_MS.toDouble()
        return max(0, ceil(left).toInt())
    }

    fun derive(subscribed: Boolean, trialStartedAt: String?, now: Instant): PlanView {
        if (subscribed) return PlanView("annual", 0, null)
        if (trialStartedAt != null) {
            val left = daysRemaining(trialStartedAt, now)
            val ends = endsAt(trialStartedAt)
            if (left > 0) return PlanView("trial", left, ends)
            return PlanView("expired", 0, ends)
        }
        return PlanView("expired", 0, null)
    }

    fun formatInr(amount: Int): String {
        val digits = kotlin.math.abs(amount).toString()
        val grouped = if (digits.length <= 3) digits else {
            val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${digits.takeLast(3)}"
        }
        return "${if (amount < 0) "-" else ""}₹$grouped"
    }

    fun inTrial(state: LicenceState): Boolean = state.plan == "trial" && state.daysLeft > 0

    fun enhancementsEnabled(state: LicenceState): Boolean = inTrial(state) || state.subscribed

    fun appUnlocked(state: LicenceState): Boolean {
        if (state.sideloadBlocked && !state.subscribed) return false
        return state.subscribed || state.plan == "trial"
    }

    fun canPurchaseAnnual(state: LicenceState): Boolean =
        !state.subscribed && !state.sideloadBlocked

    fun gmailSyncAllowed(state: LicenceState): Boolean {
        if (state.sideloadBlocked && !state.subscribed) return false
        if (inTrial(state)) return true
        return state.subscribed
    }

    private fun endsAt(startIso: String): String? {
        val start = runCatching { Instant.parse(startIso) }.getOrNull() ?: return startIso
        return Instant.ofEpochMilli(start.toEpochMilli() + TRIAL_DAYS * DAY_MS).toString()
    }
}

data class PlanView(val plan: String, val daysLeft: Int, val trialEndsAt: String?)

data class LicenceState(
    val plan: String,
    val subscribed: Boolean,
    val daysLeft: Int,
    val sideloadBlocked: Boolean,
    val trialStartedAt: String? = null,
    val trialEndsAt: String? = null,
    val renewsAt: String? = null,
    val playAccountBound: Boolean = false,
    val source: String = "play",
)
