package `in`.growurdhan.app.presentation

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.growurdhan.domain.vault.AppPasswordAuthorization
import `in`.growurdhan.domain.vault.LocalPasswordRules
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LocalLoginStatus {
    /** Only a never-enrolled installation may create its first verifier. */
    NEEDS_SETUP, LOCKED, VERIFIED, STORAGE_UNAVAILABLE,
}

/**
 * Adds GrowUrDhan-specific login without replacing device-credential checks.
 * PBKDF2-HMAC-SHA256 (210k rounds, as in the existing TS password rules) is
 * HMAC-bound to an Android Keystore non-exportable key. SharedPreferences
 * hold a salt and HMAC tag, never a password, PAN, or financial-record key.
 *
 * The app password is a LOCAL authorization gate. It does NOT decrypt the
 * SQLCipher key and cannot by itself recover a lost Keystore or phone.
 */
@Singleton
class LocalAppPasswordStore private constructor(
    context: Context, prefsName: String, private val keyAlias: String,
) : AppPasswordAuthorization {
    @Inject constructor(@ApplicationContext context: Context) : this(
        context, "gud_local_password_v1", "in.growurdhan.app.local_password.v1",
    )

    /** Tests isolate their verifier and Keystore namespace from real app data. */
    internal constructor(context: Context, isolatedTestName: String) : this(
        context, "gud_local_password_test_$isolatedTestName",
        "in.growurdhan.app.local_password.test.$isolatedTestName",
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val mutableStatus = MutableStateFlow(initialStatus())
    val status: StateFlow<LocalLoginStatus> = mutableStatus

    private var authorized = false

    private fun initialStatus(): LocalLoginStatus = try {
        when {
            !prefs.contains("enrolled") &&
                !prefs.contains("salt") && !prefs.contains("tag") -> LocalLoginStatus.NEEDS_SETUP
            prefs.getBoolean("enrolled", false) &&
                !prefs.getString("salt", null).isNullOrEmpty() &&
                !prefs.getString("tag", null).isNullOrEmpty() -> LocalLoginStatus.LOCKED
            else -> LocalLoginStatus.STORAGE_UNAVAILABLE
        }
    } catch (_: Exception) {
        LocalLoginStatus.STORAGE_UNAVAILABLE
    }

    @Synchronized override fun hasVerifiedPassword(): Boolean =
        authorized && mutableStatus.value == LocalLoginStatus.VERIFIED

    private fun emailMac(email: String): ByteArray {
        val value = email.trim().lowercase(Locale.ROOT)
        require(value.length in 5..254 &&
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value))
        val bytes = ("GUD:email:v1:" + value).toByteArray(Charsets.UTF_8)
        return try { hmac(bytes, allowCreate = false) } finally { bytes.fill(0) }
    }

    @Synchronized fun hasAccountEmailBinding(): Boolean =
        !prefs.getString("account_email_mac", null).isNullOrBlank()

    /** An email mismatch revokes the local password admission immediately. */
    @Synchronized fun verifyAccountEmail(email: String): Boolean {
        if (!hasVerifiedPassword()) return false
        val expected = prefs.getString("account_email_mac", null) ?: return true
        val obtained = try { emailMac(email) } catch (_: Exception) {
            lock(); return false
        }
        return try {
            val saved = decode(expected)
            try {
                val matches = saved.size == 32 && MessageDigest.isEqual(saved, obtained)
                if (!matches) lock()
                matches
            } finally { saved.fill(0) }
        } catch (_: Exception) {
            lock(); false
        } finally { obtained.fill(0) }
    }

    /** Migrate an old verified installation after native account registration. */
    @Synchronized fun bindAccountEmail(email: String): Boolean {
        if (!hasVerifiedPassword()) return false
        val obtained = try { emailMac(email) } catch (_: Exception) { return false }
        return try {
            val old = prefs.getString("account_email_mac", null)
            if (old == null) prefs.edit()
                .putString("account_email_mac", encode(obtained)).commit()
            else {
                val saved = decode(old)
                try { saved.size == 32 && MessageDigest.isEqual(saved, obtained) }
                finally { saved.fill(0) }
            }
        } catch (_: Exception) { false }
        finally { obtained.fill(0) }
    }

    @Synchronized fun passwordNeedsRotation(nowMillis: Long = System.currentTimeMillis()): Boolean =
        initialStatus() == LocalLoginStatus.LOCKED &&
            LocalPasswordRules.requiresRotation(prefs.getLong("changed_at", 0L), nowMillis)

    @Synchronized fun setup(password: CharArray, accountEmail: String? = null): Boolean {
        check(initialStatus() == LocalLoginStatus.NEEDS_SETUP) {
            "An existing password may never be replaced by setup."
        }
        require(LocalPasswordRules.issues(password).isEmpty()) {
            "Password must meet the original GrowUrDhan complexity rules."
        }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val derived = derive(password, salt)
        val tag = try {
            hmac(derived, allowCreate = true)
        } finally {
            derived.fill(0)
        }
        val emailTag = accountEmail?.let { emailMac(it) }
        val editor = prefs.edit()
            .putString("salt", encode(salt))
            .putString("tag", encode(tag))
            .putInt("iterations", LocalPasswordRules.KDF_ITERATIONS)
            .putLong("changed_at", System.currentTimeMillis())
            .putBoolean("enrolled", true)
            .putInt("failed_attempts", 0)
            .putLong("blocked_until", 0L)
        if (emailTag != null) editor.putString("account_email_mac", encode(emailTag))
        val saved = editor.commit()
        emailTag?.fill(0)
        salt.fill(0)
        tag.fill(0)
        if (!saved) {
            authorized = false
            mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
            return false
        }
        authorized = true
        mutableStatus.value = LocalLoginStatus.VERIFIED
        return true
    }

    /**
     * Synchronous cryptography; callers must dispatch off the main thread.
     * Throttle state persists across process restarts; device authentication
     * is still required separately to open and mutate SQLCipher records.
     */
    @Synchronized fun verify(password: CharArray): Boolean {
        authorized = false
        if (initialStatus() != LocalLoginStatus.LOCKED) {
            mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
            return false
        }
        val now = System.currentTimeMillis()
        if (now < prefs.getLong("blocked_until", 0L)) {
            mutableStatus.value = LocalLoginStatus.LOCKED
            return false
        }
        try {
            if (prefs.getInt("iterations", 0) != LocalPasswordRules.KDF_ITERATIONS) {
                mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
                return false
            }
            val salt = decode(prefs.getString("salt", null) ?: "")
            val expected = decode(prefs.getString("tag", null) ?: "")
            if (salt.size != 16 || expected.size != 32) {
                mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
                return false
            }
            val derived = derive(password, salt)
            val obtained = try { hmac(derived, allowCreate = false) }
                finally { derived.fill(0) }
            val same = MessageDigest.isEqual(expected, obtained)
            obtained.fill(0)
            salt.fill(0)
            expected.fill(0)
            if (same) {
                if (!prefs.edit().putInt("failed_attempts", 0)
                        .putLong("blocked_until", 0L).commit()) {
                    mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
                    return false
                }
                authorized = true
                mutableStatus.value = LocalLoginStatus.VERIFIED
                return true
            }
            val attempts = (prefs.getInt("failed_attempts", 0) + 1).coerceAtMost(20)
            val seconds = if (attempts < 5) 0L
                else (30L shl (attempts - 5).coerceAtMost(6)).coerceAtMost(1800L)
            val until = if (seconds > 0) now + seconds * 1000L else 0L
            if (!prefs.edit().putInt("failed_attempts", attempts)
                    .putLong("blocked_until", until).commit()) {
                mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
                return false
            }
            mutableStatus.value = LocalLoginStatus.LOCKED
            return false
        } catch (_: Exception) {
            // Missing/invalid Keystore key or corrupt metadata must never
            // silently regenerate a verifier over the existing encrypted vault.
            mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
            return false
        }
    }

    /**
     * The Activity must require a fresh OS credential BEFORE this call.
     * Explicitly verify the previous password; no device-only reset exists.
     * The SQLCipher/Keystore database key is neither read nor modified.
     */
    @Synchronized fun rotatePassword(current: CharArray, replacement: CharArray): Boolean {
        if (!hasVerifiedPassword()) return false
        require(LocalPasswordRules.issues(replacement).isEmpty()) {
            "New password does not meet the required complexity policy."
        }
        try {
            val previousSalt = decode(prefs.getString("salt", null) ?: "")
            val previousTag = decode(prefs.getString("tag", null) ?: "")
            require(previousSalt.size == 16 && previousTag.size == 32)
            val derived = derive(current, previousSalt)
            val actual = try { hmac(derived, allowCreate = false) }
                finally { derived.fill(0) }
            val verified = MessageDigest.isEqual(previousTag, actual)
            actual.fill(0)
            previousTag.fill(0)
            previousSalt.fill(0)
            if (!verified) {
                lock()
                return false
            }
            val newSalt = ByteArray(16).also(SecureRandom()::nextBytes)
            val next = derive(replacement, newSalt)
            val newTag = try { hmac(next, allowCreate = false) }
                finally { next.fill(0) }
            val saved = prefs.edit()
                .putString("salt", encode(newSalt))
                .putString("tag", encode(newTag))
                .putLong("changed_at", System.currentTimeMillis())
                .putInt("failed_attempts", 0)
                .putLong("blocked_until", 0L)
                .commit()
            newSalt.fill(0)
            newTag.fill(0)
            lock() // Always demand a fresh app-password login after rotation.
            return saved
        } catch (_: Exception) {
            authorized = false
            mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
            return false
        }
    }

    /**
     * M25: issue a 256-bit recovery code *only after the app password has been
     * verified*. Called by MainActivity only after an Android OS credential.
     * The code is shown ONCE. No plaintext code/PAN/security answers on disk.
     * Existing native vault/database wrapping keys are never changed.
     */
    @Synchronized fun issueRecoveryCodeAfterDeviceApproval(): String {
        check(hasVerifiedPassword()) { "Verify current app password first." }
        val random = ByteArray(32).also(SecureRandom()::nextBytes)
        val code = Base64.encodeToString(random,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        try {
            val tag = hmac(random, allowCreate = false)
            try {
                check(prefs.edit().putString("recovery_tag", encode(tag))
                    .putInt("recovery_failures", 0)
                    .putLong("recovery_blocked_until", 0L).commit()) {
                    "Cannot persist recovery verifier."
                }
            } finally { tag.fill(0) }
            return code
        } finally { random.fill(0) }
    }

    @Synchronized fun hasRecoveryCode(): Boolean =
        initialStatus() == LocalLoginStatus.LOCKED &&
            !prefs.getString("recovery_tag", null).isNullOrEmpty()

    /**
     * Only MainActivity's successful fresh Android device credential callback
     * may invoke this method. It does NOT open the financial vault or mint a
     * device-session approval. Recovery is impossible when Keystore verifier,
     * recovery code or local login records have been lost/corrupted.
     */
    @Synchronized fun recoverAfterDeviceApproval(code: CharArray, next: CharArray): Boolean {
        authorized = false
        if (initialStatus() != LocalLoginStatus.LOCKED ||
            LocalPasswordRules.issues(next).isNotEmpty() ||
            code.size !in 40..64 ||
            System.currentTimeMillis() < prefs.getLong("recovery_blocked_until", 0L)
        ) {
            mutableStatus.value = initialStatus()
            return false
        }
        val stored = prefs.getString("recovery_tag", null) ?: return false
        val actual = try {
            Base64.decode(code.concatToString(), Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (_: Exception) { return false }
        try {
            if (actual.size != 32) return false
            val expected = decode(stored)
            if (expected.size != 32) return false
            val tag = try { hmac(actual, allowCreate = false) } catch (_: Exception) {
                mutableStatus.value = LocalLoginStatus.STORAGE_UNAVAILABLE
                return false
            }
            val accepted = try { MessageDigest.isEqual(expected, tag) } finally {
                expected.fill(0)
                tag.fill(0)
            }
            if (!accepted) {
                val failures = (prefs.getInt("recovery_failures", 0) + 1).coerceAtMost(20)
                val seconds = if (failures < 3) 0L
                    else (60L shl (failures - 3).coerceAtMost(5)).coerceAtMost(1800L)
                prefs.edit().putInt("recovery_failures", failures)
                    .putLong("recovery_blocked_until",
                        if (seconds > 0) System.currentTimeMillis() + seconds * 1000L else 0L)
                    .commit()
                mutableStatus.value = LocalLoginStatus.LOCKED
                return false
            }
            val salt = ByteArray(16).also(SecureRandom()::nextBytes)
            val derived = derive(next, salt)
            val replacement = try { hmac(derived, allowCreate = false) }
                finally { derived.fill(0) }
            val written = try {
                prefs.edit().putString("salt", encode(salt))
                    .putString("tag", encode(replacement))
                    .remove("recovery_tag") // single-use code cannot be replayed
                    .putLong("changed_at", System.currentTimeMillis())
                    .putInt("failed_attempts", 0).putLong("blocked_until", 0L)
                    .putInt("recovery_failures", 0).putLong("recovery_blocked_until", 0L)
                    .commit()
            } finally { replacement.fill(0); salt.fill(0) }
            mutableStatus.value = if (written) LocalLoginStatus.LOCKED
                else LocalLoginStatus.STORAGE_UNAVAILABLE
            return written
        } finally { actual.fill(0) }
    }

    @Synchronized fun lock() {
        authorized = false
        mutableStatus.value = initialStatus()
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, LocalPasswordRules.KDF_ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun hmac(derived: ByteArray, allowCreate: Boolean): ByteArray {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        var key = keystore.getKey(keyAlias, null) as? SecretKey
        if (key == null && allowCreate) {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore",
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                ).setDigests(KeyProperties.DIGEST_SHA256).build(),
            )
            key = generator.generateKey()
        }
        check(key != null) { "Local login hardware verifier is unavailable." }
        return Mac.getInstance("HmacSHA256").run {
            init(key)
            doFinal(derived)
        }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
