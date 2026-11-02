package app.sprout.habits.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** `hsl(hue s% l%)`, the notation used in design/TOKENS.md. */
private fun tone(hue: Float, saturation: Int, lightness: Number): Color =
    Color.hsl(hue, saturation / 100f, lightness.toFloat() / 100f)

/**
 * App chrome when dynamic color is off: the same palette as Burrow (user, 2026-11-02).
 * Roles: background the page (and sheets / dialogs, surfaceContainerLowest); surfaceContainerLow cards and
 * rows; surfaceContainer icon buttons, tiles, tracks, the nav bar; surfaceContainerHigh pressed / today's
 * column; primaryContainer hero and welcome panel; secondaryContainer selected chips and pills.
 */
fun seedColorScheme(hue: Float, dark: Boolean): ColorScheme = if (dark) {
    val bg = tone(hue, 12, 6.5f)
    val text = tone(hue, 15, 91f)
    darkColorScheme(
        primary = tone(hue, 86, 82f),
        onPrimary = tone(hue, 60, 17f),
        primaryContainer = tone(hue, 40, 27f),
        onPrimaryContainer = tone(hue, 90, 92f),
        secondary = tone(hue, 86, 82f),
        onSecondary = tone(hue, 60, 17f),
        secondaryContainer = tone(hue, 14, 25f),
        onSecondaryContainer = tone(hue, 40, 90f),
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = tone(hue, 9, 15f),
        onSurfaceVariant = tone(hue, 9, 71f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = tone(hue, 10, 11f),
        surfaceContainer = tone(hue, 9, 15f),
        surfaceContainerHigh = tone(hue, 8, 20f),
        surfaceContainerHighest = tone(hue, 8, 24f),
        outline = tone(hue, 6, 34f),
        outlineVariant = tone(hue, 8, 20f),
        error = tone(356f, 90, 78f),
        errorContainer = tone(356f, 42, 27f),
        onErrorContainer = tone(356f, 90, 92f),
        scrim = Color(0x99000000),
    )
} else {
    val bg = tone(hue, 45, 98.4f)
    val text = tone(hue, 14, 11f)
    lightColorScheme(
        primary = tone(hue, 55, 40f),
        onPrimary = Color.White,
        primaryContainer = tone(hue, 88, 91f),
        onPrimaryContainer = tone(hue, 62, 19f),
        secondary = tone(hue, 55, 40f),
        onSecondary = Color.White,
        secondaryContainer = tone(hue, 34, 87f),
        onSecondaryContainer = tone(hue, 30, 17f),
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = tone(hue, 24, 91.5f),
        onSurfaceVariant = tone(hue, 7, 35f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = tone(hue, 32, 95f),
        surfaceContainer = tone(hue, 24, 91.5f),
        surfaceContainerHigh = tone(hue, 18, 88f),
        surfaceContainerHighest = tone(hue, 16, 84f),
        outline = tone(hue, 9, 72f),
        outlineVariant = tone(hue, 18, 88f),
        error = tone(356f, 64, 44f),
        errorContainer = tone(356f, 86, 92f),
        onErrorContainer = tone(356f, 60, 22f),
        scrim = Color(0x6B120C0E),
    )
}

/** Fixed per-habit colors derived from the habit's hue (see `hab()` in design/TOKENS.md). */
@Immutable
data class HabitColors(
    val solid: Color,
    val soft: Color,
    val mid: Color,
    val on: Color,
    val ink: Color,
)

fun habitColors(hue: Float, dark: Boolean): HabitColors = if (dark) {
    HabitColors(
        solid = tone(hue, 50, 66),
        soft = tone(hue, 22, 19),
        mid = tone(hue, 30, 30),
        on = tone(hue, 40, 12),
        ink = tone(hue, 60, 84),
    )
} else {
    HabitColors(
        solid = tone(hue, 48, 40),
        soft = tone(hue, 55, 92),
        mid = tone(hue, 48, 82),
        on = Color.White,
        ink = tone(hue, 50, 24),
    )
}
