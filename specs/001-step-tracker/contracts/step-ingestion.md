# Contract: Step Ingestion (domain core)

Pure function, with no Android types, so it can be unit tested on the JVM.

## Signature

```text
applyReading(snapshot: StepSnapshot?, reading: Reading, today: LocalDate, goal: Int)
    -> IngestResult(newSnapshot: StepSnapshot, creditedSteps: Int, creditedDate: LocalDate)

Reading = { counter: Long, bootCount: Int, at: Instant }
```

## Rules

1. `snapshot == null` → `creditedSteps = 0`, and the new snapshot is built from the reading.
2. `reading.bootCount != snapshot.lastBootCount` or `reading.counter < snapshot.lastCounter` → reboot: `creditedSteps = reading.counter`.
3. Otherwise `creditedSteps = reading.counter − snapshot.lastCounter`.
4. Steps are credited to `creditedDate = today` (the local date of `reading.at`).
5. `creditedSteps` is never negative. The result is always ≥ 0.
6. Applying the same reading twice credits 0 the second time, because the snapshot already holds that counter. The operation is idempotent.
7. A multi-day gap credits everything to the day of the reading (see spec Assumptions).

## Test cases (required)

| # | Scenario | Snapshot | Reading | Expected credit |
|---|---|---|---|---|
| 1 | First run | none | 5000 | 0 |
| 2 | Normal | 5000 | 5120 | 120 |
| 3 | Duplicate reading | 5120 | 5120 | 0 |
| 4 | Reboot, lower value | 5120, boot 7 | 300, boot 8 | 300 |
| 5 | Reboot, higher value, boot count changed | 100, boot 7 | 400, boot 8 | 400 |
| 6 | Midnight crossing | read 23:55 / 6000 | read 00:05 / 6040 | 40 to the new day |
| 7 | Multi-day gap | 6000 | 9000 three days later | 3000 to the reading's day |
| 8 | Clock or time zone change | any | any | Never negative, no change to past rows |
