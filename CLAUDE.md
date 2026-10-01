# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Greenfield: the repository has no code or commits yet. Update this file once the project is scaffolded (build system, module layout, test commands).

## Purpose

An **Android-only** mobile app that does simple step counting using the phone's own sensor data. There is no iOS target, so no cross-platform framework is needed unless the user explicitly asks for one.

## Domain notes for step tracking on Android

These platform facts shape the architecture and are easy to get wrong:

- **Sensor source:** `Sensor.TYPE_STEP_COUNTER` reports a cumulative count **since the last device reboot**, not per day. Daily steps have to be computed by storing a baseline (e.g. at midnight or on first reading) and subtracting it. Handle reboots: when the counter drops below the stored baseline, the device has restarted.
- `Sensor.TYPE_STEP_DETECTOR` fires one event per step. Use it only for live UI feedback, not as the source of truth, because events are lost while the app isn't listening.
- **Permission:** Android 10 (API 29) and later require the runtime permission `android.permission.ACTIVITY_RECOGNITION` before step sensors return data.
- **Hardware availability:** some devices have no step counter sensor. Check with `SensorManager.getDefaultSensor(...)` and handle `null`.
- **Background counting:** the hardware counter keeps counting while the app is closed, so the app does not need to run all the time. Reading the counter periodically (e.g. with WorkManager) and on app open is usually enough to keep daily totals. A foreground service is only needed for continuous live tracking.
- **Health Connect** is the alternative if the app should read steps from other sources or share data with other apps. It is not needed for counting from the phone's own sensor alone.

## Commands

Not yet defined. Once a Gradle project exists, document the build, lint, unit test, single-test and instrumented-test commands here.
