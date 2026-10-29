package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.EmiReviewScreen
import `in`.growurdhan.app.presentation.screens.InterestReviewScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.EmiPlan
import `in`.growurdhan.domain.finance.FinancialHolding
import `in`.growurdhan.domain.finance.LoanEmi
import `in`.growurdhan.domain.finance.MoneyEngine
import `in`.growurdhan.domain.finance.SavingsInterestEngine
import `in`.growurdhan.domain.finance.SchemeTerms
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P22/P23. Amounts come from the engines. The same PPF and car-loan vectors
 * are golden in InterestCatchUpTest and EmiPlanTest.
 */
@RunWith(AndroidJUnit4::class)
class FinanceReviewInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ppfInterestReviewShowsTheEngineNoteAndDoesNotLookPosted() {
        val owner = OwnerId("synthetic-owner")
        val holding = FinancialHolding(
            HoldingId("synthetic-ppf"), owner, "Public Provident Fund", "FIXED_INCOME",
            MoneyScaled(1_940_000L * 10_000), 100_000_000L,
            openingCostPerUnit = MoneyScaled(1_450_000L * 10_000),
            subCategory = "PPF", acquiredOn = LocalDate.parse("2011-04-01"),
        )
        val terms = SchemeTerms(
            holding, MoneyScaled(1_940_000L * 10_000), ratePercent = 7.1,
            valuedOn = LocalDate.parse("2026-08-31"), priceSource = "CAS",
        )
        val plan = requireNotNull(SavingsInterestEngine.planInterest(terms, LocalDate.parse("2027-03-31")))
        assertEquals("PPF interest for FY 2026-27 (7.1%)", plan.events.single().notes)
        assertEquals(1_377_400_000L, plan.events.single().amount.raw)
        val shown = MoneyEngine.formatRupees(plan.events.single().amount.raw)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                InterestReviewScreen(plan, onBack = {})
            }
        }
        compose.onNodeWithText("does not write a ledger event", substring = true).assertIsDisplayed()
        compose.onNodeWithText("PPF interest for FY 2026-27 (7.1%)", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(shown, substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not posted.", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun emiReviewShowsDueInstalmentsAndAPausedLoanStaysBlank() {
        val loan = LoanEmi(
            HoldingId("synthetic-loan"), OwnerId("synthetic-owner"), "Synthetic car loan", "LIABILITY",
            outstanding = MoneyScaled(612_000L * 10_000), monthlyEmi = MoneyScaled(18_500L * 10_000),
            remainingCount = 6, nextDue = LocalDate.parse("2026-07-05"), status = "PAYING",
        )
        val plan = requireNotNull(EmiPlan.plan(loan, LocalDate.parse("2026-09-09")))
        assertEquals(3, plan.events.size)
        assertNull(EmiPlan.plan(loan.copy(status = "PAUSED"), LocalDate.parse("2026-09-09")))
        val shown = MoneyEngine.formatRupees(plan.events.first().amount.raw)
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                EmiReviewScreen(plan, onBack = {})
            }
        }
        compose.onNodeWithText("does not write a ledger event", substring = true).assertIsDisplayed()
        assertEquals(3, compose.onAllNodesWithText("Monthly EMI (automatic)", substring = true).fetchSemanticsNodes().size)
        assertEquals(3, compose.onAllNodesWithText(shown, substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Outstanding", substring = true).assertIsDisplayed()
        compose.onNodeWithText("2026-09-05", substring = true).performScrollTo().assertIsDisplayed()
    }
}
