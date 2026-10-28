package `in`.growurdhan.data.vault

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.FamilyAdminChange
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** First-run identity is one vault write: name, DOB, questions, and consent. */
@RunWith(AndroidJUnit4::class)
class FirstRunIdentityInstrumentedTest {
    @Test fun consentIsRequiredAndAcceptedProfileReopensWithoutAnswerPlaintext() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "first-run-$suffix")
        val alias = "in.growurdhan.test.firstrun.$suffix"
        val vault = SqlCipherVaultRepository(context, root, alias)
        try {
            vault.openOrCreate()
            vault.registerFamilyMember("Synthetic Owner", "Self")
            val refused = FamilyAdminChange.RegisterOwnerIdentity(
                "Synthetic", "Owner", "synthetic@example.test", "ABCDE1234F",
                "1990-01-01", "school", "synthetic answer1".toCharArray(),
                "city", "synthetic answer2".toCharArray(), registrationConsent = false,
            )
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking { vault.applyFamilyAdministration(refused) }
                }
            } finally { refused.wipeSecrets() }
            assertNull(vault.familyAdministration().ownerIdentity)
            val accepted = FamilyAdminChange.RegisterOwnerIdentity(
                "Synthetic", "Owner", "synthetic@example.test", "ABCDE1234F",
                "1990-01-01", "school", "synthetic answer1".toCharArray(),
                "city", "synthetic answer2".toCharArray(), registrationConsent = true,
            )
            try {
                vault.applyFamilyAdministration(accepted)
            } finally { accepted.wipeSecrets() }
            vault.close()
            val reopened = SqlCipherVaultRepository(context, root, alias)
            try {
                reopened.openOrCreate()
                val identity = reopened.familyAdministration().ownerIdentity!!
                assertEquals("Synthetic", identity.firstName)
                assertEquals("1990-01-01", identity.dateOfBirth)
                assertEquals("school", identity.firstQuestionId)
                assertEquals("city", identity.secondQuestionId)
            } finally { reopened.close() }
        } finally {
            vault.close()
            root.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        }
    }
}
