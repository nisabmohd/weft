package app.sprout.habits.ui.more

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import app.sprout.habits.R
import app.sprout.habits.data.AlarmSound
import app.sprout.habits.notify.AlarmSounds
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
import app.sprout.habits.ui.components.ExtraActionCard
import app.sprout.habits.ui.components.tick

/** How long tapping a sound plays it. */
private const val PREVIEW_MS = 4_000L

/**
 * Alarm sound (alarm-11-sound-sheet.png): Weft, Weft · Soft, the phone's alarm sound, and a sound
 * picked from the phone once there is one; then Choose from phone (Android's sound picker).
 * Tapping a row picks it and plays it for a few seconds on the alarm stream ("Playing"); closing
 * the sheet stops it. No Done button, like the other choice sheets.
 */
@Composable
fun AlarmSoundSheet(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val preview = remember { SoundPreview() }
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    var playing by remember { mutableStateOf<String?>(null) }
    preview.onDone = { playing = null }
    // A sound picked from the phone stays as a fourth row while it's the chosen one.
    var picked by remember { mutableStateOf(current.takeIf { AlarmSounds.isPicked(it) }) }

    fun choose(sound: String) {
        haptics.tick()
        onPick(sound)
        playing = sound
        preview.play(context, AlarmSounds.uri(context, sound))
    }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            picked = uri.toString()
            onPick(uri.toString())
        }
    }

    val playingLine = stringResource(R.string.alarm_sound_playing)
    @Composable
    fun Row(sound: String, title: String, line: String?) {
        val isPlaying = playing == sound
        ChoiceRow(
            title,
            if (isPlaying) playingLine else line,
            selected = current == sound,
            subtitleIcon = if (isPlaying) R.drawable.ic_volume_loud else null,
            subtitleColor = if (isPlaying) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified,
        ) { choose(sound) }
    }

    ChoiceSheet(
        title = stringResource(R.string.alarm_sound),
        onDismiss = onDismiss,
        footer = {
            ExtraActionCard(R.drawable.ic_music_note, stringResource(R.string.alarm_sound_choose), stringResource(R.string.alarm_sound_choose_desc)) {
                preview.stop()
                playing = null
                chooser.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, picked?.let(Uri::parse)),
                )
            }
        },
    ) {
        Row(AlarmSound.WEFT, stringResource(R.string.alarm_sound_weft), stringResource(R.string.alarm_sound_weft_desc))
        Row(AlarmSound.WEFT_SOFT, stringResource(R.string.alarm_sound_weft_soft), stringResource(R.string.alarm_sound_weft_soft_desc))
        Row(AlarmSound.SYSTEM, stringResource(R.string.alarm_sound_phone), remember { AlarmSounds.title(context, AlarmSounds.phoneAlarm(context)) })
        picked?.let { uri ->
            val name = remember(uri) { AlarmSounds.title(context, Uri.parse(uri)) } ?: stringResource(R.string.alarm_sound_choose)
            Row(uri, name, stringResource(R.string.alarm_sound_picked_desc))
        }
    }
}

/** The name shown for [sound] on the Alarms screen's Alarm sound row. */
@Composable
fun alarmSoundName(sound: String): String {
    val context = LocalContext.current
    return when (sound) {
        AlarmSound.WEFT -> stringResource(R.string.alarm_sound_weft)
        AlarmSound.WEFT_SOFT -> stringResource(R.string.alarm_sound_weft_soft)
        AlarmSound.SYSTEM -> stringResource(R.string.alarm_sound_phone)
        else -> remember(sound) { AlarmSounds.title(context, Uri.parse(sound)) } ?: stringResource(R.string.alarm_sound_picked_desc)
    }
}

/** Plays one sound at a time on the alarm stream for [PREVIEW_MS]. */
private class SoundPreview {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    var onDone: () -> Unit = {}

    fun play(context: android.content.Context, uri: Uri) {
        stop()
        val p = MediaPlayer()
        player = runCatching {
            p.apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrElse { p.release(); null }
        if (player == null) { onDone(); return }
        handler.postDelayed({ stop(); onDone() }, PREVIEW_MS)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}
