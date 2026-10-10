package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState

/** No new vault read: all filters operate on the authorized Home snapshot. */
@Composable
fun PortfolioScreen(
    state: HouseholdDashboardUiState,
    onOpenVault: () -> Unit,
    onRefresh: () -> Unit,
    onManageHoldings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Portfolio", style = MaterialTheme.typography.headlineMedium)
        when (state) {
            HouseholdDashboardUiState.Locked -> {
                Text("Unlock the vault to see your family's recorded holdings.")
                Button(onClick = onOpenVault) { Text("Open vault") }
            }
            HouseholdDashboardUiState.Loading -> Text("Reading the encrypted portfolio.")
            HouseholdDashboardUiState.Unavailable -> {
                Text("The recorded portfolio is unavailable. No values were inferred.")
                OutlinedButton(onClick = onRefresh) { Text("Retry portfolio") }
            }
            is HouseholdDashboardUiState.Ready -> {
                val dashboard = state.dashboard
                // Keep both selections ephemeral. When the vault locks, the Ready
                // subtree leaves composition and the filters disappear with it.
                var category by remember(dashboard) { mutableStateOf<String?>(null) }
                var owner by remember(dashboard) { mutableStateOf<String?>(null) }
                val holdings = dashboard.recordedHoldings
                val owners = holdings.mapNotNull { it.ownerName }.distinct().sorted()
                val categories = holdings.map { it.category }.distinct().sorted()
                val filtered = holdings.filter {
                    (category == null || it.category == category) &&
                        (owner == null || it.ownerName == owner)
                }
                Text(
                    "Recorded as of calculation date ${dashboard.asOf}. Stored prices, " +
                        "not live quotations. Values can be stale; no market refresh is made here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Family assets: ${dashboard.assets.rupees().toPlainString()} INR  ·  " +
                        "Liabilities: ${dashboard.liabilities.rupees().toPlainString()} INR",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (dashboard.integrityWarnings.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Recorded totals have ${dashboard.integrityWarnings.size} " +
                                "reconciliation warning(s). Do not treat them as complete.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                if (holdings.isEmpty()) {
                    Text("No recorded holdings. Register a family account or CAS holding to begin.")
                } else {
                    Text("Filter by category", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = category == null,
                            onClick = { category = null },
                            label = { Text("All categories") },
                            modifier = Modifier.semantics { contentDescription = "Filter all categories" },
                        )
                        categories.forEach { item ->
                            FilterChip(
                                selected = category == item,
                                onClick = { category = item },
                                label = { Text(item.replace('_', ' ')) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Filter category $item"
                                },
                            )
                        }
                    }
                    if (owners.size > 1) {
                        Text("Filter by family member", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = owner == null,
                                onClick = { owner = null },
                                label = { Text("All members") },
                            )
                            owners.forEach { name ->
                                FilterChip(
                                    selected = owner == name,
                                    onClick = { owner = name },
                                    label = { Text(name) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Filter member $name"
                                    },
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        Text("No holdings match these filters. Change the selection to see others.")
                    } else {
                        // A filter change should not inherit an earlier search or
                        // pagination selection from a different asset/member view.
                        key(category, owner) { RecordedHoldingsSection(filtered) }
                    }
                    Text(
                        "${filtered.size} of ${holdings.size} recorded holdings match the filters. " +
                            "Filters do not change family totals.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onManageHoldings) {
                    Text("Manage family accounts and CAS holdings")
                }
                OutlinedButton(onClick = onRefresh) { Text("Refresh from encrypted book") }
            }
        }
    }
}
