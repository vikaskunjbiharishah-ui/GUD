package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Fixed-input parity oracles taken from:
 * src/lib/kosha.test.ts, src/lib/snapshot.test.ts and src/lib/lots.ts.
 * No CASPARSER code or real statement files are used.
 */
class FinancialGoldenTest {
    private val owner = OwnerId("f1")
    private val holdingId = HoldingId("h1")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(value: Long) = MoneyScaled(Math.multiplyExact(value, 10_000))
    private fun ev(
        id: String, kind: FinancialEventKind, on: String, qty: Long,
        price: Long = 0, holding: HoldingId = holdingId, ownerId: OwnerId = owner,
        supersedes: String? = null,
    ) = FinancialEvent(id, holding, ownerId, kind, date(on),
        Math.multiplyExact(qty, QUANTITY_SCALE), rupees(price),
        rupees(Math.multiplyExact(qty, price)), supersedesEventId = supersedes)
    private fun holding(
        category: String = "EQUITY",
        price: Long = 2950,
        storedQuantity: Long = 999,
        ownerId: OwnerId = owner,
        id: HoldingId = holdingId,
    ) = FinancialHolding(id, ownerId, "Synthetic holding", category, rupees(price),
        Math.multiplyExact(storedQuantity, QUANTITY_SCALE))

    @Test fun goldenFifoLedgerNotStoredQuantity() {
        val events = listOf(
            ev("e1", FinancialEventKind.BUY, "2024-03-14", 100, 2450),
            ev("e2", FinancialEventKind.BUY, "2024-09-01", 50, 2600),
            ev("e3", FinancialEventKind.SELL, "2025-04-09", 40, 2750),
        )
        val position = LotEngine.fold(holdingId, "EQUITY", events)
        assertEquals(110 * QUANTITY_SCALE, position.quantityScaled)
        assertEquals(277_000, position.investedCost.rupees().longValueExact())
        assertEquals(2_518, position.averageUnitCost.rupees().toLong())
        assertEquals(12_000, position.realisedGain.rupees().longValueExact())
        assertTrue(position.disposals.first().longTerm) // 391 days > 365
        assertEquals(391, LotEngine.calendarDays(date("2024-03-14"), date("2025-04-09")))
        val book = PortfolioEngine.derive(listOf(holding()), events, date("2026-08-01"))
        assertEquals(324_500, book.familyNetWorth.rupees().longValueExact())
        assertEquals(110 * QUANTITY_SCALE, book.members.single().holdings.single().position.quantityScaled)
    }

    @Test fun saleChargesReduceRealisedProceedsWithoutChangingAcquisitionLots() {
        val bought = ev("paid", FinancialEventKind.BUY, "2026-01-01", 2, 100)
        val sold = FinancialEvent(
            "disposed", holdingId, owner, FinancialEventKind.SELL,
            date("2026-10-01"), -QUANTITY_SCALE,
            rupees(150), rupees(150), rupees(5),
        )
        val result = LotEngine.fold(holdingId, "EQUITY", listOf(bought, sold))
        assertTrue(result.errors.isEmpty())
        assertEquals(QUANTITY_SCALE, result.quantityScaled)
        assertEquals(100L, result.investedCost.rupees().longValueExact())
        assertEquals(145L, result.disposals.single().proceeds.rupees().longValueExact())
        assertEquals(45L, result.realisedGain.rupees().longValueExact())
    }

    @Test fun bonusAndSplitPreserveInvestedCost() {
        val events = listOf(
            ev("buy", FinancialEventKind.BUY, "2025-01-01", 10, 100),
            ev("bonus", FinancialEventKind.BONUS, "2025-02-01", 5),
            FinancialEvent("split", holdingId, owner, FinancialEventKind.SPLIT,
                date("2025-03-01"), 2, MoneyScaled(1)),
        )
        val folded = LotEngine.fold(holdingId, "EQUITY", events)
        assertEquals(30 * QUANTITY_SCALE, folded.quantityScaled)
        assertEquals(1000, folded.investedCost.rupees().longValueExact())
    }

