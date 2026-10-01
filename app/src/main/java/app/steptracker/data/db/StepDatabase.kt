package app.steptracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStepEntity::class, StepSnapshotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StepDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao

    companion object {
        fun create(context: Context): StepDatabase =
            Room.databaseBuilder(context.applicationContext, StepDatabase::class.java, "steps.db").build()
    }
}
