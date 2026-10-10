package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.LegacyBackupIntent
import `in`.growurdhan.app.presentation.screens.LegacyBackupScreen
import `in`.growurdhan.app.presentation.screens.LegacyBackupUiState
import `in`.growurdhan.data.vault.LegacyBackupPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyBackupShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun legacyScreenOffersInspectionNotUnverifiedRestore() {
        var last: LegacyBackupIntent? = null
        compose.setContent {
            LegacyBackupScreen(LegacyBackupUiState.Empty, onIntent = { last = it }, onBack = {})
        }
        compose.onNodeWithText("Select locked .kosha file").performClick()
        compose.runOnIdle { assertEquals(LegacyBackupIntent.SelectFile, last) }
        compose.onNodeWithText("Import into vault").assertDoesNotExist()
    }

    @Test fun authenticatedSummaryDoesNotRevealAccountIdentifiersOrOfferMerge() {
        val summary = LegacyBackupPreview(
            2, 3, 4, 1, 1, true, true, "a".repeat(64),
        )
        compose.setContent {
            LegacyBackupScreen(LegacyBackupUiState.Preview(summary), {}, {})
        }
        compose.onNodeWithText("Holdings: 3").assertIsDisplayed()
        compose.onNodeWithText("Goals: 1").assertIsDisplayed()
        compose.onNodeWithText("Import into vault").assertDoesNotExist()
        compose.onNodeWithText("Merge financial records").assertDoesNotExist()
    }
}
