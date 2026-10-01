# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Implemented from `specs/001-step-tracker/` (spec, plan, tasks). Debug build, lint and JVM unit tests pass. Instrumented tests and the manual scenarios (T040, T041) have not been run: no emulator or device is set up yet.

## Purpose

An **Android-only** mobile app that does simple step counting using the phone's own sensor data. There is no iOS target, so no cross-platform framework is needed unless the user explicitly asks for one.

## Module layout

Single Gradle module `:app`, package `app.steptracker` (Kotlin, Jetpack Compose + Material 3, Room, WorkManager, DataStore; manual DI through `AppContainer`).

- `domain/`: pure Kotlin, no Android imports. `StepEngine` (delta ingestion), `GoalValidator`, `HistoryBuilder`, `Models`.
- `data/`: `StepSensorSource` (one-shot read of `TYPE_STEP_COUNTER`), `StepRepository`, `SettingsStore` (DataStore goal), `db/` (Room entities, `StepDao`, `StepDatabase`).
- `work/`: `StepSyncWorker`, `SyncScheduler` (15-minute periodic read plus a one-time read after local midnight).
- `ui/`: `today/`, `history/`, `goal/`, `permission/`, `theme/`. `TodayScreen` is stateless; `TodayRoute` wires the ViewModel and permission launcher.
- Tests: `app/src/test` (JVM: StepEngine, GoalValidator, HistoryBuilder), `app/src/androidTest` (Room DAO, Compose UI).
- Dependency versions live in `gradle/libs.versions.toml`. Room schemas are exported to `app/schemas/` and stay tracked.

## Environment

- Use the JDK bundled with Android Studio: `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` (the build targets Java 17 bytecode, no toolchain download needed).
- The Android SDK must be installed and located through `ANDROID_HOME` or `sdk.dir` in `local.properties` (git-ignored).

## Commands

Run from the repo root (`./gradlew` on Git Bash, `.\gradlew.bat` on PowerShell):

```text
./gradlew assembleDebug                  # build the debug APK
./gradlew lintDebug                      # Android lint
./gradlew testDebugUnitTest              # JVM unit tests
./gradlew testDebugUnitTest --tests "app.steptracker.domain.StepEngineTest"        # one test class
./gradlew connectedDebugAndroidTest      # Room + Compose UI tests, needs an emulator or device
./gradlew :app:processDebugMainManifest  # inspect the merged manifest (only ACTIVITY_RECOGNITION, no INTERNET)
```

## Domain notes for step tracking on Android

These platform facts shape the architecture and are easy to get wrong:

- **Sensor source:** `Sensor.TYPE_STEP_COUNTER` reports a cumulative count **since the last device reboot**, not per day. The app accumulates **deltas** between readings (`StepEngine`) instead of storing a midnight baseline. A reboot is detected by a changed `Settings.Global.BOOT_COUNT` or a counter lower than the last reading.
- The first reading ever only creates the snapshot and credits 0 steps (the hardware value includes steps from before install).
- `Sensor.TYPE_STEP_DETECTOR` is not used. Events are lost while the app isn't listening.
- **Permission:** Android 10 (API 29) and later require the runtime permission `android.permission.ACTIVITY_RECOGNITION` before step sensors return data. `PermissionChecker` keeps an "already asked" flag so first-run and permanently-denied states can be told apart.
- **Hardware availability:** some devices have no step counter sensor. `StepSensorSource.isSupported()` handles `null`, and the Today screen shows an Unsupported state with priority over the permission states.
- **Background counting:** the hardware counter keeps counting while the app is closed. WorkManager reads it periodically and the app re-reads on resume. No foreground service, no exact alarms, no `INTERNET` permission.
- **Health Connect** is not used. It would only be needed to read steps from other sources or share data with other apps.
