# Research: Simple Step Tracker

No unresolved `NEEDS CLARIFICATION` items remained after specification. These are the technology and design decisions made for planning.

## R1: Language and UI toolkit
- **Decision**: Kotlin with Jetpack Compose (Material 3).
- **Rationale**: This is the standard native Android stack. It gives the smallest UI code for three simple screens and the best fit with the Android-only scope.
- **Alternatives**: Java and XML views (more boilerplate). Flutter or React Native (cross-platform not needed, and an extra runtime for no gain).

## R2: Step data source
- **Decision**: `Sensor.TYPE_STEP_COUNTER` read as a one-shot value, with delta accumulation.
- **Rationale**: The hardware counter keeps counting while the app is closed and is the only source that does not lose steps. Step Detector loses events when no listener is registered.
- **Alternatives**: Step Detector (lossy). Health Connect (adds a dependency and permissions, and the spec scopes it out).

## R3: Background reads
- **Decision**: WorkManager periodic work (15 min) plus a one-time job near midnight, and a read on app open.
- **Rationale**: It survives reboots and process death, needs no foreground service or exact-alarm permission, and its battery cost is minimal.
- **Alternatives**: Foreground service (persistent notification, only needed for live tracking). AlarmManager exact alarms (extra permission, needs rescheduling on boot).

## R4: Reboot handling
- **Decision**: If the new reading is lower than the stored one, treat the new reading as the steps since boot. No `BOOT_COMPLETED` receiver.
- **Rationale**: The counter resets to 0 at boot, so the first reading afterward equals the steps since boot. Steps between the last read and shutdown are unrecoverable, because the value is gone once the counter resets. The loss is bounded by the sync interval.
- **Caveat**: A reboot followed by enough steps that the new reading already exceeds the old one before any read would be misread as a continuation. It is rare, because the old reading would have to be passed within one sync interval. Mitigation: also persist the device boot count (`Settings.Global.BOOT_COUNT`, API 24+) with each reading and treat a changed boot count as a reboot regardless of values.
- **Alternatives**: Shutdown broadcast (not reliably delivered).

## R5: Local storage
- **Decision**: Room for daily records and the last-reading snapshot, DataStore Preferences for the goal.
- **Rationale**: Room gives transactional updates (the delta and snapshot must change atomically) and Flow-based queries for the UI. DataStore is the simplest fit for one preference.
- **Alternatives**: Everything in DataStore (no transactions across multiple records). Raw SQLite (more code).

## R6: Permission flow
- **Decision**: `ACTIVITY_RECOGNITION` runtime permission with an in-app rationale screen first. If the system stops showing the prompt (permanently denied), link to the app's settings page.
- **Rationale**: It matches spec acceptance scenarios 1.3 and 1.4. On Android 8 and 9 no runtime permission is needed, and the code skips the request.

## R7: Minimum SDK
- **Decision**: `minSdk 26`.
- **Rationale**: It covers nearly all active devices, gives the modern WorkManager and Compose baseline, and is above `BOOT_COUNT` (API 24). The permission logic only branches at API 29.
- **Alternatives**: `minSdk 29` (simpler permission code but excludes some devices). This can be raised later without design changes.

## R8: Dependency injection
- **Decision**: Manual `AppContainer` created in the `Application` class.
- **Rationale**: Under 10 objects to wire. Hilt would add build complexity for no benefit.
