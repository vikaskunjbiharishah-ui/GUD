package `in`.growurdhan.data.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.domain.finance.FeedbackState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The rating clock is DataStore, not a Compose remember. The process was not killed. */
@RunWith(AndroidJUnit4::class)
class FeedbackClockInstrumentedTest {
    @Test fun feedbackClockSurvivesANewRepository(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DataStoreAppearanceRepository(context)
        val stamp = "2026-09-01T00:00:00.000Z"
        val snooze = "2026-09-10T00:00:00.000Z"
        first.saveFeedback(FeedbackState(firstSeenAt = stamp, snoozeUntil = snooze, promptCount = 2))
        val loaded = DataStoreAppearanceRepository(context).feedback.first()
        assertEquals(stamp, loaded.firstSeenAt)
        assertEquals(snooze, loaded.snoozeUntil)
        assertEquals(2, loaded.promptCount)
        assertNull(loaded.completedAt)
        first.saveFeedback(FeedbackState())
    }
}
