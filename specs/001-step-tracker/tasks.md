# Tasks: Simple Step Tracker

**Input**: Design documents in `specs/001-step-tracker/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)
**Prerequisites**: plan.md, spec.md

**Tests**: The spec does not request TDD. Only tests that the contracts mark as required are included: the `StepEngine` cases in `contracts/step-ingestion.md`, plus unit tests for the two other pure-logic pieces (goal validation, history gap filling) and one Room test for atomic ingestion.

**Format**: `- [ ] T### [P?] [Story?] Description with file path`. `[P]` means it can run in parallel (different files, no unfinished dependency).

**Conventions**: The package is `app.steptracker`. `SRC` below means `app/src/main/java/app/steptracker`, `TEST` means `app/src/test/java/app/steptracker`, `ATEST` means `app/src/androidTest/java/app/steptracker`.

---

## Phase 1: Setup (Project Initialization)

- [X] T001 Create Gradle root files `settings.gradle.kts` (single module `:app`, project name `steps_tracking`), root `build.gradle.kts`, `gradle.properties`, and the version catalog `gradle/libs.versions.toml` with current stable versions of: Android Gradle Plugin, Kotlin, KSP, Compose BOM, Material 3, Navigation Compose, Lifecycle (viewmodel-compose, runtime-compose), Room (runtime, ktx, compiler), WorkManager, DataStore Preferences, kotlinx-coroutines (android + test), JUnit 4, AndroidX test (junit, runner), Compose UI test. Generate the Gradle wrapper (`gradle wrapper`).
- [X] T002 Create `app/build.gradle.kts`: namespace and applicationId `app.steptracker`, `minSdk = 26`, `targetSdk` and `compileSdk` = latest stable, Java/Kotlin toolchain 17, Compose enabled, KSP for the Room compiler, Room schema export directory `app/schemas`, dependencies from the catalog.
- [X] T003 Create `app/src/main/AndroidManifest.xml`: `<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>`, `<uses-feature android:name="android.hardware.sensor.stepcounter" android:required="false"/>`, **no INTERNET permission**, `android:allowBackup="false"`, `StepApp` as the application class, `MainActivity` as the launcher activity.
- [X] T004 [P] Create `.gitignore` for Android/Gradle (`build/`, `.gradle/`, `local.properties`, `.idea/`, `*.iml`, `captures/`); keep `app/schemas/` tracked.
- [X] T005 [P] Create the Material 3 theme in `SRC/ui/theme/Theme.kt`, `Color.kt` and `Type.kt` (light and dark, dynamic color on API 31+).
- [X] T006 [P] Add `app/src/main/res/values/strings.xml` with the app name and all user-facing strings used by later tasks (rationale text, denied text, unsupported text, goal-reached text, goal error text, history labels), and a launcher icon in `res/mipmap-*`.
- [X] T007 Verify the skeleton builds: run `./gradlew assembleDebug` and fix any configuration errors.

---

## Phase 2: Foundational (Blocks All User Stories)

**⚠️ No user story work can begin until this phase is complete.**

- [X] T008 [P] Create domain models in `SRC/domain/Models.kt`: `Reading(counter: Long, bootCount: Int, at: Instant)`, `StepSnapshot(lastCounter: Long, lastBootCount: Int, lastReadAt: Instant)`, `IngestResult(newSnapshot: StepSnapshot, creditedSteps: Int, creditedDate: LocalDate)`, `DailyStepRecord(date: LocalDate, steps: Int, goal: Int)`. Constraints (verbatim from data-model.md): `steps` "Int, ≥ 0", `goal` "Int, > 0".
- [X] T009 [P] Create Room entities in `SRC/data/db/Entities.kt`: `DailyStepEntity` (table `daily_steps`; `date` ISO-text primary key, `steps` Int ≥ 0, `goal` Int > 0) and `StepSnapshotEntity` (table `step_snapshot`; `id` fixed to 1 as primary key, `lastCounter` Long, `lastBootCount` Int, `lastReadAt` epoch-millis Long), plus `Converters.kt` for `LocalDate` ↔ ISO text if needed.
- [X] T010 Create `SRC/data/db/StepDao.kt` and `SRC/data/db/StepDatabase.kt` (version 1, `exportSchema = true`). The DAO provides: `observeDay(date): Flow<DailyStepEntity?>`, `observeRecent(limit): Flow<List<DailyStepEntity>>`, `getSnapshot()`, `getDay(date)`, upserts, and a `@Transaction` method `applyIngest(date, creditedSteps, goal, newSnapshot)` that creates the day row if missing (steps = 0, goal = given goal), adds `creditedSteps` to it, and saves the snapshot in one transaction. Depends on T009.
- [X] T011 [P] Create `SRC/data/SettingsStore.kt` over DataStore Preferences: key `daily_goal`, default 10000, `val goal: Flow<Int>`, `suspend fun setGoal(value: Int)`. The store only persists; validation lives in the domain (T025).
- [X] T012 Create `SRC/AppContainer.kt` (manual DI: builds the database, DAO, `SettingsStore`, and later the repository and sensor source) and `SRC/StepApp.kt` (Application subclass that holds the container). Depends on T010 and T011.
- [X] T013 Create `SRC/MainActivity.kt` with a Compose `NavHost` and routes `today` (start) and `history`, using temporary placeholder composables, wrapped in the theme from T005. Depends on T005 and T012.

