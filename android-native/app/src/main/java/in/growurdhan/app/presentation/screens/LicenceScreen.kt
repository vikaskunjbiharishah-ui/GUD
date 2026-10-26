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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.LicenceUiState
import `in`.growurdhan.domain.finance.Licence

@Composable
fun LicenceScreen(
    state: LicenceUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val licence = state.licence
    val price = Licence.formatInr(Licence.PRICE_ANNUAL)
    val title = when {
        licence.sideloadBlocked -> "This copy cannot be licensed"
        licence.plan == "expired" -> "Your 30-day trial has ended"
        licence.subscribed -> "Yearly plan"
        else -> "One yearly plan"
    }
    val body = if (licence.sideloadBlocked) {
        "This copy was not installed from Google Play. A copied APK cannot carry a subscription. It lives on the Play account that bought it."
    } else {
        "Thirty days free, then $price a year. That is the only plan. It renews through Google Play and includes the vault and Gmail sync. Cancel in Play; access lasts until the paid year ends."
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (licence.plan == "trial" && !licence.sideloadBlocked) {
            Text("${licence.daysLeft} days left in the trial.", style = MaterialTheme.typography.titleMedium)
        }
        if (!licence.sideloadBlocked && licence.subscribed) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$price a year is on this Play account. The vault and Gmail stay open until this paid year ends.",
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else if (!licence.sideloadBlocked) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Only plan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Yearly plan · $price / year", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "One subscription. The vault stays open and Gmail sync stays on while the year is paid. There is no lifetime licence and no second product.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onPurchase,
                        enabled = !state.busy && Licence.canPurchaseAnnual(licence),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.busy) "Opening Play…" else "Subscribe $price / year")
                    }
                }
            }
        }
        OutlinedButton(onClick = onRestore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Restore purchases on this Play account")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "Payments are processed by Google Play. GrowUrDhan never sees your card.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
