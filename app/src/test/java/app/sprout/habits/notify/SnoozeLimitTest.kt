package app.sprout.habits.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** An alarm snoozes three times a day (user, 2026-11-11), counted per habit and day. */
class SnoozeLimitTest {
    @Test fun threeSnoozesADay() = assertEquals(3, ReminderActions.MAX_SNOOZES)

    @Test fun eachSnoozeUsesOneUp() {
        assertEquals(3, ReminderActions.snoozesLeft(0))
        assertEquals(2, ReminderActions.snoozesLeft(1))
        assertEquals(1, ReminderActions.snoozesLeft(2))
        assertEquals(0, ReminderActions.snoozesLeft(3))
    }

    @Test fun neverBelowZero() = assertEquals(0, ReminderActions.snoozesLeft(7))

    @Test fun eachHabitCountsOnItsOwn() =
        assertNotEquals(ReminderActions.snoozeKey(1, 20_000), ReminderActions.snoozeKey(2, 20_000))

    @Test fun theNextDayStartsFresh() =
        assertNotEquals(ReminderActions.snoozeKey(1, 20_000), ReminderActions.snoozeKey(1, 20_001))

    @Test fun keysDontCollideAcrossIdsAndDays() =
        // "1@120000" vs "11@20000": the separator keeps them apart.
        assertNotEquals(ReminderActions.snoozeKey(1, 120_000), ReminderActions.snoozeKey(11, 20_000))
}
