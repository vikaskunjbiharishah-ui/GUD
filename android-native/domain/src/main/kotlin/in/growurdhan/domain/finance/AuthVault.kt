package `in`.growurdhan.domain.finance

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Local vault login from src/lib/auth.ts.
 * The password is hashed with PBKDF2-SHA256. It is never stored in the clear.
 * Remembering the email stays on the device.
 */
object AuthVault {
    const val PASSWORD_MAX_AGE_DAYS = 180
    const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private val random = SecureRandom()

    val questions = listOf(
        "maiden" to "What is your mother's maiden name?",
        "school" to "What was the name of your first school?",
        "pet" to "What was the name of your first pet?",
        "city" to "In which city were you born?",
        "teacher" to "What was the name of your favourite teacher?",
        "vehicle" to "What was your first car or bike?",
    )

    fun titleCaseName(raw: String): String {
        val text = raw.trim().replace(Regex("\\s+"), " ")
        if (text.isEmpty()) return ""
        return text.split(" ").joinToString(" ") { word ->
            word.take(1).uppercase(Locale.ROOT) + word.drop(1).lowercase(Locale.ROOT)
        }
    }

    fun passwordIssues(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 8) issues += "At least 8 characters"
        if (!Regex("[A-Z]").containsMatchIn(password)) issues += "One capital letter"
        if (!Regex("[a-z]").containsMatchIn(password)) issues += "One small letter"
        if (!Regex("[0-9]").containsMatchIn(password)) issues += "One number"
        if (!Regex("[^A-Za-z0-9]").containsMatchIn(password)) issues += "One special character"
        return issues
    }

    /** A full PAN only. A masked PAN is not accepted here. */
    fun isValidPan(pan: String): Boolean =
        Regex("""^[A-Z]{5}[0-9]{4}[A-Z]$""").matches(pan.trim().uppercase())

    fun isValidEmail(email: String): Boolean =
        Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(email.trim())

    fun normaliseAnswer(raw: String): String =
        raw.trim().lowercase().replace(Regex("\\s+"), " ")

    fun needsRotation(changedAt: String, now: Instant): Boolean {
        val changed = runCatching { Instant.parse(changedAt) }.getOrNull() ?: return true
        val days = (now.toEpochMilli() - changed.toEpochMilli()) / 86_400_000.0
        return days >= PASSWORD_MAX_AGE_DAYS
    }

    fun hashSecret(secret: String): SecretHash {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return SecretHash(base64(salt), base64(pbkdf2(secret, salt, ITERATIONS)), ITERATIONS)
    }

    fun verify(secret: String, salt: String, hash: String, iterations: Int): Boolean {
        val rounds = if (iterations == 0) ITERATIONS else iterations
        val got = base64(pbkdf2(secret, fromBase64(salt), rounds))
        return same(got, hash)
    }

    fun pack(hash: SecretHash): String = "${hash.salt}:${hash.hash}:${hash.iterations}"

    fun unpack(packed: String): SecretHash? {
        val parts = packed.split(":")
        if (parts.size != 3) return null
        val rounds = parts[2].toIntOrNull()
        return SecretHash(parts[0], parts[1], if (rounds == null || rounds == 0) ITERATIONS else rounds)
    }

    fun toPublic(account: VaultAccount): PublicAccount = PublicAccount(
        account.email, account.firstName, account.lastName, account.panMasked,
        account.dateOfBirth, account.passwordChangedAt, account.q1Id, account.q2Id,
    )

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val length = HASH_BITS / 8
        val blocks = (length + 31) / 32
        val out = ByteArray(blocks * 32)
        val block = ByteArray(4)
        for (index in 1..blocks) {
            block[0] = (index shr 24).toByte()
            block[1] = (index shr 16).toByte()
            block[2] = (index shr 8).toByte()
            block[3] = index.toByte()
            mac.reset()
            mac.update(salt)
            var mixed = mac.doFinal(block)
            val total = mixed.copyOf()
            for (round in 2..iterations) {
                mac.reset()
                mixed = mac.doFinal(mixed)
                for (i in total.indices) total[i] = (total[i].toInt() xor mixed[i].toInt()).toByte()
            }
            total.copyInto(out, (index - 1) * 32)
        }
        return out.copyOf(length)
    }

    private fun same(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        var bits = 0
        for (i in left.indices) bits = bits or (left[i].code xor right[i].code)
        return bits == 0
    }

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun fromBase64(value: String): ByteArray = Base64.getDecoder().decode(value)
}

data class SecretHash(val salt: String, val hash: String, val iterations: Int)

data class VaultAccount(
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordSalt: String,
    val passwordHash: String,
    val passwordIterations: Int,
    val passwordChangedAt: String,
    val q1Id: String,
    val q1Hash: String,
    val q2Id: String,
    val q2Hash: String,
    val createdAt: String,
    val panMasked: String? = null,
    val panForStatements: String? = null,
    val dateOfBirth: String? = null,
)

data class PublicAccount(
    val email: String,
    val firstName: String,
    val lastName: String,
    val panMasked: String?,
    val dateOfBirth: String?,
    val passwordChangedAt: String,
    val q1Id: String,
    val q2Id: String,
)
