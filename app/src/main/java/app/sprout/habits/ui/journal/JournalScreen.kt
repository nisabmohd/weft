package app.sprout.habits.ui.journal

import app.sprout.habits.ui.theme.ChipShape
import app.sprout.habits.ui.theme.FabMargin
import app.sprout.habits.ui.theme.FabShape
import app.sprout.habits.ui.theme.IconSize
import app.sprout.habits.ui.theme.IconSizeTiny
import app.sprout.habits.ui.theme.SnackbarLift
import app.sprout.habits.ui.theme.SnackbarSideMargin
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.Space16
import app.sprout.habits.ui.theme.Space2
import app.sprout.habits.ui.theme.Space24
import app.sprout.habits.ui.theme.Space32
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.FabListEndPadding
import app.sprout.habits.ui.theme.HeaderChipsGap
import app.sprout.habits.ui.theme.ScreenPadding
import app.sprout.habits.ui.theme.TabHeaderPadding
import app.sprout.habits.ui.components.PendingUndoHost
import app.sprout.habits.ui.components.PromptSheet
import app.sprout.habits.ui.components.GroupGap
import app.sprout.habits.ui.components.SectionGapBelow
import app.sprout.habits.ui.components.SectionGapAbove
import app.sprout.habits.ui.components.SectionHeader
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.sprout.habits.ui.components.NoteCard
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import app.sprout.habits.ui.components.CustomRangeChip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.sheetBottomPadding
import app.sprout.habits.ui.components.ActionGroup
import app.sprout.habits.ui.components.ActionRow
import app.sprout.habits.ui.components.SheetHeader
import app.sprout.habits.ui.components.TabHeader
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import app.sprout.habits.ui.today.UndoSnackbarHost
import app.sprout.habits.ui.components.WeftSheet
import app.sprout.habits.ui.components.NoteCardUi
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import app.sprout.habits.ui.components.DateRangeSheet
import app.sprout.habits.ui.components.HabitFilterChip
import app.sprout.habits.ui.components.FilterChipRow
import app.sprout.habits.ui.components.groupedShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.pluralStringResource
import app.sprout.habits.ui.components.HabitFilterSheet
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.components.HabitFilterOption
import app.sprout.habits.ui.components.ButtonRole
import app.sprout.habits.ui.components.WeftButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import app.sprout.habits.ui.components.HeaderIconButton

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    weekStart: java.time.DayOfWeek,
    onAddNote: (() -> Unit)?,
    onOpenNote: ((Long) -> Unit)?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    var pickingRange by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<NoteCardUi?>(null) }
    var deleting by remember { mutableStateOf<NoteCardUi?>(null) }
    val snackbar = remember { SnackbarHostState() }
    PendingUndoHost(snackbar)
    val deletedText = stringResource(R.string.note_deleted)
    val undo = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.deleted.collectLatest { note ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(deletedText, actionLabel = undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreNote(note)
        }
    }
    val colors = MaterialTheme.colorScheme
    val res = LocalContext.current.resources
    val addNote = stringResource(R.string.add_note)
    Box(Modifier.fillMaxSize()) {
        // The header stays put while the list scrolls under it, so its buttons are always in reach.
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(TabHeaderPadding)) {
                // Every filter lives in a sheet; the chips under the title show what is applied.
                Column {
                    TabHeader(stringResource(R.string.journal_title)) {
                        state?.let { ui ->
                            HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.filter_by_date), active = ui.from != null) { pickingRange = true }
                            if (ui.options.isNotEmpty()) {
                                HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { choosing = true }
                            }
                        }
                    }
                    // No preset ranges: the row only shows the filters that are on, each with its ✕.
                    val ui = state
                    if (ui != null && (ui.from != null || ui.filter.isNotEmpty())) {
                        FilterChipRow(Modifier.padding(top = HeaderChipsGap)) {
                            if (ui.from != null) {
                                CustomRangeChip(
                                    label = ui.dateLabel,
                                    openLabel = stringResource(R.string.filter_by_date),
                                    clearLabel = stringResource(R.string.clear_date_filter),
                                    onOpen = { pickingRange = true },
                                    onClear = viewModel::clearRange,
                                )
                            }
                            if (ui.filter.isNotEmpty()) {
                                val picked = ui.options.filter { it.habitId in ui.filter }
                                HabitFilterChip(
                                    selected = picked,
                                    // Names while there is room for them; a count next to a picked range.
                                    label = if (ui.from == null) picked.joinToString(", ") { it.name } else pluralStringResource(R.plurals.habit_count, picked.size, picked.size),
                                    removeLabel = stringResource(R.string.remove_habit_filter),
                                    onOpen = { choosing = true },
                                    onClear = { viewModel.setFilter(emptySet()) },
                                )
                            }
                        }
                    }
                }
            
        }
        val shown = state
        if (shown != null && shown.days.isEmpty()) {
            // Nothing to list: centred in the space between the chips and Add note (journal-empty.png).
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = FabListEndPadding), contentAlignment = Alignment.Center) {
                if (shown.hasNotes) {
                    val picked = shown.options.filter { it.habitId in shown.filter }
                    val one = picked.singleOrNull()
                    EmptyJournal(
                        habit = one,
                        title = when {
                            shown.from != null && picked.isNotEmpty() -> stringResource(R.string.journal_empty_filters)
                            shown.from != null -> stringResource(R.string.journal_empty_range, shown.dateLabel)
                            one != null -> stringResource(R.string.journal_empty_one_habit, one.name)
                            else -> pluralStringResource(R.plurals.journal_empty_n_habits, picked.size, picked.size)
                        },
                        text = if (one != null && shown.from == null) stringResource(R.string.journal_empty_one_habit_text, one.name)
                            else stringResource(R.string.journal_empty_earlier),
                        action = stringResource(R.string.journal_show_all),
                        onAction = viewModel::showAllNotes,
                    )
                } else {
                    EmptyJournal(null, stringResource(R.string.journal_empty), stringResource(R.string.journal_empty_hint), action = null, onAction = {})
                }
            }
        } else LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = FabListEndPadding),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            val list = state?.days
            list.orEmpty().forEachIndexed { index, day ->
                item(key = "d${day.day}") {
                    // The day on the left, how many notes it has on the right. The list's own 2 dp
                    // gaps are taken off, so the header has the usual space above and below.
                    SectionHeader(
                        day.label,
                        Modifier.padding(top = if (index == 0) 0.dp else SectionGapAbove - GroupGap, bottom = SectionGapBelow - GroupGap),
                        meta = pluralStringResource(R.plurals.note_count, day.notes.size, day.notes.size),
                    )
                }
                // One group per day: 2 dp between its notes, big corners only on the outside.
                itemsIndexed(day.notes, key = { _, note -> note.id }) { i, note ->
                    NoteCard(note, onOpenNote, shape = groupedShape(i, day.notes.size), onLongPress = { actionsFor = it })
                }
            }
        }
        }
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (onAddNote != null && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(IconSize)) },
                text = { Text(stringResource(R.string.add_note), style = WeftType.label) },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = FabShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(FabMargin)
                    // Moves up while the Undo snackbar shows, like on Today.
                    .padding(bottom = if (snackbar.currentSnackbarData != null) SnackbarLift else 0.dp)
                    .semantics { contentDescription = addNote },
            )
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = SnackbarSideMargin))
    }
    val ui = state
    if (pickingRange && ui != null) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = stringResource(R.string.all_dates),
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.clearRange(); pickingRange = false },
            onDismiss = { pickingRange = false },
        )
    }
    actionsFor?.let { note ->
        NoteActionsSheet(
            note,
            onEdit = onOpenNote?.let { open -> { actionsFor = null; open(note.id) } },
            onDelete = { actionsFor = null; deleting = note },
            onDismiss = { actionsFor = null },
        )
    }
    deleting?.let { note ->
        // Asked first like Edit note's Delete; the snackbar then offers Undo.
        PromptSheet(
            title = stringResource(R.string.delete_note_title),
            action = stringResource(R.string.action_delete),
            onAction = { deleting = null; viewModel.deleteNote(note.id) },
            onDismiss = { deleting = null },
            actionRole = ButtonRole.Destructive,
        )
    }
    if (choosing && ui != null) {
        HabitFilterSheet(
            options = ui.options,
            selected = ui.filter,
            helper = stringResource(R.string.filter_helper_journal),
            applyLabel = { n -> if (n == 0) res.getString(R.string.journal_show_all) else res.getQuantityString(R.plurals.journal_show_for_habits, n, n) },
            onApply = { ids -> viewModel.setFilter(ids); choosing = false },
            onDismiss = { choosing = false },
        )
    }
}

