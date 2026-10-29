package `in`.growurdhan.app

import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.app.presentation.VaultSessionGate
import `in`.growurdhan.data.vault.SqlCipherVaultRepository
import `in`.growurdhan.domain.vault.UnitTestPasswordAdmission
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The device-credential handoff is the only writer. Secrets must not reach logcat. */
@RunWith(AndroidJUnit4::class)
class FirstRunIdentityHandoffInstrumentedTest {
    @Test fun identityIsSavedOnlyAfterDeviceCredentialAndSecretsStayOutOfLogs() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "handoff-$suffix")
        val alias = "in.growurdhan.test.handoff.$suffix"
        val email = "synthetic.owner@example.test"
        val dob = "1990-01-15"
        val firstAnswer = "synthetic-answer-one"
        val secondAnswer = "synthetic-answer-two"
        val vault = SqlCipherVaultRepository(context, root, alias)
        val identity = PendingFirstRunIdentity(
            "Synthetic", "Owner", email, "ABCDE1234F", dob,
            "school", firstAnswer.toCharArray(), "city", secondAnswer.toCharArray(),
        )
        shell("logcat -c")
        try {
            vault.openOrCreate()
            val gate = VaultSessionGate(UnitTestPasswordAdmission)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    FirstRunIdentityCommit.saveIfAbsent(gate, vault, identity, vaultReady = true)
                }
            }
            assertNull(vault.familyAdministration().ownerIdentity)
            gate.grantDeviceCredential()
            assertNull(FirstRunIdentityCommit.saveIfAbsent(gate, vault, identity, vaultReady = true))
            vault.close()
            val reopened = SqlCipherVaultRepository(context, root, alias)
            try {
                reopened.openOrCreate()
                val saved = reopened.familyAdministration().ownerIdentity!!
                assertEquals("Synthetic", saved.firstName)
                assertEquals(dob, saved.dateOfBirth)
                assertEquals(email, saved.email)
                assertEquals("school", saved.firstQuestionId)
                assertFalse(saved.toString().contains(firstAnswer))
                assertFalse(saved.toString().contains(secondAnswer))
            } finally { reopened.close() }
            Log.i("GudFirstRunTest", "handoff-marker")
            val log = shell("logcat -d -t 400")
            assertTrue(log.contains("handoff-marker"))
            for (secret in listOf(email, dob, firstAnswer, secondAnswer)) {
                assertFalse("a registration secret reached logcat", log.contains(secret))
            }
        } finally {
            identity.wipe()
            vault.close()
            root.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        }
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
