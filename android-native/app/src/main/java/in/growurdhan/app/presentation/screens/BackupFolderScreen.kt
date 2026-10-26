package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.BackupFolderUiState

@Composable
fun BackupFolderScreen(
    state: BackupFolderUiState,
    onPickFolder: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onBackupNow: (CharArray) -> Unit,
    onBack: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Backup folder", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A locked .kosha file can be written to a folder you choose, including Google Drive or OneDrive. " +
                "The password is used once and is not stored. Only the last five copies are kept.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.folderLabel ?: "No folder chosen", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose folder")
                }
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep a locked copy in this folder")
                    Switch(checked = state.enabled, onCheckedChange = onEnabled)
                }
                if (state.lastKept > 0) Text("Last copy kept ${state.lastKept} file(s).")
                state.lastRunAt?.let { Text("Last run $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(1024) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Backup password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
        )
        Button(
            onClick = {
                val secret = password.toCharArray()
                password = ""
                onBackupNow(secret)
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.busy) "Writing…" else "Back up now") }
        state.message?.let { Text(it) }
        state.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
