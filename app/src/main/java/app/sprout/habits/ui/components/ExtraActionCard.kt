package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.IconSizeSmall
import app.sprout.habits.ui.theme.RowPadding
import app.sprout.habits.ui.theme.ScreenPadding
import app.sprout.habits.ui.theme.Space20
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.theme.TileTextGap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * The card under a [ChoiceSheet]'s rows for something that leaves the sheet ("Help translate
 * Weft", "Choose from phone"): a 40 dp primaryContainer tile, title and line, and ↗. Use it as the
 * sheet's footer; it brings the sheet's bottom padding.
 */
@Composable
fun ExtraActionCard(icon: Int, title: String, text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = Space8, bottom = Space20)
            .navigationBarsPadding()
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TileTextGap),
    ) {
        Box(Modifier.size(ActionTileSize).clip(HabitTileShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(ActionIconSize))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TitleLineGap)) {
            Text(title, style = WeftType.cardTitle, color = colors.onSurface)
            Text(text, style = WeftType.supporting, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_open_external), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(IconSizeSmall))
    }
}
