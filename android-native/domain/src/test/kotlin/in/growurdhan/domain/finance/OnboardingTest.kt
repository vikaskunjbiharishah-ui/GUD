package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section Y, plus the owner Hindi of 2026-10-29. */
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
        assertTrue(price.hiBody.contains("₹99"))
        assertFalse(price.hiBody.contains("₹249") || price.hiBody.contains("₹499"))
        assertEquals(
            "परिवार का वॉल्ट — एक और ब्रोकर लॉगिन नहीं।",
            slides.single { it.id == "why" }.hiTitle,
        )
        assertTrue(Onboarding.seen("1"))
        assertFalse(Onboarding.seen(null))
        assertFalse(Onboarding.seen("0"))
        assertEquals("gud.onboarding.seen", Onboarding.SEEN_KEY)
    }

    @Test fun hindiSentencesUseDevanagariAndTheOwnerGlossary() {
        val banned = listOf("तिजोरी", "बयान", "बादल")
        for (slide in Onboarding.slides) {
            for (text in listOf(slide.hiTitle, slide.hiBody)) {
                assertTrue(text.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.DEVANAGARI })
                assertTrue(text.endsWith("।") || text.endsWith("?"))
                assertFalse(text.contains("."))
                banned.forEach { word -> assertFalse(text.contains(word), "$text contains $word") }
            }
        }
        val hindi = Onboarding.chrome(true)
        assertEquals("आगे", hindi.next)
        assertEquals("पीछे", hindi.back)
        assertEquals("छोड़ें", hindi.skip)
        assertEquals("अंग्रेज़ी", hindi.languageToggle)
        assertTrue(hindi.scrollCue.endsWith("।"))
        assertFalse(hindi.scrollCue.contains("."))
        banned.forEach { word -> assertFalse(hindi.scrollCue.contains(word)) }
        assertEquals("Next", Onboarding.chrome(false).next)
        assertEquals("Skip", Onboarding.chrome(false).skip)
    }
}
