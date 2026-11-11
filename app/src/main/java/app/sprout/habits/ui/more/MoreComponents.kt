package app.sprout.habits.ui.more

import app.sprout.habits.ui.theme.CardGap
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.IconSize
import app.sprout.habits.ui.theme.ItemGap
import app.sprout.habits.ui.theme.RowMinHeight
import app.sprout.habits.ui.theme.RowPadding
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.TileTextGap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.Color
import app.sprout.habits.ui.components.GroupGap
import app.sprout.habits.ui.components.TitleLineGap
import app.sprout.habits.ui.components.SectionGapBelow
import app.sprout.habits.ui.components.SectionGapAbove
import app.sprout.habits.ui.components.SectionHeader
import app.sprout.habits.ui.components.toggle
import androidx.compose.ui.platform.LocalHapticFeedback
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import app.sprout.habits.ui.components.groupedShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A section header in a list with 8 dp gaps, put in the same item as its group: the usual 20 dp
 * above it (none for the [first]) and 8 dp below it.
 */
@Composable
fun SectionLabel(text: String, first: Boolean = false) {
    SectionHeader(text, Modifier.padding(top = if (first) 0.dp else SectionGapAbove - CardGap, bottom = SectionGapBelow))
}

/** One white card with 24 dp corners, for a row that stands alone. */
@Composable
fun SettingsCard(shape: Shape = CardShape, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content,
    )
}

/**
 * Rows grouped like the Journal's notes: each row its own card, 2 dp apart, 24 dp corners on the
 * outside of the group and 6 dp between rows. No dividers. Null rows are left out.
 */
@Composable
fun SettingsGroup(vararg rows: (@Composable ColumnScope.() -> Unit)?) {
    val shown = rows.filterNotNull()
    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        shown.forEachIndexed { i, row -> SettingsCard(groupedShape(i, shown.size), row) }
    }
}

/** Title, optional subtitle and a trailing slot; the whole row is the touch target. */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: Int? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    /** The line under the title; error for "2 things to allow" (More → Alarms). */
    subtitleColor: Color = Color.Unspecified,
    /** Shown after the subtitle, e.g. the "Update available" pill. */
    badge: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSize).padding(end = 0.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(TileTextGap))
        }
        Column(Modifier.weight(1f).padding(end = Space12), verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
            Text(title, style = WeftType.cardTitle, color = titleColor.takeOrElse { MaterialTheme.colorScheme.onSurface })
            if (badge == null) {
                subtitle?.let { Text(it, style = WeftType.supporting, color = subtitleColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
                    subtitle?.let { Text(it, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false)) }
                    badge()
                }
            }
        }
        trailing()
    }
}

/** A row that toggles as a whole and reads as a switch to TalkBack. */
@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, icon: Int? = null, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(value = checked, role = Role.Switch) { haptics.toggle(it); onChange(it) }
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Same leading icon as SettingsRow, so a switch lines up with the rows around it.
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSize))
            androidx.compose.foundation.layout.Spacer(Modifier.size(TileTextGap))
        }
        Column(Modifier.weight(1f).padding(end = Space12), verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
            Text(title, style = WeftType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun TrailingValue(text: String) =
    Text(text, style = WeftType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
