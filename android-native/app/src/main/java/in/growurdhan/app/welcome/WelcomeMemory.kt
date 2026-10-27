package `in`.growurdhan.app.welcome

import android.content.Context
import `in`.growurdhan.domain.finance.Onboarding
import `in`.growurdhan.domain.finance.Terms
import java.time.Instant

/** Device-only record that the walkthrough and the current terms were accepted. */
class WelcomeMemory(context: Context) {
    private val prefs = context.getSharedPreferences("gud.welcome", Context.MODE_PRIVATE)

    fun onboardingSeen(): Boolean = Onboarding.seen(prefs.getString(Onboarding.SEEN_KEY, null))

    fun markOnboardingSeen() {
        prefs.edit().putString(Onboarding.SEEN_KEY, "1").apply()
    }

    fun termsAccepted(): Boolean = Terms.hasAccepted(
        prefs.getString(Terms.ACCEPTED_VERSION_KEY, null),
        prefs.getString(Terms.ACCEPTED_ID_KEY, null),
    )

    fun acceptTerms(now: Instant = Instant.now()) {
        val accepted = Terms.accept(now)
        prefs.edit()
            .putString(Terms.ACCEPTED_VERSION_KEY, accepted.version)
            .putString(Terms.ACCEPTED_ID_KEY, accepted.textId)
            .putString(Terms.ACCEPTED_AT_KEY, accepted.acceptedAt)
            .apply()
    }
}
