package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section Y, the walkthrough. */
class OnboardingTest {
    @Test fun sixSlidesKeepTheStoreCopy() {
        val slides = Onboarding.slides
        assertEquals(6, slides.size)
        assertEquals(listOf("why", "quiet", "fraud", "cas", "family", "price"), slides.map { it.id })
        assertEquals(listOf("shield", "phone", "lock", "file", "users", "rupee"), slides.map { it.icon })
        assertTrue(slides.all { it.hiTitle.length > 4 && it.enTitle.length > 4 })
        val price = slides.single { it.id == "price" }
        assertTrue(price.enBody.contains("30") && price.enBody.contains("99"))
        assertFalse(price.enBody.contains("249") || price.enBody.contains("499") || price.enBody.contains("250") || price.enBody.contains("199"))
        assertTrue(price.hiBody.contains("₹99"))
        assertFalse(price.hiBody.contains("₹249") || price.hiBody.contains("₹499"))
        assertTrue(Onboarding.seen("1"))
        assertFalse(Onboarding.seen(null))
        assertFalse(Onboarding.seen("0"))
        assertEquals("gud.onboarding.seen", Onboarding.SEEN_KEY)
    }
}
