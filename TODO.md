# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Rename Sprout → Weft (2026-11-01)

Done in the repo: the app (name, strings in all 7 languages, icon and in-app mark from `weft-icon.zip`, class names, export file names, `weft://` links), `CLAUDE.md`, README, PRIVACY (now dated 1 November 2026), CONTRIBUTING, AGENTS, CHANGELOG (1.5.0), `docs/`, `release/`, the skills, the issue templates, `tools/verify/` (plus a check that `sprout://` still opens), the release workflow (APKs now `weft-<tag>-github.apk` / `-play.apk`, title "Weft <tag>"), fastlane (title, text, icon, feature graphic, 1.5.0 changelog), and the design PNGs that show the logo (see `DESIGN-CHANGES.md`). Every GitHub link points to `nisabmohd/weft` (the repo was renamed on 2026-11-01).

**Kept on purpose, don't rename:** these let Sprout installs update to Weft and keep their data.
- Package / applicationId `app.sprout.habits` and the `app/sprout/habits` source folders. A new package would be a new app on Play, GitHub/Obtainium and F-Droid, with users stranded on Sprout. Renamed apps commonly keep theirs (X is `com.twitter.android`, Signal is `org.thoughtcrime.securesms`).
- `sprout.db`, the backup file's `"app":"sprout"` tag (old and new versions read each other's files), the Drive backup names `sprout-backup-*`, `sprout://` links (still accepted next to `weft://`), the `app.sprout.habits.action.*` intent actions.
- `SPROUT_KEYSTORE_*` env vars and GitHub secrets, `keystore/sprout-release.jks` and its alias: renaming only risks a broken release build.

