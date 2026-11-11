package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.Note
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.formatNumber
import app.sprout.habits.domain.measure
import app.sprout.habits.ui.datePattern
import app.sprout.habits.ui.theme.habitColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
/** One note as the Journal and a habit's detail show it. */
@Immutable
data class NoteCardUi(
    val id: Long,
    /** Epoch day the note is about. */
    val day: Long,
    val habitName: String,
    val icon: Int,
    val hue: Float,
    /** That day's outcome for the habit, drawn like the Week view marks. */
    val mark: DayMark,
    /** "Skipped", "8 of 20 pages", "Done · 45 min". */
    val outcome: String,
    /**
     * When the note was last written: "9:12 PM", or "written Thu, 9:12 PM" ("written 2 Oct, …"
     * after 6 days) when that wasn't the day the note is about, so a late note doesn't read as
     * written on its own day.
     */
    val time: String,
    val text: String,
) {
    companion object {
        // Built on each use so a change of language shows up.
        private val TIME get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        fun of(note: Note, habit: Habit, entry: Entry?, today: LocalDate, strings: Strings): NoteCardUi {
            val (mark, outcome) = dayOutcome(habit, entry, note.date, today.toEpochDay(), strings)
            return NoteCardUi(
                id = note.id,
                day = note.date,
                habitName = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                mark = mark,
                outcome = outcome,
                time = writtenAt(note, today, strings),
                text = note.text,
            )
        }

        private fun writtenAt(note: Note, today: LocalDate, strings: Strings): String {
            val at = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault())
            val time = at.toLocalTime().format(TIME)
            val day = at.toLocalDate()
            if (day.toEpochDay() == note.date) return time
            val dayLabel = when {
                !day.isBefore(today.minusDays(6)) -> day.format(datePattern("EEE"))
                day.year == today.year -> day.format(datePattern("d MMM"))
                else -> day.format(datePattern("d MMM yyyy"))
            }
            return strings(R.string.note_written_at, dayLabel, time)
        }

        /** The day's mark and its words. A past day with nothing logged counts as skipped. */
        fun dayOutcome(habit: Habit, entry: Entry?, day: Long, today: Long, strings: Strings): Pair<DayMark, String> = when (entry?.status) {
            EntryStatus.DONE -> DayMark(MarkKind.DONE, 1f) to
                if (habit.trackType == TrackType.CHECK) strings(R.string.outcome_done) else strings(R.string.outcome_done_amount, habit.measure(entry.amount))
            EntryStatus.PARTIAL -> DayMark(MarkKind.PARTIAL, (entry.amount / habit.target).toFloat().coerceIn(0f, 1f)) to
                strings(R.string.amount_of_goal, formatNumber(entry.amount), habit.measure(habit.target))
            EntryStatus.SKIP -> DayMark(MarkKind.SKIP) to strings(R.string.outcome_skipped)
            null -> if (day < today) DayMark(MarkKind.SKIP) to strings(R.string.outcome_skipped) else DayMark(MarkKind.OPEN_TODAY) to strings(R.string.not_logged_yet)
        }
    }
}

/** A note card's tile (design 2026-11-11): 36 dp, 12 dp corners, an 18 dp icon. */
val NoteTileSize = 36.dp
val NoteTileShape = RoundedCornerShape(12.dp)
val NoteTileIconSize = 18.dp
/** Inside a note card: 14 dp top and left, 16 dp right and bottom; 12 dp from the tile to the text. */
val NoteCardPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 16.dp, bottom = 16.dp)
val NoteCardTileGap = 12.dp
/** The day mark on a note card's top line, and the gap before it. */
val NoteMarkSize = 14.dp
val NoteMarkGap = 6.dp
/** From the top line to the note, and from the note to the time line. */
val NoteTextGap = 4.dp

/**
 * A note (design 2026-11-11, second version): the 36 dp habit tile at the top left; the habit name
 * and the day's mark on the top line (nothing else, so a long name gets the whole width); the note;
 * then the time line ("11:40 PM", "written Tue, 12:23 AM"). The outcome words aren't drawn, the
 * mark says it; TalkBack reads them. Without [showHabit] (a habit's own page) there is no tile, the
 * top line is the mark and the outcome, and the time line names the day ("Yesterday · 11:40 PM").
 * [shape] rounds it as part of a day's group.
 */
@Composable
fun NoteCard(
    note: NoteCardUi,
    onOpen: ((Long) -> Unit)?,
    showHabit: Boolean = true,
    dateLabel: String? = null,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    onLongPress: ((NoteCardUi) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(note.hue)
    val marks = MarkColors(skip = colors.outlineVariant, skipInk = colors.onSurfaceVariant, outline = colors.outline)
    val partial = stringResource(R.string.outcome_partial)
    // On a habit's own page the time line names the day: "Yesterday · 11:40 PM".
    val timeLine = if (showHabit) note.time else listOfNotNull(dateLabel, note.time).joinToString(" · ")
    // TalkBack reads the card as one: "Eat clean, Skipped, Biryani, written Tue, 12:23 AM".
    val description = listOfNotNull(
        note.habitName.takeIf { showHabit },
        partial.takeIf { note.mark.kind == MarkKind.PARTIAL },
        note.outcome,
        note.text,
        timeLine,
    ).joinToString(", ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLowest)
            .then(
                if (onOpen != null || onLongPress != null) {
                    Modifier.combinedClickable(
                        onClickLabel = stringResource(R.string.edit_note),
                        onLongClickLabel = stringResource(R.string.action_more_options),
                        onLongClick = onLongPress?.let { { it(note) } },
                        onClick = { onOpen?.invoke(note.id) },
                    )
                } else {
                    Modifier
                },
            )
            .padding(NoteCardPadding),
        horizontalArrangement = Arrangement.spacedBy(NoteCardTileGap),
    ) {
        if (showHabit) {
            HabitTile(note.icon, hc.soft, hc.ink, shape = NoteTileShape, size = NoteTileSize, iconSize = NoteTileIconSize)
        }
        Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(NoteTextGap)) {
            // The top line is only the name (or the outcome) and the mark, so the name gets the whole width.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHabit) {
                    Text(note.habitName, style = WeftType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    DayMarkView(note.mark, hc, marks, Modifier.padding(start = NoteMarkGap).size(NoteMarkSize))
                } else {
                    DayMarkView(note.mark, hc, marks, Modifier.size(NoteMarkSize))
                    Text(note.outcome, style = WeftType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = NoteMarkGap))
                }
            }
            Text(note.text, style = WeftType.body, color = colors.onSurface)
            Text(timeLine, style = WeftType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
