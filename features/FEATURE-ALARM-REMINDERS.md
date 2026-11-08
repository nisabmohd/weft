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

## 4. More ways to answer (ringing screen)
Designs: `alarm-4-ringing-measured.png` (Read), `alarm-5-ringing-check-off.png` (Wake up at 7).

- **Open Weft** at the top: a text button (14sp SemiBold, habit `ink`, trailing `alt-arrow-right`). It stops the sound, asks to unlock (`requestDismissKeyguard`), then opens the habit's detail.
- Measured habits show their goal under the name ("Goal 20 pages", 15sp, `ink` at 75%).
- One text button sits between the slide bar and Snooze | Dismiss (48dp tall, 16sp SemiBold, habit `ink`):
  - measured habit: **Log amount**, which opens the amount sheet (below)
  - check-off habit: **Skip today**, which logs Skip, stops the alarm and closes the screen
- Layout from the bottom: the button pair (40dp from the bottom), the text button, then the slide bar.

## 5. Logging an amount from the alarm
Design: `alarm-6-log-amount.png`.

- **Log amount** stops the sound and opens the app's existing amount sheet over the ringing screen. The habit-coloured screen stays behind it under a 45% scrim.
- The sheet is the same component as in the app, with nothing alarm-specific: the habit header ("Read · Today · goal 20 pages"), Done / Partial / Skip, − 15 / 20 +, "pages · 75%", the slider, the note field and Cancel | Save. It can show over the lock screen, so no unlock is needed.
- **Save** logs the amount (Partial below the goal, Done at or above it) and closes the alarm. **Cancel** or a swipe down goes back to the ringing screen, with the sound still stopped and the snooze clock not started.

## 6. Two alarms at the same time
Designs: `alarm-7-two-alarms-1-of-2.png`, `alarm-8-two-alarms-2-of-2.png`.

- When several alarm habits share a time, they ring as one alarm on one screen, one habit at a time, in the habits' order.
- A small chip above the tile shows the position: "1 of 2" (13sp SemiBold, habit `mid` / `ink`). Under the time, "Next: Workout" names the next one (14sp, `ink` at 75%).
- Answering the first (slide, Skip today, Log amount → Save, or Dismiss) shows the next habit straight away with "2 of 2", in its own habit colour, without ringing again. The last screen has no "Next" line.
- **Snooze** snoozes only the habit on screen and moves on to the next one. **Open Weft** opens the app on Today and leaves the rest unanswered (their notifications stay).

## Platform notes
- Use `AlarmManager.setAlarmClock()` (exact, shown in the status bar). On Android 12+ this needs `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`. If it isn't granted, the Alarm row shows "Allow alarms in settings" and opens the system page.
- Android 14+: `USE_FULL_SCREEN_INTENT` must be granted. Otherwise, fall back to a heads-up notification with the same actions.
- Reschedule after boot, time change and app update (the existing reminder receiver).
- Backup/restore: store the reminder type with the reminder (`"type": "notification" | "alarm"`, default notification).

## Out of scope
- Picking a ringtone, and gradual volume. These may come later, in the reminder sheet.

## As built (1.6.0, 2026-11-08)
- **Power or a volume button** silences every alarm that is ringing, like Dismiss on each (confirmed by the user, 2026-11-08).
- **Any log of the habit for today** stops its alarm: Done, Skip and Partial, from Today, a widget or a notification. A habit already partly done before the alarm still rings.
- **Notification reminders** have Done · (+step) · Dismiss, no Snooze; only alarms snooze (see FEATURE-NOTIFICATION-ACTIONS.md).
- **Open and close** fade (no slide), and the screen is see-through until its first frame, so it never flashes the page colour.
- Code: `notify/AlarmRinger.kt` (the queue, `hush` for Log amount, `leave` for Open Weft, the log watcher), `ui/alarm/AlarmActivity.kt`. Every case is a smoke check (`python3 tools/verify/smoke.py alarm`).