**Checkpoint**: The app builds and launches to a placeholder screen. Stories can start.

---

## Phase 3: User Story 1 - See today's steps (Priority: P1) 🎯 MVP

**Goal**: The app shows today's step total from the phone's sensor, keeps counting while closed, handles permission denial and unsupported devices, and survives reboots and midnight.

**Independent Test**: Run quickstart scenarios 1–4, 8, 9 and 11. Run `./gradlew testDebugUnitTest` and confirm the 8 ingestion cases pass.

### Tests for User Story 1

- [X] T014 [P] [US1] Write `TEST/domain/StepEngineTest.kt` covering the 8 cases in `contracts/step-ingestion.md`: (1) first run credits 0, (2) normal 5000→5120 credits 120, (3) duplicate reading credits 0, (4) reboot with lower value 5120/boot 7 → 300/boot 8 credits 300, (5) reboot with higher value 100/boot 7 → 400/boot 8 credits 400, (6) midnight crossing credits 40 to the new day, (7) multi-day gap credits 3000 to the reading's day, (8) clock/time zone change never yields a negative credit. Also assert that applying the same reading twice credits 0 the second time (idempotent). These tests must fail before T015.
- [X] T015 [P] [US1] Write `ATEST/data/db/StepDaoTest.kt` (in-memory Room): `applyIngest` creates the day row with the given goal on first use, adds credits across two calls, and updates the snapshot in the same transaction; a second day gets its own row.

### Implementation for User Story 1

