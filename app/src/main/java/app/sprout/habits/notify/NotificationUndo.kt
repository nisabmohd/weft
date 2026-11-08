package app.sprout.habits.notify

import android.content.Context
import androidx.core.content.edit
import app.sprout.habits.R
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.domain.measure
import app.sprout.habits.ui.today.TodayViewModel.Companion.formatNumber
import app.sprout.habits.ui.today.UndoableChange
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The last change made from a notification button, kept until Today shows it in its Undo bar
 * ("Read marked done · Undo"). Stored on disk, because the app may not be running when the
 * button is tapped; only a change from the same day is offered.
 */
class NotificationUndo(context: Context) {
    private val prefs = context.getSharedPreferences("notification_undo", Context.MODE_PRIVATE)
    private val _pending = MutableStateFlow(prefs.contains(KEY_HABIT))

    /** True while a change is waiting for Today; Today takes it with [take]. */
    val pending: StateFlow<Boolean> = _pending

    fun record(context: Context, habit: Habit, previous: Entry?, next: Entry) {
        val message = when (next.status) {
            EntryStatus.DONE -> context.getString(R.string.log_marked_done, habit.name)
            EntryStatus.SKIP -> context.getString(R.string.log_skipped, habit.name)
            EntryStatus.PARTIAL -> context.getString(R.string.log_partial, habit.name, formatNumber(next.amount), habit.measure(habit.target))
        }
        prefs.edit {
            putLong(KEY_HABIT, habit.id)
            putLong(KEY_DAY, next.date)
            putString(KEY_MESSAGE, message)
            if (previous == null) {
                remove(KEY_PREV_STATUS)
            } else {
                putString(KEY_PREV_STATUS, previous.status.name)
                putFloat(KEY_PREV_AMOUNT, previous.amount.toFloat())
                putLong(KEY_PREV_LOGGED_AT, previous.loggedAt ?: 0L)
            }
        }
        _pending.value = true
    }

    /** The waiting change, once; null if there is none or it's from another day. */
    fun take(today: LocalDate = LocalDate.now()): UndoableChange? {
        if (!prefs.contains(KEY_HABIT)) return null
        val habitId = prefs.getLong(KEY_HABIT, 0L)
        val day = prefs.getLong(KEY_DAY, 0L)
        val message = prefs.getString(KEY_MESSAGE, null)
        val previous = prefs.getString(KEY_PREV_STATUS, null)?.let { status ->
            Entry(
                habitId,
                day,
                EntryStatus.valueOf(status),
                prefs.getFloat(KEY_PREV_AMOUNT, 0f).toDouble(),
                prefs.getLong(KEY_PREV_LOGGED_AT, 0L).takeIf { it > 0L },
            )
        }
        prefs.edit { clear() }
        _pending.value = false
        if (message == null || day != today.toEpochDay()) return null
        return UndoableChange(message, habitId, day, previous)
    }

    private companion object {
        const val KEY_HABIT = "habitId"
        const val KEY_DAY = "day"
        const val KEY_MESSAGE = "message"
        const val KEY_PREV_STATUS = "previousStatus"
        const val KEY_PREV_AMOUNT = "previousAmount"
        const val KEY_PREV_LOGGED_AT = "previousLoggedAt"
    }
}
