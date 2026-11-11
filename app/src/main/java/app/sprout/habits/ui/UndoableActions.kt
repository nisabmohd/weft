package app.sprout.habits.ui

import androidx.compose.runtime.staticCompositionLocalOf
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.components.PendingUndo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Changes that are asked about first (a `PromptSheet`) and then offer Undo in the snackbar
 * ("Read archived · Undo"), from whichever screen they're made. They run in the app's scope, so
 * they finish, and can be undone, after the screen that made them has closed. [notOn] names a
 * screen that is closing as it makes the change, so the message shows on the one underneath.
 */
class UndoableActions(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val strings: Strings,
    private val pending: PendingUndo,
    private val scope: CoroutineScope,
) {
    fun setArchived(habit: Habit, archived: Boolean, notOn: String? = null) = scope.launch {
        repository.setArchived(habit.id, archived)
        pending.offer(strings(if (archived) R.string.habit_archived else R.string.habit_restored, habit.name), notOn) {
            repository.setArchived(habit.id, !archived)
        }
    }

    fun deleteHabit(habit: Habit, notOn: String? = null) = scope.launch {
        val removed = repository.deleteHabits(listOf(habit))
        pending.offer(strings(R.string.habit_deleted, habit.name), notOn) { repository.restore(removed) }
    }

    fun deleteNote(note: Note, notOn: String? = null) = scope.launch {
        repository.deleteNote(note.id)
        pending.offer(strings(R.string.note_deleted), notOn) { repository.restoreNote(note) }
    }

    /** Deletes the sample habits with their days and notes; Undo brings them and the banner back. */
    fun removeSampleData() = scope.launch {
        val ids = settings.settings.first().sampleHabitIds
        val removed = repository.deleteHabits(ids.mapNotNull { repository.getHabit(it) })
        settings.setSampleHabitIds(emptySet())
        pending.offer(strings(R.string.sample_data_removed)) {
            repository.restore(removed)
            settings.setSampleHabitIds(ids)
        }
    }

    fun clearSuggestionHistory() = scope.launch {
        val before = settings.settings.first().suggestionsClearedAt
        settings.clearSuggestionHistory()
        pending.offer(strings(R.string.suggestion_history_cleared)) { settings.setSuggestionsClearedAt(before) }
    }
}

val LocalUndoableActions = staticCompositionLocalOf<UndoableActions> { error("No UndoableActions") }
