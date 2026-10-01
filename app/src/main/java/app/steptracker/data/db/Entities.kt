package app.steptracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepEntity(
    /** Local calendar day as ISO-8601 text (yyyy-MM-dd). */
    @PrimaryKey val date: String,
    val steps: Int,
    val goal: Int,
)

@Entity(tableName = "step_snapshot")
data class StepSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val lastCounter: Long,
    val lastBootCount: Int,
    val lastReadAt: Long,
)
