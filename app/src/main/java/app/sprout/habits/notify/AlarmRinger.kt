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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** What is ringing right now: the habit, and the time it rang for (minutes after midnight). */
data class Ringing(
    val habitId: Long,
    val name: String,
    val icon: String,
    val hue: Int,
    val minutes: Int,
    /** When today's entry was last set before it rang (a partly done habit still rings); null if none. */
    val loggedBefore: Long? = null,
    /** The habit's place in the list: alarms that share a time go in this order. */
    val sortOrder: Int = 0,
)

/**
 * Alarm reminders (features/FEATURE-ALARM-REMINDERS.md). An alarm is a notification on the
 * Alarms channel: the alarm sound on the alarm stream with vibration, repeated until it is
 * answered, and a full-screen intent that opens [AlarmActivity] (a heads-up notification with
 * Done / Snooze / Dismiss when Android doesn't allow full-screen intents). No service keeps
 * running: cancelling the notification stops the sound. After [RING_MINUTES] it stops by
 * itself and a missed-alarm notification is posted.
 */
class AlarmRinger(private val context: Context, private val repository: HabitRepository) {
    /**
     * Every alarm ringing now, in the order they rang (two habits can share a time). Only the
     * first one sounds and is on the ringing screen; the rest wait silently, and when the first is
     * answered the next one sounds and takes the screen (user, 2026-11-08).
     */
    private val _queue = MutableStateFlow<List<Ringing>>(emptyList())
    val queue: StateFlow<List<Ringing>> = _queue

    /** How many of the alarms ringing together were answered already: the chip's "2 of 2". */
    private val _answered = MutableStateFlow(0)
    val answered: StateFlow<Int> = _answered

    /**
     * The power button silences every ringing alarm, like Dismiss on each, also when they ring as
     * heads-up notifications (no ringing screen to catch the key). Registered only while one rings.
     */
    private var screenOff: BroadcastReceiver? = null

    private fun listenForPowerButton() {
        if (screenOff != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                _queue.value.forEach { r ->
                    c.sendBroadcast(
                        Intent(c, ReminderActionReceiver::class.java)
                            .setAction(ReminderActions.ACTION_DISMISS)
                            .putExtra(ReminderScheduler.EXTRA_HABIT_ID, r.habitId),
                    )
                }
            }
        }
        context.applicationContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        screenOff = receiver
    }

    private fun stopListening() {
        screenOff?.let { runCatching { context.applicationContext.unregisterReceiver(it) } }
        screenOff = null
    }

    /**
     * Stops an alarm as soon as its habit is logged for today anywhere (Today, a widget, a
     * notification), Partial included: logging is an answer (user, 2026-11-08).
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            val start = LocalDate.now().toEpochDay()
            repository.observeEntries(start, start + 7).collect { entries ->
                val today = LocalDate.now().toEpochDay()
                // A log made after it started ringing (not the partial entry it rang with).
                val byHabit = entries.filter { it.date == today }.associateBy { it.habitId }
                _queue.value.filter { r ->
                    val e = byHabit[r.habitId] ?: return@filter false
                    e.loggedAt != r.loggedBefore
                }.forEach { stop(it.habitId) }
            }
        }
    }

    /**
     * Rings for [habit], unless today is already Done or Skip (a Partial one still rings); waits
     * silently if another alarm is sounding.
     */
    suspend fun ring(habit: Habit) {
        if (habit.archived || !Notifications.canPost(context)) return
        val today = repository.getEntry(habit.id, LocalDate.now().toEpochDay())
        if (today?.status == EntryStatus.DONE || today?.status == EntryStatus.SKIP) return
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        val entry = Ringing(habit.id, habit.name, habit.icon, habit.colorHue, minutes, today?.loggedAt, habit.sortOrder)
        // Alarms that share a time ring as one, in the habits' order (design 2026-11-08, alarm-7).
        // Only the first sounds; if this one goes first, the one that was sounding goes quiet.
        val (oldHead, newHead) = synchronized(this) {
            val before = _queue.value.firstOrNull()
            val q = (_queue.value.filterNot { it.habitId == habit.id } + entry).sortedBy { it.sortOrder }
            _queue.value = q
            before to q.first()
        }
        if (newHead.habitId == habit.id) {
            if (oldHead != null && oldHead.habitId != habit.id) {
                repository.getHabit(oldHead.habitId)?.let { post(it, oldHead.minutes, loud = false) }
            }
            post(habit, minutes, loud = true)
        } else {
            post(habit, minutes, loud = false)
        }
        scheduleTimeout(habit.id)
        listenForPowerButton()
    }

    /**
     * The alarm's notification. [loud]: the alarm sound, repeated until it is cancelled, and the
     * full-screen intent. A waiting alarm is the same notification without sound or screen.
     */
    @SuppressLint("MissingPermission") // checked by canPost()
    private fun post(habit: Habit, minutes: Int, loud: Boolean) {
        val screen = PendingIntent.getActivity(
            context,
            ReminderNotifier.reminderId(habit.id),
            AlarmActivity.intent(context, habit.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(TodayViewModel.formatTime(minutes))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screen)
            .apply { ReminderActions.forAlarm(context, habit).forEach(::addAction) }
        if (loud) builder.setFullScreenIntent(screen, true) else builder.setSilent(true)
        val notification = builder.build()
        if (loud) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(ReminderNotifier.reminderId(habit.id), notification)
    }

    /**
     * Stops the alarm for [habitId] (Done, Snooze, Dismiss, a log anywhere, the 10-minute limit).
     * The next waiting alarm takes the screen without ringing again (design 2026-11-08: the user
     * is already there); its notification stays until it is answered.
     */
    fun stop(habitId: Long) {
        val next = synchronized(this) {
            val before = _queue.value
            _queue.value = before.filterNot { it.habitId == habitId }
            val next = _queue.value.firstOrNull()
            _answered.value = when {
                next == null -> 0
                before.size != _queue.value.size -> _answered.value + 1
                else -> _answered.value
            }
            next
        }
        NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(habitId))
        context.getSystemService(AlarmManager::class.java).cancel(timeoutIntent(habitId))
        if (next == null) stopListening()
    }

    /** Stops the sound of the alarm for [habitId] but keeps it on screen (Log amount). */
    fun hush(habitId: Long) {
        val r = _queue.value.firstOrNull { it.habitId == habitId } ?: return
        CoroutineScope(Dispatchers.IO).launch {
            repository.getHabit(habitId)?.let { post(it, r.minutes, loud = false) }
        }
    }

    /**
     * Open Weft: the alarm on screen stops; the others stay as quiet notifications, unanswered
     * (design 2026-11-08), and leave the screen.
     */
    fun leave(habitId: Long) {
        val rest = synchronized(this) {
            val r = _queue.value.filterNot { it.habitId == habitId }
            _queue.value = emptyList()
            _answered.value = 0
            r
        }
        NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(habitId))
        context.getSystemService(AlarmManager::class.java).cancel(timeoutIntent(habitId))
        stopListening()
        CoroutineScope(Dispatchers.IO).launch {
            rest.forEach { r -> repository.getHabit(r.habitId)?.let { post(it, r.minutes, loud = false) } }
        }
    }

    /** Rang for [RING_MINUTES] with no answer: stop, and say it was missed. */
    @SuppressLint("MissingPermission")
    suspend fun timedOut(habitId: Long) {
        val active = context.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == ReminderNotifier.reminderId(habitId) && it.notification.channelId == Notifications.CHANNEL_ALARMS }
        val rang = _queue.value.firstOrNull { it.habitId == habitId }
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
