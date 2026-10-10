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
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        when (state) {
            HouseholdDashboardUiState.Locked -> {
                Text("Unlock the vault to view your family's private financial insights.")
                Button(onClick = onOpenVault) { Text("Open vault") }
            }
            HouseholdDashboardUiState.Loading -> Text("Calculating insights from encrypted records.")
            HouseholdDashboardUiState.Unavailable -> {
                Text("Insights cannot be calculated from this book right now.")
                Button(onClick = onRefresh) { Text("Retry insights") }
            }
            is HouseholdDashboardUiState.Ready -> {
                val dashboard = state.dashboard
                val result = remember(dashboard) { PortfolioInsightsProjector.from(dashboard) }
                Text(
                    "Snapshot calculated ${dashboard.asOf}; valuations use stored prices " +
                        "and may be stale. No internet connection is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Household position", style = MaterialTheme.typography.titleMedium)
                        Text("Assets: ${dashboard.assets.rupees().toPlainString()} INR")
                        Text("Liabilities: ${dashboard.liabilities.rupees().toPlainString()} INR")
                        Text("Net worth: ${dashboard.netWorth.rupees().toPlainString()} INR")
                        val debtRatio = result.debtToAssetsPercent
                        if (result.reconciled && debtRatio != null) {
                            Text(
                                "Debt-to-assets: ${debtRatio.toPlainString()}% " +
                                    "(recorded balances only)",
                            )
                        } else if (result.reconciled) {
                            Text("Debt-to-assets percentage unavailable: no recorded assets.")
                        }
                    }
                }
                if (!result.reconciled) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Allocation withheld", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Household totals, category values, or recorded holdings do not " +
                                    "reconcile. No percentages will be shown until the book is checked.",
                            )
                            if (result.warningCount > 0) {
                                Text("${result.warningCount} integrity warning(s) need review.")
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
                                "Percentages are independently rounded; stored prices are " +
                                    "not live market quotes.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            result.allocations.forEach { item ->
                                val pct = BigDecimal.valueOf(item.shareBasisPoints.toLong(), 2)
                                    .setScale(1, RoundingMode.HALF_UP).toPlainString()
                                Text("${item.category.replace('_', ' ')} · $pct%")
                                LinearProgressIndicator(
                                    progress = { item.shareBasisPoints / 10_000f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    "${item.value.rupees().toPlainString()} INR · " +
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
                            "Historical acquisition cost is unverified for " +
                                "${result.unknownHistoricalCostHoldings} asset holding(s). " +
                                "Total unrealised gain and return percentages are intentionally unavailable.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
                Text(
                    "Not provided: investment returns, XIRR, tax advice or forecasts. " +
                        "Those require verified dated cash flows and up-to-date valuations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRefresh) { Text("Recalculate from encrypted book") }
            }
        }
    }
}
