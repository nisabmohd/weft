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

## 7. Set up alarms sheet (`alarm-9a/b/c-setup-sheet-*.png`)
One sheet lists everything an alarm needs on this phone. It's built like the app's other sheets: a plain list, with no cards or tiles.

- Title "Set up alarms" (sheet title, no subtitle, like Backup frequency). The switches show the state.
- Rows are full width with the same padding and type as `ChoiceRow` (24dp sides, 8dp vertical, cardTitle + supporting, about 53dp tall). Only rows this Android version needs are shown:
  - Notifications (13+): "So the alarm can show"
  - Alarms & reminders (exact alarms, 12+): "So it rings on the minute"
  - Full-screen alarms (14+): "So it fills the screen when locked"
- Trailing: the app's switch, the same as Wi-Fi only. **On** = allowed, **off** = missing. Tapping an off switch (or the row) opens that system page. On return (`ON_RESUME`), re-check every row and flip the switch to match. Tapping an on switch also opens the page, since Android doesn't let apps revoke permissions; the switch only reflects the real state.
- **Show over other apps** (optional, `SYSTEM_ALERT_WINDOW`): a switch row right after the required rows, reading "Show over other apps" with the subtitle "Optional · helps on some phones".
  - Shown only on phones that block the full-screen alarm (OnePlus / ColorOS seen), together with the phone-maker rows.
  - A switch, because Android can report it (`Settings.canDrawOverlays`). On = allowed, off = not. Tapping opens `ACTION_MANAGE_OVERLAY_PERMISSION`, and the switch updates on return.
  - Never counted in "N things to allow", never blocks **Done** (the sheet says Done with it off), and never triggers the Reminder card warning.
  - When on and the phone is locked, Weft opens the ringing screen itself if the full-screen intent was blocked. Play reviews this permission, and Android 15+ limits it, so it stays optional.
- Under them, with no label, the phone-maker rows. They're shown only on phones whose maker adds its own switches (ColorOS / OnePlus / Oppo / Realme, Xiaomi / HyperOS, Vivo, Huawei, Samsung battery) and hidden on Pixel and similar:
  - "Lock screen & pop-ups": "Check once in phone settings"
  - "Battery": "Check "Don't restrict" once"
  - Trailing muted `arrow-right-up` (opens the maker's page), never a switch, because Android can't report these.
- Bottom `WeftButton`, full width, 20dp from the list: secondary **Use a notification instead** while a required row is missing (it switches this reminder to Notification and closes the sheet), primary **Done** when all set.
- Opens from three places: picking Alarm in the Reminder sheet when something is missing; the Reminder card warning; More → Alarms rows.

## 8. Reminder card warning (`alarm-10-reminder-card-warning.png`)
- If a habit's reminder is an Alarm and a required permission is missing, the card's second line turns error colour: "Alarm · can't ring full screen · tap to fix". Name the most important missing piece: notifications > exact alarms > full screen.
- Tapping the card opens Set up alarms. Once fixed, the line goes back to "Alarm · on the days above · tap to change".

## 9. Alarm sound sheet (`alarm-11-sound-sheet.png`, `ChoiceSheet`)
- Title "Alarm sound". `ChoiceRow`s:
  - **Weft** (default)
  - **Weft · Soft** ("A gentler tone", if we ship it)
  - **Phone's alarm sound** (subtitle: the phone's alarm sound name)
- Below the list, the same extra-action card as "Help translate Weft": **Choose from phone** ("Any sound on your phone", primaryContainer tile with `music-note-2`, trailing `arrow-right-up`). It opens Android's sound picker. Once a sound is picked, it appears as a fourth ChoiceRow with its name.
- Tapping a row selects it and plays about 4 seconds on the alarm stream. The row's subtitle shows `volume-loud` + "Playing" in primary. Swiping the sheet away, or Back, stops playback. There's no Done button, as in the other ChoiceSheets.
- **Vibrate** isn't in this sheet. It's a switch on the Alarms screen (below).

