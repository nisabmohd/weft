package app.sprout.habits.ui.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.ui.components.ButtonRole
import app.sprout.habits.ui.components.PromptSheet

/** "Delete <habit>?" as a sheet: Cancel | Delete. */
@Composable
fun DeleteHabitSheet(name: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    PromptSheet(
        title = stringResource(R.string.delete_habit_title, name),
        text = stringResource(R.string.delete_habit_text),
        action = stringResource(R.string.action_delete),
        onAction = onDelete,
        onDismiss = onDismiss,
        actionRole = ButtonRole.Destructive,
    )
}
