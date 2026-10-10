package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.vault.LocalPasswordRules

/** Non-saveable credential fields; never written to a Bundle or Compose save state. */
@Composable
fun ChangeLocalPasswordScreen(
    onChange: (old: String, replacement: String) -> Unit,
    onBack: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    var warning by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Change app password", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Changing the GrowUrDhan login password does not decrypt, replace or " +
                "erase the Keystore-wrapped SQLCipher key. A new Android device " +
                "credential is required. You must sign in again afterward.",
        )
        OutlinedTextField(
            value = current, onValueChange = { if (it.length <= 128) current = it },
            label = { Text("Current app password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = next, onValueChange = { if (it.length <= 128) next = it },
            label = { Text("New app password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = repeated, onValueChange = { if (it.length <= 128) repeated = it },
            label = { Text("Repeat new app password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = current.isNotEmpty() && next.isNotEmpty() && repeated.isNotEmpty(),
            onClick = {
                val issues = LocalPasswordRules.issues(next.toCharArray())
                when {
                    next != repeated -> warning = "The new password confirmation does not match."
                    issues.isNotEmpty() -> warning = issues.first()
                    current == next -> warning = "Choose a different new password."
                    else -> {
                        val old = current
                        val replacement = next
                        current = ""
                        next = ""
                        repeated = ""
                        warning = null
                        onChange(old, replacement)
                    }
                }
            },
        ) { Text("Verify device credential and change password") }
        OutlinedButton(onClick = onBack) { Text("Back to settings") }
    }
}
