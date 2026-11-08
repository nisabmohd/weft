package app.sprout.habits.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.WeftType
import app.sprout.habits.ui.theme.ButtonLabelMinSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.sp

/** What a [WeftButton] does, which sets its colours (docs/DESIGN-RULES.md → 6). */
enum class ButtonRole {
    /** Confirms or goes ahead: primary / onPrimary. At most one per screen or sheet. */
    Primary,

    /** Cancels, closes or waits (Cancel, Later, Maybe later, Snooze): secondaryContainer. */
    Secondary,

    /** Goes ahead with something that can't be undone (Delete, Replace): error / onError. */
    Destructive,
}

/** Height of every button in a sheet or form (Get started's are the one exception, 56 dp). */
val ButtonHeight = 48.dp

/** Space between the two buttons of a [ButtonPair]. */
val ButtonPairGap = 8.dp

/** The app's 48 dp fully rounded button, filled in the colours of its [role]. Never outlined. */
@Composable
fun WeftButton(
    label: String,
    role: ButtonRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Shows a spinner instead of the label while the action runs (Restore, Back up now). */
    loading: Boolean = false,
    /** An optional 18 dp icon before the label. */
    icon: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (role) {
        ButtonRole.Primary -> colors.primary to colors.onPrimary
        ButtonRole.Secondary -> colors.secondaryContainer to colors.onSecondaryContainer
        ButtonRole.Destructive -> colors.error to colors.onError
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = ButtonHeight),
        shape = RoundedCornerShape(ButtonHeight / 2),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = content)
            else -> {
                if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                // A long label ("Snooze 10 min" at a large text size) shrinks to fit instead of being cut off.
                val style = WeftType.cardTitle
                BasicText(
                    label,
                    style = style.copy(color = LocalContentColor.current),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = ButtonLabelMinSize, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                )
            }
        }
    }
}

/**
 * The one button pair: two equal [WeftButton]s filling the width, 8 dp apart, the secondary one
 * (Cancel, Later, Snooze) on the left and the one that goes ahead on the right, in [primaryRole].
 */
@Composable
fun ButtonPair(
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryRole: ButtonRole = ButtonRole.Primary,
    primaryEnabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonPairGap)) {
        WeftButton(secondaryLabel, ButtonRole.Secondary, onSecondary, Modifier.weight(1f))
        WeftButton(primaryLabel, primaryRole, onPrimary, Modifier.weight(1f), enabled = primaryEnabled)
    }
}
