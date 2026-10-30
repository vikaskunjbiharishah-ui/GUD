package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.FamilyMember
import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.HoldingProfileEdit
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.QuantityScaled
import `in`.growurdhan.domain.finance.VaultFinanceReview
import `in`.growurdhan.domain.finance.toFinancialHolding
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Restart and wrong-owner checks. Reviews are not ledger writes. */
@RunWith(AndroidJUnit4::class)
class VaultFinanceReviewInstrumentedTest {
    private lateinit var folder: File
    private lateinit var alias: String
    private lateinit var vault: SqlCipherVaultRepository
    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")

    @Before fun setup() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "finance-review-$suffix")
        alias = "in.growurdhan.finance.test.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test fun storedRateAndEmiSurviveReopenAndDoNotPost(): Unit = runBlocking {
        vault.openOrCreate()
        vault.createMember(FamilyMember(owner, "Primary", "Self", true))
        vault.createMember(FamilyMember(other, "Relative", "Sibling", false))
        val ppf = Holding(
            HoldingId("synthetic-ppf"), owner, null, "Synthetic PPF",
            "FIXED_INCOME", QuantityScaled(100_000_000L), MoneyScaled(100_000L * 10_000),
        )
        val otherPpf = Holding(
            HoldingId("synthetic-ppf-other"), other, null, "Other PPF",
            "FIXED_INCOME", QuantityScaled(100_000_000L), MoneyScaled(50_000L * 10_000),
        )
        val loan = Holding(
            HoldingId("synthetic-loan"), owner, null, "Synthetic car loan",
            "LIABILITY", QuantityScaled(100_000_000L), MoneyScaled(612_000L * 10_000),
        )
        val paused = Holding(
            HoldingId("synthetic-paused"), owner, null, "Synthetic paused loan",
            "LIABILITY", QuantityScaled(100_000_000L), MoneyScaled(100_000L * 10_000),
        )
        listOf(ppf, otherPpf, loan, paused).forEach { vault.createHolding(it) }
        vault.updateHoldingProfile(HoldingProfileEdit(
            ppf.id, owner, ppf.name, subCategory = "PPF", acquiredOn = "2022-04-01",
            verifiedCostPerUnit = MoneyScaled(100_000L * 10_000),
            priceSource = "Manual Initial Entry",
            richMetadataJson = """{"interest_rate_bps":710}""",
        ))
        vault.updateHoldingProfile(HoldingProfileEdit(
            otherPpf.id, other, otherPpf.name, subCategory = "PPF", acquiredOn = "2022-04-01",
            verifiedCostPerUnit = MoneyScaled(50_000L * 10_000),
            richMetadataJson = """{"interest_rate_bps":710}""",
        ))
        vault.updateHoldingProfile(HoldingProfileEdit(
            loan.id, owner, loan.name, subCategory = "Car loan",
            instrumentMetadataJson = """{"nextEmiDate":"2026-07-05"}""",
            richMetadataJson = """{"monthly_emi_scaled":185000000,"remaining_emi_count":6,"emi_status":"Paying"}""",
        ))
        vault.updateHoldingProfile(HoldingProfileEdit(
            paused.id, owner, paused.name, subCategory = "Car loan",
            instrumentMetadataJson = """{"nextEmiDate":"2026-07-05"}""",
            richMetadataJson = """{"monthly_emi_scaled":100000000,"remaining_emi_count":4,"emi_status":"Paused"}""",
        ))
        assertTrue(vault.events().isEmpty())
        vault.close()
        vault.openOrCreate()
        val holdings = vault.readBook().holdings.map { it.toFinancialHolding() }
        val profiles = vault.holdingProfiles()
        val interest = VaultFinanceReview.interest(
            holdings, profiles, LocalDate.parse("2026-09-26"), owner,
        )
        val emi = VaultFinanceReview.emi(
            holdings, profiles, LocalDate.parse("2026-09-09"), owner,
        )
        assertEquals("Synthetic PPF", interest.single().name)
        assertEquals(71_000_000L, interest.single().plan.events.first().amount.raw)
        assertEquals(owner, interest.single().ownerId)
        assertTrue(VaultFinanceReview.interest(holdings, profiles, LocalDate.parse("2026-09-26"), other)
            .all { it.ownerId == other })
        assertEquals(1, VaultFinanceReview.interest(holdings, profiles, LocalDate.parse("2026-09-26"), other).size)
        assertEquals("Synthetic car loan", emi.single().name)
        assertEquals(3, emi.single().plan.events.size)
        assertTrue(vault.events().isEmpty())
        assertEquals(0, vault.health().foreignKeyViolations)
    }
}
