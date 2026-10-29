package `in`.growurdhan.app.gmail

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import `in`.growurdhan.domain.mail.MailboxSyncSchedule
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The wrapper is WorkManager. The worker does not open a network connection.
 */
@RunWith(AndroidJUnit4::class)
class MailboxSyncSchedulerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After fun cancel() {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(MailboxSyncSchedule.MANUAL_WORK)
        manager.cancelUniqueWork(MailboxSyncSchedule.MONTHLY_WORK)
        MailboxSyncRuntime.run = { androidx.work.ListenableWorker.Result.success() }
    }

    @Test fun manualSyncRunsTheHookAndMonthlyWorkStaysQueued() {
        var calls = 0
        MailboxSyncRuntime.run = {
            calls += 1
            androidx.work.ListenableWorker.Result.success()
        }
        val launcher = WorkMailboxSyncLauncher(context)
        launcher.enqueueMonthly()
        launcher.enqueueManual()
        val manager = WorkManager.getInstance(context)
        val monthly = manager
            .getWorkInfosForUniqueWork(MailboxSyncSchedule.MONTHLY_WORK)
            .get(10, TimeUnit.SECONDS)
        assertEquals(1, monthly.size)
        assertEquals(WorkInfo.State.ENQUEUED, monthly.single().state)
        val deadline = System.currentTimeMillis() + 20_000
        while (calls == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(200)
        }
        assertTrue("manual worker did not run", calls >= 1)
        val manual = manager
            .getWorkInfosForUniqueWork(MailboxSyncSchedule.MANUAL_WORK)
            .get(10, TimeUnit.SECONDS)
        assertTrue(manual.single().state == WorkInfo.State.SUCCEEDED ||
            manual.single().state == WorkInfo.State.RUNNING)
    }
}
