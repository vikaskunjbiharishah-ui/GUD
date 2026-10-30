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
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.EmiCatchUp
import `in`.growurdhan.domain.finance.InterestCatchUp
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.finance.VaultFinanceReview

/** Shows an engine review. It never posts INTEREST events. */
@Composable
fun InterestReviewScreen(
    plan: InterestCatchUp? = null,
    lines: List<VaultFinanceReview.InterestLine> = emptyList(),
    onBack: () -> Unit,
) {
    val shown = if (lines.isNotEmpty()) lines else listOfNotNull(plan?.let {
        VaultFinanceReview.InterestLine(ownerId = `in`.growurdhan.core.model.OwnerId("review"), "Interest", it)
    })
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Interest review", style = MaterialTheme.typography.headlineMedium)
        Text("Estimate only. This screen does not write a ledger event. Posting needs a separate confirmation.")
        if (shown.isEmpty()) {
            Text("No holding rate is loaded here yet.")
        } else {
            shown.forEach { line ->
                Text(line.name, style = MaterialTheme.typography.titleMedium)
                line.plan.events.forEach { event ->
                    Text("${event.date} · ${event.notes}")
                    Text(MoneyEngine.formatRupees(event.amount.raw))
                }
                val grown = line.plan.grownBalance
                if (grown != null) {
                    Text("Projected balance ${MoneyEngine.formatRupees(grown.raw)}. Not posted.")
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to Home")
        }
    }
}

/** Shows an EMI catch-up. It never posts REPAYMENT events. A paused loan is absent, not ₹0. */
@Composable
fun EmiReviewScreen(
    plan: EmiCatchUp? = null,
    lines: List<VaultFinanceReview.EmiLine> = emptyList(),
    onBack: () -> Unit,
) {
    val shown = if (lines.isNotEmpty()) lines else listOfNotNull(plan?.let {
        VaultFinanceReview.EmiLine(ownerId = `in`.growurdhan.core.model.OwnerId("review"), "EMI", it)
    })
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("EMI review", style = MaterialTheme.typography.headlineMedium)
        Text("This screen does not write a ledger event. Posting needs a separate confirmation.")
        if (shown.isEmpty()) {
            Text("No loan schedule is loaded here yet. A paused loan is not estimated.")
        } else {
            shown.forEach { line ->
                Text(line.name, style = MaterialTheme.typography.titleMedium)
                Text("Status ${line.plan.status}. Remaining ${line.plan.remainingCount}.")
                line.plan.nextDue?.let { Text("Next due $it") }
                Text("Outstanding ${MoneyEngine.formatRupees(line.plan.outstanding.raw)}")
                line.plan.events.forEach { event ->
                    Text("${event.date} · ${event.notes}")
                    Text(MoneyEngine.formatRupees(event.amount.raw))
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to Home")
        }
    }
}
