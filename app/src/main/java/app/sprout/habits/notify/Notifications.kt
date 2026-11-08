package app.sprout.habits.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.sprout.habits.R

object Notifications {
    /** One notification per habit reminder. */
    const val CHANNEL_REMINDERS = "reminders"

    /** The evening nudge to add a note to a skipped habit. */
    const val CHANNEL_NOTES = "notes"

    /** Alarm reminders: a silent heads-up; AlarmSoundService plays the sound and vibrates. */
    const val CHANNEL_ALARMS = "alarms_ring"

    /** 1.6.0 (27)'s alarm channel, with the sound in the channel; deleted on start. */
    private const val OLD_CHANNEL_ALARMS = "alarms"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_reminders_desc)
                },
                NotificationChannel(CHANNEL_NOTES, context.getString(R.string.channel_notes), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_notes_desc)
                },
                // Silent: AlarmSoundService plays the alarm itself, so no phone (a cooldown on
                // repeated notifications, ColorOS) can mute it. The heads-up still needs HIGH.
                NotificationChannel(CHANNEL_ALARMS, context.getString(R.string.channel_alarms), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_alarms_desc)
                    setSound(null, null)
                    enableVibration(false)
                    setBypassDnd(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            ),
        )
        // 1.6.0 (27) rang through this channel's sound; a channel's sound can't change once made.
        manager.deleteNotificationChannel(OLD_CHANNEL_ALARMS)
    }

    /** True when the app may post notifications right now. */
    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    /** Opens "Alarms & reminders" for this app (Android 12+), where exact alarms are allowed. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** True when alarms may open full screen: always before Android 14, then only if allowed. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** Opens "Full screen notifications" for this app (Android 14+). */
    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Opens the system screen for this app's notifications. */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