- [X] T016 [US1] Implement `SRC/domain/StepEngine.kt` with `fun applyReading(snapshot: StepSnapshot?, reading: Reading, today: LocalDate): IngestResult` following the rules in `contracts/step-ingestion.md`: null snapshot → credit 0; `reading.bootCount != snapshot.lastBootCount` or `reading.counter < snapshot.lastCounter` → credit `reading.counter`; otherwise credit `reading.counter − snapshot.lastCounter`; never negative; credited to `today`. Pure Kotlin with no Android imports. Makes T014 pass.
- [X] T017 [P] [US1] Implement `SRC/data/StepSensorSource.kt`: `fun isSupported(): Boolean` (true when `SensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null`) and `suspend fun read(): Reading?` that registers a listener, awaits the first event (with a timeout of a few seconds), unregisters, and returns the counter, `Settings.Global.BOOT_COUNT` and `Instant.now()`. Returns null on timeout or when unsupported. Uses `suspendCancellableCoroutine` and always unregisters.
- [X] T018 [US1] Implement `SRC/data/StepRepository.kt`: `suspend fun syncNow()` (read the sensor, load the snapshot, call `StepEngine.applyReading` with `LocalDate.now()` in the device's zone, then call `StepDao.applyIngest` with the current goal), `fun observeToday(): Flow<DailyStepRecord?>`, `val isSupported: Boolean`. A failed or null read does nothing. Register it in `AppContainer`. Depends on T010, T016 and T017.
- [X] T019 [US1] Implement `SRC/work/StepSyncWorker.kt` (`CoroutineWorker` calling `repository.syncNow()`; return `Result.success()` even when the permission is missing) and `SRC/work/SyncScheduler.kt`: `schedulePeriodic()` (unique periodic work, 15-minute interval, `KEEP` policy) and `scheduleNextMidnight()` (unique one-time work with an initial delay until 1 minute after the next local midnight; the worker re-schedules it when it runs). Call both from `StepApp.onCreate`. Depends on T018.
- [X] T020 [P] [US1] Implement permission state in `SRC/ui/permission/PermissionState.kt`: a helper that returns one of `NotRequired` (API < 29), `Granted`, `NeedsRationale` (never asked), `Denied`. Also provide `fun openAppSettingsIntent(context): Intent`. Uses `ContextCompat.checkSelfPermission` and `shouldShowRequestPermissionRationale`, and remembers "already asked" in the ViewModel or a saved flag.
- [X] T021 [US1] Implement `SRC/ui/today/TodayViewModel.kt` exposing `StateFlow<TodayUiState>` with the states `Loading`, `Active(steps, goal)`, `NeedsPermission`, `PermissionDenied(canPrompt)`, `Unsupported`, as in `contracts/ui-screens.md`. Unsupported takes priority over the permission states. Trigger `syncNow()` when the screen resumes and after permission is granted. Depends on T018 and T020.
- [X] T022 [US1] Implement `SRC/ui/today/TodayScreen.kt`: shows the step count large and centered for `Active`, the rationale text with a "Continue" button that launches the system permission request for `NeedsPermission`, the denied explanation with "Grant permission" or "Open settings" (depending on `canPrompt`) for `PermissionDenied`, and the unsupported message with no step value for `Unsupported`. Use strings from T006. Depends on T021.
- [X] T023 [US1] Replace the placeholder `today` route in `SRC/MainActivity.kt` with `TodayScreen`, and hook the activity-result permission launcher to the ViewModel. Depends on T022.

**Checkpoint**: User Story 1 works on its own. This is the MVP.

---

## Phase 4: User Story 2 - Daily goal and progress (Priority: P2)

**Goal**: The user sets a daily goal (default 10,000) and sees progress and a goal-reached indication.

**Independent Test**: Quickstart scenarios 5 and 6. `./gradlew testDebugUnitTest` passes `GoalValidatorTest`.

- [X] T024 [P] [US2] Write `TEST/domain/GoalValidatorTest.kt`: accepts `"500"` and `"10000"`; rejects `""`, `"0"`, `"-5"`, `"abc"`, `"12.5"` and values that overflow `Int`. Must fail before T025.
- [X] T025 [P] [US2] Implement `SRC/domain/GoalValidator.kt`: `fun parse(input: String): Int?` returning a positive whole number or null, matching "Zero, negative, empty, or non-numeric goals are rejected". Makes T024 pass.
- [X] T026 [US2] Extend `SRC/data/StepRepository.kt`: `val goal: Flow<Int>` from `SettingsStore`, and `suspend fun updateGoal(value: Int)` that saves the setting and updates today's `daily_steps.goal` (creating today's row with 0 steps if it does not exist). Past rows are never touched. Add the matching `StepDao` query in `SRC/data/db/StepDao.kt`. Depends on T018.
- [X] T027 [US2] Update `SRC/ui/today/TodayViewModel.kt` so `Active` carries `goal` and progress (`steps / goal`, capped at 100% for display), and expose `saveGoal(input: String): GoalResult` (`Saved` or `Invalid`) using `GoalValidator`. Depends on T025 and T026.
- [X] T028 [P] [US2] Create `SRC/ui/goal/GoalDialog.kt`: numeric text field prefilled with the current goal, inline error text from `strings.xml` on invalid input (previous goal is kept), Save and Cancel buttons.
- [X] T029 [US2] Update `SRC/ui/today/TodayScreen.kt`: add the progress indicator (ring or bar) with a text percentage and "steps of goal", a "Goal reached" text label (not color alone) when steps ≥ goal, and a goal edit action that opens `GoalDialog`. Reachable in 3 taps or fewer from the main screen (SC-006). Depends on T027 and T028.

**Checkpoint**: Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 - Review past days (Priority: P3)

**Goal**: The user sees the last 7+ days with totals and goal status.

**Independent Test**: Quickstart scenario 7. `./gradlew testDebugUnitTest` passes `HistoryBuilderTest`.

