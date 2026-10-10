package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.finance.*
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated SQLCipher/Keystore test data; never touches an actual household. */
@RunWith(AndroidJUnit4::class)
class NativeManualTradeInstrumentedTest {
    private lateinit var folder: File
    private lateinit var alias: String
    private lateinit var vault: SqlCipherVaultRepository
    private val owner = OwnerId("synthetic-owner")
    private val holding = Holding(
        HoldingId("synthetic-asset"), owner, null, "Synthetic fund",
        "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(100_000),
    )
    private val asOf = LocalDate.parse("2026-10-10")

    @Before fun setup() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "manual-ledger-$suffix")
        alias = "in.growurdhan.manual.test.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    private suspend fun seed() {
        vault.openOrCreate()
        vault.createMember(FamilyMember(owner, "Primary", "Self", true))
        vault.createHolding(holding)
    }

    @Test fun approvedManualBuyAndSellAppendTwoRealImmutableLedgerRows(): Unit = runBlocking {
        seed()
        val before = vault.readBook()
        val buy = ManualTradePlanner.prepare(before, ManualTradeDraft(
            holding.id, ManualTradeKind.BUY, "2026-09-01", "2", "100", "1",
        ), "synthetic-buy", asOf)
        assertEquals(0, before.events.size)
        vault.commitManualTrade(ManualTradeDraft(
            holding.id, ManualTradeKind.BUY, "2026-09-01", "2", "100", "1",
        ), buy)
        assertEquals(LedgerKind.BUY, vault.events().single().kind)
        val sell = ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
            holding.id, ManualTradeKind.SELL, "2026-10-01", "1", "125", "0.25",
        ), "synthetic-sell", asOf)
        vault.commitManualTrade(ManualTradeDraft(
            holding.id, ManualTradeKind.SELL, "2026-10-01", "1", "125", "0.25",
        ), sell)
        val persisted = vault.readBook()
        assertEquals(2, persisted.events.size)
        assertEquals(listOf(LedgerKind.BUY, LedgerKind.SELL),
            persisted.events.map { it.kind })
        assertEquals(-100_000_000L, persisted.events.last().quantityDeltaScaled)
        assertEquals(0, vault.health().foreignKeyViolations)
        val position = LotEngine.fold(
            holding.id, holding.assetCategory, persisted.events.map { it.toFinancialEvent() },
        )
        assertEquals(100_000_000L, position.quantityScaled)
        assertTrue(position.errors.isEmpty())
        vault.close()
        vault.openOrCreate()
        assertEquals(persisted, vault.readBook())
    }

    @Test fun interveningLedgerWriteCannotMakeOldReviewCommit(): Unit = runBlocking {
        seed()
        val draft = ManualTradeDraft(
            holding.id, ManualTradeKind.BUY, "2026-10-01", "2", "100",
        )
        val reviewed = ManualTradePlanner.prepare(vault.readBook(), draft, "stale", asOf)
        // An independent approved financial event arrives after the UI review.
        vault.appendFinancialEvents(listOf(FinancialEvent(
            id = "concurrent", holdingId = holding.id, ownerId = owner,
            kind = FinancialEventKind.BUY, date = LocalDate.parse("2026-09-20"),
            quantityScaled = 100_000_000L, unitPrice = MoneyScaled(95_000),
            totalAmount = MoneyScaled(95_000),
        )))
        assertThrows(IllegalStateException::class.java) {
            runBlocking { vault.commitManualTrade(draft, reviewed) }
        }
        assertEquals(listOf("concurrent"), vault.events().map { it.id })
    }

    @Test fun duplicateTradeIdIsRejectedWithoutChangingExistingRows(): Unit = runBlocking {
        seed()
        val event = ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
            holding.id, ManualTradeKind.BUY, "2026-10-01", "1", "100",
        ), "synthetic-once", asOf).event
        vault.commitManualTrade(
            ManualTradeDraft(holding.id, ManualTradeKind.BUY, "2026-10-01", "1", "100"),
            ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                holding.id, ManualTradeKind.BUY, "2026-10-01", "1", "100",
            ), event.id, asOf),
        )
        assertThrows(Exception::class.java) {
            runBlocking { vault.commitManualTrade(
            ManualTradeDraft(holding.id, ManualTradeKind.BUY, "2026-10-01", "1", "100"),
            ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                holding.id, ManualTradeKind.BUY, "2026-10-01", "1", "100",
            ), event.id, asOf),
        ) }
        }
        assertEquals(listOf("synthetic-once"), vault.events().map { it.id })
    }

    @Test fun snapshotOnlyUnitsDoNotPermitInventedSellOrLedgerWrite(): Unit = runBlocking {
        vault.openOrCreate()
        vault.createMember(FamilyMember(owner, "Primary", "Self", true))
        vault.createHolding(holding.copy(quantity = QuantityScaled(200_000_000L)))
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(vault.readBook(), ManualTradeDraft(
                holding.id, ManualTradeKind.SELL, "2026-10-01", "1", "100",
            ), "synthetic-unbacked-sell", asOf)
        }
        assertTrue(vault.events().isEmpty())
    }
}
