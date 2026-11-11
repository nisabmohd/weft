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
  - Snooze: stops the alarm and rings again in 10 minutes. Only one snooze is pending at a time. **Three snoozes a day** per habit (user, 2026-11-11): on the fourth ring Snooze is gone from the ringing screen (Dismiss is the one full-width button) and from the card; a Snooze from an old card counts as Dismiss (`ReminderActions.MAX_SNOOZES`, `SnoozeLimitTest`, smoke `alarm_snooze_stops_after_three`).
  - Dismiss: stops the alarm without logging anything.
  - Power button: silences it, like Dismiss. A volume button only stops the sound; the screen stays to be answered (user, 2026-11-09).
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
- **A notification reminder at the same minute** (another habit, reminder type Notification): the alarm is the only thing that pops up and sounds; the reminder goes into the shade quietly (no banner, no sound, no `NotificationChime`), whichever arrives first: while an alarm rings or waits (`AlarmRinger` queue), or when another habit's alarm is due at that minute today and isn't done or skipped (`ReminderNotifier.alarmDueWith`). Alone, a reminder pops up as usual (user, 2026-11-11: a reminder and an alarm came a second apart on a OnePlus 13s). Tests: `AlarmTogetherTest`, smoke `notification_and_alarm_at_the_same_time`.

## 7. Set up alarms sheet (`alarm-9a/b/c-setup-sheet-*.png`)
Plain language, short, and built like the app's other sheets.

- Title "Set up alarms" (no subtitle).
- Rows with the app switch and a line saying what each is for (on = allowed, off = missing; tapping opens that system page; the switch updates on return, `ON_RESUME`) (user, 2026-11-11):
  - **Send notifications** · "For reminders. Tap to choose banners, sound and lock screen." Every phone. Tapping asks for the permission (13+) or opens Weft's notification settings, where the person picks how they show. No separate Banner toggle (user, 2026-11-11, like Instagram); while banners are off it says "Banners are off, so reminders won't pop up. Tap to turn them on."
  - **Ring alarms on time** · "Without this, Android may ring them a few minutes late." (exact alarms, Android 12+)
  - **Show alarms full screen** · "An alarm fills the screen, even when the phone is locked." (full-screen intent, Android 14+)
  - Hide rows this Android version doesn't need. The labels say what the person gets, not Android's permission names.
- **Extra phone settings** ↗ ("Only needed on some phones"): one row, on every phone (no list of phone makers, user 2026-11-11). It opens the Extra phone settings sheet.
- System pages open through `notify/SystemPages.kt`: inside Weft's task, never with `FLAG_ACTIVITY_NEW_TASK` (with it, a Settings task left on another page could come back instead of the page asked for), each with fallbacks (app notifications, then Weft's app info) for phones that lack the exact page.
- Bottom `WeftButton`: secondary **Use a notification instead** while a switch is off, primary **Done** when all three are on.
- Opens when Alarm is picked and something is missing, and from the Reminder card warning.

## 7b. Extra phone settings sheet (`extra-phone-settings-sheet.png`, opened from Reminders & alarms or Set up alarms)
- Title, plus one muted line: "Some phones stop alarms on their own. Android can't tell us if these are on, so check them once."
- **Show over other apps** switch ("Optional · lets an alarm open over other apps", `SYSTEM_ALERT_WINDOW`, the real state). It's optional: it's never counted, never blocks Done and never triggers a warning. Android 11+ opens the list of apps (Android ignores the app named in the public intent); Weft tries AOSP's per-app page first.
- **Lock screen & pop-ups** ↗ ("Turn both on in Weft's app info") and **Battery** ↗ ("Set to "Don't restrict" so alarms aren't stopped"). They open Weft's app info and never show a state.
- Primary **Done**.

## 8. Reminder card warning (`alarm-10-reminder-card-warning.png`)
- If a habit's reminder is an Alarm and a switch is off, the card's second line turns error colour: "Alarm · can't ring full screen · tap to fix". Name the most important missing piece: notifications > on the minute > full screen.
- Tapping the card opens Set up alarms.

## 9. Alarm sound sheet (`alarm-11-sound-sheet.png`, `ChoiceSheet`, opened from Reminders & alarms › Alarm sound)
- Title "Alarm sound". `ChoiceRow`s:
  - **Weft** (default)
  - **Weft · Soft** (if we ship it)
  - **Phone's alarm sound** (the phone's sound name)
- Below the list, the "Help translate Weft"-style card **Choose from phone** ↗ opens Android's sound picker. The picked sound then appears as a ChoiceRow.
- Tapping a row selects it and plays about 4 seconds ("Playing" with `volume-loud` in primary). Closing the sheet stops it. There's no button, as in the other ChoiceSheets.

