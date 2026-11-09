package app.sprout.habits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.theme.WeftType

/** An action row: 60 dp tall (68 dp with a second line), with a 40 dp tile and a 20 dp icon. */
val ActionRowHeight = 60.dp
val ActionRowTwoLineHeight = 68.dp
val ActionTileSize = 40.dp
val ActionIconSize = 20.dp

/*
 * A list of actions (a note's Edit / Share / Delete, the Google account sheet, the support
 * prompt), as grouped rows: docs/DESIGN-RULES.md → 3.
 */

/** A grouped list of [ActionRow]s, 2 dp apart, inset by the sheet's side padding. */
@Composable
fun ActionGroup(modifier: Modifier = Modifier, rows: @Composable () -> Unit) {
    Column(
        // 20 dp under the title block: the header leaves 16, this adds 4.
        modifier.fillMaxWidth().padding(horizontal = SheetSidePadding).padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
    ) { rows() }
}

/**
 * One row of a grouped actions list: surfaceContainerHigh, 24/6 dp grouped corners, 60 dp tall, a 40 dp icon
 * tile and the label, with an optional second [line] and a chevron when it opens something.
 * [destructive] rows (always the last) use the error colours.
 */
@Composable
fun ActionRow(
    icon: Int,
    label: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    line: String? = null,
    destructive: Boolean = false,
    chevron: Boolean = false,
    tileColor: androidx.compose.ui.graphics.Color? = null,
    iconColor: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val content = if (destructive) colors.error else colors.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(groupedShape(index, count))
            .background(colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = if (line != null) ActionRowTwoLineHeight else ActionRowHeight)
            // 16 dp inside, like every card and grouped row.
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(ActionTileSize)
                .clip(HabitTileShape)
                // primaryContainer, like "Help translate Weft": secondaryContainer blended into the row (2026-11-09).
                .background(tileColor ?: if (destructive) colors.errorContainer else colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = iconColor ?: if (destructive) colors.error else colors.onPrimaryContainer,
                modifier = Modifier.size(ActionIconSize),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = WeftType.cardTitle, color = content)
            if (line != null) Text(line, style = WeftType.supporting, color = colors.onSurfaceVariant)
        }
        if (chevron) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

