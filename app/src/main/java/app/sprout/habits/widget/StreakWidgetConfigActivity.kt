package app.sprout.habits.widget

import app.sprout.habits.ui.theme.ScreenPadding
import app.sprout.habits.ui.theme.CardGap
import app.sprout.habits.ui.theme.SectionHeaderInset
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.HabitCardMinHeight
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.HabitCardPadding
import app.sprout.habits.ui.theme.TileTextGap
import app.sprout.habits.ui.components.HabitTile
import androidx.compose.foundation.layout.heightIn
import app.sprout.habits.ui.theme.WeftType
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.sprout.habits.ui.drawEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import app.sprout.habits.R
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.theme.WeftTheme
import app.sprout.habits.ui.theme.habitColors
import kotlinx.coroutines.launch

/** Shown when a streak widget is added: pick which habit it follows. */
class StreakWidgetConfigActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(app.sprout.habits.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without picking cancels adding the widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        drawEdgeToEdge(dark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
        setContent {
            WeftTheme {
                val habits by produceState<List<Habit>?>(null) {
                    value = WidgetDataSource(container.repository, container.settings).habitsForPicker()
                }
                LazyColumn(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
                    contentPadding = PaddingValues(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    item {
                        Text(stringResource(R.string.pick_a_habit), style = WeftType.title, modifier = Modifier.padding(SectionHeaderInset, Space8, SectionHeaderInset, Space12))
                    }
                    items(habits.orEmpty(), key = { it.id }) { habit -> HabitRow(habit) { pick(appWidgetId, habit.id) } }
                }
            }
        }
    }

    private fun pick(appWidgetId: Int, habitId: Long) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@StreakWidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@StreakWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply { this[StreakWidget.HabitKey] = habitId }
            }
            StreakWidget().update(this@StreakWidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun HabitRow(habit: Habit, onClick: () -> Unit) {
    val hc = habitColors(habit.colorHue.toFloat())
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = HabitCardMinHeight)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(HabitCardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The same habit row as everywhere: the 44 dp tile, 12 dp to the name.
        HabitTile(HabitIcon.fromKey(habit.icon).drawable, hc.soft, hc.ink)
        Text(habit.name, style = WeftType.cardTitle, modifier = Modifier.padding(start = TileTextGap))
    }
}
