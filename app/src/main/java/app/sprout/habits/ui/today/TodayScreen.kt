package app.sprout.habits.ui.today

import app.sprout.habits.ui.theme.CardContentGap
import app.sprout.habits.ui.theme.CardPadding
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.FabMargin
import app.sprout.habits.ui.theme.FabShape
import app.sprout.habits.ui.theme.IconSize
import app.sprout.habits.ui.theme.SectionGap
import app.sprout.habits.ui.theme.SnackbarLift
import app.sprout.habits.ui.theme.SnackbarSideMargin
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.Space4
import app.sprout.habits.ui.theme.Space48
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.CardGap
import app.sprout.habits.ui.theme.FabListEndPadding
import app.sprout.habits.ui.theme.ScreenPadding
import app.sprout.habits.ui.theme.TabHeaderPadding
import app.sprout.habits.ui.components.PendingUndoHost
import app.sprout.habits.ui.components.InfoRow
import app.sprout.habits.ui.components.TitleLineGap
import app.sprout.habits.ui.components.PromptSheet
import app.sprout.habits.ui.components.ButtonRole
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.BuildConfig
import androidx.compose.runtime.getValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.BarSegment
import app.sprout.habits.ui.components.SegmentBar
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.TabHeader
import app.sprout.habits.support.SupportPromptSheet
import app.sprout.habits.ui.openUrl
import app.sprout.habits.data.update.PlayReview
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.sprout.habits.domain.DayOutcome
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    /** Habit to preselect (0 = none) and the epoch day shown. */
    onAddNote: (epochDay: Long) -> Unit,
    /** True while the sample habits a fresh install starts with are still there. */
    hasSampleData: Boolean = false,
    onRemoveSampleData: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshToday()
        viewModel.maybeShowSupportPrompt()
    }
    val showSupport by viewModel.supportPrompt.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val reviewScope = rememberCoroutineScope()
    if (showSupport) {
        SupportPromptSheet(
            showRate = BuildConfig.PLAY_LISTED,
            showSponsor = !BuildConfig.PLAY_STORE,
            // Google Play's rating card over the app; the store listing if Play can't show it.
            onRate = {
                viewModel.supportPromptClosed(dismissed = false)
                reviewScope.launch {
                    val activity = context.findActivity()
                    if (activity == null || !PlayReview.launch(activity)) openUrl(context, BuildConfig.PLAY_STORE_URL)
                }
            },
            onStar = { openUrl(context, BuildConfig.REPO_URL); viewModel.supportPromptClosed(dismissed = false) },
            onSponsor = { openUrl(context, BuildConfig.SPONSOR_URL); viewModel.supportPromptClosed(dismissed = false) },
            onLater = { viewModel.supportPromptClosed(dismissed = true) },
        )
    }
    DayLogHost(viewModel.logger, snackbar)
    PendingUndoHost(snackbar)
    var confirmRemoveSample by remember { mutableStateOf(false) }
    if (confirmRemoveSample) {
        PromptSheet(
            title = stringResource(R.string.sample_remove_title),
            text = stringResource(R.string.sample_remove_text),
            action = stringResource(R.string.action_remove),
            onAction = { confirmRemoveSample = false; onRemoveSampleData() },
            onDismiss = { confirmRemoveSample = false },
            actionRole = ButtonRole.Destructive,
        )
    }
    Box(Modifier.fillMaxSize()) {
        TodayContent(
            state,
            onSelectDay = viewModel::select,
            onAddHabit = onAddHabit,
            onDone = viewModel::markDone,
            onSkip = viewModel::markSkipped,
            onUndo = viewModel::resetDay,
            onToggle = viewModel::toggleDone,
            onLongPress = viewModel::openLogSheet,
            onOpen = onOpenHabit,
            sampleCard = if (hasSampleData) {
                { SampleDataCard(onRemove = { confirmRemoveSample = true }) }
            } else {
                null
            },
        )
        val addNote = stringResource(R.string.add_note)
        // Lift the button above the Undo snackbar while it shows, so the two never overlap.
        val fabLift by animateDpAsState(if (snackbar.currentSnackbarData != null) SnackbarLift else 0.dp, label = "fabLift")
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (state.habits.isNotEmpty() && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                // New note picks the habit, the same way as the Journal's Add note.
                onClick = { onAddNote(state.selectedDate.toEpochDay()) },
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(IconSize)) },
                text = { Text(stringResource(R.string.add_note), style = WeftType.label) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = FabShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(FabMargin).padding(bottom = fabLift).semantics { contentDescription = addNote },
            )
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = SnackbarSideMargin))
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    onSelectDay: (LocalDate) -> Unit,
    onAddHabit: () -> Unit,
    onDone: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onLongPress: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    sampleCard: (@Composable () -> Unit)?,
) {
    // The header stays put while the list scrolls under it, so its buttons are always in reach.
    Column(Modifier.fillMaxSize()) {
    Box(Modifier.padding(TabHeaderPadding)) {
 Header(state, onAddHabit) 
    }
    val listState = rememberLazyListState()
    // On first run the banner can arrive a moment after the list: it is added above the score
    // card, and the list would stay on that card with the banner hidden above it.
    LaunchedEffect(sampleCard != null) {
        if (sampleCard != null && listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset == 0) listState.scrollToItem(0)
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = FabListEndPadding),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        // Nothing until the first data is in, or the app opens on a "0% of today" card that
        // is replaced a moment later.
        if (state.loading) return@LazyColumn
        if (sampleCard != null) item(key = "sample") { sampleCard() }
        item(key = "score") { ScoreCard(state) }
        if (state.habits.isEmpty()) {
            item(key = "empty") { EmptyState() }
        }
        items(state.habits, key = { it.id }) { habit ->
            SwipeableHabitCard(
                habit,
                onDone = { onDone(habit.id) },
                onSkip = { onSkip(habit.id) },
                onUndo = { onUndo(habit.id) },
                onToggle = { onToggle(habit.id) },
                onLongPress = { onLongPress(habit.id) },
                onClick = { onOpen(habit.id) },
            )
        }
    }
    }
}

