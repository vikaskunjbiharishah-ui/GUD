package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState
import `in`.growurdhan.core.model.OwnerId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/**
 * One picture and one next step. Secondary jobs stay under More.
 * Person selection is the shell chip row, never a second defaulted chip.
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
    onAddHoldings: () -> Unit = {},
    onUpdateFundPrices: () -> Unit = {},
    fundPriceNote: String? = null,
    selectedFamilyOwner: OwnerId? = null,
    onSelectFamilyOwner: (OwnerId?) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Your family's money, in one place.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        when (dashboardState) {
            is HouseholdDashboardUiState.Ready -> {
                ReadyHome(
                    dashboardState = dashboardState,
                    selectedFamilyOwner = selectedFamilyOwner,
                    onSelectFamilyOwner = onSelectFamilyOwner,
                    fundPriceNote = fundPriceNote,
                    onUpdateFundPrices = onUpdateFundPrices,
                    onOpenVault = onOpenVault,
                    onRefreshDashboard = onRefreshDashboard,
                    onOpenStatements = onOpenStatements,
                    onOpenPortfolio = onOpenPortfolio,
                    onOpenInsights = onOpenInsights,
                    onManageFamily = onManageFamily,
                    onOpenAccounts = onOpenAccounts,
                    onOpenTaxScope = onOpenTaxScope,
                    onOpenDocuments = onOpenDocuments,
                    onOpenInterest = onOpenInterest,
                    onOpenEmi = onOpenEmi,
                    onOpenRecords = onOpenRecords,
                    onAddHoldings = onAddHoldings,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenSettings = onOpenSettings,
                )
            }
            HouseholdDashboardUiState.Loading -> Text(
                "Adding up the family book.",
                style = MaterialTheme.typography.bodyLarge,
            )
            HouseholdDashboardUiState.Unavailable -> {
                Text(
                    "The totals did not load. Your records are still on this phone.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onRefreshDashboard,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Try again") }
            }
            HouseholdDashboardUiState.Locked -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Enter your password to see the family's money.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Button(
                    onClick = onOpenVault,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                        contentDescription = "Open the books"
                    },
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open the books")
                }
            }
        }
    }
}

@Composable
private fun ReadyHome(
    dashboardState: HouseholdDashboardUiState.Ready,
    selectedFamilyOwner: OwnerId?,
    onSelectFamilyOwner: (OwnerId?) -> Unit,
    fundPriceNote: String?,
    onUpdateFundPrices: () -> Unit,
    onOpenVault: () -> Unit,
    onRefreshDashboard: () -> Unit,
    onOpenStatements: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenInsights: () -> Unit,
    onManageFamily: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenTaxScope: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenInterest: () -> Unit,
    onOpenEmi: () -> Unit,
    onOpenRecords: () -> Unit,
    onAddHoldings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dashboard = dashboardState.dashboard
    val focused = selectedFamilyOwner?.let { id ->
        dashboard.byMember.firstOrNull { it.ownerId == id }
    }
    val people = dashboard.memberCount.coerceAtLeast(dashboard.byMember.size)
    val holdings = dashboard.holdingCount.coerceAtLeast(dashboard.recordedHoldings.size)
    val month = YearMonth.from(dashboard.asOf)
    val statementThisMonth = (dashboardState.importRecords.map { it.dated } +
        dashboardState.importBatches.map { it.statementDate })
        .any { YearMonth.from(it) == month }
    val primary = when {
        people == 0 -> NextStep("Add yourself", Icons.Filled.Group, onManageFamily)
        holdings == 0 -> NextStep("Add what the family owns", Icons.Filled.Add, onAddHoldings)
        !statementThisMonth -> NextStep("Review this month's statement", Icons.Filled.Description, onOpenStatements)
        else -> NextStep("See what we own", Icons.Filled.AccountBalance, onOpenPortfolio)
    }
    Text(
        if (focused == null) "What the family owns"
        else "${focused.displayName ?: "This person"} only",
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        if (focused == null) "Figures saved in the book as of ${dashboard.asOf}. Not today's market price."
        else "The tiles are this person. Other people are hidden. Figures saved as of ${dashboard.asOf}. Not today's market price.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
    )
    Text(
        fundPriceNote ?: "Mutual fund prices: not updated on this phone yet.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        "Shares and gold: last saved price. Not a live quote.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
    )
    OutlinedButton(
        onClick = onUpdateFundPrices,
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            contentDescription = "Update mutual fund prices"
        },
    ) { Text("Update mutual fund prices") }
    MetricTile(
        label = "Net worth",
        icon = Icons.Filled.Home,
        value = (focused?.netWorth ?: dashboard.netWorth).asBook(),
        note = if (focused == null) "$people people · $holdings investments and loans"
        else "This person only · ${focused.holdings} investments and loans",
        container = MaterialTheme.colorScheme.primaryContainer,
        ink = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    MetricTile(
        label = "What you own",
        icon = Icons.Filled.Savings,
        value = (focused?.assets ?: dashboard.assets).asBook(),
        note = "Saved value",
        container = MaterialTheme.colorScheme.tertiaryContainer,
        ink = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    MetricTile(
        label = "What you owe",
        icon = Icons.Filled.Payments,
        value = (focused?.liabilities ?: dashboard.liabilities).asBook(),
        note = "Loans",
        container = MaterialTheme.colorScheme.secondaryContainer,
        ink = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    if (people == 0) {
        Text(
            "Nothing is saved yet. Start with your own name.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Button(
        onClick = primary.onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            contentDescription = primary.label
        },
    ) {
        Icon(primary.icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(primary.label)
    }
    OutlinedButton(
        onClick = onRefreshDashboard,
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Update totals" },
    ) { Text("Update totals") }
    if (dashboard.unknownCostHoldings > 0 || dashboard.integrityWarnings.isNotEmpty() || !dashboard.isReliable) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Needs a look", style = MaterialTheme.typography.titleMedium)
                if (dashboard.unknownCostHoldings > 0) {
                    Text(
                        "${dashboard.unknownCostHoldings} investments have no purchase price. The gain is not shown. It is not zero.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (dashboard.integrityWarnings.isNotEmpty() || !dashboard.isReliable) {
                    Text(
                        "Some rows could not be added into the totals. Look at those before you rely on this page.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
    if (dashboard.byMember.isNotEmpty() && focused == null) {
        Text("People", style = MaterialTheme.typography.titleMedium)
        dashboard.byMember.forEach { member ->
            TextButton(
                onClick = { onSelectFamilyOwner(member.ownerId) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("${member.displayName ?: "Family member"} — owns ${member.assets.asBook()} · owes ${member.liabilities.asBook()}")
            }
        }
    }
    val focusedName = focused?.displayName
    val listed = dashboard.recordedHoldings.filter {
        selectedFamilyOwner == null || (focusedName != null && it.ownerName == focusedName)
    }
    if (listed.isNotEmpty()) RecordedHoldingsSection(listed)
    var more by remember { mutableStateOf(false) }
    TextButton(
        onClick = { more = !more },
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "More on this phone" },
    ) { Text(if (more) "Hide more" else "More on this phone") }
    if (more) {
        MoreAction("What we own", "Open portfolio", Icons.Filled.AccountBalance, onOpenPortfolio)
        MoreAction("How the money is split", "Open insights", Icons.Filled.PieChart, onOpenInsights)
        MoreAction("Monthly statement", "Open documents", Icons.Filled.Description, onOpenDocuments)
        MoreAction("Interest to record", "Open interest review", Icons.Filled.Savings, onOpenInterest)
        MoreAction("Loan instalments", "Open EMI review", Icons.Filled.Payments, onOpenEmi)
        MoreAction("Saved statements", "Open records", Icons.Filled.Description, onOpenRecords)
        MoreAction("Tax for one person", "Open tax owner scope", Icons.Filled.AccountBalance, onOpenTaxScope)
        MoreAction("Accounts and nominees", "Open accounts", Icons.Filled.AccountBalance, onOpenAccounts)
        MoreAction("People", "Manage family", Icons.Filled.Group, onManageFamily)
        MoreAction("Privacy", "Open privacy", Icons.Filled.Shield, onOpenPrivacy)
        MoreAction("Open the books", "Open the books", Icons.Filled.Lock, onOpenVault)
        MoreAction("Settings", "Open settings", Icons.Filled.Settings, onOpenSettings)
    }
}

@Composable
private fun MoreAction(label: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            contentDescription = description
        },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private data class NextStep(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun MetricTile(
    label: String,
    icon: ImageVector,
    value: String,
    note: String,
    container: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = container, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(note, style = MaterialTheme.typography.bodyLarge, color = ink)
        }
    }
}
