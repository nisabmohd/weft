package app.sprout.habits.ui.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.sprout.habits.R
import app.sprout.habits.notify.AlarmNeed
import app.sprout.habits.notify.AlarmSetup
import app.sprout.habits.notify.Notifications
import app.sprout.habits.ui.components.ButtonRole
import app.sprout.habits.ui.components.ChoiceSheet
import app.sprout.habits.ui.components.SheetActionGap
import app.sprout.habits.ui.components.SheetSidePadding
import app.sprout.habits.ui.components.SheetTitleInset
import app.sprout.habits.ui.components.WeftButton
import app.sprout.habits.ui.components.sheetBottomPadding
import app.sprout.habits.ui.components.toggle
import app.sprout.habits.ui.theme.WeftType

/**
 * What alarms need on this phone, re-read whenever the app comes back to the front (the user may
 * have just allowed something in Android's settings).
 */
@Immutable
class AlarmSetupState(
    /** The rows this phone shows, most important first. */
    val needs: List<AlarmNeed>,
    /** Of [needs], the ones not allowed: "N things to allow", the Reminder card warning. */
    val missing: List<AlarmNeed>,
    /** The optional Show over other apps; never counted in [missing]. */
    val showOverApps: Boolean,
    val open: (AlarmNeed) -> Unit,
    val openShowOverApps: () -> Unit,
    val openPhoneSettings: () -> Unit,
) {
    val allSet: Boolean get() = missing.isEmpty()
}

@Composable
fun rememberAlarmSetup(): AlarmSetupState {
    val context = LocalContext.current
    var needs by remember { mutableStateOf(AlarmSetup.needs(context)) }
    var missing by remember { mutableStateOf(AlarmSetup.missing(context)) }
    var overApps by remember { mutableStateOf(AlarmSetup.canShowOverApps(context)) }
    fun refresh() {
        needs = AlarmSetup.needs(context)
        missing = AlarmSetup.missing(context)
        overApps = AlarmSetup.canShowOverApps(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    var askedAt by remember { mutableLongStateOf(0L) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh()
        // Android answers "no" at once, without asking, once the user said no twice: go to settings.
        if (!granted && SystemClock.elapsedRealtime() - askedAt < 500) Notifications.openSettings(context)
    }
    return AlarmSetupState(
        needs = needs,
        missing = missing,
        showOverApps = overApps,
        open = { need ->
            val askable = need == AlarmNeed.NOTIFICATIONS && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (askable) {
                askedAt = SystemClock.elapsedRealtime()
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AlarmSetup.open(context, need)
            }
        },
        openShowOverApps = { AlarmSetup.openShowOverApps(context) },
        openPhoneSettings = { AlarmSetup.openAppDetails(context) },
    )
}

@Composable
fun alarmNeedTitle(need: AlarmNeed): String = stringResource(
    when (need) {
        AlarmNeed.NOTIFICATIONS -> R.string.alarm_need_notifications
        AlarmNeed.EXACT_ALARMS -> R.string.alarm_need_exact
        AlarmNeed.FULL_SCREEN -> R.string.alarm_need_full_screen
    },
)

@Composable
private fun alarmNeedText(need: AlarmNeed): String = stringResource(
    when (need) {
        AlarmNeed.NOTIFICATIONS -> R.string.alarm_need_notifications_desc
        AlarmNeed.EXACT_ALARMS -> R.string.alarm_need_exact_desc
        AlarmNeed.FULL_SCREEN -> R.string.alarm_need_full_screen_desc
    },
)

/** Row padding in the sheet: full width, the text at the title inset, like `ChoiceRow`. */
private val SheetRowPadding = PaddingValues(horizontal = SheetTitleInset, vertical = 6.dp)

/** Row padding on the Alarms screen: inside a grouped card, like `SettingsRow`. */
val CardRowPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

/**
 * A permission row: title, why, and the app's switch showing whether Android allows it. Tapping it
 * (switch included) opens that settings page; the switch follows the real state when the user comes
 * back, as an app can't turn a permission off itself.
 */
@Composable
fun SetupSwitchRow(title: String, text: String, allowed: Boolean, padding: PaddingValues, onOpen: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = allowed, role = Role.Switch) { haptics.toggle(it); onOpen() }
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, text, Modifier.weight(1f))
        Switch(checked = allowed, onCheckedChange = null)
    }
}

/** A phone-maker row: Android can't tell if it's on, so no switch; ↗ opens Weft's page in settings. */
@Composable
fun SetupLinkRow(title: String, text: String, padding: PaddingValues, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onOpen).padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, text, Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.ic_open_external),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RowText(title: String, text: String, modifier: Modifier) {
    Column(modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
        Text(text, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The permission rows of [state], each its own composable so a screen can group them. */
fun permissionRows(state: AlarmSetupState, padding: PaddingValues): List<@Composable () -> Unit> =
    state.needs.map { need ->
        @Composable { SetupSwitchRow(alarmNeedTitle(need), alarmNeedText(need), need !in state.missing, padding) { state.open(need) } }
    }

/** Show over other apps and the phone-maker rows; empty on phones that don't need them (Pixel). */
fun phoneRows(state: AlarmSetupState, padding: PaddingValues): List<@Composable () -> Unit> = buildList {
    if (AlarmSetup.showLockScreenRow) {
        add @Composable {
            SetupSwitchRow(stringResource(R.string.alarm_overlay), stringResource(R.string.alarm_overlay_desc), state.showOverApps, padding, state.openShowOverApps)
        }
        add @Composable {
            SetupLinkRow(stringResource(R.string.alarm_lock_screen), stringResource(R.string.alarm_lock_screen_desc), padding, state.openPhoneSettings)
        }
    }
    if (AlarmSetup.showBatteryRow) {
        add @Composable {
            SetupLinkRow(stringResource(R.string.alarm_battery), stringResource(R.string.alarm_battery_desc), padding, state.openPhoneSettings)
        }
    }
}

/**
 * Set up alarms (alarm-9a/b/c-setup-sheet-*.png): everything an alarm needs on this phone as one
 * plain list, then **Use a notification instead** while something required is missing, or **Done**.
 * [onUseNotification] null: the button only closes (opened from More → Alarms).
 */
@Composable
fun AlarmSetupSheet(onDismiss: () -> Unit, onUseNotification: (() -> Unit)? = null) {
    val state = rememberAlarmSetup()
    val useNotification = !state.allSet && onUseNotification != null
    ChoiceSheet(
        title = stringResource(R.string.set_up_alarms),
        onDismiss = onDismiss,
        footer = {
            WeftButton(
                stringResource(if (useNotification) R.string.alarm_use_notification else R.string.action_done),
                if (useNotification) ButtonRole.Secondary else ButtonRole.Primary,
                onClick = { if (useNotification) onUseNotification?.invoke() else onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = SheetSidePadding).padding(top = SheetActionGap).sheetBottomPadding(),
            )
        },
    ) {
        (permissionRows(state, SheetRowPadding) + phoneRows(state, SheetRowPadding)).forEach { it() }
    }
}
