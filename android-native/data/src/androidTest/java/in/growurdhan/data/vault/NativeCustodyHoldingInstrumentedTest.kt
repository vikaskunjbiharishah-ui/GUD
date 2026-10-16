package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.*
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Fully synthetic encrypted-vault tests. No user accounts or financial documents. */
@RunWith(AndroidJUnit4::class)
class NativeCustodyHoldingInstrumentedTest {
    private lateinit var context: Context
    private lateinit var folder: File
    private lateinit var alias: String
    private lateinit var vault: SqlCipherVaultRepository
    private val owner = OwnerId("self")
    private val spouse = OwnerId("spouse")

    @Before fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "cas-onboarding-$suffix")
        alias = "in.growurdhan.test.m7.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    private suspend fun seed(): CustodyAccount {
        vault.openOrCreate()
        vault.registerFamilyMember("Synthetic Primary", "Self")
        val actualOwner = vault.members().single().id
        return vault.registerCustodyAccount(actualOwner, "CAMS", "Synthetic registrar")
    }

    @Test fun allOriginalHoldingDetailKeysPersistWithTheirHistoricalValueTypes(): Unit = runBlocking {
        val registered = seed()
        val holding = vault.registerDetailedAsset(
            registered.ownerId, "Synthetic multi-field fixture",
            "SPECIALISED", "AIF",
        )
        val fields = JSONObject()
        for (key in ReactV9HoldingDetailsContract.fields) {
            val value: Any = when (key) {
                "tdsApplicable" -> true
                "areaSqFt", "ownershipPercent", "originalTenorMonths", "incomeDay" -> 1
                "annualContributionMinor", "faceValueMinor", "commitmentMinor",
                "capitalCalledMinor", "monthlyIncomeMinor" -> "10000"
                else -> "synthetic"
            }
            fields.put(key, value)
        }
        vault.updateHoldingProfile(HoldingProfileEdit(
            holding.id, registered.ownerId, holding.name,
            "AIF", fields.toString(),
        ))
        vault.close()
        vault.openOrCreate()
        val restored = JSONObject(vault.holdingProfiles().single().detailsJson)
        assertEquals(ReactV9HoldingDetailsContract.fields.size, restored.length())
        for (key in ReactV9HoldingDetailsContract.fields) {
            assertTrue("Missing original v9 key " + key, restored.has(key))
            assertEquals(fields.get(key), restored.get(key))
        }
        assertNull(vault.holdingProfiles().single().verifiedCostPerUnit)
        assertTrue(vault.events().isEmpty())
    }

    @Test fun originalHoldingFinancialMetadataAndTagsSurviveEncryptedReopen(): Unit = runBlocking {
        val account = seed()
        val holding = vault.registerDetailedAsset(account.ownerId,
            "Synthetic housing loan", "LIABILITY", "Home loan")
        val rich = JSONObject()
            .put("interest_rate_bps", 825)
            .put("monthly_emi_scaled", 2250000)
            .put("remaining_emi_count", 120)
            .put("emi_status", "Paying")
            .put("institution_name", "Synthetic Bank")
            .put("maturity_date", "2040-10-01")
            .put("tags_json", "[\"Home\",\"Family\"]")
        vault.updateHoldingProfile(HoldingProfileEdit(
            holdingId = holding.id, ownerId = account.ownerId,
            name = holding.name, subCategory = "Home loan",
            richMetadataJson = rich.toString(),
        ))
        vault.close()
        vault.openOrCreate()
        val saved = JSONObject(vault.holdingProfiles().single().richMetadataJson)
        assertEquals(825L, saved.getLong("interest_rate_bps"))
        assertEquals(2250000L, saved.getLong("monthly_emi_scaled"))
        assertEquals("Paying", saved.getString("emi_status"))
        assertEquals("[\"Home\",\"Family\"]", saved.getString("tags_json"))
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
        assertThrows(Exception::class.java) {
            runBlocking {
                vault.updateHoldingProfile(HoldingProfileEdit(
                    holdingId = holding.id, ownerId = account.ownerId,
                    name = holding.name,
                    richMetadataJson = """{"account_number_masked":"1234567890"}""",
                ))
            }
        }
    }

    @Test fun newManualCustodyLinkRejectsCrossOwnerAndRegistrarBypass(): Unit = runBlocking {
        val account = seed()
        val asset = vault.registerDetailedAsset(
            account.ownerId, "Synthetic custody share", "EQUITY", "Large cap", account.id,
        )
        assertEquals(account.id, asset.accountId)
        assertEquals(account.id, vault.holdings().single().accountId)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerDetailedAsset(OwnerId("not-an-owner"),
                    "Spoofed", "EQUITY", "Large cap", account.id)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerDetailedAsset(account.ownerId,
                    "Fake registrar", "MUTUAL_FUND", "Equity fund", account.id)
            }
        }
        assertEquals(1, vault.holdings().size)
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun portableBackupRoundTripPreservesEditedOriginalHoldingFields(): Unit = runBlocking {
        val account = seed()
        val holding = vault.registerDetailedAsset(account.ownerId,
            "Synthetic portfolio property", "REAL_ESTATE", "Residential")
        vault.updateHoldingProfile(HoldingProfileEdit(
            holdingId = holding.id, ownerId = account.ownerId,
            name = "Updated synthetic property", subCategory = "Residential",
            detailsJson = """{"ownershipPercent":50,"areaSqFt":1000}""",
            notes = "Synthetic fixture", location = "India",
            instrumentMetadataJson = """{"advisor":"Synthetic property consultant","exchange":"BSE"}""",
            richMetadataJson = """{"benchmark_price_scaled":50000000,"tags_json":"[\"House\"]"}""",
            archived = true,
        ))
        val source = vault.holdingProfiles().single()
        val pass = "synthetic encrypted backup passphrase".toCharArray()
        val archive = vault.exportEncryptedBackup(pass)
        val restoreRoot = File(context.noBackupFilesDir, "m27-restore-" + UUID.randomUUID())
        val restoreAlias = "in.growurdhan.restore.m27." + UUID.randomUUID()
        val target = SqlCipherVaultRepository(context, restoreRoot, restoreAlias)
        try {
            target.openOrCreate()
            target.restoreEncryptedBackup(pass, archive)
            assertEquals(source, target.holdingProfiles().single())
            assertEquals(vault.readBook(), target.readBook())
            assertEquals(0, target.health().foreignKeyViolations)
        } finally {
            pass.fill('\u0000')
            archive.fill(0)
            target.close()
            restoreRoot.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(restoreAlias)
        }
    }

    @Test fun canonicalSubtypeAndHoldingProfileEditsAreEncryptedAndReversible(): Unit = runBlocking {
        val account = seed()
        val asset = vault.registerDetailedAsset(
            account.ownerId, "Synthetic SGB", "GOLD_SGB", "Sovereign Gold Bond",
        )
        assertEquals(0L, asset.quantity.raw)
        assertTrue(vault.events().isEmpty())
        assertEquals("Sovereign Gold Bond", vault.holdingProfiles().single().subCategory)
        assertNull(vault.holdingProfiles().single().verifiedCostPerUnit)
        vault.updateHoldingProfile(HoldingProfileEdit(
            asset.id, account.ownerId, "Synthetic Sovereign Gold Bond",
            "Sovereign Gold Bond", """{"goldHoldMode":"DEMAT","goldPurity":"24K"}""",
            "synthetic note", "India", "SGB-TEST", null, "2026-08-01",
            MoneyScaled(750_000), "2026-10-10", "USER_ENTERED",
            MoneyScaled(600_000), true,
        ))
        val saved = vault.holdingProfiles().single()
        assertEquals("Synthetic Sovereign Gold Bond", saved.name)
        assertTrue(saved.archived)
        assertEquals(600_000L, saved.verifiedCostPerUnit?.raw)
        assertEquals("USER_ENTERED", saved.priceSource)
        assertEquals("DEMAT", JSONObject(saved.detailsJson).getString("goldHoldMode"))
        assertEquals(750_000L, vault.holdings().single().currentUnitPrice.raw)
        assertTrue(vault.events().isEmpty())
        vault.close()
        vault.openOrCreate()
        assertTrue(vault.holdingProfiles().single().archived)
        vault.updateHoldingProfile(HoldingProfileEdit(
            asset.id, account.ownerId, "Synthetic Sovereign Gold Bond",
            "Sovereign Gold Bond", "{}", archived = false,
        ))
        val restored = vault.holdingProfiles().single()
        assertFalse(restored.archived)
        assertEquals("DEMAT", JSONObject(restored.detailsJson).getString("goldHoldMode"))
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun subtypeConflictAndCrossOwnerProfileChangeRollbackWithoutLedgerMutation(): Unit = runBlocking {
        val account = seed()
        val holding = vault.registerDetailedAsset(
            account.ownerId, "Synthetic PPF", "FIXED_INCOME", "PPF",
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.registerDetailedAsset(account.ownerId,
                "Invalid", "LIABILITY", "NPS_TIER_I") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                holding.id, OwnerId("another-owner"), "Spoofed owner")) }
        }
        assertThrows(Exception::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                holding.id, account.ownerId, "Bad details", "PPF", """{"fakeField":"unsafe"}""")) }
        }
        assertEquals(1, vault.holdings().size)
        assertEquals("Synthetic PPF", vault.holdings().single().name)
        assertTrue(vault.events().isEmpty())
    }

    @Test fun manualRetirementAssetIsZeroValueAndRestorableWithoutCasIdentity(): Unit = runBlocking {
        val account = seed()
        val asset = vault.registerManualAsset(account.ownerId, "Synthetic NPS Tier I", "NPS")
        assertEquals(0, asset.quantity.raw)
        assertEquals(0, asset.currentUnitPrice.raw)
        assertTrue(vault.casIdentities().isEmpty())
        assertTrue(vault.events().isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerManualAsset(account.ownerId, " synthetic nps tier i ", "NPS")
            }
        }
        vault.close()
        vault.openOrCreate()
        assertEquals(asset, vault.holdings().single())
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun accountAndEnrolledHoldingPersistAcrossEncryptedReopen(): Unit = runBlocking {
        val account = seed()
        val holding = vault.registerCasHolding(
            account.ownerId, account.id, "Synthetic Index", "INF000000001", "FOL123",
        )
        assertEquals(0L, holding.quantity.raw)
        assertEquals(0L, holding.currentUnitPrice.raw)
        assertEquals("MUTUAL_FUND", holding.assetCategory)
        assertTrue(vault.events().isEmpty())
        assertEquals(account, vault.accounts().single())
        assertEquals(holding.id, vault.casIdentities().single().holdingId)
        vault.close()
        assertEquals(VaultSchema.VERSION, vault.openOrCreate().schemaVersion)
        assertEquals(account, vault.accounts().single())
        assertEquals(holding, vault.holdings().single())
        assertEquals("INF000000001", vault.casIdentities().single().isin)
        assertEquals("FOL123", vault.casIdentities().single().folio)
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun wrongOwnerAndMissingAccountCannotEnrolOrWritePartialHolding(): Unit = runBlocking {
        val account = seed()
        vault.createMember(FamilyMember(spouse, "Synthetic Spouse", "Spouse", false))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerCasHolding(spouse, account.id, "Synthetic", "INF000000001", "123")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerCasHolding(owner, "not-a-registrar", "Synthetic", "INF000000001", "123")
            }
        }
        assertTrue(vault.holdings().isEmpty())
        assertTrue(vault.casIdentities().isEmpty())
    }

    @Test fun duplicateIdentityDoesNotLeaveSecondHoldingOrFakePurchase(): Unit = runBlocking {
        val account = seed()
        vault.registerCasHolding(account.ownerId, account.id, "Synthetic", "INF000000001", "123")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerCasHolding(account.ownerId, account.id, "Duplicate", "INF000000001", "123")
            }
        }
        assertEquals(1, vault.holdings().size)
        assertEquals(1, vault.casIdentities().size)
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun invalidIsinFolioAndRegistrarAccountCannotMutateVault(): Unit = runBlocking {
        val account = seed()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerCasHolding(account.ownerId, account.id, "Synthetic", "NOT-ISIN", "123")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerCasHolding(account.ownerId, account.id, "Synthetic", "INF000000001", "a b")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.registerCustodyAccount(account.ownerId, "OTHER", "Bad") }
        }
        assertEquals(1, vault.accounts().size)
        assertTrue(vault.holdings().isEmpty())
        assertTrue(vault.casIdentities().isEmpty())
    }

    @Test fun portableV3BackupRestoresExactEnrolledCasIdentity(): Unit = runBlocking {
        val account = seed()
        vault.registerCasHolding(account.ownerId, account.id, "Synthetic", "INF000000001", "123")
        val password = "synthetic portable backup password".toCharArray()
        val bytes = vault.exportEncryptedBackup(password)
        val restoreRoot = File(context.noBackupFilesDir, "m7-restore-" + UUID.randomUUID())
        val restoreAlias = "in.growurdhan.restore.m7." + UUID.randomUUID()
        val target = SqlCipherVaultRepository(context, restoreRoot, restoreAlias)
        try {
            target.openOrCreate()
            target.restoreEncryptedBackup(password, bytes)
            assertEquals(vault.readBook(), target.readBook())
            assertEquals(1, target.casIdentities().size)
            assertEquals(0, target.health().foreignKeyViolations)
        } finally {
            password.fill('\u0000')
            target.close()
            restoreRoot.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(restoreAlias)
        }
    }

    @Test fun legacyV2LogicalBackupRestoresWithoutInventingCasIdentity(): Unit = runBlocking {
        val account = seed()
        val pass = "synthetic legacy format password".toCharArray()
        val bytes = vault.exportEncryptedBackup(pass)
        val payload = JSONObject(VaultBackupCodec.decrypt(bytes, pass).toString(Charsets.UTF_8))
        payload.put("format", 2)
        payload.put("databaseVersion", 2)
        payload.remove("holding_cas_identity")
        val oldBytes = VaultBackupCodec.encrypt(payload.toString().toByteArray(Charsets.UTF_8), pass)
        val root = File(context.noBackupFilesDir, "m7-v2-restore-" + UUID.randomUUID())
        val aliasRestore = "in.growurdhan.v2.restore." + UUID.randomUUID()
        val target = SqlCipherVaultRepository(context, root, aliasRestore)
        try {
            target.openOrCreate()
            target.restoreEncryptedBackup(pass, oldBytes)
            assertEquals(listOf(account), target.accounts())
            assertTrue(target.casIdentities().isEmpty())
            assertEquals(0, target.health().foreignKeyViolations)
        } finally {
            pass.fill('\u0000')
            target.close()
            root.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(aliasRestore)
        }
    }

    @Test fun realSqlCipherV2UpgradesToCurrentSchemaWithoutModifyingExistingFinancialRows(): Unit = runBlocking {
        assertTrue(folder.mkdirs())
        val key = VaultKeyEnvelope(File(folder, "wrapped.key"), alias)
            .unwrapOrCreate(allowCreate = true)
        System.loadLibrary("sqlcipher")
        val db = SQLiteDatabase.openOrCreateDatabase(
            File(folder, "wealth.db"), key, null, null, null,
        )
        try {
            db.execSQL("PRAGMA foreign_keys=ON")
            for (sql in VaultSchema.legacyV1Statements) db.execSQL(sql)
            for (sql in VaultSchema.migrationToV2) db.execSQL(sql)
            db.execSQL("PRAGMA user_version=2")
            db.execSQL(
                "INSERT INTO family_member(id,display_name,relationship,is_primary) " +
                    "VALUES('self','Old synthetic owner','Self',1)"
            )
            db.execSQL(
                "INSERT INTO holding(id,owner_id,display_name,asset_category,quantity_scaled,price_scaled) " +
                    "VALUES('legacy','self','Old synthetic fund','MUTUAL_FUND',0,0)"
            )
            db.execSQL(
                "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date,quantity_delta_scaled,amount_scaled) " +
                    "VALUES('evt','self','legacy','BUY','2026-01-01',100000000,10000)"
            )
        } finally {
            db.close()
            key.fill(0)
        }
        File(folder, "ready").writeText("GUDREADY02")
        assertEquals(VaultSchema.VERSION, vault.openOrCreate().schemaVersion)
        assertEquals("evt", vault.events().single().id)
        assertTrue(vault.casIdentities().isEmpty())
        assertEquals("Old synthetic fund", vault.holdings().single().name)
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun fullyDetailedAssetAndOwnerGoalAreInsertedInOneEncryptedTransaction(): Unit = runBlocking {
        val registrar = seed()
        vault.applyFamilyAdministration(FamilyAdminChange.Goal(
            id = null, ownerId = registrar.ownerId, name = "Synthetic retirement",
            target = null, targetDate = null, priority = null,
        ))
        val goal = vault.familyAdministration().goals.single()
        val details = JSONObject()
            .put("npsTier", "I").put("npsCra", "CAMS")
            .put("npsPfm", "Synthetic Pension Manager")
            .put("annualContributionMinor", "1250000")
        val instruments = JSONObject().put("amc", "Synthetic AMC")
            .put("rta", "Synthetic RTA").put("amfiCode", "123")
            .put("advisor", "Synthetic advisor").put("schemeType", "HYBRID")
            .put("ucc", "MASKED").put("exchange", "NSE")
            .put("dpName", "Synthetic Depository")
        val registered = vault.registerDetailedAsset(
            registrar.ownerId, "Synthetic NPS", "FIXED_INCOME", "NPS",
            initialProfile = HoldingProfileEdit(
                holdingId = HoldingId("draft"), ownerId = registrar.ownerId,
                name = "Synthetic NPS", subCategory = "NPS",
                detailsJson = details.toString(),
                instrumentMetadataJson = instruments.toString(),
                notes = "No fabricated purchase", acquiredOn = "2025-04-01",
            ),
            goalIds = listOf(goal.id),
        )
        assertTrue(vault.events().isEmpty())
        assertEquals(0L, registered.quantity.raw)
        val saved = vault.holdingProfiles().single { it.holdingId == registered.id }
        assertEquals("NPS", saved.subCategory)
        assertEquals("I", JSONObject(saved.detailsJson).getString("npsTier"))
        assertEquals("Synthetic Depository",
            JSONObject(saved.instrumentMetadataJson).getString("dpName"))
        assertTrue(vault.familyAdministration().goals.single().holdingIds.contains(registered.id))
        vault.close()
        vault.openOrCreate()
        val reopened = vault.holdingProfiles().single { it.holdingId == registered.id }
        assertEquals(JSONObject(saved.detailsJson).toString(), JSONObject(reopened.detailsJson).toString())
        assertEquals(instruments.toString(), JSONObject(reopened.instrumentMetadataJson).toString())
        assertTrue(vault.familyAdministration().goals.single().holdingIds.contains(registered.id))
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun invalidGoalOrInstrumentFieldAbortsEntireAssetCreation(): Unit = runBlocking {
        val account = seed()
        vault.applyFamilyAdministration(FamilyAdminChange.Goal(
            id = null, ownerId = account.ownerId, name = "Synthetic goal",
            target = null, targetDate = null, priority = null,
        ))
        val baselineHoldings = vault.holdings().size
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerDetailedAsset(
                    account.ownerId, "Should rollback goal", "EQUITY", "Large cap",
                    goalIds = listOf("unknown-goal"),
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerDetailedAsset(
                    account.ownerId, "Should rollback field", "EQUITY", "Large cap",
                    initialProfile = HoldingProfileEdit(
                        HoldingId("draft"), account.ownerId, "Should rollback field",
                        instrumentMetadataJson = "{\"unsupportedSensitiveField\":\"bad\"}",
                    ),
                )
            }
        }
        assertEquals(baselineHoldings, vault.holdings().size)
        assertTrue(vault.holdingProfiles().isEmpty())
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun instrumentEditMergesAndDoesNotDeleteOriginalFields(): Unit = runBlocking {
        val account = seed()
        val registered = vault.registerDetailedAsset(account.ownerId,
            "Synthetic market holding", "EQUITY", "Large cap",
            initialProfile = HoldingProfileEdit(
                HoldingId("draft"), account.ownerId, "Synthetic market holding",
                instrumentMetadataJson = "{\"amc\":\"First\",\"rta\":\"Original\"}",
            ),
        )
        vault.updateHoldingProfile(HoldingProfileEdit(
            registered.id, account.ownerId, registered.name,
            subCategory = "Large cap",
            instrumentMetadataJson = "{\"amc\":\"Second\"}",
        ))
        val metadata = JSONObject(vault.holdingProfiles().single().instrumentMetadataJson)
        assertEquals("Second", metadata.getString("amc"))
        assertEquals("Original", metadata.getString("rta"))
        assertTrue(vault.events().isEmpty())
    }

    @Test fun confirmedLoanOpeningAndZeroValuationPermitSafeClosure(): Unit = runBlocking {
        val account = seed()
        val loan = vault.registerDetailedAsset(
            account.ownerId, "Synthetic loan opening", "LIABILITY", "Home loan")
        val draft = `in`.growurdhan.domain.finance.ManualTradeDraft(
            loan.id, `in`.growurdhan.domain.finance.ManualTradeKind.OPENING_BALANCE,
            "2026-10-01", "1", "125000.0000")
        val reviewed = `in`.growurdhan.domain.finance.ManualTradePlanner.prepare(
            vault.readBook(), draft, "loan-start", java.time.LocalDate.parse("2026-10-10"))
        vault.commitManualTrade(draft, reviewed)
        assertEquals(1, vault.events().size)
        assertEquals(1_250_000_000L, vault.holdings().single().currentUnitPrice.raw)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                loan.id, account.ownerId, loan.name,
                subCategory = "Home loan", closedOn = "2026-10-10")) }
        }
        val payoff = `in`.growurdhan.domain.finance.ManualTradeDraft(
            loan.id, `in`.growurdhan.domain.finance.ManualTradeKind.VALUATION_UPDATE,
            "2026-10-10", "", "0")
        val payoffPlan = `in`.growurdhan.domain.finance.ManualTradePlanner.prepare(
            vault.readBook(), payoff, "loan-payoff", java.time.LocalDate.parse("2026-10-10"))
        vault.commitManualTrade(payoff, payoffPlan)
        assertEquals(0L, vault.holdings().single().currentUnitPrice.raw)
        vault.updateHoldingProfile(HoldingProfileEdit(
            loan.id, account.ownerId, loan.name,
            subCategory = "Home loan", closedOn = "2026-10-10"))
        assertEquals("2026-10-10", vault.holdingProfiles().single().closedOn)
        assertEquals(2, vault.events().size)
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun loanPropertyLinksAreSymmetricAndDematIdentityPairsAreValidated(): Unit = runBlocking {
        val account = seed()
        val loan = vault.registerDetailedAsset(account.ownerId, "Synthetic loan", "LIABILITY", "Home loan")
        val property = vault.registerDetailedAsset(account.ownerId, "Synthetic home", "REAL_ESTATE", "Residential")
        vault.updateHoldingProfile(HoldingProfileEdit(
            loan.id, account.ownerId, loan.name, subCategory = "Home loan",
            detailsJson = JSONObject().put("linkedHoldingId", property.id.raw).toString()))
        vault.updateHoldingProfile(HoldingProfileEdit(
            property.id, account.ownerId, property.name, subCategory = "Residential",
            detailsJson = JSONObject().put("linkedHoldingId", loan.id.raw).toString()))
        assertEquals(property.id.raw,
            JSONObject(vault.holdingProfiles().first { it.holdingId == loan.id }.detailsJson)
                .getString("linkedHoldingId"))
        val shares = vault.registerDetailedAsset(account.ownerId, "Synthetic shares",
            "EQUITY", "Large cap")
        val nsdl = JSONObject().put("depository", "NSDL")
            .put("dpId", "IN300214").put("clientId", "12345678")
        vault.updateHoldingProfile(HoldingProfileEdit(shares.id, account.ownerId,
            shares.name, subCategory = "Large cap", detailsJson = nsdl.toString()))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                shares.id, account.ownerId, shares.name,
                detailsJson = JSONObject().put("depository", "CDSL").toString())) }
        }
        assertEquals("NSDL",
            JSONObject(vault.holdingProfiles().first { it.holdingId == shares.id }.detailsJson)
                .getString("depository"))
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun listedReitCannotBeMistakenForMortgageLinkedProperty(): Unit = runBlocking {
        val account = seed()
        val loan = vault.registerDetailedAsset(account.ownerId,
            "Synthetic loan for REIT guard", "LIABILITY", "Home loan")
        val reit = vault.registerDetailedAsset(account.ownerId,
            "Synthetic REIT", "REAL_ESTATE", "REIT")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                loan.id, account.ownerId, loan.name, subCategory = "Home loan",
                detailsJson = JSONObject().put("linkedHoldingId", reit.id.raw).toString())) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { vault.updateHoldingProfile(HoldingProfileEdit(
                reit.id, account.ownerId, reit.name, subCategory = "REIT",
                detailsJson = JSONObject().put("linkedHoldingId", loan.id.raw).toString())) }
        }
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    @Test fun duplicateIsinForSameOwnerCannotCreateSecondFinancialHolding(): Unit = runBlocking {
        val account = seed()
        val first = vault.registerDetailedAsset(
            account.ownerId, "Synthetic listed security", "EQUITY", "Large cap",
            initialProfile = HoldingProfileEdit(
                HoldingId("draft-a"), account.ownerId, "Synthetic listed security",
                subCategory = "Large cap", isin = "INE000000001",
            ),
        )
        val baseline = vault.holdings().size
        val existing = vault.holdingProfiles().single { it.holdingId == first.id }
        assertEquals("INE000000001", existing.isin)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.registerDetailedAsset(
                    account.ownerId, "Synthetic duplicate listed security", "EQUITY",
                    "Large cap", initialProfile = HoldingProfileEdit(
                        HoldingId("draft-b"), account.ownerId,
                        "Synthetic duplicate listed security",
                        subCategory = "Large cap", isin = "INE000000001",
                    ),
                )
            }
        }
        assertEquals(baseline, vault.holdings().size)
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }
}
