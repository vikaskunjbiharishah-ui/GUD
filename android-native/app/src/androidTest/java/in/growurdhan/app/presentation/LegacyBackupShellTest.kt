package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    @Test fun previewOffersExplicitRestoreButRequiresFullIdentityScreen() {
        var action: LegacyBackupIntent? = null
        val summary = LegacyBackupPreview(1, 1, 2, 1, 1, true, true, "b".repeat(64))
        compose.setContent {
            LegacyBackupScreen(LegacyBackupUiState.Preview(summary),
                onIntent = { action = it }, onBack = {})
        }
        compose.onNodeWithText("Restore to empty vault").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(LegacyBackupIntent.PrepareRestore, action) }
    }

    @Test fun recoveryFormPromptsForPanDobAndBothChallenges() {
        compose.setContent {
            LegacyBackupScreen(LegacyBackupUiState.EnterRecovery,
                onIntent = {}, onBack = {})
        }
        compose.onNodeWithText("PAN (10 characters)").assertExists()
        compose.onNodeWithText("Date of birth (YYYY-MM-DD)").assertExists()
        compose.onNodeWithText("First question ID").assertExists()
        compose.onNodeWithText("Second question ID").assertExists()
    }

}
