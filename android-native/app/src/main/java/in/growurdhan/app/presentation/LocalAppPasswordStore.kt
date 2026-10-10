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

    @Synchronized fun passwordNeedsRotation(nowMillis: Long = System.currentTimeMillis()): Boolean =
        initialStatus() == LocalLoginStatus.LOCKED &&
            LocalPasswordRules.requiresRotation(prefs.getLong("changed_at", 0L), nowMillis)

    @Synchronized fun setup(password: CharArray): Boolean {
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
        val saved = prefs.edit()
            .putString("salt", encode(salt))
            .putString("tag", encode(tag))
            .putInt("iterations", LocalPasswordRules.KDF_ITERATIONS)
            .putLong("changed_at", System.currentTimeMillis())
            .putBoolean("enrolled", true)
            .putInt("failed_attempts", 0)
            .putLong("blocked_until", 0L)
            .commit()
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
