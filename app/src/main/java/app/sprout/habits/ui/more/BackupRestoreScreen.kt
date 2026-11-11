package app.sprout.habits.ui.more

import app.sprout.habits.ui.theme.BorderThick
import app.sprout.habits.ui.theme.IconSizeChip
import app.sprout.habits.ui.theme.IconSizeSmall
import app.sprout.habits.ui.theme.ItemGap
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.Space16
import app.sprout.habits.ui.theme.Space24
import app.sprout.habits.ui.theme.Space4
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.TouchTarget
import app.sprout.habits.ui.components.TitleLineGap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.backup.BackupManager
import app.sprout.habits.data.backup.DriveBackup
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.theme.WeftType
import app.sprout.habits.ui.today.UndoSnackbarHost
import kotlinx.coroutines.launch

/**
 * More → Backups (FEATURE-BACKUP-SCREEN.md, 2026-10-31): a status card with the one action that
 * matters now, then grouped rows. The play and github builds show Google Drive
 * ([DriveBackupContent]); foss shows file backup only. See docs/DRIVE-BACKUP.md.
 */
@Composable
fun BackupRestoreScreen(
    settings: Settings,
    repository: SettingsRepository,
    backup: BackupManager,
    drive: DriveBackup,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val message: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val files = rememberFileBackup(backup, repository, message)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(stringResource(R.string.backup_restore), onBack)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = Space16, end = Space16, top = Space16, bottom = Space24).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(ItemGap),
            ) {
                if (drive.enabled) DriveBackupContent(drive, backup, settings, repository, files, message)
                else FileBackupContent(settings, repository, files)
            }
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(Space16))
    }
}

/**
 * The foss build: "Last exported …" with Export now (in the chosen file format), Import from file,
 * and the File format. No Google, no network.
 */
@Composable
private fun FileBackupContent(settings: Settings, repository: SettingsRepository, files: FileBackupActions) {
    val scope = rememberCoroutineScope()
    var pickingFormat by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    StatusCard(
        icon = R.drawable.ic_smartphone,
        tile = colors.secondaryContainer,
        ink = colors.onSecondaryContainer,
        title = settings.lastExportAt?.let { lastExportLine(it, withTime = true) } ?: stringResource(R.string.drive_no_backups_yet),
        subtitle = stringResource(R.string.file_backup_text),
    ) {
        StatusButton(stringResource(R.string.export_now), R.drawable.ic_download, filled = false) { files.export(settings.exportFormat) }
    }

    SectionLabel(stringResource(R.string.restore))
    SettingsCard {
        SettingsRow(stringResource(R.string.import_from_file), stringResource(R.string.import_desc), onClick = files.import) { Chevron() }
    }

    SectionLabel(stringResource(R.string.export_section))
    SettingsCard {
        SettingsRow(stringResource(R.string.export_format), onClick = { pickingFormat = true }) {
            TrailingValue(stringResource(formatName(settings.exportFormat)))
            Chevron()
        }
    }

    if (pickingFormat) {
        ExportFormatSheet(
            selected = settings.exportFormat,
            onPick = { format -> pickingFormat = false; scope.launch { repository.setExportFormat(format) } },
            onDismiss = { pickingFormat = false },
        )
    }
}

/**
 * The card at the top of Backup & restore: the 44 dp tile, the state ([title], read again by TalkBack
 * when it changes) with a line under it, then one full-width [button].
 */
@Composable
internal fun StatusCard(icon: Int, tile: Color, ink: Color, title: String, subtitle: String, button: @Composable () -> Unit) {
    SettingsCard {
        // Card header → its content is 12 dp, as in every card (docs/SPACING.md).
        Column(Modifier.padding(Space16), verticalArrangement = Arrangement.spacedBy(Space12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space12)) {
                HabitTile(icon, tile, ink)
                Column(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
                    Text(title, style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            button()
        }
    }
}

/**
 * The status card's button: tonal (secondaryContainer) or [filled] primary, full width and 48 dp
 * tall like every full-width button in the app (Set time, the sheets' Show buttons).
 */
@Composable
internal fun StatusButton(
    text: String,
    icon: Int?,
    filled: Boolean,
    enabled: Boolean = true,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().height(TouchTarget)
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) CircularProgressIndicator(Modifier.size(IconSizeChip), strokeWidth = BorderThick, color = LocalContentColor.current)
            else if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSizeChip))
            if (busy || icon != null) Spacer(Modifier.size(Space8))
            Text(text, style = WeftType.label)
        }
    }
    if (filled) Button(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    else FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
}

@Composable
internal fun Chevron() =
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.padding(start = Space4).size(IconSizeSmall))
