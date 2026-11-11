package app.sprout.habits.ui.habits

import app.sprout.habits.ui.theme.BorderThin
import app.sprout.habits.ui.theme.CardContentGap
import app.sprout.habits.ui.theme.CardPadding
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.IconLabelGap
import app.sprout.habits.ui.theme.IconSizeSmall
import app.sprout.habits.ui.theme.LegendSwatchSize
import app.sprout.habits.ui.theme.SectionGap
import app.sprout.habits.ui.theme.SectionHeaderInset
import app.sprout.habits.ui.theme.SnackbarSideMargin
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.Space16
import app.sprout.habits.ui.theme.Space2
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.TileTextGap
import app.sprout.habits.ui.theme.TrailingGap
import app.sprout.habits.ui.theme.WeekMarkSize
import app.sprout.habits.ui.theme.CardGap
import app.sprout.habits.ui.theme.ListEndPadding
import app.sprout.habits.ui.theme.ScreenPadding
import app.sprout.habits.ui.theme.TabHeaderPadding
import app.sprout.habits.ui.components.TitleLineGap
import app.sprout.habits.ui.components.PendingUndoHost
import androidx.compose.runtime.getValue
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.components.tick
import androidx.compose.ui.platform.LocalHapticFeedback
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.sprout.habits.ui.today.DayLogHost
import app.sprout.habits.ui.today.UndoSnackbarHost
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.TabHeader
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.components.DayMarkView
import app.sprout.habits.ui.components.MarkColors
import app.sprout.habits.ui.components.MarkKind
import app.sprout.habits.ui.theme.habitColors
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    onAddHabit: () -> Unit,
    onManage: () -> Unit,
    onOpenHabit: (Long) -> Unit,
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val overall by viewModel.overall.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }
    val colors = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    DayLogHost(viewModel.logger, snackbar)
    PendingUndoHost(snackbar)

    Box(Modifier.fillMaxSize()) {
    // The header stays put while the list scrolls under it, so its buttons are always in reach.
    Column(Modifier.fillMaxSize()) {
    Box(Modifier.padding(TabHeaderPadding)) {
            TabHeader(stringResource(R.string.tab_habits)) {
                HeaderIconButton(R.drawable.ic_drag, stringResource(R.string.manage_habits), onClick = onManage)
                HeaderIconButton(R.drawable.ic_plus, stringResource(R.string.new_habit), onClick = onAddHabit)
            }
        
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = ListEndPadding),
        // Cards sit 8 dp apart on every tab.
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        item(key = "mode") {
            // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
            CappedFontScale {
            val haptics = LocalHapticFeedback.current
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = SectionGap - CardGap)) {
                HabitsMode.entries.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { if (mode != m) haptics.tick(); viewModel.setMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(index, HabitsMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = colors.primaryContainer, activeContentColor = colors.onPrimaryContainer),
                    ) { Text(stringResource(if (m == HabitsMode.WEEK) R.string.week else R.string.overall), style = WeftType.label) }
                }
            }
            }
        }
        when (mode) {
            HabitsMode.WEEK -> week?.let { w ->
                item(key = "week-nav") { WeekNavigator(w, viewModel::previousWeek, viewModel::nextWeek) }
                items(w.habits, key = { "w${it.id}" }) { WeekCard(it, w, onOpenHabit) { i -> viewModel.logger.open(it.id, w.dates[i]) } }
            }
            HabitsMode.OVERALL -> overall?.let { o ->
                item(key = "legend") { Legend() }
                items(o.habits, key = { "o${it.id}" }) { OverallCard(it, o, onOpenHabit) }
            }
        }
    }
    }
    UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = SnackbarSideMargin))
    }
}

@Composable
private fun WeekNavigator(week: WeekUi, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous_week), modifier = Modifier.size(IconSizeSmall))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(week.label, style = WeftType.cardTitle, color = colors.onBackground)
            Text(stringResource(R.string.week_complete, week.percent), style = WeftType.supporting, color = colors.onSurfaceVariant)
        }
        IconButton(onClick = onNext, enabled = week.canGoForward) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next_week), modifier = Modifier.size(IconSizeSmall))
        }
    }
}

@Composable
private fun CardHeader(icon: Int, hue: Float, title: String, subtitle: String?, trailing: @Composable () -> Unit) {
    val hc = habitColors(hue)
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The same 44 dp tile and 22 dp icon as a Today habit card.
        HabitTile(icon, hc.soft, hc.ink)
        Column(Modifier.weight(1f).padding(start = TileTextGap, end = TrailingGap), verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
            Text(title, style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing()
    }
}

@Composable
private fun markColors() = MaterialTheme.colorScheme.let {
    MarkColors(skip = it.outlineVariant, skipInk = it.onSurfaceVariant, outline = it.outline)
}

@Composable
private fun WeekCard(habit: HabitWeekUi, week: WeekUi, onOpen: (Long) -> Unit, onEditDay: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.hue)
    val mc = markColors()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surfaceContainerLowest)
            .clickable(onClickLabel = stringResource(R.string.open_habit, habit.name)) { onOpen(habit.id) }
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        CardHeader(habit.icon, habit.hue, habit.name, null) {
            Text(habit.goal, style = WeftType.supporting, color = colors.onSurfaceVariant)
        }
        // Seven equal columns that share the card's width, so they fit narrow phones; the day
        // labels stop scaling at 1.3x so they never wrap.
        CappedFontScale {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space2)) {
            habit.marks.forEachIndexed { i, mark ->
                val isToday = i == week.todayIndex
                // Any scheduled day up to today can be edited; future and unscheduled days can't.
                val editable = mark.kind != MarkKind.FUTURE && mark.kind != MarkKind.NOT_SCHEDULED
                val editLabel = stringResource(R.string.edit_day, week.dayNames[i])
                val description = stringResource(R.string.mark_description, habit.name, week.dayNames[i], describe(mark.kind, mark.fraction))
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Space16))
                        .background(if (isToday) colors.surfaceContainerHigh else colors.surfaceContainerLowest)
                        .then(if (editable) Modifier.clickable(onClickLabel = editLabel) { onEditDay(i) } else Modifier)
                        .padding(vertical = Space8)
                        .clearAndSetSemantics {
                            contentDescription = description
                            if (editable) onClick(editLabel) { onEditDay(i); true }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space8),
                ) {
                    Text(
                        week.dayLabels[i],
                        style = WeftType.tiny,
                        color = if (isToday) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                    DayMarkView(mark, hc, mc, Modifier.size(WeekMarkSize))
                }
            }
        }
        }
    }
}

