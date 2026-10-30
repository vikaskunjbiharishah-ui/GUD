package `in`.growurdhan.app.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.FeedbackPromptCard
import `in`.growurdhan.app.presentation.screens.FirstRunIdentityScreen
import `in`.growurdhan.app.presentation.screens.HomeScreen
import `in`.growurdhan.app.presentation.screens.ManualPriceScreen
import `in`.growurdhan.app.presentation.screens.RecordsScreen
import `in`.growurdhan.app.presentation.screens.SaleReviewScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.FeedbackPrompt
import `in`.growurdhan.domain.finance.FeedbackState
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.FinancialEventKind
import `in`.growurdhan.domain.finance.FinancialHolding
import `in`.growurdhan.domain.finance.HouseholdRecord
import `in`.growurdhan.domain.finance.ImportBatchView
import `in`.growurdhan.domain.finance.InsightsReturns
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.finance.RecordKind
import `in`.growurdhan.domain.finance.SaleReview
import `in`.growurdhan.domain.market.ManualPrice
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P16, P17, P20, P24, P27, P32, P36 and the first-run identity step.
 * TalkBack itself was not run. The device-credential Activity is not launched.
 */
@RunWith(AndroidJUnit4::class)
class Run8SurfaceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")
    private val holdingId = HoldingId("synthetic-equity")
    private fun rupees(value: Long) = MoneyScaled(value * 10_000L)
    private fun units(count: Long) = count * 100_000_000L

    @Test fun saleScreenShowsFifoGainAndFeeOrder() {
        val review = SaleReview.of(
            FinancialHolding(holdingId, owner, "Synthetic equity", "EQUITY", rupees(150), units(6)),
            listOf(
                FinancialEvent(
                    "buy", holdingId, owner, FinancialEventKind.BUY, LocalDate.parse("2026-01-01"),
                    units(10), rupees(100), rupees(1_000), rupees(10),
                ),
                FinancialEvent(
                    "sell", holdingId, owner, FinancialEventKind.SELL, LocalDate.parse("2026-06-01"),
                    units(4), rupees(150), rupees(600), rupees(4),
                ),
            ),
        )
        val known = requireNotNull(review.knownGain)
        assertEquals(1_960_000L, known.raw)
        val shown = MoneyEngine.formatRupees(known.raw)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                SaleReviewScreen(listOf(review), onBack = {})
            }
        }
        compose.onNodeWithText("does not write a ledger event", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Oldest units are sold first.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("FIFO gain $shown").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to portfolio").assertHeightIsAtLeast(48.dp)
    }

    @Test fun unknownSaleCostIsNotPrintedAsZeroEvenAtDoubleFont() {
        val review = SaleReview.of(
            FinancialHolding(holdingId, owner, "Synthetic equity", "EQUITY", rupees(150), units(6)),
            listOf(
                FinancialEvent(
                    "open", holdingId, owner, FinancialEventKind.OPENING_BALANCE,
                    LocalDate.parse("2026-01-01"), units(10), MoneyScaled(0),
                ),
                FinancialEvent(
                    "sell", holdingId, owner, FinancialEventKind.SELL, LocalDate.parse("2026-06-01"),
                    units(4), rupees(150), rupees(600),
                ),
            ),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                    SaleReviewScreen(listOf(review), onBack = {})
                }
            }
        }
        compose.onNodeWithText("Need what you paid. The gain is not shown as zero.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gain not shown").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("₹ 0.00", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun missingCloseIsNotZeroAndManualPriceNamesItself() {
        val missing = ManualPrice.resolve(null, null, null, null, LocalDate.parse("2026-10-30"))
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                ManualPriceScreen(missing, onBack = {})
            }
        }
        compose.onAllNodesWithText("No close is saved. It is not zero.")[0].assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("₹ 0.00", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Back from price").assertHeightIsAtLeast(48.dp)
    }

    @Test fun recordsHideTheOtherOwner() {
        val records = listOf(
            HouseholdRecord("s1", owner, RecordKind.STATEMENT, "Synthetic CAS", LocalDate.parse("2026-09-01")),
            HouseholdRecord("t1", other, RecordKind.TAX, "Other tax note", LocalDate.parse("2026-04-01")),
        )
        val batches = listOf(
            ImportBatchView("b1", owner, "NSDL", LocalDate.parse("2026-09-01"), "a".repeat(64), 1),
            ImportBatchView("b2", other, "CDSL", LocalDate.parse("2026-09-02"), "b".repeat(64), 2),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                RecordsScreen(records, batches, owner, onBack = {})
            }
        }
        compose.onNodeWithText("Synthetic CAS", substring = true).assertIsDisplayed()
        compose.onNodeWithText("NSDL", substring = true).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Other tax note", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("CDSL", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Back to home").assertHeightIsAtLeast(48.dp)
    }

    @Test fun insightsUnknownLabelIsNotAZeroRate() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.DARK, false)) {
                HomeScreen(
                    onOpenSettings = {},
                    onOpenPrivacy = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Open portfolio").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Open insights").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Open documents").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Open tax owner scope").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Open records").performScrollTo().assertHeightIsAtLeast(48.dp)
        assertEquals(InsightsReturns.UNKNOWN, InsightsReturns.note(emptyList(), MoneyScaled(0), LocalDate.parse("2026-10-30")))
    }

    @Test fun feedbackPromptFollowsTheSnoozeAndDoesNotClaimTheStore() {
        val now = Instant.parse("2026-09-28T12:00:00Z")
        val due = FeedbackPrompt.seedFirstSeen(FeedbackState(), "2026-09-21T12:00:00.000Z", now)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                FeedbackPromptCard(due, now, onSnooze = {}, onComplete = {})
            }
        }
        compose.onNodeWithText("does not open the store", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Snooze the rating prompt").assertHeightIsAtLeast(48.dp)
        compose.onNode(hasContentDescription("Snooze the rating prompt")).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("rated on Google Play", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun firstRunIdentityStepStaysDisabledUntilConsentAndNamesTheDeviceLock() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                FirstRunIdentityScreen(
                    email = "synthetic@example.test",
                    message = null,
                    onCancel = {},
                    onContinue = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Save the account after the device lock")
            .performScrollTo()
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("device lock", substring = true).assertIsDisplayed()
    }

    @Test fun feedbackDismissHidesThePrompt() {
        val now = Instant.parse("2026-09-28T12:00:00Z")
        val due = FeedbackPrompt.seedFirstSeen(FeedbackState(), "2026-09-21T12:00:00.000Z", now)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                var state by remember { mutableStateOf(due) }
                FeedbackPromptCard(
                    state,
                    now,
                    onSnooze = { state = FeedbackPrompt.snooze(state, now) },
                    onComplete = {},
                )
            }
        }
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("waits 7 days", substring = true).assertIsDisplayed()
    }
}
