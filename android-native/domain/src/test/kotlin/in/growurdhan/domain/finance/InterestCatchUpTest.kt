package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section W and src/lib/interest.ts. */
class InterestCatchUpTest {
    private val owner = OwnerId("f1")
    private fun date(v: String) = LocalDate.parse(v)
    private fun rupees(v: Long) = MoneyScaled(v * 10_000)
    private fun book(
        id: String, category: String, sub: String, name: String,
        balance: Long, cost: Long = balance, qty: Long = 1, acquired: String? = null,
    ) = FinancialHolding(
        HoldingId(id), owner, name, category, rupees(balance), qty * QUANTITY_SCALE,
        openingCostPerUnit = rupees(cost), subCategory = sub, acquiredOn = acquired?.let(::date),
    )

    @Test fun calendarAndSimpleInterestMatchTauri() {
        assertEquals(emptyList<LocalDate>(), SavingsInterestEngine.fyEndDatesAfter(date("2026-08-31"), date("2026-09-15")))
        assertEquals(listOf(date("2027-03-31")), SavingsInterestEngine.fyEndDatesAfter(date("2026-08-31"), date("2027-03-31")))
        assertEquals(
            listOf(date("2019-03-31"), date("2020-03-31")),
            SavingsInterestEngine.fyEndDatesAfter(date("2018-09-08"), date("2020-03-31")),
        )
        assertEquals(12, SavingsInterestEngine.monthsHeldInFy(date("2011-04-01"), date("2026-03-31")))
        assertEquals(6, SavingsInterestEngine.monthsHeldInFy(date("2018-09-08"), date("2019-03-31")))
        assertEquals(rupees(2_500), SavingsInterestEngine.annualAccrual(rupees(100_000), 250, 12))
    }

    @Test fun sgbCouponsDoNotChangeTheGoldValue() {
        val holding = book(
            "h_sgb2", "GOLD_SGB", "Sovereign Gold Bond", "SGB 2018-19 Series VI",
            balance = 7_200, cost = 7_200, qty = 10, acquired = "2018-09-08",
        )
        assertEquals(SavingsKind.SGB, SavingsInterestEngine.interestKindOf(holding, 2.5))
        val terms = SchemeTerms(
            holding, rupees(7_200), couponRatePercent = 2.5, issuePrice = rupees(3_119),
        )
        val plan = requireNotNull(SavingsInterestEngine.planInterest(terms, date("2020-03-31")))
        assertEquals(
            listOf(date("2019-03-08"), date("2019-09-08"), date("2020-03-08")),
            plan.events.map { it.date },
        )
        assertEquals(3_898_750L, plan.events.first().amount.raw)
        assertNull(plan.grownBalance)
        assertTrue(plan.events.first().notes.startsWith("SGB half-year coupon"))
        assertNull(SavingsInterestEngine.planInterest(
            terms, date("2020-03-31"), plan.events.map { it.date }.toSet(),
        ))
    }

