package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.FamilyAdminIntent
import `in`.growurdhan.app.presentation.FamilyAdminUiState
import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.vault.CustodyHoldingOnboarding
import java.math.BigDecimal

/**
 * M26 encrypted household administration. No full PAN or account number is
 * shown, no destructive family action, and no form writes before device approval.
 */
@Composable
fun FamilyAdministrationScreen(
    state: FamilyAdminUiState,
    onIntent: (FamilyAdminIntent) -> Unit,
    onAddMember: () -> Unit,
    onBack: () -> Unit,
    selectedFamilyOwner: OwnerId? = null,
    onSelectFamilyOwner: (OwnerId?) -> Unit = {},
    initialSection: String = "Family",
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Family, accounts and goals", style = MaterialTheme.typography.headlineMedium)
        when (state) {
            FamilyAdminUiState.Locked -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(
                        "Open the books before adding a person.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
            }
            FamilyAdminUiState.Loading, FamilyAdminUiState.Saving -> {
                CircularProgressIndicator()
                Text("Checking this phone…")
            }
            is FamilyAdminUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { onIntent(FamilyAdminIntent.Refresh) }) { Text("Try again") }
            }
            is FamilyAdminUiState.AwaitingApproval -> {
                Text("Confirm this change with this phone's screen lock. Nothing is saved if you cancel.")
                Text("People, accounts, goals and past entries stay as they are until you confirm.")
                OutlinedButton(onClick = { onIntent(FamilyAdminIntent.Cancel) }) { Text("Cancel unconfirmed change") }
            }
            FamilyAdminUiState.Saved -> {
                Text("Saved in the books on this phone.")
                Button(onClick = { onIntent(FamilyAdminIntent.Refresh) }) { Text("Review updated records") }
            }
            is FamilyAdminUiState.Ready -> {
                val book = state.data
                var section by remember(initialSection) {
                    mutableStateOf(when (initialSection) {
                        "Family" -> "People"
                        else -> initialSection
                    })
                }
                val selectedOwner = selectedFamilyOwner
                Text("Which person", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selectedOwner == null, onClick = { onSelectFamilyOwner(null) },
                        label = { Text("All family") })
                    book.family.forEach { member ->
                        FilterChip(selectedOwner == member.id,
                            onClick = { onSelectFamilyOwner(member.id) },
                            label = { Text(member.name + if (!member.active) " (inactive)" else "") })
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("People","Accounts","Goals","Danger").forEach { name ->
                        FilterChip(section == name, onClick = { section = name },
                            label = { Text(name) })
                    }
                }
                state.warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (section) {
                    "People" -> {
                        Text("People", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = onAddMember, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add a person") }

                        book.ownerIdentity?.let { identity ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("Registered owner account",style = MaterialTheme.typography.titleMedium)
                                    Text(identity.firstName + " " + identity.lastName)
                                    Text("Email: " + identity.email)
                                    identity.panMasked?.let { Text("PAN: " + it + " (masked)") }
                                    identity.dateOfBirth?.let { Text("DOB: " + it) }
                                    Text("Security questions: " + listOfNotNull(
                                        identity.firstQuestionId, identity.secondQuestionId,
                                    ).joinToString(", ") { questionLabel(it) })
                                    Text("Security answers and original plaintext PAN are never shown.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (book.ownerIdentity == null && book.family.any { it.primary }) {
                            var first by remember { mutableStateOf("") }
                            var last by remember { mutableStateOf("") }
                            var email by remember { mutableStateOf("") }
                            var pan by remember { mutableStateOf("") }
                            var dob by remember { mutableStateOf("") }
                            var q1 by remember { mutableStateOf("school") }
                            var q2 by remember { mutableStateOf("city") }
                            var answer1 by remember { mutableStateOf("") }
                            var answer2 by remember { mutableStateOf("") }
                            var consent by remember { mutableStateOf(false) }
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Register primary owner identity",
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("These answers stay on this phone. They are for getting back in. They are not sent to GrowUrDhan.")
                                    OutlinedTextField(first,{ if(it.length <= 100) first=it },
                                        label = { Text("First name") },modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(last,{ if(it.length <= 100) last=it },
                                        label = { Text("Last name") },modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(email,{ if(it.length <= 254) email=it },
                                        label = { Text("Owner account email") },modifier = Modifier.fillMaxWidth())
                                    var hidePan by remember { mutableStateOf(false) }
                                    OutlinedTextField(pan,{ if(it.length <= 10) pan=it.uppercase() },
                                        label = { Text("PAN") },modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = if (hidePan) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                                        trailingIcon = {
                                            TextButton(onClick = { hidePan = !hidePan }) {
                                                Text(if (hidePan) "Show" else "Hide")
                                            }
                                        })
                                    BookDateField(
                                        label = "Date of birth, for example 15/08/1980",
                                        iso = dob,
                                        onIso = { dob = it },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    val options = listOf("maiden","school","pet","city","teacher","vehicle")
                                    Text("First original security question",style = MaterialTheme.typography.titleSmall)
                                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                                        options.forEach { id ->
                                            FilterChip(q1 == id,onClick = { q1=id },label = { Text(questionLabel(id)) })
                                        }
                                    }
                                    OutlinedTextField(answer1,{ if(it.length <= 256) answer1=it },
                                        label = { Text("First security answer") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth())
                                    Text("Second original security question",style = MaterialTheme.typography.titleSmall)
                                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                                        options.forEach { id ->
                                            FilterChip(q2 == id,onClick = { q2=id },label = { Text(questionLabel(id)) })
                                        }
                                    }
                                    OutlinedTextField(answer2,{ if(it.length <= 256) answer2=it },
                                        label = { Text("Second security answer") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth())
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Checkbox(checked = consent, onCheckedChange = { consent = it })
                                        Text("I agree to keep this profile in the books on this phone.")
                                    }
                                    Button(
                                        enabled = first.isNotBlank() && last.isNotBlank() &&
                                            email.isNotBlank() && pan.length == 10 && dob.length == 10 &&
                                            q1 != q2 && answer1.length >= 3 && answer2.length >= 3 &&
                                            consent,
                                        onClick = {
                                            val cmd = FamilyAdminChange.RegisterOwnerIdentity(
                                                first,last,email,pan,dob,q1,answer1.toCharArray(),
                                                q2,answer2.toCharArray(), registrationConsent = consent,
                                            )
                                            first="";last="";email="";pan="";dob=""
                                            answer1="";answer2="";consent=false
                                            onIntent(FamilyAdminIntent.Stage(cmd))
                                        },
                                    ) { Text("Save and confirm with the phone lock") }
                                }
                            }
                        }

                        val shown = book.family.filter { selectedOwner == null || it.id == selectedOwner }
                        if (shown.isEmpty()) Text("No one in this view yet.")
                        shown.forEach { person ->
                            var editing by remember(person.id.raw) { mutableStateOf(false) }
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(person.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${person.relation} • ${if(person.primary) "You" else if(person.active) "Active" else "Inactive"}")
                                    person.dob?.let { Text("Birth date: $it") }
                                    person.panMasked?.let { Text("PAN: $it (masked)") }
                                    person.guardianId?.let { id ->
                                        Text("Guardian: ${book.family.find { it.id == id }?.name ?: "Unknown"}")
                                    }
                                    if (!editing) {
                                        OutlinedButton(onClick = { editing = true }) { Text("Edit member details") }
                                        if (!person.primary) {
                                            Text(if (person.active)
                                                "Deactivation is reversible but blocked while accounts, holdings, goals or dependants remain."
                                                else "Inactive members retain their historical identity and can be reactivated.",
                                                style = MaterialTheme.typography.bodySmall)
                                            OutlinedButton(onClick = {
                                                onIntent(FamilyAdminIntent.Stage(
                                                    FamilyAdminChange.SetMemberActive(person.id, !person.active)))
                                            }) { Text(if (person.active) "Confirm deactivation" else "Reactivate") }
                                        }
                                    } else {
                                        var name by remember(person.id.raw) { mutableStateOf(person.name) }
                                        var relation by remember(person.id.raw) { mutableStateOf(person.relation) }
                                        var dob by remember(person.id.raw) { mutableStateOf(person.dob ?: "") }
                                        var color by remember(person.id.raw) { mutableStateOf(person.color ?: "#64748B") }
                                        var notes by remember(person.id.raw) { mutableStateOf(person.notes ?: "") }
                                        var pan by remember(person.id.raw) { mutableStateOf("") }
                                        var guardian by remember(person.id.raw) { mutableStateOf(person.guardianId) }
                                        var clearPan by remember(person.id.raw) { mutableStateOf(false) }
                                        OutlinedTextField(name, { if(it.length <= 100) name = it },
                                            label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
                                        if (!person.primary) {
                                            Row(Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("Spouse","Parent","Child","HUF","Joint").forEach { option ->
                                                    FilterChip(relation == option, onClick = { relation = option },
                                                        label = { Text(option) })
                                                }
                                            }
                                        }
                                        BookDateField(
                                            label = "Date of birth, for example 15/08/1980",
                                            iso = dob,
                                            onIso = { dob = it },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Text("Guardian for a minor child")
                                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                                            FilterChip(guardian == null, onClick = { guardian = null },
                                                label = { Text("None") })
                                            book.family.filter { it.active && it.id != person.id }.forEach { other ->
                                                FilterChip(guardian == other.id, onClick = { guardian = other.id },
                                                    label = { Text(other.name) })
                                            }
                                        }
                                        Text("Profile colour")
                                        Row(Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                "Green" to "#007A49",
                                                "Gold" to "#B86E00",
                                                "Indigo" to "#2F4FA8",
                                                "Slate" to "#64748B",
                                                "Stone" to "#78716C",
                                                "Moss" to "#3F6212",
                                                "Ink" to "#1F2937",
                                            ).forEach { (swatch, hex) ->
                                                FilterChip(
                                                    selected = color.equals(hex, ignoreCase = true),
                                                    onClick = { color = hex },
                                                    label = { Text(swatch) },
                                                )
                                            }
                                        }
                                        OutlinedTextField(notes, { if(it.length <= 2000) notes = it },
                                            label = { Text("Private family notes") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(pan, { if(it.length <= 10) pan = it.uppercase() },
                                            label = { Text("Replace PAN for statement unlock (optional)") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth())
                                        OutlinedButton(onClick = { clearPan = !clearPan; if(clearPan) pan = "" }) {
                                            Text(if(clearPan) "Keep existing PAN instead" else "Clear stored PAN")
                                        }
                                        Button(onClick = {
                                            onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.Member(
                                                person.id,name,relation,dob.ifBlank { null },
                                                guardian,color.ifBlank { null },notes.ifBlank { null },
                                                pan.ifBlank { null },clearPan,
                                            )))
                                        }) { Text("Save this person") }
                                        OutlinedButton(onClick = { editing = false }) { Text("Cancel editing") }
                                    }
                                }
                            }
                        }
                    }
                    "Accounts" -> {
                        val family = book.family.filter { it.active }
                        var owner by remember(book.family) {
                            mutableStateOf(book.family.firstOrNull { it.active }?.id)
                        }
                        val selected = selectedOwner?.takeIf { id -> family.any { it.id == id } } ?: owner
                        Text("Financial accounts and nominees", style = MaterialTheme.typography.titleLarge)
                        if (family.isEmpty()) Text("Register a family owner first.")
                        else {
                            Text("Account owner (cannot be reassigned after creation)")
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                family.forEach { member ->
                                    FilterChip(selected == member.id,
                                        onClick = { owner = member.id; onSelectFamilyOwner(member.id) },
                                        label = { Text(member.name) })
                                }
                            }
                            var search by remember { mutableStateOf("") }
                            OutlinedTextField(search, { if (it.length <= 100) search = it },
                                label = { Text("Search nickname, institution or masked DP / folio") },
                                modifier = Modifier.fillMaxWidth())
                            val listed = book.accounts.filter {
                                (selectedOwner == null || it.ownerId == selectedOwner) &&
                                (search.isBlank() || listOfNotNull(
                                    it.nickname, it.type, it.institution, it.dpMasked,
                                    it.clientMasked, it.boMasked,
                                ).any { field -> field.contains(search, ignoreCase = true) })
                            }
                            if (listed.isEmpty()) Text("No accounts yet. Add a custodian, registrar or bank account.")
                            var editAccount by remember { mutableStateOf<String?>(null) }
                            // Group account books under their custodian/issuer. The user
                            // owner filter is applied BEFORE grouping, never by display name.
                            val grouped = listed.groupBy {
                                it.institution?.trim()?.takeIf { name -> name.isNotEmpty() } ?: it.type
                            }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                            grouped.forEach { (issuer, groupAccounts) ->
                                Text("Custodian / issuer: $issuer",
                                    style = MaterialTheme.typography.titleMedium)
                                groupAccounts.forEach { acc ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text(acc.nickname, style = MaterialTheme.typography.titleMedium)
                                        Text("${book.family.find { it.id == acc.ownerId }?.name ?: "Owner"} • ${acc.type}")
                                        Text(if (acc.closedOn == null) "Active account" else "Closed ${acc.closedOn}")
                                        acc.institution?.let { Text("Institution: $it") }
                                        acc.dpMasked?.let { Text("DP: $it") }
                                        acc.clientMasked?.let { Text("Client: $it") }
                                        acc.boMasked?.let { Text("BO: $it") }
                                        acc.nominee?.let { Text("Nominee: $it") }
                                        val linked = book.holdings.filter {
                                            it.accountId == acc.id && it.ownerId == acc.ownerId
                                        }
                                        Text("Associated holdings: ${linked.size}")
                                        linked.take(5).forEach { holding ->
                                            Text("• ${holding.name}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (linked.size > 5) Text(
                                            "And ${linked.size - 5} other linked holdings",
                                            style = MaterialTheme.typography.bodySmall)
                                        if (acc.closedOn == null) {
                                            OutlinedButton(onClick = { editAccount = acc.id }) {
                                                Text("Edit account and nomination")
                                            }
                                            Text("Account numbers stay hidden. You see only the last digits.")
                                        }
                                    }
                                }
                            }
                            }
                            val current = book.accounts.find { it.id == editAccount }
                            var nickname by remember(editAccount,selected) { mutableStateOf(current?.nickname ?: "") }
                            var type by remember(editAccount,selected) { mutableStateOf(current?.type ?: "BANK") }
                            var institution by remember(editAccount,selected) { mutableStateOf(current?.institution ?: "") }
                            var nominee by remember(editAccount,selected) { mutableStateOf(current?.nominee ?: "") }
                            var dp by remember(editAccount,selected) { mutableStateOf("") }
                            var client by remember(editAccount,selected) { mutableStateOf("") }
                            var bo by remember(editAccount,selected) { mutableStateOf("") }
                            var opened by remember(editAccount,selected) { mutableStateOf(current?.openedOn ?: "") }
                            var clearNominee by remember(editAccount) { mutableStateOf(false) }
                            var clearIds by remember(editAccount) { mutableStateOf(false) }
                            Text(if (current == null) "Register a new account" else "Edit: ${current.nickname}",
                                style = MaterialTheme.typography.titleMedium)
                            Text("Account type", style = MaterialTheme.typography.titleSmall)
                            if (current != null) {
                                Text("$type (fixed after account creation)")
                            } else {
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CustodyHoldingOnboarding.accountTypes.forEach { option ->
                                        FilterChip(type == option, onClick = { type = option },
                                            label = { Text(option) })
                                    }
                                }
                            }
                            OutlinedTextField(nickname,{ if(it.length <= 100) nickname=it },
                                label = { Text("Account nickname (not full number)") },
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(institution,{ if(it.length <= 200) institution=it },
                                label = { Text("Institution / registrar") },modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(nominee,{ if(it.length <= 300) nominee=it },
                                label = { Text("Nominee / nomination details") },modifier = Modifier.fillMaxWidth())
                            Text("Account number is stored hidden. You will see only the last digits.")
                            OutlinedTextField(dp,{ if(it.length <= 80) dp=it },
                                label = { Text("Account id") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedTextField(client,{ if(it.length <= 80) client=it },
                                label = { Text("Client account id") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedTextField(bo,{ if(it.length <= 80) bo=it },
                                label = { Text("Beneficiary account id") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedButton(onClick = {
                                clearNominee = !clearNominee
                                if(clearNominee) nominee = ""
                            }) { Text(if(clearNominee) "Keep nominee" else "Remove saved nomination") }
                            OutlinedButton(onClick = {
                                clearIds = !clearIds
                                if(clearIds) { dp = ""; client = ""; bo = "" }
                            }) { Text(if(clearIds) "Keep stored account ids" else "Clear stored account ids") }
                            BookDateField(
                                label = "Opened on, for example 15/08/2020",
                                iso = opened,
                                onIso = { opened = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(enabled = selected != null && nickname.isNotBlank(),
                                onClick = {
                                    selected?.let { id ->
                                        onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.Account(
                                            current?.id,current?.ownerId ?: id,type,nickname,institution.ifBlank{null},
                                            dp.ifBlank{null},client.ifBlank{null},bo.ifBlank{null},
                                            nominee.ifBlank{null},opened.ifBlank{null},
                                            clearNominee,clearIds,
                                        )))
                                    }
                                }) { Text("Save this account") }
                            if(current != null) OutlinedButton(onClick = { editAccount = null }) {
                                Text("Register a different account")
                            }
                        }
                    }
                    "Goals" -> {
                        Text("Goals", style = MaterialTheme.typography.titleLarge)
                        var editGoal by remember { mutableStateOf<String?>(null) }
                        val listed = book.goals.filter { selectedOwner == null || it.ownerId == null || it.ownerId == selectedOwner }
                        if (listed.isEmpty()) Text("No matching goals. Add a household-wide or member-specific goal.")
                        listed.forEach { goal ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(goal.name,style = MaterialTheme.typography.titleMedium)
                                    Text("Whose goal: ${goal.ownerId?.let { id -> book.family.find { it.id == id }?.name } ?: "Whole family"}")
                                    Text("Target: ${goal.target?.asBook() ?: "Not entered"}")
                                    goal.targetDate?.let { Text("Target date: $it") }
                                    OutlinedButton(onClick = { editGoal = goal.id }) { Text("Edit goal") }
                                    var showLink by remember(goal.id) { mutableStateOf(false) }
                                    TextButton(onClick = { showLink = !showLink }) {
                                        Text(if (showLink) "Hide linked investments" else "Link investments")
                                    }
                                    if (showLink) {
                                    Text("Amount linked to this goal: ${goal.taggedExposure.asBook()}")
                                    val available = book.holdings.filter {
                                        goal.ownerId == null || it.ownerId == goal.ownerId
                                    }
                                    if (available.isEmpty()) Text("No eligible holdings to tag.")
                                    available.forEach { holding ->
                                        val tagged = holding.id in goal.holdingIds
                                        OutlinedButton(onClick = {
                                            onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.TagHolding(
                                                goal.id,holding.id,holding.ownerId,!tagged)))
                                        }) { Text((if(tagged) "Remove tag: " else "Tag: ") + holding.name) }
                                    }
                                    }
                                }
                            }
                        }
                        val original = book.goals.find { it.id == editGoal }
                        var name by remember(editGoal) { mutableStateOf(original?.name ?: "") }
                        var target by remember(editGoal) {
                            mutableStateOf(original?.target?.rupees()?.toPlainString() ?: "")
                        }
                        var date by remember(editGoal) { mutableStateOf(original?.targetDate ?: "") }
                        var priority by remember(editGoal) { mutableStateOf(original?.priority ?: "") }
                        var owner by remember(editGoal) { mutableStateOf(original?.ownerId) }
                        Text(if (original == null) "Add family goal" else "Edit family goal",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Goal owner (All family permits holdings from multiple owners)")
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChip(owner == null,onClick = { owner = null },label = { Text("All family") })
                            book.family.filter { it.active }.forEach { member ->
                                FilterChip(owner == member.id,onClick = { owner = member.id },
                                    label = { Text(member.name) })
                            }
                        }
                        OutlinedTextField(name,{ if(it.length <= 200) name=it },
                            label = { Text("Goal name") },modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(target,{ if(it.length <= 25) target=it },
                            label = { Text("Target INR (blank if unknown)") },modifier = Modifier.fillMaxWidth())
                        BookDateField(
                            label = "Target date, for example 15/08/2030",
                            iso = date,
                            onIso = { date = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(priority,{ if(it.length <= 50) priority=it },
                            label = { Text("Priority") },modifier = Modifier.fillMaxWidth())
                        val parsedTarget = if(target.isBlank()) null else runCatching {
                            MoneyScaled.fromRupees(BigDecimal(target))
                        }.getOrNull()
                        Button(enabled = name.isNotBlank() && (target.isBlank() ||
                            (parsedTarget != null && parsedTarget.raw >= 0)),onClick = {
                            onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.Goal(
                                original?.id,owner,name,parsedTarget,
                                date.ifBlank { null },priority.ifBlank{null},
                            )))
                        }) { Text("Save this goal") }
                        if(original != null) OutlinedButton(onClick = { editGoal = null }) {
                            Text("Add another goal")
                        }
                    }
                    "Danger" -> {
                        Text(
                            "Close an account or remove a goal",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Past entries stay in the book. Closing an account does not delete investments. Removing a goal does not delete investments.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        book.accounts.filter {
                            it.closedOn == null && (selectedOwner == null || it.ownerId == selectedOwner)
                        }.forEach { acc ->
                            Text(acc.nickname, style = MaterialTheme.typography.titleMedium)
                            Text("Closing ${acc.nickname} keeps existing investments and every past entry.")
                            OutlinedButton(
                                onClick = {
                                    onIntent(FamilyAdminIntent.Stage(
                                        FamilyAdminChange.CloseAccount(acc.id, acc.ownerId),
                                    ))
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("Close ${acc.nickname}") }
                        }
                        book.goals.filter {
                            selectedOwner == null || it.ownerId == null || it.ownerId == selectedOwner
                        }.forEach { goal ->
                            Text(goal.name, style = MaterialTheme.typography.titleMedium)
                            Text("Removing ${goal.name} deletes only the goal and its links, not investments or past entries.")
                            OutlinedButton(
                                onClick = {
                                    onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.DeleteGoal(goal.id)))
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("Remove ${goal.name}") }
                        }
                    }
                }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
        }
    }
}