## 10. More → Alarms (`more-alarms-row.png`, `more-alarms-screen.png`)
- New row in **General**, between Default reminder and Notifications: "Alarms", with the subtitle "All set" (muted) or "2 things to allow" (error colour), and a chevron.
- Decided: it opens a **screen**, built like Backup & restore:
  - a status card (44dp tile with `danger-circle` on errorContainer, "2 things to allow", "Alarms may not ring on time"; when all set, a `check` tile on surfaceContainerHigh with "All set")
  - **Needed**: the same three rows as grouped white cards (24/6 corners, 2dp gaps), with the same switches
  - **On some phones**: Show over other apps (optional switch), then the two phone-maker rows, with ↗
  - **Sound**: "Alarm sound" with the value "Weft" and a chevron (opens the sound sheet), and "Vibrate" ("Along with the sound") with a switch, on by default
- The row and screen always show, even with no alarm habits, so people can set things up ahead of time.

## 11. More → Notifications (`more-notifications-row.png`, `notifications-setup-sheet.png`)
- The General row "Notifications" now works like "Alarms": a subtitle of "All set" (muted) or "1 thing to allow" (error colour), and a chevron. The old trailing value "System settings" is gone.
- It opens `NotificationSetupSheet`, the same pieces as Set up alarms:
  - title "Notifications"
  - **Notifications** switch ("So reminders can show")
  - **Alarms & reminders** switch ("So reminders come on the minute"). Both show the real state and update on return.
  - **Battery** ↗ ("Check "Don't restrict" once"), only on phone makers that need it
  - **Sound and style** ↗ ("Android's notification settings", opens `ACTION_APP_NOTIFICATION_SETTINGS`)
  - primary **Done**
- "N things to allow" counts only the two switches.

## 12. Ringing while the phone is in use (decision)
- Keep the **same full ringing screen** when it opens over another app (Show over other apps on). Don't make a smaller version: the user picked Alarm for this habit precisely so it can't be missed, and one screen keeps the behaviour predictable. A compact card would just repeat the heads-up notification, which the Notification type already gives.
- Two small differences when the phone is unlocked:
  - **Open Weft** opens the habit straight away, with no unlock step.
  - **Dismiss**, **Snooze**, sliding to done and Back return to the app the person was in, not the home screen.
- If the person is in a call, don't open the screen. Show the heads-up notification with its actions instead, and ring at in-call volume.
- Built (2026-11-08): the screen is its own task, so closing it returns to the app underneath. Back is Dismiss only while the phone is unlocked; on the lock screen it still does nothing. In a call (or while one rings in), the Show over other apps fallback doesn't open the screen and the sound plays at 20 % (`IN_CALL_VOLUME`).

## Platform notes
- Use `AlarmManager.setAlarmClock()` (exact, shown in the status bar). On Android 12+ this needs `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`. If it isn't granted, picking Alarm opens Set up alarms (section 7).
- Android 14+: `USE_FULL_SCREEN_INTENT` must be granted. Otherwise, fall back to a heads-up notification with the same actions.
- Reschedule after boot, time change and app update (the existing reminder receiver).
- Backup/restore: store the reminder type with the reminder (`"type": "notification" | "alarm"`, default notification).

## Out of scope
- Gradual volume. (Picking the alarm sound is in 1.6.2: section 9.)

## As built (1.6.0, 2026-11-08)
- **Power or a volume button** silences every alarm that is ringing, like Dismiss on each (confirmed by the user, 2026-11-08).
- **Any log of the habit for today** stops its alarm: Done, Skip and Partial, from Today, a widget or a notification. A habit already partly done before the alarm still rings.
- **Notification reminders** have Done · (+step) · Dismiss, no Snooze; only alarms snooze (see FEATURE-NOTIFICATION-ACTIONS.md).
- **Open and close** fade (no slide), and the screen is see-through until its first frame, so it never flashes the page colour.
- Code: `notify/AlarmRinger.kt` (the queue, `hush` for Log amount, `leave` for Open Weft, the log watcher), `ui/alarm/AlarmActivity.kt`. Every case is a smoke check (`python3 tools/verify/smoke.py alarm`).

