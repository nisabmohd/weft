package app.sprout.habits.data.backup

import androidx.activity.result.IntentSenderRequest

/** The answer to "may Weft use the user's Google Drive?" (play and github builds, see `DriveBackup`). */
sealed interface DriveAuth {
    /** Allowed: an access token for Drive's hidden app folder. */
    data class Token(val value: String) : DriveAuth
    /** Google's account or consent screen has to be shown first. */
    data class NeedsConsent(val request: IntentSenderRequest) : DriveAuth
    data object Failed : DriveAuth
    /** The account allowed only its email: Google's screen lets the user untick Google Drive. */
    data object DriveRefused : DriveAuth
}

/** How a Drive action ended. */
enum class DriveResult {
    DONE,
    /** Restore: this Google account has no Weft backup yet. */
    NO_BACKUP,
    /** No internet connection. */
    OFFLINE,
    /** Wi-Fi only is on and the phone is on mobile data. */
    WIFI_NEEDED,
    /** The Google account's storage is full. */
    FULL,
    /** The scheduled backup found that Google wants the user to allow Weft again. */
    NEEDS_CONSENT,
    FAILED,
}

/** One backup in Drive, as the Restore sheet lists it. */
data class DriveBackupInfo(
    val id: String,
    val createdAt: Long,
    /** The phone that made it ("Pixel 8"). */
    val device: String,
    val habits: Int,
    val sizeBytes: Long,
)
