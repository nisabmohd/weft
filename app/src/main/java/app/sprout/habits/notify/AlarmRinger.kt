package app.sprout.habits.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import app.sprout.habits.ui.theme.habitColors
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sprout.habits.R
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import kotlinx.coroutines.flow.first
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
 * (silent) Alarms channel with a full-screen intent that opens [AlarmActivity] (a heads-up with
 * Done / Snooze / Dismiss when Android doesn't open it full screen), while [AlarmSoundService]
 * plays the alarm sound and vibrates until it is answered. Alarms ringing together are one
 * notification, the one on top: "6:45 AM · 1 of 2 · Next: Workout". After [RING_MINUTES] it
 * stops by itself and a missed-alarm notification is posted.
 */
class AlarmRinger(
    private val context: Context,
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
) {
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
            // The one that was on top now waits behind this one, without a card of its own.
            if (oldHead != null && oldHead.habitId != habit.id) {
                AlarmSoundService.stop()
                NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(oldHead.habitId))
            }
            post(habit, minutes, loud = true, sound = settings.settings.first())
        } else {
            // It waits: the card on top now says "1 of 2 · Next: …".
            showHead(loud = false)
        }
        scheduleTimeout(habit.id)
        listenForPowerButton()
    }

    /** Posts the alarm on top again: after one is answered, or to update its "1 of 2" line. */
    private suspend fun showHead(loud: Boolean) {
        val head = _queue.value.firstOrNull() ?: return
        val prefs = if (loud) settings.settings.first() else null
        repository.getHabit(head.habitId)?.let { post(it, head.minutes, loud, sound = prefs) }
    }

    /** "6:45 AM · 1 of 2 · Next: Workout" for the alarm on top; just the time otherwise. */
    private fun line(habitId: Long, minutes: Int): String {
        val time = TodayViewModel.formatTime(minutes)
        val q = _queue.value
        if (q.firstOrNull()?.habitId != habitId) return time
        val total = _answered.value + q.size
        if (total < 2) return time
        val parts = mutableListOf(time, context.getString(R.string.alarm_position, _answered.value + 1, total))
        q.getOrNull(1)?.let { parts += context.getString(R.string.widget_next, it.name) }
        return parts.joinToString(" · ")
    }

    /**
     * The alarm's notification: our own layout (like Burrow's) with a Done pill and Snooze 10 min /
     * Dismiss as text buttons, as Android's action buttons get cut and tinted on some phones.
     * [loud]: the sound ([AlarmSoundService]) and the full-screen intent; otherwise it is quiet.
     */
    @SuppressLint("MissingPermission") // checked by canPost()
    private fun post(habit: Habit, minutes: Int, loud: Boolean, line: String = line(habit.id, minutes), sound: Settings? = null) {
        val id = ReminderNotifier.reminderId(habit.id)
        val screen = PendingIntent.getActivity(
            context,
            id,
            AlarmActivity.intent(context, habit.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val small = RemoteViews(context.packageName, R.layout.notification_alarm_small).apply {
            setTextViewText(R.id.title, habit.name)
            setTextViewText(R.id.line, line)
        }
        val big = RemoteViews(context.packageName, R.layout.notification_alarm).apply {
            setTextViewText(R.id.title, habit.name)
            setTextViewText(R.id.line, line)
            setTextViewText(R.id.done, context.getString(R.string.outcome_done))
            setOnClickPendingIntent(R.id.done, ReminderActions.pendingAction(context, ReminderActions.ACTION_DONE, habit.id))
            setTextViewText(R.id.snooze, context.getString(R.string.notify_snooze_minutes, ReminderActions.SNOOZE_MINUTES))
            setOnClickPendingIntent(R.id.snooze, ReminderActions.pendingAction(context, ReminderActions.ACTION_SNOOZE, habit.id))
            setTextViewText(R.id.dismiss, context.getString(R.string.alarm_dismiss))
            setOnClickPendingIntent(R.id.dismiss, ReminderActions.pendingAction(context, ReminderActions.ACTION_DISMISS, habit.id))
            // In the habit's colours, like the ringing screen (user, 2026-11-08): Done a pill in its
            // soft tone with ink text, Snooze / Dismiss in its solid tone, light and dark. Android 12+
            // only (night-aware colours); below that the logo green from colors.xml.
            if (Build.VERSION.SDK_INT >= 31) {
                val light = habitColors(habit.colorHue.toFloat(), dark = false)
                val dark = habitColors(habit.colorHue.toFloat(), dark = true)
                setColorStateList(
                    R.id.done, "setBackgroundTintList",
                    ColorStateList.valueOf(light.soft.toArgb()), ColorStateList.valueOf(dark.soft.toArgb()),
                )
                setColorInt(R.id.done, "setTextColor", light.ink.toArgb(), dark.ink.toArgb())
                for (id in intArrayOf(R.id.snooze, R.id.dismiss)) {
                    setColorInt(id, "setTextColor", light.solid.toArgb(), dark.solid.toArgb())
                }
            }
        }
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(line)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setCustomHeadsUpContentView(big)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(!loud)
            // The next alarm's card while the ringing screen already shows it ("2 of 2"): no pop-up
            // banner over that screen (it hid Open Weft); it waits in the shade.
            .setSilent(!loud && AlarmActivity.showing)
            .setContentIntent(screen)
        if (loud) builder.setFullScreenIntent(screen, true)
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(id, notification)
        if (loud) {
            val prefs = sound ?: Settings()
            AlarmSoundService.start(context, id, notification, AlarmSounds.uri(context, prefs.alarmSound), prefs.alarmVibrate)
            AlarmScreenFallback.schedule(context, habit.id) { _queue.value.firstOrNull()?.habitId == habit.id }
        }
    }

    /**
     * Stops the alarm for [habitId] (Done, Snooze, Dismiss, a log anywhere, the 10-minute limit).
     * The next waiting alarm takes the screen without ringing again (design 2026-11-08: the user
     * is already there); its notification stays until it is answered.
     */
    fun stop(habitId: Long) {
        val (wasHead, next) = synchronized(this) {
            val before = _queue.value
            _queue.value = before.filterNot { it.habitId == habitId }
            val next = _queue.value.firstOrNull()
            _answered.value = when {
                next == null -> 0
                before.size != _queue.value.size -> _answered.value + 1
                else -> _answered.value
            }
            val wasHead = before.firstOrNull()?.habitId == habitId
            // The sound service takes its card down (Android ignores the app's cancel of a foreground
            // service's notification); the cancel covers a card it never held. Inside the lock, so a
            // "next" card being posted can't come back after it.
            if (wasHead) AlarmSoundService.stop()
            NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(habitId))
            wasHead to next
        }
        context.getSystemService(AlarmManager::class.java).cancel(timeoutIntent(habitId))
        if (next == null) {
            stopListening()
        } else {
            // The next one shows without ringing again (design 2026-11-08: the user is already there),
            // or the one on top gets its new "2 of 2" line.
            val head = next.habitId
            CoroutineScope(Dispatchers.IO).launch {
                val habit = repository.getHabit(head) ?: return@launch
                // Only if it's still waiting (the power button answers every alarm at once).
                synchronized(this@AlarmRinger) {
                    if (_queue.value.firstOrNull()?.habitId == head) post(habit, next.minutes, loud = false)
                }
            }
        }
    }

    /** Stops the sound of the alarm for [habitId] but keeps it on screen (Log amount). */
    fun hush(habitId: Long) {
        val r = _queue.value.firstOrNull { it.habitId == habitId } ?: return
        AlarmSoundService.stop(keepCard = true)
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
        AlarmSoundService.stop()
        NotificationManagerCompat.from(context).cancel(ReminderNotifier.reminderId(habitId))
        context.getSystemService(AlarmManager::class.java).cancel(timeoutIntent(habitId))
        stopListening()
        CoroutineScope(Dispatchers.IO).launch {
            rest.forEach { r -> repository.getHabit(r.habitId)?.let { post(it, r.minutes, loud = false, line = TodayViewModel.formatTime(r.minutes)) } }
        }
    }

    /** Rang for [RING_MINUTES] with no answer: stop, and say it was missed. */
    @SuppressLint("MissingPermission")
    suspend fun timedOut(habitId: Long) {
        val active = context.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == ReminderNotifier.reminderId(habitId) && it.notification.channelId == Notifications.CHANNEL_ALARMS }
        val rang = _queue.value.firstOrNull { it.habitId == habitId }
        stop(habitId)
        // A waiting alarm has no card of its own, but it rang unanswered too.
        if (!active && rang == null) return
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
