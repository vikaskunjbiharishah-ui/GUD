package `in`.growurdhan.data.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.domain.finance.FeedbackState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * gudStep=seed writes the clock. gudStep=read checks it after the process was killed.
 * A run with no argument only proves a second repository in the same process.
 */
@RunWith(AndroidJUnit4::class)
class FeedbackProcessDeathInstrumentedTest {
    @Test fun feedbackClockStep(): Unit = runBlocking {
        val step = InstrumentationRegistry.getArguments().getString("gudStep")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stamp = "2026-08-15T00:00:00.000Z"
        val repo = DataStoreAppearanceRepository(context)
        if (step == "read") {
            val loaded = repo.feedback.first()
            assertEquals(stamp, loaded.firstSeenAt)
            assertEquals(5, loaded.promptCount)
            repo.saveFeedback(FeedbackState())
            return@runBlocking
        }
        repo.saveFeedback(FeedbackState(firstSeenAt = stamp, promptCount = 5))
        if (step == "seed") return@runBlocking
        val loaded = DataStoreAppearanceRepository(context).feedback.first()
        assertEquals(stamp, loaded.firstSeenAt)
        assertEquals(5, loaded.promptCount)
        repo.saveFeedback(FeedbackState())
    }
}
