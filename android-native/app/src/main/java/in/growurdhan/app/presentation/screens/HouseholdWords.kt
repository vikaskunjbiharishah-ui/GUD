package `in`.growurdhan.app.presentation.screens

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.vault.FirstRunRegistration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Household labels. Engines keep their own exact sentences. */

/** Ink and paper that stay readable if the phone wallpaper recolours Material. */
@Composable
internal fun authPaper(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    return if (dark) Color(0xFF0C1210) else Color(0xFFF7FAF8)
}

@Composable
internal fun authInk(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    return if (dark) Color(0xFFE7F2EB) else Color(0xFF121C18)
}

@Composable
internal fun householdFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = authInk(),
    unfocusedTextColor = authInk(),
    disabledTextColor = authInk(),
    focusedLabelColor = authInk(),
    unfocusedLabelColor = authInk(),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = authInk(),
    focusedContainerColor = authPaper(),
    unfocusedContainerColor = authPaper(),
    disabledContainerColor = authPaper(),
)

internal fun householdCategory(raw: String): String = when (raw.trim().lowercase().replace('_', ' ')) {
    "mutual fund" -> "Mutual fund"
    "equity" -> "Shares"
    "gold", "gold sgb" -> "Gold"
    "fixed income" -> "Fixed income"
    "real estate" -> "Property"
    "liability", "loan" -> "Loan"
    else -> raw.replace('_', ' ').lowercase().split(' ')
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
}

internal fun questionLabel(id: String): String = FirstRunRegistration.questionLabel(id)

private val shownDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

internal fun isoToHouseholdDate(iso: String): String {
    val parsed = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return ""
    return parsed.format(shownDate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDateField(
    label: String,
    iso: String,
    onIso: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val shown = isoToHouseholdDate(iso)
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(if (shown.isBlank()) label else "$label · $shown")
    }
    if (open) {
        val initial = runCatching { LocalDate.parse(iso) }.getOrNull()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onIso(date.toString())
                    }
                    open = false
                }) { Text("Save date") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
