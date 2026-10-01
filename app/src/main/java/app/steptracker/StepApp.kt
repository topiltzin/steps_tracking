package app.steptracker

import android.app.Application
import androidx.work.ExistingWorkPolicy
import app.steptracker.work.SyncScheduler

class StepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedulePeriodic(this)
        SyncScheduler.scheduleNextMidnight(this, ExistingWorkPolicy.KEEP)
    }
}
