package app.sprout.habits.data.backup

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import app.sprout.habits.WeftApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The scheduled Drive backup (JobScheduler: overnight, charging, on the right network). It never
 * shows a screen: if Google asks the user to allow Weft again, the backup is marked failed and
 * Backup & restore shows "Last backup failed" with Try again.
 */
// Its id is far above WorkManager's (see DriveBackup.JOB_ID), so no id range is needed.
@SuppressLint("SpecifyJobSchedulerIdRange")
class DriveBackupJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val container = (application as WeftApp).container
        running = scope.launch {
            val drive = container.drive
            val s = container.settings.settings.first()
            // Backed up by hand a few hours ago: nothing new to keep, wait for the next slot.
            val recent = s.lastBackupAt != null && System.currentTimeMillis() - s.lastBackupAt in 0 until RECENT_MS
            val result = when {
                s.driveAccount == null -> DriveResult.DONE
                recent -> DriveResult.DONE
                else -> when (val auth = drive.authorize()) {
                    is DriveAuth.Token -> drive.backUpNow(auth.value, manual = false)
                    else -> {
                        container.settings.setBackupFailed(DriveResult.NEEDS_CONSENT.name)
                        DriveResult.NEEDS_CONSENT
                    }
                }
            }
            // A dropped connection is worth another try soon; anything else waits for the next slot.
            val retry = result == DriveResult.OFFLINE
            if (!retry) drive.reschedule()
            jobFinished(params, retry)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // The system stopped it (charger out, Wi-Fi lost): try again when it can.
        running?.cancel()
        return true
    }

    private companion object {
        const val RECENT_MS = 6 * 60 * 60 * 1000L
    }
}
