package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

enum class RecordKind { STATEMENT, TAX, RECORD }

data class HouseholdRecord(
    val id: String,
    val ownerId: OwnerId,
    val kind: RecordKind,
    val title: String,
    val dated: LocalDate,
)

data class ImportBatchView(
    val id: String,
    val ownerId: OwnerId,
    val registrar: String,
    val statementDate: LocalDate,
    val sourceSha256: String,
    val lineCount: Int,
)

/** Owner-scoped lists. A repeated statement fingerprint is not stored twice. */
object HouseholdRecords {
    fun visible(records: List<HouseholdRecord>, owner: OwnerId?): List<HouseholdRecord> =
        records.filter { owner == null || it.ownerId == owner }
            .sortedWith(compareByDescending<HouseholdRecord> { it.dated }.thenBy { it.id })

    fun history(batches: List<ImportBatchView>, owner: OwnerId?): List<ImportBatchView> =
        batches.filter { owner == null || it.ownerId == owner }
            .sortedWith(compareByDescending<ImportBatchView> { it.statementDate }.thenBy { it.id })

    fun accept(existing: List<ImportBatchView>, next: ImportBatchView): List<ImportBatchView> {
        if (existing.any { it.ownerId == next.ownerId && it.sourceSha256 == next.sourceSha256 }) return existing
        return existing + next
    }
}
