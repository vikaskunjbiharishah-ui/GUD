package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class M27CorrectionIntegrityTest {
    private val holding = HoldingId("synthetic-fund")
    private val owner = OwnerId("synthetic-owner")
    private val original = FinancialEvent(
        id = "source", holdingId = holding, ownerId = owner,
        kind = FinancialEventKind.BUY, date = LocalDate.of(2026, 1, 1),
        quantityScaled = 100_000_000L, unitPrice = MoneyScaled(100_0000L),
        totalAmount = MoneyScaled(100_0000L),
    )
    private fun correction(
        id: String = "corrected",
        supersedes: String = "source",
        ownerId: OwnerId = owner,
        date: LocalDate = LocalDate.of(2026, 1, 2),
    ) = FinancialEvent(
        id = id, holdingId = holding, ownerId = ownerId,
        kind = FinancialEventKind.CORRECTION, date = date,
        quantityScaled = 200_000_000L, unitPrice = MoneyScaled(100_0000L),
        totalAmount = MoneyScaled(200_0000L), supersedesEventId = supersedes,
    )

    @Test fun validCorrectionSupersedesWithoutDeletingOriginal() {
        val historical = listOf(original, correction())
        val folded = LotEngine.fold(holding, "EQUITY", historical)
        assertTrue(folded.errors.isEmpty(), folded.errors.toString())
        assertEquals(200_000_000L, folded.quantityScaled)
        assertEquals(2, historical.size)
    }

    @Test fun missingAndCrossOwnerCorrectionsAreRejected() {
        val missing = LotEngine.fold(holding, "EQUITY",
            listOf(original, correction(supersedes = "absent")))
        assertTrue(missing.errors.any { it.contains("missing event") })
        val crossing = LotEngine.fold(holding, "EQUITY",
            listOf(original, correction(ownerId = OwnerId("other"))))
        assertTrue(crossing.errors.any { it.contains("owner boundaries") })
    }

    @Test fun competingAndBackdatedCorrectionsAreRejected() {
        val competing = LotEngine.fold(holding, "EQUITY",
            listOf(original, correction(), correction(id = "other-correction")))
        assertTrue(competing.errors.any { it.contains("competing") })
        val backdated = LotEngine.fold(holding, "EQUITY",
            listOf(original, correction(date = LocalDate.of(2025, 12, 31))))
        assertTrue(backdated.errors.any { it.contains("predates") })
    }

    @Test fun ordinaryEventCannotSupersedeLedgerHistory() {
        val invalid = original.copy(
            id = "bad-buy",
            supersedesEventId = "source",
            date = LocalDate.of(2026, 1, 2),
        )
        val folded = LotEngine.fold(holding, "EQUITY", listOf(original, invalid))
        assertTrue(folded.errors.any { it.contains("Only CORRECTION") })
    }
}