/**
 * Nothing to list (journal-empty.png): two note cards (the front one with [habit]'s tile, or the
 * note icon), what is missing and why, and with [action] a secondary button that brings every
 * note back.
 */
@Composable
private fun EmptyJournal(habit: HabitFilterOption?, title: String, text: String, action: String?, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // About as wide as the mockup's text block, so lines break evenly on wide phones too.
    Column(Modifier.widthIn(max = EmptyTextWidth).padding(horizontal = Space24), horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyNotesPicture(habit)
        Text(title, style = WeftType.title, color = colors.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space32))
        Text(text, style = WeftType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space8))
        if (action != null) {
            WeftButton(action, ButtonRole.Secondary, onClick = onAction, modifier = Modifier.padding(top = Space24))
        }
    }
}

/** The empty state's text column, at most this wide. */
private val EmptyTextWidth = 300.dp

/** The empty state picture: a front note card and a smaller faded one behind it, on the 4 dp grid. */
private object EmptyArt {
    val width = 200.dp
    val height = 136.dp
    val frontWidth = 156.dp
    val frontHeight = 112.dp
    val backWidth = 120.dp
    val backHeight = 72.dp
    /** The back card sits this much lower than the front one. */
    val backDrop = Space8
    val corner = Space16
    val shadow = Space8
    val tileSize = 24.dp
    /** The grey lines that stand for text: 8 dp tall. */
    val lineHeight = Space8
    val nameLine = 60.dp
    val backLines = listOf(28.dp, 36.dp, 24.dp)
    val frontLines = listOf(84.dp, 108.dp, 64.dp)
}

