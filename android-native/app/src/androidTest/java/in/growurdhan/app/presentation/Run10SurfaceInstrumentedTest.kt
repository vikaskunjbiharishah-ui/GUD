package `in`.growurdhan.app.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.LicenceScreen
import `in`.growurdhan.app.presentation.screens.SettingsScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.commercial.LicenceGate
import `in`.growurdhan.domain.finance.FeedbackState
import `in`.growurdhan.domain.finance.LicenceState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P37 restore stays fail-closed. P36 feedback clock is the saved stamp, not a fresh remember.
 * TalkBack was not run. Play Billing was not called.
 */
@RunWith(AndroidJUnit4::class)
class Run10SurfaceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun restoreTapLeavesThePlanOff() {
        var subscribed = true
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                var state by remember {
                    mutableStateOf(
                        LicenceUiState(
                            licence = LicenceState(
                                plan = "trial",
                                subscribed = false,
                                daysLeft = 12,
                                sideloadBlocked = false,
                            ),
                            checking = false,
                        ),
                    )
                }
                LicenceScreen(
                    state = state,
                    onPurchase = {},
                    onRestore = {
                        val resolved = LicenceGate.resolve(
                            emptyList(), null, installedFromPlay = true, debugTrialAllowed = false,
                            Instant.parse("2026-10-30T00:00:00Z"),
                        )
                        subscribed = resolved.state.subscribed
                        state = state.copy(licence = resolved.state, checking = false, busy = false)
                    },
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("If Play has no yearly purchase, the plan stays off. A button cannot turn it on.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Restore purchases on this Play account")
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        assertFalse(subscribed)
        compose.onNodeWithText("One yearly plan").assertIsDisplayed()
        compose.onNodeWithText("If Play has no yearly purchase, the plan stays off. A button cannot turn it on.")
            .assertIsDisplayed()
        assertEquals(
            0,
            compose.onAllNodesWithText("is on this Play account", substring = true).fetchSemanticsNodes().size,
        )
    }

    @Test fun aSavedFeedbackClockShowsThePromptWithoutOpeningTheStore() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                SettingsScreen(
                    appearance = AppearanceSettings(),
                    onIntent = {},
                    feedback = FeedbackState(firstSeenAt = "2026-09-01T00:00:00.000Z"),
                )
            }
        }
        compose.onNodeWithText("does not open the store", substring = true)
            .performScrollTo().assertIsDisplayed()
        assertEquals(
            0,
            compose.onAllNodesWithText("rated on Google Play", substring = true).fetchSemanticsNodes().size,
        )
    }

    @Test fun aMissingFeedbackClockIsHandedToTheSaver() {
        var saved: FeedbackState? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                SettingsScreen(
                    appearance = AppearanceSettings(),
                    onIntent = { intent ->
                        if (intent is AppIntent.SaveFeedback) saved = intent.state
                    },
                )
            }
        }
        compose.onNodeWithText("waits 7 days", substring = true).performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        assertNotNull(saved?.firstSeenAt)
    }
}
