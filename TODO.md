# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## 1.6.2 (branch `release-1.6.2`, bug fixes; merged when the user decides)

### Build (design in, 2026-11-08: `features/FEATURE-ALARM-REMINDERS.md` 7–10)
- [x] **Set up alarms sheet** (`alarm-9a/b/c-setup-sheet-*.png`): switches that show the real state and re-check on resume; the phone-maker rows only on ColorOS / OnePlus / Oppo / Realme, Xiaomi, Vivo, Huawei, Samsung; Use a notification instead / Done. The optional **Show over other apps** switch on those phones (`SYSTEM_ALERT_WINDOW`, user-approved 2026-11-08): when on and the full-screen alarm is blocked on a locked phone, Weft opens the ringing screen itself. Opens from the Reminder sheet when Alarm is picked and something's missing, from the Reminder card warning, and from More → Alarms.
- [x] **Reminder card warning** (`alarm-10-reminder-card-warning.png`): names the most important missing piece (notifications > exact alarms > full screen).
- [x] **More → Alarms row + Alarms screen** (`more-alarms-row.png`, `more-alarms-screen.png`): status card, Needed, On some phones, Sound (Alarm sound, Vibrate on by default; DataStore `alarmVibrate`).
- [x] **Alarm sound sheet** (`alarm-11-sound-sheet.png`): Weft (default), Weft · Soft, Phone's alarm sound, Choose from phone (Android's sound picker; the picked one becomes a fourth row). About 4 s of preview on the alarm stream. DataStore `alarmSound`, in the backup; `AlarmSoundService` and its `Ringtone` fallback read it; a picked sound that's gone falls back to Weft.
- [x] **Two Weft tones in `res/raw`** Done: our own, drawn by `tools/alarm_tones.py` (AAC .m4a, ~15 KB each), so nothing to credit.
- [ ] Recapture the seven screens from the app when built, and update the docs (`docs/` reminders skill, README if shown).

### Bugs
- [ ] **OnePlus: alarms never open full screen, locked or not; only the notification shows.** Expected: phone in use → the heads-up card (as now); screen off or locked → the ringing screen. To find out on the phone (which build, Play or GitHub; OxygenOS / Android version):
  - Settings → Apps → Weft → **Full screen notifications** (Android 14+). The Play build loses `USE_FULL_SCREEN_INTENT` unless the Play Console declaration is accepted; `Notifications.canUseFullScreen()` would be false.
  - OxygenOS / ColorOS app settings: **Lock screen** notifications, **Banners / floating**, **Pop-up windows** (blocks activities started from the background), battery **Allow background activity**.
  - `adb shell dumpsys notification` for the alarm: is the full-screen intent there and was it suppressed?
  - Fix in 1.6.2: the setup sheet (notifications, alarms, full screen, and the phone-maker rows that open the right settings page). Plus the optional Show over other apps fallback.
- [x] **Reminder sheet: the full-screen line never refreshes.** `canFullScreen` in `EditHabitScreen.kt` is a plain `remember`, unlike `exactAlarms` (re-read on resume), so after allowing it the sheet still says "Allow full-screen alarms in settings". Fixed by the setup sheet.
- [x] **Only one missing permission is shown at a time** (exact alarms first, full screen after), and notifications are asked only on "Set reminder". Fixed by the setup sheet.

## Testing (on a real phone)
- [ ] **Alarm reminders (Android 14+, Play build):** ring full screen with the phone locked (slide = Done), silenced by the power button in a pocket, and with Do Not Disturb on (the alarm channel bypasses it). After the full-screen intent declaration in Play Console, check Play kept `USE_FULL_SCREEN_INTENT` (Settings → Apps → Weft → Full screen notifications). The emulator covers every other case (smoke `alarm_*`).
- [ ] **Google Drive on the release build:** install `weft-v1.6.0-github.apk` (or the Play build), Connect Google Drive (tick Google Drive), Back up now, Restore. The minified Drive path hasn't run on the emulator since it lost its connection to Google (2026-11-02).
- [ ] **Google Drive edge cases:** Switch account with two accounts; revoke Weft in the Google account, then Back up now (it should ask again).
- [ ] **In-App Updates** from a Play install with a lower versionCode (internal testing or internal app sharing): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
