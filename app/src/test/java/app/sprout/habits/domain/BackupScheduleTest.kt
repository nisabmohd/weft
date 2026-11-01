package app.sprout.habits.domain

import app.sprout.habits.data.DriveFrequency
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    @Test fun dailyBeforeOneAmIsTonight() =
        assertEquals(at(2026, 10, 31, 1), nextBackupSlot(at(2026, 10, 31, 0, 30), DriveFrequency.DAILY))

    @Test fun dailyAtOrAfterOneAmIsTomorrow() {
        assertEquals(at(2026, 11, 1, 1), nextBackupSlot(at(2026, 10, 31, 1), DriveFrequency.DAILY))
        assertEquals(at(2026, 11, 1, 1), nextBackupSlot(at(2026, 10, 31, 21, 45), DriveFrequency.DAILY))
    }

    @Test fun weeklyIsSundayNight() {
        // Saturday 31 Oct → Monday 2 Nov, 1 AM.
        assertEquals(at(2026, 11, 2, 1), nextBackupSlot(at(2026, 10, 31, 14), DriveFrequency.WEEKLY))
        // Monday after 1 AM → the next Monday.
        assertEquals(at(2026, 11, 9, 1), nextBackupSlot(at(2026, 11, 2, 1, 5), DriveFrequency.WEEKLY))
        // Monday before 1 AM (still Sunday night) → tonight.
        assertEquals(at(2026, 11, 2, 1), nextBackupSlot(at(2026, 11, 2, 0, 10), DriveFrequency.WEEKLY))
    }

    @Test fun monthlyIsTheFirst() {
        assertEquals(at(2026, 11, 1, 1), nextBackupSlot(at(2026, 10, 31, 14), DriveFrequency.MONTHLY))
        assertEquals(at(2026, 12, 1, 1), nextBackupSlot(at(2026, 11, 1, 3), DriveFrequency.MONTHLY))
        // Across the year end.
        assertEquals(at(2027, 1, 1, 1), nextBackupSlot(at(2026, 12, 15, 9), DriveFrequency.MONTHLY))
    }

    @Test fun alwaysStrictlyLater() {
        val now = at(2026, 11, 1, 1)
        DriveFrequency.entries.forEach { f -> assert(nextBackupSlot(now, f).isAfter(now)) { "$f" } }
    }
}
