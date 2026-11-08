package app.sprout.habits.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every widget can be resized between the min and max resize sizes in its `res/xml/widget_*.xml`;
 * at every size in between, what [WidgetLayout] picks must fit.
 */
class WidgetLayoutTest {

    private data class Limits(val minW: Float, val minH: Float, val startW: Float, val startH: Float, val maxW: Float, val maxH: Float)

    private fun limits(name: String): Limits {
        val xml = listOf("src/main/res/xml/widget_$name.xml", "app/src/main/res/xml/widget_$name.xml")
            .map(::File).first { it.exists() }.readText()
        fun dp(attr: String): Float {
            val m = Regex("""android:$attr="(\d+)dp"""").find(xml)
            assertTrue("widget_$name.xml has no $attr", m != null)
            return m!!.groupValues[1].toFloat()
        }
        return Limits(dp("minResizeWidth"), dp("minResizeHeight"), dp("minWidth"), dp("minHeight"), dp("maxResizeWidth"), dp("maxResizeHeight"))
    }

    /** Every size from the smallest to the largest, in 10 dp steps (plus both ends). */
    private fun sizes(l: Limits): List<Pair<Float, Float>> {
        fun steps(from: Float, to: Float) = (generateSequence(from) { it + 10f }.takeWhile { it < to } + to).toList()
        return steps(l.minW, l.maxW).flatMap { w -> steps(l.minH, l.maxH).map { h -> w to h } }
    }

    @Test
    fun everyWidgetCanShrinkAndHasAMaximum() {
        for (name in listOf("today", "week", "strip", "streak")) {
            val l = limits(name)
            assertTrue("$name can't shrink below its starting width", l.minW < l.startW || l.minW <= 110f)
            assertTrue("$name can't shrink below its starting height", l.minH < l.startH || l.minH <= 50f)
            assertTrue("$name max width is below its start", l.maxW >= l.startW)
            assertTrue("$name max height is below its start", l.maxH >= l.startH)
            // No bigger than a phone's full width (5 columns) and most of its height.
            assertTrue("$name can grow too wide", l.maxW <= 560f)
            assertTrue("$name can grow too tall", l.maxH <= 620f)
        }
    }

    @Test
    fun todayFitsAtEverySize() {
        for ((w, h) in sizes(limits("today"))) {
            val t = WidgetLayout.today(w, h)
            val innerW = w - 2 * t.pad
            val innerH = h - 2 * t.pad
            assertTrue("ring too tall at ${w}x$h", t.ring <= maxOf(innerH, 28f))
            assertTrue("ring too wide at ${w}x$h", t.ring <= innerW)
            assertTrue("ring too small at ${w}x$h", t.ring >= 28f)
            val listHeight = (if (t.showHeader) WidgetLayout.HEADER + WidgetLayout.ROW_GAP else 0f) +
                t.rows * WidgetLayout.ROW + (t.rows - 1).coerceAtLeast(0) * WidgetLayout.ROW_GAP
            assertTrue("rows don't fit at ${w}x$h: $t", listHeight <= innerH + 0.5f)
            if (t.showList) {
                assertTrue("no room for the habits at ${w}x$h", innerW - t.ring - 16f >= 80f)
                assertTrue("nothing to show beside the ring at ${w}x$h", t.rows > 0 || t.nextLine)
            }
            assertTrue("at most 3 rows", t.rows <= 3)
        }
        // The usual 4x2 size looks as before: the full ring, "Up next" and two rows.
        val usual = WidgetLayout.today(320f, 180f)
        assertTrue(usual.ring == 104f && usual.ringLabel && usual.showHeader && usual.rows == 2)
    }

    @Test
    fun weekFitsAtEverySize() {
        for ((w, h) in sizes(limits("week"))) {
            val k = WidgetLayout.week(w, h)
            val inner = w - 32f
            val used = k.marksWidth + if (k.showNames) 130f else 38f
            assertTrue("marks overflow at ${w}x$h: $k", used <= inner + 0.5f)
            assertTrue("marks too small at ${w}x$h", k.marksWidth >= 7 * 10f)
            if (k.showNames) assertTrue("names squeezed at ${w}x$h", inner - k.marksWidth - 38f >= 80f)
        }
        val usual = WidgetLayout.week(320f, 320f)
        assertTrue(usual.showNames && usual.showRange && usual.showLetters && usual.bigPercent)
    }

    @Test
    fun weekRowsFillTheHeight() {
        // A tall widget with 8 habits: the rows grow to fill it, and stay within their limits.
        val tall = WidgetLayout.week(320f, 560f, habits = 8)
        assertTrue("rows don't stretch: $tall", tall.rowHeight > WidgetLayout.WEEK_ROW_MIN)
        assertTrue(tall.rowHeight <= WidgetLayout.WEEK_ROW_MAX)
        // The rows plus what's above them never take more than the widget.
        val above = 16f + 48f + 8f + 16f
        assertTrue("rows overflow: $tall", above + tall.rowHeight * 8 + 16f + 8f <= 560f + 0.5f)
        // Too many habits for the height: the smallest rows, and the list scrolls.
        assertTrue(WidgetLayout.week(320f, 200f, habits = 12).rowHeight == WidgetLayout.WEEK_ROW_MIN)
        // Two habits on a big widget: capped, not spread across the whole height.
        assertTrue(WidgetLayout.week(320f, 560f, habits = 2).rowHeight == WidgetLayout.WEEK_ROW_MAX)
    }

    @Test
    fun stripFitsAtEverySize() {
        for ((w, h) in sizes(limits("strip"))) {
            val s = WidgetLayout.strip(w, h)
            val text = 14f * 1.3f
            val needed = 2 * s.padV + text + if (s.showBar) 10f + 10f else 0f
            assertTrue("strip overflows at ${w}x$h: $s", needed <= h + 0.5f)
        }
        val usual = WidgetLayout.strip(320f, 70f)
        assertTrue(usual.showNext && usual.showBar)
    }

    @Test
    fun streakFitsAtEverySize() {
        for ((w, h) in sizes(limits("streak"))) {
            val s = WidgetLayout.streak(w, h)
            if (s.showMarks) assertTrue("marks squeezed at ${w}x$h", s.marksWidth >= 7 * s.markSize + 12f)
            assertTrue("padding eats the widget at ${w}x$h", w - 2 * s.pad >= 30f && h - 2 * s.pad >= 30f)
        }
        val usual = WidgetLayout.streak(180f, 180f)
        assertTrue(!usual.micro && usual.showName && usual.showMarks)
    }
}
