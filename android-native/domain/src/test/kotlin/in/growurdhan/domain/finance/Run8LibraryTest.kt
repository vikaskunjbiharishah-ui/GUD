package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Run8LibraryTest {
    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")

    @Test fun sgbToggleDoesNotInventTheMissingSide() {
        val rbi = MoneyScaled(6_200L * 10_000)
        val market = MoneyScaled(6_500L * 10_000)
        assertEquals(rbi, SgbPrice.choose(SgbPriceBasis.RBI, rbi, market))
        assertEquals(market, SgbPrice.choose(SgbPriceBasis.MARKET, rbi, market))
        assertNull(SgbPrice.choose(SgbPriceBasis.MARKET, rbi, null))
        assertNull(SgbPrice.choose(SgbPriceBasis.RBI, MoneyScaled(0), market))
        assertEquals(SgbPrice.MARKET_MISSING, SgbPrice.label(SgbPriceBasis.MARKET, null))
        assertEquals("Using the RBI price.", SgbPrice.label(SgbPriceBasis.RBI, rbi))
    }

    @Test fun diagnosticsStayOffAndDropSecrets() {
        val kept = listOf("Opened settings.")
        assertEquals(kept, DiagnosticsLog.record(false, kept, "ABCDE1234F"))
        assertEquals(emptyList<String>(), DiagnosticsLog.clear())
        val noted = DiagnosticsLog.record(true, emptyList(), "Vault opened for synthetic-owner")
        assertEquals(listOf("Vault opened for synthetic-owner"), noted)
        assertEquals(emptyList<String>(), DiagnosticsLog.record(true, emptyList(), "PAN ABCDE1234F mail a@b.c token"))
        val redacted = DiagnosticsLog.record(true, emptyList(), "Member ABCDE1234F checked")
        assertEquals(listOf("Member [redacted] checked"), redacted)
        assertTrue(redacted.none { it.contains("ABCDE1234F") })
    }

    @Test fun recordsAndImportHistoryStayWithTheOwnerAndIgnoreADuplicate() {
        val statement = HouseholdRecord("s1", owner, RecordKind.STATEMENT, "Synthetic CAS", LocalDate.parse("2026-09-01"))
        val tax = HouseholdRecord("t1", owner, RecordKind.TAX, "FY note", LocalDate.parse("2026-04-01"))
        val theirs = HouseholdRecord("s2", other, RecordKind.RECORD, "Other person", LocalDate.parse("2026-10-01"))
        val mine = HouseholdRecords.visible(listOf(statement, tax, theirs), owner)
        assertEquals(listOf("s1", "t1"), mine.map { it.id })
        val batch = ImportBatchView(
            "b1", owner, "NSDL", LocalDate.parse("2026-09-01"), "a".repeat(64), 1,
        )
        val again = batch.copy(id = "b2")
        val stored = HouseholdRecords.accept(HouseholdRecords.accept(emptyList(), batch), again)
        assertEquals(listOf("b1"), stored.map { it.id })
        assertTrue(HouseholdRecords.history(stored, other).isEmpty())
    }
}
