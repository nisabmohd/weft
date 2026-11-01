package app.sprout.habits.domain

import app.sprout.habits.data.DriveFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Scheduled Drive backups start after this local time, overnight. */
private val NIGHT: LocalTime = LocalTime.of(1, 0)

/**
 * The next scheduled Drive backup strictly after [now]: 1 AM tonight or tomorrow (Daily), the
 * coming Monday 1 AM, i.e. Sunday night (Weekly), or 1 AM on the 1st (Monthly). Off has no slot;
 * it is treated as Daily so callers needn't special-case it (they cancel instead).
 */
fun nextBackupSlot(now: ZonedDateTime, frequency: DriveFrequency): ZonedDateTime {
    val today: LocalDate = now.toLocalDate()
    fun at(day: LocalDate): ZonedDateTime = day.atTime(NIGHT).atZone(now.zone)
    return when (frequency) {
        DriveFrequency.DAILY, DriveFrequency.OFF -> at(today).takeIf { it.isAfter(now) } ?: at(today.plusDays(1))
        DriveFrequency.WEEKLY -> at(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))).takeIf { it.isAfter(now) }
            ?: at(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)))
        DriveFrequency.MONTHLY -> at(today.withDayOfMonth(1)).takeIf { it.isAfter(now) } ?: at(today.withDayOfMonth(1).plusMonths(1))
    }
}
