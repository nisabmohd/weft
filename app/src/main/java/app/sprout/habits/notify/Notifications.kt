package app.sprout.habits.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.sprout.habits.R

object Notifications {
    /** One notification per habit reminder: a pop-up (user, 2026-11-09). */
    const val CHANNEL_REMINDERS = "reminders_popup"

    /** Alarm reminders: a pop-up with a full-screen intent; AlarmSoundService plays the sound and vibrates. */
    const val CHANNEL_ALARMS = "alarms_popup"

    /**
     * Channels from before 1.6.2, deleted on start: "alarms" (1.6.0, sound in the channel), and
     * "reminders" / "notes" / "alarms_ring", which ColorOS showed as Silent (no pop-up, and no
     * full screen on a locked OnePlus 13s; 2026-11-09). A channel's importance and sound can't
     * change once made, so they got new ids. "notes_popup": the evening note nudge now uses the
     * reminders channel, so there is one switch less to turn on (user, 2026-11-09).
     */
    private val OLD_CHANNELS = listOf("alarms", "alarms_ring", "reminders", "notes", "notes_popup")

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_reminders_desc)
                },
                // AlarmSoundService plays the alarm itself, so no phone (a cooldown on repeated
                // notifications, ColorOS) can mute it. The channel's own sound is half a second of
                // silence: with no sound at all ColorOS files the channel as Silent and drops the
                // pop-up and the full-screen intent.
                NotificationChannel(CHANNEL_ALARMS, context.getString(R.string.channel_alarms), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_alarms_desc)
                    setSound(
                        android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_channel_silence}"),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    enableVibration(false)
                    setBypassDnd(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            ),
        )
        OLD_CHANNELS.forEach(manager::deleteNotificationChannel)
    }

    /** True when the app may post notifications right now. */
    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    /** The channels that should show as banners: reminders (and note nudges), then alarms. */
    private val POP_UP_CHANNELS = listOf(CHANNEL_REMINDERS, CHANNEL_ALARMS)

    /** The pop-up channels the phone or the user set below high importance (a channel turned off doesn't count). */
    private fun quietChannels(context: Context): List<String> {
        val manager = context.getSystemService(NotificationManager::class.java)
        return POP_UP_CHANNELS.filter { id ->
            val importance = manager.getNotificationChannel(id)?.importance ?: return@filter false
            importance != NotificationManager.IMPORTANCE_NONE && importance < NotificationManager.IMPORTANCE_HIGH
        }
    }

    /**
     * True when Weft's notifications pop up on screen. Android only opens an alarm full screen
     * from a channel that pops up; ColorOS sets every new channel below that (2026-11-09). Not a
     * row of its own (user, 2026-11-11): the notifications row's line asks for banners while off.
     */
    fun canPopUp(context: Context): Boolean = quietChannels(context).isEmpty()

    /** Opens "Alarms & reminders" for this app (Android 12+), where exact alarms are allowed. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SystemPages.exactAlarms(context)
    }

    /** True when alarms may open full screen: always before Android 14, then only if allowed. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** Opens "Full screen notifications" for this app (Android 14+). */
    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) SystemPages.fullScreen(context)
    }

    /** Opens the system screen for this app's notifications. */
    fun openSettings(context: Context) = SystemPages.appNotifications(context)
}
