package `in`.growurdhan.domain.finance

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Play review prompt from src/lib/feedback.ts.
 * Seven days after first open, then a two-day snooze. Six dismissals stop it.
 * GrowUrDhan does not collect the rating. Play does.
 */
object FeedbackPrompt {
    const val FIRST_DELAY_DAYS = 7
    const val SNOOZE_DAYS = 2
    const val MAX_PROMPTS = 6
    const val PLAY_PACKAGE_ID = "in.growurdhan.app"
    const val PLAY_LISTING_URL = "https://play.google.com/store/apps/details?id=$PLAY_PACKAGE_ID"
    private const val DAY_MS = 86_400_000.0
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC)
    private var sessionClaimed = false

    fun seedFirstSeen(state: FeedbackState, trialStartedAt: String?, now: Instant): FeedbackState {
        if (state.firstSeenAt != null) return state
        return state.copy(firstSeenAt = trialStartedAt ?: stamp.format(now))
    }

    fun shouldShow(state: FeedbackState, now: Instant): Boolean {
        if (state.completedAt != null) return false
        if (state.promptCount >= MAX_PROMPTS) return false
        val first = state.firstSeenAt ?: return false
        if (daysBetween(first, now) < FIRST_DELAY_DAYS) return false
        val snooze = state.snoozeUntil?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (snooze != null && snooze.toEpochMilli() > now.toEpochMilli()) return false
        return true
    }

    fun snooze(state: FeedbackState, now: Instant): FeedbackState = state.copy(
        promptCount = state.promptCount + 1,
        snoozeUntil = stamp.format(Instant.ofEpochMilli(now.toEpochMilli() + (SNOOZE_DAYS * 86_400_000L))),
    )

    fun complete(state: FeedbackState, now: Instant): FeedbackState =
        state.copy(completedAt = stamp.format(now), snoozeUntil = null)

    fun claim(state: FeedbackState, now: Instant): Boolean {
        if (sessionClaimed) return false
        if (!shouldShow(state, now)) return false
        sessionClaimed = true
        return true
    }

    fun resetSession() {
        sessionClaimed = false
    }

    private fun daysBetween(fromIso: String, now: Instant): Double {
        val start = runCatching { Instant.parse(fromIso) }.getOrNull() ?: return 0.0
        return (now.toEpochMilli() - start.toEpochMilli()) / DAY_MS
    }
}

data class FeedbackState(
    val firstSeenAt: String? = null,
    val snoozeUntil: String? = null,
    val completedAt: String? = null,
    val promptCount: Int = 0,
)
