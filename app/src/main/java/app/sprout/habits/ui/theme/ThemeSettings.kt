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

/**
 * Rust, the default accent (user, 2026-11-09; Amber 38 was the default from 2026-11-02 until its
 * swatch was dropped as the second brown). Installs keep the hue they saved.
 */
const val DEFAULT_ACCENT_HUE = 12f

/** Amber, a swatch and the default until 1.6.2: read as [DEFAULT_ACCENT_HUE] (Rust) now. */
const val OLD_AMBER_HUE = 38f

/**
 * The logo's hue: its green tile #2A6F52 (weft-icon.zip, 2026-11-08; navy, hue 215, before). Used
 * where the user's accent can't apply: the widgets below Android 12, the launch screen and the
 * launcher shortcuts (`res/values/colors.xml`).
 */
const val LOGO_HUE = 155f
