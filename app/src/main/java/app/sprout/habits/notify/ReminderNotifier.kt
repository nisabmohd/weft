package app.sprout.habits.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sprout.habits.MainActivity
import app.sprout.habits.R
import app.sprout.habits.ResourceStrings
import app.sprout.habits.Strings
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.domain.measure
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.TrackType
import app.sprout.habits.ui.today.TodayViewModel.Companion.formatNumber
import app.sprout.habits.domain.isScheduled
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import app.sprout.habits.data.ReminderType
import kotlinx.coroutines.launch

/**
 * Reminder notifications: a title, one line and the buttons from [ReminderActions] (Done, +step,
 * Snooze; Add note on the evening nudge); a tap opens the habit.
 */
class ReminderNotifier(
    private val context: Context,
    private val repository: HabitRepository,
    /** True while an alarm is ringing or waiting its turn (`AlarmRinger`'s queue). */
    private val alarmRinging: () -> Boolean = { false },
) {
    /**
     * The habit's reminder, unless today is already done or skipped. [alert]: false when +step
     * updates the line of a reminder already showing.
     */
    @SuppressLint("MissingPermission") // checked by canPost()
    suspend fun showReminder(habit: Habit, alert: Boolean = true) {
        if (habit.archived || !Notifications.canPost(context)) return
        val entry = repository.getEntry(habit.id, LocalDate.now().toEpochDay())
        if (entry?.status == EntryStatus.DONE || entry?.status == EntryStatus.SKIP) return
        val (title, text) = reminderText(habit, entry?.amount ?: 0.0, ResourceStrings(context))
        // An alarm at the same time takes the screen and the sound; this one waits in the shade.
        val quiet = alert && alarmTakesOver(habit)
        post(habit.id, Notifications.CHANNEL_REMINDERS, title, text, reminderId(habit.id), ReminderActions.forReminder(context, habit), alert && !quiet, silent = quiet)
    }

    private suspend fun alarmTakesOver(habit: Habit): Boolean {
        if (alarmRinging()) return true
        val today = LocalDate.now()
        val day = today.toEpochDay()
        val settled = repository.observeEntries(day, day).first()
            .filter { it.status == EntryStatus.DONE || it.status == EntryStatus.SKIP }.map { it.habitId }.toSet()
        return alarmDueWith(habit, repository.observeHabits().first(), today, settled)
    }

    /**
     * Takes a reminder that is still showing out of the shade once its habit is done or skipped
     * today, wherever that was logged (the app, a widget, an import).
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            // A week ahead covers a process that stays alive past midnight.
            val start = LocalDate.now().toEpochDay()
            repository.observeEntries(start, start + 7).collect { entries ->
                val today = LocalDate.now().toEpochDay()
                val manager = NotificationManagerCompat.from(context)
                for (entry in entries) {
                    if (entry.date != today || entry.status == EntryStatus.PARTIAL) continue
                    manager.cancel(reminderId(entry.habitId))
                }
            }
        }
    }

    /** For each habit that asks for a note: if it was skipped today and has no note yet, nudge. */
    suspend fun showNoteNudges(habits: List<Habit>) {
        val today = LocalDate.now()
        val day = today.toEpochDay()
        for (habit in habits) {
            if (!habit.askForNote || habit.archived || !isScheduled(habit.daysMask, today)) continue
            if (repository.getEntry(habit.id, day)?.status != EntryStatus.SKIP) continue
            if (repository.getNote(habit.id, day) != null) continue
            post(
                habit.id,
                Notifications.CHANNEL_REMINDERS,
                context.getString(R.string.nudge_title, habit.name),
                context.getString(R.string.nudge_text),
                noteReminderId(habit.id),
                ReminderActions.forNoteNudge(context, habit, day),
                alert = true,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun post(
        habitId: Long,
        channel: String,
        title: String,
        text: String,
        id: Int,
        actions: List<NotificationCompat.Action> = emptyList(),
        alert: Boolean = false,
        /** Into the shade without a banner or sound (an alarm is ringing). */
        silent: Boolean = false,
    ) {
        if (!Notifications.canPost(context)) return
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openHabit(habitId, id))
            .setAutoCancel(true)
            // +step updates the same notification: it shouldn't ring again.
            .setOnlyAlertOnce(true)
            .setSilent(silent)
        actions.forEach(builder::addAction)
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(id, notification)
        // The sound and buzz the phone may have taken off the channel (ColorOS).
        if (alert) NotificationChime.play(context, channel)
    }

    private fun openHabit(habitId: Long, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_HABIT)
            .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /**
         * True when another habit's alarm rings at [habit]'s reminder time today (and isn't done or
         * skipped, so it will ring): the two arrive within a second of each other, in either order,
         * and the alarm should be the only thing that pops up and sounds (user, 2026-11-11).
         */
        fun alarmDueWith(habit: Habit, habits: List<Habit>, today: LocalDate, settled: Set<Long>): Boolean {
            val minutes = habit.reminderMinutes ?: return false
            return habits.any {
                it.id != habit.id && !it.archived && it.reminderType == ReminderType.ALARM &&
                    it.reminderMinutes == minutes && isScheduled(it.daysMask, today) && it.id !in settled
            }
        }

        fun reminderId(habitId: Long) = habitId.toInt() * 2
        fun noteReminderId(habitId: Long) = habitId.toInt() * 2 + 1

        /** Title and line for a reminder, given how much is already logged today. */
        fun reminderText(habit: Habit, amount: Double, strings: Strings): Pair<String, String> {
            return when {
                habit.trackType == TrackType.CHECK -> habit.name to strings(R.string.reminder_check)
                amount > 0 -> strings(R.string.reminder_to_go, habit.name, habit.measure((habit.target - amount).coerceAtLeast(0.0))) to
                    strings(R.string.reminder_progress, formatNumber(amount), habit.measure(habit.target))
                else -> habit.name to strings(R.string.reminder_goal, habit.measure(habit.target))
            }
        }
    }
}
