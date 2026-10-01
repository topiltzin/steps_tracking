package app.steptracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observeDay(date: String): Flow<DailyStepEntity?>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM step_snapshot WHERE id = 1")
    suspend fun getSnapshot(): StepSnapshotEntity?

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getDay(date: String): DailyStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(entity: DailyStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(entity: StepSnapshotEntity)

    @Query("UPDATE daily_steps SET goal = :goal WHERE date = :date")
    suspend fun updateGoalForDay(date: String, goal: Int): Int

    /**
     * Creates the day row if missing (steps = 0, goal = [goal]), adds [creditedSteps] to it and
     * saves [newSnapshot], all in one transaction.
     */
    @Transaction
    suspend fun applyIngest(
        date: String,
        creditedSteps: Int,
        goal: Int,
        newSnapshot: StepSnapshotEntity,
    ) {
        val existing = getDay(date) ?: DailyStepEntity(date = date, steps = 0, goal = goal)
        upsertDay(existing.copy(steps = existing.steps + creditedSteps))
        upsertSnapshot(newSnapshot)
    }

    /** Sets today's goal, creating today's row with 0 steps if needed. Past rows are untouched. */
    @Transaction
    suspend fun setGoalForDay(date: String, goal: Int) {
        val existing = getDay(date)
        if (existing == null) {
            upsertDay(DailyStepEntity(date = date, steps = 0, goal = goal))
        } else {
            updateGoalForDay(date, goal)
        }
    }
}