- [X] T030 [P] [US3] Write `TEST/domain/HistoryBuilderTest.kt`: given stored records and `today`, the result has exactly 7 days (or the number of days since the first record if fewer), newest first, missing days filled with 0 steps and the current goal, and `goalMet = steps >= goal`. With no records at all, the result is empty or a single zero day for today. Must fail before T031.
- [X] T031 [P] [US3] Implement `SRC/domain/HistoryBuilder.kt`: `fun build(records: List<DailyStepRecord>, today: LocalDate, currentGoal: Int, days: Int = 7): List<HistoryDay>` where `HistoryDay(date, steps, goal, goalMet)`. Makes T030 pass.
- [X] T032 [US3] Add `observeHistory(): Flow<List<HistoryDay>>` to `SRC/data/StepRepository.kt` (combines `StepDao.observeRecent(limit = 30)`, the current goal and today's date). Depends on T031.
- [X] T033 [P] [US3] Implement `SRC/ui/history/HistoryViewModel.kt` exposing `StateFlow<List<HistoryDay>>` from the repository.
- [X] T034 [US3] Implement `SRC/ui/history/HistoryScreen.kt`: a lazy list (newest first) with date, step total, and a "Goal met" or "Goal not met" text for each row; top app bar with back navigation. Depends on T033.
- [X] T035 [US3] Replace the placeholder `history` route in `SRC/MainActivity.kt` with `HistoryScreen`, and add a one-tap History action to `SRC/ui/today/TodayScreen.kt` (SC-007). Depends on T034.

**Checkpoint**: All three user stories work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T036 [P] Accessibility pass on `SRC/ui/today/TodayScreen.kt`, `SRC/ui/history/HistoryScreen.kt` and `SRC/ui/goal/GoalDialog.kt`: content descriptions on the step count and progress, touch targets of at least 48dp, goal-reached and goal-met conveyed by text (per `contracts/ui-screens.md`).
- [X] T037 [P] Write `ATEST/ui/TodayScreenTest.kt` (Compose UI test with a fake ViewModel state) covering the five Today states from `contracts/ui-screens.md`.
- [X] T038 Confirm privacy and permissions in the merged manifest: run `./gradlew :app:processDebugMainManifest` and check that the only permission is `ACTIVITY_RECOGNITION` and that there is no `INTERNET` (FR-012, SC-008). (Result: no INTERNET; the merged manifest also contains WorkManager's own WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED and FOREGROUND_SERVICE, which are library-added and do not give the app network access.)
- [X] T039 Run `./gradlew lintDebug testDebugUnitTest` and fix every error and warning in the new code.
- [ ] T040 Run `./gradlew connectedDebugAndroidTest` on an emulator or device and fix failures.
- [ ] T041 Execute all 11 manual scenarios in `specs/001-step-tracker/quickstart.md` and record any deviation from the success criteria.
- [X] T042 Update `CLAUDE.md` (project status and the **Commands** section) with the real build, lint, unit test, single test (`./gradlew testDebugUnitTest --tests "app.steptracker.domain.StepEngineTest"`), and instrumented test commands, plus the module layout from `plan.md`.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies. T007 needs T001–T003.
- **Foundational (Phase 2)**: needs Setup. It blocks all user stories.
- **User Stories (Phases 3–5)**: need Foundational. US2 and US3 extend `StepRepository`, `TodayViewModel` and `TodayScreen`, which US1 creates, so build **US1 first**. After US1, US2 and US3 can proceed in parallel (US3 touches only the repository and `MainActivity` in places that do not conflict with US2, apart from the small `TodayScreen` edit in T035, which should come after T029).
- **Polish (Phase 6)**: needs the stories you intend to ship.

### Within each story

- Tests are written first and must fail, then the implementation: models, then logic, then repository, then ViewModel, then UI.

### Dependency graph

```text
Setup ──▶ Foundational ──▶ US1 (P1, MVP) ──┬──▶ US2 (P2) ──┐
                                           └──▶ US3 (P3) ──┴──▶ Polish
```

## Parallel Opportunities

- **Setup**: T004, T005, T006 together after T001–T003.
- **Foundational**: T008, T009, T011 together. T010 follows T009, and T012 follows T010 and T011.
- **US1**: T014, T015, T017, T020 together (different files). Then T016 → T018 → T019/T021 → T022 → T023.
- **US2**: T024, T025, T028 together.
- **US3**: T030, T031, T033 together.
- **Polish**: T036 and T037 together.

### Parallel example: User Story 1

```text
Run together: T014 (StepEngineTest), T015 (StepDaoTest), T017 (StepSensorSource), T020 (PermissionState)
Then: T016 (StepEngine) → T018 (StepRepository) → T019 (Worker) and T021 (ViewModel) → T022 (Screen) → T023 (wire up)
```

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 (Setup) and Phase 2 (Foundational).
2. Phase 3 (US1).
3. **Stop and validate** with quickstart scenarios 1–4, 8, 9 and 11. This is a usable step counter.

### Incremental delivery

1. Add US2 (goal and progress) → validate scenarios 5 and 6.
2. Add US3 (history) → validate scenario 7.
3. Polish, then the full manual pass (T041).

## Notes

- Each task names the file it touches. Commit after each task or logical group.
- Keep `StepEngine`, `GoalValidator` and `HistoryBuilder` free of Android imports so they stay JVM-testable.
- Do not add a foreground service, an exact-alarm permission or any network code (see `plan.md`).
