package `in`.growurdhan.data.vault

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.CasHoldingIdentity
import `in`.growurdhan.core.model.FamilyMember
import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.LedgerKind
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.QuantityScaled
import `in`.growurdhan.domain.finance.CasImportContract
import `in`.growurdhan.domain.finance.CasValidationException
import `in`.growurdhan.domain.finance.FinancialHolding
import `in`.growurdhan.domain.finance.ReconciliationOutcome
import `in`.growurdhan.core.model.SnapshotResult
import `in`.growurdhan.domain.finance.SyntheticIssuerStatement
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic CDSL and NPS text, not a real PDF. Parse, review, commit, and
 * a repeated import must not write a second ledger event.
 */
@RunWith(AndroidJUnit4::class)
class SyntheticIssuerImportInstrumentedTest {
    private lateinit var context: Context
    private lateinit var vault: SqlCipherVaultRepository
    private lateinit var folder: File
    private lateinit var alias: String
    private val owner = OwnerId("self")
    private val spouse = OwnerId("spouse")
    private val today = LocalDate.parse("2026-10-16")

    @Before fun prepare() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        folder = File(context.noBackupFilesDir, "issuer-import-$suffix")
        alias = "in.growurdhan.issuer.$suffix"
        vault = SqlCipherVaultRepository(context, folder, alias)
    }

    @After fun cleanup(): Unit = runBlocking {
        vault.close()
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test fun cdslAndNpsCommitOnceAndRejectTheOtherOwner(): Unit = runBlocking {
        vault.openOrCreate()
        vault.createMember(FamilyMember(owner, "Synthetic", "Self", true))
        vault.createMember(FamilyMember(spouse, "Other", "Spouse", false))
        val equityId = HoldingId("eq")
        val npsId = HoldingId("nps")
        vault.createHolding(Holding(
            equityId, owner, null, "Synthetic CDSL equity", "EQUITY",
            QuantityScaled(0), MoneyScaled(1_000_000),
        ))
        vault.createHolding(Holding(
            npsId, owner, null, "Synthetic NPS equity scheme", "FIXED_INCOME",
            QuantityScaled(0), MoneyScaled(200_000),
        ))
        val equity = FinancialHolding(
            equityId, owner, "Synthetic CDSL equity", "EQUITY", MoneyScaled(1_000_000),
        )
        val nps = FinancialHolding(
            npsId, owner, "Synthetic NPS equity scheme", "FIXED_INCOME", MoneyScaled(200_000),
        )
        val cdslText = """
            SYNTHETIC-ISSUER 1
            issuer: CDSL
            statement-date: 2026-09-30
            source-sha256: ${"ab".repeat(32)}
            holding: Registrar printed name|EQUITY|10|100.0000|800.0000|INE000000001|BO1234567890
        """.trimIndent()
        val cdsl = CasImportContract.validate(
            SyntheticIssuerStatement.parse(cdslText), owner, listOf(equity), today,
            listOf(CasHoldingIdentity(
                equityId, owner, "demat", "INE000000001", "BO1234567890", "CDSL",
            )),
        )
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(
                SyntheticIssuerStatement.parse(cdslText), owner, listOf(equity), today,
                listOf(CasHoldingIdentity(
                    equityId, spouse, "demat", "INE000000001", "BO1234567890", "CDSL",
                )),
            )
        }
        assertEquals(0, vault.events().size)
        val review = vault.prepareStatementReview("cdsl-1", owner, cdsl.sourceSha256, cdsl.lines)
        assertEquals(ReconciliationOutcome.NEW_POSITION, review.plan.lines.single().outcome)
        assertEquals(SnapshotResult.IMPORTED, vault.commitReviewedStatement(review))
        assertEquals(SnapshotResult.ALREADY_IMPORTED, vault.commitReviewedStatement(review))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vault.prepareStatementReview("cdsl-again", owner, cdsl.sourceSha256, cdsl.lines)
            }
        }
        val npsText = """
            SYNTHETIC-ISSUER 1
            issuer: NPS
            statement-date: 2026-10-15
            source-sha256: ${"cd".repeat(32)}
            holding: Synthetic NPS equity scheme|FIXED_INCOME|3.5|20.0000||NP0000000001|110123981321
        """.trimIndent()
        val parsedNps = SyntheticIssuerStatement.parse(npsText)
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(
                parsedNps, spouse, listOf(nps.copy(ownerId = spouse)), today,
                listOf(CasHoldingIdentity(
                    npsId, owner, "nps", "NP0000000001", "110123981321", "NPS",
                )),
            )
        }
        val npsPreview = CasImportContract.validate(
            parsedNps, owner, listOf(nps), today,
            listOf(CasHoldingIdentity(
                npsId, owner, "nps", "NP0000000001", "110123981321", "NPS",
            )),
        )
        val npsReview = vault.prepareStatementReview(
            "nps-1", owner, npsPreview.sourceSha256, npsPreview.lines,
        )
        assertEquals(SnapshotResult.IMPORTED, vault.commitReviewedStatement(npsReview))
        vault.close()
        vault.openOrCreate()
        val book = vault.events()
        assertEquals(2, book.size)
        val opening = book.single { it.holdingId == equityId }
        assertEquals(LedgerKind.OPENING_BALANCE, opening.kind)
        assertEquals(1_000_000_000L, opening.quantityDeltaScaled)
        assertEquals(8_000_000L, opening.amount.raw)
        assertEquals(800_000L, opening.unitPrice?.raw)
        val pension = book.single { it.holdingId == npsId }
        assertEquals(350_000_000L, pension.quantityDeltaScaled)
        assertEquals(0L, pension.amount.raw)
    }
}
