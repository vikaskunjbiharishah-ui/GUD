package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.round

/** Goldens from src/lib/kosha.test.ts section K. */
class MatchHoldingTest {
    private val owner = OwnerId("f1")
    private val folio = "17001385/94"
    private fun row(name: String, isin: String?, folio: String) =
        StatementRow(name, isin, folio)
    private fun known(id: String, row: StatementRow) = KnownHolding(
        id, owner, row.holdingName, isin = row.isin, folio = row.folio,
    )

    @Test fun sameFolioDoesNotMergeDifferentSchemes() {
        val momentum = row("3572 - ICICI Prudential Active Momentum Fund - Growth", "INF109K1A377", folio)
        val multi = row("15 - ICICI Prudential Multi-Asset Fund - Growth", "INF109K01761", folio)
        val avap = row("AVAP - ICICI Prudential Aggressive Hybrid Active FOF - Growth", "INF109K01852", folio)
        val edelweiss = "9109231221/0"
        val cycle = row("CYRG - Edelweiss Business Cycle Fund", "INF754K01SW8", edelweiss)
        val multiCap = row("MURG - Edelweiss Multi Cap Fund", "INF754K01RW0", edelweiss)

        assertNull(MatchHolding.find(listOf(known("h_mom", momentum)), multi, owner))
        assertEquals("h_av", MatchHolding.find(listOf(known("h_av", avap)), avap, owner)?.id)
        assertNull(MatchHolding.find(listOf(known("h_cyrg", cycle)), multiCap, owner))

        val working = mutableListOf<KnownHolding>()
        for (record in listOf(momentum, multi, avap, avap, cycle, multiCap)) {
            if (MatchHolding.find(working, record, owner) == null) working += known("h${working.size}", record)
        }
        assertEquals(5, working.size)

        val mid = row("101G - WhiteOak Capital - Mid Cap Fund - Regular Plan Growth", "INF03CN01103", "1000129793")
        val large = row("111G - WhiteOak Capital - Large Cap Fund - Regular Plan Growth", "INF03CN01111", "1000129793")
        assertNull(MatchHolding.find(listOf(known("h_wo", mid)), large, owner))
        val unnamed = known("h_noisin", mid.copy(isin = null))
        assertEquals("h_noisin", MatchHolding.find(listOf(unnamed), mid, owner)?.id)
        assertFalse(MatchHolding.identityKey(known("a", momentum)) == MatchHolding.identityKey(known("b", multi)))

        val icici = listOf(
            Triple("3572 - ICICI Prudential Active Momentum Fund - Growth", "INF109K1A377", 29998.5 * 12.81),
            Triple("15 - ICICI Prudential Multi-Asset Fund - Growth", "INF109K01761", 8160.55 * 756.6303),
            Triple("AVAP - ICICI Prudential Aggressive Hybrid Active FOF - Growth", "INF109K01852", 38.415 * 221.04),
            Triple("AVAP - ICICI Prudential Aggressive Hybrid Active FOF - Growth", "INF109K01852", 11703.407 * 221.04),
            Triple("ICICI Prudential Value Fund", "INF109K01AF8", 1012.787 * 180.0),
        )
        val book = mutableListOf<KnownHolding>()
        var correct = 0.0
        var units = 29998.5 + 8160.55 + 38.415 + 11703.407 + 1012.787
        for ((name, isin, value) in icici) {
            val record = row(name, isin, folio)
            correct += value
            if (MatchHolding.find(book, record, owner) == null) book += known("h${book.size}", record)
        }
        assertEquals(4, book.size)
        assertEquals(9_336_514L, round(correct).toLong())
        assertEquals(9_164_459L, round(units * 180.0).toLong())
        assertTrue(round(correct).toLong() != round(units * 180.0).toLong())
    }

    @Test fun placeholdersDoNotBecomeASecondHolding() {
        assertTrue(MatchHolding.isPlaceholderName("MUTUAL FUND UNITS"))
        assertTrue(MatchHolding.isPlaceholderName("MURG"))
        assertTrue(MatchHolding.isPlaceholderName("17001385/94"))
        assertTrue(MatchHolding.isPlaceholderName(""))
        assertFalse(MatchHolding.isPlaceholderName("MURG - Edelweiss Multi Cap Fund - Regular Plan Growth"))
        assertEquals(
            "MURG - Edelweiss Multi Cap Fund",
            MatchHolding.preferSchemeName("MUTUAL FUND UNITS", "MURG - Edelweiss Multi Cap Fund"),
        )
        assertEquals(
            "Edelweiss Multi Cap Fund",
            MatchHolding.preferSchemeName("Edelweiss Multi Cap Fund", "MUTUAL FUND UNITS"),
        )
        assertEquals(
            "Edelweiss Multi Cap Fund - Regular Plan Growth",
            MatchHolding.preferSchemeName("Edelweiss Multi", "Edelweiss Multi Cap Fund - Regular Plan Growth"),
        )

        val edelweiss = "9109231221/0"
        val cycle = row("CYRG - Edelweiss Business Cycle Fund", "INF754K01SW8", edelweiss)
        val stub = KnownHolding("h_stub", owner, "MUTUAL FUND UNITS", folio = edelweiss)
        val placeholder = StatementRow("MUTUAL FUND UNITS", folio = edelweiss)
        assertEquals("h_stub", MatchHolding.find(listOf(stub), placeholder, owner)?.id)
        assertNull(MatchHolding.find(listOf(known("h_cyrg", cycle), known("h_murg", row("MURG - Edelweiss Multi Cap Fund", "INF754K01RW0", edelweiss))), placeholder, owner))

        val byAmfi = KnownHolding("h_amfi", owner, "Parag Parikh Flexi Cap", amfiCode = "122639")
        assertEquals("h_amfi", MatchHolding.find(listOf(byAmfi), StatementRow("Flexi", amfiCode = "122639"), owner)?.id)
        assertNull(MatchHolding.find(listOf(known("h_mom", row("Reliance", "INE002A01018", "IN300000"))), StatementRow("Reliance"), OwnerId("f2")))
        assertEquals("", MatchHolding.folioPrefix("MF_FOLIO"))
    }
}
