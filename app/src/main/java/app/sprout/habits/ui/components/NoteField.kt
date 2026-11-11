package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.CardPadding
import app.sprout.habits.ui.theme.Space12
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.WeftType

/** Corners of a filled note field: the same 24 dp as a card. */
val NoteFieldCorner = 24.dp
/** At least two lines of `body` with its padding (was 76 dp). */
val NoteFieldMinHeight = 80.dp

/**
 * The note field of a sheet or form (docs/DESIGN-RULES.md → 5): filled surfaceContainerHigh, 24 dp corners, no outline, with a
 * question for the habit and day as the [placeholder].
 */
@Composable
fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = NoteFieldMinHeight,
) {
    val colors = MaterialTheme.colorScheme
    val style = WeftType.body.copy(color = colors.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(NoteFieldCorner))
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = CardPadding, vertical = Space12), // docs/DESIGN-RULES.md → 5
            ) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = colors.onSurfaceVariant))
                inner()
            }
        },
    )
}
