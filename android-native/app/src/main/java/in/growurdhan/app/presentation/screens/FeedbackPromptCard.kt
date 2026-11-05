package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.FeedbackPrompt
import `in`.growurdhan.domain.finance.FeedbackState
import java.time.Instant

/**
 * Uses the same 7-day / 2-day / six-dismiss rules as src/lib/feedback.ts.
 * This card does not open the Play Store and does not collect a rating.
 */
@Composable
fun FeedbackPromptCard(
    state: FeedbackState,
    now: Instant,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Feedback", style = MaterialTheme.typography.titleMedium)
        if (!FeedbackPrompt.shouldShow(state, now)) {
            Text("A rating prompt waits 7 days, then snoozes for 2 days. GrowUrDhan does not collect a rating.")
        } else {
            Text("Has this book been useful? You can rate it on Google Play yourself. This app does not open the store and does not take the rating.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "Snooze the rating prompt"
                    },
                ) { Text("Not now") }
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "Stop the rating prompt"
                    },
                ) { Text("Don't ask again") }
            }
        }
    }
}