/** Two tilted note cards: a faded one behind, and in front one with the habit's tile and text lines. */
@Composable
private fun EmptyNotesPicture(habit: HabitFilterOption?) {
    val colors = MaterialTheme.colorScheme
    val card = colors.surfaceContainerLowest
    val line = colors.surfaceContainerHigh
    Box(Modifier.size(width = EmptyArt.width, height = EmptyArt.height)) {
        // Back card: faded, tilted the other way, peeking out on the left.
        Column(
            Modifier.align(Alignment.CenterStart).offset(y = EmptyArt.backDrop).rotate(-5f).alpha(0.6f)
                .size(width = EmptyArt.backWidth, height = EmptyArt.backHeight).background(card, RoundedCornerShape(EmptyArt.corner)).padding(start = Space16, top = Space16),
            verticalArrangement = Arrangement.spacedBy(Space8),
        ) {
            EmptyArt.backLines.forEach { Box(Modifier.size(width = it, height = EmptyArt.lineHeight).background(line, CircleShape)) }
        }
        // Front card.
        Column(
            Modifier.align(Alignment.CenterEnd).rotate(4f)
                .shadow(EmptyArt.shadow, RoundedCornerShape(EmptyArt.corner), ambientColor = colors.onSurface.copy(alpha = 0.2f), spotColor = colors.onSurface.copy(alpha = 0.2f))
                .size(EmptyArt.frontWidth, EmptyArt.frontHeight).background(card, RoundedCornerShape(EmptyArt.corner)).padding(Space12),
            verticalArrangement = Arrangement.spacedBy(Space8),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (soft, ink) = habit?.let { habitColors(it.hue).let { hc -> hc.soft to hc.ink } } ?: (colors.secondaryContainer to colors.onSecondaryContainer)
                Box(Modifier.size(EmptyArt.tileSize).background(soft, ChipShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(habit?.icon ?: R.drawable.ic_sym_edit_note), contentDescription = null, tint = ink, modifier = Modifier.size(IconSizeTiny))
                }
                Box(Modifier.padding(start = Space8).size(width = EmptyArt.nameLine, height = EmptyArt.lineHeight).background(line, CircleShape))
            }
            Spacer(Modifier.size(Space2))
            EmptyArt.frontLines.forEach { Box(Modifier.size(width = it, height = EmptyArt.lineHeight).background(line, CircleShape)) }
        }
    }
}

/** Long-press on a note: its habit and text on top, then Edit, Share and Delete. */
@Composable
private fun NoteActionsSheet(note: NoteCardUi, onEdit: (() -> Unit)?, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.action_share)
    // Grouped action rows (`note-actions-sheet.png`): the habit and the note on top, Delete last in red.
    WeftSheet(onDismissRequest = onDismiss) {
        SheetHeader(note.habitName, note.text)
        val count = if (onEdit != null) 3 else 2
        var i = 0
        ActionGroup(Modifier.sheetBottomPadding()) {
            if (onEdit != null) ActionRow(R.drawable.ic_pen, stringResource(R.string.edit_note), i++, count, onEdit)
            ActionRow(R.drawable.ic_share, shareTitle, i++, count, {
                onDismiss()
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${note.habitName}\n${note.text}")
                context.startActivity(Intent.createChooser(send, shareTitle))
            })
            ActionRow(R.drawable.ic_delete, stringResource(R.string.delete_note), i++, count, onDelete, destructive = true)
        }
    }
}
