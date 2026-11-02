package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.StoredStatementImport
import java.time.LocalDate

/** Rows already stored in snapshot_import. The vault does not record a registrar name. */
object VaultImportHistory {
    const val LABEL = "Saved statement"

    fun batches(rows: List<StoredStatementImport>): List<ImportBatchView> =
        rows.map { row ->
            ImportBatchView(
                id = row.id,
                ownerId = row.ownerId,
                registrar = LABEL,
                statementDate = LocalDate.parse(row.asOf),
                sourceSha256 = row.sourceSha256,
                lineCount = row.lineCount,
            )
        }

    fun records(rows: List<StoredStatementImport>): List<HouseholdRecord> =
        rows.map { row ->
            HouseholdRecord(
                id = row.id,
                ownerId = row.ownerId,
                kind = RecordKind.STATEMENT,
                title = LABEL,
                dated = LocalDate.parse(row.asOf),
            )
        }
}
