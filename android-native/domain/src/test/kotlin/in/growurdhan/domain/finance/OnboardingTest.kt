package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section Y. Hindi was removed on 2026-11-09. */
class OnboardingTest {
    @Test fun sixSlidesKeepTheStoreCopy() {
        val slides = Onboarding.slides
        assertEquals(6, slides.size)
        assertEquals(listOf("why", "quiet", "fraud", "cas", "family", "price"), slides.map { it.id })
        assertEquals(listOf("shield", "phone", "lock", "file", "users", "rupee"), slides.map { it.icon })
        assertTrue(slides.all { it.hiTitle.length > 4 && it.enTitle.length > 4 })
        val price = slides.single { it.id == "price" }
        assertTrue(price.enBody.contains("30") && price.enBody.contains("99"))
        assertTrue(price.enBody.endsWith("You can cancel any time in Google Play."))
        assertFalse(price.enBody.contains("249") || price.enBody.contains("499") || price.enBody.contains("250") || price.enBody.contains("199"))
        assertEquals("A family vault. Not another brokerage login.", slides.single { it.id == "why" }.enTitle)
        assertTrue(Onboarding.seen("1"))
        assertFalse(Onboarding.seen(null))
        assertFalse(Onboarding.seen("0"))
        assertEquals("gud.onboarding.seen", Onboarding.SEEN_KEY)
    }

    @Test fun englishChromeDoesNotOfferHindi() {
        val english = Onboarding.chrome(false)
        val ignored = Onboarding.chrome(true)
        assertEquals(english, ignored)
        assertEquals("Next", english.next)
        assertEquals("Back", english.back)
        assertEquals("Skip", english.skip)
        assertEquals("Continue", english.start)
        assertEquals("", english.languageToggle)
        assertEquals("Scroll for the rest of this slide.", english.scrollCue)
        assertFalse(english.next.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.DEVANAGARI })
    }
}
