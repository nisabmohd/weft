package app.sprout.habits

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.sprout.habits.widget.StreakWidgetReceiver
import app.sprout.habits.widget.StripWidgetReceiver
import app.sprout.habits.widget.TodayWidgetReceiver
import app.sprout.habits.widget.WeekWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.graphics.drawable.GradientDrawable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.sprout.habits.ui.drawEdgeToEdge
import app.sprout.habits.widget.StreakWidget
import app.sprout.habits.widget.WidgetDataSource

/**
 * Debug builds only: all four widgets at once, at chosen sizes, over the home screen wallpaper,
 * for the store and README shots (a launcher puts them at its own sizes, too big for one page).
 * They are real widgets, hosted the way a launcher hosts them. Needs the bind permission first:
 * `adb shell appwidget grantbind --package app.sprout.habits`, then
 * `adb shell am start -n app.sprout.habits/.WidgetBoardActivity`.
 */
class WidgetBoardActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private val ids = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, HOST_ID)
        host.deleteHost() // a clean start: no widgets left from an earlier run
        val manager = AppWidgetManager.getInstance(this)
        val gap = dp(12f)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(56f), dp(16f), dp(16f))
            // A calm wallpaper of its own (the emulator's default one is busy): warm rose-brown,
            // dark at the bottom, so the pale widgets stand out (user, 2026-11-08).
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF9C6B62.toInt(), 0xFF6A3B35.toInt(), 0xFF2B1715.toInt()),
            )
        }
        val streakIds = mutableListOf<Int>()
        fun widget(receiver: Class<*>, widthDp: Float, heightDp: Float, label: String): View {
            val id = host.allocateAppWidgetId().also(ids::add)
            val provider = ComponentName(this, receiver)
            // The size goes with the bind, so the widget's first draw is already at this size.
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp.toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp.toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp.toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp.toInt())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(SizeF(widthDp, heightDp)))
                }
            }
            check(manager.bindAppWidgetIdIfAllowed(id, provider, options)) { "run: adb shell appwidget grantbind --package $packageName" }
            val info = manager.getAppWidgetInfo(id)
            val view = host.createView(this, id, info)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(options, listOf(SizeF(widthDp, heightDp)))
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(options, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
            }
            manager.updateAppWidgetOptions(id, options)
            if (receiver == StreakWidgetReceiver::class.java) streakIds += id
            // A label TalkBack and the capture script can find the widget by.
            return FrameLayout(this).apply {
                contentDescription = label
                addView(view, FrameLayout.LayoutParams(dp(widthDp), dp(heightDp)))
            }
        }
        // Five widgets, all at sizes where nothing is cut: Today, This week (five habits) and the
        // strip full width, then two streak widgets side by side (the best two streaks). Fits 1080 x 2400.
        val widthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density - 32f
        val half = (widthDp - 12f) / 2f
        column.addView(widget(TodayWidgetReceiver::class.java, widthDp, 160f, "widget-today"))
        column.addView(space(gap))
        column.addView(widget(WeekWidgetReceiver::class.java, widthDp, 384f, "widget-week"))
        column.addView(space(gap))
        column.addView(widget(StripWidgetReceiver::class.java, widthDp, 84f, "widget-strip"))
        column.addView(space(gap))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
        row.addView(widget(StreakWidgetReceiver::class.java, half, 150f, "widget-streak"))
        row.addView(space(gap))
        row.addView(widget(StreakWidgetReceiver::class.java, half, 150f, "widget-streak-2"))
        column.addView(row)
        setContentView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        drawEdgeToEdge(dark = true)
        // Each streak widget on its own habit (the two best streaks), then draw them all with today's data.
        val app = application as WeftApp
        CoroutineScope(Dispatchers.Default).launch {
            val source = WidgetDataSource(app.container.repository, app.container.settings)
            val best = app.container.repository.observeHabits().first()
                .filter { !it.archived }
                .mapNotNull { source.streak(it.id) }
                .sortedByDescending { it.streak }
                .map { it.habit.id }
            streakIds.zip(best).forEach { (appWidgetId, habitId) ->
                val glanceId = GlanceAppWidgetManager(this@WidgetBoardActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@WidgetBoardActivity, PreferencesGlanceStateDefinition, glanceId) {
                    it.toMutablePreferences().apply { this[StreakWidget.HabitKey] = habitId }
                }
            }
            app.container.widgets.updateAll()
            StreakWidget().updateAll(this@WidgetBoardActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        host.startListening()
    }

    override fun onStop() {
        host.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        ids.forEach(host::deleteAppWidgetId)
        super.onDestroy()
    }

    private fun space(px: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(px, px) }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private companion object {
        const val HOST_ID = 4711
    }
}
