package app.sprout.habits.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Weft's spacing and size tokens, one name per job (docs/SPACING.md). Screens use these, never a raw
 * number (user, 2026-11-11: "smallest detail matters"). Everything is on the 4 dp grid; the few
 * documented exceptions (22 dp icons, 14 dp tile corners, 2 dp hairlines) are named too.
 * Component-specific sizes live next to their component (HabitTileSize, ButtonHeight, Sheet*,
 * ActionRow*, InfoRow*, Note*, HabitChip*), and use these where they mean the same thing.
 */

// --- The grid ------------------------------------------------------------------------------------

/** 2 dp: a title to the line under it, rows of one group, hairline gaps. */
val Space2 = 2.dp
/** 4 dp: a section header's inset, the smallest gap between a label and its control. */
val Space4 = 4.dp
/** 8 dp: between cards, chips and header buttons; an icon to its text. */
val Space8 = 8.dp
/** 12 dp: a tile to its text, a card's header to its content, text to a trailing control. */
val Space12 = 12.dp
/** 16 dp: page sides, inside every card, between sections. */
val Space16 = 16.dp
/** 20 dp: above a section header; sheet sides. */
val Space20 = 20.dp
/** 24 dp: big gaps (empty states, a sheet's title inset). */
val Space24 = 24.dp
/** 32 dp: the largest gap (empty state art to its title). */
val Space32 = 32.dp
/** 40 dp: big breathing room (the alarm screen's bottom). */
val Space40 = 40.dp
/** 48 dp: room above an empty state or under a list's last card. */
val Space48 = 48.dp

// --- What the gaps are for (docs/SPACING.md → Gaps) ----------------------------------------------

/** Left and right of every screen's content. */
val ScreenPadding = Space16
/** Inside every card and grouped row. */
val CardPadding = Space16
/** Between separate cards (Today, Week, Overall, Manage habits). */
val CardGap = Space8
/** Between sections that have no header. */
val SectionGap = Space16
/** A habit tile or row icon to the text next to it. */
val TileTextGap = Space12
/** A card's header to its content (marks, heatmap, the score bar). */
val CardContentGap = Space12
/** Text to the switch, value or chevron on its right. */
val TrailingGap = Space12
/** Between the round buttons in a header, between chips. */
val ItemGap = Space8
/** A small icon to its label inside a chip, pill or button. */
val IconLabelGap = Space8
/** A section header's inset from the cards' edges. */
val SectionHeaderInset = Space4

/** A tab's fixed header (title, buttons, filter chips): 16 dp at the sides, 12 dp on top, 16 dp to the first card. */
val TabHeaderPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = Space12, bottom = SectionGap)
/** A tab title to its filter chips. */
val HeaderChipsGap = Space12
/** Under a list's last card, so it can scroll clear of the nav bar's edge. */
val ListEndPadding = Space24
/** The extended Add note button: 16 dp corners, 16 dp from the screen's edges; lists end 96 dp up so it never covers the last card. */
val FabCorner = Space16
val FabShape = RoundedCornerShape(FabCorner)
val FabMargin = Space16
val FabListEndPadding = 96.dp
/** How far the Add note button moves up while the Undo bar shows, and the bar's side margin. */
val SnackbarLift = 72.dp
val SnackbarSideMargin = Space8
/** The Undo bar's corners, like the habit tile's. */
val SnackbarShape = RoundedCornerShape(14.dp)

/** A settings, choice or link row: 16 dp at the sides, 12 dp above and below, at least 56 dp tall. */
val RowPadding = PaddingValues(horizontal = Space16, vertical = Space12)
val RowMinHeight = 56.dp

// --- Sizes ---------------------------------------------------------------------------------------

/** Today's habit card, the model for every habit card: at least 68 dp, 12 dp at the sides and 8 dp above and below. */
val HabitCardMinHeight = 68.dp
val HabitCardPadding = PaddingValues(horizontal = Space12, vertical = Space8)
/** The round Done / to-do circle on a habit card (40 dp, in a 48 dp touch target). */
val CheckCircleSize = 40.dp
/** Get started's bottom buttons (Continue, Skip for now): 56 dp. */
val LargeButtonHeight = 56.dp
/** About's logo tile: 80 dp with a 40 dp mark. */
val AboutLogoSize = 80.dp
val AboutLogoIconSize = 40.dp
/** A small pill button inside a row or sheet header (Remove, Undo, Check for updates). */
val SmallButtonHeight = 40.dp
/** A full-screen form's top bar (back or close, the title, Save): 64 dp, 8 dp from the status bar. */
val TopBarHeight = 64.dp
val TopBarTopPadding = Space8
/** The back or close button to the top bar's title. */
val TopBarTitleGap = Space12
/** Outlined text fields (Name, Target…): 14 dp corners, like the habit tile. */
val FieldShape = RoundedCornerShape(14.dp)
/** New habit's day circles (M–S). */
val DayCircleSize = 44.dp

/** Header icon buttons and every touch target. */
val TouchTarget = 48.dp
val HeaderButtonSize = TouchTarget
/** Icons: 22 dp in header buttons and habit tiles, 20 dp in action tiles and as chevrons, 18 dp in chips, 16 dp inside small marks. */
val IconSize = 22.dp
val IconSizeSmall = 20.dp
val IconSizeChip = 18.dp
val IconSizeTiny = 16.dp
/** A day mark in Habits → Week. */
val WeekMarkSize = 32.dp
/** A legend's colour square (Overall, Weekly rhythm). */
val LegendSwatchSize = Space12
/** Habit detail's header tile: 56 dp, 16 dp corners, a 28 dp icon. */
val DetailTileSize = 56.dp
val DetailTileShape = RoundedCornerShape(Space16)
val DetailTileIconSize = 28.dp
/** A day in a month grid (date range sheet, habit detail calendar). */
val CalendarDaySize = 40.dp
/** The dot that marks an active header button. */
val ActiveDotSize = 8.dp
/** A 1 dp border (unpicked chips); 2 dp for a selected ring. */
val BorderThin = 1.dp
val BorderThick = 2.dp

// --- Shapes (docs/SPACING.md → Sizes) ------------------------------------------------------------

/** Cards and grouped rows (outer corners). */
val CardCorner = 24.dp
val CardShape = RoundedCornerShape(CardCorner)
/** Today's habit cards. */
val HabitCardCorner = 22.dp
/** Sheets and big panels (New note's card, the Welcome panel). */
val SheetCorner = 28.dp
/** Filter chips and small selected marks. */
val ChipCorner = 8.dp
val ChipShape = RoundedCornerShape(ChipCorner)
val ChipHeight = 32.dp
/** Fully rounded: pills, buttons, round buttons. */
val PillShape = CircleShape
