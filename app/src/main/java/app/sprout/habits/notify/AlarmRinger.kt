package app.sprout.habits.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sprout.habits.R
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.ui.alarm.AlarmActivity
import app.sprout.habits.ui.today.TodayViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What is ringing right now: the habit, and the time it rang for (minutes after midnight). */
data class Ringing(val habitId: Long, val name: String, val icon: String, val hue: Int, val minutes: Int)

/**
 * Alarm reminders (features/FEATURE-ALARM-REMINDERS.md). An alarm is a notification on the
 * Alarms channel: the alarm sound on the alarm stream with vibration, repeated until it is
 * answered, and a full-screen intent that opens [AlarmActivity] (a heads-up notification with
 * Done / Snooze / Dismiss when Android doesn't allow full-screen intents). No service keeps
 * running: cancelling the notification stops the sound. After [RING_MINUTES] it stops by
 * itself and a missed-alarm notification is posted.
 */
class AlarmRinger(private val context: Context, private val repository: HabitRepository) {
    private val _ringing = MutableStateFlow<Ringing?>(null)

    /** The alarm on screen, or null; [AlarmActivity] closes when it goes back to null. */
    val ringing: StateFlow<Ringing?> = _ringing

    /**
     * The power button silences a ringing alarm like Dismiss, also when it rings as a heads-up
     * notification (no ringing screen to catch the key). Registered only while it rings.
     */
    private var screenOff: BroadcastReceiver? = null

    private fun listenForPowerButton(habitId: Long) {
        stopListening()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                c.sendBroadcast(
                    Intent(c, ReminderActionReceiver::class.java)
                        .setAction(ReminderActions.ACTION_DISMISS)
                        .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId),
                )
            }
        }
        context.applicationContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        screenOff = receiver
    }

    private fun stopListening() {
        screenOff?.let { runCatching { context.applicationContext.unregisterReceiver(it) } }
        screenOff = null
    }

    /** Rings for [habit], unless today is already done or skipped. */
    @SuppressLint("MissingPermission") // checked by canPost()
    suspend fun ring(habit: Habit) {
        if (habit.archived || !Notifications.canPost(context)) return
        val entry = repository.getEntry(habit.id, LocalDate.now().toEpochDay())
        if (entry?.status == EntryStatus.DONE || entry?.status == EntryStatus.SKIP) return
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        _ringing.value = Ringing(habit.id, habit.name, habit.icon, habit.colorHue, minutes)
        val screen = PendingIntent.getActivity(
            context,
            ReminderNotifier.reminderId(habit.id),
            AlarmActivity.intent(context, habit.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(TodayViewModel.formatTime(minutes))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .apply { ReminderActions.forAlarm(context, habit).forEach(::addAction) }
            .build()
        // Sound and vibration repeat until the notification is cancelled.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(ReminderNotifier.reminderId(habit.id), notification)
        scheduleTimeout(habit.id)
        listenForPowerButton(habit.id)
    }

    /** Stops the alarm for [habitId] if it is ringing (Done, Snooze, Dismiss, a button press). */
    fun stop(habitId: Long) {
        if (_ringing.value?.habitId == habitId) {
            _ringing.value = null
            stopListening()
        }
        NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(habitId))
        context.getSystemService(AlarmManager::class.java).cancel(timeoutIntent(habitId))
    }

    /** Rang for [RING_MINUTES] with no answer: stop, and say it was missed. */
    @SuppressLint("MissingPermission")
    suspend fun timedOut(habitId: Long) {
        val active = context.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == ReminderNotifier.reminderId(habitId) && it.notification.channelId == Notifications.CHANNEL_ALARMS }
        val rang = _ringing.value?.takeIf { it.habitId == habitId }
        stop(habitId)
        if (!active) return
        val habit = repository.getHabit(habitId) ?: return
        val at = rang?.minutes ?: habit.reminderMinutes ?: return
        ReminderNotifier(context, repository).post(
            habit.id,
            Notifications.CHANNEL_REMINDERS,
            context.getString(R.string.alarm_missed, habit.name, TodayViewModel.formatTime(at)),
            context.getString(R.string.reminder_check),
            ReminderNotifier.reminderId(habit.id),
            ReminderActions.forReminder(context, habit),
        )
    }

    private fun scheduleTimeout(habitId: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + RING_MINUTES * 60_000L
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, timeoutIntent(habitId))
        else alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, timeoutIntent(habitId))
    }

    private fun timeoutIntent(habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        TIMEOUT_REQUEST_BASE + habitId.toInt(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderScheduler.ACTION_ALARM_TIMEOUT)
            .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** An unanswered alarm stops after this long. */
        const val RING_MINUTES = 10

        /** Clear of the reminders' (the habit id) and the snoozes' request codes. */
        private const val TIMEOUT_REQUEST_BASE = 0x30000000
    }
}
