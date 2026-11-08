package app.sprout.habits.notify

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import app.sprout.habits.ui.alarm.AlarmActivity

/**
 * The optional "Show over other apps" fallback (features/FEATURE-ALARM-REMINDERS.md 7): some
 * phones (OnePlus / ColorOS seen, 2026-11-08) never open an alarm's full-screen intent, locked or
 * not. When the user allowed drawing over other apps, Weft opens the ringing screen itself if it
 * isn't up shortly after the alarm starts: on the lock screen and while the phone is in use too
 * (user, 2026-11-08: with everything allowed it should always fill the screen, not stay a card),
 * except during a call. Closing it returns to the app the person was in (its own task).
 */
object AlarmScreenFallback {
    /** Time Android gets to open the full-screen intent by itself first. */
    private const val WAIT_MS = 1_200L

    /** A second try if the screen still isn't up (the first start can be refused while the overlay is new). */
    private const val RETRY_MS = 1_500L

    /** How long the 1-pixel window stays: Android 15+ only lets an overlay app start a screen while one is visible. */
    private const val OVERLAY_MS = 3_000L

    private val main = Handler(Looper.getMainLooper())

    /** True during a phone or video call, or while a call is ringing in. */
    fun inCall(context: Context): Boolean =
        context.getSystemService(AudioManager::class.java).mode in
            setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_RINGTONE)

    /** After the alarm for [habitId] starts ringing; [stillRinging] says it hasn't been answered meanwhile. */
    fun schedule(context: Context, habitId: Long, stillRinging: () -> Boolean) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        // In a call the screen never opens; the heads-up card is enough (design 2026-11-08, 12).
        val needed = { stillRinging() && !AlarmActivity.showing && !inCall(app) }
        main.postDelayed({
            if (!needed()) return@postDelayed
            open(app, habitId)
            main.postDelayed({ if (needed()) open(app, habitId) }, RETRY_MS)
        }, WAIT_MS)
    }

    /**
     * Shows a 1-pixel overlay and starts the ringing screen once that overlay is drawn: Android 15+
     * checks for a visible overlay window, and one only just added doesn't count yet.
     */
    private fun open(context: Context, habitId: Long) {
        val windows = context.getSystemService(WindowManager::class.java)
        val view = View(context)
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        val start = { runCatching { context.startActivity(AlarmActivity.intent(context, habitId)) } }
        val added = runCatching { windows.addView(view, params) }.isSuccess
        if (!added) {
            start()
            return
        }
        var started = false
        view.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                if (started) return
                started = true
                // A listener can't remove itself while drawing; the next frame is soon enough.
                main.post {
                    view.viewTreeObserver.removeOnDrawListener(this)
                    start()
                }
            }
        })
        // If the overlay never draws (screen off on some phones), start anyway.
        main.postDelayed({ if (!started) { started = true; start() } }, 400L)
        main.postDelayed({ runCatching { windows.removeView(view) } }, OVERLAY_MS)
    }
}
