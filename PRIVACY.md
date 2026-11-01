# Weft privacy policy

_Last updated: 1 November 2026_

Weft (called Sprout before November 2026) is an open-source habit tracker for Android, published by Mohd. Nisab under the developer name mid apps ("we"). This policy covers the Weft app from Google Play, GitHub Releases and F-Droid.

## What Weft stores

Your habits, check-ins, notes and settings are stored on your device, in the app's private storage. We don't run a server and never receive this data. In the Google Play and GitHub builds you can also choose to keep backups in your own Google Drive (see below).

## What Weft collects and shares

We collect nothing: Weft has no account, no ads, no analytics and no crash reporting, and it doesn't share data with anyone. The only data that leaves your phone is the optional Google Drive backup, which goes to your own Google account (Google Play's Data safety section lists it as collected, optional and not shared).

- The F-Droid build doesn't connect to the internet and contains no Google code.
- The Google Play build connects to the internet only for Google Drive backup, once you connect it (see below). Checking for updates and the "Rate on Play Store" card go through the Google Play app on your phone, not through Weft.
- The GitHub build connects to GitHub (`api.github.com` and GitHub's download servers) to check for a new version of Weft and download it; that request contains no personal data. It also connects to Google for Google Drive backup, once you connect it.

## Permissions

- **Notifications**: to show the habit reminders you set.
- **Alarms & reminders** (exact alarms): optional, so reminders arrive on time.
- **Run at startup**: to set your reminders again after the phone restarts.

## Backups and exports

- **Export** writes a JSON or CSV file wherever you choose, using Android's file picker. What happens to that file afterwards is up to you.
- **Google Drive backup** (Google Play and GitHub builds, off until you tap Connect): you pick a Google account and allow Weft to use its own hidden folder in that account's Drive (the `drive.appdata` permission) and to read the account's email address, which Weft shows on its Backup & restore screen. Weft can only see the files it made there, not the rest of your Drive. Each backup is the same data as a JSON export (your habits, check-ins, notes and settings), compressed, plus your phone's name and the number of habits, so you can tell backups apart when restoring. Backups go straight from your phone to your Drive, when you tap "Back up now" or on the schedule you choose (overnight while charging, on Wi-Fi only unless you turn that off). Weft keeps the latest 10 and deletes older ones. Google stores them under your Google account and Google's privacy policy; we never receive them.
  - Weft uses the Google account access only to keep and restore these backups and to show the account's email address. It doesn't use it for ads, doesn't sell it or share it with anyone, and no person reads it. Weft's use and transfer of information received from Google APIs adheres to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.
  - **Disconnect** stops backups and removes Weft's access to your account; the backups stay in your Drive. **Delete backups from Drive** deletes them. You can also delete them in Google Drive (Settings → Manage apps → Weft → Delete hidden app data) and remove Weft's access in your Google account's security settings.
- **Android backup**: if your phone's backup is turned on, Android may include Weft's data in your device backup. Google runs that backup under your Google account and its own privacy policy. Weft never sees it.

## Deleting your data

Uninstall Weft, or clear its storage in Android Settings → Apps → Weft → Storage. If you used Google Drive backup, also use "Delete backups from Drive" (or delete Weft's hidden data in Google Drive) as described above. Because we hold no copy, there is nothing for us to delete on our side.

## Children

Weft isn't directed at children under 13 and collects no data from anyone.

## Changes

If this policy changes, the new version is published at this address with a new date.

## Contact

Email hello.midapps@gmail.com, or open an issue at https://github.com/nisabmohd/weft/issues.
