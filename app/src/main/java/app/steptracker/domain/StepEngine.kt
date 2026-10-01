package app.steptracker.domain

import java.time.LocalDate

/**
 * Pure step-ingestion logic (see specs/001-step-tracker/contracts/step-ingestion.md).
 * No Android types, so it runs as a plain JVM unit test.
 */
object StepEngine {

    fun applyReading(snapshot: StepSnapshot?, reading: Reading, today: LocalDate): IngestResult {
        val newSnapshot = StepSnapshot(
            lastCounter = reading.counter,
            lastBootCount = reading.bootCount,
            lastReadAt = reading.at,
        )
        val credit = when {
            snapshot == null -> 0L
            reading.bootCount != snapshot.lastBootCount || reading.counter < snapshot.lastCounter ->
                reading.counter
            else -> reading.counter - snapshot.lastCounter
        }
        val credited = credit.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        return IngestResult(newSnapshot, credited, today)
    }
}
