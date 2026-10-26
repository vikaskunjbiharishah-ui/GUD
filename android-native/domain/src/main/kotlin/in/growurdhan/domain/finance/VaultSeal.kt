package `in`.growurdhan.domain.finance

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption at rest from src/lib/vault.ts.
 * PBKDF2-SHA256 and AES-GCM. The passphrase is not stored.
 * A desktop build is still expected to move to Argon2id.
 */
object VaultSeal {
    const val MAGIC = "KOSHA1"
    const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private val random = SecureRandom()

    fun encrypt(json: String, passphrase: String): EncryptedBlob {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val key = pbkdf2(passphrase, salt, ITERATIONS)
        val cipher = aes(Cipher.ENCRYPT_MODE, key, iv, json.toByteArray(Charsets.UTF_8))
        return EncryptedBlob(MAGIC, "PBKDF2-SHA256", ITERATIONS, base64(salt), base64(iv), base64(cipher))
    }

    fun decrypt(blob: EncryptedBlob, passphrase: String): String {
        if (blob.magic != MAGIC) throw IllegalArgumentException("This is not a Kosha backup file.")
        val rounds = if (blob.iterations == 0) ITERATIONS else blob.iterations
        try {
            val key = pbkdf2(passphrase, fromBase64(blob.salt), rounds)
            val plain = aes(Cipher.DECRYPT_MODE, key, fromBase64(blob.iv), fromBase64(blob.cipher))
            return plain.toString(Charsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (_: Exception) {
            throw WrongPassphraseException()
        }
    }

    fun isEncrypted(blob: EncryptedBlob?): Boolean = blob?.magic == MAGIC

    fun recoveryKey(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return bytes.joinToString("") { "%02X".format(it) }.chunked(5).joinToString("-")
    }

    fun maskPan(pan: String): String {
        val value = pan.trim().uppercase()
        if (value.length != 10) return if (value.isEmpty()) "" else "**********"
        return value.substring(0, 5) + "****" + value.substring(9)
    }

    private fun aes(mode: Int, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val out = ByteArray(32)
        val block = byteArrayOf(0, 0, 0, 1)
        mac.update(salt)
        var mixed = mac.doFinal(block)
        val total = mixed.copyOf()
        for (round in 2..iterations) {
            mac.reset()
            mixed = mac.doFinal(mixed)
            for (i in total.indices) total[i] = (total[i].toInt() xor mixed[i].toInt()).toByte()
        }
        total.copyInto(out)
        return out
    }

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun fromBase64(value: String): ByteArray = Base64.getDecoder().decode(value)
}

data class EncryptedBlob(
    val magic: String,
    val kdf: String,
    val iterations: Int,
    val salt: String,
    val iv: String,
    val cipher: String,
)

class WrongPassphraseException : Exception("Wrong password, or this file has been altered.")
