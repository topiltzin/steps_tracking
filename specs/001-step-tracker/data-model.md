# Data Model: Simple Step Tracker

## Entities

### DailyStepRecord (table `daily_steps`)

| Field | Type | Notes |
|---|---|---|
| `date` | `LocalDate` (ISO text), **primary key** | Local calendar day on the device |
| `steps` | Int, ≥ 0 | Accumulated steps for that day |
| `goal` | Int, > 0 | Goal in effect for that day (snapshotted). Today's row is updated when the user changes the goal. |

Rules:
- One row per day. A row for today is created on the first reading of the day.
- Days with no readings are shown as 0 in history (FR-010, scenario 3.2). They are filled at read time and not stored.
- Past rows are immutable except through the delta ingestion for the current day.

### StepSnapshot (table `step_snapshot`, single row, `id = 1`)

| Field | Type | Notes |
|---|---|---|
| `lastCounter` | Long | Last raw cumulative sensor value processed |
| `lastBootCount` | Int | Device boot count at the last reading, used to detect reboots |
| `lastReadAt` | Instant (epoch millis) | Time of the last reading |

Rules:
- Updated in the same transaction as the `DailyStepRecord` change.
- Absent on first run. The first reading only creates the snapshot with **no steps credited** (the hardware value includes steps from before the app was installed).

### UserSettings (DataStore key `daily_goal`)

| Field | Type | Notes |
|---|---|---|
| `dailyGoal` | Int, > 0 | Default 10,000. Zero, negative and non-numeric input is rejected and the previous goal is kept. |

## Relationships

`StepSnapshot` (1) feeds the delta that is added to one `DailyStepRecord` per reading. `UserSettings.dailyGoal` is copied into each new `DailyStepRecord.goal`.

## State Transitions (ingestion)

```text
no snapshot ──first reading──▶ snapshot saved, 0 steps credited
snapshot ──reading (same boot, C ≥ P)──▶ credit C − P to today's day
snapshot ──reading (boot changed, or C < P)──▶ credit C to today's day
any ──new local date──▶ create new DailyStepRecord (steps = 0, goal = current goal), then credit
```

The exact algorithm and edge cases are in [contracts/step-ingestion.md](contracts/step-ingestion.md).
