package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    val ink = authInk()
    val fields = householdFieldColors()
    val typed = TextStyle(color = ink, fontWeight = FontWeight.Medium)
    Column(
        modifier = Modifier.fillMaxSize().background(authPaper()).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text("Change app password", style = MaterialTheme.typography.headlineMedium, color = ink, fontWeight = FontWeight.Bold)
        Text(
            "A new password does not erase the books. This phone will ask for its screen lock. " +
                "You will need to open the books again afterwards.",
            color = ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
        )
        Text(
            "The app asks for a new password every 180 days. You still need the " +
                "current password and this phone's screen lock. A forgotten password " +
                "uses the recovery code. It does not wipe the books.",
            color = ink, style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = current, onValueChange = { if (it.length <= 128) current = it },
            label = { Text("Current app password", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed, colors = fields,
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = next, onValueChange = { if (it.length <= 128) next = it },
            label = { Text("New app password", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed, colors = fields,
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = repeated, onValueChange = { if (it.length <= 128) repeated = it },
            label = { Text("Repeat new app password", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed, colors = fields,
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = current.isNotEmpty() && next.isNotEmpty() && repeated.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text("Verify device credential and change password")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Back to settings", color = ink)
        }
    }
}
