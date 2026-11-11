package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.Space16
import app.sprout.habits.ui.theme.Space4
import app.sprout.habits.ui.theme.Space8
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/** The share of the screen a sheet may take; the rest stays visible above it. */
private const val MAX_SHEET_FRACTION = 0.9f

/** The sheet's drag handle area, which sits above the content. */
private val DRAG_HANDLE = 48.dp

/**
 * Every bottom sheet in the app: opens fully, sits on the bottom edge, and leaves the top 10% of
 * the screen visible however much it holds (just the status bar while the keyboard is up). Long
 * content scrolls inside it.
 *
 * The limit is on the content, not the sheet: a height limit on the sheet itself lifts it off
 * the bottom edge. It uses the room the sheet really has, because before Android 9 the sheet's
 * window shrinks to fit above the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeftSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    val screen = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val navBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val statusBar = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    // While typing (e.g. searching icons) the keyboard takes half the screen, so the sheet may
    // grow to just under the status bar; otherwise it leaves the top 10% free.
    val topGap = if (WindowInsets.isImeVisible) statusBar + 8.dp else screen * (1 - MAX_SHEET_FRACTION)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        BoxWithConstraints {
            val byScreen = screen - topGap - DRAG_HANDLE - navBar
            val max = if (constraints.hasBoundedHeight) minOf(byScreen, maxHeight - topGap - DRAG_HANDLE) else byScreen
            // The sheet takes focus itself, so opening one never pops up the keyboard: before
            // Android 9 the first text field in it would get focus on its own.
            Column(Modifier.heightIn(max = max).nestedScroll(remember { ScrollStaysInside() }).focusable(), content = content)
        }
    }
}

/**
 * The sheet rule (CLAUDE.md → Sheets): a scroll or fling that moved the sheet's content stops when
 * the content reaches its top. What is left of it is used up here, so it never reaches the sheet
 * and drags or closes it. A new drag that starts with the content already at its top still moves
 * the sheet, so it can be pulled down on purpose.
 */
private class ScrollStaysInside : NestedScrollConnection {
    /** True once the current gesture (or the fling after it) has scrolled the content. */
    private var scrolledContent = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (consumed.y != 0f) scrolledContent = true
        return when {
            // The fling after a drag never drags the sheet, whether or not it moved the content.
            source == NestedScrollSource.SideEffect -> available
            scrolledContent -> available
            else -> Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val keep = scrolledContent || consumed.y != 0f
        scrolledContent = false
        return if (keep) available else Velocity.Zero
    }
}

/*
 * Sheet spacing (docs/SPACING.md → Sheets). Every sheet uses these, never its own numbers.
 */

/** Space between a sheet's content (rows, fields, buttons) and its left and right edges. */
val SheetSidePadding = 20.dp

/** A sheet's title row sits a little further in than its content. */
val SheetTitleInset = 24.dp

/** Space between a sheet's list and the one full-width button under it (Set up alarms). */
val SheetActionGap = 20.dp

/** Space under a sheet's last element, on top of the navigation bar's own inset. */
val SheetBottomGap = 16.dp

/** The bottom of every sheet: [SheetBottomGap] above the gesture bar or the 3-button bar. */
fun Modifier.sheetBottomPadding(): Modifier = this.padding(bottom = SheetBottomGap).navigationBarsPadding()

/**
 * Every sheet's header: the left-aligned title (`title`, 22 sp) and an optional muted line under
 * it (`supporting`), [SheetTitleInset] from the sides, 16 dp above the content.
 */
@Composable
fun SheetHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(horizontal = SheetTitleInset).padding(top = Space8, bottom = Space16)) {
        Text(title, style = WeftType.title, color = colors.onSurface)
        if (subtitle != null) {
            Text(
                subtitle,
                style = WeftType.supporting,
                color = colors.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Space4),
            )
        }
    }
}
