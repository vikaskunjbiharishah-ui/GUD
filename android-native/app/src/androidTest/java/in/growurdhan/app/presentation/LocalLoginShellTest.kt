package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.LocalLoginScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalLoginShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstInstallRequiresTwoMatchingPasswordsBeforeDeviceApproval() {
        var entered: Pair<String, String?>? = null
        compose.setContent {
            LocalLoginScreen(LocalLoginStatus.NEEDS_SETUP, null) { password, repeated ->
                entered = password to repeated
            }
        }
        compose.onNodeWithTag("LocalAppPassword").performTextInput("Synthetic!Pass123")
        compose.onNodeWithTag("LocalAppPasswordRepeat").performTextInput("Synthetic!Pass123")
        compose.onNodeWithText("Protect app with device credential").performClick()
        compose.runOnIdle {
            assertEquals("Synthetic!Pass123" to "Synthetic!Pass123", entered)
        }
    }

    @Test fun lockedLoginHidesAllFinancialNavigationAndHoldings() {
        compose.setContent {
            LocalLoginScreen(LocalLoginStatus.LOCKED, null) { _, _ -> }
        }
        compose.onNodeWithText("Unlock your private books").assertIsDisplayed()
        compose.onNodeWithText("Portfolio").assertDoesNotExist()
        compose.onNodeWithText("Synthetic private holding").assertDoesNotExist()
        compose.onNodeWithText("Confirm app password").assertDoesNotExist()
    }
}
