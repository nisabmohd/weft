# Google Drive backup (1.5.0, play and github builds)

Backup to the user's **own Google Drive**, next to Export / Import file backup. The first design came as `drive-backup.zip` (2026-10-31: rows on More); the same day `drive-backup (1).zip` (`FEATURE-BACKUP-SCREEN.md` and eight mockups) moved backup to its own **Backup & restore** screen, which is what is built. Captures: `design/more.png`, `design/backup-restore.png` (connected), `design/backup-restore-not-connected.png`, `design/backup-restore-foss.png`, `design/backup-frequency-sheet.png`, `design/drive-account-sheet.png`; `design/drive-restore-sheet.png` is the designer's mockup (the sheet needs real backups in Drive).

The **foss** build (F-Droid) has no Drive and no Google code: its Backup & restore screen is file backup only.

## More

The first section, **"Backup & restore"**, holds one row, **Backups** (›), which opens the screen. Leading icon: `cloud_done` when Drive is connected, `cloud` when it isn't, `smartphone` in foss. Its line: "Last backup today, 9:12 AM" / "yesterday, 2:04 AM" / "27 Oct", "Backing up…", "Last backup failed", "Google Drive not connected"; foss "Last exported 27 Oct" or "No backups yet".

## Backup & restore screen

`ui/more/BackupRestoreScreen.kt`: the form header (48dp back circle, "Backup & restore"), then a **status card** (the 44dp `HabitTile`, a title and a line, then 12dp below one full-width 48dp button, the size of every full-width button in the app; the spec's 40dp was smaller than the rest), then grouped rows with section headers (no leading icons, no dividers). The status title is a polite live region, so TalkBack reads "Backed up today…" or "Last backup failed" when it changes.

**Connected** (`DriveSection.kt`, `DriveBackupContent`):

- Status: ✓ on secondaryContainer, "Backed up today, 9:12 AM" (or "No backups yet" with `cloud` on surfaceContainerHigh), "Google Drive · email", tonal **Back up now** (`cloud_upload`); while uploading "Backing up…" with a spinner, disabled. After a failure: `error` on errorContainer, "Last backup failed", the reason ("No connection", "Drive is full", "Google needs you to allow Weft again", "Couldn't reach Google Drive") and **Try again**.
- **Automatic backup:** Frequency ("Daily" ›, opens the frequency sheet) and Wi-Fi only · "Don't use mobile data" (switch, on by default).
- **Restore:** Restore from Drive · "7 backups, newest today" (the count is saved after each backup or listing; "Pick a backup to bring back" until then) and Import from file · "JSON backup".
- **Other:** Export to file · "Save a copy anywhere, no account needed" (opens the File format sheet: JSON / CSV) and Google account · "Switch account or disconnect" (the account sheet).

**Not connected:** status "Back up to Google Drive" / "Saved to your own Drive every night. Only Weft can see these files." with a filled **Connect Google Drive**; Connect runs a first backup straight away. Then **Backup to a file**: Export to file, Import from file.

**foss** (`FileBackupContent`): status `smartphone` on secondaryContainer, "Last exported today, 9:12 AM" (or "No backups yet"), "Your data stays on this phone. Export a file and keep it somewhere safe.", tonal **Export now** (`download`) in the chosen format. **Restore:** Import from file. **Export:** File format ("JSON" ›, a sheet: JSON / CSV, saved as `exportFormat`). The spec's "Include notes" switch is left out: Export always includes notes (the spec says to leave it out in that case).

**Account sheet** (the spec's "menu"; the app never uses menus, every choice is a sheet), built like the Journal's note actions sheet (`ActionRow`s: 22dp icon + `body` label, full width): title "Google account", the email, then **Switch account** (Google's account picker; the old account's access to Weft is removed), **Disconnect** (access removed, schedule cancelled, backups stay in Drive; the snackbar says so) and **Delete backups from Drive** (red; a "Delete backups from Drive?" dialog, then every Weft file in that account is deleted).

**Backup frequency sheet:** the app's `ChoiceSheet` (as Language and Week starts on): Daily · Every night while charging (default), Weekly · Sunday night, Monthly · On the 1st, Off · Only when you tap Back up now. Tapping saves and closes.

**Restore from Drive sheet:** `ChoiceSheet` with "Your current habits and notes will be replaced by the backup you pick.", up to 10 backups newest first (title "Today, 9:12 AM" / "Yesterday, 2:04 AM" / "27 Oct, 2:01 AM"; supporting "Pixel 8 · 7 habits · 48 KB"), the newest preselected, and a full-width 48dp **Restore** button (as the filter sheet's Show button; the spec's 56dp was bigger than every other sheet button). Restore downloads the backup, then "Replace current data?" / "This can't be undone. Export a file first if you want to keep it." (Cancel / Replace), then the snackbar "Restored backup from 27 Oct". A spinner shows while the list loads; "No Weft backup in this Google Drive yet" when there's none.

**Not followed from the spec:** WorkManager (JobScheduler is used, see Scheduling); "Import from file · JSON or CSV backup" (Import only reads JSON, so the line says "JSON backup"); the account menu (a sheet).

## Data

- Scope `drive.appdata` (the hidden app folder; Weft can't see any other file) plus `email` (to show the account). Google's `AuthorizationClient` (play-services-auth) picks the account and asks for consent; Drive v3 is called with plain `HttpURLConnection`.
- Each backup is the Export JSON, gzipped, named `sprout-backup-<ISO time>.json.gz`, with `appProperties` `device` (the phone's name, else its model) and `habits`. The newest **10** are kept; older ones are deleted after each upload.
- Settings: `driveAccount`, `driveFrequency` (DAILY), `driveWifiOnly` (true), `lastBackupAt`, `lastBackupError` (a `DriveResult` name, null when the last one worked), `driveBackupCount`; for files `lastExportAt` and `exportFormat` (JSON). Restoring a backup doesn't change them.

## Scheduling

**JobScheduler, not WorkManager** (WorkManager would be a new library; JobScheduler is part of Android and has the same constraints). One persisted job (`DriveBackupJob`, id 2000003101: far above the ids WorkManager, which Glance uses, hands out): minimum latency until the next slot (`domain/BackupSchedule.kt`: 1 AM daily, Monday 1 AM for "Sunday night", 1 AM on the 1st), requires charging, network `UNMETERED` when Wi-Fi only is on, else any. A missed slot runs at the next chance. After a run the next slot is scheduled; a dropped connection retries with backoff. Rescheduled on connect, frequency and Wi-Fi only changes, boot, clock and time zone changes; on app start only if nothing is pending (re-scheduling would stop a running job). A run within 6 hours of a manual backup is skipped.

The job never shows a screen. If Google wants the user to allow Weft again, the backup is marked failed ("Google needs you to allow Weft again") and Try again opens Google's screen.

## Edge cases

- **Offline:** checked before Google's screen opens (Connect, Back up now, Restore) and again when a connection drops part way: "No internet connection. Try again when you're online". An offline backup isn't marked failed.
- **Backing up elsewhere:** the nightly job's upload shows as "Backing up…" on More and the screen too (`DriveBackup.backingUp`), and Back up now waits for it.
- **Wi-Fi only on mobile data:** Back up now says "Wi-Fi only is on. Connect to Wi-Fi, or turn Wi-Fi only off"; the schedule waits for Wi-Fi.
- **Backing out of Google's screen:** nothing is said. The pending action survives the activity being recreated (rotation, dark mode) while that screen is open.
- **Expired or revoked token (401):** cleared, asked for again, retried once.
- **Drive full** (403 storageQuotaExceeded): "Your Google Drive is full. Free up space, then try again".
- **Rate limits and server errors** (429, 5xx, rateLimitExceeded): two retries, 2 s and 4 s apart.
- **One at a time:** a second tap while one runs does nothing; uploads, downloads and deletes are behind one lock, so the job and a manual backup never overlap.
- **Damaged, foreign or newer backups:** the same messages as Import from file.
- **No Google Play services, or no OAuth client:** "Couldn't reach Google Drive. Try again" Also when Google's account or consent screen ends with an error (no OAuth client gives status 8, `UNREGISTERED_ON_API_CONSOLE`); backing out of it (status 16, `CANCELED`) says nothing. If the user unticks Google Drive on Google's permission screen (it starts unticked), Google returns a token without `drive.appdata`: Weft doesn't connect and says "Weft needs Google Drive to keep your backups. Connect again and tick Google Drive". Every failure is logged under the `WeftDrive` tag: `adb logcat -s WeftDrive Auth`.

## Setup (owner)

Google Cloud: OAuth clients of type **Android** for `app.sprout.habits` with the SHA-1 of the Play app-signing key (play) and of the release key (github), plus the debug key for local testing; the **Google Drive API** on. `drive.appdata` and `email` aren't restricted scopes. See `TODO.md`. Play Console's Data safety answers for Drive backup are in [`PLAY-DATA-SAFETY.md`](PLAY-DATA-SAFETY.md).

## Testing

`python3 tools/verify/smoke.py drive` (play debug): More's Backups row and the screen in each state, the failed card, frequency sheet and the scheduled job, account sheet, format sheet, offline, no Google, foss without Drive code. The debug build's `DriveStateActivity` sets the state without an account (it closes Weft's task, so open Weft again after it):

```sh
adb shell am start -W -f 0x10008000 -n app.sprout.habits/.DriveStateActivity --es account you@gmail.com --el lastBackupAt 1790000000000 --ei count 7
adb shell am start -W -f 0x10008000 -n app.sprout.habits/.DriveStateActivity --es account you@gmail.com --es failed FULL
adb shell am start -W -f 0x10008000 -n app.sprout.habits/.DriveStateActivity --el lastExportAt 1790000000000   # foss "Last exported"
adb shell am start -W -f 0x10008000 -n app.sprout.habits/.DriveStateActivity   # disconnect
```

A real backup and restore needs a phone with a Google account and the OAuth client: connect, back up, clear app data, restore, compare counts; back up 11 times and check 10 are listed; switch account; disconnect; delete backups; run the job with `adb shell cmd jobscheduler run -f app.sprout.habits 2000003101` while charging.
