package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.HoldingProfileSnapshot
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Stored vault facts only. A missing rate must not become the published default. */
class VaultFinanceReviewTest {
    private val owner = OwnerId("synthetic-owner")
    private val other = OwnerId("synthetic-other")
    private val asOf = LocalDate.parse("2026-09-26")

    private fun holding(
        id: String,
        ownerId: OwnerId,
        category: String,
        name: String,
        rupees: Long,
        sub: String,
    ) = FinancialHolding(
        HoldingId(id), ownerId, name, category, MoneyScaled(rupees * 10_000),
        storedQuantityScaled = 100_000_000L, subCategory = sub,
    )

    private fun profile(
        holding: FinancialHolding,
        rich: String = "{}",
        details: String = "{}",
        acquired: String? = null,
        cost: Long? = null,
        source: String? = null,
    ) = HoldingProfileSnapshot(
        holdingId = holding.holdingId,
        ownerId = holding.ownerId,
        name = holding.name,
        assetCategory = holding.category,
        subCategory = holding.subCategory,
        detailsJson = details,
        acquiredOn = acquired,
        priceSource = source,
        verifiedCostPerUnit = cost?.let { MoneyScaled(it * 10_000) },
        richMetadataJson = rich,
    )

    @Test fun storedPpfRateMatchesTheEngineAndAMissingRateIsNotDefaulted() {
        val ppf = holding("ppf", owner, "FIXED_INCOME", "Synthetic PPF", 100_000, "PPF")
        val stored = profile(
            ppf, rich = """{"interest_rate_bps":710}""", acquired = "2022-04-01",
            cost = 100_000, source = "Manual Initial Entry",
        )
        val lines = VaultFinanceReview.interest(listOf(ppf), listOf(stored), asOf)
        val direct = SavingsInterestEngine.planInterest(
            SchemeTerms(
                ppf.copy(acquiredOn = LocalDate.parse("2022-04-01"), openingCostPerUnit = MoneyScaled(100_000L * 10_000)),
                MoneyScaled(100_000L * 10_000), ratePercent = 7.1, priceSource = "Manual Initial Entry",
            ),
            asOf,
        )
        assertEquals(direct, lines.single().plan)
        assertEquals(71_000_000L, lines.single().plan.events.first().amount.raw)
        val blank = profile(ppf, acquired = "2022-04-01", cost = 100_000)
        assertTrue(VaultFinanceReview.interest(listOf(ppf), listOf(blank), asOf).isEmpty())
    }

    @Test fun otherOwnerIsExcludedAndPausedLoanIsNotEstimated() {
        val mine = holding("ppf", owner, "FIXED_INCOME", "Synthetic PPF", 100_000, "PPF")
        val theirs = holding("ppf2", other, "FIXED_INCOME", "Other PPF", 50_000, "PPF")
        val profiles = listOf(
            profile(mine, """{"interest_rate_bps":710}""", acquired = "2022-04-01", cost = 100_000),
            profile(theirs, """{"interest_rate_bps":710}""", acquired = "2022-04-01", cost = 50_000),
        )
        val scoped = VaultFinanceReview.interest(listOf(mine, theirs), profiles, asOf, owner)
        assertEquals(listOf(owner), scoped.map { it.ownerId })
        assertTrue(VaultFinanceReview.interest(listOf(mine, theirs), profiles, asOf, OwnerId("nobody")).isEmpty())

        val loan = holding("loan", owner, "LIABILITY", "Synthetic car loan", 612_000, "Car loan")
        val paying = profile(
            loan,
            rich = """{"monthly_emi_scaled":185000000,"remaining_emi_count":6,"emi_status":"Paying"}""",
            details = """{"nextEmiDate":"2026-07-05"}""",
        )
        val paused = profile(
            loan,
            rich = """{"monthly_emi_scaled":185000000,"remaining_emi_count":6,"emi_status":"Paused"}""",
            details = """{"nextEmiDate":"2026-07-05"}""",
        )
        val due = VaultFinanceReview.emi(listOf(loan), listOf(paying), LocalDate.parse("2026-09-09"))
        assertEquals(listOf("2026-07-05", "2026-08-05", "2026-09-05"), due.single().plan.events.map { it.date.toString() })
        assertEquals(612_000L - 18_500L * 3, due.single().plan.outstanding.raw / 10_000)
        assertTrue(VaultFinanceReview.emi(listOf(loan), listOf(paused), LocalDate.parse("2026-09-09")).isEmpty())
        assertNull(EmiPlan.plan(
            LoanEmi(
                loan.holdingId, owner, loan.name, "LIABILITY", MoneyScaled(612_000L * 10_000),
                MoneyScaled(185_000_000L), 6, LocalDate.parse("2026-07-05"), "PAUSED",
            ),
            LocalDate.parse("2026-09-09"),
        ))
    }
}
