package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState
import `in`.growurdhan.core.model.OwnerId
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Honest empty state: M1 does not create a financial vault, import fabricated
 * holdings, or read/mutate the existing Tauri database.
 */
@Composable
fun HomeScreen(
    dashboardState: HouseholdDashboardUiState = HouseholdDashboardUiState.Locked,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenVault: () -> Unit = {},
    onRefreshDashboard: () -> Unit = {},
    onOpenStatements: () -> Unit = {},
    onOpenPortfolio: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onManageFamily: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenTaxScope: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenInterest: () -> Unit = {},
    onOpenEmi: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
    selectedFamilyOwner: OwnerId? = null,
    onSelectFamilyOwner: (OwnerId?) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Your family's money, in one place.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "The numbers stay on this phone. They are the values saved in your book, not today's market price.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (dashboardState) {
            is HouseholdDashboardUiState.Ready -> {
                val dashboard = dashboardState.dashboard
                val focused = selectedFamilyOwner?.let { id ->
                    dashboard.byMember.firstOrNull { it.ownerId == id }
                }
                Text(
                    if (focused == null) "What the family owns today"
                    else "Showing one person only",
                    style = MaterialTheme.typography.titleLarge,
                )
                MetricTile(
                    label = "Net worth",
                    value = (focused?.netWorth ?: dashboard.netWorth).asBook(),
                    note = if (focused == null)
                        "${dashboard.memberCount} people · ${dashboard.holdingCount} holdings"
                    else "This person only · ${focused.holdings} holdings",
                    container = MaterialTheme.colorScheme.primaryContainer,
                    ink = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                MetricTile(
                    label = "What you own",
                    value = (focused?.assets ?: dashboard.assets).asBook(),
                    note = "Saved value",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    ink = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                MetricTile(
                    label = "What you owe",
                    value = (focused?.liabilities ?: dashboard.liabilities).asBook(),
                    note = "Loans",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    ink = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (!dashboard.isReliable) {
                    Text(
                        "Some totals may be incomplete. Open the holdings that need a look.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenVault) { Text("Open vault") }
                    OutlinedButton(onClick = onRefreshDashboard) { Text("Refresh totals") }
                }
            }
            HouseholdDashboardUiState.Loading -> Text("Adding up the family book.")
            HouseholdDashboardUiState.Unavailable -> {
                Text("The overview did not load. Unlock the vault and try again.")
                OutlinedButton(onClick = onRefreshDashboard) { Text("Retry overview") }
            }
            HouseholdDashboardUiState.Locked -> {
                Text("Unlock the vault to see the family's money.")
                Button(onClick = onOpenVault) { Text("Open vault") }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready) {
            val dashboard = dashboardState.dashboard
            val ownerLens = selectedFamilyOwner ?:
                dashboard.byMember.firstOrNull()?.ownerId
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Who is this for?", style = MaterialTheme.typography.titleMedium)
                    Text("The tiles above are the whole family. Pick a person to see only their money.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dashboard.byMember.forEach { m ->
                            FilterChip(ownerLens == m.ownerId,
                                onClick = { onSelectFamilyOwner(m.ownerId) },
                                label = { Text(m.displayName ?: "Family member") })
                        }
                    }
                    val selected = dashboard.byMember.firstOrNull { it.ownerId == ownerLens }
                    if (selected != null) {
                        Text("Recorded assets: " + selected.assets.asBook())
                        Text("Recorded liabilities: " + selected.liabilities.asBook())
                        Text("Recorded net worth: " + selected.netWorth.asBook())
                        Text("Holdings: " + selected.holdings)
                    }
                    OutlinedButton(onClick = onManageFamily) {
                        Text("Manage family, ownership, accounts and goals")
                    }
                }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready &&
            dashboardState.dashboard.unknownCostHoldings > 0
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Purchase price not saved yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        dashboardState.dashboard.unknownCostHoldings.toString() +
                            " holding(s) have no purchase price. We can show what they are worth now, but not the gain or loss.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready &&
            selectedFamilyOwner == null &&
            dashboardState.dashboard.byCategory.isNotEmpty()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("How the money is spread", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Saved values, not today's market price.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    dashboardState.dashboard.byCategory.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                category.category.replace('_', ' '),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                category.value.asBook() + " (" +
                                    category.holdingCount + ")",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready &&
            dashboardState.dashboard.integrityWarnings.isNotEmpty()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Please check a few holdings", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Some rows could not be added into the totals. Look at those holdings before you rely on this page.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        dashboardState.dashboard.integrityWarnings.size.toString() +
                            " item(s) need a look.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready &&
            dashboardState.dashboard.byMember.isNotEmpty()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Each person", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Names show only while the vault is open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val people = dashboardState.dashboard.byMember.filter {
                        selectedFamilyOwner == null || it.ownerId == selectedFamilyOwner
                    }
                    people.forEachIndexed { index, member ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                member.displayName ?: "Family member " + (index + 1),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "Owns " + member.assets.asBook() +
                                    " · owes " + member.liabilities.asBook() +
                                    " · net " + member.netWorth.asBook() +
                                    " · " + member.holdings + " holdings",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        if (dashboardState is HouseholdDashboardUiState.Ready) {
            val focusedName = selectedFamilyOwner?.let { id ->
                dashboardState.dashboard.byMember.firstOrNull { it.ownerId == id }?.displayName
            }
            val listed = dashboardState.dashboard.recordedHoldings.filter {
                selectedFamilyOwner == null || (focusedName != null && it.ownerName == focusedName)
            }
            if (listed.isNotEmpty()) RecordedHoldingsSection(listed)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onOpenPortfolio,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "Open portfolio"
                },
            ) {
                Text("Portfolio")
            }
            OutlinedButton(
                onClick = onOpenInsights,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "Open insights"
                },
            ) {
                Text("Insights")
            }
        }

        OutlinedButton(onClick = onOpenAccounts, modifier = Modifier.fillMaxWidth()) {
            Text("Accounts, custodians and nominees")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onOpenTaxScope,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "Open tax owner scope"
                },
            ) {
                Text("Tax owner scope")
            }
            OutlinedButton(
                onClick = onOpenDocuments,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "Open documents"
                },
            ) {
                Text("Documents / CAS")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenInterest, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("Interest review")
            }
            OutlinedButton(onClick = onOpenEmi, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("EMI review")
            }
        }
        OutlinedButton(
            onClick = onOpenRecords,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                contentDescription = "Open records"
            },
        ) { Text("Records") }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenStatements, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Description, contentDescription = null)
                Text("Statements", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Shield, contentDescription = null)
                Text("Privacy", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    note: String,
    container: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = container, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            )
            Text(note, style = MaterialTheme.typography.bodySmall, color = ink)
        }
    }
}
