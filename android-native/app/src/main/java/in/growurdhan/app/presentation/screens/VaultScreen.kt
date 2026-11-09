package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 96.dp),
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
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
                        Text("Creating the books asks for this phone's screen lock.", color = MaterialTheme.colorScheme.onSurface)
                        BookAction("Create the books on this phone", Icons.Filled.LockOpen, filled = true) {
                            onIntent(VaultIntent.OpenOrCreate)
                        }
                        BookAction("Recover from encrypted backup", Icons.Filled.Restore, filled = false, onBackupRecovery)
                        BookAction("Inspect older .kosha backup (read-only)", Icons.Filled.Shield, filled = false, onInspectLegacyBackup)
                    }
                    VaultUiState.Closed -> {
                        Text("Books are locked", style = MaterialTheme.typography.titleLarge)
                        Text("The records stay protected on this phone. Confirm your screen lock to open them.", color = MaterialTheme.colorScheme.onSurface)
                        BookAction("Open the books", Icons.Filled.LockOpen, filled = true) {
                            onIntent(VaultIntent.OpenOrCreate)
                        }
                        BookAction("Backup & recovery", Icons.Filled.Restore, filled = false, onBackupRecovery)
                        BookAction("Inspect older .kosha backup (read-only)", Icons.Filled.Shield, filled = false, onInspectLegacyBackup)
                    }
                    is VaultUiState.Ready -> {
                        Text("Books are open", style = MaterialTheme.typography.titleLarge)
                        Text("Protected on this phone. GrowUrDhan cannot read it.", color = MaterialTheme.colorScheme.onSurface)
                        Text("People: " + state.health.memberCount, color = MaterialTheme.colorScheme.onSurface)
                        BookAction(
                            if (state.health.memberCount == 0) "Add first family member" else "Manage family members",
                            Icons.Filled.Group,
                            filled = false,
                            onAddFamilyMember,
                        )
                        Text("Investments and loans: " + state.health.holdingCount, color = MaterialTheme.colorScheme.onSurface)
                        BookAction("Accounts and investments", Icons.Filled.AccountBalance, filled = false, onManageHoldings)
                        Text("Entries in the book: " + state.health.eventCount, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Reference checks: " + if (state.health.foreignKeyViolations == 0) "Passed" else "Needs attention",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        BookAction("Backup & recovery", Icons.Filled.Restore, filled = false, onBackupRecovery)
                        BookAction("Inspect older .kosha backup (read-only)", Icons.Filled.Shield, filled = false, onInspectLegacyBackup)
                        BookAction("Lock the books", Icons.Filled.Lock, filled = true) { onIntent(VaultIntent.Close) }
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
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
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

@Composable
private fun BookAction(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
) {
    if (filled) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}
