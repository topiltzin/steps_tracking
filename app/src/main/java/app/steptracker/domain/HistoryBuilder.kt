package app.steptracker.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object HistoryBuilder {

    /**
     * Builds up to [days] days ending at [today], newest first. Days without a record are
     * filled with 0 steps and [currentGoal]. Never goes back before the first record, so a
     * new install shows only the days available. With no records, returns today with 0 steps.
     */
    fun build(
        records: List<DailyStepRecord>,
        today: LocalDate,
        currentGoal: Int,
        days: Int = 7,
    ): List<HistoryDay> {
        val byDate = records.associateBy { it.date }
        val earliest = records.minOfOrNull { it.date } ?: today
        val available = (ChronoUnit.DAYS.between(earliest, today) + 1).coerceAtLeast(1)
        val count = minOf(days.toLong(), available).toInt()
        return (0 until count).map { offset ->
            val date = today.minusDays(offset.toLong())
            val record = byDate[date]
            val steps = record?.steps ?: 0
            val goal = record?.goal ?: currentGoal
            HistoryDay(date, steps, goal, goalMet = steps >= goal)
        }
    }
}
