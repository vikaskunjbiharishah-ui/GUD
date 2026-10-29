package `in`.growurdhan.app.gmail

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import `in`.growurdhan.domain.mail.MailboxSyncSchedule
import java.util.concurrent.TimeUnit

/**
 * Schedules a monthly check and a manual check. The worker does not call
 * Gmail. A test or a future signed-in gateway assigns [MailboxSyncRuntime.run].
 * Real OAuth stays blocked.
 */
interface MailboxSyncLauncher {
    fun enqueueMonthly()
    fun enqueueManual()
}

object MailboxSyncRuntime {
    @Volatile var run: () -> Result = { Result.success() }
}

class WorkMailboxSyncLauncher(private val context: Context) : MailboxSyncLauncher {
    override fun enqueueMonthly() {
        val request = PeriodicWorkRequestBuilder<MailboxSyncWorker>(
            MailboxSyncSchedule.MONTHLY_DAYS, TimeUnit.DAYS,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MailboxSyncSchedule.MONTHLY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun enqueueManual() {
        val request = OneTimeWorkRequestBuilder<MailboxSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MailboxSyncSchedule.MANUAL_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

class MailboxSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = MailboxSyncRuntime.run()
}
