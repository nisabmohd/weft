# Play Store release assets

Graphics and listing text for the Play Store.

## Graphics

| Asset | Play requirement | File | Status |
| --- | --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, up to 1 MB. Full square, Play applies the rounded mask | `icon-512.png` (source `icon.svg`: the Tabler sock on navy #2E4A7A, centred optically like the launcher icon; render with headless Chrome) | Ready |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG, no transparency | `feature-graphic-1024x500.png` | Ready |
| Phone screenshots | 2–8 images, PNG or JPEG, 16:9 or 9:16, each side 320–3840 px, long side at most 2× the short side. At least 4 at 1080 px or wider to be eligible for promotion | `phone-screenshots/01–08` (1080 × 1920) | Ready |
| 7-inch tablet screenshots | Optional (needed only to show as tablet-ready) | – | Not made |
| 10-inch tablet screenshots | Optional | – | Not made |
| Promo video | Optional YouTube link | – | Not made |

The raw emulator screenshots are 1080 × 2400 (20:9). Play rejects those because the long side is more than twice the short side, so the store shots put each screenshot in a 9:16 frame with a caption.

How they were taken: Pixel-size emulator (API 36), gesture navigation, status bar demo mode (9:30, full battery, Wi-Fi only), date set to Sunday 27 September 2026, fresh install with the sample habits (Get started → Skip for now → Go to Today, then one change and its Undo so the sample banner goes), Dynamic color off, **Blue** accent picked in More (the logo's colour; the app's default stays Amber), Lexend font. Widgets follow the system colours, so they use the wallpaper's colours rather than the in-app accent.

The store shots are made by `python3 tools/store_shots.py <folder of raw shots>`, which writes all eight files here (copy them to `fastlane/metadata/android/en-US/images/phoneScreenshots/1–8.png`). Style (2026-11-01): the logo's navy (#2E4A7A) as a flat page, a two-line white caption in Lexend SemiBold, and a plain phone with a thin rim. Kept plain on purpose: the owner found the earlier flat set childish, and the tries with lifted-out cards, woven-thread patterns, a glow, gold text and small labels looked overdone and AI-made. The status bar is redrawn a little lower and pulled in from the sides, so the frame's rounded corners never cut the clock or the battery. Shot 7 is the home screen with the This week widget. History: retaken on 2026-10-19 from 1.3.7; 1, 3, 5, 6 and 8 rebuilt on 2026-10-29 (1.4.0) for the nav bar fix; all eight retaken from 1.5.0 in Blue on 2026-11-01, then redrawn in the style above the same day with `--reframe` (no raw shots were kept). `python3 tools/store_shots.py --reframe` redraws the page around the screens already in these files.

The feature graphic is made by `python3 tools/feature_graphic.py <folder of raw shots>` from the same `today.png`, or `--reframe` (from `01-today.png` here): the same flat navy, the white Weft tile and name with "Habit tracker and journal" under it on the left, and one upright Today phone running off the bottom on the right. Nothing else (a slogan, extra lines and a second phone were tried on 2026-11-01 and looked cluttered). It writes both copies (here and fastlane). The store icon is `icon.svg` rendered with headless Chrome.

| # | Screen | Caption |
| --- | --- | --- |
| 1 | Today | Check off habits / with a swipe |
| 2 | Amount sheet | Log part of a habit, / like 15 of 20 pages |
| 3 | Habits → Week | See your week / at a glance |
| 4 | Habit detail | Streaks and a calendar / for every habit |
| 5 | Journal | Write a note / for any day |
| 6 | Insights | See which habits / are working |
| 7 | Home screen widgets | Widgets for your / home screen |
| 8 | Today, dark | Light and dark themes, / your font and colors |

## What's new (Play Console, 500 characters max)

1.5.0 (versionCode 22), 457 characters:

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
