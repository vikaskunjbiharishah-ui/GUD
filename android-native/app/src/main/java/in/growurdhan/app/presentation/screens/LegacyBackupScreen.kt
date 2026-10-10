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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.data.vault.LegacyBackupPreview

sealed interface LegacyBackupUiState {
    data object Empty : LegacyBackupUiState
    data object EnterCredentials : LegacyBackupUiState
    data object Inspecting : LegacyBackupUiState
    data class Preview(val summary: LegacyBackupPreview) : LegacyBackupUiState
    data class Error(val message: String) : LegacyBackupUiState
}

sealed interface LegacyBackupIntent {
    data object SelectFile : LegacyBackupIntent
    data class Authenticate(val email: String, val password: String) : LegacyBackupIntent
    data object Cancel : LegacyBackupIntent
}

/** Safe inspection only; neither import nor password reset is offered. */
@Composable
fun LegacyBackupScreen(
    state: LegacyBackupUiState,
    onIntent: (LegacyBackupIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Inspect older backup", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Original GrowUrDhan .kosha archives use a different encrypted format. " +
                "This inspection shows record counts only. Nothing is imported, " +
                "merged, overwritten, or sent to GrowUrDhan servers.",
            style = MaterialTheme.typography.bodyMedium,
        )
        when (state) {
            LegacyBackupUiState.Empty -> {
                Button(onClick = { onIntent(LegacyBackupIntent.SelectFile) }) {
                    Text("Select locked .kosha file")
                }
            }
            LegacyBackupUiState.EnterCredentials -> {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = email, onValueChange = { if (it.length <= 254) email = it },
                    label = { Text("Legacy vault email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= 1024) password = it },
                    label = { Text("Legacy backup password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = email.isNotBlank() && password.isNotEmpty(),
                    onClick = {
                        val selectedEmail = email
                        val selectedPassword = password
                        email = ""
                        password = ""
                        onIntent(LegacyBackupIntent.Authenticate(selectedEmail, selectedPassword))
                    },
                ) { Text("Confirm device lock and inspect") }
                Text(
                    "This does not verify the extra PAN, date-of-birth or security-answer " +
                        "requirements for recovery to a different device.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LegacyBackupUiState.Inspecting -> {
                Text("Authenticating and inspecting the selected archive on-device.")
            }
            is LegacyBackupUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { onIntent(LegacyBackupIntent.SelectFile) }) {
                    Text("Choose another locked backup")
                }
            }
            is LegacyBackupUiState.Preview -> {
                val p = state.summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Authenticated archive summary",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Family members: ${p.familyMemberCount}")
                        Text("Holdings: ${p.holdingCount}")
                        Text("Ledger events: ${p.eventCount}")
                        Text("Accounts: ${p.accountCount}")
                        Text("Goals: ${p.goalCount}")
                        Text("Contains CAS history: ${if (p.hasCasHistory) "Yes" else "No"}")
                        Text(
                            "Extended legacy holding details: " +
                                if (p.hasExtendedHoldingDetails) "Present" else "Not detected",
                        )
                    }
                }
                Text(
                    "A lossless migration is not yet available. Goals, CAS history, " +
                        "and legacy holding metadata are not all represented in the " +
                        "current native schema. An automatic import could lose records; " +
                        "the application correctly refuses it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { onIntent(LegacyBackupIntent.SelectFile) }) {
                    Text("Inspect another archive")
                }
            }
        }
        OutlinedButton(onClick = { onIntent(LegacyBackupIntent.Cancel); onBack() }) {
            Text("Return to vault")
        }
    }
}
