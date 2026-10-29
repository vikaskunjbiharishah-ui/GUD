package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.CasHoldingIdentity
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SyntheticIssuerStatementTest {
    private val owner = OwnerId("self")
    private val spouse = OwnerId("spouse")
    private val today = LocalDate.parse("2026-10-16")
    private val equity = FinancialHolding(
        HoldingId("eq"), owner, "Printed name can differ", "EQUITY", MoneyScaled(1_000_000),
    )
    private val nps = FinancialHolding(
        HoldingId("nps"), owner, "Synthetic NPS equity scheme", "FIXED_INCOME", MoneyScaled(200_000),
    )

    private fun text(issuer: String, holding: String, sha: String = "ab".repeat(32)) = """
        SYNTHETIC-ISSUER 1
        issuer: $issuer
        statement-date: 2026-09-30
        source-sha256: $sha
        holding: $holding
    """.trimIndent()

    @Test fun cdslMatchesIsinAndBoIdWithoutUsingThePrintedName() {
        val parsed = SyntheticIssuerStatement.parse(text(
            "CDSL",
            "Some other printed name|EQUITY|10|100.0000|800.0000|INE000000001|BO1234567890",
        ))
        val preview = CasImportContract.validate(
            parsed, owner, listOf(equity), today,
            listOf(CasHoldingIdentity(
                equity.holdingId, owner, "demat-1", "INE000000001", "BO1234567890", "CDSL",
            )),
        )
        assertEquals(equity.holdingId, preview.lines.single().holdingId)
        assertEquals(1_000_000_000L, preview.lines.single().quantityScaled)
        assertEquals(MoneyScaled(8_000_000L), preview.lines.single().cumulativeInvested)
    }

    @Test fun npsRequiresPranAndSchemeAndRejectsTheOtherOwner() {
        val parsed = SyntheticIssuerStatement.parse(text(
            "NPS",
            "Synthetic NPS equity scheme|FIXED_INCOME|3.5|20.0000||NP0000000001|110123981321",
            sha = "cd".repeat(32),
        ))
        val own = CasHoldingIdentity(
            nps.holdingId, owner, "nps-1", "NP0000000001", "110123981321", "NPS",
        )
        val preview = CasImportContract.validate(parsed, owner, listOf(nps), today, listOf(own))
        assertEquals(350_000_000L, preview.lines.single().quantityScaled)
        assertNull(preview.lines.single().cumulativeInvested)
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(
                parsed, owner, listOf(nps), today,
                listOf(own.copy(ownerId = spouse)),
            )
        }
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(parsed, spouse, listOf(nps), today, listOf(own))
        }
    }

    @Test fun wrongBoIdOrSchemeNameIsNotImported() {
        val cdsl = SyntheticIssuerStatement.parse(text(
            "CDSL",
            "Printed|EQUITY|1|1.0000||INE000000001|OTHERBO",
        ))
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(
                cdsl, owner, listOf(equity), today,
                listOf(CasHoldingIdentity(
                    equity.holdingId, owner, "demat-1", "INE000000001", "BO1234567890", "CDSL",
                )),
            )
        }
        val renamed = SyntheticIssuerStatement.parse(text(
            "NPS",
            "Different scheme|FIXED_INCOME|1|1.0000||NP0000000001|110123981321",
        ))
        assertThrows(CasValidationException::class.java) {
            CasImportContract.validate(
                renamed, owner, listOf(nps), today,
                listOf(CasHoldingIdentity(
                    nps.holdingId, owner, "nps-1", "NP0000000001", "110123981321", "NPS",
                )),
            )
        }
    }
}
