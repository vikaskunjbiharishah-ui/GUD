package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate

/**
 * Family growth book from src/lib/growth.ts.
 * A closed row is left out. A statement price standing in for cost is not
 * treated as invested, so it cannot invent a return of zero.
 * Look-through splits of a hybrid fund stay in lookthrough.ts.
 */
object GrowthModel {
    private val bucketColor = mapOf(
        "SHARES" to "#38bdf8",
        "FUNDS" to "#818cf8",
        "GOLD" to "#f5c14a",
        "GOVT" to "#34d399",
        "PROPERTY" to "#c084fc",
        "BANK" to "#22d3ee",
        "SPECIAL" to "#f472b6",
    )
    private val memberPalette = listOf(
        "#f5c14a", "#38bdf8", "#34d399", "#c084fc", "#22d3ee", "#fb7185", "#818cf8",
    )

    fun incomeByFinancialYear(
        events: List<FinancialEvent>,
        holdings: List<FinancialHolding>,
    ): List<IncomeYear> {
        val byId = holdings.associateBy { it.holdingId }
        val rows = linkedMapOf<String, IncomeYear>()
        for (event in events) {
            if (event.kind != FinancialEventKind.DIVIDEND && event.kind != FinancialEventKind.INTEREST) continue
            val fy = LotEngine.fiscalYear(event.date)
            val row = rows.getOrPut(fy) { IncomeYear(fy, MoneyScaled(0), MoneyScaled(0), MoneyScaled(0)) }
            val amount = MoneyScaled(eventCash(event))
            val property = byId[event.holdingId]?.category == "REAL_ESTATE"
            val next = when {
                event.kind == FinancialEventKind.DIVIDEND -> row.copy(dividend = row.dividend + amount)
                property -> row.copy(rental = row.rental + amount)
                else -> row.copy(interest = row.interest + amount)
            }
            rows[fy] = next
        }
        return rows.values.sortedBy { it.fy }
    }

    fun cumulativeByMonth(events: List<FinancialEvent>, asOf: LocalDate): List<MonthPoint> {
        val first = events.minByOrNull { it.date }?.date ?: return emptyList()
        val months = mutableListOf<LocalDate>()
        var cursor = first.withDayOfMonth(1)
        val end = asOf.withDayOfMonth(1)
        var guard = 0
        while (cursor <= end && guard++ < 240) {
            months += cursor
            cursor = cursor.plusMonths(1)
        }
        val investedAt = mutableMapOf<LocalDate, Long>()
        val incomeAt = mutableMapOf<LocalDate, Long>()
        for (event in events) {
            val key = event.date.withDayOfMonth(1)
            val amount = eventCash(event)
            if (amount == 0L) continue
            when (event.kind) {
                FinancialEventKind.BUY, FinancialEventKind.OPENING_BALANCE ->
                    investedAt[key] = Math.addExact(investedAt[key] ?: 0L, amount)
                FinancialEventKind.SELL ->
                    investedAt[key] = Math.subtractExact(investedAt[key] ?: 0L, amount)
                FinancialEventKind.DIVIDEND, FinancialEventKind.INTEREST ->
                    incomeAt[key] = Math.addExact(incomeAt[key] ?: 0L, amount)
                else -> Unit
            }
        }
        var invested = 0L
        var income = 0L
        return months.map { month ->
            invested = Math.addExact(invested, investedAt[month] ?: 0L)
            if (invested < 0L) invested = 0L
            income = Math.addExact(income, incomeAt[month] ?: 0L)
            MonthPoint(month, MoneyScaled(invested), MoneyScaled(income))
        }
    }

