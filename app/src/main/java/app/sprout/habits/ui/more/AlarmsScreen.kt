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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.alarm.AlarmSetupState
import app.sprout.habits.ui.alarm.CardRowPadding
import app.sprout.habits.ui.alarm.permissionRows
import app.sprout.habits.ui.alarm.phoneRows
import app.sprout.habits.ui.alarm.rememberAlarmSetup
import kotlinx.coroutines.launch

/**
 * More → Alarms (more-alarms-screen.png), built like Backup & restore: a status card, Needed (the
 * permission switches), On some phones (Show over other apps and the phone-maker rows; not on a
 * Pixel), and Sound (Alarm sound, Vibrate). It shows even with no alarm habits, so alarms can be
 * set up ahead of time.
 */
@Composable
fun AlarmsScreen(settings: Settings, repository: SettingsRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberAlarmSetup()
    var pickingSound by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        TopBar(stringResource(R.string.alarms), onBack)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.allSet) {
                StatusCard(R.drawable.ic_check, colors.surfaceContainerHigh, colors.onSurface, stringResource(R.string.alarms_all_set), stringResource(R.string.alarms_all_set_desc)) {}
            } else {
                StatusCard(
                    R.drawable.ic_error, colors.errorContainer, colors.error,
                    pluralStringResource(R.plurals.alarms_to_allow, state.missing.size, state.missing.size),
                    stringResource(R.string.alarms_may_not_ring),
                ) {}
            }

            val needed = permissionRows(state, CardRowPadding)
            if (needed.isNotEmpty()) {
                SectionLabel(stringResource(R.string.alarms_needed))
                SettingsGroup(*needed.asCardRows())
            }
            val phone = phoneRows(state, CardRowPadding)
            if (phone.isNotEmpty()) {
                SectionLabel(stringResource(R.string.alarms_some_phones))
                SettingsGroup(*phone.asCardRows())
            }

            SectionLabel(stringResource(R.string.alarms_sound_section))
            SettingsGroup(
                {
                    SettingsRow(stringResource(R.string.alarm_sound), onClick = { pickingSound = true }) {
                        TrailingValue(alarmSoundName(settings.alarmSound))
                        Chevron()
                    }
                },
                {
                    SwitchRow(stringResource(R.string.alarm_vibrate), stringResource(R.string.alarm_vibrate_desc), settings.alarmVibrate) { on ->
                        scope.launch { repository.setAlarmVibrate(on) }
                    }
                },
            )
        }
    }

    if (pickingSound) {
        AlarmSoundSheet(
            current = settings.alarmSound,
            onPick = { sound -> scope.launch { repository.setAlarmSound(sound) } },
            onDismiss = { pickingSound = false },
        )
    }
}

private fun List<@Composable () -> Unit>.asCardRows(): Array<@Composable ColumnScope.() -> Unit> =
    map { row ->
        val card: @Composable ColumnScope.() -> Unit = { row() }
        card
    }.toTypedArray()

/** The More row's line: "All set", or "2 things to allow" (shown in the error colour). */
@Composable
fun alarmsLine(state: AlarmSetupState): String =
    if (state.allSet) stringResource(R.string.alarms_all_set)
    else pluralStringResource(R.plurals.alarms_to_allow, state.missing.size, state.missing.size)
