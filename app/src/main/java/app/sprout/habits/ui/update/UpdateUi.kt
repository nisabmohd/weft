package app.sprout.habits.ui.update

import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.components.sheetBottomPadding
import app.sprout.habits.ui.components.ButtonPair
import app.sprout.habits.ui.components.ButtonRole
import app.sprout.habits.ui.components.WeftButton
import app.sprout.habits.ui.components.SheetHeader
import app.sprout.habits.ui.components.SheetSidePadding
import app.sprout.habits.ui.components.WeftSheet
import app.sprout.habits.data.update.AppUpdate
import app.sprout.habits.data.update.DownloadState
import app.sprout.habits.data.update.UpdateCheck
import app.sprout.habits.data.update.UpdateManager
import app.sprout.habits.data.update.PlayUpdateCheck
import app.sprout.habits.data.update.PlayUpdates
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import app.sprout.habits.support.LaunchPrompts
import app.sprout.habits.ui.openUrl
import kotlinx.coroutines.launch

/** The version without the build suffix: "1.0.0" rather than "1.0.0-github". */
private val currentVersion get() = BuildConfig.VERSION_NAME.substringBefore('-')

/**
 * "Update available": the new version, up to five What's new lines, then Update (download with
 * progress, then Android's installer) or Later.
 */
@Composable
fun UpdateSheet(
    update: AppUpdate,
    updates: UpdateManager,
    onLater: () -> Unit,
    onDismiss: () -> Unit,
    /** The installed version as shown; only the debug sheet preview sets it. */
    currentVersionOverride: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var state by remember { mutableStateOf<DownloadState?>(null) }
    // Checked again on return from "Install unknown apps", so the hint goes once it's allowed.
    var canInstall by remember { mutableStateOf(updates.canInstall()) }
    LifecycleResumeEffect(Unit) {
        canInstall = updates.canInstall()
        onPauseOrDispose {}
    }

    // A sheet like every prompt (`update-sheet.png`): the header, What's new in a card, then
    // Later | Update. The notes scroll on their own so the buttons stay at the bottom.
    WeftSheet(onDismissRequest = onDismiss) {
        SheetHeader(stringResource(R.string.update_available), stringResource(R.string.update_version_line, update.version, currentVersionOverride ?: currentVersion))
        if (update.notes.isNotEmpty()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = SheetSidePadding)
                    .fillMaxWidth()
                    .background(colors.surfaceContainerHigh, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    // 16 dp inside, like every card.
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.whats_new), style = WeftType.cardTitle, color = colors.onSurface)
                update.notes.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 9.dp).size(6.dp).background(colors.primary, CircleShape))
                        Text(line, style = WeftType.body, color = colors.onSurface, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
        Column(
            Modifier.padding(horizontal = SheetSidePadding).padding(top = 20.dp).sheetBottomPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is DownloadState.Progress -> {
                    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val fraction = s.fraction
                        if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        Text(
                            if (fraction == null) stringResource(R.string.downloading) else stringResource(R.string.downloading_percent, (fraction * 100).toInt()),
                            style = WeftType.supporting,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    WeftButton(stringResource(R.string.later), ButtonRole.Secondary, onClick = onLater, modifier = Modifier.fillMaxWidth())
                }
                is DownloadState.Ready -> {
                    if (!canInstall) {
                        Text(stringResource(R.string.allow_installs_hint), style = WeftType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                    }
                    ButtonPair(stringResource(R.string.later), onLater, stringResource(R.string.install), {
                        if (updates.canInstall()) updates.install(s.apk) else updates.openInstallPermission()
                    })
                }
                DownloadState.Failed -> {
                    Text(stringResource(R.string.update_failed), style = WeftType.supporting, color = colors.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    ButtonPair(stringResource(R.string.later), onLater, stringResource(R.string.open_release_page), { openUrl(context, update.pageUrl); onDismiss() })
                }
                null -> ButtonPair(stringResource(R.string.later), onLater, stringResource(R.string.update), {
                    if (update.apkUrl == null) {
                        openUrl(context, update.pageUrl)
                        onDismiss()
                    } else {
                        scope.launch { updates.download(update).collect { state = it } }
                    }
                })
            }
        }
    }
}

private enum class CheckState { IDLE, CHECKING, LATEST, ERROR }

/**
 * About's "Check for updates" pill: Checking… → "You're on the latest version" or the update. The
 * github build shows its Update dialog; the play build hands over to Google Play's update screen.
 * The F-Droid build says where its updates come from instead.
 */
@Composable
fun CheckForUpdates(updates: UpdateManager, play: PlayUpdates) {
    if (play.enabled) { PlayCheckForUpdates(play); return }
    if (!updates.enabled) {
        if (!BuildConfig.PLAY_STORE) {
            Text(stringResource(R.string.updates_from_fdroid), style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(CheckState.IDLE) }
    var found by remember { mutableStateOf<AppUpdate?>(null) }
    UpdatesPill(state) {
        if (state == CheckState.LATEST) { state = CheckState.IDLE; return@UpdatesPill }
        state = CheckState.CHECKING
        scope.launch {
            state = when (val r = updates.check()) {
                is UpdateCheck.Available -> { found = r.update; CheckState.IDLE }
                UpdateCheck.UpToDate -> CheckState.LATEST
                UpdateCheck.Failed -> CheckState.ERROR
            }
        }
    }
    found?.let { update ->
        UpdateSheet(
            update = update,
            updates = updates,
            onLater = { found = null; scope.launch { updates.later(update) } },
            onDismiss = { found = null },
        )
    }
}

/** The play build's pill: asks Google Play, and opens its update screen when there is one. */
@Composable
private fun PlayCheckForUpdates(play: PlayUpdates) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(CheckState.IDLE) }
    // On success Play restarts the app into the new version; anything else is the user backing out.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    UpdatesPill(state) {
        if (state == CheckState.LATEST) { state = CheckState.IDLE; return@UpdatesPill }
        state = CheckState.CHECKING
        scope.launch {
            state = when (play.check()) {
                PlayUpdateCheck.AVAILABLE -> if (play.start(launcher)) CheckState.IDLE else CheckState.ERROR
                PlayUpdateCheck.UP_TO_DATE -> CheckState.LATEST
                PlayUpdateCheck.FAILED -> CheckState.ERROR
            }
        }
    }
}

