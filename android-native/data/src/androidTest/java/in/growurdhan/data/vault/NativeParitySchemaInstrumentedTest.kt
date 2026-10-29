package `in`.growurdhan.data.vault

import `in`.growurdhan.core.model.FamilyAdminChange
import `in`.growurdhan.core.model.OwnerId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M24 SQLCipher device tests; data are synthetic and no original .kosha import
 * is attempted. Existing native v3 data must survive v4 schema initialization.
 */
@RunWith(AndroidJUnit4::class)
class NativeParitySchemaInstrumentedTest {
    private fun withDatabase(block: (SQLiteDatabase) -> Unit) {
        System.loadLibrary("sqlcipher")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "m24-" + UUID.randomUUID() + ".db")
        val password = ByteArray(32).also(SecureRandom()::nextBytes)
        val db = SQLiteDatabase.openOrCreateDatabase(file, password, null, null, null)
        try {
            block(db)
        } finally {
            db.close()
            password.fill(0)
            SQLiteDatabase.deleteDatabase(file)
        }
    }

    private fun seedMemberAndHolding(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO family_member(id,display_name,relationship,is_primary) " +
            "VALUES('self','Synthetic Owner','Self',1)")
        db.execSQL("INSERT INTO holding(id,owner_id,display_name,asset_category," +
            "quantity_scaled,price_scaled) VALUES('h1','self','Test PPF','FIXED_INCOME',0,10000)")
        db.execSQL("INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date," +
            "quantity_delta_scaled,amount_scaled) VALUES('e1','self','h1','BUY'," +
            "'2026-01-01',100000000,10000)")
    }

    @Test fun nativeV3UpgradesToV4WithoutRewritingExistingLedger() = withDatabase { db ->
        db.execSQL("PRAGMA foreign_keys = ON")
        db.beginTransaction()
        try {
            VaultSchema.legacyV1Statements.forEach { db.execSQL(it) }
            VaultSchema.migrationToV2.forEach { db.execSQL(it) }
            VaultSchema.migrationToV3.forEach { db.execSQL(it) }
            db.execSQL("PRAGMA user_version = 3")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        seedMemberAndHolding(db)
        val before = db.rawQuery(
            "SELECT kind,event_date,quantity_delta_scaled,amount_scaled FROM ledger_event WHERE id='e1'", null,
        ).use { c -> assertTrue(c.moveToFirst()); (0..3).map { c.getString(it) } }
        VaultSchema.initializeOrValidate(db)
        db.rawQuery("PRAGMA user_version", null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(VaultSchema.VERSION, c.getInt(0))
        }
        val after = db.rawQuery(
            "SELECT kind,event_date,quantity_delta_scaled,amount_scaled FROM ledger_event WHERE id='e1'", null,
        ).use { c -> assertTrue(c.moveToFirst()); (0..3).map { c.getString(it) } }
        assertEquals(before, after)
        assertThrows(Exception::class.java) { db.execSQL("DELETE FROM ledger_event WHERE id='e1'") }
        db.execSQL("INSERT INTO family_profile(owner_id,entity_type) VALUES('self','INDIVIDUAL')")
        assertThrows(Exception::class.java) {
            db.execSQL("INSERT INTO family_profile(owner_id) VALUES('stranger')")
        }
    }

    @Test fun editingImportedNonHufMemberCannotEraseOriginalLegalEntityClass() =
        withDatabase { db ->
            VaultSchema.initializeOrValidate(db)
            db.execSQL("INSERT INTO family_member(id,display_name,relationship,is_primary) " +
                "VALUES('trust','Synthetic Trust','Joint',0)")
            db.execSQL("INSERT INTO family_profile(owner_id,entity_type) " +
                "VALUES('trust','PRIVATE_TRUST')")
            db.beginTransaction()
            try {
                FamilyAdministrationSql.apply(db, FamilyAdminChange.Member(
                    OwnerId("trust"),"Renamed Synthetic Trust","Joint",null,null,
                    null,"Testing source identity preservation",null,
                ))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.rawQuery("SELECT entity_type FROM family_profile WHERE owner_id='trust'",null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("PRIVATE_TRUST",c.getString(0))
            }
            assertEquals(0,db.rawQuery("PRAGMA foreign_key_check",null).use { it.count })
        }

    @Test fun existingV4HufOwnerTypeIsReconciledWithoutLedgerMutation() = withDatabase { db ->
        VaultSchema.initializeOrValidate(db)
        seedMemberAndHolding(db)
        db.execSQL("INSERT INTO family_member(id,display_name,relationship,is_primary) " +
            "VALUES('huf-legacy','Synthetic HUF','HUF',0)")
        db.execSQL("INSERT INTO family_profile(owner_id,entity_type) " +
            "VALUES('huf-legacy','INDIVIDUAL')")
        db.execSQL("INSERT INTO family_member(id,display_name,relationship,is_primary) " +
            "VALUES('huf-without-profile','Unprofiled HUF','HUF',0)")
        repeat(2) { FamilyAdministrationSql.alignHufEntities(db) }
        db.rawQuery("SELECT COUNT(*) FROM family_profile WHERE entity_type='HUF'",null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2,c.getInt(0))
        }
        db.rawQuery("SELECT kind,quantity_delta_scaled FROM ledger_event WHERE id='e1'",null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("BUY",c.getString(0))
            assertEquals(100000000L,c.getLong(1))
        }
        assertEquals(0,db.rawQuery("PRAGMA foreign_key_check",null).use { it.count })
    }

    @Test fun v4LogicalBackupRoundTripsUnknownCostAndRichRecords() = withDatabase { source ->
        VaultSchema.initializeOrValidate(source)
        seedMemberAndHolding(source)
        source.execSQL("INSERT INTO family_profile(owner_id,entity_type,date_of_birth) " +
            "VALUES('self','INDIVIDUAL','1990-01-01')")
        source.execSQL("INSERT INTO holding_profile(holding_id,owner_id,details_json,cost_unknown) " +
            "VALUES('h1','self','{\"npsTier\":\"I\",\"mfPlan\":\"DIRECT\"}',1)")
        source.execSQL("INSERT INTO family_goal(id,owner_id,name,target_scaled,created_at) " +
            "VALUES('g1','self','Retirement',250000000,'2026-10-10')")
        source.execSQL("INSERT INTO ledger_profile(event_id,owner_id,holding_id,source_ref) " +
            "VALUES('e1','self','h1','synthetic')")
        val portable = VaultBackupTables.export(source)
        withDatabase { restored ->
            VaultSchema.initializeOrValidate(restored)
            restored.beginTransaction()
            try {
                VaultBackupTables.restoreIntoEmpty(restored, portable)
                restored.setTransactionSuccessful()
            } finally {
                restored.endTransaction()
            }
            restored.rawQuery(
                "SELECT details_json,average_cost_per_unit_scaled,cost_unknown " +
                    "FROM holding_profile WHERE holding_id='h1'", null,
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("{\"npsTier\":\"I\",\"mfPlan\":\"DIRECT\"}", c.getString(0))
                assertTrue(c.isNull(1))
                assertEquals(1, c.getInt(2))
            }
            restored.rawQuery("SELECT target_scaled FROM family_goal WHERE id='g1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(250000000L, c.getLong(0))
            }
            assertThrows(Exception::class.java) {
                restored.execSQL("UPDATE ledger_profile SET source_ref='forged' WHERE event_id='e1'")
            }
            assertThrows(Exception::class.java) {
                VaultBackupTables.restoreIntoEmpty(restored, portable)
            }
        }
    }
    @Test fun previousNativeV3PortableBackupRestoresWithoutInventingMetadata() = withDatabase { original ->
        VaultSchema.initializeOrValidate(original)
        seedMemberAndHolding(original)
        val older = JSONObject(VaultBackupTables.export(original).toString(Charsets.UTF_8))
        older.put("format", 3).put("databaseVersion", 3)
        val extensionNames = listOf(
            "family_profile", "custody_profile", "instrument_profile", "holding_profile",
            "ledger_profile", "family_goal", "holding_goal", "import_batch_profile",
            "source_document_profile", "statement_snapshot_profile",
            "statement_position_profile", "price_observation_profile", "tax_rule_profile",
            "liability_profile", "legacy_account_identity", "legacy_field_extension",
            "listed_security_profile", "egress_log_profile", "gmail_seen_profile",
            "setting_profile",
        )
        extensionNames.forEach(older::remove)
        withDatabase { restored ->
            VaultSchema.initializeOrValidate(restored)
            restored.beginTransaction()
            try {
                VaultBackupTables.restoreIntoEmpty(restored, older.toString().toByteArray(Charsets.UTF_8))
                restored.setTransactionSuccessful()
            } finally {
                restored.endTransaction()
            }
            restored.rawQuery("SELECT count(*) FROM ledger_event", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            restored.rawQuery("SELECT count(*) FROM holding_profile", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test fun v4LedgerProfileGainsLegacyColumnsWithoutRewritingEvents() {
        System.loadLibrary("sqlcipher")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "m24-v5-" + UUID.randomUUID() + ".db")
        val password = ByteArray(32).also(SecureRandom()::nextBytes)
        fun open() = SQLiteDatabase.openOrCreateDatabase(file, password, null, null, null)
        val created = open()
        val before = try {
            created.execSQL("PRAGMA foreign_keys = ON")
            created.beginTransaction()
            try {
                VaultSchema.legacyV1Statements.forEach { created.execSQL(it) }
                VaultSchema.migrationToV2.forEach { created.execSQL(it) }
                VaultSchema.migrationToV3.forEach { created.execSQL(it) }
                VaultSchema.migrationToV4.forEach { created.execSQL(it) }
                created.execSQL("PRAGMA user_version = 4")
                created.setTransactionSuccessful()
            } finally {
                created.endTransaction()
            }
            seedMemberAndHolding(created)
            created.execSQL(
                "INSERT INTO ledger_profile(event_id,owner_id,holding_id,source_ref) " +
                    "VALUES('e1','self','h1','synthetic-ref')",
            )
            created.rawQuery(
                "SELECT kind,event_date,quantity_delta_scaled,amount_scaled FROM ledger_event WHERE id='e1'",
                null,
            ).use { c -> assertTrue(c.moveToFirst()); (0..3).map { c.getString(it) } }
        } finally {
            created.close()
        }
        val upgraded = open()
        try {
            VaultSchema.initializeOrValidate(upgraded)
            upgraded.rawQuery("PRAGMA user_version", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(VaultSchema.VERSION, c.getInt(0))
            }
            val after = upgraded.rawQuery(
                "SELECT kind,event_date,quantity_delta_scaled,amount_scaled FROM ledger_event WHERE id='e1'",
                null,
            ).use { c -> assertTrue(c.moveToFirst()); (0..3).map { c.getString(it) } }
            assertEquals(before, after)
            upgraded.rawQuery(
                "SELECT source_ref,settlement_date,unit_balance_after,description,entered_by " +
                    "FROM ledger_profile WHERE event_id='e1'",
                null,
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("synthetic-ref", c.getString(0))
                assertTrue(c.isNull(1))
                assertTrue(c.isNull(2))
                assertTrue(c.isNull(3))
                assertEquals("owner", c.getString(4))
            }
            assertThrows(Exception::class.java) {
                upgraded.execSQL("UPDATE ledger_event SET amount_scaled=1 WHERE id='e1'")
            }
            upgraded.execSQL(
                "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date," +
                    "quantity_delta_scaled,amount_scaled) VALUES('early','self','h1','BUY'," +
                    "'2026-01-15',1,1)",
            )
            assertThrows(Exception::class.java) {
                upgraded.execSQL(
                    "INSERT INTO ledger_profile(event_id,owner_id,holding_id,settlement_date) " +
                        "VALUES('early','self','h1','2020-01-01')",
                )
            }
        } finally {
            upgraded.close()
        }
        val reopened = open()
        try {
            VaultSchema.initializeOrValidate(reopened)
            reopened.rawQuery(
                "SELECT source_ref,entered_by FROM ledger_profile WHERE event_id='e1'", null,
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("synthetic-ref", c.getString(0))
                assertEquals("owner", c.getString(1))
            }
            reopened.execSQL(
                "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date," +
                    "quantity_delta_scaled,amount_scaled) VALUES('e2','self','h1','BUY'," +
                    "'2026-02-01',100000000,20000)",
            )
            reopened.execSQL(
                "INSERT INTO ledger_profile(event_id,owner_id,holding_id,source_ref," +
                    "settlement_date,unit_balance_after,description) VALUES(" +
                    "'e2','self','h1','second','2026-02-03',200000000,'Synthetic settlement')",
            )
            assertThrows(Exception::class.java) {
                reopened.execSQL(
                    "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date," +
                        "quantity_delta_scaled,amount_scaled) VALUES('e3','self','h1','BUY'," +
                        "'2026-03-01',1,1)",
                )
                reopened.execSQL(
                    "INSERT INTO ledger_profile(event_id,owner_id,holding_id,settlement_date) " +
                        "VALUES('e3','self','h1','2026-02-01')",
                )
            }
            reopened.execSQL(
                "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date," +
                    "quantity_delta_scaled,amount_scaled) VALUES('e4','self','h1','BUY'," +
                    "'2026-04-01',1,1)",
            )
            assertThrows(Exception::class.java) {
                reopened.execSQL(
                    "INSERT INTO ledger_profile(event_id,owner_id,holding_id,entered_by) " +
                        "VALUES('e4','self','h1','clerk')",
                )
            }
            val portable = VaultBackupTables.export(reopened)
            val oldShape = JSONObject(portable.toString(Charsets.UTF_8))
            oldShape.put("databaseVersion", 4)
            val profiles = oldShape.getJSONArray("ledger_profile")
            for (i in 0 until profiles.length()) {
                val row = profiles.getJSONArray(i)
                val short = org.json.JSONArray()
                for (j in 0 until row.length() - 4) short.put(row.get(j))
                profiles.put(i, short)
            }
            withDatabase { restored ->
                VaultSchema.initializeOrValidate(restored)
                restored.beginTransaction()
                try {
                    VaultBackupTables.restoreIntoEmpty(
                        restored, portable,
                    )
                    restored.setTransactionSuccessful()
                } finally {
                    restored.endTransaction()
                }
                restored.rawQuery(
                    "SELECT settlement_date,unit_balance_after,description,entered_by " +
                        "FROM ledger_profile WHERE event_id='e2'",
                    null,
                ).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("2026-02-03", c.getString(0))
                    assertEquals(200000000L, c.getLong(1))
                    assertEquals("Synthetic settlement", c.getString(2))
                    assertEquals("owner", c.getString(3))
                }
                restored.rawQuery(
                    "SELECT kind,amount_scaled FROM ledger_event WHERE id='e1'", null,
                ).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("BUY", c.getString(0))
                    assertEquals(10000L, c.getLong(1))
                }
            }
            withDatabase { restoredOld ->
                VaultSchema.initializeOrValidate(restoredOld)
                restoredOld.beginTransaction()
                try {
                    VaultBackupTables.restoreIntoEmpty(
                        restoredOld, oldShape.toString().toByteArray(Charsets.UTF_8),
                    )
                    restoredOld.setTransactionSuccessful()
                } finally {
                    restoredOld.endTransaction()
                }
                restoredOld.rawQuery(
                    "SELECT source_ref,settlement_date,unit_balance_after,description,entered_by " +
                        "FROM ledger_profile WHERE event_id='e1'",
                    null,
                ).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("synthetic-ref", c.getString(0))
                    assertTrue(c.isNull(1))
                    assertTrue(c.isNull(2))
                    assertTrue(c.isNull(3))
                    assertEquals("owner", c.getString(4))
                }
            }
        } finally {
            reopened.close()
            password.fill(0)
            SQLiteDatabase.deleteDatabase(file)
        }
    }

}
