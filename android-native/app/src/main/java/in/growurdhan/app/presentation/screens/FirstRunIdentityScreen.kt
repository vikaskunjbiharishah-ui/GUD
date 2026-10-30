package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.vault.FirstRunRegistration

/**
 * Same first-run as the app password. Nothing here is written until the
 * Android screen lock approves it, and answers are not kept in saved state.
 */
@Composable
fun FirstRunIdentityScreen(
    email: String,
    message: String?,
    onCancel: () -> Unit,
    onContinue: (FirstRunRegistration.Draft) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var q1 by remember { mutableStateOf("school") }
    var q2 by remember { mutableStateOf("city") }
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    val draft = FirstRunRegistration.Draft(
        first, last, email, pan, dob, q1, answer1, q2, answer2, consent,
    )
    val issues = FirstRunRegistration.issues(draft)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Who is this account?", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Name, date of birth and two security questions are saved in the encrypted vault on this phone, together with your app password. They are not sent to GrowUrDhan.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Account email: $email", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(first, { if (it.length <= 100) first = it },
            label = { Text("First name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("FirstRunFirstName"))
        OutlinedTextField(last, { if (it.length <= 100) last = it },
            label = { Text("Last name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("FirstRunLastName"))
        OutlinedTextField(pan, { if (it.length <= 10) pan = it.uppercase() },
            label = { Text("PAN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("FirstRunPan"))
        OutlinedTextField(dob, { if (it.length <= 10) dob = it },
            label = { Text("Date of birth YYYY-MM-DD") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("FirstRunDob"))
        Text("First security question", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FirstRunRegistration.questionIds.forEach { id ->
                FilterChip(selected = q1 == id, onClick = { q1 = id }, label = { Text(id) })
            }
        }
        OutlinedTextField(answer1, { if (it.length <= 256) answer1 = it },
            label = { Text("First security answer") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("FirstRunAnswer1"))
        Text("Second security question", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FirstRunRegistration.questionIds.forEach { id ->
                FilterChip(selected = q2 == id, onClick = { q2 = id }, label = { Text(id) })
            }
        }
        OutlinedTextField(answer2, { if (it.length <= 256) answer2 = it },
            label = { Text("Second security answer") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("FirstRunAnswer2"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consent, onCheckedChange = { consent = it },
                modifier = Modifier.testTag("FirstRunConsent"))
            Text("I agree to keep this profile in the encrypted vault on this phone.")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = issues.isEmpty(),
            onClick = {
                val submitted = draft
                first = ""; last = ""; pan = ""; dob = ""
                answer1 = ""; answer2 = ""; consent = false
                onContinue(submitted)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("FirstRunContinue").semantics {
                contentDescription = "Save the account after the device lock"
            },
        ) { Text("Save account with device lock") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
