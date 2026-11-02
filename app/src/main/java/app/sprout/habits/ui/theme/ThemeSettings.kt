package app.sprout.habits.ui.theme

import androidx.compose.runtime.Immutable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BodyFont { SYSTEM, SPACE_GROTESK, OUTFIT, LEXEND, ATKINSON }

/** Everything the theme needs from user settings. */
@Immutable
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    /** Seed hue for the app chrome when dynamic color is off or unavailable. */
    val accentHue: Float = DEFAULT_ACCENT_HUE,
    val font: BodyFont = BodyFont.LEXEND,
    /** Multiplies every type-scale size, on top of the system font scale. */
    val textScale: Float = 1f,
)

/** Amber, the last swatch (user, 2026-11-02). Installs keep the hue they saved. */
const val DEFAULT_ACCENT_HUE = 38f

/**
 * The logo's blue (the Blue accent). Used where the user's accent can't apply: the widgets below
 * Android 12, the launch screen and the launcher shortcuts (`res/values/colors.xml`) (user, 2026-11-02).
 */
const val LOGO_HUE = 215f
