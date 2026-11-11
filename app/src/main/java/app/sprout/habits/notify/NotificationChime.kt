package app.sprout.habits.notify

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The sound and buzz of a reminder when the phone took them off Weft's channel. ColorOS creates
 * every new channel with no sound and no vibration (OnePlus 13s, 2026-11-09), so reminders arrived
 * silently. Then Weft plays the phone's notification sound and vibrates itself, like alarms
 * ([AlarmSoundService]). It follows the phone: nothing in silent mode, Do Not Disturb or a call,
 * only the buzz in vibrate mode, and nothing for a channel the user made Silent or turned off.
 */
object NotificationChime {
    private val BUZZ = longArrayOf(0, 250, 150, 250)

    fun play(context: Context, channelId: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(channelId) ?: return
        // Silent (low) or off: the user's choice. A channel that kept its own sound and buzz rings by itself.
        if (channel.importance < NotificationManager.IMPORTANCE_DEFAULT) return
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        if (AlarmScreenFallback.inCall(context)) return
        val ringer = context.getSystemService(AudioManager::class.java).ringerMode
        if (ringer == AudioManager.RINGER_MODE_SILENT) return
        if (channel.sound == null && ringer == AudioManager.RINGER_MODE_NORMAL) playSound(context)
        if (!channel.shouldVibrate() && channel.sound == null) vibrate(context)
    }

    private fun playSound(context: Context) = runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return@runCatching
        RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
    }

    private fun vibrate(context: Context) = runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return@runCatching
        // Marked as a notification buzz: Android drops a plain (touch) buzz from an app in the
        // background ("Ignoring incoming vibration as process is background", OnePlus 13s).
        val effect = VibrationEffect.createWaveform(BUZZ, -1)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
    }
}
