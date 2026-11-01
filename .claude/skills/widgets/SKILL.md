---
name: widgets
description: The four Glance home-screen widgets (This week, Today, Today strip, Streak) and how to add and test them. Use when changing widgets or their refresh.
---

# Widgets

**Code:** `widget/` — `WidgetData.kt` (data from the repository; habits with "Show on widget" off are excluded), `WidgetBitmaps.kt` (Glance has no Canvas, so rings, marks and strips are drawn into bitmaps with the app's own drawing code), `WeekWidget`, `TodayWidget` (circle = `MarkDoneAction`), `StripWidget`, `StreakWidget` (+ `StreakWidgetConfigActivity`; with no choice it follows the longest current streak), `WidgetUpdater` (refreshes all widgets after any change), `WidgetTheme.kt` (widgets always use the system colors and light or dark mode, never the in-app theme settings).

**Verify:** debug builds include `PinWidgetActivity`:
```sh
adb shell am start -n app.sprout.habits/.PinWidgetActivity --es widget today   # week | today | strip | streak
```
then tap "Add to home screen", screenshot the home screen, tap a circle in the Today widget and check every widget updates (also after `am force-stop`).

**Refresh:** `WidgetUpdater` (every habit, entry and setting change, all dates), `MarkDoneAction` (tap may start the process), `WidgetDayReceiver` (alarm just after midnight), `RescheduleReceiver` (boot, clock, zone, locale, app update), `WeftApp.onConfigurationChanged` (dark mode, per-app language; rings and marks are bitmaps baked in light or dark), `AppLanguage.set` on Android 8–12.

**Smoke:** `python3 tools/verify/smoke.py widget launcher` pins each widget (it clears the launcher's home screen: test emulator only) and checks every change shows within 4 s, cold and killed starts, the midnight rollover (needs `adb root`), dark mode, language, an old-day edit, "Nothing due today" and the long-press widget list. Force stop resets widgets to the loading layout until Weft is opened (Android's rule); the check is that opening redraws them.

**Pitfalls:** an action can start the process, so `MarkDoneAction` refreshes widgets itself; the launcher doesn't run the configure step for pinned widgets.
