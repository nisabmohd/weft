package app.sprout.habits.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.data.ExportFormat
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.backup.BackupException
import app.sprout.habits.data.backup.BackupFile
import app.sprout.habits.data.backup.BackupManager
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Export to file and Import from file, through the system file picker (no storage permission). */
class FileBackupActions(
    /** Opens the file picker to save a backup in [format]. */
    val export: (ExportFormat) -> Unit,
    /** Opens the file picker for a JSON backup, then asks before replacing the data. */
    val import: () -> Unit,
)

/**
 * The file backup flows of Backup & restore. Also shows the "Replace current data?" dialog after a
 * file is picked for import. A finished export is remembered for the foss build's "Last exported".
 */
@Composable
fun rememberFileBackup(backup: BackupManager, repository: SettingsRepository, onMessage: (String) -> Unit): FileBackupActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<BackupFile?>(null) }

    fun write(uri: Uri?, block: suspend (java.io.OutputStream) -> Unit, done: String) {
        uri ?: return
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { block(it) }
                }
            }
            if (result.isSuccess) repository.setExported()
            onMessage(if (result.isSuccess) done else context.getString(R.string.save_failed))
        }
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, { backup.exportJson(it) }, context.getString(R.string.backup_saved))
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, { backup.exportCsv(it) }, context.getString(R.string.csv_saved))
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pending = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { BackupManager.parse(it) }
                }
            } catch (e: BackupException) {
                onMessage(context.backupError(e))
            } catch (_: java.io.IOException) {
                onMessage(context.getString(R.string.read_failed))
            }
        }
    }

    pending?.let { file ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.replace_data_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.replace_data_text,
                        pluralStringResource(R.plurals.habit_count, file.habits.size, file.habits.size),
                        pluralStringResource(R.plurals.logged_day_count, file.entries.size, file.entries.size),
                        pluralStringResource(R.plurals.note_count, file.notes.size, file.notes.size),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        try {
                            backup.restore(file)
                            onMessage(context.getString(R.string.backup_restored))
                        } catch (e: BackupException) {
                            onMessage(context.backupError(e))
                        } catch (_: android.database.sqlite.SQLiteException) {
                            onMessage(context.getString(R.string.restore_failed))
                        }
                    }
                }) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    return remember(exportJson, exportCsv, import) {
        FileBackupActions(
            export = { format ->
                when (format) {
                    ExportFormat.JSON -> exportJson.launch("weft-backup-${LocalDate.now()}.json")
                    ExportFormat.CSV -> exportCsv.launch("weft-${LocalDate.now()}.csv")
                }
            },
            import = { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        )
    }
}

/** File format: a JSON backup (can be imported again) or CSV (for spreadsheets). */
@Composable
fun ExportFormatSheet(selected: ExportFormat?, onPick: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    ChoiceSheet(title = stringResource(R.string.export_format), onDismiss = onDismiss) {
        ExportFormat.entries.forEach { format ->
            ChoiceRow(stringResource(formatName(format)), stringResource(formatDesc(format)), selected == format) { onPick(format) }
        }
    }
}

fun formatName(format: ExportFormat) = when (format) {
    ExportFormat.JSON -> R.string.json
    ExportFormat.CSV -> R.string.csv
}

private fun formatDesc(format: ExportFormat) = when (format) {
    ExportFormat.JSON -> R.string.export_json_desc
    ExportFormat.CSV -> R.string.export_csv_desc
}

internal fun android.content.Context.backupError(e: BackupException): String = when (e.reason) {
    BackupException.Reason.DAMAGED -> getString(R.string.backup_error_damaged)
    BackupException.Reason.NOT_WEFT -> getString(R.string.backup_error_not_weft)
    BackupException.Reason.NEWER -> getString(R.string.backup_error_newer)
    BackupException.Reason.BAD_DATE -> getString(R.string.backup_error_date, e.detail)
}
