# Implementation Plan: Simple Step Tracker

**Branch**: `main` (no feature branch created) | **Date**: 2026-10-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-step-tracker/spec.md`

## Summary

A single-module, Android-only app that reads the phone's hardware cumulative step counter, converts it into per-day totals by accumulating **deltas** (so phone reboots and day rollovers are handled by design), and stores them in a local database. The UI shows today's steps and goal progress, and a 7-day history. A periodic background job reads the counter while the app is closed so totals stay current. The app is fully offline, with no accounts and no network access.

## Technical Context

**Language/Version**: Kotlin (current stable at scaffold time), JVM 17 toolchain
**Primary Dependencies**: Jetpack Compose + Material 3 (UI), Room (storage), WorkManager (background reads), DataStore Preferences (goal), Kotlin Coroutines/Flow. No DI framework; manual constructor injection through one `AppContainer`.
**Storage**: Room (SQLite) for daily records and the last-reading snapshot, DataStore for the goal setting
**Testing**: JUnit 4 and kotlinx-coroutines-test for the step engine (pure Kotlin, no Android types). Room in-memory tests and Compose UI tests as instrumented tests.
**Target Platform**: Android, `minSdk 26` (Android 8.0), `targetSdk` = latest stable at scaffold time
**Project Type**: Mobile app, single Gradle module (`app`)
**Performance Goals**: Main screen shows today's total within 2 s of launch (SC-002). Background read completes in under 1 s.
**Constraints**: Offline only. No foreground service and no exact-alarm permission. Only permissions are `ACTIVITY_RECOGNITION` (runtime, API 29+) and none beyond that.
**Scale/Scope**: One user, 3 screens (Today, History, Goal dialog). About 365 rows per year of history.

## Constitution Check

No constitution exists (`.specify/memory/constitution.md` is absent), so there are no gates to evaluate. Self-imposed principles for this plan:

| Principle | Status |
|---|---|
| Simplicity: one module, no DI framework, no network layer | Pass |
| Privacy: no `INTERNET` permission, no analytics, data stays on the device | Pass (FR-012) |
| Testability: counting logic is a pure function, separated from the Android sensor APIs | Pass |

Re-evaluated after Phase 1: still passing.

## Project Structure

### Documentation (this feature)

```text
specs/001-step-tracker/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── step-ingestion.md   # core domain contract (counter reading → daily totals)
│   └── ui-screens.md       # screen/state contract
└── checklists/requirements.md
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/<package>/
    │       ├── StepApp.kt                 # Application, builds AppContainer, schedules worker
    │       ├── MainActivity.kt
    │       ├── domain/
    │       │   ├── StepEngine.kt          # pure: applyReading(state, reading) -> state/changes
    │       │   └── Models.kt
    │       ├── data/
    │       │   ├── db/                    # Room: entities, DAO, database
    │       │   ├── SettingsStore.kt       # DataStore goal
    │       │   ├── StepSensorSource.kt    # one-shot read of TYPE_STEP_COUNTER
    │       │   └── StepRepository.kt
    │       ├── work/
    │       │   └── StepSyncWorker.kt      # periodic read, ~15 min
    │       └── ui/
    │           ├── today/  history/  goal/  permission/
    │           └── theme/
    ├── test/                              # StepEngine unit tests
    └── androidTest/                       # Room + Compose UI tests
gradle/libs.versions.toml
settings.gradle.kts
build.gradle.kts
```

**Structure Decision**: One `app` module with `domain` / `data` / `work` / `ui` packages. Splitting into modules would add build overhead with no benefit at this size.

## Key Design Decisions

1. **Accumulate deltas, not baselines.** Each reading `C` is compared with the previously stored reading `P`. `delta = C − P` if `C ≥ P`, otherwise `delta = C` (the counter restarted at a reboot). The delta is added to the local day of the reading. This is robust to reboots and avoids storing a midnight baseline. See [contracts/step-ingestion.md](contracts/step-ingestion.md).
2. **Reads happen in three places:** on app open/resume, in a periodic WorkManager job (15 min, the platform minimum), and on a one-time job aimed at shortly after local midnight to tighten day boundaries.
3. **No live sensor listener for the source of truth.** The UI re-reads on resume and shows Room data via Flow. Step Detector is not used.
4. **Day attribution is by reading time.** Steps are credited to the day in which they are read, so a boundary error of up to one sync interval is possible. Accepted, and documented in the spec Assumptions.
5. **Goal per day is snapshotted** into each day record, so changing the goal never rewrites history.

## Risks & Open Items

- **Steps lost around a reboot:** steps taken after the last read and before shutdown cannot be read after the counter resets. The loss is bounded by the sync interval (about 15 minutes). SC-005 is therefore tested as "no double counting and no loss beyond one sync interval". See research R4.
- **OEM battery optimizations** can delay WorkManager jobs. Opening the app always catches up, so totals stay correct, with only boundary attribution affected.
- **Unsupported devices**: no step counter sensor means a dedicated unsupported screen (FR-005).
