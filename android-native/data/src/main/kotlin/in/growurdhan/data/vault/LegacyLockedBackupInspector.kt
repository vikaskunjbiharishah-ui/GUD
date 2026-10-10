package `in`.growurdhan.data.vault

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Locale

/** Counts only; no PAN, identity, account number, names or PDF bytes are returned. */
data class LegacyBackupPreview(
    val familyMemberCount: Int,
    val holdingCount: Int,
    val eventCount: Int,
    val accountCount: Int,
    val goalCount: Int,
    val hasCasHistory: Boolean,
    val hasExtendedHoldingDetails: Boolean,
    val sourceFingerprint: String,
    /** Never true until a separate lossless migration plan/schema is approved. */
    val losslessImportAvailable: Boolean = false,
)

/**
 * Read-only authentication and structural inspection of original GrowUrDhan
 * GUDVLT01 .kosha AES-GCM archives. NOT an importer or password reset.
 *
 * The source backup's KDF: PBKDF2-SHA256(email/password UTF-8, salt16,
 * 600000 iterations, 256 bits), nonce12, AES-GCM tag16. Strict bounds avoid
 * hostile iterations, decompression or oversized JSON allocations.
 */
object LegacyLockedBackupInspector {
    private val MAGIC = "GUDVLT01".toByteArray(Charsets.US_ASCII)
    const val MAX_INPUT_BYTES = 32 * 1024 * 1024
    private const val HEADER_BYTES = 40
    private const val TAG_BYTES = 16
    private const val ITERATIONS = 600_000

    fun inspect(data: ByteArray, email: String, password: CharArray): LegacyBackupPreview {
        require(data.size in HEADER_BYTES + TAG_BYTES..MAX_INPUT_BYTES) {
            "Unsupported legacy backup size."
        }
        require(data.copyOfRange(0, 8).contentEquals(MAGIC)) {
            "Only original locked .kosha backups are supported."
        }
        val normalized = email.trim().lowercase(Locale.ROOT)
        require(normalized.length in 5..254 &&
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(normalized)) {
            "A valid backup account email is required."
        }
        require(password.size in 1..1024) { "A backup password is required." }
        val header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        header.position(8)
        require(header.int == ITERATIONS) {
            "Unsupported or unsafe legacy backup key derivation count."
        }
        val salt = ByteArray(16).also(header::get)
        val nonce = ByteArray(12).also(header::get)
        // Replicate the legacy TextEncoder key material exactly, never persist it.
        val secretChars = ("GrowUrDhan\n" + normalized + "\n" + password.concatToString())
            .toCharArray()
        val spec = PBEKeySpec(secretChars, salt, ITERATIONS, 256)
        secretChars.fill('\u0000')
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            salt.fill(0)
        }
        val plaintext = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
                    GCMParameterSpec(128, nonce))
                doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
            }
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "Backup email/password failed authentication, or file was changed."
            )
        } finally {
            key.fill(0)
            nonce.fill(0)
        }
        try {
            val root = JSONObject(plaintext.toString(Charsets.UTF_8))
            require(root.optString("kind") == "GUDVAULT" && root.optInt("version") == 2) {
                "Only locked GrowUrDhan version-two exports can be inspected."
            }
            val account = root.getJSONObject("account")
            require(account.getString("email").trim().lowercase(Locale.ROOT) == normalized) {
                "The embedded vault account does not match the chosen email."
            }
            fun count(name: String): Int {
                val array = root.getJSONArray(name)
                require(array.length() <= 100_000) {
                    "Legacy backup has too many records."
                }
                return array.length()
            }
            val members = count("familyMembers")
            val holdings = count("holdings")
            val events = count("ledgerEvents")
            val accounts = count("accounts")
            val goals = count("goals")
            val hasHistory = !root.isNull("casLatest")
            val holdingsData = root.getJSONArray("holdings")
            val rich = (0 until holdingsData.length()).any {
                val entry = holdingsData.getJSONObject(it)
                !entry.isNull("details") || !entry.isNull("allocationSplit") ||
                    entry.optString("subCategory").isNotBlank() ||
                    !entry.isNull("monthlyEmiMinor") ||
                    !entry.isNull("accountNumberMasked")
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return LegacyBackupPreview(
                members, holdings, events, accounts, goals, hasHistory, rich, digest,
            )
        } finally {
            plaintext.fill(0)
        }
    }
}
