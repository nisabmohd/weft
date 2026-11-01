package app.sprout.habits.widget

import app.sprout.habits.ui.theme.WidgetType
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

/** Today's ring and the next habits still to do, each with a circle to mark it done. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataSource(context.container.repository, context.container.settings).today()
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            val live by liveWidgetData(context, data) { it.today() }
            WeftGlanceTheme { TodayContent(live, bitmaps) }
        }
    }
}

@Composable
private fun TodayContent(data: TodayWidgetData, bitmaps: WidgetBitmaps) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val size = LocalSize.current
    val layout = WidgetLayout.today(size.width.value, size.height.value)
    val ringDp = layout.ring
    val ring = bitmaps.ring(
        progress = if (data.total == 0) 0f else data.done.toFloat() / data.total,
        color = colors.primary.getColor(context),
        track = colors.surfaceVariant.getColor(context),
        sizeDp = ringDp,
        strokeDp = (ringDp * 0.1f).coerceIn(3f, 10f),
    )
    val heading = context.getString(
        when {
            // A day with nothing scheduled (or every habit hidden from widgets) isn't "all done".
            data.total == 0 -> R.string.widget_nothing_today
            data.upNext.isEmpty() -> R.string.widget_all_done_today
            else -> R.string.widget_up_next
        },
    )
    Row(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(layout.pad.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = if (layout.showList) Alignment.Horizontal.Start else Alignment.Horizontal.CenterHorizontally,
    ) {
        Box(GlanceModifier.size(ringDp.dp).clickable(actionStartActivity(context.openAppIntent())), contentAlignment = Alignment.Center) {
            Image(ImageProvider(ring), contentDescription = context.getString(R.string.widget_done_today, data.done, data.total), modifier = GlanceModifier.size(ringDp.dp))
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text(
                    context.getString(R.string.widget_ratio, data.done, data.total),
                    maxLines = 1,
                    style = TextStyle(fontSize = if (layout.bigCount) WidgetType.screenTitle else WidgetType.cardTitle, fontWeight = FontWeight.Medium, color = colors.onSurface),
                )
                if (layout.ringLabel) {
                    Text(context.getString(R.string.widget_today_lower), style = TextStyle(fontSize = WidgetType.caption, color = colors.onSurfaceVariant))
                }
            }
        }
        if (!layout.showList) return@Row
        Spacer(GlanceModifier.width(if (layout.pad < 16f) 10.dp else 16.dp))
        Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            if (layout.nextLine) {
                // Too short for a row: one line, like the strip.
                Text(
                    data.upNext.firstOrNull()?.let { context.getString(R.string.widget_next, it.name) } ?: heading,
                    maxLines = 1,
                    style = TextStyle(fontSize = WidgetType.supporting, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant),
                )
                return@Column
            }
            if (layout.showHeader || data.upNext.isEmpty()) {
                Text(heading, maxLines = 1, style = TextStyle(fontSize = WidgetType.supporting, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant))
            }
            data.upNext.take(layout.rows).forEachIndexed { i, habit ->
                val hc = habitColors(habit.hue, bitmaps.dark)
                if (i > 0 || layout.showHeader) Spacer(GlanceModifier.height(WidgetLayout.ROW_GAP.dp))
                // Two sibling tap areas, never nested: the name opens the habit, the circle marks it
                // done. Nested clickables let the outer one win on some launchers.
                Row(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(WidgetLayout.ROW.dp)
                        .background(ColorProvider(hc.soft, hc.soft))
                        .cornerRadius(16.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Box(
                        GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(start = 14.dp)
                            .clickable(actionStartActivity(context.openHabitIntent(habit.id))),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            habit.name,
                            maxLines = 1,
                            style = TextStyle(fontSize = WidgetType.cardTitle, fontWeight = FontWeight.Medium, color = ColorProvider(hc.ink, hc.ink)),
                        )
                    }
                    Box(
                        GlanceModifier
                            .size(48.dp)
                            .clickable(actionRunCallback<MarkDoneAction>(actionParametersOf(MarkDoneAction.HabitIdKey to habit.id))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            ImageProvider(R.drawable.widget_circle),
                            contentDescription = context.getString(R.string.mark_done, habit.name),
                            colorFilter = ColorFilter.tint(ColorProvider(hc.solid, hc.solid)),
                            modifier = GlanceModifier.size(34.dp),
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}
