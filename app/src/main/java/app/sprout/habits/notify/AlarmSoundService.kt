package app.sprout.habits.notify

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

/**
 * Plays the ringing alarm: the chosen alarm sound (Weft's tone by default, More → Alarms → Alarm
 * sound) on the alarm stream, looped, and vibration (unless turned off), until it is answered. Its foreground notification is the alarm notification itself, so
 * nothing extra shows. The sound used to come from the notification channel, but some phones
 * mute a channel's sound (Android's cooldown on notifications in a row, ColorOS), and the alarm
 * didn't ring (user, 2026-11-08). Only [AlarmRinger] starts and stops it.
 */
class AlarmSoundService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var sound: Uri? = null
    private var vibrate = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, 0) ?: 0
        val notification = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_NOTIFICATION, Notification::class.java) }
        intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_SOUND, Uri::class.java) }?.let { sound = it }
        vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Android needs startForeground for every start, even one already called off.
        ServiceCompat.startForeground(
            this,
            id,
            notification,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        synchronized(lock) {
            if (wanted != id) {
                // Answered before the service was up: take its notification down with it.
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            running = this
            play()
        }
        return START_NOT_STICKY
    }

    private fun play() {
        if (player == null) {
            // A sound picked from the phone may be gone or unreadable: Weft's tone plays instead.
            player = listOfNotNull(sound, AlarmSounds.weft(this)).firstNotNullOfOrNull { uri -> loop(this, uri) }
            // In a call it rings quietly, near in-call volume, so it doesn't blast the ear (design 2026-11-08, 12).
            if (AlarmScreenFallback.inCall(this)) player?.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME)
        }
        if (vibrate && vibrator == null) {
            vibrator = (
                if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
                else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
                ).also { it.vibrate(VibrationEffect.createWaveform(VIBRATION, 0), ALARM_AUDIO) }
        }
    }

    /**
     * Stops the sound. [keepCard]: the notification stays (Log amount posts its quiet version);
     * otherwise Android removes it with the service. A cancel from the app can't do that, as
     * Android lets go of a service's notification later than the call returns.
     */
    private fun silence(keepCard: Boolean) {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        ServiceCompat.stopForeground(this, if (keepCard) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        synchronized(lock) {
            if (running === this) {
                silence(keepCard = false)
                running = null
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_NOTIFICATION = "notification"
        private const val EXTRA_SOUND = "sound"
        private const val EXTRA_VIBRATE = "vibrate"
        private val VIBRATION = longArrayOf(0, 600, 400, 600, 400)
        /** The player's volume during a call, as a share of the alarm volume. */
        private const val IN_CALL_VOLUME = 0.2f
        private val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private val lock = Any()

        /** The notification id that should be sounding, or null. */
        private var wanted: Int? = null
        private var running: AlarmSoundService? = null

        /** [uri] looped on the alarm stream, playing; null if it can't be played. */
        private fun loop(context: Context, uri: Uri): MediaPlayer? {
            val player = MediaPlayer()
            return runCatching {
                player.apply {
                    setAudioAttributes(ALARM_AUDIO)
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrElse { player.release(); null }
        }

        /**
         * Rings with [notification] (posted under [id]) as the service's own notification, playing
         * [sound] (Weft's tone if it can't be played), vibrating if [vibrate].
         */
        fun start(context: Context, id: Int, notification: Notification, sound: Uri, vibrate: Boolean) {
            synchronized(lock) {
                if (wanted == id && running != null) return
                wanted = id
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmSoundService::class.java)
                        .putExtra(EXTRA_ID, id)
                        .putExtra(EXTRA_NOTIFICATION, notification)
                        .putExtra(EXTRA_SOUND, sound)
                        .putExtra(EXTRA_VIBRATE, vibrate),
                )
            } catch (e: RuntimeException) {
                // Android 12+ refuses a foreground service from the background unless the alarm
                // was exact (it isn't when exact alarms aren't allowed): ring from here instead.
                synchronized(lock) {
                    fallback?.stop()
                    fallback = runCatching {
                        RingtoneManager.getRingtone(context, sound)?.apply {
                            audioAttributes = ALARM_AUDIO
                            if (Build.VERSION.SDK_INT >= 28) isLooping = true
                            play()
                        }
                    }.getOrNull()
                }
            }
        }

        /** The sound when the service couldn't start (see [start]). */
        private var fallback: Ringtone? = null

        /**
         * Stops the sound at once (returns when it is quiet) and takes the alarm's card down with
         * it, unless [keepCard] (then the caller posts the quiet version).
         */
        fun stop(keepCard: Boolean = false) {
            synchronized(lock) {
                wanted = null
                fallback?.stop()
                fallback = null
                running?.let {
                    it.silence(keepCard)
                    running = null
                }
            }
        }

        /** True while it rings (for the smoke checks and the timeout). */
        val isRinging: Boolean get() = synchronized(lock) { running != null || fallback?.isPlaying == true }
    }
}
