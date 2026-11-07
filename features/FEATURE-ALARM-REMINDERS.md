# Feature: alarm reminders

A habit's reminder can be a quiet **Notification** (today's behaviour) or an **Alarm** that rings full screen until the user acts. This is made for habits like "Wake up at 7".

Designs: `design/alarm-1-new-habit-reminder.png`, `design/alarm-2-reminder-sheet.png`, `design/alarm-3-ringing.png`.

## 1. New habit / Edit habit: Reminder card
- The Reminder section has one card: a habit tile with an alarm icon, the time ("6:30 AM", 16sp SemiBold), a subtitle, and the on/off switch.
- Subtitle: "<Notification|Alarm> · on the days above · tap to change".
- Tapping the card (not the switch) opens the reminder sheet. The switch turns the reminder on or off.
- When the reminder is off, the subtitle is "Off · tap to set a time".

## 2. Reminder sheet
- Title "Reminder", subtitle = habit name.
- The time wheel is the existing reminder time picker (hour · minute in 5s · AM/PM).
- Two `ChoiceRow`s:
  - **Notification**: "A quiet nudge in your notifications"
  - **Alarm**: "Rings full screen until you act"
- One full-width primary button: **Set reminder**.

## 3. Ringing screen
A full-screen activity (`showWhenLocked`, `turnScreenOn`) started by a full-screen-intent notification on the alarm channel.

- Background: habit `soft`. Tile: 64dp, habit `mid` with the `ink` icon. Name: 20sp SemiBold `ink`. Time: 88sp onSurface.
- Slide bar: 64dp, habit `mid` track with a habit `solid` thumb and a white ✓, labelled "Slide to mark done".
- Below it, the button pair: **Snooze 10 min** (secondary) | **Dismiss** (primary).
- Actions:
  - Slide: marks the habit Done for today (same as a tap on Today), stops the alarm, and closes the screen.
  - Snooze: stops the alarm and rings again in 10 minutes. Only one snooze is pending at a time.
  - Dismiss: stops the alarm without logging anything.
  - Power or volume button: silences it, like Dismiss.
- Sound: the default alarm ringtone on the alarm stream, with vibration. It stops by itself after 10 minutes, and a missed-alarm notification is posted ("Wake up at 7 · missed at 6:30").
- If the habit is already done today, the alarm doesn't ring.

## Platform notes
- Use `AlarmManager.setAlarmClock()` (exact, shown in the status bar). On Android 12+ this needs `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`. If it isn't granted, the Alarm row shows "Allow alarms in settings" and opens the system page.
- Android 14+: `USE_FULL_SCREEN_INTENT` must be granted. Otherwise, fall back to a heads-up notification with the same actions.
- Reschedule after boot, time change and app update (the existing reminder receiver).
- Backup/restore: store the reminder type with the reminder (`"type": "notification" | "alarm"`, default notification).

## Out of scope
- Picking a ringtone, and gradual volume. These may come later, in the reminder sheet.
