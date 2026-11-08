---
name: reminders
description: Habit reminders, their notification buttons, alarm reminders and the evening note nudge (AlarmManager, notification permission, full-screen alarms, rescheduling). Use when changing reminders, notifications or alarms, or when a reminder doesn't fire.
---

# Reminders

**Rules:** a reminder is a **Notification** or an **Alarm** (`habit.reminderType`, 1.6.0). Notifications are a title and one line with buttons (`features/FEATURE-NOTIFICATION-ACTIONS.md`); tapping opens the habit. Alarms ring full screen until answered (`features/FEATURE-ALARM-REMINDERS.md`). Reschedule on boot, time change, time-zone change and every habit edit. Nothing rings or reminds once today is Done or Skip.

**Code**
- `notify/ReminderScheduler.kt`: one alarm per habit; exact (`setExactAndAllowWhileIdle`) when "Alarms & reminders" is allowed, otherwise a 10-minute window; alarm reminders use `setAlarmClock` (status-bar clock). `start()` watches the database (the key includes `reminderType`), so any edit, archive, delete or import reschedules. Also the 9 PM note nudge (`NOTE_NUDGE_MINUTES`).
- `notify/ReminderReceiver.kt`: fires (`remind()` picks notification or alarm), schedules the next; `ACTION_SNOOZED` (one-off, doesn't move the schedule), `ACTION_ALARM_TIMEOUT`. `RescheduleReceiver` for boot/time/zone/package replaced/exact-alarm permission change.
- `notify/ReminderNotifier.kt` (text, skips days done or skipped, `setOnlyAlertOnce`), `notify/Notifications.kt` (channels `reminders`, `notes`, `alarms`; permission checks, `canUseFullScreen`, settings pages), `domain/ReminderTime.kt` (+ `ReminderTimeTest`).
- `notify/ReminderActions.kt`: the buttons (check-off Done · Dismiss; measured Done · +step · Dismiss — no Snooze on notifications, user 2026-11-08; note nudge Add note · Done; alarm Done · Snooze · Dismiss) and `ReminderActionReceiver`, which logs like the app (check-in, widgets), stops any ringing alarm and records Undo in `notify/NotificationUndo.kt` (Today's Undo bar on the next open).
- `notify/AlarmRinger.kt`: a queue of ringing alarms (alarms at the same time ring as one, in list order, one sound; `hush` for Log amount, `leave` for Open Weft; a watcher stops an alarm on any new log of its habit, Partial too). An alarm is a notification on the `alarms` channel (alarm sound, alarm stream, vibration, `FLAG_INSISTENT`, full-screen intent); no service. Stops on any answer, on the power button (screen-off receiver while ringing) and after 10 minutes ("missed at" notification). `ui/alarm/AlarmActivity.kt`: the ringing screen over the lock screen (Open Weft, goal line, slide to mark done, Log amount / Skip today, Snooze | Dismiss, "1 of 2" chip and Next line; power or volume = Dismiss all; Back does nothing; fades in and out, see-through theme `Theme.Weft.Alarm`).
- New habit → Reminder card and sheet: `ui/edit/EditHabitScreen.kt` (`TimePickerSheet` with Notification / Alarm `ChoiceRow`s).

**Verify**
- `python3 tools/verify/smoke.py alarm_ notification_buttons reminder` on a rootable emulator (Sprout_API_35), animations off. It covers: heads-up while in use, full screen when locked, slide = Done, Snooze rings again, volume and power silence, no ring when Done / Skip, 10-minute timeout → missed, no full-screen permission → heads-up, no exact alarms → the sheet says so, scheduled as an alarm clock and kept after reboot, notification buttons + Undo, Skip today, Log amount (Cancel / Save), Open Weft, two alarms at once (one sound, 1 of 2 → 2 of 2), power silences all, a Partial log in the app stops it, a partly done habit still rings.
- `adb shell dumpsys alarm | grep -A2 action.REMINDER` shows the next times; an alarm reminder appears under "Next alarm clock information".
- Fire one by hand: `adb shell am broadcast -n app.sprout.habits/.notify.ReminderReceiver -a app.sprout.habits.action.REMINDER --el habitId <id>` (as root).
- Exact alarms: `adb shell appops set app.sprout.habits SCHEDULE_EXACT_ALARM allow`; full screen: `... USE_FULL_SCREEN_INTENT allow|deny`.

**Pitfalls**
- With the screen on and unlocked Android shows a full-screen intent as a heads-up; lock the screen (`input keyevent KEYCODE_SLEEP`) to see the ringing screen.
- The heads-up hides after a few seconds: open the shade (`cmd statusbar expand-notifications`) to reach its buttons in a test.
- A notification channel's sound can't change after it is created; a new sound needs a new channel id.
