package app.sprout.habits.ui.more

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.DriveFrequency
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.backup.BackupException
import app.sprout.habits.data.backup.BackupFile
import app.sprout.habits.data.backup.BackupManager
import app.sprout.habits.data.backup.DriveAuth
import app.sprout.habits.data.backup.DriveBackup
import app.sprout.habits.data.backup.DriveBackupInfo
import app.sprout.habits.data.backup.DriveResult
import app.sprout.habits.ui.components.ActionRow
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
import app.sprout.habits.ui.components.WeftSheet
import app.sprout.habits.ui.theme.WeftType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What waits while Google's account or consent screen is open. */
private enum class DriveStep { CONNECT, BACK_UP, LIST, DELETE_ALL }

/**
 * Backup & restore in the play and github builds (FEATURE-BACKUP-SCREEN.md 2a / 2b). Not connected:
 * "Back up to Google Drive" with Connect Google Drive (which runs a first backup), then Backup to a
 * file. Connected: the last backup with Back up now (Try again after a failure), Automatic backup
 * (Frequency, Wi-Fi only), Restore (from Drive, from a file) and Other (Export to file, Google
 * account: switch, disconnect, delete backups). See docs/DRIVE-BACKUP.md.
 */
@Composable
fun DriveBackupContent(
    drive: DriveBackup,
    backup: BackupManager,
    settings: Settings,
    repository: SettingsRepository,
    files: FileBackupActions,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val connected = settings.driveAccount != null
    // Saveable: the activity can be recreated (rotation, dark mode) while Google's screen is open.
    var waiting by rememberSaveable { mutableStateOf<DriveStep?>(null) }
    var busy by remember { mutableStateOf<DriveStep?>(null) }
    // From the tap until the upload ends; the upload itself (also the nightly job's) is drive.backingUp.
    var startingBackup by remember { mutableStateOf(false) }
    val uploading by drive.backingUp.collectAsState()
    val backingUp = startingBackup || uploading
    var accountSheet by remember { mutableStateOf(false) }
    var frequencySheet by remember { mutableStateOf(false) }
    var formatSheet by remember { mutableStateOf(false) }
    var restoreSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var backups by remember { mutableStateOf<List<DriveBackupInfo>?>(null) }
    var restoreToken by remember { mutableStateOf<String?>(null) }

    fun say(result: DriveResult) = onMessage(context.driveMessage(result))

    fun done() {
        busy = null
        startingBackup = false
    }

    fun run(step: DriveStep, token: String) {
        busy = step
        scope.launch {
            when (step) {
                DriveStep.CONNECT -> {
                    val result = drive.finishConnect(token)
                    if (result == DriveResult.DONE) {
                        // Connected: the first backup runs straight away.
                        startingBackup = true
                        val first = drive.backUpNow(token)
                        val email = repository.settings.first().driveAccount.orEmpty()
                        if (first == DriveResult.DONE) onMessage(context.getString(R.string.drive_connected_to, email)) else say(first)
                    } else say(result)
                }
                DriveStep.BACK_UP -> say(drive.backUpNow(token))
                DriveStep.LIST -> {
                    val (result, list) = drive.list(token)
                    if (result == DriveResult.DONE) {
                        restoreToken = token
                        backups = list
                    } else {
                        restoreSheet = false
                        say(result)
                    }
                }
                DriveStep.DELETE_ALL -> {
                    val result = drive.deleteAll(token)
                    if (result == DriveResult.DONE) onMessage(context.getString(R.string.drive_deleted)) else say(result)
                }
            }
            done()
        }
    }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val step = waiting ?: return@rememberLauncherForActivityResult
        waiting = null
        val token = drive.tokenFrom(result.data)
        when {
            token != null -> run(step, token)
            // Google Drive left unticked on Google's screen: say what's needed.
            drive.driveRefused(result.data) -> { done(); if (step == DriveStep.LIST) restoreSheet = false; onMessage(context.getString(R.string.drive_refused)) }
            // Backed out of Google's screen: nothing to say.
            drive.backedOut(result.resultCode, result.data) -> { done(); if (step == DriveStep.LIST) restoreSheet = false }
            // Google's screen failed (no network, or no OAuth client for this build): say so.
            else -> { done(); if (step == DriveStep.LIST) restoreSheet = false; onMessage(context.getString(R.string.drive_failed)) }
        }
    }

    fun start(step: DriveStep) {
        // One at a time: a second tap while one runs (or Google's screen is open) does nothing.
        if (busy != null || (step == DriveStep.BACK_UP && uploading)) return
        if (!drive.online()) {
            if (step == DriveStep.LIST) restoreSheet = false
            say(DriveResult.OFFLINE)
            return
        }
        busy = step
        if (step == DriveStep.BACK_UP) startingBackup = true
        scope.launch {
            val auth = if (step == DriveStep.CONNECT) drive.connect() else drive.authorize()
            when (auth) {
                is DriveAuth.Token -> run(step, auth.value)
                is DriveAuth.NeedsConsent -> { waiting = step; consent.launch(auth.request) }
                DriveAuth.DriveRefused -> {
                    done()
                    if (step == DriveStep.LIST) restoreSheet = false
                    onMessage(context.getString(R.string.drive_refused))
                }
                DriveAuth.Failed -> {
                    done()
                    if (step == DriveStep.LIST) restoreSheet = false
                    onMessage(context.getString(R.string.drive_failed))
                }
            }
        }
    }

    if (!connected) {
        StatusCard(
            icon = R.drawable.ic_cloud,
            tile = colors.surfaceContainerHigh,
            ink = colors.onSurface,
            title = stringResource(R.string.drive_connect_title),
            subtitle = stringResource(R.string.drive_connect_text),
        ) {
            StatusButton(stringResource(R.string.drive_connect_button), icon = null, filled = true, enabled = busy == null, busy = busy == DriveStep.CONNECT || backingUp) {
                start(DriveStep.CONNECT)
            }
        }
        SectionLabel(stringResource(R.string.backup_to_file))
        SettingsGroup(
            { SettingsRow(stringResource(R.string.export_to_file), stringResource(R.string.export_anywhere_desc), onClick = { formatSheet = true }) { Chevron() } },
            { SettingsRow(stringResource(R.string.import_from_file), stringResource(R.string.import_desc), onClick = files.import) { Chevron() } },
        )
    } else {
        val failed = settings.lastBackupError != null && !backingUp
        val account = stringResource(R.string.drive_account_line, settings.driveAccount.orEmpty())
        when {
            failed -> StatusCard(
                icon = R.drawable.ic_error,
                tile = colors.errorContainer,
                ink = colors.onErrorContainer,
                title = stringResource(R.string.backups_last_failed),
                subtitle = stringResource(failureReason(settings.lastBackupError)),
            ) { StatusButton(stringResource(R.string.try_again), R.drawable.ic_cloud_upload, filled = false) { start(DriveStep.BACK_UP) } }
            else -> {
                val at = settings.lastBackupAt
                StatusCard(
                    icon = if (at != null) R.drawable.ic_check else R.drawable.ic_cloud,
                    tile = if (at != null) colors.secondaryContainer else colors.surfaceContainerHigh,
                    ink = if (at != null) colors.onSecondaryContainer else colors.onSurface,
                    title = at?.let { dayLine(it, R.string.backed_up_today, R.string.backed_up_yesterday, R.string.backed_up_date) }
                        ?: stringResource(R.string.drive_no_backups_yet),
                    subtitle = account,
                ) {
                    StatusButton(
                        stringResource(if (backingUp) R.string.drive_backing_up else R.string.drive_back_up_now),
                        R.drawable.ic_cloud_upload,
                        filled = false,
                        enabled = !backingUp,
                        busy = backingUp,
                    ) { start(DriveStep.BACK_UP) }
                }
            }
        }

        SectionLabel(stringResource(R.string.automatic_backup))
        SettingsGroup(
            {
                SettingsRow(stringResource(R.string.drive_frequency_short), onClick = { frequencySheet = true }) {
                    TrailingValue(stringResource(frequencyName(settings.driveFrequency)))
                    Chevron()
                }
            },
            {
                SwitchRow(stringResource(R.string.drive_wifi_only), stringResource(R.string.drive_wifi_only_desc), settings.driveWifiOnly) { on ->
                    scope.launch {
                        repository.setDriveWifiOnly(on)
                        drive.reschedule()
                    }
                }
            },
        )

        SectionLabel(stringResource(R.string.restore))
        SettingsGroup(
            {
                SettingsRow(stringResource(R.string.drive_restore_title), restoreLine(settings), onClick = {
                    backups = null
                    restoreSheet = true
                    start(DriveStep.LIST)
                }) { Chevron() }
            },
            { SettingsRow(stringResource(R.string.import_from_file), stringResource(R.string.import_short_desc), onClick = files.import) { Chevron() } },
        )

        SectionLabel(stringResource(R.string.backup_other))
        SettingsGroup(
            { SettingsRow(stringResource(R.string.export_to_file), stringResource(R.string.export_anywhere_desc), onClick = { formatSheet = true }) { Chevron() } },
            { SettingsRow(stringResource(R.string.google_account), stringResource(R.string.google_account_desc), onClick = { accountSheet = true }) { Chevron() } },
        )
    }

    if (formatSheet) {
        ExportFormatSheet(selected = null, onPick = { format -> formatSheet = false; files.export(format) }, onDismiss = { formatSheet = false })
    }

    if (accountSheet) {
        // Built like the Journal's note actions sheet: the title and a line, then plain action rows.
        WeftSheet(onDismissRequest = { accountSheet = false }) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.google_account), style = WeftType.title, color = colors.onSurface, modifier = Modifier.semantics { heading() })
                Text(settings.driveAccount.orEmpty(), style = WeftType.supporting, color = colors.onSurfaceVariant)
            }
            ActionRow(R.drawable.ic_cloud, stringResource(R.string.drive_switch_account), colors.onSurface) {
                accountSheet = false
                start(DriveStep.CONNECT)
            }
            ActionRow(R.drawable.ic_close, stringResource(R.string.drive_disconnect), colors.onSurface) {
                accountSheet = false
                scope.launch {
                    drive.disconnect()
                    onMessage(context.getString(R.string.drive_disconnected))
                }
            }
            ActionRow(R.drawable.ic_delete, stringResource(R.string.drive_delete_backups), colors.error) { accountSheet = false; confirmDelete = true }
            Spacer(Modifier.padding(bottom = 12.dp).navigationBarsPadding())
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.drive_delete_title)) },
            text = { Text(stringResource(R.string.drive_delete_text, settings.driveAccount.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; start(DriveStep.DELETE_ALL) }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (frequencySheet) {
        ChoiceSheet(title = stringResource(R.string.drive_frequency), onDismiss = { frequencySheet = false }) {
            DriveFrequency.entries.forEach { f ->
                ChoiceRow(stringResource(frequencyName(f)), stringResource(frequencyDesc(f)), settings.driveFrequency == f) {
                    frequencySheet = false
                    scope.launch {
                        repository.setDriveFrequency(f)
                        drive.reschedule()
                    }
                }
            }
        }
    }

    if (restoreSheet) {
        RestoreSheet(
            backups = backups,
            onDismiss = { restoreSheet = false; backups = null },
            onRestore = { picked, file ->
                restoreSheet = false
                scope.launch {
                    try {
                        backup.restore(file)
                        onMessage(context.getString(R.string.drive_restored_from, shortDate(picked.createdAt)))
                    } catch (e: BackupException) {
                        onMessage(context.backupError(e))
                    } catch (_: android.database.sqlite.SQLiteException) {
                        onMessage(context.getString(R.string.restore_failed))
                    }
                }
            },
            download = { info ->
                val token = restoreToken ?: return@RestoreSheet null
                try {
                    val (result, file) = drive.download(token, info.id)
                    if (result != DriveResult.DONE) say(result)
                    file
                } catch (e: BackupException) {
                    onMessage(context.backupError(e))
                    null
                }
            },
        )
    }
}

/** "7 backups, newest today", once a backup or the Restore sheet has counted them. */
@Composable
private fun restoreLine(settings: Settings): String {
    val count = settings.driveBackupCount ?: return stringResource(R.string.drive_restore_desc)
    if (count == 0) return stringResource(R.string.drive_no_backups_yet)
    val backups = pluralStringResource(R.plurals.drive_backup_count, count, count)
    val at = settings.lastBackupAt ?: return backups
    val newest = dayLine(at, R.string.drive_newest_today, R.string.drive_newest_yesterday, R.string.drive_newest_date)
    return stringResource(R.string.drive_restore_summary, backups, newest)
}

/**
 * Restore from Drive: the backups newest first (the newest picked), Restore, then "Replace
 * current data?". [backups] is null while they load.
 */
@Composable
private fun RestoreSheet(
    backups: List<DriveBackupInfo>?,
    onDismiss: () -> Unit,
    onRestore: (DriveBackupInfo, BackupFile) -> Unit,
    download: suspend (DriveBackupInfo) -> BackupFile?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf<Pair<DriveBackupInfo, BackupFile>?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(backups) { if (picked == null) picked = backups?.firstOrNull()?.id }
    val chosen = backups?.firstOrNull { it.id == picked }

    ChoiceSheet(
        title = stringResource(R.string.drive_restore_title),
        onDismiss = onDismiss,
        footer = {
            // As the filter sheet's Show button: 48 dp, 24 dp from the sides.
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        val info = chosen ?: return@Button
                        loading = true
                        scope.launch {
                            download(info)?.let { confirming = info to it }
                            loading = false
                        }
                    },
                    enabled = chosen != null && !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(stringResource(R.string.drive_restore), style = WeftType.label)
                }
            }
        },
    ) {
        Text(
            stringResource(R.string.drive_restore_text),
            style = WeftType.supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        )
        when {
            backups == null -> Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(32.dp))
            }
            backups.isEmpty() -> Text(
                stringResource(R.string.drive_no_backup),
                style = WeftType.body,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            )
            else -> backups.forEach { info ->
                val habits = pluralStringResource(R.plurals.habit_count, info.habits, info.habits)
                val meta = listOf(info.device, habits, Formatter.formatShortFileSize(context, info.sizeBytes)).filter { it.isNotBlank() }
                ChoiceRow(backupTime(info.createdAt), meta.joinToString(" · "), info.id == picked) { picked = info.id }
            }
        }
    }

    confirming?.let { (info, file) ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(R.string.drive_replace_title)) },
            text = { Text(stringResource(R.string.drive_replace_text)) },
            confirmButton = { TextButton(onClick = { confirming = null; onRestore(info, file) }) { Text(stringResource(R.string.replace)) } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun frequencyName(f: DriveFrequency) = when (f) {
    DriveFrequency.DAILY -> R.string.drive_daily
    DriveFrequency.WEEKLY -> R.string.drive_weekly
    DriveFrequency.MONTHLY -> R.string.drive_monthly
    DriveFrequency.OFF -> R.string.drive_off
}

private fun frequencyDesc(f: DriveFrequency) = when (f) {
    DriveFrequency.DAILY -> R.string.drive_daily_desc
    DriveFrequency.WEEKLY -> R.string.drive_weekly_desc
    DriveFrequency.MONTHLY -> R.string.drive_monthly_desc
    DriveFrequency.OFF -> R.string.drive_off_desc
}

/** "Today, 9:12 AM", "Yesterday, 2:04 AM", "27 Oct, 2:01 AM". */
@Composable
private fun backupTime(at: Long): String {
    return dayLine(at, R.string.drive_time_today, R.string.drive_time_yesterday, R.string.drive_time_date, timeOnOtherDays = true)
}

private fun android.content.Context.driveMessage(result: DriveResult): String = getString(
    when (result) {
        DriveResult.DONE -> R.string.drive_backed_up
        DriveResult.NO_BACKUP -> R.string.drive_no_backup
        DriveResult.OFFLINE -> R.string.drive_offline
        DriveResult.WIFI_NEEDED -> R.string.drive_wifi_needed
        DriveResult.FULL -> R.string.drive_full
        DriveResult.NEEDS_CONSENT, DriveResult.FAILED -> R.string.drive_failed
    },
)
