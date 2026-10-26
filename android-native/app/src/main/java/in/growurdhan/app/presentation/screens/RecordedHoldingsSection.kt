package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.RecordedHoldingDashboard

private const val PAGE_SIZE = 30

/** Present only while unlocked. Search is ephemeral, not saved to disk or SavedState. */
@Composable
internal fun RecordedHoldingsSection(holdings: List<RecordedHoldingDashboard>) {
    var query by remember { mutableStateOf("") }
    var shown by remember { mutableIntStateOf(PAGE_SIZE) }
    var includeInactive by remember { mutableStateOf(false) }
    val filteredByStatus = if (includeInactive) holdings else holdings.filter {
        !it.archived && it.closedOn == null
    }
    val matches = matchingRecordedHoldings(filteredByStatus, query)
    val visible = matches.take(shown)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recorded holdings", style = MaterialTheme.typography.titleMedium)
            Text(
                "Stored prices, not live quotes. Account IDs and folios are not shown. " +
                    "Use nicknames without personal identifiers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeInactive, onCheckedChange = {
                    includeInactive = it
                    shown = PAGE_SIZE
                })
                Text("Show archived and closed holdings",
                    style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80); shown = PAGE_SIZE },
                modifier = Modifier.fillMaxWidth().testTag("RecordedHoldingsSearch"),
                label = { Text("Search recorded holdings") },
                singleLine = true,
            )
            Text(
                "Showing ${visible.size} of ${matches.size} matching holding(s); " +
                    "${holdings.size} recorded in total.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (matches.isEmpty()) Text("No recorded holdings match this search.")
            visible.forEach { holding ->
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(holding.name, style = MaterialTheme.typography.titleSmall)
                    if (holding.archived) Text("Archived · history retained",
                        style = MaterialTheme.typography.bodySmall)
                    holding.closedOn?.let { Text("Closed on $it",
                        style = MaterialTheme.typography.bodySmall) }
                    Text(
                        holding.category.replace('_', ' ') +
                            (holding.ownerName?.let { " · " + it } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        (if (holding.isLiability) "Outstanding liability: " else "Recorded value: ") +
                            holding.recordedValue.asBook(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!holding.isLiability) {
                        Text(
                            "Quantity: " + bookQuantity(holding.quantityScaled),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val verifiedGain = holding.unrealisedGain
                        Text(
                            when {
                                holding.unknownHistoricalCost -> "Historical cost unknown; gain unavailable"
                                verifiedGain != null -> "Unrealised gain/loss: " + verifiedGain.asBook()
                                else -> "Unrealised gain/loss unavailable"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (visible.size < matches.size) {
                TextButton(onClick = { shown = (shown + PAGE_SIZE).coerceAtMost(matches.size) }) {
                    Text("Show more holdings")
                }
            }
        }
    }
}

/** Case-insensitive, multi-term search on visible labels; no IDs or network. */
internal fun matchingRecordedHoldings(
    holdings: List<RecordedHoldingDashboard>, input: String,
): List<RecordedHoldingDashboard> {
    val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return holdings
    return holdings.filter { holding ->
        val label = holding.name + " " + holding.category.replace('_', ' ') +
            " " + (holding.ownerName ?: "")
        tokens.all { label.contains(it, ignoreCase = true) }
    }
}
