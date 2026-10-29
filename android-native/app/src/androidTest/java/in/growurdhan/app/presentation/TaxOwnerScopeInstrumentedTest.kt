package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.TaxOwnerScopeScreen
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.FinancialEventKind
import `in`.growurdhan.domain.finance.FinancialHolding
import `in`.growurdhan.domain.finance.FyTaxEstimate
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.OwnerFyTax
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FY 2024-25 must not paint a rupee tax. Gains and the boundary note stay. */
@RunWith(AndroidJUnit4::class)
class TaxOwnerScopeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun boundaryYearShowsGainsAndDoesNotPaintARupeeTax() {
        val owner = OwnerId("synthetic-owner")
        val holding = HoldingId("synthetic-equity")
        val summary = FyTaxEstimate.summarize(
            holdings = listOf(
                FinancialHolding(holding, owner, "Synthetic equity", "EQUITY", MoneyScaled(10_000),
                    subCategory = "Shares"),
            ),
            events = listOf(
                event("buy", holding, owner, FinancialEventKind.BUY, "2020-04-01", 1000, 100),
                event("before", holding, owner, FinancialEventKind.SELL, "2024-07-22", 400, 200),
                event("after", holding, owner, FinancialEventKind.SELL, "2024-07-23", 400, 200),
            ),
            financialYear = "2024-25",
            asOf = LocalDate.parse("2025-03-31"),
        )
        assertNull(summary.estimatedTax)
        assertEquals(FyTaxEstimate.RATE_CHANGED_NOTE, summary.rateNote)
        val dashboard = HouseholdDashboard(
            asOf = LocalDate.parse("2025-03-31"),
            assets = MoneyScaled(0),
            liabilities = MoneyScaled(0),
            netWorth = MoneyScaled(0),
            memberCount = 1,
            holdingCount = 1,
            unknownCostHoldings = 0,
            integrityWarnings = emptyList(),
            byMember = emptyList(),
            taxEstimates = listOf(OwnerFyTax(owner, summary)),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                TaxOwnerScopeScreen(
                    state = HouseholdDashboardUiState.Ready(dashboard),
                    ownerId = owner,
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("estimate, not advice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(FyTaxEstimate.RATE_CHANGED_NOTE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Equity long-term gain", substring = true).performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Estimated tax", substring = true).fetchSemanticsNodes().size)
    }

    private fun event(
        id: String,
        holding: HoldingId,
        owner: OwnerId,
        kind: FinancialEventKind,
        on: String,
        qty: Long,
        price: Long,
    ) = FinancialEvent(
        id, holding, owner, kind, LocalDate.parse(on),
        Math.multiplyExact(qty, 100_000_000L),
        MoneyScaled(Math.multiplyExact(price, 10_000)),
        MoneyScaled(Math.multiplyExact(qty * price, 10_000)),
    )
}
