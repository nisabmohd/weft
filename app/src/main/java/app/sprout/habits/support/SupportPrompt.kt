package app.sprout.habits.support

import app.sprout.habits.data.Settings

/** When the "Enjoying Weft?" prompt may show. */
object SupportPrompt {
    private const val DAY = 24 * 60 * 60 * 1000L
    const val MIN_DAYS_OF_USE = 7
    const val MIN_CHECK_INS = 30
    /** Weekly (user, 2026-11-08; it was every 60 days). */
    const val MIN_DAYS_BETWEEN = 7
    /** After someone rates, stars or sponsors, they aren't asked again for about six months. */
    const val DAYS_AFTER_SUPPORT = 180
    const val MAX_DISMISSALS = 4

    /**
     * After 7+ days of use and 30+ check-ins, at most once a week, never again after "Maybe later"
     * four times, and not for 180 days after Rate / Star / Sponsor. The caller also never shows it
     * right after a skip.
     */
    fun shouldShow(s: Settings, now: Long): Boolean {
        val firstOpen = s.firstOpenAt ?: return false
        if (now - firstOpen < MIN_DAYS_OF_USE * DAY) return false
        if (s.checkInCount < MIN_CHECK_INS) return false
        if (s.supportPromptDismissCount >= MAX_DISMISSALS) return false
        s.supportPromptSupportedAt?.let { if (now - it < DAYS_AFTER_SUPPORT * DAY) return false }
        val lastShown = s.supportPromptShownAt
        return lastShown == null || now - lastShown >= MIN_DAYS_BETWEEN * DAY
    }
}
