package `in`.growurdhan.data.vault

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic v2 GUDVLT01 interop fixture; no actual account or financial PDF. */
@RunWith(AndroidJUnit4::class)
class LegacyBackupInspectorInstrumentedTest {
    private val email = "synthetic@example.test"
    private val password = "Synthetic!Pass123".toCharArray()

    private fun fixture(includeGoals: Boolean = true): ByteArray {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val keyMaterial = ("GrowUrDhan\n" + email + "\n" + password.concatToString())
            .toCharArray()
        val spec = PBEKeySpec(keyMaterial, salt, 600_000, 256)
        keyMaterial.fill('\u0000')
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        val body = JSONObject()
            .put("kind", "GUDVAULT")
            .put("version", 2)
            .put("account", JSONObject().put("email", email))
            .put("familyMembers", JSONArray().put(JSONObject().put("id", "synthetic-owner")))
            .put("holdings", JSONArray().put(
                JSONObject().put("id", "holding").put("subCategory", "PPF"),
            ))
            .put("ledgerEvents", JSONArray())
            .put("accounts", JSONArray())
            .put("goals", if (includeGoals) JSONArray().put(JSONObject().put("id", "goal")) else JSONArray())
            .put("casLatest", JSONObject().put("fixture", true))
            .toString().toByteArray(Charsets.UTF_8)
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                doFinal(body)
            }
        } finally {
            key.fill(0)
            body.fill(0)
        }
        return ByteBuffer.allocate(40 + cipher.size).order(ByteOrder.BIG_ENDIAN)
            .put("GUDVLT01".toByteArray(Charsets.US_ASCII))
            .putInt(600_000)
            .put(salt).put(nonce).put(cipher).array()
    }

    @Test fun originalBinaryFormatDecryptsAndProducesCountsButNeverRestores() {
        val payload = fixture()
        val result = LegacyLockedBackupInspector.inspect(payload, email.uppercase(), password)
        assertEquals(1, result.familyMemberCount)
        assertEquals(1, result.holdingCount)
        assertEquals(0, result.eventCount)
        assertEquals(1, result.goalCount)
        assertTrue(result.hasCasHistory)
        assertTrue(result.hasExtendedHoldingDetails)
        assertFalse(result.losslessImportAvailable)
        assertEquals(64, result.sourceFingerprint.length)
    }

    @Test fun tamperingWrongPasswordAndWrongEmailDoNotExposePreview() {
        val payload = fixture()
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect(payload, email, "Wrong!Password999".toCharArray())
        }
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect(payload, "unknown@example.test", password)
        }
        payload[payload.lastIndex] = (payload.last().toInt() xor 0x7f).toByte()
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect(payload, email, password)
        }
    }

    @Test fun unsafeKdfWrongMagicOrTruncatedArchiveFailsBeforeDecryption() {
        val original = fixture()
        val altered = original.copyOf().also {
            ByteBuffer.wrap(it, 8, 4).order(ByteOrder.BIG_ENDIAN).putInt(10)
        }
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect(altered, email, password)
        }
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect("plain-json".toByteArray(), email, password)
        }
        assertThrows(Exception::class.java) {
            LegacyLockedBackupInspector.inspect(original.copyOfRange(0, 20), email, password)
        }
    }
}
