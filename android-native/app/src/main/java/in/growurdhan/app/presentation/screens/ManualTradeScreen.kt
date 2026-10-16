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
import `in`.growurdhan.domain.finance.ManualTradePlanner
import `in`.growurdhan.core.model.HoldingId
import java.math.BigDecimal
import java.time.LocalDate

/** A visible review step precedes the separate OS device-credential challenge. */
@Composable
fun ManualTradeScreen(
    state: ManualTradeUiState,
    onIntent: (ManualTradeIntent) -> Unit,
    onReturn: () -> Unit,
    preselectedHoldingId: HoldingId? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Record a transaction", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Explicit transaction, income, split and correction entries use a device-approved " +
                "append-only ledger. Statement snapshots stay in the CAS workflow. " +
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
                        Text("Event date: ${plan.event.date}")
                        plan.event.supersedesEventId?.let { Text("Corrects recorded event: $it") }
                        Text(
                            "Quantity / split numerator: " +
                                (if (plan.event.kind.name == "SPLIT") plan.quantity.toString()
                                    else BigDecimal.valueOf(plan.quantity, 8)
                                        .stripTrailingZeros().toPlainString()),
                        )
                        Text("Gross: ${plan.gross.rupees().toPlainString()} INR")
                        Text("Charges: ${plan.charges.rupees().toPlainString()} INR")
                        Text(
                            "Reviewed cash or stated amount: " +
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
                var search by remember(state.book) { mutableStateOf("") }
                OutlinedTextField(value = search, onValueChange = { search = it.take(120) },
                    label = { Text("Search holding or category") },
                    modifier = Modifier.fillMaxWidth().testTag("ManualHoldingSearch"))
                val eligible = state.book.holdings.filter {
                    state.book.members.any { member -> member.id == it.ownerId } &&
                        (search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                            it.assetCategory.contains(search, ignoreCase = true))
                }
                if (eligible.isEmpty()) {
                    Text("No enrolled asset holding exists. Enrol a family account or fund first.")
                    OutlinedButton(onClick = onReturn) { Text("Return to portfolio") }
                } else {
                    var selectedId by remember(state.book, preselectedHoldingId) {
                        mutableStateOf(eligible.firstOrNull { it.id == preselectedHoldingId }?.id
                            ?: eligible.first().id)
                    }
                    val selected = eligible.firstOrNull { it.id == selectedId } ?: eligible.first()
                    val validKinds = ManualTradePlanner.allowedKindsFor(selected.assetCategory)
                    var kind by remember(selected.id, preselectedHoldingId) {
                        mutableStateOf(if (selected.id == preselectedHoldingId)
                            ManualTradeKind.OPENING_BALANCE
                            else if (ManualTradeKind.BUY in validKinds)
                                ManualTradeKind.BUY else ManualTradeKind.OPENING_BALANCE)
                    }
                    var day by remember(selected.id) { mutableStateOf(LocalDate.now().toString()) }
                    var quantity by remember(selected.id, kind) { mutableStateOf("") }
                    var price by remember(selected.id, kind) { mutableStateOf("") }
                    var fees by remember(selected.id, kind) { mutableStateOf("0") }
                    var showHoldings by remember { mutableStateOf(false) }
                    var showKinds by remember { mutableStateOf(false) }
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
                        ManualTradeKind.entries.filter { it in validKinds }
                            .forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = { kind = option; showKinds = false },
                            )
                        }
                    }
                    var targetId by remember(selected.id, state.book.events) { mutableStateOf<String?>(null) }
                    var showTargets by remember { mutableStateOf(false) }
                    if (kind == ManualTradeKind.CORRECTION) {
                        val correctionTargets = state.book.events.filter { event ->
                            event.holdingId == selected.id && event.kind.name != "SNAPSHOT_ADJUSTMENT" &&
                                event.kind.name != "CORRECTION" &&
                                state.book.events.none { it.supersedesEventId == event.id }
                        }
                        Text("Original immutable event to correct")
                        OutlinedButton(onClick = { showTargets = true },
                            modifier = Modifier.testTag("ManualCorrectionTarget")) {
                            Text(correctionTargets.firstOrNull { it.id == targetId }?.let {
                                "${it.eventDate} · ${it.kind.name} · ${it.id.take(8)}"
                            } ?: "Choose an original event")
                        }
                        DropdownMenu(expanded = showTargets, onDismissRequest = { showTargets = false }) {
                            correctionTargets.forEach { record -> DropdownMenuItem(
                                text = { Text("${record.eventDate} · ${record.kind} · ${record.id.take(8)}") },
                                onClick = { targetId = record.id; showTargets = false },
                            ) }
                        }
                        Text("The replacement takes the original economic event type. " +
                            "Enter corrected units, price/amount, charges and date.")
                    }
                    if (selected.assetCategory == "LIABILITY") {
                        Text("For an existing loan, choose OPENING_BALANCE, enter exactly 1 " +
                            "unit and the documented outstanding debt as unit price. " +
                            "Later REPAYMENT records cash flow but does not infer principal; " +
                            "use a separate dated VALUATION_UPDATE for the lender-confirmed " +
                            "outstanding balance.", style = MaterialTheme.typography.bodySmall)
                    } else if (kind == ManualTradeKind.OPENING_BALANCE) {
                        Text("Record only the documented starting units and historical unit " +
                            "cost. Enter zero cost only when genuinely verified; a statement " +
                            "snapshot is not automatically an acquisition.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.take(10) },
                        label = { Text("Trade date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradeDate"),
                    )
                    OutlinedTextField(
                        value = quantity, onValueChange = { quantity = it.take(24) },
                        label = { Text(when (kind) {
                            ManualTradeKind.SPLIT -> "Split numerator (whole number)"
                            ManualTradeKind.DIVIDEND, ManualTradeKind.INTEREST,
                            ManualTradeKind.REPAYMENT, ManualTradeKind.VALUATION_UPDATE -> "Units (leave blank)"
                            else -> "Units (up to 8 decimals)"
                        }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ManualTradeUnits"),
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it.take(24) },
                        label = { Text(when (kind) {
                            ManualTradeKind.SPLIT -> "Split denominator (whole number)"
                            ManualTradeKind.DIVIDEND, ManualTradeKind.INTEREST,
                            ManualTradeKind.REPAYMENT -> "Income / repayment amount (INR)"
                            ManualTradeKind.VALUATION_UPDATE -> "New quoted unit price (INR)"
                            else -> "Unit price (INR, up to 4 decimals)"
                        }) },
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
                        "Backdated disposals are checked against the complete FIFO lot history. " +
                            "Corrections supersede but never delete original events. " +
                            "Registrar snapshot adjustments cannot be manually entered.",
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
                                recent.filter { it.holdingId == selected.id }.forEach { event ->
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
                                Text("Showing the full recorded timeline for this selected holding.")
                            }
                        }
                    }
                    Button(onClick = {
                        onIntent(ManualTradeIntent.Stage(
                            ManualTradeDraft(selected.id, kind, day.trim(),
                                quantity.trim(), price.trim(), fees.trim(),
                                if (kind == ManualTradeKind.CORRECTION) targetId else null),
                        ))
                    }) { Text("Review recorded transaction") }
                }
            }
        }
    }
}
