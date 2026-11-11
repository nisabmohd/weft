package app.sprout.habits.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.WeftType

/** An info row (design 2026-11-11): 64 dp at least, primaryContainer, 24 dp corners. */
val InfoRowMinHeight = 64.dp
val InfoRowShape = RoundedCornerShape(24.dp)
/** 10 dp top and bottom, 16 dp left, 20 dp right (12 dp when it ends in a button); 14 dp between the parts. */
val InfoRowPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 10.dp)
val InfoRowPaddingWithButton = PaddingValues(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
val InfoRowGap = 14.dp
/** The row's small button: 36 dp tall, fully rounded, 14 dp at the sides. */
val InfoRowButtonHeight = 36.dp
val InfoRowButtonSidePadding = 14.dp

/**
 * A tinted info row, the same shape as the rows around it: a white tile with an icon, a title, one
 * muted line, and an optional small button on the right (About's Free and open source, Today's
 * Sample habits). Everything is in onPrimaryContainer, the line at 80%; the app's own `cardTitle`
 * and `supporting` (the design's 15 / 13 sp looked too small, user 2026-11-11).
 */
@Composable
fun InfoRow(
    @DrawableRes icon: Int,
    title: String,
    line: String,
    modifier: Modifier = Modifier,
    button: String? = null,
    onButton: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.onPrimaryContainer
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = InfoRowMinHeight)
            .background(colors.primaryContainer, InfoRowShape)
            .padding(if (button != null) InfoRowPaddingWithButton else InfoRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InfoRowGap),
    ) {
        Box(Modifier.size(ActionTileSize).background(colors.surfaceContainerLowest, HabitTileShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(ActionIconSize))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = WeftType.cardTitle, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = WeftType.supporting, color = ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (button != null) {
            Box(
                Modifier
                    .height(InfoRowButtonHeight)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerLowest)
                    .clickable(role = Role.Button, onClick = onButton)
                    .padding(horizontal = InfoRowButtonSidePadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(button, style = WeftType.label, color = ink, maxLines = 1)
            }
        }
    }
}
