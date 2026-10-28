package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.FirstRunIdentityScreen
import `in`.growurdhan.domain.vault.FirstRunRegistration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstRunIdentityShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun continueStaysOffUntilConsentAndThenSubmitsTheProfile() {
        var captured: FirstRunRegistration.Draft? = null
        compose.setContent {
            FirstRunIdentityScreen(
                email = "synthetic@example.test",
                message = null,
                onCancel = {},
                onContinue = { captured = it },
            )
        }
        compose.onNodeWithTag("FirstRunFirstName").performTextInput("Synthetic")
        compose.onNodeWithTag("FirstRunLastName").performTextInput("Owner")
        compose.onNodeWithTag("FirstRunPan").performTextInput("ABCDE1234F")
        compose.onNodeWithTag("FirstRunDob").performTextInput("1990-01-01")
        compose.onNodeWithTag("FirstRunAnswer1").performTextInput("first answer")
        compose.onNodeWithTag("FirstRunAnswer2").performScrollTo().performTextInput("second answer")
        compose.onNodeWithTag("FirstRunContinue").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("FirstRunConsent").performScrollTo().performClick()
        compose.onNodeWithTag("FirstRunContinue").performScrollTo().performClick()
        compose.runOnIdle {
            val draft = captured
            assertEquals("Synthetic", draft?.firstName)
            assertEquals("1990-01-01", draft?.dateOfBirth)
            assertEquals(true, draft?.consent)
            assertEquals("school", draft?.firstQuestionId)
            assertEquals("city", draft?.secondQuestionId)
        }
        compose.onAllNodesWithText("first answer").assertCountEquals(0)
    }
}
