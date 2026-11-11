package app.sprout.habits.notify

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens Android's settings pages for Weft, the same way on every phone.
 *
 * A page opens inside Weft's own task, never with FLAG_ACTIVITY_NEW_TASK: with that flag, a
 * Settings task already in the background (left on, say, the lock screen notifications page) can be
 * brought back as it was instead of opening the page asked for. Phones whose Settings routes every
 * page through one screen do this every time (OnePlus 13s, 2026-11-11). Back then returns to Weft.
 *
 * Each call lists pages from the most exact to the most general; the first one this phone has opens.
 */
object SystemPages {
    fun appNotifications(context: Context) = open(context, appNotificationsIntent(context), appDetailsIntent(context))

    fun channel(context: Context, channelId: String) = open(
        context,
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
        appNotificationsIntent(context),
        appDetailsIntent(context),
    )

    /** "Alarms & reminders" (Android 12+). */
    fun exactAlarms(context: Context) = open(
        context,
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)),
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
        appDetailsIntent(context),
    )

    /** "Full screen notifications" (Android 14+); some phones keep it on the app's notification page. */
    fun fullScreen(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)),
        appNotificationsIntent(context),
        appDetailsIntent(context),
    )

    /**
     * "Display over other apps". Since Android 11 the public page ignores the app and lists every
     * app, so Weft's own page (AOSP's action, not public) is tried first.
     */
    fun overlay(context: Context) = open(
        context,
        Intent("android.settings.MANAGE_APP_OVERLAY_PERMISSION", packageUri(context)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        appDetailsIntent(context),
    )

    /**
     * Weft's battery page ("Unrestricted" / "Don't restrict"), the one AOSP and most makers keep
     * (not public, so it falls back to Android's battery optimization list, then app info).
     */
    fun battery(context: Context) = open(
        context,
        Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL", packageUri(context)),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetailsIntent(context),
    )

    /** Weft's app info page: where phone makers keep their own lock screen and pop-up switches. */
    fun appDetails(context: Context) = open(context, appDetailsIntent(context), Intent(Settings.ACTION_SETTINGS))

    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")

    private fun appNotificationsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    private fun appDetailsIntent(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun open(context: Context, vararg intents: Intent) {
        val activity = context.findActivity()
        for (intent in intents) {
            // Without an activity (never from the UI), start a fresh task so an old one can't show instead.
            if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            try {
                (activity ?: context).startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
