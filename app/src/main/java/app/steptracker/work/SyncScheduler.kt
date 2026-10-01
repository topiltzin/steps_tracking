package app.steptracker.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val PERIODIC_NAME = "step-sync-periodic"
    private const val MIDNIGHT_NAME = "step-sync-midnight"
    const val KEY_RESCHEDULE_MIDNIGHT = "reschedule_midnight"

    /** Reads the counter every 15 minutes (the platform minimum). */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * One-time read shortly after the next local midnight, to tighten day boundaries. The app
     * start uses KEEP; the worker itself re-schedules with REPLACE once its read is done.
     */
    fun scheduleNextMidnight(context: Context, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now()
        val target = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
            .setInitialDelay(Duration.between(now, target).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_RESCHEDULE_MIDNIGHT to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(MIDNIGHT_NAME, policy, request)
    }
}
