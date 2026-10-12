package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.LocalLoginScreen
import `in`.growurdhan.app.presentation.screens.ForgottenPasswordRecoveryScreen
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
            LocalLoginScreen(
                state = LocalLoginStatus.NEEDS_SETUP,
                message = null,
                onSubmitWithEmail = { _, password, repeated ->
                    entered = password to repeated
                },
                onSubmit = { password, repeated -> entered = password to repeated },
            )
        }
        compose.onNodeWithTag("LocalAccountEmail").performTextInput("synthetic@example.test")
        compose.onNodeWithTag("LocalAppPassword").performTextInput("Synthetic!Pass123")
        compose.onNodeWithTag("LocalAppPasswordRepeat").performTextInput("Synthetic!Pass123")
        compose.onNodeWithText("Protect app with device credential").performClick()
        compose.runOnIdle {
            assertEquals("Synthetic!Pass123" to "Synthetic!Pass123", entered)
        }
    }

    @Test fun signedInAccountEmailIsRequiredWhenExistingLoginHasEmailBinding() {
        var captured: String? = null
        compose.setContent {
            LocalLoginScreen(
                state = LocalLoginStatus.LOCKED,
                message = null,
                accountEmailRequired = true,
                onSubmitWithEmail = { email, _, _ -> captured = email },
                onSubmit = { _, _ -> },
            )
        }
        compose.onNodeWithTag("LocalAccountEmail").performTextInput("SYNTHETIC@EXAMPLE.TEST")
        compose.onNodeWithTag("LocalAppPassword").performTextInput("Synthetic!Pass123")
        compose.onNodeWithText("Verify app password").performClick()
        compose.runOnIdle { assertEquals("SYNTHETIC@EXAMPLE.TEST", captured) }
    }

    @Test fun forgotPasswordIsOfferedOnlyWhenOneTimeRecoveryCodeExists() {
        var requested = false
        compose.setContent {
            LocalLoginScreen(
                state = LocalLoginStatus.LOCKED, message = null,
                onSubmit = { _, _ -> }, hasRecoveryCode = true,
                onForgotPassword = { requested = true },
            )
        }
        compose.onNodeWithText("Forgot password? Use saved recovery code").performClick()
        compose.runOnIdle { assertEquals(true, requested) }
    }

    @Test fun recoveryScreenNeverOffersVaultAccessBeforeOsApproval() {
        var requested = false
        compose.setContent {
            ForgottenPasswordRecoveryScreen(
                message = null,
                onReset = { _, _ -> requested = true },
                onBack = {},
            )
        }
        compose.onNodeWithText("Recover app password").assertIsDisplayed()
        compose.onNodeWithText("Portfolio").assertDoesNotExist()
        compose.runOnIdle { assertEquals(false, requested) }
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
