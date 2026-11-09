package app.sprout.habits.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.notify.AlarmNeed
import app.sprout.habits.notify.Notifications
import app.sprout.habits.ui.alarm.AlarmSetupState
import app.sprout.habits.ui.alarm.CardRowPadding
import app.sprout.habits.ui.alarm.ExtraPhoneSettingsRow
import app.sprout.habits.ui.alarm.ExtraPhoneSettingsSheet
import app.sprout.habits.ui.alarm.SetupLinkRow
import app.sprout.habits.ui.alarm.needsExtraPhoneSettings
import app.sprout.habits.ui.alarm.permissionRows
import app.sprout.habits.ui.alarm.rememberAlarmSetup
import app.sprout.habits.ui.components.TimePickerSheet
import app.sprout.habits.ui.today.TodayViewModel
import kotlinx.coroutines.launch

/**
 * More → Reminders & alarms (more-reminders-alarms-screen.png), built like Backup & restore:
 * Reminders (Default reminder), Allow Weft to (the three switches with the real state, then Extra
 * phone settings on the phones that need it) and Sound (Alarm sound, Vibrate, Notification sound ↗).
 * No status card: the switches show the state.
 */
@Composable
fun RemindersAlarmsScreen(settings: Settings, repository: SettingsRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = rememberAlarmSetup()
    var pickingSound by remember { mutableStateOf(false) }
    var pickingReminder by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(stringResource(R.string.reminders_alarms), onBack)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(stringResource(R.string.reminders_section), first = true)
            SettingsCard {
                SettingsRow(stringResource(R.string.default_reminder), stringResource(R.string.default_reminder_desc), onClick = { pickingReminder = true }) {
                    TrailingValue(TodayViewModel.formatTime(settings.defaultReminderMinutes))
                    Chevron()
                }
            }

            SectionLabel(stringResource(R.string.allow_weft_to))
            val allow = permissionRows(state, CardRowPadding) +
                if (needsExtraPhoneSettings) listOf(@Composable { ExtraPhoneSettingsRow(CardRowPadding) { extra = true } }) else emptyList()
            SettingsGroup(*allow.asCardRows())

            SectionLabel(stringResource(R.string.alarms_sound_section))
            SettingsGroup(
                {
                    SettingsRow(stringResource(R.string.alarm_sound), onClick = { pickingSound = true }) {
                        TrailingValue(alarmSoundName(settings.alarmSound))
                        Chevron()
                    }
                },
                {
                    SwitchRow(stringResource(R.string.alarm_vibrate), null, settings.alarmVibrate) { on ->
                        scope.launch { repository.setAlarmVibrate(on) }
                    }
                },
                { SetupLinkRow(stringResource(R.string.notification_sound), null, CardRowPadding) { Notifications.openSettings(context) } },
            )
        }
    }

    if (pickingReminder) {
        TimePickerSheet(
            title = stringResource(R.string.default_reminder),
            initialMinutes = settings.defaultReminderMinutes,
            onApply = { minutes -> scope.launch { repository.setDefaultReminderMinutes(minutes) }; pickingReminder = false },
            onDismiss = { pickingReminder = false },
            line = stringResource(R.string.default_reminder_desc),
        )
    }
    if (pickingSound) {
        AlarmSoundSheet(
            current = settings.alarmSound,
            onPick = { sound -> scope.launch { repository.setAlarmSound(sound) } },
            onDismiss = { pickingSound = false },
        )
    }
    if (extra) ExtraPhoneSettingsSheet(state, onDismiss = { extra = false })
}

private fun List<@Composable () -> Unit>.asCardRows(): Array<@Composable ColumnScope.() -> Unit> =
    map { row ->
        val card: @Composable ColumnScope.() -> Unit = { row() }
        card
    }.toTypedArray()

/**
 * The More row's line: "Notifications and alarms allowed", or what's wrong in plain words (shown
 * in the error colour), the most important first, the same order as the Reminder card warning.
 */
@Composable
fun remindersLine(state: AlarmSetupState): String = stringResource(
    when (state.missing.firstOrNull()) {
        null -> R.string.reminders_all_allowed
        AlarmNeed.NOTIFICATIONS -> R.string.reminders_off_notifications
        AlarmNeed.EXACT_ALARMS -> R.string.reminders_off_exact
        AlarmNeed.FULL_SCREEN -> R.string.reminders_off_full_screen
    },
)
