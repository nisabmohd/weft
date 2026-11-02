# Design tokens (extracted from `Habit Tracker UI.html`)

The designs are in `Habit Tracker UI.html` in this folder: open it in a browser to see every screen as a 390 dp wide artboard.

## Colors
`tone(h, s, l)` = `hsl(h s% l%)`. Default accent hue = **345** (Rose) since 1.5.1; it was 38 (Amber) from 1.2 and 150 (green) before. Installs keep the hue they saved.

Accents (More → Appearance, shown when Dynamic color is off): Rose 345, Rust 12, Amber 38, Green 150, Teal 192, Blue 215, Purple 275. Blue is the logo's colour; the design and store captures use it.

App palette `seedColorScheme(h, dark)` in `ui/theme/Color.kt` (1.5.1, Burrow's palette and roles). With Dynamic color on (Android 12+), the system's wallpaper scheme replaces all of it; habit colours never change.

| Role | Used for | Light | Dark |
| --- | --- | --- | --- |
| background / surface / surfaceContainerLowest | the page, sheets, dialogs | tone(h,45,98.4) | tone(h,12,6.5) |
| surfaceContainerLow | cards and rows | tone(h,32,95) | tone(h,10,11) |
| surfaceContainer / surfaceVariant | icon buttons, tiles, tracks, the nav bar | tone(h,24,91.5) | tone(h,9,15) |
| surfaceContainerHigh / outlineVariant | pressed, today's column | tone(h,18,88) | tone(h,8,20) |
| surfaceContainerHighest | | tone(h,16,84) | tone(h,8,24) |
| primary / secondary | | tone(h,55,40) | tone(h,86,82) |
| onPrimary / onSecondary | | #ffffff | tone(h,60,17) |
| primaryContainer | hero, Welcome panel | tone(h,88,91) | tone(h,40,27) |
| onPrimaryContainer | | tone(h,62,19) | tone(h,90,92) |
| secondaryContainer | selected chips and pills | tone(h,34,87) | tone(h,14,25) |
| onSecondaryContainer | | tone(h,30,17) | tone(h,40,90) |
| onBackground / onSurface | text | tone(h,14,11) | tone(h,15,91) |
| onSurfaceVariant | muted text | tone(h,7,35) | tone(h,9,71) |
| outline | | tone(h,9,72) | tone(h,6,34) |
| error / errorContainer / onErrorContainer | | tone(356,64,44) / tone(356,86,92) / tone(356,60,22) | tone(356,90,78) / tone(356,42,27) / tone(356,90,92) |

Colours that can't follow the accent (`res/values*/colors.xml`):
- Launch screen (`weft_bg`) and the widgets' loading screen (`widget_bg`) follow the system. On Android 12+ they are `system_neutral1_10` / `system_neutral1_900` and Glance's `widgetBackground` (`system_accent2_50` / `system_accent2_800`). Below 12 there are no system colours, so they use the logo's blue (hue 215): page tone(215,45,98.4) / tone(215,12,6.5), widget tone(215,34,87) / tone(215,14,25).
- Widgets below Android 12 use `seedColorScheme(LOGO_HUE = 215)`. On 12+ they use the wallpaper colours.
- Launcher shortcuts use the logo's blue: primaryContainer tone(215,88,91) with onPrimaryContainer tone(215,62,19).
- The launcher icon background is the logo's navy `#2E4A7A` (`weft_primary`).

Habit palette `hab(hue, dark)`:

| Token | Light | Dark |
| --- | --- | --- |
| solid | tone(h,48,40) | tone(h,50,66) |
| soft | tone(h,55,92) | tone(h,22,19) |
| mid | tone(h,48,82) | tone(h,30,30) |
| on | #ffffff | tone(h,40,12) |
| ink | tone(h,50,24) | tone(h,60,84) |

Habit card: bg = skip ? hi : card; progress fill = done ? mid : soft; icon tile = done ? solid : partial ? mid : skip ? card : soft.

Sample habit hues: sun 38, book 215, drop 192, calm 275, dumbbell 12, pill 95, leaf 150, pen 330.

## Habit icons (24x24, stroke 1.9, round caps/joins, no fill)
- sun: circle(12,12,r4) + `M12 2.5v2M12 19.5v2M4.6 4.6L6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4L6 18M18 6l1.4-1.4`
- book: `M5 4.5A1.5 1.5 0 0 1 6.5 3H19v14H6.5A1.5 1.5 0 0 0 5 18.5z` + `M5 18.5A1.5 1.5 0 0 0 6.5 20H19`
- drop: `M12 3.5s6 6.4 6 10.5a6 6 0 0 1-12 0c0-4.1 6-10.5 6-10.5z`
- calm: `M12 19c-1.8-1.6-3-3.8-3-6.5s1.2-5.2 3-7c1.8 1.8 3 4.3 3 7s-1.2 4.9-3 6.5z` + `M12 19c-3.5 0-7-2-8-6 2.6-.2 5 .6 6.6 2.2` + `M12 19c3.5 0 7-2 8-6-2.6-.2-5 .6-6.6 2.2`
- dumbbell: `M6.5 7v10M3.5 9.5v5M17.5 7v10M20.5 9.5v5M6.5 12h11`
- pill: rect(x3 y8.5 w18 h7 rx3.5) rotated -45° about (12,12) + `M9.5 9.5l5 5`
- leaf: `M5 19c0-8 5-14 14-14 0 9-6 14-14 14z` + `M5 19l8-8`
- pen: `M4 20h4L19 9l-4-4L4 16z` + `M14 6l4 4`

UI glyphs: plus `M12 5v14M5 12h14`, check `M5 12.5l4.5 4.5L19 7.5`.
