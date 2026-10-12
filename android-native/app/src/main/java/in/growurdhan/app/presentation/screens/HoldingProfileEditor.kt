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
import `in`.growurdhan.core.model.HoldingRichMetadataContract
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
    val richOriginal = remember(profile.holdingId, profile.richMetadataJson) {
        JSONObject(profile.richMetadataJson)
    }
    var richEdits by remember(profile.holdingId, profile.richMetadataJson) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var richExpanded by remember(profile.holdingId) { mutableStateOf(false) }
    val relevantRich = when (profile.assetCategory) {
        "GOLD_SGB" -> setOf("benchmark_price_scaled", "issue_price_scaled",
            "coupon_rate_bps", "next_coupon_date", "maturity_date", "tag", "tags_json")
        "FIXED_INCOME" -> setOf("interest_rate_bps", "institution_name", "maturity_date",
            "account_number_masked", "tag", "tags_json")
        "REAL_ESTATE" -> setOf("institution_name", "fmv_31jan2018_scaled",
            "exposure_node", "tag", "tags_json")
        "LIABILITY" -> setOf("interest_rate_bps", "monthly_emi_scaled",
            "remaining_emi_count", "emi_status", "institution_name", "tag")
        else -> setOf("exposure_node", "l2_node", "wrapper",
            "allocation_split_json", "tag", "tags_json")
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

        Text("Holding-level rate, maturity, EMI, allocation and tags",
            style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = richExpanded, onCheckedChange = { richExpanded = it })
            Text("Show all original Holding financial fields")
        }
        HoldingRichMetadataContract.editableColumns.sorted().filter { key ->
            richExpanded || key in relevantRich || richOriginal.has(key)
        }.forEach { key ->
            val initial = if (richOriginal.has(key) && !richOriginal.isNull(key)) {
                val source = richOriginal.get(key)
                when {
                    key in HoldingRichMetadataContract.moneyColumns ->
                        BigDecimal.valueOf((source as Number).toLong(), 4).toPlainString()
                    key in HoldingRichMetadataContract.rateColumns ->
                        BigDecimal.valueOf((source as Number).toLong(), 2).toPlainString()
                    else -> source.toString()
                }
            } else ""
            val label = when {
                key in HoldingRichMetadataContract.moneyColumns -> "$" + "{key} (INR)"
                key in HoldingRichMetadataContract.rateColumns -> "$" + "{key} (percent p.a.)"
                key in HoldingRichMetadataContract.jsonColumns -> "$" + "{key} (JSON array)"
                else -> key
            }
            OutlinedTextField(
                value = richEdits[key] ?: initial,
                onValueChange = { richEdits = richEdits + (key to it.take(4000)) },
                label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
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
        val supportedMoney = Regex("[0-9]{1,12}(\\.[0-9]{1,4})?")
        val detailInputsValid = edits.all { (key, value) -> when {
            value.isBlank() -> true
            key == "tdsApplicable" -> value.equals("true", true) || value.equals("false", true)
            key == "incomeDay" -> (value.toIntOrNull() ?: -1) in 1..28
            key == "originalTenorMonths" -> (value.toIntOrNull() ?: -1) >= 0
            key in setOf("areaSqFt", "ownershipPercent") ->
                value.toDoubleOrNull()?.let { it.isFinite() && it >= 0 &&
                    (key != "ownershipPercent" || it <= 100) } ?: false
            else -> true
        } }

        val richValid = richEdits.all { (key, value) ->
            when {
                value.isBlank() -> true
                key in HoldingRichMetadataContract.moneyColumns ->
                    supportedMoney.matches(value)
                key in HoldingRichMetadataContract.rateColumns ->
                    runCatching { BigDecimal(value).movePointRight(2).longValueExact() >= 0 }
                        .getOrDefault(false)
                key in HoldingRichMetadataContract.countColumns ->
                    value.toLongOrNull()?.let { it >= 0 } ?: false
                key in HoldingRichMetadataContract.jsonColumns ->
                    runCatching { org.json.JSONArray(value); true }.getOrDefault(false)
                else -> value.length <= 300
            }
        }
        val valid = name.trim().isNotBlank() && detailInputsValid && richValid &&
            (price.isBlank() || supportedMoney.matches(price)) &&
            (cost.isBlank() || supportedMoney.matches(cost)) &&
            (price.isBlank() || parsedPrice != null && parsedPrice.raw >= 0) &&
            (cost.isBlank() || parsedCost != null && parsedCost.raw >= 0)
        Button(enabled = valid, onClick = {
            val delta = JSONObject()
            edits.forEach { (key, value) ->
                val typed = when {
                    value.isBlank() -> JSONObject.NULL
                    key == "tdsApplicable" -> value.trim().equals("true", true)
                    key in setOf("areaSqFt", "ownershipPercent") ->
                        value.toDoubleOrNull() ?: JSONObject.NULL
                    key in setOf("originalTenorMonths", "incomeDay") ->
                        value.toIntOrNull() ?: JSONObject.NULL
                    else -> value
                }
                delta.put(key, typed)
            }

            val richDelta = JSONObject()
            richEdits.forEach { (key, value) ->
                val typed: Any = when {
                    value.isBlank() -> JSONObject.NULL
                    key in HoldingRichMetadataContract.moneyColumns ->
                        MoneyScaled.fromRupees(BigDecimal(value)).raw
                    key in HoldingRichMetadataContract.rateColumns ->
                        BigDecimal(value).movePointRight(2).longValueExact()
                    key in HoldingRichMetadataContract.countColumns -> value.toLong()
                    else -> value
                }
                richDelta.put(key, typed)
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
                richMetadataJson = richDelta.toString(),
            ))
        }, modifier = Modifier.fillMaxWidth()) { Text("Review holding edits with device lock") }
    }
}
