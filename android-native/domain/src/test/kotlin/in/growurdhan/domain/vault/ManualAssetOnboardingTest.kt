package `in`.growurdhan.domain.vault

import `in`.growurdhan.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ManualAssetOnboardingTest {
    private val owner = OwnerId("owner")
    private val id = HoldingId("asset")
    @Test fun supportsLocalRetirementAndFamilyWealthCategoriesWithoutInventingValue() {
        for (category in ManualAssetOnboarding.supportedCategories) {
            val asset = ManualAssetOnboarding.create(owner, "Synthetic ${category}", category, id, emptyList())
            assertEquals(category, asset.assetCategory)
            assertEquals(0, asset.quantity.raw)
            assertEquals(0, asset.currentUnitPrice.raw)
            assertNull(asset.accountId)
        }
    }
    @Test fun rejectsUnknownCategoriesPanNamesAndCrossOwnerDuplicates() {
        assertThrows(IllegalArgumentException::class.java) {
            ManualAssetOnboarding.create(owner, "Synthetic", "UNKNOWN", id, emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualAssetOnboarding.create(owner, "ABCDE1234F", "NPS", id, emptyList())
        }
        val first = ManualAssetOnboarding.create(owner, "Synthetic Gold", "GOLD", id, emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            ManualAssetOnboarding.create(owner, " synthetic gold ", "GOLD",
                HoldingId("second"), listOf(first))
        }
        assertNotNull(ManualAssetOnboarding.create(OwnerId("other"), first.name, "GOLD",
            HoldingId("third"), listOf(first)))
    }
}