## 10. More → Reminders & alarms (`more-reminders-alarms-row.png`, `more-reminders-alarms-screen.png`)
- **One row** in General replaces both "Notifications" (which used to open Android's settings with "System settings" as its value) and a separate Alarms row. It reads "Reminders & alarms", then a chevron.
  - Subtitle when everything's on: "Notifications and alarms allowed" (muted).
  - Subtitle when something's off: say what's wrong, in the error colour, the same phrase as the card warning: "Notifications are off", "Alarms may ring late" or "Alarms can't ring full screen".
  - Never "All set" or "N things to allow".
- It opens one simple screen, "Reminders & alarms", built like Backup & restore. Everything about reminders lives here:
  - **Reminders**: Default reminder › 9:00 AM ("For new habits"). This **moves here from More → General**, so General is Language, Week starts on, then Reminders & alarms.
  - **Allow Weft to**: Send notifications / Ring alarms on time / Show alarms full screen (switches with a line each, see 7), then **Extra phone settings** ↗ (opens 7b).
  - **Sound**: Alarm sound › Weft (opens the sound sheet), Vibrate ("When an alarm rings", on by default), Notification sound ↗ ("Change it in your phone's settings").
  - There's no status card and no "Needed / On some phones" split. The switches show the state.
- `NotificationSetupSheet` is no longer needed. More opens the screen.

- Built (1.6.2, 2026-11-09): `ui/more/RemindersAlarmsScreen.kt`, `ExtraPhoneSettingsSheet` in `ui/alarm/AlarmSetupSheet.kt`.

## 12. Ringing while the phone is in use (decision)
- Keep the **same full ringing screen** when it opens over another app (Show over other apps on). Don't make a smaller version: the user picked Alarm for this habit precisely so it can't be missed, and one screen keeps the behaviour predictable. A compact card would just repeat the heads-up notification, which the Notification type already gives.
- Two small differences when the phone is unlocked:
  - **Open Weft** opens the habit straight away, with no unlock step.
  - **Dismiss**, **Snooze**, sliding to done and Back return to the app the person was in, not the home screen.
- If the person is in a call, don't open the screen. Show the heads-up notification with its actions instead, and ring at in-call volume.
- Built (2026-11-08): the screen is its own task, so closing it returns to the app underneath. Back is Dismiss only while the phone is unlocked; on the lock screen it still does nothing. In a call (or while one rings in), the Show over other apps fallback doesn't open the screen and the sound plays at 20 % (`IN_CALL_VOLUME`).

## Exact time (applies to notification reminders too)
- "Ring alarms on time" is Android's exact-alarm permission: Settings → Apps → Special app access → Alarms & reminders (`SCHEDULE_EXACT_ALARM`; `USE_EXACT_ALARM` is allowed for alarm-clock apps on Android 13+).
- It is used for **every** reminder, notification or alarm: schedule with `AlarmManager.setAlarmClock()` for alarms and `setExactAndAllowWhileIdle()` for notification reminders.
- If it's off, fall back to `setAndAllowWhileIdle()`. Reminders still come, but can be a few minutes late in Doze. Check `canScheduleExactAlarms()` on each app start and listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` to reschedule.
- Possible rename (not decided): "Remind at the exact time" / "Otherwise reminders can be a few minutes late".
- Built since 1.6.0: `ReminderScheduler` uses `setExactAndAllowWhileIdle` (alarms `setAlarmClock`) and falls back to a 1-minute window when exact alarms aren't allowed.

## Platform notes
- Use `AlarmManager.setAlarmClock()` (exact, shown in the status bar). On Android 12+ this needs `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`. If it isn't granted, picking Alarm opens Set up alarms (section 7).
- Android 14+: `USE_FULL_SCREEN_INTENT` must be granted. Otherwise, fall back to a heads-up notification with the same actions.
- Reschedule after boot, time change and app update (the existing reminder receiver).
- Backup/restore: store the reminder type with the reminder (`"type": "notification" | "alarm"`, default notification).

## Out of scope
- Gradual volume. (Picking the alarm sound is in 1.6.2: section 9.)

## As built (1.6.0, 2026-11-08)
- **The power button** silences every alarm that is ringing, like Dismiss on each (confirmed by the user, 2026-11-08). **A volume button** only stops the sound and keeps the ringing screen (user, 2026-11-09).
- **Any log of the habit for today** stops its alarm: Done, Skip and Partial, from Today, a widget or a notification. A habit already partly done before the alarm still rings.
- **Notification reminders** have Done · (+step) · Dismiss, no Snooze; only alarms snooze (see FEATURE-NOTIFICATION-ACTIONS.md).
- **Opens at once and fades out** (no slide). It starts see-through so Android 12+ shows no splash screen, then turns opaque in onCreate (only an opaque screen covers the lock screen; 1.6.2 stayed see-through and never showed when locked), with the habit's colour painted before the first frame (fixed 2026-11-09).
- Code: `notify/AlarmRinger.kt` (the queue, `hush` for Log amount, `leave` for Open Weft, the log watcher), `ui/alarm/AlarmActivity.kt`. Every case is a smoke check (`python3 tools/verify/smoke.py alarm`).


## 13. Pop-ups and silent channels (2026-11-09, OnePlus 13s, ColorOS 17)

- ColorOS creates every new Weft channel at default importance with no sound and no vibration, whatever Weft asks for. A default-importance alarm never opens full screen (Android needs a pop-up channel), and reminders arrived silently in the shade.
- Channels are now `reminders_popup` (reminders and the evening note nudge, so one switch less) and `alarms_popup`, both high importance; the old ones are deleted. The alarm channel's own sound is half a second of silence, so no phone files it as Silent; `AlarmSoundService` still plays the alarm.
- The Banner notifications row (1.6.2) was removed on 2026-11-11 (user: one notifications row is enough): Send notifications opens Weft's notification settings and its line asks for banners while a channel doesn't pop up. The fallback buzz is marked as a notification vibration: Android drops a plain one from an app in the background.
- When a reminder's channel has no sound, Weft plays the phone's notification sound and vibrates itself (`NotificationChime`), except in silent mode (vibrate mode: buzz only), Do Not Disturb, a call, or for a channel the user set Silent or off. A +step update doesn't alert again.
- ColorOS also stretches a plain exact alarm into a window of up to an hour (measured: 80 s for one 2 minutes away, an hour for one 3 hours away), with battery limits off. On the makers in `AlarmSetup.delaysExactAlarms` notification reminders and the 9 PM note nudge are scheduled as alarm clocks, which stay on the minute; the status bar shows the alarm icon (user's choice, 2026-11-09). Other phones keep plain exact alarms.
