// Same signatures as the play build's class, whose parameters this stand-in doesn't need.
@file:Suppress("UNUSED_PARAMETER", "unused")

package app.sprout.habits.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import app.sprout.habits.data.SettingsRepository

/** The foss and github builds have no Google Play code: this stands in for the play build's class. */
class PlayUpdates(context: Context, settings: SettingsRepository) {
    val enabled: Boolean = false
    fun check(): PlayUpdateCheck = PlayUpdateCheck.UP_TO_DATE
    fun checkIfDue(now: Long = System.currentTimeMillis()): Boolean = false
    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean = false
    fun later() = Unit
}
