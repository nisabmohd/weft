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
import app.sprout.habits.notify.SystemPages
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
    /** Notifications are on but don't pop up as banners (some phones set new apps that way). */
    val bannersOff: Boolean,
    val open: (AlarmNeed) -> Unit,
    val openShowOverApps: () -> Unit,
    val openPhoneSettings: () -> Unit,
    val openBattery: () -> Unit,
) {
    val allSet: Boolean get() = missing.isEmpty()
}

@Composable
fun rememberAlarmSetup(): AlarmSetupState {
    val context = LocalContext.current
    var needs by remember { mutableStateOf(AlarmSetup.needs(context)) }
    var missing by remember { mutableStateOf(AlarmSetup.missing(context)) }
    var overApps by remember { mutableStateOf(AlarmSetup.canShowOverApps(context)) }
    var bannersOff by remember { mutableStateOf(Notifications.canPost(context) && !Notifications.canPopUp(context)) }
    fun refresh() {
        needs = AlarmSetup.needs(context)
        missing = AlarmSetup.missing(context)
        overApps = AlarmSetup.canShowOverApps(context)
        bannersOff = Notifications.canPost(context) && !Notifications.canPopUp(context)
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
        bannersOff = bannersOff,
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
        openBattery = { SystemPages.battery(context) },
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

/** Row padding in a sheet: full width, the text at the title inset, like `ChoiceRow`. */
private val SheetRowPadding = PaddingValues(horizontal = SheetTitleInset, vertical = 6.dp)

/** Row padding inside a grouped card (Reminders & alarms), like `SettingsRow`. */
val CardRowPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

/**
 * A permission row: one line saying what the person gets, and the app's switch showing whether
 * Android allows it. Tapping it (switch included) opens that settings page; the switch follows the
 * real state when the user comes back, as an app can't turn a permission off itself.
 */
@Composable
fun SetupSwitchRow(title: String, text: String?, allowed: Boolean, padding: PaddingValues, onOpen: () -> Unit) {
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

/** A row that opens a page outside Weft (or Extra phone settings): no switch, a muted ↗. */
@Composable
fun SetupLinkRow(title: String, text: String?, padding: PaddingValues, onOpen: () -> Unit) {
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
private fun RowText(title: String, text: String?, modifier: Modifier) {
    Column(modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
        text?.let { Text(it, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** What a row is for, under its title. Notifications that don't pop up ask for banners instead. */
@Composable
private fun alarmNeedLine(need: AlarmNeed, state: AlarmSetupState): String = stringResource(
    when (need) {
        AlarmNeed.NOTIFICATIONS ->
            if (state.bannersOff && need !in state.missing) R.string.alarm_need_notifications_banners_off else R.string.alarm_need_notifications_desc
        AlarmNeed.EXACT_ALARMS -> R.string.alarm_need_exact_desc
        AlarmNeed.FULL_SCREEN -> R.string.alarm_need_full_screen_desc
    },
)

/** The permission rows of [state], each its own composable so a screen can group them. */
fun permissionRows(state: AlarmSetupState, padding: PaddingValues): List<@Composable () -> Unit> =
    state.needs.map { need ->
        @Composable { SetupSwitchRow(alarmNeedTitle(need), alarmNeedLine(need, state), need !in state.missing, padding) { state.open(need) } }
    }

/** "Extra phone settings · Only needed on some phones" ↗: opens [ExtraPhoneSettingsSheet]. */
@Composable
fun ExtraPhoneSettingsRow(padding: PaddingValues, onOpen: () -> Unit) =
    SetupLinkRow(stringResource(R.string.extra_phone_settings), stringResource(R.string.extra_phone_settings_desc), padding, onOpen)

/**
 * Set up alarms (alarm-9a/b/c-setup-sheet-*.png): the switches, Extra phone settings, then **Use a notification instead** while a switch is off, or **Done**.
 * [onUseNotification] null: the button only closes.
 */
@Composable
fun AlarmSetupSheet(onDismiss: () -> Unit, onUseNotification: (() -> Unit)? = null) {
    val state = rememberAlarmSetup()
    var extra by remember { mutableStateOf(false) }
    // Offered only for what a notification doesn't need (exact time, full screen): without
    // notifications or pop-ups a notification reminder is no better off.
    val useNotification = onUseNotification != null && state.missing.any { it == AlarmNeed.EXACT_ALARMS || it == AlarmNeed.FULL_SCREEN }
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
        permissionRows(state, SheetRowPadding).forEach { it() }
        ExtraPhoneSettingsRow(SheetRowPadding) { extra = true }
    }
    if (extra) ExtraPhoneSettingsSheet(state, onDismiss = { extra = false })
}

/**
 * Extra phone settings (extra-phone-settings-sheet.png): what some phone makers add on top of
 * Android. Show over other apps is a switch (Android reports it; optional, never counted); Lock
 * screen & pop-ups and Battery open Weft's page in settings, as Android can't tell if they're on.
 */
@Composable
fun ExtraPhoneSettingsSheet(state: AlarmSetupState, onDismiss: () -> Unit) {
    ChoiceSheet(
        title = stringResource(R.string.extra_phone_settings),
        subtitle = stringResource(R.string.extra_phone_settings_text),
        onDismiss = onDismiss,
        footer = {
            WeftButton(
                stringResource(R.string.action_done),
                ButtonRole.Primary,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SheetSidePadding).padding(top = SheetActionGap).sheetBottomPadding(),
            )
        },
    ) {
        // The same rows on every phone: Android reports only the first; the others are on Weft's app page.
        SetupSwitchRow(stringResource(R.string.alarm_overlay), stringResource(R.string.alarm_overlay_desc), state.showOverApps, SheetRowPadding, state.openShowOverApps)
        SetupLinkRow(stringResource(R.string.alarm_lock_screen), stringResource(R.string.alarm_lock_screen_desc), SheetRowPadding, state.openPhoneSettings)
        SetupLinkRow(stringResource(R.string.alarm_battery), stringResource(R.string.alarm_battery_desc), SheetRowPadding, state.openBattery)
    }
}
