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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class EmergencyExportUiState { READY, PREPARING, SAVED, FAILED }

/** Deliberately plaintext report; the encrypted .gud backup is the default. */
@Composable
fun EmergencyExportScreen(
    status: EmergencyExportUiState,
    onSelectDestination: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Emergency family summary", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unencrypted export", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error)
                Text(
                    "This CSV can be read by anyone who has access to the destination " +
                        "you choose, including a cloud document provider. It is NOT a backup. " +
                        "Use the password-encrypted .gud backup for full financial recovery.",
                )
                Text(
                    "The summary contains family names, asset names, recorded balances " +
                        "and valuation dates. It excludes PAN, folios, account identifiers, " +
                        "passwords, inferred gains, and unreconciled financial totals.",
                    style = MaterialTheme.typography.bodySmall,
                )
                when (status) {
                    EmergencyExportUiState.READY -> Button(onClick = onSelectDestination) {
                        Text("Choose CSV destination and confirm device lock")
                    }
                    EmergencyExportUiState.PREPARING ->
                        Text("Preparing an authorized local export.")
                    EmergencyExportUiState.SAVED ->
                        Text("CSV was written to your chosen destination. Store it securely.")
                    EmergencyExportUiState.FAILED ->
                        Text(
                            "Export could not be confirmed. Check the selected destination " +
                                "before trying again; a partial CSV may exist.",
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            }
        }
        OutlinedButton(onClick = onBack) { Text("Return to settings") }
    }
}
