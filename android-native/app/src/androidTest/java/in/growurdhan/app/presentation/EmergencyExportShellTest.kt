package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.EmergencyExportScreen
import `in`.growurdhan.app.presentation.screens.EmergencyExportUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmergencyExportShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unencryptedDisclosureMustAppearBeforeFileDestinationSelection() {
        var requested = 0
        compose.setContent {
            EmergencyExportScreen(EmergencyExportUiState.READY, { requested++ }, {})
        }
        compose.onNodeWithText("Unencrypted export").assertIsDisplayed()
        compose.onNodeWithText("Choose CSV destination and confirm device lock").performClick()
        compose.runOnIdle { assertEquals(1, requested) }
    }

    @Test fun cancelledOrUnconfirmedExportsNeverClaimSuccessfulWrite() {
        compose.setContent {
            EmergencyExportScreen(EmergencyExportUiState.FAILED, {}, {})
        }
        compose.onAllNodesWithText("CSV was written to your chosen destination. Store it securely.")
            .assertCountEquals(0)
        compose.onNodeWithText("Unencrypted export").assertIsDisplayed()
    }
}