    @Test fun compoundProductsBackfillOnlyWhileTheBalanceIsStillTheDeposit() {
        val grown = book(
            "h_ppf2", "FIXED_INCOME", "PPF", "Public Provident Fund",
            balance = 1_940_000, cost = 1_450_000, acquired = "2011-04-01",
        )
        val valued = SchemeTerms(grown, rupees(1_940_000), ratePercent = 7.1, valuedOn = date("2026-08-31"), priceSource = "CAS")
        assertEquals(SavingsKind.PPF, SavingsInterestEngine.interestKindOf(grown, 7.1))
        assertNull(SavingsInterestEngine.planInterest(valued, date("2026-09-15")))
        val forward = requireNotNull(SavingsInterestEngine.planInterest(valued, date("2027-03-31")))
        assertEquals(date("2027-03-31"), forward.events.single().date)
        assertEquals(1_377_400_000L, forward.events.single().amount.raw)
        assertEquals(20_777_400_000L, forward.grownBalance?.raw)
        assertEquals("PPF interest for FY 2026-27 (7.1%)", forward.events.single().notes)

        val deposit = book(
            "h_ppf_open", "FIXED_INCOME", "PPF", "PPF from deposit",
            balance = 100_000, acquired = "2022-04-01",
        )
        val open = SchemeTerms(deposit, rupees(100_000), ratePercent = 7.1, priceSource = "Manual Initial Entry")
        val rebuilt = requireNotNull(SavingsInterestEngine.planInterest(open, date("2026-09-26")))
        assertEquals(
            listOf(71_000_000L, 76_041_000L, 81_439_911L, 87_222_144L),
            rebuilt.events.map { it.amount.raw },
        )
        assertEquals(date("2023-03-31"), rebuilt.events.first().date)
        assertEquals(1_315_703_055L, rebuilt.grownBalance?.raw)
        assertNull(SavingsInterestEngine.planInterest(
            open.copy(priceSource = "Manual Verified Entry"), date("2026-09-26"),
        ))
        assertNull(SavingsInterestEngine.planInterest(
            open.copy(balance = rebuilt.grownBalance!!, valuedOn = date("2026-09-26")),
            date("2026-09-26"),
            rebuilt.events.map { it.date }.toSet(),
        ))

        val fd = book("h_fd_old", "FIXED_INCOME", "Bank FD", "HDFC FD 2023", 100_000, acquired = "2023-04-01")
        val cumulative = SchemeTerms(
            fd, rupees(100_000), ratePercent = 7.0, compounding = "CUMULATIVE",
            priceSource = "Manual Initial Entry",
        )
        val fdPlan = requireNotNull(SavingsInterestEngine.planInterest(cumulative, date("2026-09-26")))
        assertEquals(
            listOf(date("2024-03-31"), date("2025-03-31"), date("2026-03-31")),
            fdPlan.events.map { it.date },
        )
        assertEquals(listOf(71_859_031L, 77_022_752L, 82_557_532L), fdPlan.events.map { it.amount.raw })
        assertTrue(fdPlan.events[1].amount.raw > fdPlan.events[0].amount.raw)
        assertNull(SavingsInterestEngine.planInterest(
            cumulative.copy(balance = fdPlan.grownBalance!!, valuedOn = date("2026-09-26"), priceSource = "FY interest"),
            date("2026-09-26"), fdPlan.events.map { it.date }.toSet(),
        ))

        val payout = SchemeTerms(
            book("h_fd2", "FIXED_INCOME", "Bank FD", "HDFC 3-year FD", 1_000_000, acquired = "2026-04-01"),
            rupees(1_000_000), ratePercent = 7.5, compounding = "QUARTERLY",
        )
        val paid = requireNotNull(SavingsInterestEngine.planInterest(payout, date("2027-04-01")))
        assertEquals(4, paid.events.size)
        assertEquals(187_500_000L, paid.events.first().amount.raw)
        assertNull(paid.grownBalance)
        assertNull(SavingsInterestEngine.planInterest(payout.copy(compounding = null), date("2027-04-01")))
        assertNull(SavingsInterestEngine.planInterest(payout.copy(ratePercent = null), date("2027-04-01")))

        val monthly = SchemeTerms(
            book("h_fd_month", "FIXED_INCOME", "Bank FD", "HDFC FD 2023", 100_000, acquired = "2024-01-15"),
            rupees(100_000), ratePercent = 7.0, compounding = "MONTHLY",
        )
        assertEquals(
            listOf(date("2024-02-15"), date("2024-03-15"), date("2024-04-15")),
            SavingsInterestEngine.planInterest(monthly, date("2024-04-20"))?.events?.map { it.date },
        )

        val scss = SchemeTerms(
            book("h_scss", "FIXED_INCOME", "SCSS", "SCSS", 1_000_000, acquired = "2026-04-01"),
            rupees(1_000_000),
        )
        val scssPlan = requireNotNull(SavingsInterestEngine.planInterest(scss, date("2026-10-02")))
        assertEquals(date("2026-07-01"), scssPlan.events.first().date)
        assertEquals(205_000_000L, scssPlan.events.first().amount.raw)
        assertNull(scssPlan.grownBalance)

        val other = book("h_other", "FIXED_INCOME", "Other", "Staff deposit", 50_000, acquired = "2025-04-01")
        assertEquals(SavingsKind.OTHER, SavingsInterestEngine.interestKindOf(other, 6.0))
        assertEquals(
            date("2026-03-31"),
            SavingsInterestEngine.planInterest(
                SchemeTerms(other, rupees(50_000), ratePercent = 6.0), date("2026-09-26"),
            )?.events?.single()?.date,
        )
        assertNull(SavingsInterestEngine.interestKindOf(
            book("h_aif", "SPECIALISED", "AIF", "INDIA DISCOVERY FUND II", 170),
        ))
    }

    @Test fun monthlyIncomeStopsOnTheEndDate() {
        val reit = book(
            "h_reit", "REAL_ESTATE", "REIT", "Embassy Office Parks REIT",
            balance = 320, cost = 300, qty = 100, acquired = "2026-01-01",
        )
        val terms = SchemeTerms(
            reit, rupees(320), monthlyIncome = rupees(15_000), incomeDay = 15,
            incomeStart = date("2026-01-10"), incomeEnd = date("2026-03-20"),
        )
        val posted = requireNotNull(SavingsInterestEngine.planMonthlyIncome(terms, date("2026-09-26")))
        assertEquals(
            listOf(date("2026-01-15"), date("2026-02-15"), date("2026-03-15")),
            posted.map { it.date },
        )
        assertEquals(rupees(15_000), posted.first().amount)
        assertTrue(posted.first().notes.startsWith("Monthly income"))
        assertNull(SavingsInterestEngine.planMonthlyIncome(terms, date("2026-09-26"), posted.map { it.date }.toSet()))
        assertEquals(date("2026-02-15"), SavingsInterestEngine.nextMonthlyIncomeDate(terms, date("2026-02-01")))
        assertNull(SavingsInterestEngine.nextMonthlyIncomeDate(terms, date("2026-03-16")))
        val clamped = terms.copy(incomeDay = 31, incomeStart = date("2026-01-01"), incomeEnd = null)
        assertEquals(
            date("2026-01-28"),
            SavingsInterestEngine.planMonthlyIncome(clamped, date("2026-01-31"))?.first()?.date,
        )
    }
}
