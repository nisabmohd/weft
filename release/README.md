# Play Store release assets

Graphics and listing text for the Play Store.

## Graphics

| Asset | Play requirement | File | Status |
| --- | --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, up to 1 MB. Full square, Play applies the rounded mask | `icon-512.png` (source `icon.svg`, written by `tools/weft_mark.py`: the yarn-ball mark in white on green #2A6F52, 284px and exactly centred; render with headless Chrome) | Ready |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG, no transparency | `feature-graphic-1024x500.png` | Ready |
| Phone screenshots | 2–8 images, PNG or JPEG, 16:9 or 9:16, each side 320–3840 px, long side at most 2× the short side. At least 4 at 1080 px or wider to be eligible for promotion | `phone-screenshots/01–08` (1080 × 1920) | Ready |
| 7-inch tablet screenshots | Optional (needed only to show as tablet-ready) | – | Not made |
| 10-inch tablet screenshots | Optional | – | Not made |
| Promo video | Optional YouTube link | – | Not made |

The raw emulator screenshots are 1080 × 2400 (20:9). Play rejects those because the long side is more than twice the short side, so the store shots put each screenshot in a 9:16 frame with a caption.

How they were taken: Pixel-size emulator (API 36), gesture navigation, status bar demo mode (9:30, full battery, Wi-Fi only), date set to Sunday 27 September 2026, fresh install with the sample habits (Get started → Skip for now → Go to Today, then one change and its Undo so the sample banner goes), Dynamic color off, **Blue** accent (the logo's colour; the app's default is Amber, and the design captures in `design/` use Amber), Lexend font. Widgets follow the system colours, so they use the wallpaper's colours rather than the in-app accent.

**2026-11-09 (1.6.2): all eight shots retaken in Rust**, the app's new default accent: the app in its default Rust (Dynamic color off), the page #733626, pale pink captions. The feature graphic stays the logo's green (user: it goes with the logo). Raw shots: `python3 tools/verify/capture_docs.py store <dir>`.

The store shots are made by `python3 tools/store_shots.py <folder of raw shots>`, which writes all eight files here (copy them to `fastlane/metadata/android/en-US/images/phoneScreenshots/1–8.png`). Style (2026-11-01, colours 2026-11-08): a rose-brown (#77453F) flat page, a two-line pale pink (#FFDAD4) caption in Lexend SemiBold, and a plain phone with a thin rim; the feature graphic shows the green logo tile. Kept plain on purpose: the owner found the earlier flat set childish, and the tries with lifted-out cards, woven-thread patterns, a glow, gold text and small labels looked overdone and AI-made. The status bar is redrawn a little lower and pulled in from the sides, so the frame's rounded corners never cut the clock or the battery. Shot 7 is the home screen with the This week widget. History: retaken on 2026-10-19 from 1.3.7; 1, 3, 5, 6 and 8 rebuilt on 2026-10-29 (1.4.0) for the nav bar fix; all eight retaken from 1.5.0 in Blue on 2026-11-01, then redrawn in the style above the same day with `--reframe` (no raw shots were kept); all eight and the feature graphic retaken from 1.5.1 in Blue on 2026-11-02 (new Solar icons, the 1.5.1 palette); 1–6, 8 and the feature graphic retaken again the same day after the old look came back (shot 7 kept). For Blue from the first screen on a fresh install, write `accentHue` = 215 into `files/datastore/settings.preferences_pb` with `run-as` before the first launch (debug build); the emulator date is set with `adb shell cmd alarm set-time <ms>` after `settings put global auto_time 0`. `python3 tools/store_shots.py --reframe` redraws the page around the screens already in these files.

The feature graphic is made by `python3 tools/feature_graphic.py <folder of raw shots>` from the same `today.png`, or `--reframe` (from `01-today.png` here): the same flat navy, the white Weft tile and name with "Habit tracker and journal" under it on the left, and one upright Today phone running off the bottom on the right. Nothing else (a slogan, extra lines and a second phone were tried on 2026-11-01 and looked cluttered). It writes both copies (here and fastlane). The store icon is `icon.svg` rendered with headless Chrome.

**2026-11-11 (1.6.2): the alarm is shot 2** (user: highlight the alarm feature). Play takes 8 phone shots at most, so habit detail made room for it. All eight retaken from the final 1.6.2 build.

| # | Screen | Title / subtitle |
| --- | --- | --- |
| 1 | Today | Check off with a swipe / Swipe, tap or hold. Every habit on one calm list. |
| 2 | Alarm ringing | Alarms that really ring / Full screen, even on a locked phone. |
| 3 | Amount sheet | Log part of a habit / 15 of 20 pages still counts. |
| 4 | Habits → Week | See your week at a glance / Every habit, every day, in one look. |
| 5 | Journal | Write a note for any day / Say how it went, or what got in the way. |
| 6 | Insights | See what's working / Your best day, and the habit that needs focus. |
| 7 | Home screen widgets | Home screen widgets / Today, your week and your streak at a glance. |
| 8 | Today, dark | Private. Yours. / No ads, no tracking. Free and open source. |

## What's new (Play Console, 500 characters max)

1.5.0 (versionCode 23), 457 characters:

```
Sprout is now Weft, with a new icon. Same app: your habits, notes and settings stay.
• Back up to your own Google Drive, every night or when you tap Back up now, and restore any of your last 10 backups.
• Backup & restore has its own screen in More.
• Long-press the icon for Add note and New habit.
• Updates and ratings go through Google Play.
• Widgets move to the new day at midnight and follow light and dark mode.
• Fixes for the time picker and more.
```

## Listing text (draft)

**App name** (30 chars max): `Weft: Habit Tracker`

**Short description** (80 chars max):

> Track habits, log part of a day, and keep a journal. Free, open source, no ads.

**Full description** (4000 chars max):

> Weft is a small habit tracker that keeps your habits and notes on your phone.
>
> Each day, every habit is Done, Partial or Skipped. Swipe right to mark it done, swipe left to skip it, or press and hold to log an amount, like 15 of 20 pages or 30 of 45 minutes. Every change can be undone.
>
> • Today: your score for the day and a card for each habit
> • Habits: the week at a glance, or the last 26 weeks as a heatmap, with streaks
> • Habit detail: a month calendar where you can fix any past day
> • Journal: short notes tied to a habit and a day, filtered by habit or date
> • Insights: your score, habits per day and each habit's score for any date range
> • Reminders at the time you choose, and an evening nudge to add a note when you skip
> • Four home-screen widgets: This week, Today, Today strip and Streak
> • Export and import a JSON backup, or export CSV for spreadsheets
> • Light and dark theme, dynamic color or an accent color, five fonts and a text size
> • English, Hindi, Spanish, German, French, Portuguese (Brazil) and Japanese
>
> Skipped days are left out of your score, so one bad day doesn't sink the week.
>
> Weft is free and open source under the GNU GPL v3. No ads, no tracking, and it works without an account.

**Category:** Productivity. **Tags:** Habit tracker, Journal.

## Play Console

The privacy policy is [`PRIVACY.md`](../PRIVACY.md) at the repo root.