    fun build(
        holdings: List<FinancialHolding>,
        events: List<FinancialEvent>,
        members: List<GrowthMember>,
        asOf: LocalDate,
    ): GrowthBook {
        var assets = 0L
        var invested = 0L
        var knownAssets = 0L
        var liabilities = 0L
        var realised = 0L
        var unknownCount = 0
        val rows = mutableListOf<GrowthRow>()
        val buckets = linkedMapOf<String, Pair<Long, Int>>()
        val exposures = linkedMapOf<String, Pair<Long, Int>>()
        val byMember = linkedMapOf<OwnerId, Pair<Long, Int>>()
        for (holding in holdings) {
            val own = events.filter { it.holdingId == holding.holdingId }
            val position = positionOf(holding, own)
            realised = Math.addExact(realised, position.realised.raw)
            val closed = holding.category != "LIABILITY" && position.quantityScaled <= 0L
            if (closed) continue
            val unknown = holding.category != "LIABILITY" &&
                CostBasis.isUnknownInvested(holding, own, position.cost, position.value)
            if (holding.category == "LIABILITY") {
                liabilities = Math.addExact(liabilities, position.value.raw)
            } else {
                assets = Math.addExact(assets, position.value.raw)
                if (unknown) unknownCount += 1
                else {
                    invested = Math.addExact(invested, position.cost.raw)
                    knownAssets = Math.addExact(knownAssets, position.value.raw)
                }
                val bucket = HoldingView.bucketOf(holding)
                val previous = buckets[bucket] ?: (0L to 0)
                buckets[bucket] = Math.addExact(previous.first, position.value.raw) to previous.second + 1
                val exposure = exposureOf(holding)
                val earlier = exposures[exposure] ?: (0L to 0)
                exposures[exposure] = Math.addExact(earlier.first, position.value.raw) to earlier.second + 1
                val member = byMember[holding.ownerId] ?: (0L to 0)
                byMember[holding.ownerId] = Math.addExact(member.first, position.value.raw) to member.second + 1
            }
            rows += GrowthRow(
                holding.holdingId.raw,
                HoldingView.displayName(holding.name),
                HoldingView.bucketOf(holding),
                exposureOf(holding),
                holding.ownerId,
                members.find { it.id == holding.ownerId }?.name ?: "Family",
                position.value,
                if (unknown) MoneyScaled(0) else position.cost,
                if (unknown) MoneyScaled(0) else MoneyScaled(Math.subtractExact(position.value.raw, position.cost.raw)),
                position.realised,
                unknown,
            )
        }
        rows.sortByDescending { it.gain.raw }
        val bucketSlices = HoldingView.buckets.filter { it.id != "LOAN" }.mapNotNull { bucket ->
            val cell = buckets[bucket.id] ?: return@mapNotNull null
            if (cell.first <= 0L) return@mapNotNull null
            GrowthSlice(bucket.id, bucket.title, MoneyScaled(cell.first), bucketColor[bucket.id] ?: "#94a3b8", cell.second)
        }
        val exposureOrder = listOf("EQUITY", "DEBT", "GOLD", "PROPERTY", "CASH", "PRIVATE")
        val exposureSlices = exposureOrder.mapNotNull { id ->
            val cell = exposures[id] ?: return@mapNotNull null
            if (cell.first <= 0L) return@mapNotNull null
            GrowthSlice(id, id, MoneyScaled(cell.first), "#94a3b8", cell.second)
        }
        val memberSlices = members.mapIndexedNotNull { index, member ->
            val cell = byMember[member.id] ?: return@mapIndexedNotNull null
            if (cell.first <= 0L) return@mapIndexedNotNull null
            GrowthSlice(member.id.raw, member.name, MoneyScaled(cell.first), memberPalette[index % memberPalette.size], cell.second)
        }
        val assetIds = holdings.filter { it.category != "LIABILITY" }.map { it.holdingId }.toSet()
        val incomeYears = incomeByFinancialYear(events, holdings)
        val currentFy = LotEngine.fiscalYear(asOf)
        return GrowthBook(
            kpis = GrowthKpis(
                invested = MoneyScaled(invested),
                assets = MoneyScaled(assets),
                liabilities = MoneyScaled(liabilities),
                netWorth = MoneyScaled(Math.subtractExact(assets, liabilities)),
                absoluteGain = MoneyScaled(Math.subtractExact(knownAssets, invested)),
                absoluteReturnPercent = ReturnMetrics.absoluteReturnPercent(MoneyScaled(invested), MoneyScaled(knownAssets)),
                incomeThisFy = incomeYears.find { it.fy == currentFy }?.total ?: MoneyScaled(0),
                realised = MoneyScaled(realised),
                unrealised = MoneyScaled(Math.subtractExact(knownAssets, invested)),
                unknownInvestedCount = unknownCount,
            ),
            buckets = bucketSlices,
            exposures = exposureSlices,
            members = memberSlices,
            holdings = rows,
            incomeYears = incomeYears,
            months = cumulativeByMonth(events.filter { it.holdingId in assetIds }, asOf),
            currentFy = currentFy,
        )
    }

