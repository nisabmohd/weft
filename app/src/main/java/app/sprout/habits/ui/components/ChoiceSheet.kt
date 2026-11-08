package app.sprout.habits.ui.components

import androidx.compose.ui.text.font.FontFamily
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A bottom sheet for picking one option (Language, Week starts on): the title, then full-width
 * [ChoiceRow]s that scroll, then an optional [footer] that stays below them.
 */
@Composable
fun ChoiceSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    footer: (@Composable () -> Unit)? = null,
    rows: @Composable ColumnScope.() -> Unit,
) {
    WeftSheet(onDismissRequest = onDismiss) {
        SheetHeader(title, subtitle)
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).selectableGroup(), content = rows)
        if (footer != null) footer() else Spacer(Modifier.sheetBottomPadding())
    }
}

/**
 * One option: title with an optional line under it, the radio on the right; the chosen one is tinted.
 * [fontFamily] draws the text in that font (the font sheet shows each font in itself).
 */
@Composable
fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    fontFamily: FontFamily? = null,
    /** A 16 dp icon before the line, and its colour (the alarm sound's "Playing"). */
    subtitleIcon: Int? = null,
    subtitleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (selected) colors.surfaceContainerHigh else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            // Edge to edge, the text at the sheet's title inset (docs/DESIGN-RULES.md → 4).
            .padding(horizontal = SheetTitleInset, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = WeftType.cardTitle.copy(fontFamily = fontFamily ?: WeftType.cardTitle.fontFamily), color = colors.onSurface)
            subtitle?.let {
                val color = subtitleColor.takeOrElse { colors.onSurfaceVariant }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subtitleIcon?.let { icon -> Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(16.dp)) }
                    Text(it, style = WeftType.supporting.copy(fontFamily = fontFamily ?: WeftType.supporting.fontFamily), color = color)
                }
            }
        }
        trailing?.invoke()
        RadioButton(selected = selected, onClick = null)
    }
}
