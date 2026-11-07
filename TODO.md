# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Testing
- [ ] **1.6.0 alarms on a real phone (Android 14+, from the Play build):** an alarm reminder with the phone locked (rings full screen, slide = Done), in a pocket (power button silences), with Do Not Disturb on (the alarm channel bypasses it), and after declaring the full-screen intent in Play Console (release/HOW-TO-RELEASE.md); check Play hasn't revoked USE_FULL_SCREEN_INTENT (Settings → Apps → Weft → Full screen notifications). The emulator covered every other case (smoke `alarm_*`).
- [x] **1.6.0:** sheets instead of dialogs, one button pair, action rows, filled note fields, Insights Patterns, notification buttons with Undo, alarm reminders, weekly support prompt, the new yarn-ball logo (design `weft-design-2026-11-08 (1).zip`, `weft-icon.zip`).
- [ ] **Drive on the release build, on a real phone:** install `weft-v1.5.0-github.apk` (or the Play internal testing build), Connect Google Drive (tick Google Drive), Back up now, Restore. The emulator lost its connection to Google on 2026-11-02 ("Network error" for the debug build too), so the minified Drive path wasn't run there.
- [ ] **Test Drive on a real phone** (the emulator run on 2026-11-01 passed Connect, Back up now, Restore, keep 10, frequency and the nightly job, offline, Wi-Fi only, Disconnect, Delete backups and the unticked-Drive case): still to do Switch account with two accounts, and revoking Weft in the Google account then Back up now (should ask again).
- [ ] **Test In-App Updates** from a Play install (internal testing or internal app sharing with a lower versionCode): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **Test In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
