package `in`.growurdhan.domain.vault

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OriginalHoldingFormPolicyTest {
    @Test fun allEightOriginalAssetTypesHaveIndependentSupportedSubtypes() {
        assertEquals(8, ManualAssetOnboarding.canonicalCategories.size)
        ManualAssetOnboarding.canonicalCategories.forEach { category ->
            ManualAssetOnboarding.subCategories.getValue(category).forEach { subtype ->
                OriginalHoldingFormPolicy.required(category, subtype, emptyMap())
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            OriginalHoldingFormPolicy.required("NPS", "Bank FD", emptyMap())
        }
    }

    @Test fun schemeSpecificFieldsAreMandatoryOnlyWhenFullOnboardingIsRequested() {
        assertEquals(setOf("monthly_emi_scaled", "remaining_emi_count", "nextEmiDate"),
            OriginalHoldingFormPolicy.missing("LIABILITY", "Home loan", emptyMap())
                .map { it.key }.toSet())
        assertEquals(listOf("aifCategory", "commitmentMinor"),
            OriginalHoldingFormPolicy.missing("SPECIALISED", "AIF",
                mapOf("altHoldMode" to "LUMPSUM")).map { it.key })
        assertEquals(listOf("npsTier"),
            OriginalHoldingFormPolicy.missing("FIXED_INCOME", "NPS", emptyMap())
                .map { it.key })
        assertEquals(listOf("compounding"),
            OriginalHoldingFormPolicy.missing("FIXED_INCOME", "Bank FD", emptyMap())
                .map { it.key })
        assertEquals(listOf("goldPurity"),
            OriginalHoldingFormPolicy.missing("GOLD_SGB", "Physical gold (24K)", emptyMap())
                .map { it.key })
        assertEquals(listOf("mfHeldAs"),
            OriginalHoldingFormPolicy.missing("MUTUAL_FUND", "Equity fund", emptyMap())
                .map { it.key })
    }

    @Test fun completeSyntheticLoanAndAifFormsPassWithoutInventingLedgerAmounts() {
        assertTrue(OriginalHoldingFormPolicy.missing("LIABILITY", "Home loan", mapOf(
            "monthly_emi_scaled" to "185000000",
            "remaining_emi_count" to "120", "nextEmiDate" to "2026-11-05",
        )).isEmpty())
        assertTrue(OriginalHoldingFormPolicy.missing("SPECIALISED", "AIF", mapOf(
            "altHoldMode" to "LUMPSUM", "aifCategory" to "II",
            "commitmentMinor" to "2500000000",
        )).isEmpty())
        assertFalse(OriginalHoldingFormPolicy.usesSingleMonetaryBalance(
            "EQUITY", "Large cap", emptyMap()))
        assertTrue(OriginalHoldingFormPolicy.usesSingleMonetaryBalance(
            "FIXED_INCOME", "NPS", emptyMap()))
        assertTrue(OriginalHoldingFormPolicy.usesSingleMonetaryBalance(
            "SPECIALISED", "AIF", mapOf("altHoldMode" to "LUMPSUM")))
    }

    @Test fun equityAndBankFormsExposeOriginalIdentifierFieldsWithoutLedgerRows() {
        assertEquals(
            setOf("depository", "dpId", "clientId", "boId"),
            OriginalHoldingFormPolicy.preferredDetailKeys("EQUITY", "Large cap"),
        )
        assertEquals(
            setOf("ifsc"),
            OriginalHoldingFormPolicy.preferredDetailKeys("CASH_BANK", "Savings account"),
        )
        assertTrue(OriginalHoldingFormPolicy.preferredDetailKeys(
            "CASH_BANK", "Liquid / overnight",
        ).isEmpty())
        assertTrue(OriginalHoldingFormPolicy.preferredDetailKeys("SPECIALISED", "PMS")
            .containsAll(setOf("strategyName", "custodian")))
        assertEquals("IN300123", OriginalHoldingFormPolicy.normalizeStoredDetail("dpId", " in 300 123 "))
        assertEquals("12345678", OriginalHoldingFormPolicy.normalizeStoredDetail("clientId", "1234 5678"))
        assertEquals("1201234567890123", OriginalHoldingFormPolicy.normalizeStoredDetail("boId", "1201 2345 6789 0123"))
        assertEquals("HDFC0000123", OriginalHoldingFormPolicy.normalizeStoredDetail("ifsc", "hdfc 0000123"))
        assertEquals("Ask Strategy", OriginalHoldingFormPolicy.normalizeStoredDetail("strategyName", "Ask Strategy"))
    }
}
