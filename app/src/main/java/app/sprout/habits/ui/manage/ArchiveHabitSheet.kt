package app.sprout.habits.ui.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.ui.components.PromptSheet

/** "Archive <habit>?" as a sheet: Cancel | Archive. Restoring needs no question. */
@Composable
fun ArchiveHabitSheet(name: String, onArchive: () -> Unit, onDismiss: () -> Unit) {
    PromptSheet(
        title = stringResource(R.string.archive_habit_title, name),
        text = stringResource(R.string.archive_habit_text),
        action = stringResource(R.string.archive),
        onAction = onArchive,
        onDismiss = onDismiss,
    )
}
