package app.sprout.habits.notify

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
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
enum class AlarmNeed { NOTIFICATIONS, POP_UP, EXACT_ALARMS, FULL_SCREEN }

object AlarmSetup {
    /**
     * The rows this phone shows: notifications on 13+ (and before, only while they're off), exact
     * alarms on 12+, full screen on 14+.
     */
    fun needs(context: Context): List<AlarmNeed> = buildList {
        if (Build.VERSION.SDK_INT >= 33 || !Notifications.canPost(context)) add(AlarmNeed.NOTIFICATIONS)
        // Pop-ups: on the makers that turn them off for new apps (ColorOS did, OnePlus 13s,
        // 2026-11-09), and anywhere they are off. Without them an alarm can't open full screen.
        if (Notifications.canPost(context) && (showLockScreenRow || !Notifications.canPopUp(context))) add(AlarmNeed.POP_UP)
        if (Build.VERSION.SDK_INT >= 31) add(AlarmNeed.EXACT_ALARMS)
        if (Build.VERSION.SDK_INT >= 34) add(AlarmNeed.FULL_SCREEN)
    }

    fun isAllowed(context: Context, need: AlarmNeed): Boolean = when (need) {
        AlarmNeed.NOTIFICATIONS -> Notifications.canPost(context)
        AlarmNeed.POP_UP -> Notifications.canPopUp(context)
        AlarmNeed.EXACT_ALARMS -> Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        AlarmNeed.FULL_SCREEN -> Notifications.canUseFullScreen(context)
    }

    /** The needs that aren't allowed, most important first. */
    fun missing(context: Context): List<AlarmNeed> = needs(context).filterNot { isAllowed(context, it) }

    /** Opens the system page for [need]. */
    fun open(context: Context, need: AlarmNeed) = when (need) {
        AlarmNeed.NOTIFICATIONS -> Notifications.openSettings(context)
        AlarmNeed.POP_UP -> Notifications.openPopUpSettings(context)
        AlarmNeed.EXACT_ALARMS -> Notifications.openExactAlarmSettings(context)
        AlarmNeed.FULL_SCREEN -> Notifications.openFullScreenSettings(context)
    }

    /** The optional "Show over other apps" (never counted as missing): Weft may open the ringing screen itself. */
    fun canShowOverApps(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openShowOverApps(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * The phone's maker. Debug builds can pretend to be another (`adb shell setprop debug.weft.maker
     * oneplus`, then restart Weft) so the phone-maker rows can be seen and captured on an emulator.
     */
    private val maker: String = (debugMaker() ?: Build.MANUFACTURER).lowercase()

    private fun debugMaker(): String? = if (!BuildConfig.DEBUG) null else runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.weft.maker") as String
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Makers whose own settings can keep an alarm off the lock screen (ColorOS, MIUI / HyperOS, Funtouch, EMUI). */
    private val lockScreenMakers = setOf("oneplus", "oppo", "realme", "xiaomi", "redmi", "poco", "vivo", "iqoo", "huawei", "honor")

    /** The "Show over other apps" and "Lock screen & pop-ups" rows: only on phones whose maker adds its own switches. */
    val showLockScreenRow: Boolean get() = maker in lockScreenMakers

    /**
     * Phones that hold back a plain exact alarm: ColorOS stretched them into windows of up to an
     * hour on a OnePlus 13s (2026-11-09, battery limits off), and any phone can in deep sleep or a
     * battery saver. Only an alarm clock stays on the minute there, so notification reminders and
     * the note nudge are scheduled as alarm clocks on these phones (user, 2026-11-09), accepting
     * the status bar's alarm icon. Known makers start that way; any other phone switches the
     * first time a reminder arrives late ([noteLateness]).
     */
    fun delaysExactAlarms(context: Context): Boolean = maker in lockScreenMakers || device(context).getBoolean(KEY_DELAYS, false)

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

    /** The "Battery" row: those makers, plus Samsung, whose battery saver puts apps to sleep. */
    val showBatteryRow: Boolean get() = maker in lockScreenMakers || maker == "samsung"

    /**
     * Weft's page in the phone's settings, where these makers keep the lock screen, pop-up and
     * battery switches. Android can't tell whether they are on, so these rows never get a tick.
     */
    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
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