@Composable
private fun UpdatesPill(state: CheckState, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val latest = state == CheckState.LATEST
    Button(
        onClick = onClick,
        enabled = state != CheckState.CHECKING,
        modifier = Modifier.heightIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (latest) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = when {
                latest -> colors.onPrimaryContainer
                state == CheckState.ERROR -> colors.error
                else -> colors.onSurface
            },
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurface,
        ),
    ) {
        when (state) {
            CheckState.CHECKING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onSurface)
            CheckState.LATEST -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
            else -> Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(
                when (state) {
                    CheckState.IDLE -> R.string.check_for_updates
                    CheckState.CHECKING -> R.string.checking_updates
                    CheckState.LATEST -> R.string.up_to_date
                    CheckState.ERROR -> R.string.update_check_failed
                },
            ),
            style = WeftType.label,
        )
    }
}

/**
 * The automatic check on app start, at most once a day and only if no other prompt showed: the
 * github build's Update dialog, or Google Play's update screen in the play build.
 */
@Composable
fun UpdatePrompt(updates: UpdateManager, play: PlayUpdates) {
    if (play.enabled) { PlayUpdatePrompt(play); return }
    if (!updates.enabled) return
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<AppUpdate?>(null) }
    LaunchedEffect(Unit) {
        val update = updates.checkIfDue() ?: return@LaunchedEffect
        if (LaunchPrompts.claim()) found = update
    }
    found?.let { update ->
        UpdateSheet(
            update = update,
            updates = updates,
            onLater = { found = null; scope.launch { updates.later(update) } },
            onDismiss = { found = null },
        )
    }
}

@Composable
private fun PlayUpdatePrompt(play: PlayUpdates) {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) scope.launch { play.later() }
    }
    LaunchedEffect(Unit) {
        if (play.checkIfDue() && LaunchPrompts.claim()) play.start(launcher)
    }
}

/** A small "Update available" pill, for More → About Weft. */
@Composable
fun UpdateBadge() {
    Text(
        stringResource(R.string.update_available),
        style = WeftType.captionStrong,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
