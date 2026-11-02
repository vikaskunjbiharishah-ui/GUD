package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.StoredStatementImport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultImportHistoryTest {
    @Test fun aStoredSnapshotBecomesOneOwnersHistoryAndNotAGuessedRegistrar() {
        val owner = OwnerId("synthetic-owner")
        val other = OwnerId("synthetic-other")
        val rows = listOf(
            StoredStatementImport("s1", owner, "a".repeat(64), "2026-09-01", 2),
            StoredStatementImport("s2", other, "b".repeat(64), "2026-09-02", 1),
        )
        val batches = VaultImportHistory.batches(rows)
        assertEquals("Saved statement", batches.first().registrar)
        assertEquals(2, batches.first().lineCount)
        val mine = HouseholdRecords.history(batches, owner)
        assertEquals(listOf("s1"), mine.map { it.id })
        val records = HouseholdRecords.visible(VaultImportHistory.records(rows), owner)
        assertEquals(listOf("Saved statement"), records.map { it.title })
        assertEquals(RecordKind.STATEMENT, records.single().kind)
    }
}
