package `in`.growurdhan.data.vault

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.domain.vault.LegacyRestoreIdentity
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.security.KeyStore
import `in`.growurdhan.core.model.FamilyMember
import `in`.growurdhan.core.model.OwnerId
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Only synthetic owners and credentials. Real family files are NEVER CI fixtures. */
@RunWith(AndroidJUnit4::class)
class LegacyVaultMigrationInstrumentedTest {
    private val email = "fixture@example.test"
    private val password = "Synthetic!Password123"
    private val pan = "ABCDE1234F"
    private val dob = "1990-01-01"

    private fun hash(secret: String): Triple<String, String, Int> {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val spec = PBEKeySpec(secret.toCharArray(), salt, 210_000, 256)
        val result = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
        return Triple(
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(result, Base64.NO_WRAP), 210_000,
        ).also { salt.fill(0); result.fill(0) }
    }
    private fun lockedFixture(
        crossOwner: Boolean = false, addUnsupported: Boolean = false,
    ): ByteArray {
        val pass = hash(password)
        val first = hash("first school")
        val second = hash("blue city")
        val account = JSONObject()
            .put("email", email).put("firstName", "Synthetic")
            .put("lastName", "Owner").put("passwordSalt", pass.first)
            .put("passwordHash", pass.second).put("passwordIterations", pass.third)
            .put("passwordChangedAt", "2026-01-01").put("panForStatements", pan)
            .put("dateOfBirth", dob).put("q1Id", "school")
            .put("q1Hash", "${first.first}:${first.second}:${first.third}")
            .put("q2Id", "city")
            .put("q2Hash", "${second.first}:${second.second}:${second.third}")
            .put("createdAt", "2025-01-01")
        val member = JSONObject().put("id", "self").put("name", "Synthetic Owner")
            .put("relation", "Self").put("color", "#abcdef")
        val custodian = JSONObject().put("id", "acct").put("ownerId", "self")
            .put("accountType", "CAMS").put("custodian", "Synthetic Registrar")
            .put("identifierMasked", "folio ****0001")
        val holding = JSONObject().put("id", "h1").put("ownerId", "self")
            .put("accountId", "acct").put("name", "Synthetic Fund")
            .put("category", "MUTUAL_FUND").put("subCategory", "Equity")
            .put("symbolOrFolio", "****0001")
            .put("quantityScaled", "150000000").put("averageCostPerUnitMinor", "40000")
            .put("currentPriceMinor", "50000").put("lastPriceUpdated", "2026-10-01")
            .put("isPriceStale", true).put("priceSource", "Synthetic valuation")
            .put("details", JSONObject().put("mfPlan", "DIRECT")
                .put("mfOption", "GROWTH"))
            .put("tags", JSONArray().put("Retirement"))
        fun event(id: String, kind: String, supersedes: String? = null): JSONObject =
            JSONObject().put("id", id)
                .put("holdingId", "h1")
                .put("ownerId", if (crossOwner) "unknown" else "self")
                .put("holdingName", "Synthetic Fund").put("eventType", kind)
                .put("eventDate", "2026-01-01")
                .put("quantityScaled", "150000000").put("unitPriceMinor", "40000")
                .put("totalAmountMinor", "60000").put("chargesMinor", "100")
                .put("createdAt", "2026-01-01T00:00:00Z")
                .apply { if (supersedes != null) put("supersedesEventId", supersedes) }
        val root = JSONObject().put("kind", "GUDVAULT").put("version", 2)
            .put("exportedAt", "2026-10-01T00:00:00Z")
            .put("account", account)
            .put("familyMembers", JSONArray().put(member))
            .put("holdings", JSONArray().put(holding))
            .put("accounts", JSONArray().put(custodian))
            // Deliberately reverse correction order to test topological insertion.
            .put("ledgerEvents", JSONArray().put(event("correction", "CORRECTION", "buy"))
                .put(event("buy", "BUY")))
            .put("goals", JSONArray().put(JSONObject().put("id", "goal")
                .put("ownerId", "self").put("name", "Retirement")
                .put("createdAt", "2025-01-01"))) // Original optional target = NULL.
            .put("casLatest", JSONObject().put("fixture", true))
        if (addUnsupported) root.put("unknownRootCollection", JSONArray().put("X"))
        val raw = root.toString().toByteArray()
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val text = ("GrowUrDhan\n" + email + "\n" + password).toCharArray()
        val spec = PBEKeySpec(text, salt, 600_000, 256)
        text.fill('\u0000')
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            ByteBuffer.allocate(40 + raw.size + 16).order(ByteOrder.BIG_ENDIAN)
                .put("GUDVLT01".toByteArray(Charsets.US_ASCII))
                .putInt(600_000).put(salt).put(nonce).put(cipher.doFinal(raw)).array()
        } finally { key.fill(0); raw.fill(0); nonce.fill(0); salt.fill(0) }
    }

    private fun identity(
        passwordValue: String = password, secondAnswer: String = "blue city",
    ) = LegacyRestoreIdentity(
        email, passwordValue.toCharArray(), pan, dob, "school",
        "first school".toCharArray(), "city", secondAnswer.toCharArray(),
    )

    private fun withSql(block: (SQLiteDatabase) -> Unit) {
        System.loadLibrary("sqlcipher")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "m25-" + UUID.randomUUID() + ".db")
        val key = ByteArray(32).also(SecureRandom()::nextBytes)
        val db = SQLiteDatabase.openOrCreateDatabase(file, key, null, null, null)
        try {
            VaultSchema.initializeOrValidate(db)
            block(db)
        } finally {
            db.close()
            key.fill(0)
            SQLiteDatabase.deleteDatabase(file)
        }
    }

    @Test fun verifiedLegacyArchiveRestoresExactIdsAndFinancialAmountsAtomically() {
        val source = lockedFixture()
        val proof = identity()
        val converted = try { LegacyVaultMigration.convertAuthenticated(source, proof) }
            finally { proof.wipeSecrets() }
        try {
            withSql { db ->
                db.beginTransaction()
                try {
                    VaultBackupTables.restoreIntoEmpty(db, converted)
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
                db.rawQuery("SELECT kind,financial_kind,amount_scaled,charges_scaled " +
                    "FROM ledger_event WHERE id='buy'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("BUY", c.getString(0))
                    assertEquals("BUY", c.getString(1))
                    assertEquals(60000L, c.getLong(2))
                    assertEquals(100L, c.getLong(3))
                }
                db.rawQuery("SELECT supersedes_event_id FROM ledger_event " +
                    "WHERE id='correction'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("buy", c.getString(0))
                }
                db.rawQuery("SELECT details_json,tags_json,extra_json " +
                    "FROM holding_profile WHERE holding_id='h1'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("DIRECT", JSONObject(c.getString(0)).getString("mfPlan"))
                    assertEquals("Retirement", JSONArray(c.getString(1)).getString(0))
                    assertEquals("Synthetic Fund", JSONObject(c.getString(2)).getString("name"))
                }
                // The converter must explicitly supply NOT NULL profile fields:
                // native backup rows bind all columns, including unprovided ones.
                db.rawQuery("SELECT base_currency FROM custody_profile WHERE account_id='acct'", null)
                    .use { c ->
                        assertTrue(c.moveToFirst())
                        assertEquals("INR", c.getString(0))
                    }
                db.rawQuery("SELECT target_scaled FROM family_goal WHERE id='goal'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertTrue(c.isNull(0))
                }
                db.rawQuery("SELECT value FROM vault_meta WHERE key='original_cas_latest'", null)
                    .use { c ->
                        assertTrue(c.moveToFirst())
                        assertTrue(JSONObject(c.getString(0)).getBoolean("fixture"))
                    }
                assertThrows(Exception::class.java) {
                    VaultBackupTables.restoreIntoEmpty(db, converted)
                }
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use { it.count })
                val portable = VaultBackupTables.export(db)
                withSql { recovered ->
                    recovered.beginTransaction()
                    try {
                        VaultBackupTables.restoreIntoEmpty(recovered, portable)
                        recovered.setTransactionSuccessful()
                    } finally { recovered.endTransaction() }
                    recovered.rawQuery("SELECT count(*) FROM ledger_event", null).use { c ->
                        assertTrue(c.moveToFirst())
                        assertEquals(2, c.getInt(0))
                    }
                }
            }
        } finally {
            converted.fill(0)
            source.fill(0)
        }
    }

    @Test fun wrongIdentityOrCrossOwnerIsRejectedBeforeAnyFinancialWrite() {
        val data = lockedFixture()
        val wrong = identity(secondAnswer = "incorrect")
        assertThrows(Exception::class.java) {
            LegacyVaultMigration.convertAuthenticated(data, wrong)
        }
        wrong.wipeSecrets()
        val tampered = data.clone()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
        val proof = identity()
        assertThrows(Exception::class.java) {
            LegacyVaultMigration.convertAuthenticated(tampered, proof)
        }
        proof.wipeSecrets()
        val crossOwner = lockedFixture(crossOwner = true)
        val p = identity()
        assertThrows(Exception::class.java) {
            LegacyVaultMigration.convertAuthenticated(crossOwner, p)
        }
        p.wipeSecrets()
        withSql { db ->
            db.rawQuery("SELECT count(*) FROM ledger_event", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
        data.fill(0)
        tampered.fill(0)
        crossOwner.fill(0)
    }

    @Test fun repositoryRefusesToMergeAndKeepsExistingEncryptedBook(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "m25-repository-" + id)
        val alias = "in.growurdhan.test.m25." + id
        val vault = SqlCipherVaultRepository(context, root, alias)
        val archive = lockedFixture()
        try {
            vault.openOrCreate()
            vault.createMember(FamilyMember(OwnerId("existing"), "Existing Synthetic",
                "Self", true))
            val proof = identity()
            assertThrows(Exception::class.java) {
                runBlocking { vault.restoreOriginalLegacyBackup(proof, archive) }
            }
            assertEquals(1, vault.members().size)
            assertEquals("existing", vault.members()[0].id.raw)
            assertEquals(0, vault.events().size)
            assertEquals(0, vault.health().foreignKeyViolations)
        } finally {
            vault.close()
            root.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
            archive.fill(0)
        }
    }

    @Test fun freshDeviceCanRestoreOldArchiveAndThenCreateNativePortableBackup(): Unit =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val id = UUID.randomUUID().toString()
            val root = File(context.noBackupFilesDir, "m25-device-" + id)
            val alias = "in.growurdhan.test.m25." + id
            val vault = SqlCipherVaultRepository(context, root, alias)
            val archive = lockedFixture()
            val backupPassword = "SyntheticNativeBackup!2026".toCharArray()
            try {
                vault.openOrCreate()
                vault.restoreOriginalLegacyBackup(identity(), archive)
                assertEquals(1, vault.members().size)
                assertEquals(1, vault.holdings().size)
                assertEquals(2, vault.events().size)
                val portable = vault.exportEncryptedBackup(backupPassword)
                try {
                    assertTrue(portable.copyOfRange(0, 8)
                        .contentEquals("GUDNBACK".toByteArray()))
                    val plain = VaultBackupCodec.decrypt(portable, backupPassword)
                    try {
                        val original = JSONObject(plain.toString(Charsets.UTF_8))
                        assertEquals(4, original.getInt("format"))
                        assertEquals(1,
                            original.getJSONArray("legacy_account_identity").length())
                        assertEquals(2, original.getJSONArray("ledger_profile").length())
                    } finally { plain.fill(0) }
                } finally { portable.fill(0) }
            } finally {
                backupPassword.fill('\u0000')
                archive.fill(0)
                vault.close()
                root.deleteRecursively()
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
            }
        }

    @Test fun unknownLegacyRootCollectionRejectsRatherThanSilentlyDroppingIt() {
        val data = lockedFixture(addUnsupported = true)
        val p = identity()
        assertThrows(Exception::class.java) {
            LegacyVaultMigration.convertAuthenticated(data, p)
        }
        p.wipeSecrets()
        data.fill(0)
    }
}
