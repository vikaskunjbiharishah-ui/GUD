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
    onSubmit: (password: String, repeated: String?) -> Unit,
) {
    var password by remember(state) { mutableStateOf("") }
    var repeated by remember(state) { mutableStateOf("") }
    val enrolling = state == LocalLoginStatus.NEEDS_SETUP
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GrowUrDhan", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (enrolling) "Create your private app password"
            else "Unlock your private books",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Your password is checked on this phone. Opening the encrypted " +
                "vault also requires your Android device screen lock.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state == LocalLoginStatus.STORAGE_UNAVAILABLE) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Local login evidence or its Android Keystore key is unavailable. " +
                        "Your financial database has not been reset. Do not clear app data. " +
                        "Use a verified portable backup and supported recovery procedure.",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
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
                    "Use at least eight characters, including uppercase, lowercase, " +
                        "a digit and a special character. This matches the original " +
                        "GrowUrDhan password complexity policy.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Your existing native financial records, if any, will not be " +
                        "recreated or overwritten. An Android credential confirmation " +
                        "is required before the first password is registered.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                enabled = password.isNotEmpty() && (!enrolling || repeated.isNotEmpty()),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val entered = password
                    val confirmation = if (enrolling) repeated else null
                    password = ""
                    repeated = ""
                    onSubmit(entered, confirmation)
                },
            ) { Text(if (enrolling) "Protect app with device credential" else "Verify app password") }
            if (!enrolling && hasRecoveryCode) {
                OutlinedButton(onClick = onForgotPassword) {
                    Text("Forgot password? Use saved recovery code")
                }
            }
            Text(
                "The app password does not decrypt SQLCipher or replace the backup " +
                    "passphrase. Only a previously issued recovery code together " +
                    "with your Android screen lock can reset this local password. " +
                    "Keep a separately verified encrypted backup in a safe place.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
