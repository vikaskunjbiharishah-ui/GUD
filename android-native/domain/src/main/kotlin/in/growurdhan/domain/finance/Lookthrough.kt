package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled

/**
 * Look-through from src/lib/lookthrough.ts.
 * A hybrid is listed once, under the larger slice, and counted in proportion.
 * It is not copied into every bucket, and it is not dumped entirely into one.
 */
object Lookthrough {
    private data class DefaultSplit(val match: List<String>, val label: String, val slices: List<Slice>)
    private val defaults = listOf(
        DefaultSplit(listOf("aggressive hybrid"), "Aggressive hybrid (SEBI minimum 65% equity)", listOf(Slice("EQUITY", 6500), Slice("DEBT", 3500))),
        DefaultSplit(listOf("conservative hybrid"), "Conservative hybrid (SEBI minimum 75% debt)", listOf(Slice("DEBT", 7500), Slice("EQUITY", 2500))),
        DefaultSplit(listOf("balanced advantage", "dynamic asset allocation"), "Balanced advantage (assumed 65/35)", listOf(Slice("EQUITY", 6500), Slice("DEBT", 3500))),
        DefaultSplit(listOf("balanced hybrid"), "Balanced hybrid (assumed 50/50)", listOf(Slice("EQUITY", 5000), Slice("DEBT", 5000))),
        DefaultSplit(listOf("multi asset", "multi-asset"), "Multi asset (assumed 50/30/20)", listOf(Slice("EQUITY", 5000), Slice("DEBT", 3000), Slice("GOLD", 2000))),
        DefaultSplit(listOf("equity savings"), "Equity savings (assumed 35/65)", listOf(Slice("EQUITY", 3500), Slice("DEBT", 6500))),
        DefaultSplit(listOf("nps tier", "nps pension"), "NPS (assumed 50/30/20 E/C/G)", listOf(Slice("EQUITY", 5000), Slice("DEBT", 5000))),
        DefaultSplit(listOf("ulip"), "ULIP (assumed 60/40)", listOf(Slice("EQUITY", 6000), Slice("DEBT", 4000))),
    )

    fun resolve(
        holding: FinancialHolding,
        placement: Placement,
        allocation: List<UserSlice> = emptyList(),
    ): ResolvedSplit {
        if (allocation.isNotEmpty()) {
            val slices = allocation.map { Slice(it.exposure, java.lang.Math.round(it.percent * 100).toInt()) }.toMutableList()
            val total = slices.sumOf { it.basisPoints }
            if (total > 0) {
                slices[0] = slices[0].copy(basisPoints = slices[0].basisPoints + (10_000 - total))
                return ResolvedSplit(slices, estimated = false, source = "You set this split")
            }
        }
        val text = "${holding.name} ${holding.subCategory}".lowercase()
        for (preset in defaults) {
            if (preset.match.any { text.contains(it) }) {
                return ResolvedSplit(preset.slices, estimated = true, source = preset.label)
            }
        }
        return ResolvedSplit(listOf(Slice(placement.exposure, 10_000)), estimated = false, source = "Single asset class")
    }

    fun isHybrid(split: ResolvedSplit): Boolean = split.slices.size > 1

    /** Integer slices. Any paisa left over is added to the largest slice. */
    fun apportion(value: MoneyScaled, split: ResolvedSplit): Map<String, MoneyScaled> {
        if (split.slices.size == 1) return mapOf(split.slices[0].exposure to value)
        val out = linkedMapOf<String, Long>()
        var assigned = 0L
        var largest = split.slices[0]
        for (slice in split.slices) {
            if (slice.basisPoints > largest.basisPoints) largest = slice
            val part = (value.raw.toBigInteger() * slice.basisPoints.toBigInteger() / 10_000.toBigInteger()).toLong()
            out[slice.exposure] = Math.addExact(out[slice.exposure] ?: 0L, part)
            assigned = Math.addExact(assigned, part)
        }
        val remainder = Math.subtractExact(value.raw, assigned)
        if (remainder != 0L) {
            out[largest.exposure] = Math.addExact(out[largest.exposure] ?: 0L, remainder)
        }
        return out.mapValues { MoneyScaled(it.value) }
    }

    fun build(holdings: List<FinancialHolding>, valueOf: (FinancialHolding) -> MoneyScaled): MoneyTree {
        val totals = linkedMapOf<String, Long>()
        val placements = holdings.map { holding ->
            val placement = Taxonomy.classify(holding)
            val split = resolve(holding, placement)
            val value = valueOf(holding)
            val dominant = split.slices.reduce { left, right ->
                if (right.basisPoints > left.basisPoints) right else left
            }.exposure
            for ((node, part) in apportion(value, split)) {
                totals[node] = Math.addExact(totals[node] ?: 0L, part.raw)
            }
            PlacedHolding(holding, placement, split, value, dominant)
        }
        val nodes = Taxonomy.exposures.mapIndexed { index, exposure ->
            val levels = exposure.levels.map { level ->
                val rows = placements.filter { it.dominant == exposure.id && it.placement.level == level.id }
                ExposureLevel(
                    level.id, level.label,
                    MoneyScaled(rows.fold(0L) { sum, row -> Math.addExact(sum, row.value.raw) }),
                    rows,
                )
            }
            ExposureBranch(
                exposure.id, exposure.label, index + 1,
                MoneyScaled(totals[exposure.id] ?: 0L),
                levels,
                levels.any { it.rows.isNotEmpty() },
            )
        }
        return MoneyTree(nodes, totals.mapValues { MoneyScaled(it.value) }, placements)
    }

    fun activeWrappers(rows: List<PlacedHolding>): List<String> {
        val present = rows.map { it.placement.wrapper }.toSet()
        return Taxonomy.wrappers.map { it.id }.filter { it in present }
    }
}

data class UserSlice(val exposure: String, val percent: Double)
data class Slice(val exposure: String, val basisPoints: Int)
data class ResolvedSplit(val slices: List<Slice>, val estimated: Boolean, val source: String)
data class PlacedHolding(
    val holding: FinancialHolding,
    val placement: Placement,
    val split: ResolvedSplit,
    val value: MoneyScaled,
    val dominant: String,
)
data class ExposureLevel(val id: String, val label: String, val value: MoneyScaled, val rows: List<PlacedHolding>)
data class ExposureBranch(
    val id: String,
    val label: String,
    val order: Int,
    val value: MoneyScaled,
    val levels: List<ExposureLevel>,
    val hasHoldings: Boolean,
)
data class MoneyTree(
    val nodes: List<ExposureBranch>,
    val totals: Map<String, MoneyScaled>,
    val placements: List<PlacedHolding>,
)
