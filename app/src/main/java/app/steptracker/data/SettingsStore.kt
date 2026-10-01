package app.steptracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists the daily goal only. Validation lives in `GoalValidator`. */
class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val goal: Flow<Int> = dataStore.data.map { it[GOAL_KEY] ?: DEFAULT_GOAL }

    suspend fun setGoal(value: Int) {
        dataStore.edit { it[GOAL_KEY] = value }
    }

    companion object {
        const val DEFAULT_GOAL = 10_000
        private val GOAL_KEY = intPreferencesKey("daily_goal")
    }
}
