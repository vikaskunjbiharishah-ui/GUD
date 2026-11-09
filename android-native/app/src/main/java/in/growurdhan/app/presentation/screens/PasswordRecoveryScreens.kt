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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

/** Code shown once, only in memory. FLAG_SECURE is active on MainActivity. */
@Composable
fun OneTimeRecoveryCodeScreen(
    code: String,
    onAcknowledged: () -> Unit,
    notice: String? = null,
) {
    val ink = authInk()
    Column(Modifier.fillMaxSize().background(authPaper()).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text("Save your recovery code", style = MaterialTheme.typography.headlineMedium, color = ink, fontWeight = FontWeight.Bold)
        Text("Write this 256-bit recovery code on paper and store it separately. " +
            "You will need BOTH this code and your Android device screen lock " +
            "if you forget the GrowUrDhan app password. It is never stored in plaintext.",
            color = ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(code, style = MaterialTheme.typography.titleLarge, color = ink, fontWeight = FontWeight.Bold)
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
        Text("This code does not decrypt an old phone's missing Android Keystore. " +
            "Keep a separate encrypted portable backup for migration to another device.",
            color = ink, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAcknowledged, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("I have saved this code")
        }
    }
}

@Composable
fun RecoveryEnrollmentScreen(onEnable: () -> Unit) {
    val ink = authInk()
    Column(Modifier.fillMaxSize().background(authPaper()).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text("Set up password recovery", style = MaterialTheme.typography.headlineMedium, color = ink, fontWeight = FontWeight.Bold)
        Text("Set up a one-time recovery code so a forgotten app password cannot " +
            "lock you out of this phone. Your current verified app password and a " +
            "new Android screen-lock challenge are both required to create it.",
            color = ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text("You must record the code offline. Android Keystore and the encrypted " +
            "database are not reset, cleared or exported.",
            color = ink, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Key, contentDescription = null)
                Text("Generate recovery code with device lock")
            }
        }
    }
}

@Composable
fun ForgottenPasswordRecoveryScreen(
    message: String?, onReset: (code: String, next: String) -> Unit,
    onBack: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val ink = authInk()
    val fields = householdFieldColors()
    val typed = TextStyle(color = ink, fontWeight = FontWeight.Medium)
    Column(Modifier.fillMaxSize().background(authPaper()).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text("Recover app password", style = MaterialTheme.typography.headlineMedium, color = ink, fontWeight = FontWeight.Bold)
        Text("Requires the one-time recovery code issued while your old app password " +
            "was valid, plus a fresh Android screen-lock confirmation. Recovery keeps " +
            "the existing encrypted family book unchanged.",
            color = ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        OutlinedTextField(code, { if(it.length <= 64) code = it },
            label = { Text("One-time recovery code", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed,
            colors = fields,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { if(it.length <= 128) password = it },
            label = { Text("New app password", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed,
            colors = fields,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(confirmation, { if(it.length <= 128) confirmation = it },
            label = { Text("Confirm new password", color = ink) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = typed,
            colors = fields,
            modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
        Button(enabled = code.length >= 40 && password.isNotBlank() &&
            password == confirmation, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
            val key = code
            val next = password
            code = ""; password = ""; confirmation = ""
            onReset(key,next)
        }) { Text("Verify device identity and recover") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Back to login", color = ink)
        }
        Text("If the original Android Keystore key or the recovery code is lost, " +
            "this phone's vault cannot be unlocked by resetting its password. " +
            "Do not clear app data. Restore a separately verified encrypted backup " +
            "on a new installation instead.", color = ink, style = MaterialTheme.typography.bodyLarge)
    }
}
