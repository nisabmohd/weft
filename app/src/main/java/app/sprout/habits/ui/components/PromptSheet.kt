package app.sprout.habits.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R

/**
 * A question that needs an answer before going ahead (Delete this habit? Replace your data?), as a
 * sheet: the title, an optional [text], then Cancel | [action]. The app has no dialogs.
 * [actionRole] is [ButtonRole.Destructive] when the action can't be undone.
 */
@Composable
fun PromptSheet(
    title: String,
    action: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    text: String? = null,
    actionRole: ButtonRole = ButtonRole.Primary,
) {
    WeftSheet(onDismissRequest = onDismiss) {
        SheetHeader(title, text)
        ButtonPair(
            secondaryLabel = stringResource(R.string.action_cancel),
            onSecondary = onDismiss,
            primaryLabel = action,
            onPrimary = onAction,
            primaryRole = actionRole,
            modifier = Modifier.padding(horizontal = SheetSidePadding).padding(top = 8.dp).sheetBottomPadding(),
        )
    }
}
