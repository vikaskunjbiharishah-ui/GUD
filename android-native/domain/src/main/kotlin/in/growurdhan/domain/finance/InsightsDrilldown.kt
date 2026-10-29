package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId

/**
 * Read-only slices of one dashboard snapshot. Totals stay on the dashboard
 * rows produced by PortfolioEngine; this object only chooses which recorded
 * holdings belong in the slice. It does not reprice the book.
 */
object InsightsDrilldown {
    fun holdings(
        dashboard: HouseholdDashboard,
        ownerId: OwnerId? = null,
        ownerName: String? = null,
        category: String? = null,
    ): List<RecordedHoldingDashboard> {
        return dashboard.recordedHoldings.filter { row ->
            val ownerOk = when {
                ownerId == null && ownerName == null -> true
                row.ownerId != null && ownerId != null -> row.ownerId == ownerId
                ownerName != null -> row.ownerName == ownerName
                else -> false
            }
            val categoryOk = category == null || row.category == category
            ownerOk && categoryOk
        }
    }

    fun assetTotal(rows: List<RecordedHoldingDashboard>): MoneyScaled =
        rows.filterNot { it.isLiability }.fold(MoneyScaled(0)) { sum, row -> sum + row.recordedValue }

    fun liabilityTotal(rows: List<RecordedHoldingDashboard>): MoneyScaled =
        rows.filter { it.isLiability }.fold(MoneyScaled(0)) { sum, row -> sum + row.recordedValue }
}
