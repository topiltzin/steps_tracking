# Contract: UI Screens

The app has no external API, so the user-facing contract is the screens and their states.

## Screen: Today (start destination)

Content: today's step count, progress toward the goal (percentage plus a visual ring or bar), a goal-reached indication, an entry to edit the goal (≤ 3 taps, SC-006), and an entry to History (1 tap, SC-007).

| State | Trigger | Display |
|---|---|---|
| Loading | Launch | Skeleton or last cached value, replaced within 2 s |
| Active | Permission granted, sensor present | Steps, progress, goal-reached when steps ≥ goal |
| NeedsPermission (first run) | Permission not yet asked | Rationale text, then the system prompt on button tap |
| PermissionDenied | User denied | Explanation that counting is off, with "Grant" (or "Open settings" when the prompt can no longer be shown) |
| Unsupported | No step counter sensor | Message that the device is not supported, with no step value shown |

## Screen: History

List of the most recent days (at least 7), newest first: date, step total, and goal met or not. Days with no data show 0. Fewer than 7 days of data shows only the available days. Reads from `daily_steps` and fills gaps in memory.

## Dialog: Edit goal

Numeric input, prefilled with the current goal. A valid goal is a positive whole number. Invalid input shows an inline error and keeps the previous goal. Saving updates the Today screen immediately and today's `goal` snapshot.

## Navigation

`Today` ⇄ `History` (back returns to Today). The goal dialog opens over Today.

## Accessibility

Step count and progress have text content descriptions. Goal-reached is conveyed by text and not only by color. Touch targets are at least 48dp.
