package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.ConfirmedFinancePost
import `in`.growurdhan.domain.finance.EmiCatchUp
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.InterestCatchUp
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.finance.VaultFinanceReview

/** Shows an engine review. The list itself does not write. Post is a separate tap. */
@Composable
fun InterestReviewScreen(
    plan: InterestCatchUp? = null,
    lines: List<VaultFinanceReview.InterestLine> = emptyList(),
    onBack: () -> Unit,
    onConfirm: (List<FinancialEvent>) -> Unit = {},
) {
    val shown = if (lines.isNotEmpty()) lines else listOfNotNull(plan?.let {
        VaultFinanceReview.InterestLine(ownerId = `in`.growurdhan.core.model.OwnerId("review"), "Interest", it)
    })
    var saved by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Interest to record", style = MaterialTheme.typography.headlineMedium)
        Text("These amounts are not in the book yet. They are saved only if you tap the button.")
        if (shown.isEmpty()) {
            Text("Nothing to record. Interest appears here only after a rate is saved on that investment.")
        } else {
            shown.forEach { line ->
                Text(line.name, style = MaterialTheme.typography.titleMedium)
                line.plan.events.forEach { event ->
                    Text("${event.date} · ${event.notes}")
                    Text(MoneyEngine.formatRupees(event.amount.raw))
                }
                val grown = line.plan.grownBalance
                if (grown != null) {
                    Text("Projected balance ${MoneyEngine.formatRupees(grown.raw)}. Not in the book yet.")
                }
            }
            val toPost = shown.flatMap { ConfirmedFinancePost.interestEvents(it) }
            if (toPost.isNotEmpty()) {
                Button(
                    onClick = {
                        saved = true
                        onConfirm(toPost)
                    },
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "Confirm interest post"
                    },
                ) { Text("Save this interest in the book") }
            }
            if (saved) Text("Saved. Tapping again will not add it a second time.")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Back to home" },
        ) {
            Text("Back to Home")
        }
    }
}

/** Shows an EMI catch-up. The list itself does not write. Post is a separate tap. A paused loan is absent, not ₹0. */
@Composable
fun EmiReviewScreen(
    plan: EmiCatchUp? = null,
    lines: List<VaultFinanceReview.EmiLine> = emptyList(),
    onBack: () -> Unit,
    onConfirm: (List<FinancialEvent>) -> Unit = {},
) {
    val shown = if (lines.isNotEmpty()) lines else listOfNotNull(plan?.let {
        VaultFinanceReview.EmiLine(ownerId = `in`.growurdhan.core.model.OwnerId("review"), "EMI", it)
    })
    var saved by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Loan instalments to record", style = MaterialTheme.typography.headlineMedium)
        Text("These amounts are not in the book yet. They are saved only if you tap the button.")
        if (shown.isEmpty()) {
            Text("No loan instalment is due in this view. A paused loan is left out. It is not shown as zero.")
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
            val toPost = shown.flatMap { ConfirmedFinancePost.emiEvents(it) }
            if (toPost.isNotEmpty()) {
                Button(
                    onClick = {
                        saved = true
                        onConfirm(toPost)
                    },
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "Confirm EMI post"
                    },
                ) { Text("Save these instalments in the book") }
            }
            if (saved) Text("Saved. Tapping again will not add it a second time.")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Back to home" },
        ) {
            Text("Back to Home")
        }
    }
}
