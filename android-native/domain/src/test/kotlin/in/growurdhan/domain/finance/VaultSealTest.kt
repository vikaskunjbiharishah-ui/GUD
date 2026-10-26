package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section H, plus a Node AES-GCM ciphertext. */
class VaultSealTest {
    private val plain = """{"holdings":[{"name":"Reliance","value":389000}],"pan":"ABCDE1234F"}"""

    @Test fun passphraseRoundTripHidesTheBook() {
        val fromNode = EncryptedBlob(
            "KOSHA1", "PBKDF2-SHA256", 600_000,
            "MDEyMzQ1Njc4OWFiY2RlZg==", "MDEyMzQ1Njc4OWFi",
            "gIOZ9VWs/W7YSQo4eG9So455yrCNfDOSNv42HLRWEeiwjv+A3Y4t7u29J+AO/SthzVcH0EtfAEJSA8uy05jRLwEtYIenoxo/W0VUnScQGAvm94su",
        )
        assertTrue(VaultSeal.isEncrypted(fromNode))
        assertFalse(fromNode.cipher.contains("Reliance"))
        assertEquals(plain, VaultSeal.decrypt(fromNode, "correct horse battery"))
        assertThrows(WrongPassphraseException::class.java) { VaultSeal.decrypt(fromNode, "wrong password") }
        val tampered = fromNode.copy(cipher = fromNode.cipher.dropLast(4) + "AAAA")
        assertThrows(Exception::class.java) { VaultSeal.decrypt(tampered, "correct horse battery") }
        assertThrows(IllegalArgumentException::class.java) {
            VaultSeal.decrypt(fromNode.copy(magic = "OTHER"), "correct horse battery")
        }
        assertFalse(VaultSeal.isEncrypted(null))

        val sealed = VaultSeal.encrypt(plain, "correct horse battery")
        assertEquals("KOSHA1", sealed.magic)
        assertEquals(600_000, sealed.iterations)
        assertFalse(sealed.cipher.contains("Reliance"))
        assertEquals(plain, VaultSeal.decrypt(sealed, "correct horse battery"))

        val key = VaultSeal.recoveryKey()
        assertTrue(Regex("""^[0-9A-F]{5}(-[0-9A-F]{1,5})+$""").matches(key))
        assertEquals(64, key.replace("-", "").length)
        assertEquals("ABCDE****F", VaultSeal.maskPan("abcde1234f"))
        assertEquals("**********", VaultSeal.maskPan("short"))
        assertEquals("", VaultSeal.maskPan("  "))
    }
}
