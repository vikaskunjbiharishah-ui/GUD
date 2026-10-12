package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.growurdhan.core.model.HoldingProfileEdit
import `in`.growurdhan.core.model.HoldingProfileSnapshot
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.ReactV9HoldingDetailsContract
import java.math.BigDecimal
import org.json.JSONObject

/** Source-preserving owner-scoped edit. No synthetic opening transaction. */
@Composable
fun HoldingProfileEditor(profile: HoldingProfileSnapshot, onStage: (HoldingProfileEdit) -> Unit) {
    var name by remember(profile.holdingId) { mutableStateOf(profile.name) }
    var subCategory by remember(profile.holdingId) { mutableStateOf(profile.subCategory.orEmpty()) }
    var notes by remember(profile.holdingId) { mutableStateOf(profile.notes.orEmpty()) }
    var location by remember(profile.holdingId) { mutableStateOf(profile.location.orEmpty()) }
    var isin by remember(profile.holdingId) { mutableStateOf(profile.isin.orEmpty()) }
    var symbol by remember(profile.holdingId) { mutableStateOf(profile.symbolOrFolio.orEmpty()) }
    var acquired by remember(profile.holdingId) { mutableStateOf(profile.acquiredOn.orEmpty()) }
    var price by remember(profile.holdingId) { mutableStateOf("") }
    var priceAsOf by remember(profile.holdingId) { mutableStateOf(profile.priceAsOf.orEmpty()) }
    var priceSource by remember(profile.holdingId) { mutableStateOf(profile.priceSource.orEmpty()) }
    var cost by remember(profile.holdingId) { mutableStateOf("") }
    var archived by remember(profile.holdingId) { mutableStateOf(profile.archived) }
    var closedOn by remember(profile.holdingId) { mutableStateOf(profile.closedOn.orEmpty()) }
    var showAll by remember(profile.holdingId) { mutableStateOf(false) }
    var edits by remember(profile.holdingId, profile.detailsJson) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    val original = remember(profile.holdingId, profile.detailsJson) {
        JSONObject(profile.detailsJson)
    }
    val preferred = when (profile.assetCategory) {
        "MUTUAL_FUND" -> setOf("mfPlan", "mfOption", "mfHeldAs", "dpId", "clientId", "boId")
        "GOLD_SGB" -> setOf("goldHoldMode", "goldPurity", "goldHallmark", "earlyExitDate", "faceValueMinor")
        "FIXED_INCOME", "PPF", "EPF", "FIXED_DEPOSIT", "BOND" ->
            setOf("annualContributionMinor", "compounding", "tdsApplicable", "rateType",
                "originalTenorMonths", "contributionOn")
        "REAL_ESTATE" -> setOf("areaSqFt", "ownership", "ownershipPercent", "linkedHoldingId",
            "monthlyIncomeMinor", "incomeDay", "incomeStart", "incomeEnd")
        "SPECIALISED", "NPS" -> setOf("npsTier", "npsCra", "npsPfm",
            "aifCategory", "commitmentMinor", "capitalCalledMinor", "strategyName",
            "custodian", "altHoldMode", "ifsc")
        "LIABILITY" -> setOf("linkedHoldingId", "rateType", "compounding", "originalTenorMonths")
        else -> emptySet()
    }
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Holding details", style = MaterialTheme.typography.titleMedium)
        Text("All changes require a new device lock approval. Original ledger entries are immutable.")
        OutlinedTextField(name, { name = it.take(200) }, label = { Text("Holding name") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(subCategory, { subCategory = it.take(80) },
            label = { Text("Subcategory (NPS, SSY, REIT, SGB, FD, etc.)") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(acquired, { acquired = it.take(10) },
            label = { Text("Acquired on YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(isin, { isin = it.uppercase().take(12) },
            label = { Text("ISIN (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(symbol, { symbol = it.take(120) },
            label = { Text("Symbol or masked folio") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(location, { location = it.take(300) },
            label = { Text("Location / issuer") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it.take(4000) },
            label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showAll, onCheckedChange = { showAll = it })
            Text("Show all 36 source holding detail attributes")
        }
        ReactV9HoldingDetailsContract.fields.sorted().filter {
            showAll || original.has(it) || it in preferred
        }.forEach { key ->
            val initial = if (original.has(key) && !original.isNull(key)) original.opt(key).toString()
                else ""
            OutlinedTextField(
                value = edits[key] ?: initial,
                onValueChange = { edits = edits + (key to it.take(300)) },
                label = { Text(key) }, modifier = Modifier.fillMaxWidth(),
            )
        }
        Text("Recorded cost basis: " +
            (profile.verifiedCostPerUnit?.rupees()?.toPlainString() ?: "UNKNOWN"))
        OutlinedTextField(cost, { cost = it.take(24) },
            label = { Text("Verified unit cost in INR (optional)") },
            modifier = Modifier.fillMaxWidth())
        Text("Verified cost is metadata, never a fabricated purchase or tax lot.")
        OutlinedTextField(price, { price = it.take(24) },
            label = { Text("Custom quoted unit price in INR (optional)") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(priceAsOf, { priceAsOf = it.take(10) },
            label = { Text("Custom price as-of YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(priceSource, { priceSource = it.take(120) },
            label = { Text("Custom price source") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = archived, onCheckedChange = { archived = it })
            Text("Archive (reversible; history remains stored)")
        }
        OutlinedTextField(closedOn, { closedOn = it.take(10) },
            label = { Text("Close on YYYY-MM-DD (only when position and debt reach zero)") },
            modifier = Modifier.fillMaxWidth())
        val parsedPrice = runCatching {
            price.takeIf { it.isNotBlank() }?.let { MoneyScaled.fromRupees(BigDecimal(it)) }
        }.getOrNull()
        val parsedCost = runCatching {
            cost.takeIf { it.isNotBlank() }?.let { MoneyScaled.fromRupees(BigDecimal(it)) }
        }.getOrNull()
        val valid = name.trim().isNotBlank() &&
            (price.isBlank() || parsedPrice != null && parsedPrice.raw >= 0) &&
            (cost.isBlank() || parsedCost != null && parsedCost.raw >= 0)
        Button(enabled = valid, onClick = {
            val delta = JSONObject()
            edits.forEach { (key, value) ->
                delta.put(key, if (value.isBlank()) JSONObject.NULL else value)
            }
            onStage(HoldingProfileEdit(
                holdingId = profile.holdingId, ownerId = profile.ownerId,
                name = name, subCategory = subCategory.ifBlank { null },
                detailsJson = delta.toString(), notes = notes.ifBlank { null },
                location = location.ifBlank { null }, symbolOrFolio = symbol.ifBlank { null },
                isin = isin.ifBlank { null }, acquiredOn = acquired.ifBlank { null },
                customPrice = parsedPrice, priceAsOf = priceAsOf.ifBlank { null },
                priceSource = priceSource.ifBlank { null },
                verifiedCostPerUnit = parsedCost, archived = archived,
                closedOn = closedOn.ifBlank { null },
            ))
        }, modifier = Modifier.fillMaxWidth()) { Text("Review holding edits with device lock") }
    }
}
