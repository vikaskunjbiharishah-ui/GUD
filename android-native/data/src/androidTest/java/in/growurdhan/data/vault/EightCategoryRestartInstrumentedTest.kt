package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.HoldingProfileEdit
import `in`.growurdhan.domain.finance.ManualTradeDraft
import `in`.growurdhan.domain.finance.ManualTradeKind
import `in`.growurdhan.domain.finance.ManualTradePlanner
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Each canonical category: specialist facts survive close/reopen, and a bad date,
 * a negative quantity, or another owner's id is refused.
 */
@RunWith(AndroidJUnit4::class)
class EightCategoryRestartInstrumentedTest {
    private lateinit var context: Context
    private lateinit var folder: File
    private lateinit var alias: String
    private lateinit var vault: SqlCipherVaultRepository

    @Before fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "eight-category-$suffix")
        alias = "in.growurdhan.test.q3.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test fun equitySpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("EQUITY", "Large cap")
    @Test fun mutualFundSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("MUTUAL_FUND", "Equity fund")
    @Test fun goldSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("GOLD_SGB", "Physical gold (24K)")
    @Test fun fixedIncomeSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("FIXED_INCOME", "NPS")
    @Test fun realEstateSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("REAL_ESTATE", "Residential")
    @Test fun specialisedSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("SPECIALISED", "AIF")
    @Test fun cashBankSpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("CASH_BANK", "Savings account")
    @Test fun liabilitySpecialistFactsSurviveRestartAndRejectBadInput() = roundTrip("LIABILITY", "Home loan")

    @Test fun editedArchivedAndClosedHoldingReopensWithoutALedgerRewrite(): Unit = runBlocking {
        vault.openOrCreate()
        vault.registerFamilyMember("Synthetic Primary", "Self")
        val owner = vault.members().single().id
        val created = vault.registerDetailedAsset(owner, "Synthetic shares", "EQUITY", "Large cap")
        vault.updateHoldingProfile(HoldingProfileEdit(
            created.id, owner, created.name, "Large cap",
            JSONObject().put("depository", "NSDL").put("dpId", "IN300214")
                .put("clientId", "12345678").toString(),
        ))
        vault.updateHoldingProfile(HoldingProfileEdit(
            created.id, owner, "Synthetic renamed shares", "Large cap",
            notes = "Synthetic note", archived = true, closedOn = "2020-06-01",
        ))
        vault.close()
        vault = SqlCipherVaultRepository(context, folder, alias)
        vault.openOrCreate()
        val saved = vault.holdingProfiles().single()
        assertEquals("Synthetic renamed shares", saved.name)
        assertTrue(saved.archived)
        assertEquals("2020-06-01", saved.closedOn)
        assertEquals("NSDL", JSONObject(saved.detailsJson).getString("depository"))
        assertTrue(vault.events().isEmpty())
        assertNull(saved.verifiedCostPerUnit)
    }

    private fun roundTrip(category: String, subCategory: String): Unit = runBlocking {
        vault.openOrCreate()
        vault.registerFamilyMember("Synthetic Primary", "Self")
        val owner = vault.members().single().id
        val created = vault.registerDetailedAsset(
            owner, "Synthetic " + category.lowercase(), category, subCategory,
        )
        val facts = specialist(category)
        vault.updateHoldingProfile(HoldingProfileEdit(
            created.id, owner, created.name, subCategory, facts.toString(),
        ))
        vault.close()
        vault = SqlCipherVaultRepository(context, folder, alias)
        vault.openOrCreate()
        val saved = JSONObject(vault.holdingProfiles().single().detailsJson)
        val keys = facts.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            assertEquals(key, facts.get(key), saved.get(key))
        }
        val spouse = vault.registerFamilyMember("Synthetic Spouse", "Spouse")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.updateHoldingProfile(HoldingProfileEdit(
                    created.id, spouse.id, created.name, subCategory,
                ))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.updateHoldingProfile(HoldingProfileEdit(
                    created.id, owner, created.name, subCategory, acquiredOn = "2020-13-40",
                ))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                val draft = ManualTradeDraft(
                    created.id, ManualTradeKind.OPENING_BALANCE, "2020-01-02", "-1", "10",
                )
                ManualTradePlanner.prepare(
                    vault.readBook(), draft, "negative-units", LocalDate.parse("2026-10-28"),
                )
            }
        }
        assertEquals(1, vault.holdings().size)
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }

    private fun specialist(category: String): JSONObject = when (category) {
        "EQUITY" -> JSONObject()
            .put("depository", "NSDL").put("dpId", "IN300214").put("clientId", "12345678")
        "MUTUAL_FUND" -> JSONObject()
            .put("mfPlan", "DIRECT").put("mfOption", "GROWTH").put("mfHeldAs", "FOLIO")
        "GOLD_SGB" -> JSONObject()
            .put("goldHoldMode", "CERTIFICATE").put("goldPurity", "24K").put("goldHallmark", "BIS")
        "FIXED_INCOME" -> JSONObject()
            .put("npsTier", "I").put("npsCra", "CAMS").put("npsPfm", "Synthetic PFM")
        "REAL_ESTATE" -> JSONObject()
            .put("areaSqFt", 1200).put("ownership", "SELF").put("ownershipPercent", 100)
        "SPECIALISED" -> JSONObject()
            .put("altHoldMode", "LUMPSUM").put("aifCategory", "II").put("commitmentMinor", "2500000000")
        "CASH_BANK" -> JSONObject().put("ifsc", "SYNB0000001")
        "LIABILITY" -> JSONObject().put("rateType", "FLOATING").put("originalTenorMonths", 240)
        else -> error("unknown category")
    }
}
