package app.steptracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryBuilderTest {

    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun `returns exactly 7 days newest first with gaps filled`() {
        val records = listOf(
            DailyStepRecord(today.minusDays(9), 4000, 8000),
            DailyStepRecord(today.minusDays(2), 12000, 10000),
            DailyStepRecord(today, 3000, 10000),
        )
        val result = HistoryBuilder.build(records, today, currentGoal = 9000)
        assertEquals(7, result.size)
        assertEquals(today, result.first().date)
        assertEquals(today.minusDays(6), result.last().date)
        val gap = result.first { it.date == today.minusDays(1) }
        assertEquals(0, gap.steps)
        assertEquals(9000, gap.goal)
        assertFalse(gap.goalMet)
    }

    @Test
    fun `goalMet is steps greater or equal to goal`() {
        val records = listOf(
            DailyStepRecord(today, 10000, 10000),
            DailyStepRecord(today.minusDays(1), 9999, 10000),
        )
        val result = HistoryBuilder.build(records, today, 10000)
        assertTrue(result[0].goalMet)
        assertFalse(result[1].goalMet)
    }

    @Test
    fun `fewer days of data shows only the available days`() {
        val records = listOf(DailyStepRecord(today.minusDays(2), 100, 10000))
        val result = HistoryBuilder.build(records, today, 10000)
        assertEquals(3, result.size)
        assertEquals(today.minusDays(2), result.last().date)
    }

    @Test
    fun `no records gives a single zero day for today`() {
        val result = HistoryBuilder.build(emptyList(), today, 10000)
        assertEquals(1, result.size)
        assertEquals(today, result[0].date)
        assertEquals(0, result[0].steps)
    }
}
