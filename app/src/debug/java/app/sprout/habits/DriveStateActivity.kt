package app.sprout.habits

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only: sets the Drive backup state without a Google account, for screenshots and
 * checks of the connected rows on an emulator.
 * `adb shell am start -n app.sprout.habits/.DriveStateActivity --es account you@gmail.com --el lastBackupAt 1767000000000 --ei count 7`
 * `--es failed FULL` (a `DriveResult` name) marks the last backup failed; `--el lastExportAt …` sets
 * the foss build's "Last exported". With no extras it disconnects.
 */
class DriveStateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = (application as WeftApp).container.settings
        val account = intent.getStringExtra("account")
        runBlocking {
            settings.setDriveAccount(account)
            if (account != null) {
                val at = intent.getLongExtra("lastBackupAt", 0L)
                val count = intent.getIntExtra("count", -1).takeIf { it >= 0 }
                if (at > 0) settings.setBackupDone(at, count)
                intent.getStringExtra("failed")?.let { settings.setBackupFailed(it) }
            }
            intent.getLongExtra("lastExportAt", 0L).takeIf { it > 0 }?.let { settings.setExported(it) }
        }
        finish()
    }
}
