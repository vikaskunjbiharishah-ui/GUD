package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.FamilyMember
import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.LedgerKind
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.QuantityScaled
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.FinancialEventKind
import `in`.growurdhan.domain.finance.ManualTradeDraft
import `in`.growurdhan.domain.finance.ManualTradeKind
import `in`.growurdhan.domain.finance.ManualTradePlanner
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * User-path save, restart, and negatives for the event types that were still
 * only planned in memory. Synthetic ids only. Rows are append-only.
 */
@RunWith(AndroidJUnit4::class)
class ManualEventRestartInstrumentedTest {
    private lateinit var folder: File
    private lateinit var alias: String
    private lateinit var vault: SqlCipherVaultRepository
    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")
    private val equity = Holding(
        HoldingId("synthetic-equity"), owner, null, "Synthetic equity",
        "EQUITY", QuantityScaled(0), MoneyScaled(100_000),
    )
    private val loan = Holding(
        HoldingId("synthetic-loan"), owner, null, "Synthetic loan",
        "LIABILITY", QuantityScaled(0), MoneyScaled(0),
    )
    private val today = LocalDate.parse("2026-10-30")

    @Before fun setup() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "event-restart-$suffix")
        alias = "in.growurdhan.events.test.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    private suspend fun post(draft: ManualTradeDraft, id: String) {
        val plan = ManualTradePlanner.prepare(vault.readBook(), draft, id, today)
        vault.commitManualTrade(draft, plan)
    }

    @Test fun incomeBonusSplitOpeningAndCorrectionSurviveRestart(): Unit = runBlocking {
        vault.openOrCreate()
        vault.createMember(FamilyMember(owner, "Primary", "Self", true))
        vault.createMember(FamilyMember(other, "Relative", "Sibling", false))
        vault.createHolding(equity)
        vault.createHolding(loan)
        post(ManualTradeDraft(equity.id, ManualTradeKind.OPENING_BALANCE, "2026-09-01", "1", "100"), "eq-open")
        post(ManualTradeDraft(equity.id, ManualTradeKind.DIVIDEND, "2026-09-15", "", "12.50"), "eq-div")
        post(ManualTradeDraft(equity.id, ManualTradeKind.INTEREST, "2026-09-20", "", "3"), "eq-int")
        post(ManualTradeDraft(equity.id, ManualTradeKind.BONUS, "2026-09-25", "1", "0"), "eq-bonus")
        post(ManualTradeDraft(equity.id, ManualTradeKind.SPLIT, "2026-10-01", "2", "1"), "eq-split")
        post(ManualTradeDraft(
            equity.id, ManualTradeKind.CORRECTION, "2026-10-05", "", "10",
            supersedesEventId = "eq-div",
        ), "eq-div-fix")
        post(ManualTradeDraft(loan.id, ManualTradeKind.OPENING_BALANCE, "2026-09-01", "1", "500000"), "loan-open")
        post(ManualTradeDraft(loan.id, ManualTradeKind.REPAYMENT, "2026-10-01", "", "1000"), "loan-pay")
        post(ManualTradeDraft(loan.id, ManualTradeKind.INTEREST, "2026-10-02", "", "50"), "loan-int")

        val saved = vault.readBook()
        assertEquals(
            setOf("eq-open", "eq-div", "eq-int", "eq-bonus", "eq-split", "eq-div-fix", "loan-open", "loan-pay", "loan-int"),
            saved.events.map { it.id }.toSet(),
        )
        assertEquals(LedgerKind.OPENING_BALANCE, saved.events.single { it.id == "eq-open" }.kind)
        assertEquals(LedgerKind.DIVIDEND, saved.events.single { it.id == "eq-div" }.kind)
        assertEquals(125_000L, saved.events.single { it.id == "eq-div" }.amount.raw)
        assertEquals(LedgerKind.INTEREST, saved.events.single { it.id == "eq-int" }.kind)
        assertEquals(LedgerKind.BONUS, saved.events.single { it.id == "eq-bonus" }.kind)
        assertEquals(100_000_000L, saved.events.single { it.id == "eq-bonus" }.quantityDeltaScaled)
        assertEquals(LedgerKind.SPLIT, saved.events.single { it.id == "eq-split" }.kind)
        assertEquals(2L, saved.events.single { it.id == "eq-split" }.quantityDeltaScaled)
        assertEquals(1L, saved.events.single { it.id == "eq-split" }.unitPrice?.raw)
        val correction = saved.events.single { it.id == "eq-div-fix" }
        assertEquals(LedgerKind.CORRECTION, correction.kind)
        assertEquals("eq-div", correction.supersedesEventId)
        assertEquals(100_000L, correction.amount.raw)
        assertEquals(owner, correction.ownerId)
        assertEquals(LedgerKind.REPAYMENT, saved.events.single { it.id == "loan-pay" }.kind)
        assertEquals(10_000_000L, saved.events.single { it.id == "loan-pay" }.amount.raw)
        // The original dividend row stays. A correction does not rewrite it.
        assertTrue(saved.events.any { it.id == "eq-div" })
        assertEquals(0, vault.health().foreignKeyViolations)

        vault.close()
        vault.openOrCreate()
        assertEquals(saved, vault.readBook())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                    equity.id, ManualTradeKind.DIVIDEND, "2026-10-06", "1", "1",
                ), "bad-units", today)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                    equity.id, ManualTradeKind.INTEREST, "2026-10-06", "", "1", chargesInr = "1",
                ), "bad-fee", today)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                    equity.id, ManualTradeKind.REPAYMENT, "2026-10-06", "", "10",
                ), "bad-repay", today)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                    equity.id, ManualTradeKind.CORRECTION, "2026-10-06", "", "9",
                    supersedesEventId = "eq-div",
                ), "second-fix", today)
            }
        }
        assertThrows(Exception::class.java) {
            runBlocking {
                vault.appendFinancialEvents(listOf(FinancialEvent(
                    id = "wrong-owner-dividend",
                    holdingId = equity.id,
                    ownerId = other,
                    kind = FinancialEventKind.DIVIDEND,
                    date = LocalDate.parse("2026-09-16"),
                    totalAmount = MoneyScaled(10_000),
                )))
            }
        }
        vault.createHolding(Holding(
            HoldingId("other-equity"), other, null, "Other equity",
            "EQUITY", QuantityScaled(0), MoneyScaled(1),
        ))
        assertThrows(Exception::class.java) {
            runBlocking {
                vault.appendFinancialEvents(listOf(FinancialEvent(
                    id = "cross-owner-correction",
                    holdingId = HoldingId("other-equity"),
                    ownerId = other,
                    kind = FinancialEventKind.CORRECTION,
                    date = LocalDate.parse("2026-10-06"),
                    totalAmount = MoneyScaled(1_000),
                    supersedesEventId = "eq-open",
                )))
            }
        }
        assertEquals(saved.events.map { it.id }, vault.events().map { it.id })
        assertEquals(0, vault.health().foreignKeyViolations)
    }
}
