package app.sprout.habits.support

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.components.sheetBottomPadding
import app.sprout.habits.ui.components.ActionGroup
import app.sprout.habits.ui.components.ActionRow
import app.sprout.habits.ui.components.ButtonRole
import app.sprout.habits.ui.components.WeftButton
import app.sprout.habits.ui.components.SheetHeader
import app.sprout.habits.ui.components.SheetSidePadding
import app.sprout.habits.ui.components.WeftSheet
import app.sprout.habits.ui.theme.habitColors

/**
 * "Enjoying Weft?" (`support-prompt-sheet.png`): rate (play build, once listed), star on GitHub,
 * sponsor (not in the play build: Google Play doesn't allow asking for money outside its own
 * billing) as grouped action rows, then "Maybe later".
 */
@Composable
fun SupportPromptSheet(
    showRate: Boolean,
    showSponsor: Boolean,
    onRate: () -> Unit,
    onStar: () -> Unit,
    onSponsor: () -> Unit,
    onLater: () -> Unit,
) {
    val pink = habitColors(330f)
    WeftSheet(onDismissRequest = onLater) {
        SheetHeader(
            stringResource(R.string.support_title),
            stringResource(
                when {
                    showRate -> R.string.support_text_play
                    showSponsor -> R.string.support_text
                    else -> R.string.support_text_star
                },
            ),
        )
        val count = 1 + (if (showRate) 1 else 0) + (if (showSponsor) 1 else 0)
        var i = 0
        ActionGroup {
            if (showRate) ActionRow(R.drawable.ic_thumb_up, stringResource(R.string.rate_on_play), i++, count, onRate, line = stringResource(R.string.support_rate_line), chevron = true)
            ActionRow(R.drawable.ic_star, stringResource(R.string.star_github), i++, count, onStar, line = stringResource(R.string.support_star_line), chevron = true)
            if (showSponsor) {
                ActionRow(
                    R.drawable.ic_heart, stringResource(R.string.sponsor), i++, count, onSponsor,
                    line = stringResource(R.string.support_sponsor_line), chevron = true,
                    tileColor = pink.soft, iconColor = pink.ink,
                )
            }
        }
        WeftButton(
            stringResource(R.string.maybe_later),
            ButtonRole.Secondary,
            onClick = onLater,
            modifier = Modifier.fillMaxWidth().padding(horizontal = SheetSidePadding).padding(top = 20.dp).sheetBottomPadding(),
        )
    }
}
