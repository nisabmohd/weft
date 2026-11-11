package app.sprout.habits.notify

import app.sprout.habits.data.Habit
import app.sprout.habits.data.ReminderType
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A notification reminder goes into the shade quietly when another habit's alarm rings at the same
 * minute (user, 2026-11-11: a notification and the full-screen alarm came a second apart).
 */
class AlarmTogetherTest {
    private val sunday = LocalDate.of(2026, 11, 15)
    private val read = Habit(id = 1, name = "Read", icon = "book", colorHue = 215, reminderMinutes = 9 * 60)
    private val wake = Habit(id = 2, name = "Wake up", icon = "alarm", colorHue = 38, reminderMinutes = 9 * 60, reminderType = ReminderType.ALARM)

    private fun due(habit: Habit = read, others: List<Habit> = listOf(read, wake), settled: Set<Long> = emptySet()) =
        ReminderNotifier.alarmDueWith(habit, others, sunday, settled)

    @Test fun anotherHabitsAlarmAtTheSameMinuteTakesOver() = assertTrue(due())

    @Test fun anAlarmAtAnotherMinuteDoesNot() = assertFalse(due(others = listOf(read, wake.copy(reminderMinutes = 9 * 60 + 5))))

    @Test fun anotherNotificationAtTheSameMinuteDoesNot() =
        assertFalse(due(others = listOf(read, wake.copy(reminderType = ReminderType.NOTIFICATION))))

    @Test fun anAlarmAlreadyDoneOrSkippedWontRing() = assertFalse(due(settled = setOf(wake.id)))

    @Test fun anAlarmNotScheduledTodayWontRing() =
        assertFalse(due(others = listOf(read, wake.copy(daysMask = 0b0011111)))) // Mon–Fri, today is Sunday

    @Test fun anArchivedHabitsAlarmWontRing() = assertFalse(due(others = listOf(read, wake.copy(archived = true))))

    @Test fun theHabitItselfDoesntCount() = assertFalse(due(habit = wake, others = listOf(wake)))

    @Test fun aHabitWithoutAReminderTimeIsNeverQuieted() = assertFalse(due(habit = read.copy(reminderMinutes = null)))
}
