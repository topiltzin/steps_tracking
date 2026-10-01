# Quickstart: Validating the Step Tracker

Commands assume the Gradle project has been scaffolded (see `tasks.md` once generated).

## Prerequisites

- Android Studio or the Android SDK command-line tools, JDK 17
- An emulator (API 30+ recommended) or a physical phone with USB debugging

## Automated checks

```bash
./gradlew testDebugUnitTest            # StepEngine rules (contracts/step-ingestion.md cases 1–8)
./gradlew connectedDebugAndroidTest    # Room and Compose UI tests, needs a device or emulator
./gradlew lintDebug
```

## Manual scenarios

Emulator step simulation uses *Extended controls → Virtual sensors → Additional sensors → Step counter*, or `adb shell`-based sensor injection where supported. A physical phone is the most reliable for the walking tests.

| # | Scenario | Steps | Expected |
|---|---|---|---|
| 1 | First launch | Install, open, grant permission | Rationale appears before the system prompt. Main screen shows 0 or the current day's value within 2 s (SC-001, SC-002). |
| 2 | Counting | Walk 100 steps, reopen the app | Today's total rises by about 100 (SC-003). |
| 3 | App closed | Walk, force-stop the app, wait for a background sync, reopen | The steps are included (SC-004). |
| 4 | Permission denied | Deny, reopen | Disabled state with a way to re-grant or open settings. |
| 5 | Goal | Set the goal to 500, and try 0, -5 and empty | A valid goal updates progress. Invalid input is rejected with the old goal kept. |
| 6 | Goal reached | Reach the goal | Goal-reached indication appears. |
| 7 | History | Open History after 2+ days of use | Past days show totals and goal status. Missing days show 0. |
| 8 | Reboot | Walk, reboot the phone, walk again, open the app | Total is not lower than before and nothing is double counted. Loss at most one sync interval (SC-005). |
| 9 | Midnight | Change the device clock across midnight, or use a test hook | Steps after midnight go to the new day. |
| 10 | Offline | Enable airplane mode | Everything works (SC-008). |
| 11 | Unsupported | Run on an emulator image without a step counter | Unsupported message, no fake zero. |

See [data-model.md](data-model.md) for stored fields and [contracts/](contracts/) for the expected rules and screen states.
