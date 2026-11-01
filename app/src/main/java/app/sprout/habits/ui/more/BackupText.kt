package app.sprout.habits.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.data.Settings
import app.sprout.habits.data.backup.DriveResult
import app.sprout.habits.ui.datePattern
import app.sprout.habits.ui.today.TodayViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun localDateTime(at: Long) = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())

internal fun timeOf(at: Long): String = localDateTime(at).let { TodayViewModel.formatTime(it.hour * 60 + it.minute) }

/** "27 Oct". */
internal fun shortDate(at: Long): String = localDateTime(at).toLocalDate().format(datePattern("d MMM"))

/**
 * One of three strings by the day of [at]: [today] and [yesterday] take the time, [other] the date
 * ("27 Oct", or "27 Oct, 9:12 AM" when [timeOnOtherDays]).
 */
@Composable
internal fun dayLine(at: Long, today: Int, yesterday: Int, other: Int, timeOnOtherDays: Boolean = false): String {
    val day = localDateTime(at).toLocalDate()
    val now = LocalDate.now()
    return when (day) {
        now -> stringResource(today, timeOf(at))
        now.minusDays(1) -> stringResource(yesterday, timeOf(at))
        else -> stringResource(other, if (timeOnOtherDays) "${shortDate(at)}, ${timeOf(at)}" else shortDate(at))
    }
}

/** "Last backup today, 9:12 AM", "… yesterday, 2:04 AM" or "… 27 Oct". */
@Composable
internal fun lastBackupLine(at: Long) = dayLine(at, R.string.drive_last_today, R.string.drive_last_yesterday, R.string.drive_last_date)

/** "Last exported today, 9:12 AM", "… yesterday, 2:04 AM", "… 27 Oct" (with the time on [withTime]). */
@Composable
internal fun lastExportLine(at: Long, withTime: Boolean = false) =
    dayLine(at, R.string.last_exported_today, R.string.last_exported_yesterday, R.string.last_exported_date, withTime)

/** More → Backups' line, also what TalkBack reads for the row. */
@Composable
internal fun backupsRowLine(settings: Settings, driveEnabled: Boolean, backingUp: Boolean): String = when {
    !driveEnabled -> settings.lastExportAt?.let { lastExportLine(it) } ?: stringResource(R.string.drive_no_backups_yet)
    settings.driveAccount == null -> stringResource(R.string.backups_drive_off)
    backingUp -> stringResource(R.string.drive_backing_up)
    settings.lastBackupError != null -> stringResource(R.string.backups_last_failed)
    settings.lastBackupAt != null -> lastBackupLine(settings.lastBackupAt)
    else -> stringResource(R.string.drive_no_backups_yet)
}

/** Why the last backup failed, under "Last backup failed". */
internal fun failureReason(name: String?) = when (name) {
    DriveResult.OFFLINE.name -> R.string.drive_reason_offline
    DriveResult.FULL.name -> R.string.drive_reason_full
    DriveResult.NEEDS_CONSENT.name -> R.string.drive_reason_consent
    else -> R.string.drive_reason_failed
}
