package app.steptracker

import android.content.Context
import app.steptracker.data.SettingsStore
import app.steptracker.data.StepRepository
import app.steptracker.data.StepSensorSource
import app.steptracker.data.db.StepDatabase

/** Manual dependency container, created once by [StepApp]. */
class AppContainer(context: Context) {
    private val database = StepDatabase.create(context)
    private val settingsStore = SettingsStore(context)
    private val sensorSource = StepSensorSource(context)

    val repository = StepRepository(database.stepDao(), settingsStore, sensorSource)
}
