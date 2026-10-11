package `in`.growurdhan.app.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalAppPasswordInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scope: String
    private lateinit var store: LocalAppPasswordStore

    @Before fun setup() {
        scope = UUID.randomUUID().toString()
        store = LocalAppPasswordStore(context, scope)
    }

    @After fun cleanup() {
        store.lock()
        context.getSharedPreferences("gud_local_password_test_$scope", 0).edit().clear().commit()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            .deleteEntry("in.growurdhan.app.local_password.test.$scope")
    }

    @Test fun localPasswordRequiresExplicitSetupAndDeviceSessionRemainsSeparate() {
        assertEquals(LocalLoginStatus.NEEDS_SETUP, store.status.value)
        assertFalse(store.hasVerifiedPassword())
        val password = "Synthetic!Pass123".toCharArray()
        try {
            assertTrue(store.setup(password))
            assertTrue(store.hasVerifiedPassword())
            store.lock()
            assertEquals(LocalLoginStatus.LOCKED, store.status.value)
            assertFalse(store.verify("Incorrect!Pass123".toCharArray()))
            assertFalse(store.hasVerifiedPassword())
            assertTrue(store.verify(password))
            assertTrue(store.hasVerifiedPassword())
            store.lock()
            val restarted = LocalAppPasswordStore(context, scope)
            assertEquals(LocalLoginStatus.LOCKED, restarted.status.value)
            assertFalse(restarted.hasVerifiedPassword())
            assertTrue(restarted.verify(password))
            assertThrows(IllegalStateException::class.java) {
                restarted.setup("Different!Pass123".toCharArray())
            }
        } finally {
            password.fill('\u0000')
        }
    }

    @Test fun rotationRequiresOldPasswordAndInvalidatesPriorLogin() {
        val old = "Synthetic!Pass123".toCharArray()
        val replacement = "Another!Secure456".toCharArray()
        try {
            assertTrue(store.setup(old))
            assertFalse(store.rotatePassword("Wrong!Pass999".toCharArray(), replacement))
            assertFalse(store.hasVerifiedPassword())
            assertTrue(store.verify(old))
            assertTrue(store.rotatePassword(old, replacement))
            assertFalse(store.hasVerifiedPassword())
            assertFalse(store.verify(old))
            assertTrue(store.verify(replacement))
        } finally {
            old.fill('\u0000')
            replacement.fill('\u0000')
        }
    }

    @Test fun recoveryCodeRequiresVerifiedEnrolmentAndIsSingleUse() {
        val original="Synthetic!Pass123".toCharArray()
        val next="Replacement!Secure456".toCharArray()
        try {
            assertThrows(IllegalStateException::class.java) {
                store.issueRecoveryCodeAfterDeviceApproval()
            }
            assertTrue(store.setup(original))
            val recovery=store.issueRecoveryCodeAfterDeviceApproval().toCharArray()
            assertEquals(43,recovery.size)
            assertTrue(store.hasRecoveryCode())
            store.lock()
            val restarted=LocalAppPasswordStore(context,scope)
            assertFalse(restarted.verify("Wrong!Password123".toCharArray()))
            assertFalse(restarted.recoverAfterDeviceApproval(
                "wrong-recovery-code-recovery-code-recovery-code".toCharArray(),next))
            assertFalse(restarted.hasVerifiedPassword())
            assertTrue(restarted.recoverAfterDeviceApproval(recovery,next))
            assertFalse(restarted.hasRecoveryCode())
            assertFalse(restarted.hasVerifiedPassword())
            assertFalse(restarted.verify(original))
            assertTrue(restarted.verify(next))
            assertFalse(restarted.recoverAfterDeviceApproval(recovery,original))
            recovery.fill('\u0000')
        } finally { original.fill('\u0000');next.fill('\u0000') }
    }

    @Test fun missingKeystoreDoesNotPermitRecoveryCodeToRecreateVerifier() {
        store.setup("Synthetic!Pass123".toCharArray())
        val recovery=store.issueRecoveryCodeAfterDeviceApproval().toCharArray()
        store.lock()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            .deleteEntry("in.growurdhan.app.local_password.test.$scope")
        val restarted=LocalAppPasswordStore(context,scope)
        assertFalse(restarted.recoverAfterDeviceApproval(
            recovery,"Replacement!Secure456".toCharArray()))
        assertFalse(restarted.hasVerifiedPassword())
        assertEquals(LocalLoginStatus.STORAGE_UNAVAILABLE,restarted.status.value)
        recovery.fill('\u0000')
    }

    @Test fun corruptedVerifierCannotAutoResetOrUnlockNativeVault() {
        store.setup("Synthetic!Pass123".toCharArray())
        store.lock()
        context.getSharedPreferences("gud_local_password_test_$scope", 0)
            .edit().remove("tag").commit()
        val restarted = LocalAppPasswordStore(context, scope)
        assertEquals(LocalLoginStatus.STORAGE_UNAVAILABLE, restarted.status.value)
        assertFalse(restarted.verify("Synthetic!Pass123".toCharArray()))
        assertFalse(restarted.hasVerifiedPassword())
        assertThrows(IllegalStateException::class.java) {
            restarted.setup("Synthetic!Pass123".toCharArray())
        }
    }
}
