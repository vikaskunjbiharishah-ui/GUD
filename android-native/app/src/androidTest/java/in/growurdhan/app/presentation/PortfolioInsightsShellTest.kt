package `in`.growurdhan.app.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.PortfolioScreen
import `in`.growurdhan.app.presentation.screens.InsightsScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.CategoryDashboard
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.RecordedHoldingDashboard
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortfolioInsightsShellTest {
    @get:Rule val compose = createComposeRule()

    private val syntheticDashboard = HouseholdDashboard(
        asOf = LocalDate.parse("2026-10-10"),
        assets = MoneyScaled(300_000),
        liabilities = MoneyScaled(100_000),
        netWorth = MoneyScaled(200_000),
        memberCount = 2,
        holdingCount = 3,
        unknownCostHoldings = 1,
        integrityWarnings = emptyList(),
        byMember = emptyList(),
        byCategory = listOf(
            CategoryDashboard("MUTUAL_FUND", MoneyScaled(200_000), 1),
            CategoryDashboard("GOLD", MoneyScaled(100_000), 1),
        ),
        recordedHoldings = listOf(
            RecordedHoldingDashboard(
                "Synthetic private fund", "MUTUAL_FUND", "Primary",
                100_000_000L, MoneyScaled(200_000), null, true, false,
            ),
            RecordedHoldingDashboard(
                "Synthetic gold holding", "GOLD", "Relative",
                100_000_000L, MoneyScaled(100_000), MoneyScaled(5_000), false, false,
            ),
            RecordedHoldingDashboard(
                "Synthetic housing debt", "LIABILITY", "Primary",
                100_000_000L, MoneyScaled(100_000), null, false, true,
            ),
        ),
    )

    @Test fun portfolioTradeNavigationDoesNotSkipLockedStateOrCredential() {
        var lastIntent: ManualTradeIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                GrowUrDhanShell(
                    state = AppUiState(),
                    onIntent = {},
                    dashboardState = HouseholdDashboardUiState.Ready(syntheticDashboard),
                    manualTradeState = ManualTradeUiState.Locked,
                    onManualTradeIntent = { lastIntent = it },
                )
            }
        }
        compose.onNodeWithContentDescription("Portfolio").performClick()
        compose.onNodeWithText("Record an explicit buy or sell")
            .performScrollTo().performClick()
        compose.onNodeWithText(
            "Unlock the vault before recording a financial transaction.",
        ).assertIsDisplayed()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(ManualTradeIntent.Refresh, lastIntent)
        }
        compose.onNodeWithText("Confirm transaction with device lock").assertDoesNotExist()
    }

    @Test fun portfolioFiltersHideUnmatchedRecordsWithoutEditingTotals() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                PortfolioScreen(
                    state = HouseholdDashboardUiState.Ready(syntheticDashboard),
                    onOpenVault = {}, onRefresh = {}, onManageHoldings = {},
                )
            }
        }
        compose.onNodeWithText("Synthetic private fund").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter category LIABILITY")
            .performScrollTo().performClick()
        compose.onNodeWithText("Synthetic housing debt").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Synthetic private fund").assertDoesNotExist()
        compose.onNodeWithText("Family assets: 30.0000 INR", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(
            "1 of 3 recorded holdings match the filters. Filters do not change family totals.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test fun portfolioRemovesFilteredPrivateContentWhenVaultLocks() {
        var state by mutableStateOf<HouseholdDashboardUiState>(
            HouseholdDashboardUiState.Ready(syntheticDashboard),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                PortfolioScreen(state, onOpenVault = {}, onRefresh = {}, onManageHoldings = {})
            }
        }
        compose.onNodeWithText("Synthetic private fund").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state = HouseholdDashboardUiState.Locked }
        compose.onNodeWithText("Synthetic private fund").assertDoesNotExist()
        compose.onNodeWithText("Unlock the vault to see your family's recorded holdings.")
            .assertIsDisplayed()
    }

    @Test fun insightsShowOnlyReconciledRecordedAllocationAndDebtRatio() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InsightsScreen(HouseholdDashboardUiState.Ready(syntheticDashboard), {}, {})
            }
        }
        compose.onNodeWithText("MUTUAL FUND · 66.7%")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("GOLD · 33.3%")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Debt-to-assets: 33.3% (recorded balances only)")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Allocation withheld").assertDoesNotExist()
    }

    @Test fun inconsistentBookWithholdsAllocationInsteadOfShowingPercentages() {
        val invalid = syntheticDashboard.copy(
            byCategory = listOf(CategoryDashboard("MUTUAL_FUND", MoneyScaled(100_000), 1)),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InsightsScreen(HouseholdDashboardUiState.Ready(invalid), {}, {})
            }
        }
        compose.onNodeWithText("Allocation withheld").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("MUTUAL FUND · 66.7%").assertDoesNotExist()
    }

    @Test fun lockedInsightsNeverExposeFamilyHoldingsOrRecordedTotals() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InsightsScreen(HouseholdDashboardUiState.Locked, {}, {})
            }
        }
        compose.onNodeWithText(
            "Unlock the vault to view your family's private financial insights.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Synthetic private fund").assertDoesNotExist()
        compose.onNodeWithText("Household position").assertDoesNotExist()
    }
}
