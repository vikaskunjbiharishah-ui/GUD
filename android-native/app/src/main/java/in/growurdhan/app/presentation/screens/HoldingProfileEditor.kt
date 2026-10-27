package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.growurdhan.core.model.HoldingProfileEdit
import `in`.growurdhan.core.model.FamilyGoalProfile
import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.domain.vault.OriginalHoldingFormPolicy
import `in`.growurdhan.domain.market.CachedListedSecurity
import `in`.growurdhan.domain.market.LocalOfficialSecurityCatalog
import `in`.growurdhan.domain.market.QuoteCondition
import `in`.growurdhan.core.model.HoldingProfileSnapshot
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.ReactV9HoldingDetailsContract
import `in`.growurdhan.core.model.HoldingRichMetadataContract
import java.math.BigDecimal
import org.json.JSONObject

/** Source-preserving owner-scoped edit. No synthetic opening transaction. */
@Composable
fun HoldingProfileEditor(
    profile: HoldingProfileSnapshot,
    onStage: (HoldingProfileEdit) -> Unit,
    goals: List<FamilyGoalProfile> = emptyList(),
    onStageGoal: (String, Boolean) -> Unit = { _, _ -> },
    creating: Boolean = false,
    ownedHoldings: List<Holding> = emptyList(),
    ownedProfiles: List<HoldingProfileSnapshot> = emptyList(),
    verifiedOfficialCatalog: List<CachedListedSecurity> = emptyList(),
) {
    var name by remember(profile.holdingId) { mutableStateOf(profile.name) }
    var subCategory by remember(profile.holdingId) { mutableStateOf(profile.subCategory.orEmpty()) }
    var notes by remember(profile.holdingId) { mutableStateOf(profile.notes.orEmpty()) }
    var location by remember(profile.holdingId) { mutableStateOf(profile.location.orEmpty()) }
    var isin by remember(profile.holdingId) { mutableStateOf(profile.isin.orEmpty()) }
    var securityQuery by remember(profile.holdingId) { mutableStateOf("") }
    var symbol by remember(profile.holdingId) { mutableStateOf(profile.symbolOrFolio.orEmpty()) }
    var acquired by remember(profile.holdingId) { mutableStateOf(profile.acquiredOn.orEmpty()) }
    var price by remember(profile.holdingId) { mutableStateOf("") }
    var priceAsOf by remember(profile.holdingId) { mutableStateOf(profile.priceAsOf.orEmpty()) }
    var priceSource by remember(profile.holdingId) { mutableStateOf(profile.priceSource.orEmpty()) }
    var cost by remember(profile.holdingId) { mutableStateOf("") }
    var resetCost by remember(profile.holdingId) { mutableStateOf(false) }
    var archived by remember(profile.holdingId) { mutableStateOf(profile.archived) }
    var closedOn by remember(profile.holdingId) { mutableStateOf(profile.closedOn.orEmpty()) }
    var showAll by remember(profile.holdingId) { mutableStateOf(false) }
    var edits by remember(profile.holdingId, profile.detailsJson) {
        mutableStateOf<Map<String, String>>(if (creating) when {
            profile.assetCategory == "FIXED_INCOME" && profile.subCategory == "Bank FD" ->
                mapOf("compounding" to "CUMULATIVE")
            profile.assetCategory == "FIXED_INCOME" && profile.subCategory == "NPS" ->
                mapOf("npsTier" to "I")
            profile.assetCategory == "GOLD_SGB" &&
                profile.subCategory == "Physical gold (24K)" ->
                mapOf("goldPurity" to "24K")
            profile.assetCategory == "SPECIALISED" && profile.subCategory == "AIF" ->
                mapOf("altHoldMode" to "LUMPSUM")
            profile.assetCategory == "REAL_ESTATE" &&
                profile.subCategory in setOf("REIT", "InvIT") ->
                mapOf("altHoldMode" to "DEMAT")
            profile.assetCategory == "MUTUAL_FUND" -> mapOf("mfHeldAs" to "FOLIO")
            else -> emptyMap()
        } else emptyMap())
    }
    val richOriginal = remember(profile.holdingId, profile.richMetadataJson) {
        JSONObject(profile.richMetadataJson)
    }
    var richEdits by remember(profile.holdingId, profile.richMetadataJson) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var richExpanded by remember(profile.holdingId) { mutableStateOf(false) }
    val sourceInstrument = remember(profile.holdingId, profile.instrumentMetadataJson) {
        JSONObject(profile.instrumentMetadataJson)
    }
    var instrumentEdits by remember(profile.holdingId, profile.instrumentMetadataJson) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    val sourceInstrumentFields = listOf("amc", "rta", "amfiCode", "advisor",
        "schemeType", "ucc", "exchange", "dpName", "nextEmiDate")
    val detailMoneyKeys = setOf(
        "annualContributionMinor", "faceValueMinor", "commitmentMinor",
        "capitalCalledMinor", "monthlyIncomeMinor",
    )
    val inrPattern = Regex("[0-9]{1,12}(\\.[0-9]{1,4})?")
    val enumSelections = mapOf(
        "goldHoldMode" to setOf("DEMAT","CERTIFICATE"),
        "goldPurity" to setOf("24K","22K","999"),
        "compounding" to setOf("MONTHLY","QUARTERLY","HALF_YEARLY","YEARLY","CUMULATIVE"),
        "npsTier" to setOf("I","II"),
        "npsCra" to setOf("PROT","CAMS","KFIN"),
        "aifCategory" to setOf("I","II","III"),
        "ownership" to setOf("SELF","JOINT","HUF","SPOUSE"),
        "rateType" to setOf("FLOATING","FIXED"),
        "mfPlan" to setOf("DIRECT","REGULAR"),
        "mfOption" to setOf("GROWTH","IDCW"),
        "mfHeldAs" to setOf("FOLIO","DEMAT"),
        "depository" to setOf("NSDL","CDSL"),
        "altHoldMode" to setOf("DEMAT","LUMPSUM"),
    )
    val relevantRich = when (profile.assetCategory) {
        "GOLD_SGB" -> setOf("benchmark_price_scaled", "issue_price_scaled",
            "coupon_rate_bps", "next_coupon_date", "maturity_date", "tag", "tags_json")
        "FIXED_INCOME" -> setOf("interest_rate_bps", "institution_name", "maturity_date",
            "account_number_masked", "tag", "tags_json")
        "REAL_ESTATE" -> setOf("institution_name", "fmv_31jan2018_scaled",
            "exposure_node", "tag", "tags_json")
        "LIABILITY" -> setOf("interest_rate_bps", "monthly_emi_scaled",
            "remaining_emi_count", "emi_status", "institution_name",
            "account_number_masked", "tag")
        else -> setOf("exposure_node", "l2_node", "wrapper",
            "allocation_split_json", "tag", "tags_json")
    }
    var linkedMenuOpen by remember(profile.holdingId) { mutableStateOf(false) }
    val linkedAlternatives = ownedHoldings.filter {
        it.ownerId == profile.ownerId && it.id != profile.holdingId &&
            when (profile.assetCategory) {
                "LIABILITY" -> it.assetCategory == "REAL_ESTATE"
                "REAL_ESTATE" -> profile.subCategory !in setOf("REIT", "InvIT") &&
                    it.assetCategory == "LIABILITY"
                else -> false
            }
    }
    val original = remember(profile.holdingId, profile.detailsJson) {
        JSONObject(profile.detailsJson)
    }
    val preferred = OriginalHoldingFormPolicy.preferredDetailKeys(
        profile.assetCategory, profile.subCategory.orEmpty(),
    )
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Holding details", style = MaterialTheme.typography.titleMedium)
        Text("All changes require a new device lock approval. Original ledger entries are immutable.")
        OutlinedTextField(name, { name = it.take(200) }, label = { Text("Holding name") },
            modifier = Modifier.fillMaxWidth())
        if (creating && profile.assetCategory in setOf("EQUITY", "MUTUAL_FUND")) {
            Text("Official NSE/BSE/AMFI close/NAV lookup (cached and offline)",
                style = MaterialTheme.typography.titleSmall)
            if (verifiedOfficialCatalog.isEmpty()) {
                Text("No verified local official catalog is available on this device. " +
                    "You may record a manually identified security, but no exchange/NAV " +
                    "quote will be invented. Official catalog downloads belong to M30.",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = securityQuery,
                    onValueChange = { securityQuery = it.take(100) },
                    label = { Text("Search verified securities by name, ISIN or symbol") },
                    modifier = Modifier.fillMaxWidth().testTag("OfficialSecuritySearch"),
                )
                LocalOfficialSecurityCatalog.search(
                    verifiedOfficialCatalog, profile.assetCategory, securityQuery,
                    java.time.LocalDate.now(),
                ).forEach { row ->
                    val condition = row.condition(java.time.LocalDate.now())
                    OutlinedButton(
                        onClick = {
                            name = row.name
                            isin = row.isin
                            symbol = row.symbol.orEmpty()
                            instrumentEdits = instrumentEdits + mapOf(
                                "amfiCode" to row.amfiCode.orEmpty(),
                                "exchange" to if (row.source.name in setOf("NSE", "BSE"))
                                    row.source.name else "",
                            )
                            if (condition == QuoteCondition.CURRENT) {
                                price = row.closePrice.rupees().toPlainString()
                                priceAsOf = row.closeDate.toString()
                                priceSource = row.source.name +
                                    (if (row.source.name == "AMFI") "_OFFICIAL_NAV"
                                        else "_OFFICIAL_CLOSE")
                            } else {
                                price = ""
                                priceAsOf = ""
                                priceSource = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(row.name + " · " + row.isin + " · " + row.source.name +
                            " · " + row.closeDate.toString() +
                            if (condition == QuoteCondition.STALE) " (stale, price not used)"
                            else " (dated close/NAV)")
                    }
                }
            }
        }
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
            val initial = if (original.has(key) && !original.isNull(key)) {
                val stored = original.opt(key).toString()
                if (key in detailMoneyKeys)
                    runCatching { BigDecimal(stored).movePointLeft(4).toPlainString() }
                        .getOrDefault(stored)
                else stored
            } else ""
            if (key == "linkedHoldingId" && linkedAlternatives.isNotEmpty()) {
                OutlinedButton(onClick = { linkedMenuOpen = true }) {
                    Text("Linked " +
                        (if (profile.assetCategory == "LIABILITY") "property: " else "loan: ") +
                        (linkedAlternatives.firstOrNull {
                            it.id.raw == (edits[key] ?: initial)
                        }?.name ?: "None"))
                }
                DropdownMenu(expanded = linkedMenuOpen,
                    onDismissRequest = { linkedMenuOpen = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = {
                        edits = edits + (key to ""); linkedMenuOpen = false
                    })
                    linkedAlternatives.forEach { linked ->
                        DropdownMenuItem(text = { Text(linked.name) }, onClick = {
                            edits = edits + (key to linked.id.raw)
                            linkedMenuOpen = false
                        })
                    }
                }
            } else {
                OutlinedTextField(
                    value = edits[key] ?: initial,
                    onValueChange = { edits = edits + (key to it.take(300)) },
                    label = { Text(plainDetailLabel(key, key in detailMoneyKeys, enumSelections[key])) }, modifier = Modifier.fillMaxWidth()
                        .testTag("HoldingDetails_" + key),
                )
            }
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
            val label = plainRichLabel(key)
            OutlinedTextField(
                value = richEdits[key] ?: initial,
                onValueChange = { richEdits = richEdits + (key to it.take(4000)) },
                label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (profile.assetCategory == "LIABILITY") {
            Text("For a loan, fill the monthly EMI, how many EMIs are left, the interest rate, the next EMI date, and whether it is current. The opening balance is a separate reviewed entry. Paying an EMI does not by itself reduce the principal.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (profile.assetCategory == "FIXED_INCOME") {
            Text("FD: choose compounding and maturity. For PPF / SSY / EPF " +
                "record actual deposits and contribution dates, not estimated interest. " +
                "NPS is market-valued, not a fixed-interest account.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (profile.assetCategory == "GOLD_SGB") {
            Text("SGB: review issue price, coupon, early exit and maturity. " +
                "Physical gold: select purity, hallmark and quoted price per gram.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (profile.assetCategory == "SPECIALISED" || profile.assetCategory == "REAL_ESTATE") {
            Text("AIF / REIT / InvIT: set DEMAT or LUMPSUM and any actual " +
                "commitment, capital called, ownership percentage or lease income. " +
                "Private cash amounts are not fabricated unit lots.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("Original instrument and registrar fields",
            style = MaterialTheme.typography.titleSmall)
        Text("These are owner-private recorded metadata, not verified issuer connections.",
            style = MaterialTheme.typography.bodySmall)
        sourceInstrumentFields.filter { key ->
            key in setOf("amc", "rta", "amfiCode", "schemeType", "exchange") ||
                (profile.assetCategory == "LIABILITY" && key == "nextEmiDate") ||
                sourceInstrument.has(key) || richExpanded
        }.forEach { key ->
            OutlinedTextField(
                value = instrumentEdits[key] ?: sourceInstrument.optString(key),
                onValueChange = { value ->
                    instrumentEdits = instrumentEdits + (key to value.take(200))
                },
                label = { Text(key) }, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!creating && goals.isNotEmpty()) {
            Text("Related family goals", style = MaterialTheme.typography.titleSmall)
            Text("Each association is reviewed and approved with a fresh device lock.",
                style = MaterialTheme.typography.bodySmall)
            goals.filter { it.ownerId == null || it.ownerId == profile.ownerId }
                .forEach { goal ->
                    val selected = profile.holdingId in goal.holdingIds
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selected,
                            onCheckedChange = { checked -> onStageGoal(goal.id, checked) },
                            modifier = Modifier.testTag("HoldingGoal_" + goal.id))
                        Text(goal.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
        }
        Text("Recorded cost basis: " +
            (profile.verifiedCostPerUnit?.rupees()?.toPlainString() ?: "UNKNOWN"))
        OutlinedTextField(cost, { cost = it.take(24) },
            label = { Text("Verified unit cost in INR (optional)") },
            modifier = Modifier.fillMaxWidth())
        Text("Verified cost is metadata, never a fabricated purchase or tax lot.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = resetCost, onCheckedChange = { resetCost = it })
            Text("Mark acquisition cost as UNKNOWN (does not erase ledger history)")
        }
        OutlinedTextField(price, { price = it.take(24) },
            label = { Text("Custom quoted unit price in INR (optional)") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(priceAsOf, { priceAsOf = it.take(10) },
            label = { Text("Custom price as-of YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(priceSource, { priceSource = it.take(120) },
            label = { Text("Custom price source") }, modifier = Modifier.fillMaxWidth())
        if (!creating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = archived, onCheckedChange = { archived = it })
                Text("Archive (reversible; history remains stored)")
            }
            OutlinedTextField(closedOn, { closedOn = it.take(10) },
                label = { Text("Close on YYYY-MM-DD (only when position and debt reach zero)") },
                modifier = Modifier.fillMaxWidth())
        } else Text("New holdings start open, unarchived and with no invented transaction.",
            style = MaterialTheme.typography.bodySmall)
        val parsedPrice = runCatching {
            price.takeIf { it.isNotBlank() }?.let { MoneyScaled.fromRupees(BigDecimal(it)) }
        }.getOrNull()
        val parsedCost = runCatching {
            cost.takeIf { it.isNotBlank() }?.let { MoneyScaled.fromRupees(BigDecimal(it)) }
        }.getOrNull()
        val supportedMoney = Regex("[0-9]{1,12}(\\.[0-9]{1,4})?")
        val detailInputsValid = edits.all { (key, value) -> when {
            value.isBlank() -> true
            key in detailMoneyKeys -> inrPattern.matches(value) &&
                runCatching { MoneyScaled.fromRupees(BigDecimal(value)).raw >= 0L }
                    .getOrDefault(false)
            key in setOf("earlyExitDate", "contributionOn", "incomeStart", "incomeEnd") ->
                runCatching { java.time.LocalDate.parse(value).year in 1900..2300 }
                    .getOrDefault(false)
            key == "tdsApplicable" -> value.equals("true", true) || value.equals("false", true)
            key == "incomeDay" -> (value.toIntOrNull() ?: -1) in 1..28
            key == "originalTenorMonths" -> (value.toIntOrNull() ?: -1) >= 0
            key in setOf("areaSqFt", "ownershipPercent") ->
                value.toDoubleOrNull()?.let { it.isFinite() && it >= 0 &&
                    (key != "ownershipPercent" || it <= 100) } ?: false
            key in enumSelections -> value in enumSelections.getValue(key)
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
                key in HoldingRichMetadataContract.dateColumns ->
                    runCatching { java.time.LocalDate.parse(value).year in 1900..2300 }
                        .getOrDefault(false)
                key == "emi_status" -> value in setOf("Paying","Paused","Stopped")
                key == "account_number_masked" ->
                    value.contains("*") || value.contains("•")
                else -> value.length <= 300
            }
        }
        val instrumentValid = instrumentEdits.all { (key, value) ->
            value.length <= 200 && value.none { it.isISOControl() } &&
                (key != "nextEmiDate" || value.isBlank() ||
                    runCatching { java.time.LocalDate.parse(value).year in 1900..2300 }
                        .getOrDefault(false))
        }
        val priceProvenanceValid = price.isBlank() ||
            (priceSource.isNotBlank() && runCatching {
                java.time.LocalDate.parse(priceAsOf).let {
                    !it.isAfter(java.time.LocalDate.now()) && it.year >= 1900
                }
            }.getOrDefault(false))
        val advancedFacts = mutableMapOf<String, String?>()
        for (json in listOf(original, richOriginal, sourceInstrument)) {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                advancedFacts[key] = if (json.isNull(key)) null else json.get(key).toString()
            }
        }
        edits.forEach { (key, value) -> advancedFacts[key] = value }
        richEdits.forEach { (key, value) -> advancedFacts[key] = value }
        instrumentEdits.forEach { (key, value) -> advancedFacts[key] = value }
        val missingAdvanced = if (creating) OriginalHoldingFormPolicy.missing(
            profile.assetCategory, profile.subCategory.orEmpty(), advancedFacts,
        ) else emptyList()
        if (missingAdvanced.isNotEmpty()) {
            Text("Complete these required fields: " +
                missingAdvanced.joinToString { it.label },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        val duplicateRecordedIsin = if (creating && isin.isNotBlank()) {
            ownedProfiles.firstOrNull { it.ownerId == profile.ownerId &&
                it.isin.equals(isin.trim(), ignoreCase = true) &&
                it.holdingId != profile.holdingId }
        } else null
        if (duplicateRecordedIsin != null) {
            Text("This security is already recorded for the selected family member: " +
                duplicateRecordedIsin.name + ". Open its transaction history " +
                "and record another BUY instead of creating a second holding.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        val valid = duplicateRecordedIsin == null &&
            missingAdvanced.isEmpty() &&
            name.trim().isNotBlank() && instrumentValid &&
            priceProvenanceValid &&
            ! (resetCost && cost.isNotBlank()) &&
            detailInputsValid && richValid &&
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
                    key in detailMoneyKeys ->
                        MoneyScaled.fromRupees(BigDecimal(value)).raw.toString()
                    else -> OriginalHoldingFormPolicy.normalizeStoredDetail(key, value)
                }
                delta.put(key, typed)
            }

            val instrumentDelta = JSONObject()
            instrumentEdits.forEach { (key, value) ->
                instrumentDelta.put(key, if (value.isBlank()) JSONObject.NULL else value)
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
                clearVerifiedCost = resetCost,
                closedOn = closedOn.ifBlank { null },
                richMetadataJson = richDelta.toString(),
                instrumentMetadataJson = instrumentDelta.toString(),
            ))
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (creating) "Review and register complete holding" else
                "Review holding edits with device lock")
        }
    }
}

private fun plainDetailLabel(key: String, money: Boolean, choices: Collection<String>?): String {
    val name = friendlyField(key)
    val unit = if (money && !name.contains("₹")) "$name (₹)" else name
    return if (choices.isNullOrEmpty()) unit else "$unit (${choices.joinToString(" / ")})"
}

private fun plainRichLabel(key: String): String = when {
    key in HoldingRichMetadataContract.moneyColumns -> friendlyField(key) + " (₹)"
    key in HoldingRichMetadataContract.rateColumns -> friendlyField(key) + " (% a year)"
    key in HoldingRichMetadataContract.jsonColumns -> friendlyField(key)
    else -> friendlyField(key)
}

private fun friendlyField(key: String): String = when (key) {
    "monthly_emi_scaled" -> "Monthly EMI"
    "interest_rate_bps" -> "Interest rate"
    "remaining_emi_count" -> "EMIs left"
    "fmv_31jan2018_scaled" -> "Value on 31 Jan 2018"
    "nextEmiDate" -> "Next EMI date"
    else -> key.replace('_', ' ')
}
