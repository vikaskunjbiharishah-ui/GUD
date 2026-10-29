package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState
import `in`.growurdhan.core.model.OwnerId

/** Scoped records only. Detailed filing calculations are a later milestone. */
@Composable
fun TaxOwnerScopeScreen(
    state: HouseholdDashboardUiState,
    ownerId: OwnerId?,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Tax records • family scope", style = MaterialTheme.typography.headlineMedium)
        val estimates = (state as? HouseholdDashboardUiState.Ready)?.dashboard?.taxEstimates
            ?.filter { ownerId == null || it.ownerId == ownerId }
            .orEmpty()
        if (estimates.isEmpty()) {
            Text("Your selected owner scope also applies here. This is recorded " +
                "position information, not a tax calculation.")
        } else {
            estimates.forEach { estimate ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        estimate.summary.lines().forEach { line ->
                            Text(line)
                        }
                    }
                }
            }
        }
        when (state) {
            is HouseholdDashboardUiState.Ready -> {
                val members = state.dashboard.byMember.filter {
                    ownerId == null || it.ownerId == ownerId
                }
                if (members.isEmpty()) Text("No recorded members in this scope.")
                members.forEach { member ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(member.displayName ?: "Family owner",
                                style = MaterialTheme.typography.titleMedium)
                            Text("Recorded holdings: ${member.holdings}")
                            Text("Recorded assets: ${member.assets.asBook()}")
                            Text("This is not taxable gains or filing totals.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (state.dashboard.unknownCostHoldings > 0) {
                    Text("Some holdings have unverified original acquisition costs. " +
                        "Taxable gains cannot be inferred from current values.")
                }
            }
            else -> Text("Unlock the vault to view your family's recorded scope.")
        }
        if (estimates.isEmpty()) {
            Text("Detailed capital gains and tax calculations require later tax milestones.",
                style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onBack) { Text("Back to Home") }
    }
}
