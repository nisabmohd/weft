package app.sprout.habits.ui

import app.sprout.habits.TestStrings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.Note
import app.sprout.habits.data.TrackType
import app.sprout.habits.ui.components.MarkKind
import app.sprout.habits.ui.components.NoteCardUi
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The note card's line (design 2026-11-11b): "outcome · time", where the time says "written …" when
 * the note was written on a later day than the one it is about.
 */
class NoteCardTextTest {
    private val savedLocale = Locale.getDefault()
    private val savedZone = TimeZone.getDefault()

    @Before fun setUp() {
        Locale.setDefault(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    }

    @After fun tearDown() {
        Locale.setDefault(savedLocale)
        TimeZone.setDefault(savedZone)
    }

    private val read = Habit(id = 1, name = "Read", icon = "menu_book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")
    private val wake = Habit(id = 2, name = "Wake up", icon = "alarm", colorHue = 38)
    private val about = LocalDate.of(2026, 9, 26) // a Saturday

    private fun writtenOn(day: LocalDate, at: LocalTime = LocalTime.of(21, 45)) =
        day.atTime(at).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun card(habit: Habit = read, entry: Entry? = null, written: LocalDate = about, today: LocalDate = about.plusDays(1)) =
        NoteCardUi.of(Note(id = 9, habitId = habit.id, date = about.toEpochDay(), text = "x", updatedAt = writtenOn(written)), habit, entry, today, TestStrings)

    @Test fun aNoteWrittenOnItsOwnDayShowsOnlyTheTime() {
        val time = card().time
        assertFalse(time, time.startsWith("written"))
        assertTrue(time, time.contains("9:45") || time.contains("21:45"))
    }

    @Test fun aNoteWrittenTheNextDaySaysWhichWeekday() {
        val time = card(written = about.plusDays(1), today = about.plusDays(1)).time
        assertTrue(time, time.startsWith("written Sun, "))
    }

    @Test fun aNoteWrittenSixDaysLaterStillUsesTheWeekday() {
        val time = card(written = about.plusDays(6), today = about.plusDays(6)).time
        assertTrue(time, time.startsWith("written Fri, "))
    }

    @Test fun aNoteWrittenOverAWeekLaterUsesTheDate() {
        val time = card(written = about.plusDays(6), today = about.plusDays(13)).time
        assertTrue(time, time.startsWith("written 2 Oct, "))
    }

    @Test fun aNoteWrittenInAnotherYearSaysTheYear() {
        val time = card(written = LocalDate.of(2025, 12, 31), today = LocalDate.of(2026, 9, 27)).time
        assertTrue(time, time.startsWith("written 31 Dec 2025, "))
    }

    @Test fun doneOnACheckOffHabitIsJustDone() {
        val c = card(wake, Entry(wake.id, about.toEpochDay(), EntryStatus.DONE))
        assertEquals("Done", c.outcome)
        assertEquals(MarkKind.DONE, c.mark.kind)
    }

    @Test fun doneOnAMeasuredHabitSaysTheAmount() {
        val c = card(entry = Entry(read.id, about.toEpochDay(), EntryStatus.DONE, 20.0))
        assertEquals("Done · 20 pages", c.outcome)
    }

    @Test fun partialSaysHowFarAndFillsThePie() {
        val c = card(entry = Entry(read.id, about.toEpochDay(), EntryStatus.PARTIAL, 8.0))
        assertEquals("8 of 20 pages", c.outcome)
        assertEquals(MarkKind.PARTIAL, c.mark.kind)
        assertEquals(0.4f, c.mark.fraction, 0.001f)
    }

    @Test fun aPartialOverTheTargetNeverOverfillsThePie() {
        val c = card(entry = Entry(read.id, about.toEpochDay(), EntryStatus.PARTIAL, 30.0))
        assertEquals(1f, c.mark.fraction, 0.001f)
    }

    @Test fun skippedSaysSkipped() {
        val c = card(entry = Entry(read.id, about.toEpochDay(), EntryStatus.SKIP))
        assertEquals("Skipped", c.outcome)
        assertEquals(MarkKind.SKIP, c.mark.kind)
    }

    @Test fun aPastDayWithNothingLoggedCountsAsSkipped() {
        val c = card(entry = null, today = about.plusDays(1))
        assertEquals("Skipped", c.outcome)
        assertEquals(MarkKind.SKIP, c.mark.kind)
    }

    @Test fun todayWithNothingLoggedIsNotSkippedYet() {
        val c = card(entry = null, today = about)
        assertEquals("Not logged yet", c.outcome)
        assertEquals(MarkKind.OPEN_TODAY, c.mark.kind)
    }
}
