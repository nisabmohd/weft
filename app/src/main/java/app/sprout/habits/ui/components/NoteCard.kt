package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.CardContentGap
import app.sprout.habits.ui.theme.CardPadding
import app.sprout.habits.ui.theme.IconSizeTiny
import app.sprout.habits.ui.theme.TileTextGap
import app.sprout.habits.ui.theme.Space4
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.IconLabelGap
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
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

/**
 * A note card (design 2026-11-11b, "habit row on top"): the app's habit row (the 44 dp tile, 12 dp,
 * the name and 2 dp under it the 16 dp mark + "outcome · time"), then the note 12 dp under the row
 * across the full card width. 16 dp inside like every card.
 */
val NoteCardPadding = PaddingValues(CardPadding)
val NoteCardTileGap = TileTextGap
val NoteMarkSize = IconSizeTiny
val NoteMarkGap = IconLabelGap
/** The habit row to the note: a card's header to its content. */
val NoteTextGap = CardContentGap

/**
 * A note: the habit row (tile, name, mark + "8 of 20 pages · 11:40 PM" or "Done · written Sat,
 * 9:45 PM"), then the note under it. Without [showHabit] (a habit's own page) there is no tile and
 * no name: the row is the mark + "8 of 20 pages · Yesterday · 11:40 PM". TalkBack reads the card as
 * one item: "Read, 8 of 20 pages, 11:40 PM. Only 8 pages, fell asleep." [shape] rounds it as part of
 * a day's group.
 */
@Composable
fun NoteCard(
    note: NoteCardUi,
    onOpen: ((Long) -> Unit)?,
    showHabit: Boolean = true,
    dateLabel: String? = null,
    shape: RoundedCornerShape = CardShape,
    onLongPress: ((NoteCardUi) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(note.hue)
    val marks = MarkColors(skip = colors.outlineVariant, skipInk = colors.onSurfaceVariant, outline = colors.outline)
    val line = listOfNotNull(note.outcome, dateLabel.takeIf { !showHabit }, note.time).joinToString(" · ")
    val description = listOfNotNull(note.habitName.takeIf { showHabit }, note.outcome, dateLabel.takeIf { !showHabit }, note.time)
        .joinToString(", ") + ". " + note.text
    Column(
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
            .padding(NoteCardPadding)
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(NoteTextGap),
    ) {
        // The habit row, as on Today and Habits.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showHabit) {
                HabitTile(note.icon, hc.soft, hc.ink)
                Spacer(Modifier.width(NoteCardTileGap))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
                if (showHabit) {
                    Text(note.habitName, style = WeftType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DayMarkView(note.mark, hc, marks, Modifier.size(NoteMarkSize))
                    Text(line, style = WeftType.supporting, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = NoteMarkGap))
                }
            }
        }
        Text(note.text, style = WeftType.body, color = colors.onSurface)
    }
}
