package app.sprout.habits.ui.nav

import android.content.Intent
import android.net.Uri

/**
 * Where an outside tap should land: a widget, a notification, a launcher shortcut (long-press
 * the icon) or a `weft://` link. One path for all of them, so they behave the same.
 *
 * Links: `weft://today`, `weft://habits`, `weft://journal`, `weft://insights`,
 * `weft://more`, `weft://note/new`, `weft://habit/new`, `weft://habit/<id>`.
 * They only open screens; nothing is changed until the user saves. `sprout://` (the name before
 * the Weft rename) still works the same.
 */
sealed interface AppLink {
    data class Habit(val id: Long) : AppLink
    data object NewNote : AppLink
    data object NewHabit : AppLink
    data class OpenTab(val tab: String) : AppLink

    companion object {
        const val SCHEME = "weft"
        private val SCHEMES = setOf(SCHEME, "sprout")
        const val ACTION_OPEN_HABIT = "app.sprout.habits.action.OPEN_HABIT"
        const val EXTRA_HABIT_ID = "habitId"
        private val TABS = setOf("today", "habits", "journal", "insights", "more")

        /** The link in [intent], or null for a plain launch (or anything unknown). */
        fun from(intent: Intent?): AppLink? {
            intent ?: return null
            if (intent.action == ACTION_OPEN_HABIT) {
                return intent.getLongExtra(EXTRA_HABIT_ID, 0L).takeIf { it > 0L }?.let { Habit(it) }
            }
            if (intent.action != Intent.ACTION_VIEW) return null
            return parse(intent.data)
        }

        fun parse(uri: Uri?): AppLink? {
            if (uri == null || uri.scheme !in SCHEMES) return null
            val host = uri.host?.lowercase() ?: return null
            val first = uri.pathSegments.firstOrNull()
            return when {
                host in TABS && first == null -> OpenTab(host)
                host == "note" && first == "new" -> NewNote
                host == "habit" && first == "new" -> NewHabit
                host == "habit" -> first?.toLongOrNull()?.takeIf { it > 0L }?.let { Habit(it) }
                else -> null
            }
        }
    }
}
