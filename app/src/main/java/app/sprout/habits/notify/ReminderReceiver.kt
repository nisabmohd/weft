package app.sprout.habits.notify

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sprout.habits.WeftApp
import app.sprout.habits.widget.WidgetDayReceiver
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at a habit's reminder time (posts it, then schedules the next one) and at the evening
 * note check (nudges skipped habits that ask for a note, then schedules tomorrow's check).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WeftApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Battery saving can hold an alarm back for hours. One that arrives after its day
                // is over is dropped: it would check the new day, which has nothing logged yet,
                // and remind about a habit that was already done or skipped.
                val day = intent.getLongExtra(ReminderScheduler.EXTRA_DAY, ReminderScheduler.NO_DAY)
                val onTime = day == ReminderScheduler.NO_DAY || day == LocalDate.now().toEpochDay()
                // A plain exact reminder that came late: this phone holds them back (any maker).
                // From now on every reminder is an alarm clock, which stays on the minute.
                val exactAt = intent.getLongExtra(ReminderScheduler.EXTRA_EXACT_AT, ReminderScheduler.NO_TIME)
                if (exactAt != ReminderScheduler.NO_TIME && AlarmSetup.noteLateness(context, exactAt)) {
                    container.reminders.rescheduleAll()
                }
                when (intent.action) {
                    ReminderScheduler.ACTION_REMINDER -> {
                        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L)
                        val habit = container.repository.getHabit(habitId) ?: return@launch
                        if (onTime) remind(container, habit)
                        container.reminders.schedule(habit)
                    }
                    ReminderScheduler.ACTION_SNOOZED -> {
                        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L)
                        val habit = container.repository.getHabit(habitId) ?: return@launch
                        if (onTime) remind(container, habit)
                    }
                    ReminderScheduler.ACTION_ALARM_TIMEOUT -> {
                        container.alarmRinger.timedOut(intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L))
                    }
                    ReminderScheduler.ACTION_NOTE_NUDGE -> {
                        val habits = container.repository.observeHabits().first()
                        if (onTime) container.reminderNotifier.showNoteNudges(habits)
                        container.reminders.scheduleNoteNudge(habits.any { it.askForNote })
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** A habit's reminder, as its notification or its alarm. */
private suspend fun remind(container: app.sprout.habits.AppContainer, habit: app.sprout.habits.data.Habit) {
    if (habit.reminderType == app.sprout.habits.data.ReminderType.ALARM) container.alarmRinger.ring(habit)
    else container.reminderNotifier.showReminder(habit)
}

/**
 * Alarms don't survive a reboot, and a clock or zone change moves wall-clock times. Widgets are
 * redrawn too: a clock or zone change can move them to another day, a language change renames them.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCALE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> Unit
            else -> return
        }
        val container = (context.applicationContext as WeftApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminders.rescheduleAll()
                container.widgets.updateAll()
                WidgetDayReceiver.scheduleNextDay(context)
                // Drive backups are timed on the wall clock (after 1 AM).
                container.drive.reschedule()
            } finally {
                pending.finish()
            }
        }
    }
}
