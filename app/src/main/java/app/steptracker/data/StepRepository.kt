package app.steptracker.data

import app.steptracker.data.db.DailyStepEntity
import app.steptracker.data.db.StepDao
import app.steptracker.data.db.StepSnapshotEntity
import app.steptracker.domain.DailyStepRecord
import app.steptracker.domain.HistoryBuilder
import app.steptracker.domain.HistoryDay
import app.steptracker.domain.StepEngine
import app.steptracker.domain.StepSnapshot
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class StepRepository(
    private val dao: StepDao,
    private val settings: SettingsStore,
    private val sensor: StepSensorSource,
) {
    val isSupported: Boolean get() = sensor.isSupported()

    val goal: Flow<Int> = settings.goal

    /** Reads the sensor and credits the new steps to today. A failed or null read does nothing. */
    suspend fun syncNow() {
        val reading = sensor.read() ?: return
        val snapshot = dao.getSnapshot()?.toDomain()
        val today = LocalDate.now()
        val result = StepEngine.applyReading(snapshot, reading, today)
        dao.applyIngest(
            date = result.creditedDate.toString(),
            creditedSteps = result.creditedSteps,
            goal = settings.goal.first(),
            newSnapshot = result.newSnapshot.toEntity(),
        )
    }

    fun observeToday(date: LocalDate = LocalDate.now()): Flow<DailyStepRecord?> =
        dao.observeDay(date.toString()).map { it?.toDomain() }

    suspend fun updateGoal(value: Int) {
        settings.setGoal(value)
        dao.setGoalForDay(LocalDate.now().toString(), value)
    }

    fun observeHistory(): Flow<List<HistoryDay>> =
        combine(dao.observeRecent(limit = 30), settings.goal) { rows, goal ->
            HistoryBuilder.build(rows.map { it.toDomain() }, LocalDate.now(), goal)
        }
}

private fun DailyStepEntity.toDomain() = DailyStepRecord(LocalDate.parse(date), steps, goal)

private fun StepSnapshotEntity.toDomain() =
    StepSnapshot(lastCounter, lastBootCount, Instant.ofEpochMilli(lastReadAt))

private fun StepSnapshot.toEntity() =
    StepSnapshotEntity(id = 1, lastCounter = lastCounter, lastBootCount = lastBootCount, lastReadAt = lastReadAt.toEpochMilli())
