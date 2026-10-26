package `in`.growurdhan.domain.finance

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Device envelope from src/db/encryptedStore.ts.
 * GUDENC01, then a 16-byte salt, a 12-byte IV, and AES-GCM.
 * On Android the Keystore holds the file. This is the fallback wrap.
 */
object DeviceEnvelope {
    const val MAGIC = "GUDENC01"
    const val ITERATIONS = 310_000
    private const val PEPPER = "GrowUrDhan-vault-wrap/v1"
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private val random = SecureRandom()

    fun hasMagic(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return bytes.copyOfRange(0, 8).toString(Charsets.UTF_8) == MAGIC
    }

    fun seal(plain: ByteArray, salt: ByteArray = randomSalt()): ByteArray {
        val key = derive(salt, PEPPER)
        val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
        val cipher = aes(Cipher.ENCRYPT_MODE, key, iv, plain)
        return MAGIC.toByteArray(Charsets.US_ASCII) + salt.copyOf(SALT_LEN) + iv + cipher
    }

    fun open(packed: ByteArray, salt: ByteArray): ByteArray {
        val key = derive(salt, PEPPER)
        return openWithKey(packed, key, saltInFile = true)
    }

    private fun openWithKey(packed: ByteArray, key: ByteArray, saltInFile: Boolean): ByteArray {
        val saltOff = if (saltInFile) SALT_LEN else 0
        val start = 8 + saltOff
        val iv = packed.copyOfRange(start, start + IV_LEN)
        val cipher = packed.copyOfRange(start + IV_LEN, packed.size)
        return aes(Cipher.DECRYPT_MODE, key, iv, cipher)
    }

    private fun derive(salt: ByteArray, material: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(material.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val block = byteArrayOf(0, 0, 0, 1)
        mac.update(salt.copyOf(SALT_LEN))
        var mixed = mac.doFinal(block)
        val total = mixed.copyOf()
        for (round in 2..ITERATIONS) {
            mac.reset()
            mixed = mac.doFinal(mixed)
            for (i in total.indices) total[i] = (total[i].toInt() xor mixed[i].toInt()).toByte()
        }
        return total.copyOf(32)
    }

    private fun aes(mode: Int, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun randomSalt(): ByteArray = ByteArray(SALT_LEN).also { random.nextBytes(it) }
}
