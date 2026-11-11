package app.sprout.habits.widget

import app.sprout.habits.ui.theme.WidgetType
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.R

/** A slim strip: "5 of 8 today", the next habit, and one bar per habit. */
class StripWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataSource(context.container.repository, context.container.settings).today()
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            val live by liveWidgetData(context, data) { it.today() }
            WeftGlanceTheme { StripContent(live, bitmaps) }
        }
    }
}

@Composable
private fun StripContent(data: TodayWidgetData, bitmaps: WidgetBitmaps) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val size = LocalSize.current
    val layout = WidgetLayout.strip(size.width.value, size.height.value)
    val width = (size.width.value - 36).coerceAtLeast(40f)
    fun color(p: androidx.glance.unit.ColorProvider) = p.getColor(context)
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(WidgetCorner)
            .padding(horizontal = StripSidePadding, vertical = layout.padV.dp)
            .clickable(actionStartActivity(context.openAppIntent())),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        val count = context.getString(R.string.widget_of_today, data.done, data.total)
        val next = data.nextLabel?.let { context.getString(R.string.widget_next, it) }
            ?: context.getString(
                when {
                    data.total == 0 -> R.string.widget_nothing_today
                    data.done == data.total -> R.string.widget_all_done
                    else -> R.string.widget_all_logged // something was skipped
                },
            )
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(count, maxLines = 1, style = TextStyle(fontSize = WidgetType.supporting, fontWeight = FontWeight.Bold, color = colors.onSurface))
            if (layout.showNext) {
                Spacer(GlanceModifier.width(StripTextGap))
                // Glance's Text can't ellipsize, so shorten "Next: …" to the room left after the count.
                val room = width - 12f - textWidthDp(context, count, WidgetType.supporting.value, bold = true)
                Text(
                    ellipsizeToWidth(context, next, WidgetType.supporting.value, room),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(fontSize = WidgetType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.End),
                )
            }
        }
        if (!layout.showBar) return@Column
        Spacer(GlanceModifier.height(StripBarGap))
        Image(
            ImageProvider(bitmaps.strip(data.today, width, 10f, track = color(colors.outline).copy(alpha = 0.35f), skip = color(colors.surfaceVariant))),
            contentDescription = LocalContext.current.getString(R.string.widget_habits_done_today, data.done, data.total),
            modifier = GlanceModifier.fillMaxWidth().height(StripBarHeight),
        )
    }
}

private fun paint(context: Context, sp: Float, bold: Boolean) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
    val dm = context.resources.displayMetrics
    textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

/** Rough width in dp of [text]; the launcher's font may be wider than the default, hence the margin. */
private fun textWidthDp(context: Context, text: String, sp: Float, bold: Boolean): Float =
    paint(context, sp, bold).measureText(text) / context.resources.displayMetrics.density * FONT_MARGIN

/** [text] cut with "…" so it fits [widthDp] (with the same margin as [textWidthDp]). */
private fun ellipsizeToWidth(context: Context, text: String, sp: Float, widthDp: Float): String {
    if (widthDp <= 0f) return ""
    val px = widthDp * context.resources.displayMetrics.density / FONT_MARGIN
    return TextUtils.ellipsize(text, paint(context, sp, bold = false), px, TextUtils.TruncateAt.END).toString()
}

/** Wide system fonts (like Samsung's or custom OEM fonts) run up to ~15% wider than Roboto. */
private const val FONT_MARGIN = 1.15f

class StripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StripWidget()
}
