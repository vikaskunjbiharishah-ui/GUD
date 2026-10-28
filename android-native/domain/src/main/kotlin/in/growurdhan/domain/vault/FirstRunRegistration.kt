package `in`.growurdhan.domain.vault

import java.time.LocalDate

/**
 * First-run account profile. Validation only: answers are not stored, logged,
 * or returned in the issue list. The encrypted vault remains the writer.
 */
object FirstRunRegistration {
    val questionIds = listOf("maiden", "school", "pet", "city", "teacher", "vehicle")
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val panPattern = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

    data class Draft(
        val firstName: String,
        val lastName: String,
        val email: String,
        val pan: String,
        val dateOfBirth: String,
        val firstQuestionId: String,
        val firstAnswer: String,
        val secondQuestionId: String,
        val secondAnswer: String,
        val consent: Boolean,
    )

    fun issues(draft: Draft, today: LocalDate = LocalDate.now()): List<String> {
        val out = ArrayList<String>(8)
        if (draft.firstName.trim().length !in 1..100) out += "Enter a first name."
        if (draft.lastName.trim().length !in 1..100) out += "Enter a last name."
        val mail = draft.email.trim().lowercase()
        if (mail.length !in 3..254 || !emailPattern.matches(mail)) out += "Enter the owner email."
        if (!panPattern.matches(draft.pan.trim().uppercase())) out += "PAN must be 10 characters."
        val dob = draft.dateOfBirth.trim()
        val parsed = try {
            val day = LocalDate.parse(dob)
            if (day.toString() == dob) day else null
        } catch (_: Exception) {
            null
        }
        if (parsed == null) out += "Date of birth must be YYYY-MM-DD."
        else if (parsed.isAfter(today)) out += "Date of birth cannot be in the future."
        if (draft.firstQuestionId !in questionIds ||
            draft.secondQuestionId !in questionIds ||
            draft.firstQuestionId == draft.secondQuestionId
        ) {
            out += "Choose two different security questions."
        }
        if (normalize(draft.firstAnswer).length !in 3..256) out += "First security answer is too short."
        if (normalize(draft.secondAnswer).length !in 3..256) out += "Second security answer is too short."
        if (!draft.consent) out += "Consent is required before this account is saved on the phone."
        return out
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
