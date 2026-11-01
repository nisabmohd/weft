// Same signatures as the play and github builds' class, whose parameters this stand-in doesn't need.
@file:Suppress("UNUSED_PARAMETER", "unused")

package app.sprout.habits.data.backup

import android.content.Context
import android.content.Intent
import app.sprout.habits.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The foss build (F-Droid) has no Google code: this stands in for Drive backup, which it doesn't have. */
class DriveBackup(context: Context, backup: BackupManager, settings: SettingsRepository) {
    val enabled: Boolean = false
    val backingUp: StateFlow<Boolean> = MutableStateFlow(false)
    fun online(): Boolean = false
    fun connect(): DriveAuth = DriveAuth.Failed
    fun authorize(): DriveAuth = DriveAuth.Failed
    fun tokenFrom(data: Intent?): String? = null
    fun backedOut(resultCode: Int, data: Intent?): Boolean = true
    fun driveRefused(data: Intent?): Boolean = false
    fun finishConnect(token: String): DriveResult = DriveResult.FAILED
    fun backUpNow(token: String, manual: Boolean = true): DriveResult = DriveResult.FAILED
    fun list(token: String): Pair<DriveResult, List<DriveBackupInfo>> = DriveResult.FAILED to emptyList()
    fun download(token: String, id: String): Pair<DriveResult, BackupFile?> = DriveResult.FAILED to null
    fun deleteAll(token: String): DriveResult = DriveResult.FAILED
    fun disconnect() = Unit
    fun reschedule() = Unit
    fun ensureScheduled() = Unit
}
