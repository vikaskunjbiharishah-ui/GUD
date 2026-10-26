package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts sections C through H. */
class LookthroughTest {
    private fun holding(name: String, category: String, sub: String, rupees: Long = 100_000) = FinancialHolding(
        HoldingId("h-$name"), OwnerId("f1"), name, category, MoneyScaled(rupees * 10_000), 100_000_000L, subCategory = sub,
    )
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)

    @Test fun hybridsAreSplitWithoutLosingAPaisa() {
        val balanced = holding("HDFC Balanced Advantage Fund", "MUTUAL_FUND", "Balanced Advantage", 142_000)
        val split = Lookthrough.resolve(balanced, Taxonomy.classify(balanced))
        assertTrue(Lookthrough.isHybrid(split))
        assertTrue(split.estimated)
        assertEquals("Balanced advantage (assumed 65/35)", split.source)
        val parts = Lookthrough.apportion(rupees(142_000), split)
        assertEquals(92_300L, parts.getValue("EQUITY").rupees().longValueExact())
        assertEquals(49_700L, parts.getValue("DEBT").rupees().longValueExact())
        assertEquals(142_000L, (parts.getValue("EQUITY").raw + parts.getValue("DEBT").raw) / 10_000)

        val multi = holding("Multi Asset Fund", "MUTUAL_FUND", "Multi Asset Allocation")
        val awkward = MoneyScaled(1_000_000_000_007)
        val spread = Lookthrough.apportion(awkward, Lookthrough.resolve(multi, Taxonomy.classify(multi)))
        assertEquals(3, spread.size)
        assertEquals(awkward.raw, spread.values.sumOf { it.raw })
        assertEquals(500_000_000_004L, spread.getValue("EQUITY").raw)

        val custom = Lookthrough.resolve(
            holding("My Hybrid", "MUTUAL_FUND", "Balanced Advantage", 100_000),
            Taxonomy.classify(holding("My Hybrid", "MUTUAL_FUND", "Balanced Advantage")),
            listOf(UserSlice("EQUITY", 70.0), UserSlice("DEBT", 30.0)),
        )
        assertFalse(custom.estimated)
        assertEquals("You set this split", custom.source)
        assertEquals(70_000L, Lookthrough.apportion(rupees(100_000), custom).getValue("EQUITY").rupees().longValueExact())

        val share = holding("Reliance Industries", "EQUITY", "Large Cap Conglomerate")
        val single = Lookthrough.resolve(share, Taxonomy.classify(share))
        assertFalse(Lookthrough.isHybrid(single))
        assertEquals("Single asset class", single.source)

        val nps = Lookthrough.resolve(holding("NPS Tier I", "FIXED_INCOME", "NPS Pension"), Taxonomy.classify(holding("NPS", "FIXED_INCOME", "NPS")))
        assertEquals(2, nps.slices.size)
        assertEquals(listOf(5_000, 5_000), nps.slices.map { it.basisPoints })
    }

    @Test fun treeListsAHybridOnceAndCountsItInProportion() {
        val book = listOf(
            holding("Reliance Industries", "EQUITY", "Large Cap Conglomerate", 389_000),
            holding("HDFC Balanced Advantage Fund", "MUTUAL_FUND", "Balanced Advantage", 142_000),
            holding("SGB 2019-20 Series V", "GOLD_SGB", "RBI Sovereign Gold Bond", 412_000),
            holding("SBI Gilt Fund", "MUTUAL_FUND", "Gilt Fund", 200_000),
            holding("Nifty 50 Index Fund", "MUTUAL_FUND", "Index Fund", 112_000),
        )
        val tree = Lookthrough.build(book) { rupees(it.unitPrice.rupees().longValueExact()) }
        assertEquals(389_000L + 112_000L + 92_300L, tree.totals.getValue("EQUITY").rupees().longValueExact())
        assertEquals(200_000L + 49_700L, tree.totals.getValue("DEBT").rupees().longValueExact())
        assertEquals(412_000L, tree.totals.getValue("GOLD").rupees().longValueExact())
        val listed = tree.nodes.flatMap { it.levels }.flatMap { it.rows }.filter { "Balanced" in it.holding.name }
        assertEquals(1, listed.size)
        assertEquals("EQUITY", listed.single().dominant)
        assertFalse(tree.nodes.single { it.id == "PROTECTION" }.hasHoldings)
        val largeCap = tree.nodes.single { it.id == "EQUITY" }.levels.single { it.id == "LARGE_CAP" }
        assertEquals(listOf("DIRECT", "INDEX_ETF"), Lookthrough.activeWrappers(largeCap.rows))
        assertTrue(Lookthrough.activeWrappers(largeCap.rows).size < 11)
        assertEquals(501_000L, largeCap.value.rupees().longValueExact())
    }
}
