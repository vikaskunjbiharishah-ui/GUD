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
import `in`.growurdhan.domain.finance.SaleReview

/** FIFO sale result. It never writes a ledger event and never prints an unknown gain as zero. */
@Composable
fun SaleReviewScreen(
    reviews: List<SaleReview.Result>,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Sale result", style = MaterialTheme.typography.headlineMedium)
        Text("This screen does not write a ledger event.")
        Text(SaleReview.FEE_ORDER)
        if (reviews.isEmpty()) {
            Text("No sale is recorded for this view.")
        }
        reviews.forEach { review ->
            Text(review.holdingName, style = MaterialTheme.typography.titleMedium)
            review.warning?.let { Text(it) }
            review.errors.forEach { Text(it) }
            review.lines.forEach { line ->
                Text("Bought ${line.acquiredOn}. Sold ${line.soldOn}.")
                Text("Proceeds ${MoneyEngine.formatRupees(line.proceeds.raw)}")
                val cost = line.cost
                val gain = line.gain
                if (line.unknownCost || gain == null || cost == null) {
                    Text("Need what you paid")
                    Text("Gain not shown")
                } else {
                    Text("Cost ${MoneyEngine.formatRupees(cost.raw)}")
                    Text("Gain ${MoneyEngine.formatRupees(gain.raw)}")
                    Text(if (line.longTerm) "Long term" else "Short term")
                }
            }
            val known = review.knownGain
            if (known != null) Text("FIFO gain ${MoneyEngine.formatRupees(known.raw)}")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Back to portfolio"
            },
        ) { Text("Back to portfolio") }
    }
}
