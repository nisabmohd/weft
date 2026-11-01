# Play Console: Data safety answers (1.5.0)

What to enter in Play Console → Policy and programs → App content → **Data safety** for the play build from 1.5.0, when Google Drive backup arrived. Before 1.5.0 the answer was "No data collected". Google Play counts data as **collected** when the app sends it off the phone, even to the user's own Google account, so the Drive backup has to be declared. It isn't **shared**: the user starts each transfer, and it goes to their own account, not to us or to anyone else. The answers below match `PRIVACY.md`.

## Before you submit

The privacy policy URL is `https://nisabmohd.github.io/weft/privacy/` (the website, built from `PRIVACY.md` on every push to `main`; `https://github.com/nisabmohd/weft/blob/main/PRIVACY.md` shows the same text). Set it in Play Console → App content → Privacy policy.

## Overview

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS to Google only) |
| Which of the following methods of account creation does your app support? | **My app does not allow users to create an account** (Drive uses the Google account already on the phone; Weft has no accounts) |
| Do you provide a way for users to request that their data is deleted? | **Yes**: in the app (Backup & restore → Google account → Delete backups from Drive; or uninstall), and in Google Drive (Manage apps → Weft → Delete hidden app data) |

## Data types

Tick only these. Give every one of them the same answers:

| Data type | Collected | Shared | Processed ephemerally | Required or optional | Purpose |
| --- | --- | --- | --- | --- | --- |
| Personal info → **Email address** | Yes | No | No | **Optional** (users can choose) | App functionality |
| App activity → **Other user-generated content** (habit names, notes) | Yes | No | No | **Optional** | App functionality |
| App activity → **Other actions** (daily check-ins, settings) | Yes | No | No | **Optional** | App functionality |

The email is read from the Google account the user picks and shown on Backup & restore. It's declared to be safe, even though Weft only stores it on the phone. Each backup also carries the phone's model name (for example "Pixel 8"). That is not an identifier in Play's list, so it needs no tick of its own.

## Not collected

- Location, financial info, health and fitness, messages, photos and videos, audio, files and docs, calendar, contacts, web browsing, app info and performance (no crash reporting or analytics), and device or other IDs.
- **In-App Updates and In-App Review** (`app-update`, `review`): these show Google Play's own screens and send nothing for Weft. Play's SDK Index lists nothing for them to declare.
- The **F-Droid** and **GitHub** builds aren't on Play, so this form doesn't apply to them. The GitHub build's update check sends no personal data.

## Check after saving

On the store listing preview, the Data safety section should say: "Data is encrypted in transit", "You can request that data be deleted", "No data shared with third parties", and that the data collected is Personal info and App activity, for app functionality.
