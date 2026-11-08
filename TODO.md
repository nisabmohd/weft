# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Testing (on a real phone)
- [ ] **Alarm reminders (Android 14+, Play build):** ring full screen with the phone locked (slide = Done), silenced by the power button in a pocket, and with Do Not Disturb on (the alarm channel bypasses it). After the full-screen intent declaration in Play Console, check Play kept `USE_FULL_SCREEN_INTENT` (Settings → Apps → Weft → Full screen notifications). The emulator covers every other case (smoke `alarm_*`).
- [ ] **Google Drive on the release build:** install `weft-v1.6.0-github.apk` (or the Play build), Connect Google Drive (tick Google Drive), Back up now, Restore. The minified Drive path hasn't run on the emulator since it lost its connection to Google (2026-11-02).
- [ ] **Google Drive edge cases:** Switch account with two accounts; revoke Weft in the Google account, then Back up now (it should ask again).
- [ ] **In-App Updates** from a Play install with a lower versionCode (internal testing or internal app sharing): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
