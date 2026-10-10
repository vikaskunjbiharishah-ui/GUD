package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Read-only percentages from the same authorized book snapshot shown on Home. */
data class AssetAllocation(
    val category: String,
    val value: MoneyScaled,
    val holdingCount: Int,
    /** One hundredth of a percentage point, rounded independently per asset category. */
    val shareBasisPoints: Int,
)

data class WealthInsights(
    /** Do not publish plausible-looking allocations from inconsistent or excluded records. */
    val reconciled: Boolean,
    val allocations: List<AssetAllocation>,
    /** Null when assets are zero, or the book cannot be reconciled. */
    val debtToAssetsPercent: BigDecimal?,
    val unknownHistoricalCostHoldings: Int,
    val warningCount: Int,
) {
    val hasMeasuredAllocation: Boolean get() = reconciled && allocations.isNotEmpty()
}

object PortfolioInsightsProjector {
    fun from(dashboard: HouseholdDashboard): WealthInsights {
        val assets = BigInteger.valueOf(dashboard.assets.raw)
        val liabilities = BigInteger.valueOf(dashboard.liabilities.raw)
        val netWorth = BigInteger.valueOf(dashboard.netWorth.raw)
        val categories = dashboard.byCategory
        val categorySum = categories.fold(BigInteger.ZERO) { sum, row ->
            sum + BigInteger.valueOf(row.value.raw)
        }
        val countSum = categories.fold(0L) { sum, row -> sum + row.holdingCount }
        val memberAssets = dashboard.byMember.fold(BigInteger.ZERO) { sum, member ->
            sum + BigInteger.valueOf(member.assets.raw)
        }
        val memberDebts = dashboard.byMember.fold(BigInteger.ZERO) { sum, member ->
            sum + BigInteger.valueOf(member.liabilities.raw)
        }
        val memberWorth = dashboard.byMember.fold(BigInteger.ZERO) { sum, member ->
            sum + BigInteger.valueOf(member.netWorth.raw)
        }
        val memberHoldings = dashboard.byMember.sumOf { it.holdings.toLong() }
        val holdings = dashboard.recordedHoldings
        val rowsAssets = holdings.filterNot { it.isLiability }.fold(BigInteger.ZERO) { sum, row ->
            sum + BigInteger.valueOf(row.recordedValue.raw)
        }
        val rowsDebt = holdings.filter { it.isLiability }.fold(BigInteger.ZERO) { sum, row ->
            sum + BigInteger.valueOf(row.recordedValue.raw)
        }
        val reconciled = dashboard.isReliable && assets.signum() >= 0 &&
            liabilities.signum() >= 0 && assets - liabilities == netWorth &&
            categories.all { it.value.raw >= 0 && it.holdingCount >= 0 } &&
            categorySum == assets && countSum <= dashboard.holdingCount.toLong() &&
            (dashboard.byMember.isEmpty() ||
                (memberAssets == assets && memberDebts == liabilities &&
                    memberWorth == netWorth && memberHoldings == dashboard.holdingCount.toLong())) &&
            (holdings.isEmpty() ||
                (holdings.size == dashboard.holdingCount && rowsAssets == assets &&
                    rowsDebt == liabilities &&
                    countSum == holdings.count { !it.isLiability }.toLong()))
        if (!reconciled) {
            return WealthInsights(
                false, emptyList(), null,
                dashboard.unknownCostHoldings, dashboard.integrityWarnings.size,
            )
        }
        if (assets.signum() == 0) {
            return WealthInsights(true, emptyList(), null, dashboard.unknownCostHoldings, 0)
        }
        val shares = categories.filter { it.value.raw > 0 }.map { category ->
            // BigInteger prevents scaled-INR overflow at high family wealth values.
            val numerator = BigInteger.valueOf(category.value.raw)
                .multiply(BigInteger.valueOf(10_000L))
            val (whole, remainder) = numerator.divideAndRemainder(assets)
            val rounded = if (remainder.shiftLeft(1) >= assets) whole + BigInteger.ONE else whole
            AssetAllocation(
                category.category, category.value, category.holdingCount, rounded.intValueExact(),
            )
        }.sortedWith(compareByDescending<AssetAllocation> { it.value.raw }.thenBy { it.category })
        val debtPct = BigDecimal(liabilities)
            .multiply(BigDecimal(100))
            .divide(BigDecimal(assets), 1, RoundingMode.HALF_UP)
        return WealthInsights(true, shares, debtPct, dashboard.unknownCostHoldings, 0)
    }
}
