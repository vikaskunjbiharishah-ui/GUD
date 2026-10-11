package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
                Text("Unlock your encrypted vault to manage family relationships and accounts.")
                OutlinedButton(onClick = onBack) { Text("Back to vault") }
            }
            FamilyAdminUiState.Loading, FamilyAdminUiState.Saving -> {
                CircularProgressIndicator()
                Text("Checking encrypted family records…")
            }
            is FamilyAdminUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { onIntent(FamilyAdminIntent.Refresh) }) { Text("Reload encrypted records") }
            }
            is FamilyAdminUiState.AwaitingApproval -> {
                Text("Review and confirm this change with a NEW Android screen-lock challenge.")
                Text("Family, account, goal and ledger data remain unchanged until confirmation.")
                OutlinedButton(onClick = { onIntent(FamilyAdminIntent.Cancel) }) { Text("Cancel unconfirmed change") }
            }
            FamilyAdminUiState.Saved -> {
                Text("Approved household change saved in the encrypted vault.")
                Button(onClick = { onIntent(FamilyAdminIntent.Refresh) }) { Text("Review updated records") }
            }
            is FamilyAdminUiState.Ready -> {
                val book = state.data
                var section by remember(initialSection) { mutableStateOf(initialSection) }
                val selectedOwner = selectedFamilyOwner
                Text("Owner scope", style = MaterialTheme.typography.titleSmall)
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
                    listOf("Family","Accounts","Goals").forEach { name ->
                        FilterChip(section == name, onClick = { section = name },
                            label = { Text(name) })
                    }
                }
                state.warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (section) {
                    "Family" -> {
                        Text("Family and ownership", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = onAddMember) { Text("Register another family member") }

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
                                        identity.firstQuestionId,identity.secondQuestionId).joinToString(", "))
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
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Register primary owner identity",
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("Encrypted account profile: your name, email, PAN, DOB " +
                                        "and two independent security answers. This is NOT a " +
                                        "password-reset bypass; keep your offline recovery code.")
                                    OutlinedTextField(first,{ if(it.length <= 100) first=it },
                                        label = { Text("First name") },modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(last,{ if(it.length <= 100) last=it },
                                        label = { Text("Last name") },modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(email,{ if(it.length <= 254) email=it },
                                        label = { Text("Owner account email") },modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(pan,{ if(it.length <= 10) pan=it.uppercase() },
                                        label = { Text("PAN") },modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = PasswordVisualTransformation())
                                    OutlinedTextField(dob,{ if(it.length <= 10) dob=it },
                                        label = { Text("Date of birth YYYY-MM-DD") },
                                        modifier = Modifier.fillMaxWidth())
                                    val options = listOf("maiden","school","pet","city","teacher","vehicle")
                                    Text("First original security question",style = MaterialTheme.typography.titleSmall)
                                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                                        options.forEach { id ->
                                            FilterChip(q1 == id,onClick = { q1=id },label = { Text(id) })
                                        }
                                    }
                                    OutlinedTextField(answer1,{ if(it.length <= 256) answer1=it },
                                        label = { Text("First security answer") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth())
                                    Text("Second original security question",style = MaterialTheme.typography.titleSmall)
                                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                                        options.forEach { id ->
                                            FilterChip(q2 == id,onClick = { q2=id },label = { Text(id) })
                                        }
                                    }
                                    OutlinedTextField(answer2,{ if(it.length <= 256) answer2=it },
                                        label = { Text("Second security answer") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth())
                                    Button(
                                        enabled = first.isNotBlank() && last.isNotBlank() &&
                                            email.isNotBlank() && pan.length == 10 && dob.length == 10 &&
                                            q1 != q2 && answer1.length >= 3 && answer2.length >= 3,
                                        onClick = {
                                            val cmd = FamilyAdminChange.RegisterOwnerIdentity(
                                                first,last,email,pan,dob,q1,answer1.toCharArray(),
                                                q2,answer2.toCharArray(),
                                            )
                                            first="";last="";email="";pan="";dob=""
                                            answer1="";answer2=""
                                            onIntent(FamilyAdminIntent.Stage(cmd))
                                        },
                                    ) { Text("Register account with new device approval") }
                                }
                            }
                        }

                        val shown = book.family.filter { selectedOwner == null || it.id == selectedOwner }
                        if (shown.isEmpty()) Text("No family members in this scope.")
                        shown.forEach { person ->
                            var editing by remember(person.id.raw) { mutableStateOf(false) }
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(person.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${person.relation} • ${if(person.primary) "Primary Self" else if(person.active) "Active" else "Inactive"}")
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
                                        OutlinedTextField(dob, { if(it.length <= 10) dob = it },
                                            label = { Text("Birth date YYYY-MM-DD (optional)") },
                                            modifier = Modifier.fillMaxWidth())
                                        Text("Guardian for a minor child")
                                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                                            FilterChip(guardian == null, onClick = { guardian = null },
                                                label = { Text("None") })
                                            book.family.filter { it.active && it.id != person.id }.forEach { other ->
                                                FilterChip(guardian == other.id, onClick = { guardian = other.id },
                                                    label = { Text(other.name) })
                                            }
                                        }
                                        OutlinedTextField(color, { if(it.length <= 7) color = it },
                                            label = { Text("Profile color #RRGGBB") })
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
                                        }) { Text("Review member update") }
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
                                            Text("Closing retains existing holdings and all past ledger events.",
                                                style = MaterialTheme.typography.bodySmall)
                                            OutlinedButton(onClick = {
                                                onIntent(FamilyAdminIntent.Stage(
                                                    FamilyAdminChange.CloseAccount(acc.id,acc.ownerId)))
                                            }) { Text("Confirm account closure") }
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
                            Text("DP/client/BO identifiers are encrypted and displayed masked. Leave blank to preserve existing IDs.")
                            OutlinedTextField(dp,{ if(it.length <= 80) dp=it },
                                label = { Text("DP ID") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedTextField(client,{ if(it.length <= 80) client=it },
                                label = { Text("Client ID") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedTextField(bo,{ if(it.length <= 80) bo=it },
                                label = { Text("BO ID") },modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedButton(onClick = {
                                clearNominee = !clearNominee
                                if(clearNominee) nominee = ""
                            }) { Text(if(clearNominee) "Keep nominee" else "Remove saved nomination") }
                            OutlinedButton(onClick = {
                                clearIds = !clearIds
                                if(clearIds) { dp = ""; client = ""; bo = "" }
                            }) { Text(if(clearIds) "Keep stored IDs" else "Clear stored DP/client/BO identifiers") }
                            OutlinedTextField(opened,{ if(it.length <= 10) opened=it },
                                label = { Text("Opened YYYY-MM-DD (optional)") },
                                modifier = Modifier.fillMaxWidth())
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
                                }) { Text("Review account and nominee") }
                            if(current != null) OutlinedButton(onClick = { editAccount = null }) {
                                Text("Register a different account")
                            }
                        }
                    }
                    else -> {
                        Text("Financial goals and tagged exposures", style = MaterialTheme.typography.titleLarge)
                        var editGoal by remember { mutableStateOf<String?>(null) }
                        val listed = book.goals.filter { selectedOwner == null || it.ownerId == null || it.ownerId == selectedOwner }
                        if (listed.isEmpty()) Text("No matching goals. Add a household-wide or member-specific goal.")
                        listed.forEach { goal ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(goal.name,style = MaterialTheme.typography.titleMedium)
                                    Text("Scope: ${goal.ownerId?.let { id -> book.family.find { it.id == id }?.name } ?: "Whole family"}")
                                    Text("Target: ${goal.target?.rupees()?.toPlainString() ?: "Not entered"} INR")
                                    Text("Tagged current exposure: ${goal.taggedExposure.rupees().toPlainString()} INR")
                                    goal.targetDate?.let { Text("Target date: $it") }
                                    OutlinedButton(onClick = { editGoal = goal.id }) { Text("Edit goal") }
                                    Text("Remove goal deletes only the goal and its tags, not holdings or ledger events.",
                                        style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = {
                                        onIntent(FamilyAdminIntent.Stage(FamilyAdminChange.DeleteGoal(goal.id)))
                                    }) { Text("Confirm remove goal") }
                                    Text("Tag holdings to multiple goals")
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
                        OutlinedTextField(date,{ if(it.length <= 10) date=it },
                            label = { Text("Target YYYY-MM-DD") },modifier = Modifier.fillMaxWidth())
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
                        }) { Text("Review goal update") }
                        if(original != null) OutlinedButton(onClick = { editGoal = null }) {
                            Text("Add another goal")
                        }
                    }
                }
                OutlinedButton(onClick = onBack) { Text("Back to vault") }
            }
        }
    }
}
