package app.sprout.habits.widget

import androidx.compose.ui.unit.dp

/*
 * The home-screen widgets' sizes, named (user, 2026-11-11: no raw numbers). Widgets are laid out by
 * WidgetLayout's float maths and checked by WidgetLayoutTest, so these keep their exact values;
 * change one only together with WidgetLayout.
 */

/** Every widget: 28 dp corners (the launcher's widget radius), 16 dp inside. */
val WidgetCorner = 28.dp
val WidgetPadding = 16.dp

/** Streak: the habit icon (24 dp, 18 dp when compact) 8 dp from its name; 10 dp (6 dp compact) above the marks. */
val StreakIconSize = 24.dp
val StreakIconSizeCompact = 18.dp
val StreakIconGap = 8.dp
val StreakMarksGap = 10.dp
val StreakMarksGapCompact = 6.dp

/** Today: the ring 16 dp (10 dp when small) from the list; each row 16 dp corners, the name 14 dp in, a 34 dp circle in a 48 dp target. */
val TodayRingGap = 16.dp
val TodayRingGapSmall = 10.dp
val TodayRowCorner = 16.dp
val TodayRowTextStart = 14.dp
val TodayCircleTarget = 48.dp
val TodayCircleSize = 34.dp

/** Strip: 18 dp at the sides, the count 12 dp from "Next: …", the 10 dp bar 10 dp under them. */
val StripSidePadding = 18.dp
val StripTextGap = 12.dp
val StripBarGap = 10.dp
val StripBarHeight = 10.dp

/** This week: 8 dp under the header; a 28 dp tile (9 dp corners, 16 dp icon), the name 10 dp from it; 20 dp marks; 12 dp above "No habits yet". */
val WeekHeaderGap = 8.dp
val WeekTileSize = 28.dp
val WeekTileCorner = 9.dp
val WeekTileIconSize = 16.dp
val WeekNameGap = 10.dp
val WeekMarksHeight = 20.dp
val WeekEmptyTop = 12.dp