@Composable
private fun describe(kind: MarkKind, fraction: Float) = when (kind) {
    MarkKind.DONE -> stringResource(R.string.mark_done_lower)
    MarkKind.PARTIAL -> stringResource(R.string.mark_partial_lower, (fraction * 100).toInt())
    MarkKind.SKIP -> stringResource(R.string.mark_skipped_lower)
    MarkKind.OPEN_TODAY -> stringResource(R.string.mark_open_lower)
    MarkKind.FUTURE -> stringResource(R.string.mark_future_lower)
    MarkKind.NOT_SCHEDULED -> stringResource(R.string.mark_not_scheduled_lower)
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        // With the list's 8 dp, the legend has the same 16 dp under it as above it.
        Modifier.fillMaxWidth().padding(start = SectionHeaderInset, end = SectionHeaderInset, bottom = Space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(pluralStringResource(R.plurals.last_weeks, OVERALL_WEEKS, OVERALL_WEEKS), style = WeftType.label, color = colors.onSurfaceVariant, modifier = Modifier.padding(end = TrailingGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
        LegendItem(stringResource(R.string.outcome_done)) { drawRoundRect(colors.onSurfaceVariant, cornerRadius = CornerRadius(size.width * 0.3f)) }
        // Neutral greys: the cells themselves are in each habit's own color.
        LegendItem(stringResource(R.string.outcome_partial)) { drawRoundRect(colors.onSurfaceVariant.copy(alpha = 0.45f), cornerRadius = CornerRadius(size.width * 0.3f)) }
        LegendItem(stringResource(R.string.outcome_skipped)) { drawRoundRect(colors.outlineVariant, cornerRadius = CornerRadius(size.width * 0.3f)) }
        }
    }
}

@Composable
private fun LegendItem(label: String, draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    Row(Modifier.padding(start = Space12), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(LegendSwatchSize), onDraw = draw)
        Text(label, style = WeftType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = IconLabelGap))
    }
}

@Composable
private fun OverallCard(habit: HabitOverallUi, overall: OverallUi, onOpen: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surfaceContainerLowest)
            .clickable(onClickLabel = stringResource(R.string.open_habit, habit.name)) { onOpen(habit.id) }
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        CardHeader(habit.icon, habit.hue, habit.name, pluralStringResource(R.plurals.best_streak_days, habit.bestStreak, habit.bestStreak)) {
            Text(
                pluralStringResource(R.plurals.streak_days, habit.currentStreak, habit.currentStreak),
                style = WeftType.supporting,
                color = colors.onSurfaceVariant,
            )
        }
        // Labels sit over fixed heatmap columns, so they stop scaling at 1.3x.
        CappedFontScale {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val column = maxWidth / OVERALL_WEEKS
            Box(Modifier.fillMaxWidth()) {
                overall.months.forEach { (label, col) ->
                    Text(
                        label,
                        style = WeftType.tiny,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.offset(x = column * col),
                    )
                }
            }
        }
        }
        Heatmap(habit)
    }
}

/** 26 columns (weeks) × 7 rows (days). Done = solid, partial = light, skipped = outline. */
@Composable
private fun Heatmap(habit: HabitOverallUi) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.hue)
    val outline = colors.outlineVariant
    val faint = colors.surfaceContainer
    val description = pluralStringResource(R.plurals.heatmap_description, habit.currentStreak, habit.name, habit.currentStreak, habit.bestStreak, OVERALL_WEEKS)
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(OVERALL_WEEKS / 7f)
            .semantics { contentDescription = description },
    ) {
        val pitch = size.width / OVERALL_WEEKS
        val cell = pitch * 0.72f
        val radius = CornerRadius(cell * 0.3f)
        val stroke = BorderThin.toPx()
        habit.cells.forEachIndexed { i, mark ->
            val col = i / 7
            val row = i % 7
            val topLeft = Offset(col * pitch + (pitch - cell) / 2, row * pitch + (pitch - cell) / 2)
            val s = Size(cell, cell)
            when (mark.kind) {
                MarkKind.DONE -> drawRoundRect(hc.solid, topLeft, s, radius)
                MarkKind.PARTIAL -> drawRoundRect(hc.mid, topLeft, s, radius)
                // Filled grey, no border.
                MarkKind.SKIP -> drawRoundRect(outline, topLeft, s, radius)
                MarkKind.OPEN_TODAY -> drawRoundRect(hc.solid, topLeft + Offset(stroke, stroke), Size(cell - 2 * stroke, cell - 2 * stroke), radius, style = Stroke(stroke * 1.5f))
                MarkKind.NOT_SCHEDULED -> drawRoundRect(faint, topLeft, s, radius)
                MarkKind.FUTURE -> Unit
            }
        }
    }
}