@Composable
private fun Header(state: TodayUiState, onAddHabit: () -> Unit) {
    TabHeader(
        title = if (state.isToday) stringResource(R.string.today) else state.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        subtitle = state.dateLabel,
    ) {
        HeaderIconButton(R.drawable.ic_plus, stringResource(R.string.new_habit), onClick = onAddHabit)
    }
}

@Composable
private fun ScoreCard(state: TodayUiState) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            // With the list's 8 dp, 16 dp from the score card to the habits (a section without a header).
            .padding(bottom = SectionGap - CardGap)
            .background(colors.surfaceContainerLowest, CardShape)
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.percent, state.scorePercent), style = WeftType.screenTitleNumber, color = colors.onSurface)
            Spacer(Modifier.width(Space8))
            Text(
                stringResource(if (state.isToday) R.string.of_today else R.string.of_the_day),
                style = WeftType.supporting,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Space4),
            )
            Spacer(Modifier.width(Space12))
            // Takes the rest of the row and wraps at large text sizes.
            Text(
                summary(state),
                style = WeftType.label,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f).padding(bottom = Space4),
            )
        }
        val summary = summary(state)
        Segments(state.habits, Modifier.semantics { contentDescription = summary })
    }
}

/** "1 done · 2 partial · 3 left"; each count stays on one line with its word when text wraps. */
@Composable
private fun summary(state: TodayUiState): String = buildList {
    add(pluralStringResource(R.plurals.count_done, state.doneCount, state.doneCount))
    if (state.partialCount > 0) add(pluralStringResource(R.plurals.count_partial, state.partialCount, state.partialCount))
    if (state.openCount > 0) add(pluralStringResource(R.plurals.count_left, state.openCount, state.openCount))
}.joinToString(" · ")

/** One bar per habit: solid when done, part-filled when partial, faint when skipped. */
@Composable
private fun Segments(habits: List<HabitRowUi>, modifier: Modifier = Modifier) {
    SegmentBar(
        habits.map {
            val fill = when (it.outcome) {
                DayOutcome.DONE -> 1f
                DayOutcome.PARTIAL -> it.progress
                else -> 0f
            }
            BarSegment(it.hue, fill, skipped = it.outcome == DayOutcome.SKIP)
        },
        modifier,
    )
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space48),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TitleLineGap),
    ) {
        Text(stringResource(R.string.today_empty), style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.today_empty_hint), style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Shown while the sample habits are there: one info row (design 2026-11-11), "Sample habits ·
 * Swipe or hold to try them" and Remove, which asks first.
 */
@Composable
private fun SampleDataCard(onRemove: () -> Unit) {
    InfoRow(
        R.drawable.ic_test_tube,
        stringResource(R.string.sample_card_title),
        stringResource(R.string.sample_card_body),
        button = stringResource(R.string.sample_remove),
        onButton = onRemove,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
