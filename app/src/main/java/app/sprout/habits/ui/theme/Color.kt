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
 * App chrome when dynamic color is off (see `pal()` in design/TOKENS.md).
 * Surfaces, background and outlines are the original Weft ones, so screens look as before 1.5.1
 * (cards on surfaceContainerLowest, tiles on surfaceContainerHigh). Primary, containers, error and
 * scrim are the richer 1.5.1 Material tones (user, 2026-11-02).
 */
fun seedColorScheme(hue: Float, dark: Boolean): ColorScheme = if (dark) {
    val bg = tone(hue, 14, 7)
    val text = tone(hue, 10, 93)
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
        surfaceVariant = tone(hue, 12, 18),
        onSurfaceVariant = tone(hue, 8, 68),
        surfaceContainerLowest = tone(hue, 14, 5),
        surfaceContainerLow = tone(hue, 12, 12),
        surfaceContainer = tone(hue, 14, 15),
        surfaceContainerHigh = tone(hue, 12, 18),
        surfaceContainerHighest = tone(hue, 12, 22),
        outline = tone(hue, 8, 45),
        outlineVariant = tone(hue, 8, 28),
        error = tone(356f, 90, 78f),
        errorContainer = tone(356f, 42, 27f),
        onErrorContainer = tone(356f, 90, 92f),
        scrim = Color(0x99000000),
    )
} else {
    val bg = tone(hue, 30, 96)
    val text = tone(hue, 15, 11)
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
        surfaceVariant = tone(hue, 28, 91),
        onSurfaceVariant = tone(hue, 8, 36),
        surfaceContainerLowest = tone(hue, 40, 99),
        surfaceContainerLow = tone(hue, 40, 99),
        surfaceContainer = tone(hue, 32, 93),
        surfaceContainerHigh = tone(hue, 28, 91),
        surfaceContainerHighest = tone(hue, 24, 88),
        outline = tone(hue, 8, 60),
        outlineVariant = tone(hue, 14, 84),
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
