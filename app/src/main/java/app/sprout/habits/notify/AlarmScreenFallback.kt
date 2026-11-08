package app.sprout.habits.notify

import android.app.KeyguardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import app.sprout.habits.ui.alarm.AlarmActivity

/**
 * The optional "Show over other apps" fallback (features/FEATURE-ALARM-REMINDERS.md 7): some
 * phones (OnePlus / ColorOS seen, 2026-11-08) never open an alarm's full-screen intent. When the
 * user allowed drawing over other apps and the phone is locked or asleep, Weft opens the ringing
 * screen itself if it isn't up shortly after the alarm starts. While the phone is in use the
 * heads-up card is right, so nothing happens then.
 */
object AlarmScreenFallback {
    /** Time Android gets to open the full-screen intent by itself first. */
    private const val WAIT_MS = 1_500L

    /** How long the 1-pixel window stays: Android 15+ only lets an overlay app start a screen while one shows. */
    private const val OVERLAY_MS = 2_000L

    private val main = Handler(Looper.getMainLooper())

    /** After the alarm for [habitId] starts ringing; [stillRinging] says it hasn't been answered meanwhile. */
    fun schedule(context: Context, habitId: Long, stillRinging: () -> Boolean) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        main.postDelayed({
            if (!stillRinging() || AlarmActivity.showing) return@postDelayed
            val inUse = app.getSystemService(PowerManager::class.java).isInteractive &&
                !app.getSystemService(KeyguardManager::class.java).isKeyguardLocked
            if (!inUse) open(app, habitId)
        }, WAIT_MS)
    }

    private fun open(context: Context, habitId: Long) {
        val windows = context.getSystemService(WindowManager::class.java)
        val view = View(context)
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        val added = runCatching { windows.addView(view, params) }.isSuccess
        runCatching { context.startActivity(AlarmActivity.intent(context, habitId)) }
        if (added) main.postDelayed({ runCatching { windows.removeView(view) } }, OVERLAY_MS)
    }
}
