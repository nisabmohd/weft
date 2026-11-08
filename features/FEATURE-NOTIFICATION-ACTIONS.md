# Feature: actions on reminder notifications

Reminder notifications get action buttons, so a habit can be logged without opening the app.

Design: `design/notifications.png`.

## Actions by habit type
| Reminder | Actions |
| --- | --- |
| Check-off habit (e.g. Vitamins) | **Done** · **Dismiss** |
| Measured habit (e.g. Read, 20 pages) | **Done** · **+5 pages** · **Dismiss** |
| Note reminder ("Stay calm · how did today go?") | **Add note** · **Done** · **Skip** |

- **Done**: marks today Done (a measured habit is set to its goal), cancels the notification, and updates Today and the widgets.
- **+N <unit>**: adds the habit's step (the same step as + on the amount sheet). The notification updates its text ("You're at 20 of 20 pages today"). Once the goal is reached, the habit is Done and the notification goes away.
- **Skip**: logs Skip for today.
- **Add note**: opens New note for that habit and day (it can't run from the lock screen without unlocking).
- **Dismiss**: clears the notification without logging anything. (Changed 2026-11-08: notification reminders don't snooze; only alarm reminders have Snooze 10 min. `design/notifications.png` still shows the earlier Snooze.)

## Notes
- Use standard `NotificationCompat.Action`s with `PendingIntent`s to a broadcast receiver. Add note uses an activity intent. Labels come from string resources so they're translated.
- Android shows at most 3 actions. Keep the order above: the most likely action first, except for note reminders.
- Logging from a notification is the same as logging in the app: it's undoable from Today's Undo bar on the next open, and it's counted in Insights.
- The notification's text shows today's progress for measured habits ("Read · 5 pages to go").
