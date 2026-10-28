package `in`.growurdhan.domain.vault

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirstRunRegistrationTest {
    private val today = LocalDate.parse("2026-10-28")
    private fun draft(consent: Boolean = true, dob: String = "1990-01-01", q2: String = "city") =
        FirstRunRegistration.Draft(
            "Synthetic", "Owner", "synthetic@example.test", "ABCDE1234F", dob,
            "school", "first answer", q2, "second answer", consent,
        )

    @Test fun completeDraftHasNoIssues() {
        assertEquals(emptyList<String>(), FirstRunRegistration.issues(draft(), today))
    }

    @Test fun consentFutureDobAndSameQuestionAreRejected() {
        val issues = FirstRunRegistration.issues(
            draft(consent = false, dob = "2026-10-29", q2 = "school"), today,
        )
        assertTrue(issues.any { it.contains("Consent") })
        assertTrue(issues.any { it.contains("future") })
        assertTrue(issues.any { it.contains("two different") })
    }

    @Test fun issuesNeverEchoTheSecurityAnswer() {
        val secret = "synthetic-secret-answer"
        val issues = FirstRunRegistration.issues(
            draft().copy(firstAnswer = "x", secondAnswer = secret, consent = false), today,
        )
        assertTrue(issues.none { it.contains(secret) })
    }
}
