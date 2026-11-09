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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.LocalLoginStatus
import `in`.growurdhan.domain.vault.LocalPasswordRules

/**
 * App password gate replaces the usual tabs until verified. Never saveable:
 * Android recreation/background drops typed credentials instead of restoring
 * them through a Bundle, SavedStateHandle, intent or analytics event.
 */
@Composable
fun LocalLoginScreen(
    state: LocalLoginStatus,
    message: String?,
    hasRecoveryCode: Boolean = false,
    onForgotPassword: () -> Unit = {},
    accountEmailRequired: Boolean = false,
    onSubmitWithEmail: ((email: String, password: String, repeated: String?) -> Unit)? = null,
    onSubmit: (password: String, repeated: String?) -> Unit,
) {
    var email by remember(state) { mutableStateOf("") }
    var password by remember(state) { mutableStateOf("") }
    var repeated by remember(state) { mutableStateOf("") }
    val enrolling = state == LocalLoginStatus.NEEDS_SETUP
    val ink = authInk()
    val fields = householdFieldColors()
    val typed = TextStyle(color = ink, fontWeight = FontWeight.Medium)
    Column(
        modifier = Modifier.fillMaxSize().background(authPaper()).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text("GrowUrDhan", style = MaterialTheme.typography.headlineMedium, color = ink, fontWeight = FontWeight.Bold)
        Text(
            if (enrolling) "Choose a password for GrowUrDhan" else "Enter your GrowUrDhan password.",
            style = MaterialTheme.typography.headlineSmall,
            color = ink,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "This phone will also ask for your screen lock. We never see the password.",
            style = MaterialTheme.typography.bodyLarge,
            color = ink,
            fontWeight = FontWeight.Medium,
        )
        if (state == LocalLoginStatus.STORAGE_UNAVAILABLE) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "This phone cannot open the lock that protects your books. Your records were not deleted. " +
                        "Do not clear the app's storage. Use the recovery code and the encrypted backup you saved earlier.",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            if (enrolling || accountEmailRequired) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { if (it.length <= 254) email = it },
                    label = { Text("Owner account email", color = ink, fontWeight = FontWeight.Medium) },
                    singleLine = true,
                    textStyle = typed,
                    colors = fields,
                    modifier = Modifier.fillMaxWidth().testTag("LocalAccountEmail"),
                )
                Text(
                    "Email is checked on this phone. It is not sent to GrowUrDhan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= LocalPasswordRules.MAX_LENGTH) password = it },
                label = { Text(if (enrolling) "New app password" else "App password", color = ink, fontWeight = FontWeight.Medium) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = typed,
                colors = fields,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("LocalAppPassword"),
            )
            if (enrolling) {
                OutlinedTextField(
                    value = repeated,
                    onValueChange = { if (it.length <= LocalPasswordRules.MAX_LENGTH) repeated = it },
                    label = { Text("Confirm app password", color = ink, fontWeight = FontWeight.Medium) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = typed,
                    colors = fields,
                    modifier = Modifier.fillMaxWidth().testTag("LocalAppPasswordRepeat"),
                )
                Text(
                    "At least 8 characters, with a capital letter, a small letter, a number, and a symbol.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Android will ask for your screen lock. Nothing is saved if you cancel.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink,
                    fontWeight = FontWeight.Medium,
                )
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            Button(
                enabled = password.isNotEmpty() && (!enrolling || repeated.isNotEmpty()) &&
                    (!(enrolling || accountEmailRequired) ||
                        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("LocalLoginSubmit"),
                onClick = {
                    val entered = password
                    val confirmation = if (enrolling) repeated else null
                    val ownerEmail = email.trim()
                    email = ""
                    password = ""
                    repeated = ""
                    if (onSubmitWithEmail != null) onSubmitWithEmail(ownerEmail,entered,confirmation)
                    else onSubmit(entered, confirmation)
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Text(if (enrolling) "Save password" else "Open the books")
                }
            }
            if (!enrolling && hasRecoveryCode) {
                OutlinedButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Key, contentDescription = null)
                        Text("Forgot password? Use saved recovery code", color = ink)
                    }
                }
            }
            Text(
                "The password does not replace the backup passphrase. Only a recovery code you already saved, together with this phone's screen lock, can reset it.",
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                fontWeight = FontWeight.Medium,
            )
        }
        }
    }
}
