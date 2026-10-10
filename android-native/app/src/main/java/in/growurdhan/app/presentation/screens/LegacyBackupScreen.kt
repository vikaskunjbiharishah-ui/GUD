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
    data object EnterRecovery : LegacyBackupUiState
    data object Restoring : LegacyBackupUiState
    data object Recovered : LegacyBackupUiState
    data class Error(val message: String) : LegacyBackupUiState
}

sealed interface LegacyBackupIntent {
    data object SelectFile : LegacyBackupIntent
    data object PrepareRestore : LegacyBackupIntent
    class Restore(
        val email: String, val password: String, val pan: String, val dateOfBirth: String,
        val firstQuestionId: String, val firstAnswer: String,
        val secondQuestionId: String, val secondAnswer: String,
    ) : LegacyBackupIntent
    data class Authenticate(val email: String, val password: String) : LegacyBackupIntent
    data object Cancel : LegacyBackupIntent
}

/** M25: inspect first, then require full original identity plus fresh OS credential for empty-vault restore. */
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
                "Inspect first. Restore is available only for an empty native book, " +
                "after PAN, DOB, both security answers and a fresh device challenge. " +
                "Nothing is sent to GrowUrDhan servers.",
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
            LegacyBackupUiState.EnterRecovery -> {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var pan by remember { mutableStateOf("") }
                var dob by remember { mutableStateOf("") }
                var q1 by remember { mutableStateOf("") }
                var a1 by remember { mutableStateOf("") }
                var q2 by remember { mutableStateOf("") }
                var a2 by remember { mutableStateOf("") }
                Text("Restore original backup", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Requires the source vault login, PAN, birth date and both original " +
                        "security-question IDs and answers. Your native vault must contain " +
                        "no existing financial records. A failed restore changes nothing.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(email, { if (it.length <= 254) email = it },
                    label = { Text("Original account email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { if (it.length <= 1024) password = it },
                    label = { Text("Original password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pan, { if (it.length <= 10) pan = it.uppercase() },
                    label = { Text("PAN (10 characters)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dob, { if (it.length <= 10) dob = it },
                    label = { Text("Date of birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Original question IDs: maiden, school, pet, city, teacher, vehicle. " +
                        "Use the same two selected questions as the old app.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(q1, { if (it.length <= 32) q1 = it },
                    label = { Text("First question ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(a1, { if (it.length <= 256) a1 = it },
                    label = { Text("First answer") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(q2, { if (it.length <= 32) q2 = it },
                    label = { Text("Second question ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(a2, { if (it.length <= 256) a2 = it },
                    label = { Text("Second answer") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                Button(
                    enabled = email.isNotBlank() && password.isNotBlank() &&
                        pan.length == 10 && dob.length == 10 &&
                        q1.isNotBlank() && q2.isNotBlank() &&
                        a1.isNotBlank() && a2.isNotBlank(),
                    onClick = {
                        val request = LegacyBackupIntent.Restore(
                            email, password, pan, dob, q1, a1, q2, a2,
                        )
                        email = ""; password = ""; pan = ""; dob = ""
                        q1 = ""; a1 = ""; q2 = ""; a2 = ""
                        onIntent(request)
                    },
                ) { Text("Verify identity, device lock and restore") }
            }
            LegacyBackupUiState.Restoring -> Text(
                "Verifying original identity and restoring encrypted records. " +
                    "Do not close the application."
            )
            LegacyBackupUiState.Recovered -> {
                Text("Legacy book restored successfully into the private SQLCipher vault.")
                Text("Close and reopen the vault to reload the complete family and holdings.")
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
                    "The original archive remains unchanged. Restoration requires the " +
                        "full identity check and an otherwise empty native vault; merging is refused.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { onIntent(LegacyBackupIntent.PrepareRestore) }) {
                    Text("Restore to empty vault")
                }
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
