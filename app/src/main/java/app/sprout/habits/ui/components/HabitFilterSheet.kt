package app.sprout.habits.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors
@Immutable
data class HabitFilterOption(val habitId: Long, val name: String, val icon: Int, val hue: Float)

/** A habit chip in Filter by habit (filter-by-habit-sheet.png): 40 dp tall, 12 dp corners. */
val HabitChipHeight = 40.dp
val HabitChipShape = RoundedCornerShape(12.dp)
/** The small habit tile inside a habit chip: 28 dp, 9 dp corners (about 0.3 × size), 16 dp icon. */
val HabitChipTileSize = 28.dp
val HabitChipTileShape = RoundedCornerShape(9.dp)
val HabitChipIconSize = 16.dp

/**
 * "Filter by habit", shared by Journal and Insights (filter-by-habit-sheet.png): every habit as a
 * chip (tile + name, ✓ when picked), wrapping, then [helper] and one button. Clear unticks all;
 * an empty selection means every habit. The sheet is only as tall as its chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitFilterSheet(
    options: List<HabitFilterOption>,
    selected: Set<Long>,
    helper: String,
    applyLabel: (count: Int) -> String,
    onApply: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var picked by remember { mutableStateOf(selected) }
    val haptics = LocalHapticFeedback.current
    WeftSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(start = SheetTitleInset, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.filter_by_habit), style = WeftType.title, color = colors.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { picked = emptySet() }, enabled = picked.isNotEmpty()) { Text(stringResource(R.string.clear), style = WeftType.label) }
        }
        FlowRow(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = SheetSidePadding).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { o ->
                val checked = o.habitId in picked
                HabitChip(o, checked) { on -> haptics.toggle(on); picked = if (on) picked + o.habitId else picked - o.habitId }
            }
        }
        Text(
            helper,
            style = WeftType.supporting,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = SheetTitleInset, end = SheetTitleInset, top = 16.dp),
        )
        WeftButton(
            applyLabel(picked.size),
            ButtonRole.Primary,
            onClick = { onApply(picked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = SheetSidePadding).padding(top = SheetActionGap).sheetBottomPadding(),
        )
    }
}

/** One habit chip: picked = secondaryContainer with a ✓ after the name; not picked = a 1 dp outline. */
@Composable
private fun HabitChip(option: HabitFilterOption, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(option.hue)
    Row(
        Modifier
            .heightIn(min = HabitChipHeight)
            .clip(HabitChipShape)
            .then(if (checked) Modifier.background(colors.secondaryContainer) else Modifier.border(1.dp, colors.outlineVariant, HabitChipShape))
            .toggleable(checked, role = Role.Checkbox, onValueChange = onToggle)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitTile(hc.soft, Modifier.size(HabitChipTileSize), HabitChipTileShape) {
            Icon(painterResource(option.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(HabitChipIconSize))
        }
        Text(
            option.name,
            style = WeftType.label,
            color = if (checked) colors.onSecondaryContainer else colors.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (checked) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

/**
 * A 48 dp round header button (back, close, save, edit, delete, calendar, filter), filled in when
 * its filter is [active], which also puts a small dot in its corner. [primary] is the screen's main action (Save): filled with the primary color.
 * Every screen header uses it so they all look the same.
 */
@Composable
fun HeaderIconButton(
    icon: Int,
    label: String,
    active: Boolean = false,
    danger: Boolean = false,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = when {
                primary -> colors.primary
                active -> colors.primaryContainer
                else -> colors.surfaceContainerHigh
            },
            contentColor = when {
                primary -> colors.onPrimary
                active -> colors.onPrimaryContainer
                danger -> colors.error
                else -> colors.onSurface
            },
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
    // A plain 8 dp dot, 8 dp in from the top right, with no ring. No number: the chip under the
    // header says what is applied.
    if (active) {
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(colors.primary, CircleShape))
    }
    }
}

/**
 * The chips under a tab header: the range ([DefaultRangeChip] or a picked [CustomRangeChip]),
 * then the habit chip. The row scrolls sideways when it doesn't fit.
 */
@Composable
fun FilterChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * The default range ("This week") shown as the selected chip: filled, with a check. Tapping it
 * opens the date range sheet to pick another range.
 */
@Composable
fun DefaultRangeChip(label: String, openLabel: String, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.secondaryContainer)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        Text(label, style = WeftType.label, color = colors.onSecondaryContainer, maxLines = 1)
    }
}

/**
 * A range picked in the date sheet ("21 – 24 Sep"): filled secondaryContainer, with a ✕
 * that goes back to the default. Tapping the chip opens the date range sheet again.
 */
@Composable
fun CustomRangeChip(label: String, openLabel: String, clearLabel: String, onOpen: () -> Unit, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.secondaryContainer)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = WeftType.label, color = colors.onSecondaryContainer, maxLines = 1, modifier = Modifier.padding(end = 6.dp))
        SmallClearButton(clearLabel, 24.dp, Color.Transparent, colors.onSecondaryContainer, onClear)
    }
}

/**
 * The habit filter that is on: overlapping dots in the habits' colors, [label] ("3 habits", or
 * their names) and a ✕ that clears it. Tapping the chip opens the filter sheet again.
 */
@Composable
fun HabitFilterChip(selected: List<HabitFilterOption>, label: String, removeLabel: String, onOpen: () -> Unit, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.secondaryContainer)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
            selected.take(5).forEach { option ->
                Box(Modifier.size(18.dp).background(colors.secondaryContainer, CircleShape).padding(2.dp).background(habitColors(option.hue).solid, CircleShape))
            }
        }
        Text(
            label,
            style = WeftType.label,
            color = colors.onSecondaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        SmallClearButton(removeLabel, 24.dp, Color.Transparent, colors.onSecondaryContainer, onClear)
    }
}
