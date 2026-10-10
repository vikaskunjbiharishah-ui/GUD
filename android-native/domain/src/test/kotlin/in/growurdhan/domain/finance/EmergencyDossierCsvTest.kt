package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EmergencyDossierCsvTest {
    private fun sample(name: String = "Synthetic family asset") = HouseholdDashboard(
        asOf = LocalDate.parse("2026-10-10"),
        assets = MoneyScaled(200_000),
        liabilities = MoneyScaled(0),
        netWorth = MoneyScaled(200_000),
        memberCount = 1, holdingCount = 1, unknownCostHoldings = 1,
        integrityWarnings = emptyList(), byMember = emptyList(),
        byCategory = listOf(CategoryDashboard("GOLD", MoneyScaled(200_000), 1)),
        recordedHoldings = listOf(RecordedHoldingDashboard(
            name, "GOLD", "Primary", 100_000_000L, MoneyScaled(200_000),
            null, true, false,
        )),
    )
    @Test fun printedSummaryDisclosesStoredPricesAndUnknownCostWithoutIdentifiers() {
        val output = EmergencyDossierCsv.export(sample()).toString(Charsets.UTF_8)
        assertTrue(output.contains("UNENCRYPTED USER-REQUESTED"))
        assertTrue(output.contains("NOT live market quotations"))
        assertTrue(output.contains("\"Primary\",\"GOLD\",\"Synthetic family asset\""))
        assertTrue(output.contains("\"20.0000\""))
        assertTrue(output.contains("\"Cost verified\""))
        assertFalse(output.contains("PAN"))
        assertFalse(output.contains("folio"))
        assertFalse(output.contains("purchase password"))
    }
    @Test fun spreadsheetFormulaInjectionAndControlCharactersAreNeutralized() {
        val csv = EmergencyDossierCsv.export(sample("=HYPERLINK(\"bad\")\r\n@cmd"))
            .toString(Charsets.UTF_8)
        assertTrue(csv.contains("'=HYPERLINK"))
        assertFalse(csv.contains("\r\n@cmd"))
        assertTrue(csv.contains("\"\"bad\"\""))
    }
    @Test fun exportRefusesUnreconciledOrMisleadingTotals() {
        val inconsistent = sample().copy(
            byCategory = listOf(CategoryDashboard("GOLD", MoneyScaled(100_000), 1)),
        )
        assertThrows(IllegalStateException::class.java) {
            EmergencyDossierCsv.export(inconsistent)
        }
        assertThrows(IllegalStateException::class.java) {
            EmergencyDossierCsv.export(sample().copy(
                integrityWarnings = listOf("Inconsistent ledger"),
            ))
        }
    }
}
