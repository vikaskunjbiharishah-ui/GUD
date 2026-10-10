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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.ManualTradeIntent
import `in`.growurdhan.app.presentation.ManualTradeUiState
import `in`.growurdhan.domain.finance.ManualTradeDraft
import `in`.growurdhan.domain.finance.ManualTradeKind
import java.math.BigDecimal
import java.time.LocalDate

/** A visible review step precedes the separate OS device-credential challenge. */
@Composable
fun ManualTradeScreen(
    state: ManualTradeUiState,
    onIntent: (ManualTradeIntent) -> Unit,
    onReturn: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Record a transaction", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Only explicit buys and sells of enrolled assets. Statement snapshots " +
                "must be imported through the separate reviewed CAS workflow. " +
                "Amounts are recorded to four decimal INR places and units to eight.",
            style = MaterialTheme.typography.bodySmall,
        )
        when (state) {
            ManualTradeUiState.Locked -> {
                Text("Unlock the vault before recording a financial transaction.")
                OutlinedButton(onClick = onReturn) { Text("Return to portfolio") }
            }
            ManualTradeUiState.Loading -> Text("Reading enrolled holdings securely.")
            is ManualTradeUiState.Error -> {
                Text(state.message)
                OutlinedButton(onClick = { onIntent(ManualTradeIntent.Refresh) }) {
                    Text("Reload encrypted holdings")
                }
            }
            ManualTradeUiState.Saving -> Text("Saving the approved append-only ledger entry.")
            ManualTradeUiState.Saved -> {
                Text("Transaction recorded. Reopen the portfolio to see refreshed totals.")
                Button(onClick = onReturn) { Text("Return to portfolio") }
            }
            is ManualTradeUiState.AwaitingCredential -> {
                val plan = state.plan
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Confirm the exact transaction", style = MaterialTheme.typography.titleMedium)
                        Text("${plan.event.kind} · ${plan.holdingName} · ${plan.ownerName}")
                        Text("Trade date: ${plan.event.date}")
                        Text(
                            "Units: " + BigDecimal.valueOf(plan.quantity, 8)
                                .stripTrailingZeros().toPlainString(),
                        )
                        Text("Gross: ${plan.gross.rupees().toPlainString()} INR")
                        Text("Charges: ${plan.charges.rupees().toPlainString()} INR")
                        Text(
                            (if (plan.event.kind.name == "BUY") "Total paid: " else "Net received: ") +
                                plan.cashAmount.rupees().toPlainString() + " INR",
                        )
                        Text(
                            "This will append one permanent ledger event; you cannot edit " +
                                "or delete it. A new device credential is required.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = { onIntent(ManualTradeIntent.RequestApproval) }) {
                            Text("Confirm transaction with device lock")
                        }
                        OutlinedButton(onClick = { onIntent(ManualTradeIntent.Cancel); onReturn() }) {
                            Text("Discard draft")
                        }
                    }
                }
            }
            is ManualTradeUiState.Ready -> {
                val eligible = state.book.holdings.filter {
                    it.assetCategory != "LIABILITY" &&
                        state.book.members.any { member -> member.id == it.ownerId }
                }
                if (eligible.isEmpty()) {
                    Text("No enrolled asset holding exists. Enrol a family account or fund first.")
                    OutlinedButton(onClick = onReturn) { Text("Return to portfolio") }
                } else {
                    var selectedId by remember(state.book) { mutableStateOf(eligible.first().id) }
                    var kind by remember(state.book) { mutableStateOf(ManualTradeKind.BUY) }
                    var day by remember(state.book) { mutableStateOf(LocalDate.now().toString()) }
                    var quantity by remember(state.book) { mutableStateOf("") }
                    var price by remember(state.book) { mutableStateOf("") }
                    var fees by remember(state.book) { mutableStateOf("0") }
                    var showHoldings by remember { mutableStateOf(false) }
                    var showKinds by remember { mutableStateOf(false) }
                    val selected = eligible.firstOrNull { it.id == selectedId } ?: eligible.first()
                    Text("Enrolled asset", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = { showHoldings = true },
                        modifier = Modifier.testTag("ManualTradeHoldingPicker"),
                    ) { Text(selected.name) }
                    DropdownMenu(expanded = showHoldings, onDismissRequest = { showHoldings = false }) {
                        eligible.forEach { holding ->
                            DropdownMenuItem(
                                text = { Text(holding.name) },
                                onClick = { selectedId = holding.id; showHoldings = false },
                            )
                        }
                    }
                    Text("Transaction type", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = { showKinds = true },
                        modifier = Modifier.testTag("ManualTradeKindPicker"),
                    ) { Text(kind.name) }
                    DropdownMenu(expanded = showKinds, onDismissRequest = { showKinds = false }) {
                        ManualTradeKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = { kind = option; showKinds = false },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.take(10) },
                        label = { Text("Trade date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradeDate"),
                    )
                    OutlinedTextField(
                        value = quantity, onValueChange = { quantity = it.take(24) },
                        label = { Text("Units (up to 8 decimals)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradeUnits"),
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it.take(24) },
                        label = { Text("Unit price (INR, up to 4 decimals)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradePrice"),
                    )
                    OutlinedTextField(
                        value = fees, onValueChange = { fees = it.take(24) },
                        label = { Text("Transaction charges (INR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradeCharges"),
                    )
                    state.notice?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "A sale requires a verified acquisition ledger. CAS-only balance " +
                            "observations cannot prove saleable units or historical cost.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Recent recorded events", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Events cannot be edited or deleted. CAS snapshots are separate observations.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            val recent = state.book.events.sortedWith(
                                compareByDescending<`in`.growurdhan.core.model.LedgerEvent> {
                                    it.eventDate
                                }.thenByDescending { it.id },
                            )
                            if (recent.isEmpty()) {
                                Text("No transactions have been recorded yet.")
                            } else {
                                val names = state.book.holdings.associateBy { it.id }
                                recent.take(20).forEach { event ->
                                    val holdingName = names[event.holdingId]?.name
                                        ?: "Unmapped recorded holding"
                                    Text(
                                        "${event.eventDate} · ${event.kind.name.replace('_', ' ')} · " +
                                            holdingName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        "Amount: ${event.amount.rupees().toPlainString()} INR",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (recent.size > 20) {
                                    Text(
                                        "Showing 20 of ${recent.size} events. " +
                                            "This is a recent-record summary, not a full audit export.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    Button(onClick = {
                        onIntent(ManualTradeIntent.Stage(
                            ManualTradeDraft(selected.id, kind, day.trim(),
                                quantity.trim(), price.trim(), fees.trim()),
                        ))
                    }) { Text("Review recorded transaction") }
                }
            }
        }
    }
}
