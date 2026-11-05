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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    hindi: Boolean = false,
    onSubmitWithEmail: ((email: String, password: String, repeated: String?) -> Unit)? = null,
    onSubmit: (password: String, repeated: String?) -> Unit,
) {
    var email by remember(state) { mutableStateOf("") }
    var password by remember(state) { mutableStateOf("") }
    var repeated by remember(state) { mutableStateOf("") }
    val enrolling = state == LocalLoginStatus.NEEDS_SETUP
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GrowUrDhan", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (enrolling) {
                if (hindi) "GrowUrDhan के लिए पासवर्ड चुनें।" else "Choose a password for GrowUrDhan"
            } else {
                if (hindi) "अपना GrowUrDhan पासवर्ड डालें।" else "Enter your GrowUrDhan password."
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            if (hindi) "यह फ़ोन स्क्रीन लॉक भी माँगेगा। हम पासवर्ड कभी नहीं देखते।"
            else "This phone will also ask for your screen lock. We never see the password.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state == LocalLoginStatus.STORAGE_UNAVAILABLE) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (hindi) {
                        "यह फ़ोन आपकी किताबों का ताला नहीं खोल पा रहा। आपके रिकॉर्ड मिटे नहीं हैं। ऐप की स्टोरेज साफ़ न करें। पहले से बचा रिकवरी कोड और एन्क्रिप्टेड बैकअप इस्तेमाल करें।"
                    } else {
                        "This phone cannot open the lock that protects your books. Your records were not deleted. " +
                            "Do not clear the app's storage. Use the recovery code and the encrypted backup you saved earlier."
                    },
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            if (enrolling || accountEmailRequired) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { if (it.length <= 254) email = it },
                    label = { Text("Owner account email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("LocalAccountEmail"),
                )
                Text(
                    "Email is checked on this phone. It is not sent to GrowUrDhan.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= LocalPasswordRules.MAX_LENGTH) password = it },
                label = { Text(if (enrolling) "New app password" else "App password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("LocalAppPassword"),
            )
            if (enrolling) {
                OutlinedTextField(
                    value = repeated,
                    onValueChange = { if (it.length <= LocalPasswordRules.MAX_LENGTH) repeated = it },
                    label = { Text("Confirm app password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("LocalAppPasswordRepeat"),
                )
                Text(
                    "At least 8 characters, with a capital letter, a small letter, a number, and a symbol.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Android will ask for your screen lock. Nothing is saved if you cancel.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                enabled = password.isNotEmpty() && (!enrolling || repeated.isNotEmpty()) &&
                    (!(enrolling || accountEmailRequired) ||
                        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())),
                modifier = Modifier.fillMaxWidth().testTag("LocalLoginSubmit"),
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
            ) { Text(if (enrolling) "Save password" else if (hindi) "किताबें खोलें" else "Open the books") }
            if (!enrolling && hasRecoveryCode) {
                OutlinedButton(onClick = onForgotPassword) {
                    Text("Forgot password? Use saved recovery code")
                }
            }
            Text(
                "The password does not replace the backup passphrase. Only a recovery code you already saved, together with this phone's screen lock, can reset it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
