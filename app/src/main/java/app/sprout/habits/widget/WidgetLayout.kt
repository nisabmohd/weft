package app.sprout.habits.widget

/**
 * How each widget lays itself out at the size the launcher gives it (in dp). The widgets use
 * Glance's exact size mode, so they get their real size on every resize; these rules decide what
 * fits. Pure functions, so `WidgetLayoutTest` covers every size between the smallest and largest a
 * widget may be resized to (the min/max resize sizes in `res/xml/widget_*.xml`).
 */
internal object WidgetLayout {

    /** The Today widget: the ring, then "Up next" with up to three habits. */
    data class Today(
        val pad: Float,
        val ring: Float,
        /** Whether the ring shows "today" under the count (only when there's room). */
        val ringLabel: Boolean,
        /** "5/8" in a large size (screenTitle) or a smaller one (cardTitle). */
        val bigCount: Boolean,
        /** Whether the habits are shown at all (too narrow: just the ring). */
        val showList: Boolean,
        /** "Up next" above the rows. */
        val showHeader: Boolean,
        /** Full rows with a circle to mark done (48 dp each). */
        val rows: Int,
        /** Too short for a row: one line, "Next: Read", instead. */
        val nextLine: Boolean,
    )

    const val ROW = 48f
    const val ROW_GAP = 8f
    const val HEADER = 20f

    fun today(width: Float, height: Float): Today {
        val pad = if (height < 100f || width < 160f) 10f else 16f
        val innerW = width - 2 * pad
        val innerH = height - 2 * pad
        val showList = width >= 200f
        val ring = minOf(104f, innerH, if (showList) innerW * 0.4f else innerW).coerceAtLeast(28f)
        val headerAndRow = HEADER + ROW_GAP + ROW
        val showHeader = showList && innerH >= headerAndRow
        val rows = when {
            !showList -> 0
            showHeader -> ((innerH - HEADER) / (ROW + ROW_GAP)).toInt().coerceIn(1, 3)
            innerH >= ROW -> 1
            else -> 0
        }
        return Today(
            pad = pad,
            ring = ring,
            ringLabel = ring >= 80f,
            bigCount = ring >= 64f,
            showList = showList,
            showHeader = showHeader,
            rows = rows,
            nextLine = showList && rows == 0,
        )
    }

    /** The This week widget: header, day letters, then one row per habit. */
    data class Week(
        val showNames: Boolean,
        val showRange: Boolean,
        val showLetters: Boolean,
        val bigPercent: Boolean,
        val marksWidth: Float,
        /**
         * Each habit row's height: the rows share the height below the header so a tall widget is
         * filled, not left empty at the bottom (user, 2026-11-08). Never below [WEEK_ROW_MIN] (then
         * the list scrolls) nor above [WEEK_ROW_MAX].
         */
        val rowHeight: Float = WEEK_ROW_MIN,
    )

    /** A week row at its smallest: the 28 dp tile and 5 dp above and below. */
    const val WEEK_ROW_MIN = 38f
    /** Taller rows only spread the marks apart; past this they'd look loose. */
    const val WEEK_ROW_MAX = 56f

    /** The habit tile (28) and the gap after it. */
    private const val TILE = 28f + 10f
    /** The tile, the name and the gap before the marks, when names are shown. */
    private const val NAME_COLUMN = 130f

    fun week(width: Float, height: Float, habits: Int = 0): Week {
        val inner = width - 32f
        val showNames = width >= 290f
        val marksWidth = if (showNames) (inner - NAME_COLUMN).coerceAtLeast(120f) else (inner - TILE).coerceAtLeast(70f)
        val showRange = height >= 150f
        val showLetters = height >= 130f
        val bigPercent = width >= 220f
        // What sits above the rows: the 16 dp padding, the header (the title and range, or the
        // big percent, whichever is taller), the 8 dp gap and the day letters; then 16 dp below.
        val header = maxOf(28f + if (showRange) 20f else 0f, if (bigPercent) 36f else 28f)
        val above = 16f + header + 8f + if (showLetters) 16f else 0f
        // 16 dp padding below, and 8 dp of slack: Glance's text runs a little taller than its size.
        val room = height - above - 16f - 8f
        val rowHeight = if (habits > 0) (room / habits).coerceIn(WEEK_ROW_MIN, WEEK_ROW_MAX) else WEEK_ROW_MIN
        return Week(showNames, showRange, showLetters, bigPercent, marksWidth, rowHeight)
    }

    /** The Today strip: the count, "Next: …" and one bar per habit. */
    data class Strip(val padV: Float, val showNext: Boolean, val showBar: Boolean)

    fun strip(width: Float, height: Float): Strip = Strip(
        padV = if (height < 70f) 8f else 14f,
        showNext = width >= 230f,
        showBar = height >= 56f,
    )

    /** The streak widget: the habit, the streak and its last 7 days. */
    data class Streak(
        val pad: Float,
        val compact: Boolean,
        /** Only the number: the smallest sizes. */
        val micro: Boolean,
        val showName: Boolean,
        val showMarks: Boolean,
        val marksWidth: Float,
        val markSize: Float,
    )

    fun streak(width: Float, height: Float): Streak {
        // Launchers give a 2x2 cell anywhere from about 100 to 200 dp; shrink instead of cutting off.
        val micro = height < 90f || width < 100f
        val compact = micro || height < 170f || width < 150f
        val tiny = height < 120f
        val pad = when {
            micro -> 8f
            compact -> 12f
            else -> 18f
        }
        val markSize = if (compact) 14f else 18f
        val marksWidth = (width - 2 * pad).coerceAtLeast(0f)
        return Streak(
            pad = pad,
            compact = compact,
            micro = micro,
            showName = !micro,
            // Seven marks with a little space between them, or none.
            showMarks = !tiny && !micro && marksWidth >= 7 * markSize + 12f,
            marksWidth = marksWidth,
            markSize = markSize,
        )
    }
}
