package `in`.growurdhan.domain.finance

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Locked backup from src/lib/backup.ts.
 * The file is binary. The key is the vault email plus the password,
 * so there is no second passphrase. An old pretty-JSON export is refused.
 */
object BackupFile {
    const val KIND = "GUDVAULT"
    const val VERSION = 2
    private const val MAGIC = "GUDVLT01"
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private val random = SecureRandom()
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC)

    fun keyMaterial(email: String, password: String): String =
        "GrowUrDhan\n${email.trim().lowercase()}\n$password"

    fun isBinary(bytes: ByteArray): Boolean {
        if (bytes.size < 40) return false
        return bytes.copyOfRange(0, 8).toString(Charsets.UTF_8) == MAGIC
    }

    fun looksLikePlainJson(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("{") && (trimmed.contains("\"familyMembers\"") || trimmed.contains("\"holdings\""))
    }

    fun pack(json: String, email: String, password: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = aes(Cipher.ENCRYPT_MODE, pbkdf2(keyMaterial(email, password), salt, ITERATIONS), iv, json.toByteArray(Charsets.UTF_8))
        val out = ByteBuffer.allocate(40 + cipher.size).order(ByteOrder.BIG_ENDIAN)
        out.put(MAGIC.toByteArray(Charsets.US_ASCII))
        out.putInt(ITERATIONS)
        out.put(salt)
        out.put(iv)
        out.put(cipher)
        return out.array()
    }

    fun unpack(bytes: ByteArray, email: String, password: String): String {
        if (isBinary(bytes)) return unpackBinary(bytes, email, password)
        val text = bytes.toString(Charsets.UTF_8)
        if (looksLikePlainJson(text)) {
            throw BackupException("This file is an old unlocked backup. Export a new locked copy from Family.")
        }
        return unpackLegacy(text, password)
    }

    fun verifyIdentity(backupJson: String, id: RestoreIdentity): String? {
        val account = accountOf(backupJson)
        if (id.email.trim().lowercase() != account.email.lowercase()) return "Email or password does not match this backup."
        if (!AuthVault.verify(id.password, account.passwordSalt, account.passwordHash, account.passwordIterations)) {
            return "Email or password does not match this backup."
        }
        if (!id.strict) return null
        val pan = id.pan.orEmpty().trim().uppercase()
        val stored = account.panForStatements.orEmpty().trim().uppercase()
        if (stored.isNotEmpty()) {
            if (pan != stored) return "PAN does not match this backup."
        } else if (!account.panMasked.isNullOrBlank()) {
            if (VaultSeal.maskPan(pan) != account.panMasked) return "PAN does not match this backup."
        } else {
            return "This backup has no PAN. It cannot be opened on a new device."
        }
        if (id.dateOfBirth.isNullOrBlank() || id.dateOfBirth != account.dateOfBirth.orEmpty()) {
            return "Date of birth does not match this backup."
        }
        if (id.q1Id.isNullOrBlank() || id.q2Id.isNullOrBlank() || id.q1Id != account.q1Id || id.q2Id != account.q2Id) {
            return "Security questions do not match this backup."
        }
        val first = AuthVault.unpack(account.q1Hash) ?: return "Security questions are missing from this backup."
        val second = AuthVault.unpack(account.q2Hash) ?: return "Security questions are missing from this backup."
        val answer1 = AuthVault.verify(AuthVault.normaliseAnswer(id.q1Answer.orEmpty()), first.salt, first.hash, first.iterations)
        val answer2 = AuthVault.verify(AuthVault.normaliseAnswer(id.q2Answer.orEmpty()), second.salt, second.hash, second.iterations)
        if (!answer1 || !answer2) return "Security answers do not match this backup."
        return null
    }

    fun suggestedName(now: Instant): String = "growurdhan_${stamp.format(now).take(10)}.kosha"

    fun suggestedScheduledName(now: Instant): String {
        val compact = stamp.format(now).take(19).replace(Regex("[-:]"), "").replace("T", "_")
        return "growurdhan_$compact.kosha"
    }

    private fun unpackBinary(bytes: ByteArray, email: String, password: String): String {
        val view = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val iterations = view.getInt(8)
        val rounds = if (iterations == 0) ITERATIONS else iterations
        val salt = bytes.copyOfRange(12, 28)
        val iv = bytes.copyOfRange(28, 40)
        val cipher = bytes.copyOfRange(40, bytes.size)
        try {
            val plain = aes(Cipher.DECRYPT_MODE, pbkdf2(keyMaterial(email, password), salt, rounds), iv, cipher)
            val json = plain.toString(Charsets.UTF_8)
            val root = JsonValue.parse(json) as? Map<*, *> ?: throw BackupException("That file is not a GrowUrDhan backup.")
            if (root["kind"] != KIND || root["account"] !is Map<*, *>) throw BackupException("That file is not a GrowUrDhan backup.")
            return json
        } catch (error: BackupException) {
            throw error
        } catch (_: Exception) {
            throw BackupException("Email or password does not match this backup.")
        }
    }

    private fun unpackLegacy(text: String, password: String): String {
        val parsed = try {
            JsonValue.parse(text)
        } catch (_: IllegalArgumentException) {
            throw BackupException("That file is not a GrowUrDhan backup.")
        }
        val blob = parsed as? Map<*, *> ?: throw BackupException("That file is not a GrowUrDhan backup.")
        if (blob["magic"] != "KOSHA1" || blob["cipherB64"] !is String) {
            throw BackupException("That file is not a GrowUrDhan backup.")
        }
        val rounds = (blob["iterations"] as? Number)?.toInt()?.takeIf { it != 0 } ?: ITERATIONS
        try {
            val plain = aes(
                Cipher.DECRYPT_MODE,
                pbkdf2(password, fromBase64(blob["saltB64"] as? String ?: ""), rounds),
                fromBase64(blob["ivB64"] as? String ?: ""),
                fromBase64(blob["cipherB64"] as String),
            )
            val json = plain.toString(Charsets.UTF_8)
            val inner = JsonValue.parse(json) as? Map<*, *> ?: throw BackupException("That file is not a GrowUrDhan backup.")
            if (inner["account"] !is Map<*, *>) {
                throw BackupException("This older backup has no login. Create the vault on this computer first, then export a new locked file.")
            }
            return json
        } catch (error: BackupException) {
            throw error
        } catch (_: Exception) {
            throw BackupException("Email or password does not match this backup.")
        }
    }

    private fun accountOf(json: String): BackupAccount {
        val root = JsonValue.parse(json) as Map<*, *>
        val account = root["account"] as Map<*, *>
        return BackupAccount(
            email = account["email"] as String,
            passwordSalt = account["passwordSalt"] as String,
            passwordHash = account["passwordHash"] as String,
            passwordIterations = (account["passwordIterations"] as Number).toInt(),
            panForStatements = account["panForStatements"] as? String,
            panMasked = account["panMasked"] as? String,
            dateOfBirth = account["dateOfBirth"] as? String,
            q1Id = account["q1Id"] as String,
            q1Hash = account["q1Hash"] as String,
            q2Id = account["q2Id"] as String,
            q2Hash = account["q2Hash"] as String,
        )
    }

    private fun aes(mode: Int, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val block = byteArrayOf(0, 0, 0, 1)
        mac.update(salt)
        var mixed = mac.doFinal(block)
        val total = mixed.copyOf()
        for (round in 2..iterations) {
            mac.reset()
            mixed = mac.doFinal(mixed)
            for (i in total.indices) total[i] = (total[i].toInt() xor mixed[i].toInt()).toByte()
        }
        return total.copyOf(32)
    }

    private fun fromBase64(value: String): ByteArray = Base64.getDecoder().decode(value)
}

data class BackupAccount(
    val email: String,
    val passwordSalt: String,
    val passwordHash: String,
    val passwordIterations: Int,
    val panForStatements: String?,
    val panMasked: String?,
    val dateOfBirth: String?,
    val q1Id: String,
    val q1Hash: String,
    val q2Id: String,
    val q2Hash: String,
)

data class RestoreIdentity(
    val email: String,
    val password: String,
    val strict: Boolean,
    val pan: String? = null,
    val dateOfBirth: String? = null,
    val q1Id: String? = null,
    val q1Answer: String? = null,
    val q2Id: String? = null,
    val q2Answer: String? = null,
)

class BackupException(message: String) : Exception(message)
