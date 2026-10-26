package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section X and src/lib/homeDashboard.ts. */
class HomeBooksTest {
    private val owner = OwnerId("f1")
    private val you = FamilyBooksMember(owner, "You")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun equity(id: String, name: String, cost: Long, price: Long) = FinancialHolding(
        HoldingId(id), owner, name, "EQUITY", rupees(price), 10 * QUANTITY_SCALE,
        openingCostPerUnit = rupees(cost), subCategory = "Large Cap",
    )

    @Test fun freshnessConcentrationAndCostFlagsMatchTauri() {
        assertEquals(4L, HomeBooks.daysBetween("2026-09-12", "2026-09-16"))
        assertNull(HomeBooks.daysBetween("nope", "2026-09-16"))
        assertEquals("2025-26", HomeBooks.previousFinancialYear("2026-27"))
        assertEquals(date("2026-03-31"), HomeBooks.financialYearEnd("2025-26"))
        assertEquals(HealthTone.OK, HomeBooks.freshness(date("2026-09-12"), date("2026-09-16"), date("2026-09-16")))
        assertEquals(HealthTone.AMBER, HomeBooks.freshness(date("2026-07-01"), date("2026-09-16"), date("2026-09-16")))
        assertEquals(HealthTone.RED, HomeBooks.freshness(date("2026-04-01"), date("2026-09-16"), date("2026-09-16")))

        val reliance = equity("h_eq", "Reliance", 100, 100)
        val tcs = equity("h_eq2", "TCS", 100, 140)
        val unknown = HomeBooks.booksHealth(BooksHealthInput(
            listOf(reliance), emptyList(), listOf(you), emptyList(), null, date("2026-09-16"),
        ))
        assertEquals(HealthTone.AMBER, unknown.tone)
        assertEquals("1 thing to check", unknown.headline)
        assertEquals(HealthKind.COST_EQUALS_VALUE, unknown.findings.single().kind)
        assertEquals("Reliance has no separate purchase cost. CAS does not print that.", unknown.findings.single().detail)

        val gained = HomeBooks.booksHealth(BooksHealthInput(
            listOf(tcs), emptyList(), listOf(you), emptyList(), null, date("2026-09-16"),
        ))
        assertFalse(gained.findings.any { it.kind == HealthKind.COST_EQUALS_VALUE })
        assertEquals(HealthTone.OK, gained.tone)

        val stale = HomeBooks.booksHealth(BooksHealthInput(
            listOf(tcs), emptyList(), listOf(you),
            listOf(StatementStamp(owner, "CDSL", date("2026-06-01"))),
            null, date("2026-09-16"),
        ))
        val statement = stale.findings.single { it.kind == HealthKind.STALE_STATEMENT }
        assertEquals(HealthTone.RED, statement.tone)
        assertEquals("CDSL statement is 107 days old", statement.title)
        assertEquals("You has no PAN on file", stale.findings.single { it.kind == HealthKind.MEMBER_PAN_MISSING }.title)
        assertEquals("2 things to check", stale.headline)

        val ppf = FinancialHolding(
            HoldingId("h_ppf"), owner, "PPF SBI", "FIXED_INCOME", rupees(500_000), QUANTITY_SCALE,
            openingCostPerUnit = rupees(500_000), subCategory = "PPF", acquiredOn = date("2020-04-01"),
        )
        val ppfHealth = HomeBooks.booksHealth(BooksHealthInput(
            listOf(ppf),
            listOf(FinancialEvent(
                "ob", ppf.holdingId, owner, FinancialEventKind.OPENING_BALANCE, date("2020-04-01"),
                QUANTITY_SCALE, rupees(500_000), rupees(500_000),
            )),
            listOf(you.copy(panForStatements = "ABCDE1234F")),
            emptyList(), null, date("2026-09-16"),
        ))
        assertEquals("No FY 2025-26 interest on PPF SBI", ppfHealth.findings.single().title)
        val posted = ppfHealth.findings
        assertTrue(posted.isNotEmpty())
        val credited = HomeBooks.booksHealth(BooksHealthInput(
            listOf(ppf),
            listOf(FinancialEvent(
                "int", ppf.holdingId, owner, FinancialEventKind.INTEREST, date("2026-03-31"),
                totalAmount = rupees(35_000),
            )),
            listOf(you.copy(panForStatements = "ABCDE1234F")),
            emptyList(), null, date("2026-09-16"),
        ))
        assertFalse(credited.findings.any { it.kind == HealthKind.MISSING_FY_INTEREST })

        val mix = HomeBooks.concentration(listOf(
            ValuedHolding(HoldingId("a"), "A", MoneyScaled(60_000_000)),
            ValuedHolding(HoldingId("b"), "B", MoneyScaled(30_000_000)),
            ValuedHolding(HoldingId("c"), "C", MoneyScaled(10_000_000)),
        ), MoneyScaled(100_000_000), 2)
        assertEquals(90.0, mix.topSharePercent)
        assertEquals("A", mix.rows[0].name)
        assertEquals(60.0, mix.rows[0].percent)
    }

    @Test fun statementStepsCarryTheLastFileForward() {
        fun snap(day: String, issuer: String, rupees: Long, member: OwnerId = owner) =
            StatementSnap(date(day), issuer, member, rupees(rupees), MoneyScaled(0))
        val mixed = HomeBooks.statementSteps(listOf(
            snap("2026-01-31", "CDSL", 100),
            snap("2026-02-28", "CAMS", 40),
            snap("2026-03-31", "CDSL", 110),
        ), null, date("2026-03-31"))
        assertEquals(listOf(100L, 140L, 150L), mixed.map { it.value.rupees().longValueExact() })
        assertFalse(mixed.any { it.livePad })

        val padded = HomeBooks.statementSteps(listOf(snap("2026-01-31", "CDSL", 100)), null, date("2026-09-16"))
        assertTrue(padded.last().livePad)
        assertEquals(100L, padded.last().value.rupees().longValueExact())
        assertEquals(0, HomeBooks.statementSteps(
            listOf(snap("2026-01-31", "CDSL", 100)), OwnerId("f2"), date("2026-09-16"),
        ).size)

        assertEquals(50.0, HomeBooks.firstToLastPercent(listOf(100, 150)))
        assertNull(HomeBooks.firstToLastPercent(listOf(100)))
        val carried = HomeBooks.investedSeries(listOf(
            StatementStep(date("2026-01-31"), rupees(100), MoneyScaled(80), listOf("CDSL"), false),
            StatementStep(date("2026-03-31"), rupees(120), MoneyScaled(0), listOf("CDSL"), false),
        ), MoneyScaled(90))
        assertEquals(80L, carried[1].invested.raw)

        val tcs = equity("h_eq2", "TCS", 100, 140)
        val since = HomeBooks.sinceLastStatement(SinceLastInput(
            listOf(HoldingSnap(date("2026-08-31"), tcs.holdingId, owner, rupees(1_000))),
            listOf(tcs), emptyList(), null,
        ))
        assertEquals(date("2026-08-31"), since.asOf)
        assertEquals(1_400L, since.nowValue.rupees().longValueExact())
        assertEquals(1_000L, since.thenValue.rupees().longValueExact())
        assertEquals("TCS", since.movers.single().name)

        val reliance = equity("h_eq", "MURG - Reliance", 100, 100)
        val rows = HomeBooks.unknownCostRows(listOf(reliance, tcs), emptyList())
        assertEquals(listOf("Reliance"), rows.map { it.name })
        assertEquals("Edelweiss Multi Cap Fund", HomeBooks.displayName("MURG - Edelweiss Multi Cap Fund"))
    }
}
