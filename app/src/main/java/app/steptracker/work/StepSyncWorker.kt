package app.steptracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import app.steptracker.StepApp
import kotlin.coroutines.cancellation.CancellationException

class StepSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            (applicationContext as StepApp).container.repository.syncNow()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The next periodic run or opening the app catches up. Never fail the schedule.
        }
        if (inputData.getBoolean(SyncScheduler.KEY_RESCHEDULE_MIDNIGHT, false)) {
            SyncScheduler.scheduleNextMidnight(applicationContext, ExistingWorkPolicy.REPLACE)
        }
        return Result.success()
    }
}
