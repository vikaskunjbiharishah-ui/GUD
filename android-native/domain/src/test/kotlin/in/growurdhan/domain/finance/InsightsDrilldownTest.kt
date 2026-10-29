package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Quantities and values come from PortfolioEngine, not from a second formula. */
class InsightsDrilldownTest {
    private val primary = OwnerId("primary")
    private val relative = OwnerId("relative")
    private val day = LocalDate.parse("2026-10-10")

    @Test fun ownerAndCategorySlicesMatchThePortfolioEngine() {
        val fund = FinancialHolding(
            HoldingId("fund"), primary, "Synthetic fund", "MUTUAL_FUND",
            MoneyScaled(1_000_000),
        )
        val share = FinancialHolding(
            HoldingId("share"), relative, "Synthetic share", "EQUITY",
            MoneyScaled(500_000),
        )
        val loan = FinancialHolding(
            HoldingId("loan"), relative, "Synthetic loan", "LIABILITY",
            MoneyScaled(200_000),
        )
        val events = listOf(
            FinancialEvent(
                "buy-fund", fund.holdingId, primary, FinancialEventKind.BUY, day,
                200_000_000L, MoneyScaled(800_000), MoneyScaled(1_600_000),
            ),
            FinancialEvent(
                "buy-share", share.holdingId, relative, FinancialEventKind.BUY, day,
                100_000_000L, MoneyScaled(400_000), MoneyScaled(400_000),
            ),
        )
        val report = PortfolioEngine.derive(
            listOf(fund, share, loan.copy(storedQuantityScaled = 100_000_000L)),
            events, day, listOf(primary, relative),
        )
        val dashboard = HouseholdDashboardProjector.from(
            report, mapOf(primary to "Primary", relative to "Relative"),
        )
        val insights = PortfolioInsightsProjector.from(dashboard)
        // 2 units * ₹100 = ₹200. 1 unit * ₹50 = ₹50. Loan stored units * ₹20.
        assertEquals(2_000_000L, dashboard.byCategory.single { it.category == "MUTUAL_FUND" }.value.raw)
        assertEquals(500_000L, dashboard.byCategory.single { it.category == "EQUITY" }.value.raw)
        assertEquals(2_500_000L, dashboard.assets.raw)
        assertEquals(200_000L, dashboard.liabilities.raw)

        for (category in dashboard.byCategory) {
            val rows = InsightsDrilldown.holdings(dashboard, category = category.category)
            assertEquals(category.holdingCount, rows.size)
            assertEquals(category.value.raw, InsightsDrilldown.assetTotal(rows).raw)
            val shareRow = insights.allocations.single { it.category == category.category }
            assertEquals(category.value.raw, shareRow.value.raw)
        }
        for (member in dashboard.byMember) {
            val rows = InsightsDrilldown.holdings(dashboard, ownerId = member.ownerId)
            assertEquals(member.holdings, rows.size)
            assertEquals(member.assets.raw, InsightsDrilldown.assetTotal(rows).raw)
            assertEquals(member.liabilities.raw, InsightsDrilldown.liabilityTotal(rows).raw)
            assertEquals(true, rows.all { it.ownerId == member.ownerId })
        }
        val primaryFunds = InsightsDrilldown.holdings(
            dashboard, ownerId = primary, category = "MUTUAL_FUND",
        )
        assertEquals(listOf("Synthetic fund"), primaryFunds.map { it.name })
        assertEquals(0, InsightsDrilldown.holdings(
            dashboard, ownerId = primary, category = "EQUITY",
        ).size)
        val basis = insights.allocations.single { it.category == "MUTUAL_FUND" }.shareBasisPoints
        assertEquals(8000, basis)
    }
}
