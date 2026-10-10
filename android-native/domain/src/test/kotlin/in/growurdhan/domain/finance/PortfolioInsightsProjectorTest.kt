package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PortfolioInsightsProjectorTest {
    private fun dashboard(
        assets: Long, debt: Long, categories: List<CategoryDashboard>,
        unknownCost: Int = 0, warnings: List<String> = emptyList(),
        holdings: List<RecordedHoldingDashboard> = emptyList(),
    ) = HouseholdDashboard(
        asOf = LocalDate.parse("2026-10-10"),
        assets = MoneyScaled(assets),
        liabilities = MoneyScaled(debt),
        netWorth = MoneyScaled(Math.subtractExact(assets, debt)),
        memberCount = 1,
        holdingCount = if (holdings.isEmpty()) 2 else holdings.size,
        unknownCostHoldings = unknownCost,
        integrityWarnings = warnings,
        byMember = emptyList(),
        byCategory = categories,
        recordedHoldings = holdings,
    )

    @Test fun twoAssetCategoriesProduceDeterministicSharesAndRecordedDebtRatio() {
        val insights = PortfolioInsightsProjector.from(dashboard(
            3_000_000, 500_000,
            listOf(
                CategoryDashboard("MUTUAL_FUND", MoneyScaled(2_000_000), 1),
                CategoryDashboard("GOLD", MoneyScaled(1_000_000), 1),
            ),
        ))
        assertTrue(insights.reconciled)
        assertEquals(listOf("MUTUAL_FUND", "GOLD"), insights.allocations.map { it.category })
        assertEquals(listOf(6667, 3333), insights.allocations.map { it.shareBasisPoints })
        assertEquals("16.7", insights.debtToAssetsPercent!!.toPlainString())
    }

    @Test fun mismatchedCategoryTotalsOrAnyIntegrityWarningSuppressAllocations() {
        val badTotals = dashboard(3_000_000, 0,
            listOf(CategoryDashboard("GOLD", MoneyScaled(1_000_000), 1)))
        assertFalse(PortfolioInsightsProjector.from(badTotals).reconciled)
        assertTrue(PortfolioInsightsProjector.from(badTotals).allocations.isEmpty())
        val warnings = dashboard(1_000_000, 0,
            listOf(CategoryDashboard("GOLD", MoneyScaled(1_000_000), 1)),
            warnings = listOf("A private event could not be reconciled."))
        val guarded = PortfolioInsightsProjector.from(warnings)
        assertFalse(guarded.reconciled)
        assertNull(guarded.debtToAssetsPercent)
        assertEquals(1, guarded.warningCount)
    }

    @Test fun debtOnlyAndEmptyFamiliesNeverDivideByZeroOrInventAllocation() {
        for (debt in listOf(0L, 500_000L)) {
            val insights = PortfolioInsightsProjector.from(dashboard(0, debt, emptyList()))
            assertTrue(insights.reconciled)
            assertFalse(insights.hasMeasuredAllocation)
            assertNull(insights.debtToAssetsPercent)
        }
    }

    @Test fun maxLongMoneyValuesUseCheckedBigIntegerPercentMath() {
        val all = dashboard(Long.MAX_VALUE, 0,
            listOf(CategoryDashboard("FIXED_INCOME", MoneyScaled(Long.MAX_VALUE), 2)))
        val insights = PortfolioInsightsProjector.from(all)
        assertTrue(insights.reconciled)
        assertEquals(10_000, insights.allocations.single().shareBasisPoints)
        assertEquals("0.0", insights.debtToAssetsPercent!!.toPlainString())
    }

    @Test fun negativeOrDuplicatedValuationsDoNotProduceMisleadingInsights() {
        val negativeCategory = dashboard(100_000, 0,
            listOf(
                CategoryDashboard("A", MoneyScaled(150_000), 1),
                CategoryDashboard("B", MoneyScaled(-50_000), 1),
            ))
        assertFalse(PortfolioInsightsProjector.from(negativeCategory).reconciled)
        val missingRow = dashboard(
            100_000, 0, listOf(CategoryDashboard("A", MoneyScaled(100_000), 1)),
            holdings = listOf(RecordedHoldingDashboard(
                "Synthetic private holding", "A", null, 100_000_000L,
                MoneyScaled(80_000), null, true, false,
            )),
        )
        assertFalse(PortfolioInsightsProjector.from(missingRow).reconciled)
    }

    @Test fun realFinancialEngineBalancesReconcileAcrossMemberAndCategory() {
        val owner = `in`.growurdhan.core.model.OwnerId("synthetic-owner")
        val report = PortfolioEngine.derive(
            listOf(
                FinancialHolding(
                    `in`.growurdhan.core.model.HoldingId("asset"), owner,
                    "Synthetic savings", "CASH", MoneyScaled(250_000), 100_000_000L,
                ),
                FinancialHolding(
                    `in`.growurdhan.core.model.HoldingId("debt"), owner,
                    "Synthetic debt", "LIABILITY", MoneyScaled(80_000), 100_000_000L,
                ),
            ),
            emptyList(),
            LocalDate.parse("2026-10-10"),
            listOf(owner),
        )
        val dashboard = HouseholdDashboardProjector.from(report, mapOf(owner to "Primary"))
        val insights = PortfolioInsightsProjector.from(dashboard)
        assertTrue(insights.reconciled)
        assertEquals(10_000, insights.allocations.single().shareBasisPoints)
        assertEquals("32.0", insights.debtToAssetsPercent!!.toPlainString())
    }

    @Test fun memberAssetMismatchCannotProducePercentages() {
        val owner = `in`.growurdhan.core.model.OwnerId("member")
        val dashboard = dashboard(100_000, 0,
            listOf(CategoryDashboard("CASH", MoneyScaled(100_000), 2)))
            .copy(byMember = listOf(MemberDashboard(
                owner, MoneyScaled(90_000), MoneyScaled(0),
                MoneyScaled(90_000), 2, "Primary",
            )))
        assertFalse(PortfolioInsightsProjector.from(dashboard).reconciled)
    }

    @Test fun unknownCostIsDisclosedButDoesNotPretendToMeasureReturns() {
        val insights = PortfolioInsightsProjector.from(dashboard(
            1_000_000, 1_500_000,
            listOf(CategoryDashboard("MUTUAL_FUND", MoneyScaled(1_000_000), 2)),
            unknownCost = 2,
        ))
        assertTrue(insights.reconciled)
        assertEquals(2, insights.unknownHistoricalCostHoldings)
        assertEquals("150.0", insights.debtToAssetsPercent!!.toPlainString())
    }
}
