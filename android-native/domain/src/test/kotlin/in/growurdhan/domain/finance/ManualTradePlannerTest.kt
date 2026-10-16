package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.*
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ManualTradePlannerTest {
    private val owner = OwnerId("owner")
    private val holding = Holding(
        HoldingId("fund"), owner, "registrar", "Synthetic index fund",
        "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(15_000),
    )
    private val member = FamilyMember(owner, "Primary", "Self", true)
    private val today = LocalDate.parse("2026-10-10")
    private val purchase = LedgerEvent(
        "purchase", holding.id, owner, LedgerKind.BUY, "2026-09-01",
        200_000_000L, MoneyScaled(300_000), null, MoneyScaled(15_000),
    )
    private fun book(events: List<LedgerEvent> = emptyList()) =
        VaultBook(listOf(member), listOf(holding), events)
    private fun draft(
        kind: ManualTradeKind = ManualTradeKind.BUY,
        date: String = "2026-10-10",
        qty: String = "1.5",
        price: String = "12.3456",
        fees: String = "0",
    ) = ManualTradeDraft(holding.id, kind, date, qty, price, fees)

    @Test fun explicitBuyComputesExactScaledPrincipalAndCharges() {
        val result = ManualTradePlanner.prepare(
            book(), draft(fees = "1.0000"), "synthetic-event", today,
        )
        assertEquals(FinancialEventKind.BUY, result.event.kind)
        assertEquals(150_000_000L, result.event.quantityScaled)
        assertEquals(185_184L, result.gross.raw)
        assertEquals(195_184L, result.cashAmount.raw)
        assertEquals(owner, result.event.ownerId)
        assertEquals("synthetic-event", result.event.id)
    }

    @Test fun validSellOnlyConsumesRecordedAcquisitionLots() {
        val result = ManualTradePlanner.prepare(
            book(listOf(purchase)),
            draft(ManualTradeKind.SELL, qty = "1", price = "17", fees = "0.25"),
            "sale", today,
        )
        assertEquals(FinancialEventKind.SELL, result.event.kind)
        assertEquals(-100_000_000L, result.event.quantityScaled)
        assertEquals(170_000L, result.gross.raw)
        assertEquals(167_500L, result.cashAmount.raw)
    }

    @Test fun casQuantityWithoutPurchasesCannotBeSoldWithUnknownCost() {
        val recordedOnly = holding.copy(quantity = QuantityScaled(200_000_000L))
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book().copy(holdings = listOf(recordedOnly)),
                draft(ManualTradeKind.SELL), "sale", today)
        }
    }

    @Test fun preventsOversellingAndBackdatedDisposal() {
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase)),
                draft(ManualTradeKind.SELL, qty = "3"), "oversell", today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase)),
                draft(ManualTradeKind.SELL, date = "2026-08-01", qty = "1"),
                "backdated", today)
        }
    }

    @Test fun rejectsCrossOwnerUnknownAndLiabilityHoldings() {
        val stranger = holding.copy(ownerId = OwnerId("stranger"))
        val liability = holding.copy(assetCategory = "LIABILITY")
        for (bad in listOf(stranger, liability)) {
            assertThrows(IllegalArgumentException::class.java) {
                ManualTradePlanner.prepare(book().copy(holdings = listOf(bad)),
                    draft(), "event", today)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(), draft().copy(holdingId = HoldingId("other")),
                "event", today)
        }
    }

    @Test fun rejectsFutureImpossibleAndInvalidDateAndUnrepresentablePrecision() {
        for (date in listOf("2026-10-11", "2026-02-30", "1899-12-31", "today")) {
            assertThrows(Exception::class.java) {
                ManualTradePlanner.prepare(book(), draft(date = date), "event", today)
            }
        }
        for (quantity in listOf("1.000000001", "-1", "NaN", "0", "1e6", "")) {
            assertThrows(Exception::class.java) {
                ManualTradePlanner.prepare(book(), draft(qty = quantity), "event", today)
            }
        }
        for (price in listOf("1.00001", "-1", "0", "1e15", "9".repeat(25))) {
            assertThrows(Exception::class.java) {
                ManualTradePlanner.prepare(book(), draft(price = price), "event", today)
            }
        }
    }

    @Test fun feesCannotExceedProceedsOrOverflowBookMoney() {
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase)), draft(
                ManualTradeKind.SELL, qty = "1", price = "1", fees = "1",
            ), "event", today)
        }
        assertThrows(Exception::class.java) {
            ManualTradePlanner.prepare(book(), draft(qty = "999999999999",
                price = "999999999999"), "event", today)
        }
    }

    @Test fun positiveCasAdjustmentDoesNotCreateSaleableAcquisitionLots() {
        val syntheticSnapshot = LedgerEvent(
            "cas-observation", holding.id, owner, LedgerKind.SNAPSHOT_ADJUSTMENT,
            "2026-09-15", 500_000_000L, MoneyScaled(750_000), null,
            MoneyScaled(15_000),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase, syntheticSnapshot)),
                draft(ManualTradeKind.SELL, qty = "1"), "sale", today)
        }
    }

    @Test fun oldUnpricedAcquisitionCannotBeSoldAsProvenCost() {
        val legacy = purchase.copy(id = "legacy", unitPrice = null)
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(legacy)),
                draft(ManualTradeKind.SELL, qty = "1"), "sale", today)
        }
    }

    @Test fun changedBookFingerprintRefusesStalePreviewEvenForSameHolding() {
        val original = ManualTradePlanner.prepare(book(), draft(), "event", today)
        val newer = ManualTradePlanner.prepare(book(listOf(purchase)), draft(), "event", today)
        assertNotEquals(original.bookFingerprint, newer.bookFingerprint)
        assertNotEquals(original, newer)
        val renamed = ManualTradePlanner.prepare(book().copy(
            holdings = listOf(holding.copy(name = "Renamed fund")),
        ), draft(), "event", today)
        assertNotEquals(original.bookFingerprint, renamed.bookFingerprint)
    }


    @Test fun incomeAndValuationDoNotChangeQuantity() {
        for (kind in listOf(ManualTradeKind.DIVIDEND,
            ManualTradeKind.INTEREST, ManualTradeKind.VALUATION_UPDATE)) {
            val plan = ManualTradePlanner.prepare(book(listOf(purchase)),
                draft(kind = kind, qty = "", price = "123.4567"),
                "income-" + kind.name, today)
            assertEquals(0L, plan.event.quantityScaled)
            assertEquals(1_234_567L, plan.gross.raw)
            assertEquals(kind.name, plan.event.kind.name)
        }
    }

    @Test fun bonusAndSplitAreFoldedWithRetainedOriginalAcquisitionLots() {
        val bonus = ManualTradePlanner.prepare(book(listOf(purchase)),
            draft(kind = ManualTradeKind.BONUS, qty = "1", price = "0"),
            "bonus", today)
        assertEquals(0L, bonus.event.totalAmount.raw)
        assertEquals(100_000_000L, bonus.event.quantityScaled)
        val split = ManualTradePlanner.prepare(book(listOf(purchase)),
            draft(kind = ManualTradeKind.SPLIT, qty = "2", price = "1"),
            "split", today)
        val lot = LotEngine.fold(holding.id, holding.assetCategory,
            listOf(purchase.toFinancialEvent(), split.event))
        assertTrue(lot.errors.isEmpty())
        assertEquals(400_000_000L, lot.quantityScaled)
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(),
                draft(kind = ManualTradeKind.SPLIT, qty = "2", price = "1"),
                "split-empty", today)
        }
    }

    @Test fun correctedBuyDoesNotMutateOldEventAndRejectsNegativeHistoricalPosition() {
        val correction = ManualTradePlanner.prepare(book(listOf(purchase)),
            draft(kind = ManualTradeKind.CORRECTION, qty = "3", price = "15")
                .copy(supersedesEventId = purchase.id), "corrected", today)
        assertEquals(FinancialEventKind.CORRECTION, correction.event.kind)
        assertEquals(purchase.id, correction.event.supersedesEventId)
        val position = LotEngine.fold(holding.id, holding.assetCategory,
            listOf(purchase.toFinancialEvent(), correction.event))
        assertEquals(300_000_000L, position.quantityScaled)
        assertEquals(purchase.quantityDeltaScaled, 200_000_000L)
        val sale = LedgerEvent("sold", holding.id, owner, LedgerKind.SELL,
            "2026-09-10", -100_000_000L, MoneyScaled(170_000),
            null, MoneyScaled(170_000))
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase, sale)),
                draft(kind = ManualTradeKind.CORRECTION, qty = "0.5", price = "15")
                    .copy(supersedesEventId = purchase.id), "invalid", today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(purchase,
                LedgerEvent("corrected", holding.id, owner, LedgerKind.CORRECTION,
                    "2026-10-10", 300_000_000L, MoneyScaled(450_000),
                    null, MoneyScaled(150_000), supersedesEventId = "purchase"))),
                draft(kind = ManualTradeKind.CORRECTION, qty = "3", price = "15")
                    .copy(supersedesEventId = purchase.id), "again", today)
        }
    }

    @Test fun openingBalancesWithExplicitZeroCostAreNotInferredPurchases() {
        val opening = ManualTradePlanner.prepare(book(),
            draft(kind = ManualTradeKind.OPENING_BALANCE, qty = "1", price = "0"),
            "opening", today)
        assertEquals(FinancialEventKind.OPENING_BALANCE, opening.event.kind)
        assertEquals(0L, opening.event.unitPrice?.raw)
        assertEquals(100_000_000L, opening.event.quantityScaled)
    }

    @Test fun repaymentRequiresLiabilityAndNoManualSnapshotKindExists() {
        assertFalse(ManualTradeKind.entries.any { it.name == "SNAPSHOT_ADJUSTMENT" })
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(),
                draft(kind = ManualTradeKind.REPAYMENT, qty = "", price = "200"),
                "repayment", today)
        }
        val liability = holding.copy(assetCategory = "LIABILITY")
        val result = ManualTradePlanner.prepare(book().copy(holdings = listOf(liability)),
            draft(kind = ManualTradeKind.REPAYMENT, qty = "", price = "200"),
            "repayment-liability", today)
        assertEquals(0L, result.event.quantityScaled)
        assertEquals(2_000_000L, result.event.totalAmount.raw)
    }


    @Test fun smallFractionalTradesUseIntegerHalfUpRounding() {
        val result = ManualTradePlanner.prepare(
            book(), draft(qty = "0.00000001", price = "5000"), "event", today,
        )
        assertEquals(1L, result.gross.raw)
    }

    @Test fun existingLiabilityOpensOnceWithPreciselyOneDebtUnitAndDocumentedPrice() {
        val loan = holding.copy(assetCategory = "LIABILITY", quantity = QuantityScaled(0))
        val baseline = book().copy(holdings = listOf(loan))
        val opening = draft(ManualTradeKind.OPENING_BALANCE, qty = "1",
            price = "125000.2500")
        val approved = ManualTradePlanner.prepare(baseline, opening, "loan-open", today)
        assertEquals(FinancialEventKind.OPENING_BALANCE, approved.event.kind)
        assertEquals(QUANTITY_SCALE, approved.event.quantityScaled)
        assertEquals(1_250_002_500L, approved.event.unitPrice?.raw)
        assertEquals(1_250_002_500L, approved.gross.raw)
        for (bad in listOf("0.5", "2", "0")) {
            assertThrows(IllegalArgumentException::class.java) {
                ManualTradePlanner.prepare(baseline,
                    opening.copy(quantity = bad), "rejected-" + bad, today)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(baseline,
                opening.copy(chargesInr = "1"), "rejected-fees", today)
        }
        val already = LedgerEvent("existing-open", loan.id, owner,
            LedgerKind.OPENING_BALANCE, "2026-09-01", QUANTITY_SCALE,
            MoneyScaled(1_250_002_500L), null, MoneyScaled(1_250_002_500L))
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(baseline.copy(events = listOf(already)),
                opening, "duplicate-loan", today)
        }
    }

    @Test fun liabilityOpeningCanBeCorrectedOnceWithoutDoubleCounting() {
        val loan = holding.copy(assetCategory = "LIABILITY")
        val original = LedgerEvent("loan-opening", loan.id, owner,
            LedgerKind.OPENING_BALANCE, "2026-09-01", QUANTITY_SCALE,
            MoneyScaled(1_500_000_000L), null, MoneyScaled(1_500_000_000L))
        val correction = ManualTradePlanner.prepare(
            book(listOf(original)).copy(holdings = listOf(loan)),
            draft(ManualTradeKind.CORRECTION, qty = "1", price = "120000")
                .copy(supersedesEventId = original.id), "loan-correction", today)
        assertEquals(FinancialEventKind.CORRECTION, correction.event.kind)
        assertEquals("loan-opening", correction.event.supersedesEventId)
        val result = LotEngine.fold(loan.id, loan.assetCategory,
            listOf(original.toFinancialEvent(), correction.event))
        assertTrue(result.errors.isEmpty())
        assertEquals(QUANTITY_SCALE, result.quantityScaled)
        assertThrows(IllegalArgumentException::class.java) {
            ManualTradePlanner.prepare(book(listOf(original)).copy(holdings = listOf(loan)),
                draft(ManualTradeKind.CORRECTION, qty = "2", price = "120000")
                    .copy(supersedesEventId = original.id), "bad-loan-correction", today)
        }
    }
}
