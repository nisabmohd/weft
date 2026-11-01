# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Rename Sprout → Weft (2026-11-01)

Done in the repo: the app (name, strings in all 7 languages, icon and in-app mark from `weft-icon.zip`, class names, export file names, `weft://` links), `CLAUDE.md`, README, PRIVACY (now dated 1 November 2026), CONTRIBUTING, AGENTS, CHANGELOG (1.5.0), `docs/`, `release/`, the skills, the issue templates, `tools/verify/` (plus a check that `sprout://` still opens), the release workflow (APKs now `weft-<tag>-github.apk` / `-play.apk`, title "Weft <tag>"), fastlane (title, text, icon, feature graphic, 1.5.0 changelog), and the design PNGs that show the logo (see `DESIGN-CHANGES.md`). Every GitHub link points to `nisabmohd/weft` (the repo was renamed on 2026-11-01).

The names kept on purpose (`app.sprout.habits`, `sprout.db` and the rest) are listed in `AGENTS.md` → Names.

## Before releasing 1.5.0 (needs the owner)

Built and tested on 2026-11-02: smoke suite (all 64 checks pass, on `Sprout_API_35` for widgets), 109 unit tests per flavor, 14 component tests, and the minified github build on the emulator. The signed files are in `build/dist/v1.5.0/` (copy in `~/Downloads/weft-v1.5.0-release/`).

- [ ] **Drive on the release build, on a real phone:** install `weft-v1.5.0-github.apk` (or the Play internal testing build), Connect Google Drive (tick Google Drive), Back up now, Restore. The emulator lost its connection to Google on 2026-11-02 ("Network error" for the debug build too), so the minified Drive path wasn't run there.
- [ ] **Play Console:** upload `weft-v1.5.0-play.aab` to internal testing, check Drive and updates, then production; "What's new" is in `release/README.md`.
- [ ] **GitHub release:** push the `v1.5.0` tag; the Release workflow needs the `SPROUT_KEYSTORE_*` secrets (it failed for 1.4.0 without them). Or create the release by hand and attach `weft-v1.5.0-github.apk`, `weft-v1.5.0-foss.apk` and `SHA256SUMS.txt` with those names (the update check needs the `-github.apk` ending and the checksum file).
- [ ] **Test Drive on a real phone** (the emulator run on 2026-11-01 passed Connect, Back up now, Restore, keep 10, frequency and the nightly job, offline, Wi-Fi only, Disconnect, Delete backups and the unticked-Drive case): still to do Switch account with two accounts, and revoking Weft in the Google account then Back up now (should ask again).
- [ ] **Test In-App Updates** from a Play install (internal testing or internal app sharing with a lower versionCode): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **Test In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
