package `in`.growurdhan.domain.vault

/** M25 source .kosha identity proof. Never persist answer or source password. */
class LegacyRestoreIdentity(
    val email: String,
    val password: CharArray,
    val pan: String,
    val dateOfBirth: String,
    val firstQuestionId: String,
    val firstAnswer: CharArray,
    val secondQuestionId: String,
    val secondAnswer: CharArray,
) {
    fun wipeSecrets() {
        password.fill('\u0000')
        firstAnswer.fill('\u0000')
        secondAnswer.fill('\u0000')
    }
}