    @Test fun oversellReportedAndSnapshotReductionsNotTaxable() {
        val buy = ev("buy", FinancialEventKind.BUY, "2026-01-31", 10, 100)
        val close = ev("adj", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-02-28", -10)
        val p = LotEngine.fold(holdingId, "MUTUAL_FUND", listOf(buy, close))
        assertEquals(0, p.quantityScaled)
        assertTrue(p.disposals.isEmpty())
        assertTrue(p.errors.isEmpty())
        val tooFar = LotEngine.fold(holdingId, "MUTUAL_FUND", listOf(buy,
            ev("more", FinancialEventKind.SNAPSHOT_ADJUSTMENT, "2026-02-28", -20)))
        assertTrue(tooFar.errors.any { it.contains("exceeds units held") })
        val oversold = LotEngine.fold(holdingId, "EQUITY", listOf(buy,
            ev("sell", FinancialEventKind.SELL, "2026-03-10", 50, 120)))
        assertTrue(oversold.errors.any { it.contains("exceeds units held") })
    }

    @Test fun shortTermLossIsKept() {
        val folded = LotEngine.fold(holdingId, "EQUITY", listOf(
            ev("l1", FinancialEventKind.BUY, "2024-01-01", 10, 1000),
            ev("l2", FinancialEventKind.SELL, "2024-06-01", 10, 800),
        ))
        assertTrue(folded.errors.isEmpty())
        assertEquals(-2_000L, folded.realisedGain.rupees().longValueExact())
        assertEquals(-20_000_000L, folded.disposals.single().gain.raw)
        assertFalse(folded.disposals.single().longTerm)
        assertEquals(152, LotEngine.calendarDays(date("2024-01-01"), date("2024-06-01")))
    }

    @Test fun fractionalCorrectionMatchesTauriOpeningCost() {
        // kosha.test.ts: 6,418.7052 units corrected to an invested cost of ₹3,17,909.45.
        // Integer division of that cost back into a unit price loses ₹0.6096, which the
        // Tauri assertion allows (within ₹1) and this test pins exactly.
        val holding = HoldingId("hnps")
        val qty = 641_870_520_000L
        val opening = FinancialEvent(
            "obc", holding, owner, FinancialEventKind.OPENING_BALANCE,
            date("2026-05-31"), qty, MoneyScaled(750_840),
        )
        val correction = FinancialEvent(
            "cor1", holding, owner, FinancialEventKind.CORRECTION,
            date("2026-05-31"), qty, MoneyScaled(495_285), MoneyScaled(3_179_094_500L),
            supersedesEventId = "obc",
        )
        val folded = LotEngine.fold(holding, "FIXED_INCOME", listOf(opening, correction))
        assertTrue(folded.errors.isEmpty())
        assertEquals(qty, folded.quantityScaled)
        assertEquals(3_179_088_404L, folded.investedCost.raw)
        assertEquals(1, folded.openLots.size)
    }

    @Test fun correctionSupersedesOpeningWithoutChangingQuantity() {
        val opening = ev("ob", FinancialEventKind.OPENING_BALANCE, "2026-05-31", 100, 75)
        val correction = FinancialEvent("cor", holdingId, owner,
            FinancialEventKind.CORRECTION, date("2026-05-31"), 100 * QUANTITY_SCALE,
            rupees(50), rupees(5000), supersedesEventId = "ob")
        val p = LotEngine.fold(holdingId, "FIXED_INCOME", listOf(opening, correction))
        assertEquals(100 * QUANTITY_SCALE, p.quantityScaled)
        assertEquals(5_000, p.investedCost.rupees().longValueExact())
        assertEquals(1, p.openLots.size)
    }

    @Test fun fiscalYearAndTaxRulesMatchLegacyOracles() {
        assertEquals("2024-25", LotEngine.fiscalYear(date("2025-03-31")))
        assertEquals("2025-26", LotEngine.fiscalYear(date("2025-04-01")))
        assertEquals("AY26-27", LotEngine.assessmentYear(date("2025-06-10")))
        assertNull(IndianTaxRules.forAssessmentYear("AY40-41"))
        val rules = requireNotNull(IndianTaxRules.forAssessmentYear("AY26-27"))
        assertEquals(12_500, IndianTaxRules.applyRate(rupees(100_000), rules.ltcgEquityBps).rupees().longValueExact())
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Gilt Fund - Long Duration"))
        assertTrue(IndianTaxRules.equityOriented("MUTUAL_FUND", "Flexi Cap Fund"))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("MUTUAL_FUND", "Corporate Bond Fund", false))
        assertEquals(GainClass.SGB_EXEMPT,
            IndianTaxRules.classify("GOLD_SGB", "Sovereign Gold Bond", true, "CERTIFICATE"))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("GOLD_SGB", "Sovereign Gold Bond", true, "DEMAT"))
        assertEquals(rupees(400), IndianTaxRules.grandfatheredCost(
            rules, date("2015-06-01"), rupees(100), rupees(900), rupees(400)))
        assertEquals(rupees(300), IndianTaxRules.grandfatheredCost(
            rules, date("2015-06-01"), rupees(100), rupees(300), rupees(900)))
        assertEquals(rupees(400), IndianTaxRules.grandfatheredCost(
            rules, date("2018-01-31"), rupees(100), rupees(900), rupees(400)))
        assertEquals(rupees(100), IndianTaxRules.grandfatheredCost(
            rules, date("2018-02-01"), rupees(100), rupees(900), rupees(400)))
        assertEquals(rupees(100), IndianTaxRules.grandfatheredCost(
            rules, date("2015-06-01"), rupees(100), rupees(900), null))
        assertEquals(rupees(100), IndianTaxRules.grandfatheredCost(
            rules, date("2015-06-01"), rupees(100), rupees(900), MoneyScaled(0)))
        val noGrandfather = TaxRuleSet("AY00-01", grandfatherDate = null)
        assertEquals(rupees(100), IndianTaxRules.grandfatheredCost(
            noGrandfather, date("2015-06-01"), rupees(100), rupees(900), rupees(400)))
    }

    @Test fun taxRuleEdgesMatchTauri() {
        assertEquals(listOf("AY25-26", "AY26-27", "AY27-28"), IndianTaxRules.publishedYears())
        val rules = requireNotNull(IndianTaxRules.forAssessmentYear("AY26-27"))
        assertEquals(125_000L, rules.ltcgExemptionRupees)
        assertEquals(2_000, rules.stcgEquityBps)
        assertEquals(365, rules.equityLongTermDays)
        assertEquals(730, rules.realEstateLongTermDays)
        assertEquals(1_095, rules.otherLongTermDays)
        assertEquals(date("2018-01-31"), rules.grandfatherDate)
        assertTrue(rules.debtMfAtSlab)
        assertTrue(rules.sgbRedemptionExempt)
        assertFalse(rules.provisional)
        val provisional = requireNotNull(IndianTaxRules.forAssessmentYear("AY27-28"))
        assertTrue(provisional.provisional)
        assertTrue(provisional.notes.contains("PROVISIONAL"))
        assertEquals(20_000L, IndianTaxRules.applyRate(rupees(100_000), rules.stcgEquityBps).rupees().longValueExact())
        assertEquals(0L, IndianTaxRules.applyRate(MoneyScaled(1), rules.ltcgEquityBps).raw)
        assertEquals(0L, IndianTaxRules.applyRate(rupees(-100), rules.ltcgEquityBps).raw)

        assertTrue(IndianTaxRules.equityOriented("EQUITY", "Debt"))
        assertFalse(IndianTaxRules.equityOriented("FIXED_INCOME", ""))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Overnight Fund"))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Money Market Fund"))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Banking and PSU Fund"))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Credit Risk Fund"))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Floater Fund"))
        assertFalse(IndianTaxRules.equityOriented("MUTUAL_FUND", "Equity Income Fund"))
        assertTrue(IndianTaxRules.equityOriented("MUTUAL_FUND", "Arbitrage Fund"))
        assertTrue(IndianTaxRules.equityOriented("MUTUAL_FUND", "ELSS Tax Saver"))

        assertEquals(GainClass.REAL_ESTATE_LTCG,
            IndianTaxRules.classify("REAL_ESTATE", "Flat", false))
        assertEquals(GainClass.LTCG_EQUITY,
            IndianTaxRules.classify("EQUITY", "Shares", true))
        assertEquals(GainClass.STCG_EQUITY,
            IndianTaxRules.classify("EQUITY", "Shares", false))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("FIXED_INCOME", "PPF", true))
        assertEquals(GainClass.SGB_EXEMPT,
            IndianTaxRules.classify("GOLD_SGB", "", true, "CERTIFICATE"))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("GOLD_SGB", "", true, null))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("GOLD_SGB", "Sovereign Gold Bond", true, "certificate"))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("GOLD_SGB", "Digital Gold", true, "CERTIFICATE"))
        assertEquals(GainClass.SGB_EXEMPT,
            IndianTaxRules.classify("GOLD_SGB", "SGB 2020", true, "CERTIFICATE"))
        assertEquals(GainClass.DEBT_SLAB,
            IndianTaxRules.classify("GOLD_SGB", "holdingsgb", true, "CERTIFICATE"))
    }

    @Test fun legacyOpeningBalanceUsesStoredAverageNotMarketPrice() {
        // kosha.test.ts sections C and R. Quantity and cost come from the stored
        // average. Market value uses the current price. The loan outstanding is
        // the market value, not the original amount.
        val equity = FinancialHolding(
            HoldingId("h9"), owner, "Hh9", "EQUITY",
            rupees(1_500), 75 * QUANTITY_SCALE, openingCostPerUnit = rupees(1_200),
        )
        val loan = FinancialHolding(
            HoldingId("h_loan"), owner, "HDFC Car Loan", "LIABILITY",
            rupees(612_000), QUANTITY_SCALE, openingCostPerUnit = rupees(850_000),
        )
        val book = PortfolioEngine.derive(listOf(equity, loan), emptyList(), date("2026-08-01"))
        val eq = book.members.single().holdings.first { it.holding.holdingId.raw == "h9" }
        assertEquals(75 * QUANTITY_SCALE, eq.position.quantityScaled)
        assertTrue(eq.position.fromOpeningBalance)
        assertFalse(eq.position.unknownInvestedCost)
        assertEquals(90_000L, eq.position.investedCost.rupees().longValueExact())
        assertEquals(1_200L, eq.position.averageUnitCost.rupees().longValueExact())
        assertEquals(112_500L, eq.marketValue.rupees().longValueExact())
        assertEquals(22_500L, eq.unrealisedGain!!.rupees().longValueExact())
        assertEquals("ob_h9", eq.position.openLots.single().eventId)

        val debt = book.members.single().holdings.first { it.holding.category == "LIABILITY" }
        assertTrue(debt.position.fromOpeningBalance)
        assertEquals(850_000L, debt.position.investedCost.rupees().longValueExact())
        assertEquals(612_000L, debt.marketValue.rupees().longValueExact())
        assertEquals(112_500L, book.familyAssets.rupees().longValueExact())
        assertEquals(612_000L, book.familyLiabilities.rupees().longValueExact())

        val truncated = FinancialHolding(
            HoldingId("tiny"), owner, "Tiny", "EQUITY",
            MoneyScaled(1), 3L, openingCostPerUnit = MoneyScaled(1),
        )
        val tiny = PortfolioEngine.derive(listOf(truncated), emptyList(), date("2026-08-01"))
            .members.single().holdings.single().position
        assertEquals(0L, tiny.investedCost.raw)
        assertEquals(0L, tiny.averageUnitCost.raw)
    }

    @Test fun memberNetWorthAndOrphanEventsAreExplicit() {
        val spouse = OwnerId("spouse")
        val hs = listOf(holding(price = 1000, storedQuantity = 0),
            holding(category = "LIABILITY", price = 300, storedQuantity = 0,
                ownerId = spouse, id = HoldingId("debt")))
        val ledger = listOf(
            ev("asset", FinancialEventKind.BUY, "2026-01-01", 10, 1000),
            ev("loan", FinancialEventKind.OPENING_BALANCE, "2026-01-01", 10, 300,
                HoldingId("debt"), spouse),
            ev("orphan", FinancialEventKind.BUY, "2026-01-01", 10, 100, HoldingId("gone")),
        )
        val book = PortfolioEngine.derive(hs, ledger, date("2026-06-01"))
        assertEquals(10_000, book.familyAssets.rupees().longValueExact())
        assertEquals(3_000, book.familyLiabilities.rupees().longValueExact())
        assertEquals(7_000, book.familyNetWorth.rupees().longValueExact())
        assertTrue(book.errors.any { it.contains("no longer listed") })
    }

    @Test fun absentBuyHistoryIsShownUnknownNotFabricated() {
        val book = PortfolioEngine.derive(listOf(holding(price = 1500, storedQuantity = 75)),
            emptyList(), date("2026-08-01"))
        val item = book.members.single().holdings.single()
        assertEquals(75 * QUANTITY_SCALE, item.position.quantityScaled)
        assertTrue(item.position.fromOpeningBalance)
        assertTrue(item.position.unknownInvestedCost)
        assertNull(item.unrealisedGain)
    }

    @Test fun statementLotsCoalesceAndOnlyTheDeltaIsProposed() {
        val holding = holding(category = "MUTUAL_FUND", storedQuantity = 0)
        val sha = "a".repeat(64)
        fun line(q: Long, day: String = "2026-08-31") =
            StatementLine(holdingId, owner, date(day), Math.multiplyExact(q, QUANTITY_SCALE), rupees(100))
        val first = SnapshotReconciler.review(
            listOf(line(38), line(11703)), listOf(holding), emptyList(), owner, sha)
        assertEquals(1, first.lines.size)
        assertEquals(ReconciliationOutcome.NEW_POSITION, first.lines.single().outcome)
        val opening = first.acceptedEvents(setOf(holdingId))
        assertEquals(1, opening.size)
        assertEquals(FinancialEventKind.OPENING_BALANCE, opening.single().kind)
        val reimport = SnapshotReconciler.review(
            listOf(line(38), line(11703)), listOf(holding), opening, owner, sha)
        assertEquals(ReconciliationOutcome.MATCH, reimport.lines.single().outcome)
        assertTrue(reimport.acceptedEvents(setOf(holdingId)).isEmpty())
        val next = SnapshotReconciler.review(
            listOf(line(12241, "2026-09-30")), listOf(holding), opening, owner, "b".repeat(64))
        assertEquals(ReconciliationOutcome.ADJUST_UP, next.lines.single().outcome)
        assertEquals(500 * QUANTITY_SCALE, next.lines.single().deltaQtyScaled)
        assertEquals(FinancialEventKind.SNAPSHOT_ADJUSTMENT,
            next.acceptedEvents(setOf(holdingId)).single().kind)
    }

    @Test fun snapshotCloseAdjustAndToleranceMatchTauri() {
        val sha = "d".repeat(64)
        val bought = ev("buy", FinancialEventKind.BUY, "2026-01-31", 100, 100)
        val down = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-02-28"),
                40 * QUANTITY_SCALE, rupees(110))),
            listOf(holding(storedQuantity = 0)), listOf(bought), owner, sha,
        )
        val reduced = down.lines.single()
        assertEquals(ReconciliationOutcome.ADJUST_DOWN, reduced.outcome)
        assertEquals(-60 * QUANTITY_SCALE, reduced.deltaQtyScaled)
        assertEquals(0L, reduced.proposedEvents.single().unitPrice?.raw)
        assertEquals(-6_600L, down.estimatedValueChange.rupees().longValueExact())

        val closed = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-03-31"), 0, rupees(110))),
            listOf(holding(storedQuantity = 0)), listOf(bought), owner, "e".repeat(64),
        )
        assertEquals(ReconciliationOutcome.CLOSED, closed.lines.single().outcome)
        assertEquals(-100 * QUANTITY_SCALE, closed.lines.single().proposedEvents.single().quantityScaled)

        val within = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-02-28"),
                100 * QUANTITY_SCALE + 50, rupees(110))),
            listOf(holding(storedQuantity = 0)), listOf(bought), owner, sha,
        )
        assertEquals(ReconciliationOutcome.MATCH, within.lines.single().outcome)
        val outside = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-02-28"),
                100 * QUANTITY_SCALE + 101, rupees(110))),
            listOf(holding(storedQuantity = 0)), listOf(bought), owner, sha,
        )
        assertEquals(ReconciliationOutcome.ADJUST_UP, outside.lines.single().outcome)
        assertEquals(101L, outside.lines.single().deltaQtyScaled)

        val other = HoldingId("other")
        val otherBuy = ev("other-buy", FinancialEventKind.BUY, "2026-01-31", 5, 20,
            holding = other)
        val current = StatementLine(holdingId, owner, date("2026-04-30"),
            100 * QUANTITY_SCALE, rupees(110))
        val vanished = SnapshotReconciler.linesToClose(
            listOf(holdingId, other, HoldingId("missing")),
            listOf(current),
            listOf(holding(storedQuantity = 0), holding(id = other, price = 20, storedQuantity = 0)),
            date("2026-04-30"),
            listOf(bought, otherBuy),
        )
        assertEquals(1, vanished.size)
        assertEquals(other, vanished.single().holdingId)
        assertEquals(0L, vanished.single().quantityScaled)
        val closing = SnapshotReconciler.review(
            vanished, listOf(holding(id = other, price = 20, storedQuantity = 0)),
            listOf(otherBuy), owner, "9".repeat(64),
        )
        assertEquals(ReconciliationOutcome.CLOSED, closing.lines.single().outcome)
        val after = LotEngine.fold(other, "EQUITY", listOf(otherBuy) + closing.acceptedEvents(setOf(other)))
        assertEquals(0L, after.quantityScaled)

        val alreadyZero = SnapshotReconciler.linesToClose(
            listOf(other), emptyList(),
            listOf(holding(id = other, price = 20, storedQuantity = 0)),
            date("2026-04-30"), listOf(otherBuy) + closing.acceptedEvents(setOf(other)),
        )
        assertTrue(alreadyZero.isEmpty())
    }

    @Test fun fractionalStatementLotsAndPrintedCostMatchTauri() {
        val sha = "a".repeat(64)
        val firstQty = java.lang.Math.round(38.415 * 1e8)
        val secondQty = java.lang.Math.round(11_703.407 * 1e8)
        val lot = { qty: Long ->
            StatementLine(holdingId, owner, date("2026-03-31"), qty, MoneyScaled(2_210_400))
        }
        val merged = SnapshotReconciler.coalesce(listOf(lot(firstQty), lot(secondQty)))
        assertEquals(1, merged.size)
        assertEquals(1_174_182_200_000L, merged.single().quantityScaled)
        assertEquals(2_210_400L, merged.single().unitPrice.raw)

        val nps = HoldingId("h_nps_e")
        val qty = 641_870_520_000L
        val statementNav = MoneyScaled(750_840)
        val book = holding(id = nps, category = "FIXED_INCOME", price = 75, storedQuantity = 0)
            .copy(unitPrice = statementNav, name = "NPS scheme E")
        val first = SnapshotReconciler.review(
            listOf(StatementLine(nps, owner, date("2026-08-31"), qty, statementNav)),
            listOf(book), emptyList(), owner, sha,
        )
        assertEquals(ReconciliationOutcome.NEW_POSITION, first.lines.single().outcome)
        val opening = first.acceptedEvents(setOf(nps)).single()
        // Tauri copies the printed NAV into cost. This books do not: a holdings
        // statement without a contribution has unknown invested cost.
        assertEquals(0L, opening.unitPrice?.raw)
        val contribution = MoneyScaled(3_179_094_500L)
        val rebase = SnapshotReconciler.review(
            listOf(StatementLine(nps, owner, date("2026-08-31"), qty, statementNav, contribution)),
            listOf(book), listOf(opening), owner, "b".repeat(64),
        )
        assertEquals(ReconciliationOutcome.COST_UPDATE, rebase.lines.single().outcome)
        assertTrue(rebase.lines.single().outcome != ReconciliationOutcome.ADJUST_UP)
        val corrected = rebase.acceptedEvents(setOf(nps))
        assertEquals(1, corrected.size)
        assertEquals(opening.id, corrected.single().supersedesEventId)
        assertEquals(opening.date, corrected.single().date)
        val folded = LotEngine.fold(nps, "FIXED_INCOME", listOf(opening) + corrected)
        assertEquals(qty, folded.quantityScaled)
        assertEquals(3_179_088_404L, folded.investedCost.raw)
        val again = SnapshotReconciler.review(
            listOf(StatementLine(nps, owner, date("2026-08-31"), qty, statementNav, contribution)),
            listOf(book), listOf(opening) + corrected, owner, "c".repeat(64),
        )
        assertEquals(ReconciliationOutcome.MATCH, again.lines.single().outcome)
        assertTrue(again.acceptedEvents(setOf(nps)).isEmpty())

        val twoLots = listOf(
            ev("a", FinancialEventKind.BUY, "2026-01-01", 40, 100),
            ev("b", FinancialEventKind.BUY, "2026-02-01", 60, 200),
        )
        val many = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-08-31"),
                100 * QUANTITY_SCALE, rupees(150), rupees(10_000))),
            listOf(holding(storedQuantity = 0)), twoLots, owner, "7".repeat(64),
        )
        assertEquals(ReconciliationOutcome.CONFLICT, many.lines.single().outcome)
        assertThrows(IllegalArgumentException::class.java) {
            many.acceptedEvents(setOf(holdingId))
        }
    }

    @Test fun missingCustodianLinesAreNeverAutoClosed() {
        val hs = listOf(holding(), holding(id = HoldingId("other")))
        val preview = SnapshotReconciler.review(
            listOf(StatementLine(holdingId, owner, date("2026-10-08"),
                100 * QUANTITY_SCALE, rupees(100))),
            hs, emptyList(), owner, "c".repeat(64))
        assertEquals(1, preview.lines.size)
        assertEquals(1, preview.warnings.size)
        assertTrue(preview.warnings.first().contains("left untouched"))
    }

    @Test fun crossOwnerStatementIsRejectedBeforePlanning() {
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotReconciler.review(
                listOf(StatementLine(holdingId, OwnerId("spouse"),
                    date("2026-10-08"), 100 * QUANTITY_SCALE, rupees(100))),
                listOf(holding()), emptyList(), owner, "e".repeat(64))
        }
    }

    @Test fun truncatedFractionalValuesMatchLegacyScaledIntegerSemantics() {
        // 0.5 units at a tiny scaled price truncates to 0 (not half-up to 1).
        val tiny = FinancialHolding(holdingId, owner, "Tiny quantity", "EQUITY",
            MoneyScaled(1), 50_000_000L)
        val book = PortfolioEngine.derive(listOf(tiny), emptyList(), date("2026-10-08"))
        assertEquals(0L, book.familyNetWorth.raw)
    }

    @Test fun noHoldingsMemberRemainsVisibleWithZeroTotals() {
        val person = OwnerId("empty")
        val book = PortfolioEngine.derive(
            listOf(holding(storedQuantity = 0)), emptyList(), date("2026-10-08"),
            listOf(owner, person),
        )
        assertEquals(2, book.members.size)
        val empty = book.members.first { it.ownerId == person }
        assertEquals(0L, empty.netWorth.raw)
        assertTrue(empty.holdings.isEmpty())
    }

    @Test fun eventWithDifferentOwnerIsNotCountedUnderFamilyMember() {
        val mismatched = ev("wrong-owner", FinancialEventKind.BUY,
            "2026-01-01", 100, 100, ownerId = OwnerId("spouse"))
        val report = PortfolioEngine.derive(
            listOf(holding(storedQuantity = 0)), listOf(mismatched), date("2026-10-08"),
            listOf(owner),
        )
        assertEquals(0L, report.familyNetWorth.raw)
        assertTrue(report.errors.any { it.contains("Ownership conflict") })
    }

    @Test fun costRebaseIsCorrectionNotSecondBuy() {
        val opening = ev("original", FinancialEventKind.OPENING_BALANCE,
            "2026-01-01", 100, 75)
        val line = StatementLine(holdingId, owner, date("2026-08-31"),
            100 * QUANTITY_SCALE, rupees(75), rupees(5000))
        val plan = SnapshotReconciler.review(
            listOf(line), listOf(holding(storedQuantity = 0)),
            listOf(opening), owner, "f".repeat(64))
        assertEquals(ReconciliationOutcome.COST_UPDATE, plan.lines.single().outcome)
        val approved = plan.acceptedEvents(setOf(holdingId))
        assertEquals(FinancialEventKind.CORRECTION, approved.single().kind)
        assertEquals("original", approved.single().supersedesEventId)
        assertEquals(opening.date, approved.single().date)
        val folded = LotEngine.fold(holdingId, "EQUITY", listOf(opening) + approved)
        assertEquals(100 * QUANTITY_SCALE, folded.quantityScaled)
        assertEquals(5_000, folded.investedCost.rupees().longValueExact())
    }

    @Test fun conflictingHoldingCannotBeCommittedFromSnapshotReview() {
        val quoted = StatementLine(holdingId, owner, date("2026-10-08"),
            100 * QUANTITY_SCALE, rupees(100))
        val review = SnapshotReconciler.review(
            listOf(quoted),
            listOf(holding(ownerId = OwnerId("spouse"))),
            emptyList(), owner, "1".repeat(64))
        assertEquals(ReconciliationOutcome.CONFLICT, review.lines.single().outcome)
        assertThrows(IllegalArgumentException::class.java) {
            review.acceptedEvents(setOf(holdingId))
        }
    }

    @Test fun dateAsOfIgnoresTransactionsThatHappenedLater() {
        val existing = listOf(
            ev("opening", FinancialEventKind.BUY, "2025-01-01", 10, 100),
            ev("future", FinancialEventKind.BUY, "2026-10-20", 15, 200),
        )
        val quote = StatementLine(holdingId, owner, date("2026-10-08"),
            10 * QUANTITY_SCALE, rupees(100))
        val review = SnapshotReconciler.review(
            listOf(quote), listOf(holding(storedQuantity = 0)),
            existing, owner, "2".repeat(64))
        assertEquals(ReconciliationOutcome.MATCH, review.lines.single().outcome)
    }

    @Test fun xirrAndCashFlowsMatchTauriReturns() {
        assertNull(ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2024-01-01"), rupees(100)),
            DatedCashFlow(date("2025-01-01"), rupees(100)),
        )))
        assertNull(ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2024-01-01"), rupees(-100)),
            DatedCashFlow(date("2025-01-01"), rupees(-50)),
        )))
        assertNull(ReturnEngine.xirr(listOf(DatedCashFlow(date("2024-01-01"), rupees(-100)))))
        assertEquals(10.01, ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2025-01-01"), rupees(-1000)),
            DatedCashFlow(date("2026-01-01"), rupees(1100)),
        )))
        assertEquals(9.98, ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2024-01-01"), rupees(-1000)),
            DatedCashFlow(date("2025-01-01"), rupees(1100)),
        )))
        assertEquals(15.4, ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2024-01-01"), rupees(-1000)),
            DatedCashFlow(date("2024-06-01"), rupees(50)),
            DatedCashFlow(date("2025-01-01"), rupees(1100)),
        )))
        assertEquals(-75.12, ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2024-01-01"), rupees(-1000)),
            DatedCashFlow(date("2024-07-01"), rupees(500)),
        )))
        // Unsorted input uses the same root as the dated order.
        assertEquals(10.01, ReturnEngine.xirr(listOf(
            DatedCashFlow(date("2026-01-01"), rupees(1100)),
            DatedCashFlow(date("2025-01-01"), rupees(-1000)),
        )))
        assertEquals(25.0, ReturnEngine.totalGainPercent(rupees(1000), rupees(250)))
        assertEquals(0.0, ReturnEngine.totalGainPercent(MoneyScaled(0), rupees(10)))
        assertEquals(0.0, ReturnEngine.totalGainPercent(rupees(-5), rupees(10)))
        // 1/800 rounds half away from zero to 0.13%, matching JS Math.round.
        assertEquals(0.13, ReturnEngine.totalGainPercent(rupees(800), rupees(1)))
        assertEquals(17.15, ReturnEngine.totalGainPercent(rupees(277_000), rupees(47_500)))

        val bought = FinancialEvent(
            "buy", holdingId, owner, FinancialEventKind.BUY, date("2025-01-01"),
            10 * QUANTITY_SCALE, rupees(100), rupees(1_000), rupees(10),
        )
        val sold = FinancialEvent(
            "sell", holdingId, owner, FinancialEventKind.SELL, date("2025-06-01"),
            4 * QUANTITY_SCALE, rupees(100), rupees(400), rupees(5),
        )
        val dividend = FinancialEvent(
            "div", holdingId, owner, FinancialEventKind.DIVIDEND, date("2025-06-01"),
            totalAmount = rupees(25), charges = rupees(5),
        )
        val bonus = FinancialEvent(
            "bonus", holdingId, owner, FinancialEventKind.BONUS, date("2025-03-01"),
            2 * QUANTITY_SCALE, totalAmount = rupees(999),
        )
        val opening = ev("ob", FinancialEventKind.OPENING_BALANCE, "2024-12-01", 10, 100)
        val flows = ReturnEngine.flows(
            listOf(sold, bonus, opening, dividend, bought), rupees(700), date("2026-01-01"),
        )
        assertEquals(listOf(
            date("2025-01-01") to -1_010L,
            date("2025-06-01") to 395L,
            date("2025-06-01") to 25L,
            date("2026-01-01") to 700L,
        ), flows.map { it.date to it.amount.rupees().longValueExact() })
    }
}
