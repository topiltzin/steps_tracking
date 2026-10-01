package app.steptracker.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepDaoTest {
    private lateinit var db: StepDatabase
    private lateinit var dao: StepDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StepDatabase::class.java).allowMainThreadQueries().build()
        dao = db.stepDao()
    }

    @After
    fun tearDown() = db.close()

    private fun snap(counter: Long) = StepSnapshotEntity(lastCounter = counter, lastBootCount = 7, lastReadAt = counter)

    @Test
    fun applyIngest_createsDayWithGoalOnFirstUse() = runBlocking {
        dao.applyIngest("2026-10-01", 0, 8000, snap(5000))
        val day = dao.getDay("2026-10-01")
        assertNotNull(day)
        assertEquals(0, day!!.steps)
        assertEquals(8000, day.goal)
    }

    @Test
    fun applyIngest_addsCreditsAndUpdatesSnapshotTogether() = runBlocking {
        dao.applyIngest("2026-10-01", 0, 8000, snap(5000))
        dao.applyIngest("2026-10-01", 120, 9999, snap(5120))
        dao.applyIngest("2026-10-01", 30, 9999, snap(5150))
        val day = dao.getDay("2026-10-01")!!
        assertEquals(150, day.steps)
        assertEquals(8000, day.goal) // goal snapshot is not rewritten by ingestion
        assertEquals(5150L, dao.getSnapshot()!!.lastCounter)
    }

    @Test
    fun applyIngest_secondDayGetsItsOwnRow() = runBlocking {
        dao.applyIngest("2026-10-01", 100, 8000, snap(5100))
        dao.applyIngest("2026-10-02", 40, 8000, snap(5140))
        assertEquals(100, dao.getDay("2026-10-01")!!.steps)
        assertEquals(40, dao.getDay("2026-10-02")!!.steps)
    }

    @Test
    fun setGoalForDay_createsTodayAndNeverTouchesPastRows() = runBlocking {
        dao.applyIngest("2026-10-01", 100, 8000, snap(5100))
        dao.setGoalForDay("2026-10-02", 12000)
        assertEquals(8000, dao.getDay("2026-10-01")!!.goal)
        val today = dao.getDay("2026-10-02")!!
        assertEquals(0, today.steps)
        assertEquals(12000, today.goal)
    }
}
