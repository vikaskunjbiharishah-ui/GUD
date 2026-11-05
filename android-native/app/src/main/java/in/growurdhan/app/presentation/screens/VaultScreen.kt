package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.VaultIntent
import `in`.growurdhan.app.presentation.VaultUiState

@Composable
fun VaultScreen(
    state: VaultUiState,
    onIntent: (VaultIntent) -> Unit,
    onAddFamilyMember: () -> Unit = {},
    onManageHoldings: () -> Unit = {},
    onBackupRecovery: () -> Unit = {},
    onInspectLegacyBackup: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Your books on this phone",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "The family's records stay on this phone. GrowUrDhan cannot read them.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state) {
                    VaultUiState.Loading -> {
                        CircularProgressIndicator()
                        Text("Checking this phone…")
                    }
                    VaultUiState.NotCreated -> {
                        Text("No books on this phone yet", style = MaterialTheme.typography.titleLarge)
                        Text("Creating the books asks for this phone's screen lock.")
                        Button(onClick = { onIntent(VaultIntent.OpenOrCreate) }) {
                            Text("Create the books on this phone")
                        }
                        OutlinedButton(onClick = onBackupRecovery) {
                            Text("Recover from encrypted backup")
                        }
                        OutlinedButton(onClick = onInspectLegacyBackup) {
                            Text("Inspect older .kosha backup (read-only)")
                        }
                    }
                    VaultUiState.Closed -> {
                        Text("Books are locked", style = MaterialTheme.typography.titleLarge)
                        Text("The records stay protected on this phone. Confirm your screen lock to open them.")
                        Button(onClick = { onIntent(VaultIntent.OpenOrCreate) }) {
                            Text("Open the books")
                        }
                        OutlinedButton(onClick = onBackupRecovery) {
                            Text("Backup & recovery")
                        }
                        OutlinedButton(onClick = onInspectLegacyBackup) {
                            Text("Inspect older .kosha backup (read-only)")
                        }
                    }
                    is VaultUiState.Ready -> {
                        Text("Books are open", style = MaterialTheme.typography.titleLarge)
                        Text("Protected on this phone. GrowUrDhan cannot read it.")
                        Text("People: " + state.health.memberCount)
                        Button(onClick = onAddFamilyMember) {
                            Text(
                                if (state.health.memberCount == 0) "Add first family member"
                                else "Manage family members",
                            )
                        }
                        Text("Investments and loans: " + state.health.holdingCount)
                        Button(onClick = onManageHoldings) {
                            Text("Accounts and investments")
                        }
                        Text("Entries in the book: " + state.health.eventCount)
                        Text("Reference checks: " + if (state.health.foreignKeyViolations == 0) "Passed" else "Needs attention")
                        OutlinedButton(onClick = onBackupRecovery) {
                            Text("Backup & recovery")
                        }
                        OutlinedButton(onClick = onInspectLegacyBackup) {
                            Text("Inspect older .kosha backup (read-only)")
                        }
                        OutlinedButton(onClick = { onIntent(VaultIntent.Close) }) { Text("Lock the books") }
                    }
                    is VaultUiState.Error -> {
                        Text("Books unavailable", style = MaterialTheme.typography.titleLarge)
                        Text(state.message)
                        OutlinedButton(onClick = { onIntent(VaultIntent.Refresh) }) { Text("Try again") }
                    }
                }
            }
        }
        var how by remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { how = !how }) {
                    Text(if (how) "Hide how this is protected" else "How this is protected")
                }
                if (how) {
                    Text(
                        "The books are kept in an encrypted database on this phone. The key is created here and wrapped by Android. A password and the screen lock are both required. GrowUrDhan cannot read the books.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
