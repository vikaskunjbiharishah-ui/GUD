package `in`.growurdhan.domain.finance

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts backup section. */
class BackupFileTest {
    @Test fun lockedBackupHidesTheBookAndChecksIdentity() {
        val password = AuthVault.hashSecret("Abcd1234!")
        val fluffy = AuthVault.hashSecret(AuthVault.normaliseAnswer("Fluffy"))
        val pune = AuthVault.hashSecret(AuthVault.normaliseAnswer("Pune"))
        val json = """
            {"kind":"GUDVAULT","version":2,"exportedAt":"2026-09-19T00:00:00.000Z",
            "account":{"email":"vikas@example.com","firstName":"Vikas","lastName":"Shah",
            "passwordSalt":"${password.salt}","passwordHash":"${password.hash}","passwordIterations":${password.iterations},
            "passwordChangedAt":"2026-01-01T00:00:00.000Z","panMasked":"ABCDE****F","panForStatements":"ABCDE1234F",
            "dateOfBirth":"1980-04-01","q1Id":"pet","q1Hash":"${AuthVault.pack(fluffy)}",
            "q2Id":"city","q2Hash":"${AuthVault.pack(pune)}","createdAt":"2026-01-01T00:00:00.000Z"},
            "familyMembers":[],"holdings":[{"id":"h1","name":"Secret Fund","ownerId":"f1"}],
            "ledgerEvents":[],"accounts":[],"goals":[],"casLatest":null}
        """.trimIndent().replace("\n", "")
        val packed = BackupFile.pack(json, "vikas@example.com", "Abcd1234!")
        assertTrue(BackupFile.isBinary(packed))
        val dump = packed.toString(Charsets.UTF_8)
        assertFalse(dump.contains("Secret Fund"))
        assertFalse(dump.contains("ABCDE1234F"))
        assertFalse(dump.contains("vikas@example.com"))
        assertFalse(dump.trimStart().startsWith("{"))
        assertFalse(BackupFile.looksLikePlainJson(dump))
        val unlocked = assertThrows(BackupException::class.java) {
            BackupFile.unpack(json.toByteArray(), "vikas@example.com", "Abcd1234!")
        }
        assertTrue(unlocked.message!!.contains("unlocked backup", ignoreCase = true))

        val opened = BackupFile.unpack(packed, "vikas@example.com", "Abcd1234!")
        assertTrue(opened.contains("Secret Fund"))
        assertThrows(BackupException::class.java) { BackupFile.unpack(packed, "vikas@example.com", "WrongPass1!") }
        assertThrows(BackupException::class.java) { BackupFile.unpack(packed, "other@example.com", "Abcd1234!") }

        assertNull(BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", strict = false)))
        assertNull(BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", strict = false, pan = "XXXXX1234X")))
        assertEquals(
            "PAN does not match this backup.",
            BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", true, "XXXXX1234X", "1980-04-01", "pet", "Fluffy", "city", "Pune")),
        )
        assertEquals(
            "Date of birth does not match this backup.",
            BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", true, "ABCDE1234F", "1990-01-01", "pet", "Fluffy", "city", "Pune")),
        )
        assertEquals(
            "Security answers do not match this backup.",
            BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", true, "ABCDE1234F", "1980-04-01", "pet", "Wrong", "city", "Pune")),
        )
        assertNull(BackupFile.verifyIdentity(opened, RestoreIdentity("vikas@example.com", "Abcd1234!", true, "ABCDE1234F", "1980-04-01", "pet", "fluffy", "city", "Pune")))

        val whenSaved = Instant.parse("2026-09-19T00:00:00.000Z")
        assertEquals("growurdhan_2026-09-19.kosha", BackupFile.suggestedName(whenSaved))
        assertEquals("growurdhan_20260919_000000.kosha", BackupFile.suggestedScheduledName(whenSaved))

        val legacyInner = """{"kind":"GUDVAULT","account":{"email":"vikas@example.com","passwordSalt":"s","passwordHash":"h","passwordIterations":1,"q1Id":"pet","q1Hash":"a:b:1","q2Id":"city","q2Hash":"a:b:1"},"holdings":[{"name":"Old Fund"}]}"""
        val sealed = VaultSeal.encrypt(legacyInner, "Abcd1234!")
        val legacy = """{"magic":"KOSHA1","iterations":600000,"saltB64":"${sealed.salt}","ivB64":"${sealed.iv}","cipherB64":"${sealed.cipher}"}"""
        assertTrue(BackupFile.unpack(legacy.toByteArray(), "ignored@example.com", "Abcd1234!").contains("Old Fund"))
    }
}
