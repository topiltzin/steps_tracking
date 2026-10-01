# Feature Specification: Simple Step Tracker

**Feature Branch**: `001-step-tracker`
**Created**: 2026-10-01
**Status**: Draft
**Input**: User description: "Lets create a new simple but effective mobile android app to track steps using the cellphone data."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See today's steps (Priority: P1)

A user opens the app and immediately sees how many steps they have taken so far today, using only the movement data their phone already collects. On first launch the app explains why it needs permission to read physical activity and asks for it.

**Why this priority**: This is the core value of the app. With only this story, the app is already a usable step counter.

**Independent Test**: Install the app, grant the permission, walk a known number of steps (e.g., 100), reopen the app, and confirm today's count increased by roughly that amount.

**Acceptance Scenarios**:

1. **Given** the app is installed and permission has been granted, **When** the user opens the app, **Then** today's step count is shown on the main screen within 2 seconds.
2. **Given** the user walked with the app closed, **When** they open the app, **Then** the steps taken while it was closed are included in today's total.
3. **Given** it is the first launch, **When** the app starts, **Then** the user sees a short explanation of why activity permission is needed before the system permission prompt appears.
4. **Given** the user denied the permission, **When** they open the app, **Then** the app explains that counting is disabled and offers a way to grant permission again, including a path to the device settings if the system no longer shows the prompt.
5. **Given** the phone has no step-counting capability, **When** the user opens the app, **Then** the app clearly states that the device is not supported instead of showing a misleading zero.

---

### User Story 2 - Daily goal and progress (Priority: P2)

A user sets a daily step goal (default 10,000) and sees their progress toward it, including when it is reached.

**Why this priority**: A goal turns raw numbers into motivation, but the app is still useful without it.

**Independent Test**: Set a goal of 500 steps, walk until the goal is reached, and confirm the progress display shows 100% and a goal-reached indication.

**Acceptance Scenarios**:

1. **Given** the user has not changed settings, **When** they view the main screen, **Then** progress is shown against a 10,000-step goal.
2. **Given** the user changes the goal to a new positive number, **When** they return to the main screen, **Then** progress reflects the new goal immediately.
3. **Given** today's steps reach or exceed the goal, **When** the user views the main screen, **Then** the goal is shown as achieved.

---

### User Story 3 - Review past days (Priority: P3)

A user reviews a history of daily step totals for at least the past 7 days to see trends and whether they met their goal each day.

**Why this priority**: History adds long-term value, but needs the daily counting from Story 1 to exist first.

**Independent Test**: Use the app for several days (or seed historical data), open the history view, and confirm each day shows the correct total and goal status.

**Acceptance Scenarios**:

1. **Given** the app has recorded steps on previous days, **When** the user opens history, **Then** each past day shows its total and whether the goal was met.
2. **Given** a day with no recorded steps, **When** the user views history, **Then** that day appears with a total of zero rather than being missing.
3. **Given** the user has less than 7 days of data, **When** they open history, **Then** only the available days are shown, with no errors.

---

### Edge Cases

- **Day rollover**: Steps taken after midnight count toward the new day, not the previous one, even if the app was closed at midnight.
- **Phone restart**: When the phone reboots, the running count restarts from zero at the hardware level. Today's total must not drop or double count, and steps taken before the reboot stay in today's total.
- **Permission revoked later**: If the user revokes permission after using the app, existing history is kept and the app shows the disabled state.
- **Time zone or clock change**: Changing the time zone or device clock does not corrupt or duplicate past daily totals.
- **Not opened for several days**: Steps taken during a multi-day gap are not lost, and the app does not attribute all of them to a single day when per-day information is unavailable (see Assumptions).
- **Invalid goal input**: Zero, negative, empty, or non-numeric goals are rejected with a clear message and the previous goal is kept.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST count the user's steps using only the movement data available on the phone itself, with no account, wearable, or external service required.
- **FR-002**: The app MUST display the current day's step total on its main screen.
- **FR-003**: The app MUST keep counting steps taken while the app is closed and include them in the daily total when the user next opens it.
- **FR-004**: The app MUST request the physical activity permission only after explaining its purpose, and MUST function in a clearly communicated disabled state when the permission is denied.
- **FR-005**: The app MUST detect devices without step-counting capability and tell the user the device is unsupported.
- **FR-006**: The app MUST attribute steps to the correct calendar day based on the device's local time, including across midnight.
- **FR-007**: The app MUST produce correct daily totals across phone restarts, without losing or double counting steps.
- **FR-008**: The app MUST let the user set a daily step goal as a positive whole number, defaulting to 10,000, and MUST show progress toward it.
- **FR-009**: The app MUST indicate when the daily goal has been reached.
- **FR-010**: The app MUST store daily totals on the device and show a history of at least the past 7 days, including goal status for each day.
- **FR-011**: The app MUST persist the user's goal and step history across app restarts and phone restarts.
- **FR-012**: The app MUST keep all step data on the device and MUST NOT transmit it off the device.

### Key Entities

- **Daily Step Record**: The step total for one calendar day, with its date and the goal in effect that day, so history stays accurate if the goal changes later.
- **User Settings**: The user's current daily step goal.
- **Step Source Reading**: A reading of the phone's cumulative step count, kept so daily totals can be derived and so phone restarts are detected.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can install the app, grant permission, and see their step count in under 1 minute.
- **SC-002**: The main screen shows today's total within 2 seconds of opening the app.
- **SC-003**: Over a 100-step walking test, the displayed count is within 10% of the actual steps, matching what the phone's own sensor reports.
- **SC-004**: Steps taken while the app was closed appear in the total 100% of the time when the app is opened later the same day.
- **SC-005**: Daily totals are correct in 100% of tested day-rollover and phone-restart scenarios, with no lost or duplicated steps.
- **SC-006**: A user can change their daily goal in 3 taps or fewer from the main screen.
- **SC-007**: History for the past 7 days is visible in 1 tap from the main screen.
- **SC-008**: The app works fully offline, with no network connection required for any feature.

## Assumptions

- The app targets Android phones only. There is no iOS or web version.
- The minimum supported Android version covers the large majority of active devices, and the version is decided during planning.
- Accuracy is the phone's own step sensor accuracy. The app does not try to improve on it.
- Counting uses only the phone's built-in step sensor. Data from other apps, wearables, or health platforms is out of scope.
- Distance, calories, active minutes, and route or GPS tracking are out of scope for this version.
- There are no accounts, cloud sync, backup, sharing, or social features in this version.
- The daily goal applies to all days going forward. Past days keep the goal that was active when they were recorded.
- The app does not need to run continuously. Totals are brought up to date when the app opens and periodically in the background, so a multi-day gap may attribute steps to the day they were captured rather than split them across days.
- History keeps at least 7 days. Longer retention (e.g., unlimited) is acceptable but not required.
- Home-screen widgets and reminder notifications are out of scope for this version.
