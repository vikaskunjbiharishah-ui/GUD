package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen() {
    var technical by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Privacy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        PrivacyFact(
            title = "On this phone",
            detail = "The family's book is kept on this phone. It is not uploaded.",
        )
        PrivacyFact(
            title = "Locked",
            detail = "GrowUrDhan's password and this phone's screen lock are both required. The password is not sent to us.",
        )
        PrivacyFact(
            title = "Gmail, only if you connect it",
            detail = "Statement mail is read on this phone after you agree. The book is still not uploaded.",
        )
        TextButton(onClick = { technical = !technical }) {
            Text(if (technical) "Hide technical detail" else "Technical detail")
        }
        if (technical) {
            Text(
                "The book uses an encrypted database. The password check uses a device-bound Keystore HMAC over a PBKDF2 verifier. The password does not decrypt the database key. Display choices stay on this phone.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun PrivacyFact(title: String, detail: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
