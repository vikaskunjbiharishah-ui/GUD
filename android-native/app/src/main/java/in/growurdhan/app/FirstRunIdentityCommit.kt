package `in`.growurdhan.app

import `in`.growurdhan.core.model.FamilyAdminChange
import `in`.growurdhan.domain.vault.VaultAuthorization
import `in`.growurdhan.domain.vault.VaultRepository

/**
 * Saves the first-run profile only after [VaultAuthorization.requireRecentApproval].
 * Name, date of birth, email and answers are not written to logs.
 */
object FirstRunIdentityCommit {
    const val NOT_STORED =
        "The password is saved, but the name and security questions were not stored. Finish them under Family after you open the vault."

    suspend fun saveIfAbsent(
        authorization: VaultAuthorization,
        repository: VaultRepository,
        identity: PendingFirstRunIdentity,
        vaultReady: Boolean,
    ): String? {
        authorization.requireRecentApproval()
        if (!vaultReady) return NOT_STORED
        val snap = repository.familyAdministration()
        if (snap.ownerIdentity == null) {
            if (snap.family.none { it.primary }) {
                val display = (identity.firstName.trim() + " " + identity.lastName.trim())
                    .trim()
                    .take(100)
                repository.registerFamilyMember(display, "Self")
            }
            repository.applyFamilyAdministration(identity.toChange())
        }
        return null
    }
}

class PendingFirstRunIdentity(
    val firstName: String,
    val lastName: String,
    val email: String,
    val pan: String,
    val dateOfBirth: String,
    val firstQuestionId: String,
    val firstAnswer: CharArray,
    val secondQuestionId: String,
    val secondAnswer: CharArray,
) {
    fun wipe() {
        firstAnswer.fill('\u0000')
        secondAnswer.fill('\u0000')
    }

    fun toChange() = FamilyAdminChange.RegisterOwnerIdentity(
        firstName, lastName, email, pan, dateOfBirth,
        firstQuestionId, firstAnswer, secondQuestionId, secondAnswer,
        registrationConsent = true,
    )
}
