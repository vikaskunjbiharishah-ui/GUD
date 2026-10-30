package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.market.PricedClose

/** Shows provenance. A missing close is a sentence, never ₹0. */
@Composable
fun ManualPriceScreen(priced: PricedClose, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Price", style = MaterialTheme.typography.headlineMedium)
        Text(priced.label)
        Text("Source ${priced.provenance.name.lowercase().replace('_', ' ')}.")
        val amount = priced.amount
        if (amount != null) {
            Text(MoneyEngine.formatRupees(amount.raw))
        }
        if (priced.stale && amount != null) Text("This price is stale.")
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Back from price"
            },
        ) { Text("Back") }
    }
}
