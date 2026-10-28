package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.domain.finance.PortfolioInsightsProjector
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Allocation and debt insights are strictly read-only. No fabricated
 * XIRR, tax liability, acquisition cost, market price or performance chart.
 */
@Composable
fun InsightsScreen(
    state: HouseholdDashboardUiState,
    onOpenVault: () -> Unit,
    onRefresh: () -> Unit,
    selectedOwnerId: OwnerId? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        when (state) {
            HouseholdDashboardUiState.Locked -> {
                Text("Unlock the vault to see how the family's money is split.")
                Button(onClick = onOpenVault) { Text("Open vault") }
            }
            HouseholdDashboardUiState.Loading -> Text("Adding up the book.")
            HouseholdDashboardUiState.Unavailable -> {
                Text("Insights cannot be calculated from this book right now.")
                Button(onClick = onRefresh) { Text("Retry insights") }
            }
            is HouseholdDashboardUiState.Ready -> {
                val dashboard = state.dashboard
                val focused = selectedOwnerId?.let { id ->
                    dashboard.byMember.firstOrNull { it.ownerId == id }
                }
                if (selectedOwnerId != null) {
                    if (focused == null) {
                        Text("That person is not in this open book.")
                    } else {
                        Text("Showing one person only: ${focused.displayName ?: "Member"}.")
                        Text(
                            "Owns ${focused.assets.asBook()} · owes ${focused.liabilities.asBook()} · net ${focused.netWorth.asBook()}.",
                        )
                        val own = dashboard.recordedHoldings.filter {
                            focused.displayName != null && it.ownerName == focused.displayName
                        }
                        if (own.isEmpty()) Text("No recorded holding for this person.")
                        own.forEach { holding -> Text(holding.name) }
                        Text("Other people's holdings stay hidden in this view.")
                    }
                    Button(onClick = onRefresh) { Text("Refresh") }
                } else {
                val result = remember(dashboard) { PortfolioInsightsProjector.from(dashboard) }
                Text(
                    "As of ${dashboard.asOf}. These are saved values, not today's market price.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Household position", style = MaterialTheme.typography.titleMedium)
                        Text("Assets: ${dashboard.assets.asBook()}")
                        Text("Liabilities: ${dashboard.liabilities.asBook()}")
                        Text("Net worth: ${dashboard.netWorth.asBook()}")
                        val debtRatio = result.debtToAssetsPercent
                        if (result.reconciled && debtRatio != null) {
                            Text(
                                "Loans are ${debtRatio.toPlainString()}% of what you own.",
                            )
                        } else if (result.reconciled) {
                            Text("Loans compared with savings cannot be shown until something is owned.")
                        }
                    }
                }
                if (!result.reconciled) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("The mix is paused", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The totals do not add up yet, so percentages stay hidden.",
                            )
                            if (result.warningCount > 0) {
                                Text("${result.warningCount} item(s) need a look.")
                            }
                        }
                    }
                } else if (!result.hasMeasuredAllocation) {
                    Text("No positive asset values are recorded for allocation analysis.")
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Asset allocation", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Saved values, not today's market price. Each bar is rounded on its own.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            result.allocations.forEach { item ->
                                val pct = BigDecimal.valueOf(item.shareBasisPoints.toLong(), 2)
                                    .setScale(1, RoundingMode.HALF_UP).toPlainString()
                                Text("${item.category.replace('_', ' ')} · $pct%")
                                LinearProgressIndicator(
                                    progress = { item.shareBasisPoints / 10_000f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                                )
                                Text(
                                    "${item.value.asBook()} · " +
                                        "${item.holdingCount} holding(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                if (result.unknownHistoricalCostHoldings > 0) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Purchase price is missing for " +
                                "${result.unknownHistoricalCostHoldings} holding(s), so gain is not shown.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
                Text(
                    "This page does not guess yearly returns or tax. Those need dated cash and today's prices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRefresh) { Text("Refresh") }
                }
            }
        }
    }
}