**Needs the owner**
- [x] **GitHub repo:** renamed to `nisabmohd/weft` (2026-11-01); every link in the app and docs points there. GitHub redirects the old web, git and API URLs, so Sprout installs keep finding updates; never create a new repo called `sprout-habit-tracker` (that breaks the redirect). The local remote now uses the new URL. Still to do: the repo's description, topics and social preview image.
- [ ] **Play Console:** Main store listing: title "Weft: Habit Tracker", short and full description (`release/README.md`), icon `release/icon-512.png`, feature graphic `release/feature-graphic-1024x500.png`, the 8 phone screenshots in `release/phone-screenshots/` (all new: Blue app screens on a flat navy page, redrawn 2026-11-01). App content → Privacy policy: `https://github.com/nisabmohd/weft/blob/main/PRIVACY.md` (after pushing). Store settings → Website. Don't unpublish or create a new app: the listing keeps its installs and ratings. Google may review the name and icon change like a new submission.
- [ ] **Google Cloud:** OAuth consent screen: the app name "Weft", logo and links (users see "Sprout wants to access…" until then). The Android OAuth clients don't change.
- [ ] **Obtainium / IzzyOnDroid / F-Droid** (where listed): the source URL follows the GitHub redirect; update the name in their metadata if they keep one.
- [ ] **GitHub Sponsors profile, social posts, the contact email signature**: anything that says Sprout.
- [ ] **Tell existing users:** say "Sprout is now Weft" in the 1.5.0 GitHub release notes and Play "What's new" (Obtainium keeps showing the name "Sprout" until the user edits the app there). For a release or two, consider adding "Formerly Sprout." to the end of the Play full description (`release/README.md`, `fastlane/.../full_description.txt`) so people who search for Sprout still find it.
- [ ] **Play Console, other languages:** if the listing has translations added in the Console (fastlane only has en-US), change "Sprout" in each title and description too.
- [ ] **GitHub release for 1.5.0:** the workflow now names the APKs `weft-<tag>-github.apk` / `-play.apk` and titles the release "Weft <tag>". Check the update checker in a Sprout 1.4.0 install still finds it after the repo rename (it follows the API redirect), before announcing.
- [ ] **Keep the raw store shots next time:** the raw 1080 × 2400 shots used for 1.5.0 weren't kept, so the plain store style (2026-11-01) was drawn around the screens already in the shots (`store_shots.py --reframe`). For the next retake, capture new raw shots (Blue accent, see `release/README.md`), keep them outside `build/` (e.g. `~/Downloads/weft-raw-shots/`), and run `tools/store_shots.py <folder>` and `tools/feature_graphic.py <folder>`.
- [ ] **Recapture the edited design PNGs:** `welcome`, `about`, `more`, `support-prompt`, `update-dialog`, `notifications` and `app-icon` in `design/` were edited in place for the new mark and name, not captured from the app; recapture them from 1.5.0 on the emulator (Amber, sample data, Sunday 27 Sep, 9:30) once Burrow frees it.
- [ ] Optional: rename the local folder `~/Downloads/sprout` → `~/Downloads/weft` (move `~/.claude/projects/-Users-nisab-mohd-Downloads-sprout/` along with it, it holds Claude's notes for this project).

## Google branding verification (needs the owner)

The consent screen says "Your branding is not being shown to users": Google shows "Weft: Habit Tracker" without the logo or links until the branding is verified. Data access needs no verification (`drive.appdata` and `email` aren't sensitive). Step by step in `~/Downloads/weft-google-branding.md`.

- [ ] Push, then **Settings → Pages → Source: GitHub Actions**; check https://nisabmohd.github.io/weft/ and https://nisabmohd.github.io/weft/privacy/.
- [ ] **Search Console:** add the URL-prefix property `https://nisabmohd.github.io/` and verify it with the HTML file in the `nisabmohd.github.io` repo (the user site), signed in as an Owner of the Cloud project.
- [ ] **Branding:** home page `https://nisabmohd.github.io/weft/`, privacy policy `https://nisabmohd.github.io/weft/privacy/`, authorized domain `nisabmohd.github.io`, logo `release/oauth-logo-120.png`, then **Verify branding** (a few business days).
- [ ] Play Console → App content → Privacy policy: `https://nisabmohd.github.io/weft/privacy/`.

## Before releasing 1.5.0 (needs the owner)

Everything below needs an account, a real phone or a decision; the code, docs and screenshots for 1.5.0 are done (see `CHANGELOG.md`).

- [ ] **Google Cloud (Drive backup):** OAuth clients of type **Android** for `app.sprout.habits`: one with the SHA-1 of Play's app-signing key (Play Console → Test and release → App integrity), one with the release key's SHA-1 (the github APK), and one with the debug key's for local testing. Turn on the **Google Drive API**. The consent screen asks for `drive.appdata` and `email`, neither restricted. Without the client every Drive action says "Couldn't reach Google Drive".
- [ ] **Test Drive on a real phone** (play or github build with the OAuth client): Connect; Back up now; clear app data, Connect again, Restore and compare counts; back up 11 times and check 10 are listed; Switch account; Disconnect; Delete backups from Drive; airplane mode; mobile data with Wi-Fi only on; the nightly job (`adb shell cmd jobscheduler run -f app.sprout.habits 2000003101` while charging); revoke Weft in the Google account, then Back up now (should ask again).
- [ ] **Test In-App Updates** from a Play install (internal testing or internal app sharing with a lower versionCode): About → Check for updates, and the daily prompt on launch; back out once and check that version isn't offered again.
- [ ] **Test In-App Review** from the support prompt on a Play install (Play shows it a few times at most; the store page is the fallback).
- [ ] **Run the smoke suite** on a test emulator: `python3 tools/verify/smoke.py` (new groups: `widget`, `launcher`, `drive`, `link`; the widget ones reset the launcher's home screen, the midnight one needs `adb root`). None of the 1.5.0 checks has had a full passing run.
- [ ] **Play Console "What's new"** for 1.5.0: paste from `CHANGELOG.md` (the play build's entries; `fastlane/.../changelogs/22.txt` is the F-Droid text).
- [ ] **Delete old branches** once 1.5.0 is out: `drive-backup`, `play-services`, `play-services-before-rebase` (1.5.0 replaces them).
- [ ] **Commit** the 1.5.0 work (left uncommitted on purpose) and tag it.
