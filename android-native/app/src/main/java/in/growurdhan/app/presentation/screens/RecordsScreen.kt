package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.domain.finance.HouseholdRecords
import `in`.growurdhan.domain.finance.HouseholdRecord
import `in`.growurdhan.domain.finance.ImportBatchView

/** Statements, tax notes and import history for one owner. Other people stay hidden. */
@Composable
fun RecordsScreen(
    records: List<HouseholdRecord>,
    batches: List<ImportBatchView>,
    owner: OwnerId?,
    onBack: () -> Unit,
) {
    val shown = HouseholdRecords.visible(records, owner)
    val history = HouseholdRecords.history(batches, owner)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Records", style = MaterialTheme.typography.headlineMedium)
        Text("Statements, tax notes and import history stay with the person they belong to.")
        if (shown.isEmpty() && history.isEmpty()) {
            Text("No statement, tax note, or record is loaded for this view.")
        }
        shown.forEach { row ->
            Text("${row.kind.name.lowercase().replaceFirstChar { it.titlecase() }} · ${row.title}")
            Text(row.dated.toString())
        }
        if (history.isNotEmpty()) Text("Import history", style = MaterialTheme.typography.titleMedium)
        history.forEach { batch ->
            Text("${batch.registrar} · ${batch.statementDate} · ${batch.lineCount} line(s)")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Back to home"
            },
        ) { Text("Back to Home") }
    }
}
