package app.sprout.habits.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sprout.habits.MainActivity
import app.sprout.habits.R
import app.sprout.habits.WeftApp
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.measure
import app.sprout.habits.domain.step
import app.sprout.habits.ui.nav.AppLink
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The buttons on reminder notifications (features/FEATURE-NOTIFICATION-ACTIONS.md): log a habit
 * without opening the app. Logging here is the same as in the app: it counts as a check-in,
 * updates Today and the widgets, and Today offers Undo the next time it opens.
 */
object ReminderActions {
    const val ACTION_DONE = "app.sprout.habits.action.REMINDER_DONE"
    const val ACTION_ADD_STEP = "app.sprout.habits.action.REMINDER_ADD_STEP"
    const val ACTION_SKIP = "app.sprout.habits.action.REMINDER_SKIP"
    const val ACTION_SNOOZE = "app.sprout.habits.action.REMINDER_SNOOZE"
    /** Stops an alarm without logging anything. */
    const val ACTION_DISMISS = "app.sprout.habits.action.ALARM_DISMISS"

    /** How long Snooze waits before the reminder comes back. */
    const val SNOOZE_MINUTES = 10

    /** The buttons for a habit's own reminder: check-off habits get two, measured ones three. */
    fun forReminder(context: Context, habit: Habit): List<NotificationCompat.Action> = buildList {
        add(action(context, ACTION_DONE, habit.id, context.getString(R.string.outcome_done)))
        if (habit.trackType == TrackType.CHECK) {
            add(action(context, ACTION_SNOOZE, habit.id, context.getString(R.string.notify_snooze_minutes, SNOOZE_MINUTES)))
        } else {
            add(action(context, ACTION_ADD_STEP, habit.id, context.getString(R.string.notify_add_step, habit.measure(habit.step()))))
            add(action(context, ACTION_SNOOZE, habit.id, context.getString(R.string.notify_snooze)))
        }
    }

    /**
     * The buttons for the evening note nudge (only sent for a habit skipped today): Add note,
     * which opens New note for that habit and day, and Done, in case it got done after all.
     */
    fun forNoteNudge(context: Context, habit: Habit, day: Long): List<NotificationCompat.Action> = listOf(
        NotificationCompat.Action.Builder(0, context.getString(R.string.add_note), addNoteIntent(context, habit.id, day)).build(),
        action(context, ACTION_DONE, habit.id, context.getString(R.string.outcome_done)),
    )

    /** The buttons of a ringing alarm's notification (shown when it can't open full screen). */
    fun forAlarm(context: Context, habit: Habit): List<NotificationCompat.Action> = listOf(
        action(context, ACTION_DONE, habit.id, context.getString(R.string.outcome_done)),
        action(context, ACTION_SNOOZE, habit.id, context.getString(R.string.notify_snooze_minutes, SNOOZE_MINUTES)),
        action(context, ACTION_DISMISS, habit.id, context.getString(R.string.alarm_dismiss)),
    )

    private fun action(context: Context, action: String, habitId: Long, label: String): NotificationCompat.Action {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId)
            .putExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay())
        // One request code per habit and button, so the buttons of different habits never mix.
        val code = habitId.toInt() * 8 + ACTIONS.indexOf(action)
        val pending = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action.Builder(0, label, pending).build()
    }

    private fun addNoteIntent(context: Context, habitId: Long, day: Long): PendingIntent = PendingIntent.getActivity(
        context,
        habitId.toInt() * 8 + 7,
        Intent(context, MainActivity::class.java)
            .setAction(AppLink.ACTION_ADD_NOTE)
            .putExtra(AppLink.EXTRA_HABIT_ID, habitId)
            .putExtra(AppLink.EXTRA_DAY, day)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private val ACTIONS = listOf(ACTION_DONE, ACTION_ADD_STEP, ACTION_SKIP, ACTION_SNOOZE, ACTION_DISMISS)

    /** Brings the reminder back in [SNOOZE_MINUTES]; only one snooze per habit is ever pending. */
    fun snooze(context: Context, habitId: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        val intent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_BASE + habitId.toInt(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderScheduler.ACTION_SNOOZED)
                .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId)
                .putExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val exact = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        else alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, intent)
    }

    /** Snooze alarms use these request codes, clear of the daily reminders' (the habit id). */
    private const val SNOOZE_REQUEST_BASE = 0x20000000
}

/** Runs a reminder button: Done, +step, Skip or Snooze. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WeftApp).container
        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L)
        val day = intent.getLongExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay())
        val manager = NotificationManagerCompat.from(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = container.repository.getHabit(habitId) ?: return@launch
                // Any answer stops a ringing alarm for this habit.
                container.alarmRinger.stop(habit.id)
                // A button tapped after midnight on yesterday's reminder still logs yesterday.
                val previous = container.repository.getEntry(habit.id, day)
                val now = System.currentTimeMillis()
                val next: Entry? = when (intent.action) {
                    ReminderActions.ACTION_DONE -> Entry(habit.id, day, EntryStatus.DONE, habit.target, loggedAt = now)
                    ReminderActions.ACTION_SKIP -> Entry(habit.id, day, EntryStatus.SKIP, loggedAt = now)
                    ReminderActions.ACTION_ADD_STEP -> {
                        val amount = ((previous?.amount ?: 0.0) + habit.step()).coerceAtMost(habit.target)
                        Entry(habit.id, day, if (amount >= habit.target) EntryStatus.DONE else EntryStatus.PARTIAL, amount, loggedAt = now)
                    }
                    ReminderActions.ACTION_SNOOZE -> {
                        manager.cancel(ReminderNotifier.reminderId(habit.id))
                        ReminderActions.snooze(context, habit.id)
                        null
                    }
                    else -> null
                }
                if (next == null || next.sameOutcomeAs(previous)) return@launch
                container.repository.setEntry(next)
                if (next.status != EntryStatus.SKIP) container.settings.addCheckIn()
                container.notificationUndo.record(context, habit, previous, next)
                when (next.status) {
                    // Still short of the goal: the same reminder, with the new progress line.
                    EntryStatus.PARTIAL -> container.reminderNotifier.showReminder(habit)
                    else -> {
                        manager.cancel(ReminderNotifier.reminderId(habit.id))
                        manager.cancel(ReminderNotifier.noteReminderId(habit.id))
                    }
                }
                container.widgets.updateAll()
            } finally {
                pending.finish()
            }
        }
    }
}
