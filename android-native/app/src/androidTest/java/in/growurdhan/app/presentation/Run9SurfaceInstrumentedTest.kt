package `in`.growurdhan.app.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.InsightsScreen
import `in`.growurdhan.app.presentation.screens.ManualPriceScreen
import `in`.growurdhan.app.presentation.screens.SettingsScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.FinancialEventKind
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.InsightsReturns
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.market.ManualPrice
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P24 Insights XIRR, P27 typed stale price, P31 SGB switch, P32 diagnostics switch.
 * TalkBack was not run.
 */
@RunWith(AndroidJUnit4::class)
class Run9SurfaceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.parse("2026-10-30")
    private fun rupees(value: Long) = MoneyScaled(value * 10_000L)

    private fun book() = HouseholdDashboard(
        asOf = today,
        assets = rupees(1_100),
        liabilities = rupees(100),
        netWorth = rupees(1_000),
        memberCount = 1,
        holdingCount = 1,
        unknownCostHoldings = 0,
        integrityWarnings = emptyList(),
        byMember = emptyList(),
    )

    @Test fun insightsScreenShowsTheKnownXirrAndNotAZeroRate() {
        val buy = FinancialEvent(
            "buy", HoldingId("synthetic"), OwnerId("synthetic-owner"), FinancialEventKind.BUY,
            LocalDate.parse("2025-01-01"), totalAmount = rupees(1_000),
        )
        val note = InsightsReturns.note(listOf(buy), rupees(1_100), LocalDate.parse("2026-01-01"))
        assertEquals("XIRR 10.01%. Estimate, not advice.", note)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InsightsScreen(
                    HouseholdDashboardUiState.Ready(book(), xirrNote = note),
                    onOpenVault = {},
                    onRefresh = {},
                )
            }
        }
        compose.onNodeWithText("XIRR 10.01%. Estimate, not advice.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Estimate, not advice.", substring = true).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("XIRR is unknown", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("0%", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun insightsScreenShowsUnknownXirrWithoutCallingItZero() {
        val note = InsightsReturns.note(emptyList(), rupees(1_100), today)
        assertEquals(InsightsReturns.UNKNOWN, note)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.DARK, false)) {
                InsightsScreen(
                    HouseholdDashboardUiState.Ready(book(), xirrNote = note),
                    onOpenVault = {},
                    onRefresh = {},
                )
            }
        }
        compose.onNodeWithText("XIRR is unknown from this book.").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("0%", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("XIRR 0", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun typedManualPriceNamesItselfAndShowsStale() {
        val priced = ManualPrice.resolve(
            rupees(90), LocalDate.parse("2026-10-01"),
            rupees(125), LocalDate.parse("2026-10-01"),
            today,
        )
        val shown = MoneyEngine.formatRupees(rupees(125).raw)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                ManualPriceScreen(priced, onBack = {})
            }
        }
        compose.onNodeWithText("Manual price you typed.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Stale as of 2026-10-01.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("This price is stale.").assertIsDisplayed()
        compose.onNodeWithText(shown).assertIsDisplayed()
        compose.onNodeWithText("Source manual.").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("No close is saved", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("₹ 0.00", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("₹ 90.00", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Back from price").assertHeightIsAtLeast(48.dp)
    }

    @Test fun settingsSwitchesStartOffAndNameSgbAndDiagnostics() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                var appearance by remember { mutableStateOf(AppearanceSettings()) }
                SettingsScreen(
                    appearance = appearance,
                    onIntent = { intent ->
                        when (intent) {
                            is AppIntent.SetSgbUseMarket ->
                                appearance = appearance.copy(sgbUseMarket = intent.enabled)
                            is AppIntent.SetDiagnostics ->
                                appearance = appearance.copy(diagnosticsEnabled = intent.enabled)
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithText("Using the RBI price when one is saved. A missing price stays missing.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Off by default. Notes stay on this screen until you delete them. Nothing is sent.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Use the market close for SGB")
            .performScrollTo().assertHeightIsAtLeast(48.dp).assertIsOff()
        compose.onNodeWithContentDescription("Opt in to local diagnostics")
            .performScrollTo().assertHeightIsAtLeast(48.dp).assertIsOff()
        assertEquals(0, compose.onAllNodesWithText("Record a local note").fetchSemanticsNodes().size)

        compose.onNodeWithContentDescription("Use the market close for SGB").performClick()
        compose.onNodeWithText("Using the market close when one is saved. A missing close stays missing.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Use the market close for SGB").assertIsOn()
        assertEquals(0, compose.onAllNodesWithText("Using the RBI price", substring = true).fetchSemanticsNodes().size)

        compose.onNodeWithContentDescription("Opt in to local diagnostics").performScrollTo().performClick()
        compose.onNodeWithText("Record a local note").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Opened settings.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete local diagnostic notes").performScrollTo().performClick()
        assertEquals(0, compose.onAllNodesWithText("Opened settings.").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("uploaded", substring = true).fetchSemanticsNodes().size)
    }
}
