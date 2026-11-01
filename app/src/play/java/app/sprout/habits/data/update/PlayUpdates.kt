package app.sprout.habits.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import app.sprout.habits.data.SettingsRepository
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The play build's updates, through Google Play's In-App Updates: Play says whether it has a newer
 * Weft, and its own full-screen update (immediate type) downloads, installs and restarts the app.
 * No network from the app itself; Play does it. Installs that didn't come from Play get FAILED.
 */
class PlayUpdates(context: Context, private val settings: SettingsRepository) {
    val enabled: Boolean = true
    private val manager = AppUpdateManagerFactory.create(context)
    /** The answer [start] needs. Play takes each one once, so it is dropped after use. */
    private var info: AppUpdateInfo? = null
    /** The version [start] last offered, for [later]. */
    private var lastOffered: Int? = null

    /** Asks Play now. */
    suspend fun check(): PlayUpdateCheck {
        val i = withTimeoutOrNull(TIMEOUT_MS) { manager.appUpdateInfo.awaitOrNull() } ?: return PlayUpdateCheck.FAILED
        info = i
        return if (i.offersUpdate()) PlayUpdateCheck.AVAILABLE else PlayUpdateCheck.UP_TO_DATE
    }

    /**
     * The check on app start. True to show Play's update screen: always when an update the user
     * started was interrupted, else at most once a day and never for a version the user put off.
     */
    suspend fun checkIfDue(now: Long = System.currentTimeMillis()): Boolean {
        val s = settings.settings.first()
        if (check() != PlayUpdateCheck.AVAILABLE) return false
        val i = info ?: return false
        if (i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) return true
        if (s.lastUpdateCheckAt != null && now - s.lastUpdateCheckAt < DAY_MS) return false
        settings.setUpdateCheck(null, now)
        return i.availableVersionCode().toString() != s.dismissedUpdate
    }

    /** Opens Play's update screen for the update [check] found; false if there is none. */
    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val i = info ?: return false
        info = null
        lastOffered = i.availableVersionCode()
        return runCatching {
            manager.startUpdateFlowForResult(i, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
        }.getOrDefault(false)
    }

    /** The user backed out of the update the app start offered: don't offer that version again. */
    suspend fun later() {
        lastOffered?.let { settings.dismissUpdate(it.toString()) }
    }

    private fun AppUpdateInfo.offersUpdate(): Boolean = when (updateAvailability()) {
        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> true
        UpdateAvailability.UPDATE_AVAILABLE -> isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        else -> false
    }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
