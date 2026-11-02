# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Testing
- [ ] **Drive on the release build, on a real phone:** install `weft-v1.5.0-github.apk` (or the Play internal testing build), Connect Google Drive (tick Google Drive), Back up now, Restore. The emulator lost its connection to Google on 2026-11-02 ("Network error" for the debug build too), so the minified Drive path wasn't run there.
- [ ] **Test Drive on a real phone** (the emulator run on 2026-11-01 passed Connect, Back up now, Restore, keep 10, frequency and the nightly job, offline, Wi-Fi only, Disconnect, Delete backups and the unticked-Drive case): still to do Switch account with two accounts, and revoking Weft in the Google account then Back up now (should ask again).
- [ ] **Test In-App Updates** from a Play install (internal testing or internal app sharing with a lower versionCode): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **Test In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
