package app.steptracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepEngineTest {

    private val day1 = LocalDate.of(2026, 10, 1)
    private val day2 = day1.plusDays(1)

    private fun at(date: LocalDate, hour: Int = 12, minute: Int = 0): Instant =
        date.atTime(hour, minute).toInstant(ZoneOffset.UTC)

    private fun snap(counter: Long, boot: Int, at: Instant = at(day1)) = StepSnapshot(counter, boot, at)
    private fun read(counter: Long, boot: Int, at: Instant = at(day1)) = Reading(counter, boot, at)

    @Test
    fun `1 first run credits 0 and stores the snapshot`() {
        val r = StepEngine.applyReading(null, read(5000, 7), day1)
        assertEquals(0, r.creditedSteps)
        assertEquals(5000L, r.newSnapshot.lastCounter)
        assertEquals(7, r.newSnapshot.lastBootCount)
    }

    @Test
    fun `2 normal reading credits the delta`() {
        val r = StepEngine.applyReading(snap(5000, 7), read(5120, 7), day1)
        assertEquals(120, r.creditedSteps)
        assertEquals(5120L, r.newSnapshot.lastCounter)
    }

    @Test
    fun `3 duplicate reading credits 0`() {
        val r = StepEngine.applyReading(snap(5120, 7), read(5120, 7), day1)
        assertEquals(0, r.creditedSteps)
    }

    @Test
    fun `4 reboot with lower value credits the new counter`() {
        val r = StepEngine.applyReading(snap(5120, 7), read(300, 8), day1)
        assertEquals(300, r.creditedSteps)
        assertEquals(8, r.newSnapshot.lastBootCount)
    }

    @Test
    fun `5 reboot with higher value is detected by boot count`() {
        val r = StepEngine.applyReading(snap(100, 7), read(400, 8), day1)
        assertEquals(400, r.creditedSteps)
    }

    @Test
    fun `6 midnight crossing credits the new day`() {
        val before = snap(6000, 7, at(day1, 23, 55))
        val r = StepEngine.applyReading(before, read(6040, 7, at(day2, 0, 5)), day2)
        assertEquals(40, r.creditedSteps)
        assertEquals(day2, r.creditedDate)
    }

    @Test
    fun `7 multi-day gap credits everything to the reading day`() {
        val r = StepEngine.applyReading(snap(6000, 7), read(9000, 7, at(day1.plusDays(3))), day1.plusDays(3))
        assertEquals(3000, r.creditedSteps)
        assertEquals(day1.plusDays(3), r.creditedDate)
    }

    @Test
    fun `8 clock change never yields a negative credit`() {
        val earlier = read(5200, 7, at(day1.minusDays(2)))
        val r = StepEngine.applyReading(snap(5000, 7), earlier, day1.minusDays(2))
        assertTrue(r.creditedSteps >= 0)
        val lower = StepEngine.applyReading(snap(5000, 7), read(10, 7), day1)
        assertTrue(lower.creditedSteps >= 0)
    }

    @Test
    fun `applying the same reading twice credits 0 the second time`() {
        val reading = read(5120, 7)
        val first = StepEngine.applyReading(snap(5000, 7), reading, day1)
        val second = StepEngine.applyReading(first.newSnapshot, reading, day1)
        assertEquals(120, first.creditedSteps)
        assertEquals(0, second.creditedSteps)
    }
}
