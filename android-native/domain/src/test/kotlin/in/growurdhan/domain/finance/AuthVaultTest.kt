package `in`.growurdhan.domain.finance

import java.time.Instant
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts login checks, plus the WebCrypto PBKDF2 vector. */
class AuthVaultTest {
    @Test fun loginRulesAndPasswordHashMatchTauri() {
        assertEquals("Vikas", AuthVault.titleCaseName("vIKAS"))
        assertEquals("Shah", AuthVault.titleCaseName("SHAH"))
        assertEquals("Kunj Bihari", AuthVault.titleCaseName("  kunj   bihari "))
        assertEquals("", AuthVault.titleCaseName("   "))

        assertEquals(listOf("At least 8 characters"), AuthVault.passwordIssues("Ab1!"))
        assertEquals(emptyList<String>(), AuthVault.passwordIssues("Abcd1234!"))
        assertEquals(5, AuthVault.passwordIssues("").size)

        assertTrue(AuthVault.isValidPan("byaps1234q"))
        assertFalse(AuthVault.isValidPan("123"))
        assertFalse(AuthVault.isValidPan("ABCDE****F"))
        assertTrue(AuthVault.isValidEmail("a@b.com"))
        assertFalse(AuthVault.isValidEmail("a@b"))
        assertEquals("blue dog", AuthVault.normaliseAnswer("  Blue  Dog "))

        val changed = Instant.parse("2026-01-01T00:00:00Z")
        assertTrue(AuthVault.needsRotation(changed.toString(), changed.plusMillis(181L * 86_400_000)))
        assertTrue(AuthVault.needsRotation(changed.toString(), changed.plusMillis(180L * 86_400_000)))
        assertFalse(AuthVault.needsRotation(changed.toString(), changed.plusMillis(179L * 86_400_000)))
        assertFalse(AuthVault.needsRotation(Instant.now().toString(), Instant.now()))
        assertTrue(AuthVault.needsRotation("yesterday", Instant.now()))

        val packed = AuthVault.pack(SecretHash("s", "h", 3))
        assertEquals("h", AuthVault.unpack(packed)?.hash)
        assertEquals(AuthVault.ITERATIONS, AuthVault.unpack("s:h:0")?.iterations)
        assertNull(AuthVault.unpack("nope"))
        assertEquals(6, AuthVault.questions.size)

        val salt = Base64.getEncoder().encodeToString("0123456789abcdef".toByteArray())
        assertTrue(AuthVault.verify("Abcd1234!", salt, "PRzr6dpMdmEXQTc6VWGADNkMY0/rT9fDWufJB4EOBog=", 210_000))
        assertFalse(AuthVault.verify("nope", salt, "PRzr6dpMdmEXQTc6VWGADNkMY0/rT9fDWufJB4EOBog=", 210_000))

        val hashed = AuthVault.hashSecret("Abcd1234!")
        assertEquals(210_000, hashed.iterations)
        assertTrue(AuthVault.verify("Abcd1234!", hashed.salt, hashed.hash, hashed.iterations))
        assertFalse(AuthVault.verify("nope", hashed.salt, hashed.hash, hashed.iterations))

        val answer = AuthVault.hashSecret(AuthVault.normaliseAnswer("Fluffy"))
        val account = VaultAccount(
            email = "vikas@example.com", firstName = "Vikas", lastName = "Shah",
            passwordSalt = hashed.salt, passwordHash = hashed.hash, passwordIterations = hashed.iterations,
            passwordChangedAt = "2026-01-01T00:00:00Z",
            q1Id = "pet", q1Hash = AuthVault.pack(answer), q2Id = "city", q2Hash = AuthVault.pack(answer),
            createdAt = "2026-01-01T00:00:00Z", panMasked = "ABCDE****F", panForStatements = "ABCDE1234F",
        )
        val shown = AuthVault.toPublic(account)
        assertEquals("Vikas", shown.firstName)
        assertEquals("ABCDE****F", shown.panMasked)
        assertEquals("pet", shown.q1Id)
    }
}
