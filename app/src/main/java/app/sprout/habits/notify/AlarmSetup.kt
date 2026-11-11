package app.sprout.habits.notify

import android.app.AlarmManager
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.data.AlarmSound

/**
 * What an alarm needs from Android (features/FEATURE-ALARM-REMINDERS.md 7): the rows of Set up
 * alarms and More → Alarms, most important first (the Reminder card names the first one missing).
 */
enum class AlarmNeed { NOTIFICATIONS, EXACT_ALARMS, FULL_SCREEN }

object AlarmSetup {
    /**
     * The rows this phone shows: notifications on every phone (the row also opens Weft's
     * notification settings, where banners and sound are chosen), exact alarms on 12+, full screen on 14+.
     */
    fun needs(context: Context): List<AlarmNeed> = buildList {
        add(AlarmNeed.NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31) add(AlarmNeed.EXACT_ALARMS)
        if (Build.VERSION.SDK_INT >= 34) add(AlarmNeed.FULL_SCREEN)
    }

    fun isAllowed(context: Context, need: AlarmNeed): Boolean = when (need) {
        AlarmNeed.NOTIFICATIONS -> Notifications.canPost(context)
        AlarmNeed.EXACT_ALARMS -> Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        AlarmNeed.FULL_SCREEN -> Notifications.canUseFullScreen(context)
    }

    /** The needs that aren't allowed, most important first. */
    fun missing(context: Context): List<AlarmNeed> = needs(context).filterNot { isAllowed(context, it) }

    /** Opens the system page for [need]. */
    fun open(context: Context, need: AlarmNeed) = when (need) {
        AlarmNeed.NOTIFICATIONS -> Notifications.openSettings(context)
        AlarmNeed.EXACT_ALARMS -> Notifications.openExactAlarmSettings(context)
        AlarmNeed.FULL_SCREEN -> Notifications.openFullScreenSettings(context)
    }

    /** The optional "Show over other apps" (never counted as missing): Weft may open the ringing screen itself. */
    fun canShowOverApps(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openShowOverApps(context: Context) = SystemPages.overlay(context)

    /**
     * The phone's maker. Debug builds can pretend to be another (`adb shell setprop debug.weft.maker
     * oneplus`, then restart Weft) so the phone-maker rows can be seen and captured on an emulator.
     */
    private val maker: String = (debugMaker() ?: Build.MANUFACTURER).lowercase()

    private fun debugMaker(): String? = if (!BuildConfig.DEBUG) null else runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.weft.maker") as String
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Makers known to hold back plain exact alarms (ColorOS, MIUI / HyperOS, Funtouch, EMUI): a head start for [delaysExactAlarms]. */
    private val lateAlarmMakers = setOf("oneplus", "oppo", "realme", "xiaomi", "redmi", "poco", "vivo", "iqoo", "huawei", "honor")

    /**
     * Phones that hold back a plain exact alarm: ColorOS stretched them into windows of up to an
     * hour on a OnePlus 13s (2026-11-09, battery limits off), and any phone can in deep sleep or a
     * battery saver. Only an alarm clock stays on the minute there, so notification reminders and
     * the note nudge are scheduled as alarm clocks on these phones (user, 2026-11-09), accepting
     * the status bar's alarm icon. Known makers start that way; any other phone switches the
     * first time a reminder arrives late ([noteLateness]).
     */
    fun delaysExactAlarms(context: Context): Boolean = maker in lateAlarmMakers || device(context).getBoolean(KEY_DELAYS, false)

    /**
     * A plain exact reminder meant for [at] just arrived. More than [LATE_MS] late (and not so late
     * that the phone was off or the clock moved): remember that this phone delays them. True when
     * that is new, so the caller reschedules every reminder as an alarm clock.
     */
    fun noteLateness(context: Context, at: Long, now: Long = System.currentTimeMillis()): Boolean {
        val late = now - at
        if (late < LATE_MS || late > MAX_LATE_MS || device(context).getBoolean(KEY_DELAYS, false)) return false
        device(context).edit().putBoolean(KEY_DELAYS, true).apply()
        return true
    }

    /**
     * What Weft learned about this phone: a fact about the device, read while scheduling, so a
     * plain SharedPreferences, not the settings DataStore (and never in a backup, which may be
     * restored on another phone).
     */
    private fun device(context: Context) = context.getSharedPreferences("device", Context.MODE_PRIVATE)

    private const val KEY_DELAYS = "delaysExactAlarms"
    private const val LATE_MS = 2 * 60_000L
    private const val MAX_LATE_MS = 6 * 60 * 60_000L

    /**
     * Weft's app info page, where phone makers keep their own lock screen, pop-up and battery
     * switches. Android can't tell whether they are on, so these rows never get a tick.
     */
    fun openAppDetails(context: Context) = SystemPages.appDetails(context)
}

/** Turns the stored alarm sound into something to play and a name to show. */
object AlarmSounds {
    private fun raw(context: Context, id: Int): Uri = Uri.parse("android.resource://${context.packageName}/$id")

    /** The phone's own alarm sound (what 1.6.0 and 1.6.1 played). */
    fun phoneAlarm(context: Context): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    /** Weft's own tone: what plays by default, and when a picked sound can't be played. */
    fun weft(context: Context): Uri = raw(context, R.raw.alarm_weft)

    fun uri(context: Context, sound: String): Uri = when (sound) {
        AlarmSound.WEFT -> weft(context)
        AlarmSound.WEFT_SOFT -> raw(context, R.raw.alarm_weft_soft)
        AlarmSound.SYSTEM -> phoneAlarm(context) ?: weft(context)
        else -> runCatching { Uri.parse(sound) }.getOrNull() ?: weft(context)
    }

    /** True for a sound picked from the phone (stored as its URI). */
    fun isPicked(sound: String): Boolean = sound !in setOf(AlarmSound.WEFT, AlarmSound.WEFT_SOFT, AlarmSound.SYSTEM)

    /** The name Android gives [uri] ("Oxygen"), or null when it can't be read. */
    fun title(context: Context, uri: Uri?): String? = uri?.let {
        runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull()?.takeIf { t -> t.isNotBlank() }
    }
}
