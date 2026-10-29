package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.CustodyHoldingScreen
import `in`.growurdhan.app.presentation.screens.FamilyAdministrationScreen
import `in`.growurdhan.app.presentation.screens.HomeScreen
import `in`.growurdhan.app.presentation.screens.InsightsScreen
import `in`.growurdhan.app.presentation.screens.ManualTradeScreen
import `in`.growurdhan.app.presentation.screens.PortfolioScreen
import `in`.growurdhan.app.presentation.screens.StatementReviewScreen
import `in`.growurdhan.app.presentation.screens.TaxOwnerScopeScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.FamilyAdministrationSnapshot
import `in`.growurdhan.core.model.FamilyMember
import `in`.growurdhan.core.model.FamilyProfile
import `in`.growurdhan.core.model.Holding
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.QuantityScaled
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.core.model.VaultBook
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.MemberDashboard
import `in`.growurdhan.domain.finance.RecordedHoldingDashboard
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerLensShellTest {
    @get:Rule val compose = createComposeRule()
    private val primary = OwnerId("self")
    private val relative = OwnerId("relative")

    @Test fun insightsHideTheOtherPersonsHoldingWhenOnePersonIsSelected() {
        val dashboard = HouseholdDashboard(
            asOf = LocalDate.parse("2026-10-10"),
            assets = MoneyScaled(300_000),
            liabilities = MoneyScaled(100_000),
            netWorth = MoneyScaled(200_000),
            memberCount = 2,
            holdingCount = 2,
            unknownCostHoldings = 0,
            integrityWarnings = emptyList(),
            byMember = listOf(
                MemberDashboard(primary, MoneyScaled(200_000), MoneyScaled(0), MoneyScaled(200_000), 1, "Primary"),
                MemberDashboard(relative, MoneyScaled(100_000), MoneyScaled(100_000), MoneyScaled(0), 1, "Relative"),
            ),
            recordedHoldings = listOf(
                RecordedHoldingDashboard(
                    "Synthetic private fund", "MUTUAL_FUND", "Primary",
                    100_000_000L, MoneyScaled(200_000), null, true, false,
                ),
                RecordedHoldingDashboard(
                    "Synthetic other gold", "GOLD", "Relative",
                    100_000_000L, MoneyScaled(100_000), null, false, false,
                ),
            ),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InsightsScreen(
                    HouseholdDashboardUiState.Ready(dashboard), {}, {},
                    selectedOwnerId = primary,
                )
            }
        }
        compose.onNodeWithText("Showing one person only: Primary.").assertIsDisplayed()
        compose.onNodeWithText("Synthetic private fund").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Synthetic other gold").assertCountEquals(0)
        compose.onAllNodesWithText("Household position").assertCountEquals(0)
    }

    @Test fun manualTradePickerHidesTheOtherPersonsHolding() {
        val own = Holding(
            HoldingId("own"), primary, null, "Synthetic index asset",
            "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(0),
        )
        val other = Holding(
            HoldingId("other"), relative, null, "Synthetic other asset",
            "GOLD", QuantityScaled(0), MoneyScaled(0),
        )
        val book = VaultBook(
            listOf(
                FamilyMember(primary, "Primary", "Self", true),
                FamilyMember(relative, "Relative", "Spouse", false),
            ),
            listOf(own, other), emptyList(),
        )
        compose.setContent {
            ManualTradeScreen(
                ManualTradeUiState.Ready(book), {}, {},
                selectedOwnerId = primary,
            )
        }
        compose.onNodeWithText("Showing one person's holdings only.").assertIsDisplayed()
        compose.onNodeWithText("Synthetic index asset").assertIsDisplayed()
        compose.onAllNodesWithText("Synthetic other asset").assertCountEquals(0)
    }

    @Test fun homeHidesTheOtherPersonsHoldingAndTotals() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                HomeScreen(
                    HouseholdDashboardUiState.Ready(twoPersonDashboard()),
                    onOpenSettings = {},
                    onOpenPrivacy = {},
                    selectedFamilyOwner = primary,
                )
            }
        }
        compose.onNodeWithText("Showing one person only").assertIsDisplayed()
        compose.onNodeWithText("Synthetic private fund").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Synthetic other gold").assertCountEquals(0)
    }

    @Test fun portfolioHidesTheOtherPersonsHolding() {
        compose.setContent {
            PortfolioScreen(
                HouseholdDashboardUiState.Ready(twoPersonDashboard()),
                onOpenVault = {},
                onRefresh = {},
                onManageHoldings = {},
                selectedFamilyOwner = primary,
            )
        }
        compose.onNodeWithText("Synthetic private fund").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Synthetic other gold").assertCountEquals(0)
    }

    @Test fun custodyListHidesTheOtherPersonsHolding() {
        val own = Holding(
            HoldingId("own"), primary, null, "Synthetic index asset",
            "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(0),
        )
        val other = Holding(
            HoldingId("other"), relative, null, "Synthetic other asset",
            "GOLD_SGB", QuantityScaled(0), MoneyScaled(0),
        )
        val book = VaultBook(
            listOf(
                FamilyMember(primary, "Primary", "Self", true),
                FamilyMember(relative, "Relative", "Spouse", false),
            ),
            listOf(own, other), emptyList(),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Ready(book),
                    onIntent = {},
                    onBackToVault = {},
                    onContinueToStatements = {},
                    selectedFamilyOwner = primary,
                )
            }
        }
        compose.onAllNodesWithText("Synthetic index asset").assertCountEquals(2)
        compose.onAllNodesWithText("Synthetic other asset").assertCountEquals(0)
    }

    @Test fun taxScopeHidesTheOtherPerson() {
        compose.setContent {
            TaxOwnerScopeScreen(
                HouseholdDashboardUiState.Ready(twoPersonDashboard()),
                ownerId = primary,
                onBack = {},
            )
        }
        compose.onNodeWithText("Primary").assertIsDisplayed()
        compose.onAllNodesWithText("Relative").assertCountEquals(0)
    }

    @Test fun statementsOfferOnlyTheSelectedPerson() {
        compose.setContent {
            StatementReviewScreen(
                state = StatementReviewUiState.SelectOwner(listOf(
                    FamilyMember(primary, "Primary", "Self", true),
                    FamilyMember(relative, "Relative", "Spouse", false),
                )),
                onIntent = {},
                selectedFamilyOwner = primary,
            )
        }
        compose.onNodeWithText("Primary").assertIsDisplayed()
        compose.onAllNodesWithText("Relative").assertCountEquals(0)
    }

    @Test fun familyAdministrationHidesTheOtherPersonsBirthDate() {
        val snapshot = FamilyAdministrationSnapshot(
            family = listOf(
                FamilyProfile(primary, "Primary", "Self", true, true, null, null, null, null, null),
                FamilyProfile(relative, "Relative", "Spouse", false, true, "1992-02-02", null, null, null, null),
            ),
            accounts = emptyList(),
            goals = emptyList(),
            holdings = emptyList(),
        )
        compose.setContent {
            FamilyAdministrationScreen(
                state = FamilyAdminUiState.Ready(snapshot),
                onIntent = {},
                onAddMember = {},
                onBack = {},
                selectedFamilyOwner = primary,
            )
        }
        compose.onAllNodesWithText("1992-02-02", substring = true).assertCountEquals(0)
    }

    private fun twoPersonDashboard() = HouseholdDashboard(
        asOf = LocalDate.parse("2026-10-10"),
        assets = MoneyScaled(300_000),
        liabilities = MoneyScaled(100_000),
        netWorth = MoneyScaled(200_000),
        memberCount = 2,
        holdingCount = 2,
        unknownCostHoldings = 0,
        integrityWarnings = emptyList(),
        byMember = listOf(
            MemberDashboard(primary, MoneyScaled(200_000), MoneyScaled(0), MoneyScaled(200_000), 1, "Primary"),
            MemberDashboard(relative, MoneyScaled(100_000), MoneyScaled(100_000), MoneyScaled(0), 1, "Relative"),
        ),
        recordedHoldings = listOf(
            RecordedHoldingDashboard(
                "Synthetic private fund", "MUTUAL_FUND", "Primary",
                100_000_000L, MoneyScaled(200_000), null, true, false,
            ),
            RecordedHoldingDashboard(
                "Synthetic other gold", "GOLD_SGB", "Relative",
                100_000_000L, MoneyScaled(100_000), null, false, false,
            ),
        ),
    )
}
