package app.steptracker.domain

import java.time.Instant
import java.time.LocalDate

/** One raw reading of the hardware cumulative step counter. */
data class Reading(val counter: Long, val bootCount: Int, val at: Instant)

/** The last reading that was processed. Used to compute the next delta. */
data class StepSnapshot(val lastCounter: Long, val lastBootCount: Int, val lastReadAt: Instant)

data class IngestResult(
    val newSnapshot: StepSnapshot,
    val creditedSteps: Int,
    val creditedDate: LocalDate,
)

/** Steps for one local day. `steps` is >= 0 and `goal` is > 0. */
data class DailyStepRecord(val date: LocalDate, val steps: Int, val goal: Int)

/** One row of the history list. */
data class HistoryDay(val date: LocalDate, val steps: Int, val goal: Int, val goalMet: Boolean)