    private fun positionOf(holding: FinancialHolding, own: List<FinancialEvent>): Held {
        if (holding.category == "LIABILITY" || own.isEmpty()) {
            val metrics = HoldingView.metrics(holding)
            return Held(metrics.quantityScaled, metrics.value, metrics.cost, MoneyScaled(0))
        }
        val folded = LotEngine.fold(holding.holdingId, holding.category, own)
        val price = HoldingView.priceOf(holding)
        val value = if (folded.quantityScaled > 0L) {
            MoneyScaled(timesQuantity(folded.quantityScaled, price.raw))
        } else MoneyScaled(0)
        return Held(folded.quantityScaled, value, folded.investedCost, folded.realisedGain)
    }

    private fun exposureOf(holding: FinancialHolding): String {
        val text = "${holding.name} ${holding.subCategory}".lowercase()
        return when {
            holding.category == "GOLD_SGB" || Regex("\\bgold\\b|\\bsgb\\b").containsMatchIn(text) -> "GOLD"
            holding.category == "REAL_ESTATE" || Regex("\\breit\\b|\\binvit\\b|property").containsMatchIn(text) -> "PROPERTY"
            holding.category == "CASH_BANK" -> "CASH"
            holding.category == "FIXED_INCOME" -> "DEBT"
            holding.category == "SPECIALISED" -> "PRIVATE"
            else -> "EQUITY"
        }
    }

    private fun eventCash(event: FinancialEvent): Long {
        if (event.totalAmount.raw != 0L) return kotlin.math.abs(event.totalAmount.raw)
        val unit = event.unitPrice?.raw ?: return 0L
        val quantity = kotlin.math.abs(event.quantityScaled)
        if (quantity == 0L || unit == 0L) return 0L
        return kotlin.math.abs(timesQuantity(quantity, unit))
    }
}

private data class Held(
    val quantityScaled: Long,
    val value: MoneyScaled,
    val cost: MoneyScaled,
    val realised: MoneyScaled,
)

data class GrowthMember(val id: OwnerId, val name: String)

data class IncomeYear(
    val fy: String,
    val dividend: MoneyScaled,
    val interest: MoneyScaled,
    val rental: MoneyScaled,
) {
    val total: MoneyScaled get() = dividend + interest + rental
}

data class MonthPoint(val month: LocalDate, val invested: MoneyScaled, val income: MoneyScaled)

data class GrowthSlice(
    val id: String,
    val label: String,
    val value: MoneyScaled,
    val color: String,
    val count: Int,
)

data class GrowthRow(
    val id: String,
    val name: String,
    val bucket: String,
    val exposure: String,
    val ownerId: OwnerId,
    val ownerName: String,
    val value: MoneyScaled,
    val cost: MoneyScaled,
    val gain: MoneyScaled,
    val realised: MoneyScaled,
    val unknownInvested: Boolean,
)

data class GrowthKpis(
    val invested: MoneyScaled,
    val assets: MoneyScaled,
    val liabilities: MoneyScaled,
    val netWorth: MoneyScaled,
    val absoluteGain: MoneyScaled,
    val absoluteReturnPercent: Double?,
    val incomeThisFy: MoneyScaled,
    val realised: MoneyScaled,
    val unrealised: MoneyScaled,
    val unknownInvestedCount: Int,
)

data class GrowthBook(
    val kpis: GrowthKpis,
    val buckets: List<GrowthSlice>,
    val exposures: List<GrowthSlice>,
    val members: List<GrowthSlice>,
    val holdings: List<GrowthRow>,
    val incomeYears: List<IncomeYear>,
    val months: List<MonthPoint>,
    val currentFy: String,
)
