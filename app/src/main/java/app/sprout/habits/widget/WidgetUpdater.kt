package app.sprout.habits.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Refreshes every widget after any change to habits, entries, notes or settings. */
@OptIn(FlowPreview::class)
class WidgetUpdater(
    private val context: Context,
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        WidgetDayReceiver.scheduleNextDay(context)
        scope.launch {
            // Every entry, not a window around today: editing an old day changes a streak, and a
            // window fixed at process start would go stale after a few days.
            combine(
                repository.observeAllHabits(),
                repository.observeEntries(0, Long.MAX_VALUE),
                settings.settings,
            ) { _, _, _ -> Unit }
                .debounce(300)
                .collect { updateAll() }
        }
    }

    suspend fun updateAll() {
        val manager = GlanceAppWidgetManager(context)
        if (manager.getGlanceIds(WeekWidget::class.java).isNotEmpty()) WeekWidget().updateAll(context)
        if (manager.getGlanceIds(TodayWidget::class.java).isNotEmpty()) TodayWidget().updateAll(context)
        if (manager.getGlanceIds(StripWidget::class.java).isNotEmpty()) StripWidget().updateAll(context)
        if (manager.getGlanceIds(StreakWidget::class.java).isNotEmpty()) StreakWidget().updateAll(context)
    }
}
