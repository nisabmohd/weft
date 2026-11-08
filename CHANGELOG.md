# Changelog

## 1.6.1

Fixed:

- Widgets stayed on the Weft logo and never showed anything in release builds: the build tool removed a piece WorkManager needs to start the widget's drawing job. Kept now (`app/proguard-rules.pro`).
- Alarms could stay silent on some phones (ColorOS, newer Android): the sound came from the notification, which the phone muted. Weft now plays the alarm sound and vibration itself until you answer it.
- Alarms at the same time while the phone was in use came as separate cards, and their buttons were cut off ("Snooz…") and hard to read. They are one card now, "6:45 AM · 1 of 2 · Next: Workout", with a Done pill and Snooze 10 min / Dismiss buttons (like Burrow's); answering one shows the next without ringing again.
- Habit details: Edit uses the pen (like Edit note), and Archive / Restore a box with a lid and an arrow, instead of two look-alike rounded squares.

## 1.6.0

Added:

- Alarm reminders: a habit's reminder can be a quiet notification or an alarm that rings full screen, even over the lock screen, until you answer it. Slide to mark it done, Log amount (measured habits, the app's amount sheet right on the alarm) or Skip today, Snooze 10 min, or Dismiss; Open Weft unlocks and opens the habit; the power or a volume button silences it. Alarms at the same time ring as one, one habit after another ("1 of 2", "Next: Workout"); logging the habit anywhere, even part of it, stops its alarm. It uses the alarm sound on the alarm stream with vibration, stops by itself after 10 minutes with a "missed at" notification, doesn't ring when the habit is already done or skipped, and shows in the status bar like any alarm clock. Without full-screen permission (Android 14+) it rings as a notification with Done / Snooze / Dismiss. New habit → Reminder opens one sheet for the time and the type. Backups keep the type.
- Buttons on reminder notifications: Done and Dismiss; measured habits also get +step (e.g. "+5 pages") and the reminder updates to show what's left; the evening note nudge gets Add note (opens New note for that habit and day) and Done. Today offers Undo for the change the next time it opens.

Changed:

- No more dialogs: Update available, Enjoying Weft?, Delete habit, Replace your data, Delete backups and Delete note are sheets now. Two actions are always two equal buttons (Cancel | Save, Later | Update); action lists (a note's Edit / Share / Delete, Google account, the support prompt) are grouped rows with icon tiles.
- The amount and edit-day sheets: the note field is filled like New note's and asks the question for the outcome ("What stopped you from finishing?"); Cancel | Save at the bottom.
- Insights → Patterns: each card has an icon tile; Needs focus shows a falling graph in the habit's colours and a drop in red.
- Sheets never close by accident: a scroll or fling inside a sheet stops at the top instead of dragging the sheet down.
- A new Weft logo: a ball of yarn, white on green, exactly centred on the launcher icon (with a themed-icon layer), the Play Store icon, the website, notifications and in the app (Welcome, About, More, the widgets' loading screen, which follow your colours). Android 8–11 use its green for the launch screen and widgets. Icons for Delete, Edit note, Read, Vitamins and Stay calm follow the new design.
- "Enjoying Weft?" can show once a week (it was every 60 days); four "Maybe later"s stop it for good, and anyone who rates, stars or sponsors isn't asked again for six months.

Internal:

- Shared sheet pieces in `ui/components/`: `SheetHeader`, `WeftButton` / `ButtonPair`, `PromptSheet`, `ActionGroup` / `ActionRow`, `NoteField`, with their spacing as named constants (`docs/SPACING.md` → Sheets). Room 5 (`habit.reminderType`). `tools/verify/capture.py` recaptures design PNGs; the smoke suite covers notification buttons and every alarm case.
- Alarm queue, Log amount, Skip today and Open Weft on the ringing screen (design `alarm-4` … `alarm-8`); notification reminders say Dismiss instead of Snooze. The This week widget's rows fill its height and the Streak widget centres its number. Store shots in the same layout as Burrow's; the feature graphic on the logo green.

## 1.5.2

Fixed:

- Progress strip widget: "Next: …" ran past the right edge on smaller phones and wide launcher fonts, with no gap after "1 of 14 today". The habit name is now cut with "…" to the space left after the count, with a 12dp gap.
- Widgets: with nothing left but something skipped today, the strip said "All done" and the Today widget "All done for today" (e.g. "3 of 5 today · All done"). They now say "All logged" / "All logged for today"; "All done" only when every habit is Done.

Internal:

- Android Gradle plugin 9.1.0 (Play Console asked for AGP 9+ for R8 optimization; 9.1.0 is the newest that Android Studio 2025.3.2 syncs), Gradle 9.8.2, Kotlin 2.4.21, KSP 2.3.13, benchmark / baseline profile 1.5.0. AGP 9 compiles Kotlin itself, so the `org.jetbrains.kotlin.android` plugin is gone and the flavor folders (`src/drive`, `src/noDrive`, `src/noPlay`) are added to the `kotlin` source sets.

## 1.5.1

Changed:

- New icons everywhere (Solar, outline style, the same set as Burrow): the tab bar, buttons, habit icons and widgets. Every habit icon now shows what it says: Teeth is a tooth, Cricket a bat and ball, Stairs a staircase, Stretch a stretching figure, No alcohol a crossed-out glass, No smoking a crossed-out cigarette, Less phone and Less TV are crossed out, Fasting an hourglass, Self care, Outdoors, Morning, Art, Puzzles, Language, Clean, Cook, Plants, Garden and Elders have their own icons, and no two icons in the picker are the same. Open-source licences lists Solar icons by 480 Design (CC BY 4.0).
- Richer accent colours (buttons, highlights and the welcome panel are brighter in dark mode and deeper in light mode). Screens look as they did in 1.5.0: same cards, page and tiles.
- Accent colours: Rose, Teal, Blue, Purple, Plum, Rust and Amber. Rose takes Green's place (a Green install switches to Rose); Amber stays the default.
- The launch screen and the widgets' loading screen follow the phone's colours (Android 12 and later) instead of a fixed green tint, so they no longer flash a different colour before Weft opens. Below Android 12 they, the widgets and the Add note / New habit shortcuts use the blue of Weft's icon.

Fixed:

- Update available (GitHub version): long What's new lines were cut off with "…" and the dialog didn't scroll. The notes now show in full and scroll, and Update / Later stay at the bottom.

Internal:

- `tools/solar_icon.py` can draw an icon Solar doesn't have in its style (`custom:`) and cross one out (`slash:`).

## 1.5.0

Added:

- Sprout is now called Weft, with a new icon. It's the same app: it updates in place and keeps your habits, notes and backups. Backups made by Sprout still import, and `sprout://` links still work.
- Backup has its own screen: More → Backups opens Backup & restore, with the state of your last backup at the top and one button for what to do next.
- Google Drive backup (Google Play and GitHub versions). On Backup & restore, Connect Google Drive (it backs up straight away), then Back up now, choose how often it backs up by itself (every night while charging, weekly, monthly or off), keep it to Wi-Fi, and restore any of your last 10 backups (each shows when, from which phone, how many habits and its size). Google account lets you switch account, disconnect, or delete the backups from Drive. Backups are the same data as Export to file, kept in a hidden folder in your own Drive that only Weft can see. Weft says so when you're offline, on mobile data with Wi-Fi only on, or your Drive is full, and the screen shows when a backup failed and why, with Try again. The F-Droid version has no Google code: its Backup & restore screen shows when you last exported, with Export now and a choice of JSON or CSV.
- Long-press Weft's icon for two shortcuts: Add note and New habit.
- `weft://` links open a screen: `weft://today`, `habits`, `journal`, `insights`, `more`, `note/new`, `habit/new` and `habit/<id>`.
- Google Play version: About → Check for updates asks Google Play, and installs a new version with Play's own update screen. Weft also checks once a day on its own, and doesn't offer a version again after you back out of it.
- Google Play version: "Rate on Play Store" in the "Enjoying Weft?" prompt shows Google Play's rating card inside the app, or the store page if Play can't show it.

Changed:

- The new Weft mark (a sock, from Tabler Icons) is on the launcher icon, reminder and note notifications, the widgets' loading screen, Welcome, About, More, the update dialog and the "Enjoying Weft?" prompt, and the app name is Weft in all 7 languages. Open-source licences lists Tabler Icons (MIT).
- Exports are named `weft-backup-<date>.json` and `weft-<date>.csv`.
- About has a Privacy policy link (https://nisabmohd.com/weft/privacy).

Fixed:

- Disconnect (Google Drive) took a few seconds to show; the screen now changes at once while Google removes the access in the background.
- After Delete backups from Drive, Backup & restore still said "Backed up today"; it now says "No backups yet".
- Google's Drive permission screen lets you untick Google Drive (it starts unticked). Weft then said it was connected and every backup failed with "Couldn't reach Google Drive"; it now stays disconnected and says "Weft needs Google Drive to keep your backups. Connect again and tick Google Drive".
- The time wheel (Reminder time, Default reminder) closed its sheet when a wheel was scrolled past its first or last row, or dragged between the columns. Drags on the wheels now only move the wheels; drag the sheet's top to close it.
- Connect Google Drive showed nothing when Google's sign-in screen failed (for example when the build isn't registered with Google); it now says "Couldn't reach Google Drive. Try again". Backing out of Google's screen still says nothing.
- Widgets moved to the new day at midnight only once you logged something; until then they showed yesterday, like "7/7, All done". They now update just after midnight, and after a clock or time zone change.
- The This week widget showed a week across two months as "26 – 1 Nov"; it now says "26 Oct – 1 Nov", like the Habits tab.
- Changing a day more than a week back (from a habit's calendar) didn't update the Streak widget.
- Widgets kept their old colours after switching the phone between light and dark, and their old language after changing the language (in Weft or in the phone's settings).
- Widgets couldn't be made smaller than the size they were added at, and could be stretched across the whole screen. Each can now shrink (Today and the strip to 2 × 1, This week to 3 × 2, Streak to 1 × 1) and has a largest size, and its layout adapts: a smaller ring, fewer rows, "Next: …" on one line, icons instead of names, or just the number.
- A widget that was already on the home screen kept its old language after Weft's language was changed (until the phone restarted Weft), and kept showing the new day after the clock was set back. Widgets now redraw their text and reload their day on every language, dark mode, clock and time zone change.
- Tapping the Today widget's ring (or the strip, or This week's header) brought Weft up on whatever tab was open last; it opens Today again.
- On a day with nothing scheduled, the Today widgets said "All done for today"; they now say "Nothing due today".

Internal:

- Google Drive backup runs on Android's JobScheduler (no new library for scheduling); the backup times are unit-tested (`BackupScheduleTest`).
- Widgets, notifications, shortcuts and links share one way of opening a screen (`AppLink`); a link to a deleted habit opens nothing.
- Smoke checks for Backup & restore and Drive backup (each state, the failed card, the frequency sheet and its scheduled job, the account sheet, offline, no Google, no Drive code in the F-Droid APK), the shortcuts and the links.
- The rename keeps every stored name so Sprout installs update in place: the package `app.sprout.habits`, `sprout.db`, the backup file's `"app":"sprout"` tag, Drive's `sprout-backup-*` files, the `app.sprout.habits.action.*` actions and the `SPROUT_KEYSTORE_*` variables. Class names say Weft (`WeftApp`, `WeftDatabase`, `WeftType`, `WeftTheme`, `WeftSheet`, `WeftNavHost`); the Room schema folder moved with `WeftDatabase`. Release APKs are `weft-<tag>-github.apk` / `-play.apk`.
- A website at https://nisabmohd.github.io/weft/ (home page and privacy policy, for Google's OAuth consent screen and the store listings), built from `site/` and `PRIVACY.md` by `tools/build_site.py` and published by `.github/workflows/pages.yml`. The privacy policy adds the Google API Services User Data Policy (Limited Use) statement.
- New store graphics: the 8 phone screenshots and the feature graphic were redrawn plainly: the logo's navy as a flat page, a two-line white caption and a plain phone (`tools/store_shots.py`, `tools/feature_graphic.py`). Same app screens as before (Blue accent).
- Smoke checks for the widgets: each of the four on the home screen, following every change made in the app within 4 seconds (Done, Undo, Skip, archive and restore), the widget's circle updating the app, taps after Android stopped the app, opening a habit from the widget, the midnight change, and the launcher's long-press widget list.

## 1.4.0

Added:

- Writing suggestions in New note. Before you type: "Quick start" phrases that fit the day (Skipped, Partial or Done), led by the openers you use most, and your recent notes for that habit. While you type: the rest of a sentence you wrote before, in grey after the cursor, plus "Finish with" endings and "Similar notes". Tap one to use it. Everything is worked out on your phone from a fixed list of phrases and your own notes; nothing is sent anywhere.
- More → Journal: turn writing suggestions off, or clear what they learned from your notes (the notes stay).
- The note's placeholder asks a question that fits the day, like "What got in the way?" after a skip.
- Get started's practice card now shows the Undo bar after every change, like Today, with a new "Undo" step that's ticked once you use it.

Changed:

- The Journal's empty state uses the note icon from the rest of the app, on an accent tile.

Fixed:

- The bottom tab bar's first and last tabs sat almost on the screen edge; they now line up with the rest of the screen.

## 1.3.8

Changed:

- Get started is now a short setup, like Burrow's: a page that shows Sprout in use, then pick your habits (five suggestions, or add your own), then try logging on a practice card: swipe right for Done, left for Skip, tap the circle, and press and hold to log part of it (Partial) or add a note, which shows how it will look in the Journal. The habits you pick replace the sample habits; "Skip for now" keeps the sample habits as before.
- Reminder time and Default reminder use a new time wheel (hour, minute in 5s, AM/PM), the same as Burrow's. It replaces the hour and minute boxes with arrows.
- About shows the developer, mid apps, with a contact address: hello.midapps@gmail.com.

Fixed:

- Play Console: "Your app uses deprecated APIs or parameters for edge-to-edge". Sprout no longer uses androidx.activity's enableEdgeToEdge(), whose code for Android 8–14 sets the bar colours with APIs deprecated on Android 15. It sets up edge-to-edge itself, with no deprecated calls.
- Play Console: "Improve your app's memory and performance with R8 optimization". R8 now shrinks resources together with the code (`android.r8.optimizedResourceShrinking`).

Internal:

- One habit tile and one progress ring for every screen (`HabitTile`, `ProgressRing`), replacing nine hand-built copies of the tile and three of the ring. The Get started ring now has round ends like Today's.
- Tests: unit tests for the time wheel, the shared components and Get started's rules (22 new), component tests on an emulator for the time wheel, the tile and ring, and both Get started steps (13), and smoke checks for Get started, the time wheel and About's contact row.

## 1.3.7

Changed:

- The font you pick in More → Font now applies to titles too (screen and sheet titles, big numbers). They were always Outfit before. Home-screen widgets keep the phone's font; Android doesn't allow app fonts there.

## 1.3.6

Fixed:

- A reminder that the phone delivered late, after midnight (battery saving can hold alarms back), reminded you about a habit already marked Done or Skip. A late reminder is now dropped; the next one comes on time.

Changed:

- More → Font is now one row that opens a Font sheet, like Language, with every font written in itself.
- Choose icon: 38 more everyday icons, for people of every age and country (67 in all): shower, self-care, weight, no alcohol, bedtime, mood, breakfast, fasting; swim, hike, football, cricket, tennis or badminton, basketball, stretch, take the stairs, martial arts; study, language, art, photography, instrument, pray, puzzles, listen; to-do, email, laundry, groceries, garden, recycle, budget, less TV. A new People group holds family and friends, call someone, kids, elders, pets and kindness. Search finds them in every app language.
- Add note (Today and Journal): New note starts on the habit you logged last that day (Done, Partial or Skip) that has no note yet. The Journal used to always start on the first habit, and Today on the first skipped habit in list order.
- One grouping rule on every screen, with the Journal as the model. Section headers look the same everywhere: 16sp SemiBold title in the text colour, an optional count or button on the right, 20dp above and 8dp below. Before, More, New habit and habit detail used small grey labels, and Manage habits used a large "Archived" title.
- More: the group headers ("Backup & restore", "Appearance", "General") no longer sit right on top of their group; there is now an 8dp gap below each one.
- More, About, Open-source licences and New habit: rows in a group are separate cards 2dp apart (big corners on the outside, small between), like a day in the Journal. The thin divider lines are gone. Appearance is split into Theme, Dynamic color, Accent, Font and Text size.
- Habit detail: the notes are one group like the Journal (2dp apart, not 16dp), with the "Notes" header 8dp above them.
- Insights → Habit breakdown: the rows have the same 24/6dp corners as every other group (were 28/4dp).
- Code cleanup: 44 unused imports removed. No change to how the app works.

## 1.3.5

Changed:

- Habits → Overall: the legend ("Last 26 weeks", Done / Partial / Skipped) has the same gap above and below it.

Fixed:

- Habit detail: on narrow phones "best streak" wrapped onto two lines and "11 days" lost its "days". The three stat cards now keep their value and label on one line each and stay the same height.
- Deleting a habit from the Edit screen left an empty screen behind; it now goes back to the tab the habit was opened from.
- New note: coming back after the system closed the app in the background reset the form to the habit and day it was opened with (from Today, a skipped habit on today's date) and dropped the text, so the note could be saved on the wrong habit and day. The habit, day and text are now kept.
- First run: tapping Get started quickly could leave the "These are sample habits" banner hidden above the top of Today until you scrolled up.

## 1.3.4

Changed:

- Insights and Journal: the 7 days / 30 days chips are gone; pick other dates with the calendar button. Insights opens on this week, shown as a selected "This week" chip; the Journal opens on all notes and shows a chip only while a range or habit filter is on. A picked range's ✕ goes back to the default.
- Journal: the time a note was written sits in its status line ("Skipped · 9:12 AM", or "written Thu, 11:28 PM" for a note written on a later day), and the day headers are larger.
- Sample habits: the Today card has a plain "Remove sample data" button and goes away once you log or change a habit. The "Remove sample data" row in More is gone.

## 1.3.3

Fixed:

- Journal: a note written on a later day than the one it's about shows when it was written, "Today, 10:51 PM", instead of only the time, which read as written on the note's own day.

## 1.3.2

Changed:

- Space Grotesk is no longer lighter than the other fonts.
- Numbers (the score on Today and Insights, the amount and time pickers, the stats in habit detail) use the font you picked in More instead of always Outfit.
- Haptics: tapping a habit's circle, a swipe crossing the point where it logs, each step of − / + and the slider, Save in the amount sheet, and every switch, chip, segmented button, day and checkbox now give a light tap.

Fixed:

- Insights, Patterns: with the largest text sizes, "Prime time", "Needs focus" and the line under them wrap instead of being cut off.
- Lint errors in the French, Portuguese and Hindi translations that stopped release builds.

## 1.3.1

Changed:

- Insights, Habit breakdown: each habit's score is a ring on the right, like a partly done habit on Today, in place of the colour ribbon. The habits sit together in one group.
- Insights, Habit breakdown: a Score | Streak switch sorts the habits by score or by current streak. Sorted by streak, the ring shows each habit's streak within the chosen dates (a 7-day range shows at most 7 days).
- Insights, Patterns: "Prime time" and "Needs focus" are labels with an icon at the top of each card, and the text below matches the habit rows.
- Insights: the summary card shows just your score and the done and partial counts. The streak and best-week line is gone; each habit's streak is in Habit breakdown.
- Insights, Patterns: Prime time shows just the score ("92% completion") on one line.
- Today: the sample habits banner has a proper card title ("These are sample habits").

## 1.3.0

Fixed:

- The Journal opened on "This week" and hid older notes. It now opens on all notes.
- The app no longer opens on an empty "0% of today" card before the habits appear.

Changed:

- Journal and Insights have range chips under the title: All, 7 days and 30 days on the Journal; This week, 7 days and 30 days on Insights. A range picked from the calendar shows as its own chip.
- When no notes match, the Journal says which filter is hiding them and has a "Show all notes" button.
- Insights has a new layout. The summary card adds your longest streak and the habit having its best week. A "Patterns" card shows your best weekday and the habit that needs focus. "Weekly rhythm" is a row of rounded bars, one per weekday (one per week for ranges over two weeks). "Habit breakdown" lists each habit with its score, its streak and a colour ribbon that is wider and stronger the more consistent the habit was. Tap a bar to see its numbers, and tap a habit to open it.
- Today: the ring on a partly done habit is slimmer, with rounded ends.
- The dot on the calendar and filter buttons is smaller and has no ring.

## 1.2.2

Changed:

- Text is consistent across screens: nine text styles, used the same way everywhere. Section labels ("Today", "By habit", "Notes", "Backup & restore", "Icon", "Days") are small and grey, and the text on the right of a card is always the same size.
- Journal: the day headings are grey, and the note count and note time are a little larger.
- Insights: the line under each habit's name is a little larger.
- Habit detail: the labels under the three numbers are a little larger.
- More: the values on the right ("System default", "Monday") are smaller, and the section labels are grey instead of the accent color.
- Date range sheet: the selected range is regular weight and the weekday letters are smaller.
- Only two text weights are used now, Regular and SemiBold.
- Gaps and sizes are consistent too: cards sit the same distance apart on every tab, every card has the same padding, section labels have the same space above and below, and the habit icon tile is the same size in the filter and habit pickers as on Today.

## 1.2.1

New:

- The Journal opens on this week's notes. The date chip under the title shows what you're looking at; tap it or the calendar button to pick other dates or "All dates".
- Journal notes are grouped by day, with the number of notes next to each date.
- Insights lists each habit with how many days you kept it, its current streak and its score.
- Habits → Overall shows each habit's best streak under its name and the current streak on the right.

Changed:

- Active filters show as chips under the title on Journal and Insights: the dates first, then the habits. The calendar and filter buttons get a small dot while their filter is on.
- The sample habits banner has a round delete button, like the buttons in the headers.
- Habit cards look the same on every screen: the same icon tile as on Today, and plain text on the right.
- The reminder card in New habit has a bell in the habit's color.
- The Figtree font is replaced by Space Grotesk. If you had picked Figtree, the app goes back to Lexend; choose Space Grotesk in More → Appearance.

Fixed:

- The title, its buttons and the filter chips stay at the top of every tab while you scroll. Before, you had to scroll back to the top to change or clear a filter.
- In New note, picking a day that already has a note for that habit now shows that note instead of an empty page. Anything you had typed is added under it.

## 1.2.0

New:

- Journal and Insights show which filters are on: the calendar and filter buttons turn tinted, the filter button shows how many habits are picked, and a chip under the title lists them. Tap ✕ after the dates to clear the range, ✕ on the chip to clear the habits, or the chip to change them.
- Insights has the same card style as Today and Habits: a score card with one bar per habit, a "Habits per day" card with a column for each weekday and the best day highlighted, and one "By habit" card with each habit's icon and score.
- Insights groups ranges longer than two weeks by week ("Habits per week").
- Swipe the Undo message left or right to dismiss it.

Changed:

- The score now counts a past day you never logged as 0. Only a day you marked Skip is left out. Before, one Done in an otherwise empty week showed as 100%.
- A reminder that is still in your notifications goes away once you mark that habit done or skipped.
- New installs start with the amber accent and Dynamic color off; turn it on in More → Appearance. If you already had Sprout, your colors stay as they were.
- The accent colors Gold and Pink are now called Amber and Plum.
- About says "Made by Nisab Mohd & Contributors".

## 1.1.0

New:

- Check for updates (GitHub build): About has a "Check for updates" button, Sprout looks once a day on its own, and "Update" downloads the new version and opens Android's installer. More → About Sprout shows "Update available" when there is one. The F-Droid and Play builds are updated by their stores.
- A simpler time picker for reminders (New habit and More → Default reminder): hour and minute tiles with arrows above and below, and an AM/PM switch. Tap an arrow, hold it to move faster, or drag a number up or down. Phones set to the 24-hour clock get 00–23 and no AM/PM.
- Amount and Duration are now one type, **Measure**: a target in any unit (20 pages, 8000 steps, 45 min, 7 h) and a **Step** that − / + and the slider move by (1 page, 500 steps, 0.25 h). Your Duration habits become Measure habits in min (step 5) or h (step 0.25), with their history converted; old backups import the same way.
- New installs start with sample habits, two weeks of history and notes on several days, so every screen has something to show. A card on Today and More → Remove sample data delete them in one tap; your own habits stay. Updates don't get sample data.
- Press and hold a note in the Journal to edit, share or delete it. Deleting shows Undo.
- The widget picker shows a preview of each widget instead of the Sprout logo.

Changed:

- One header on every tab: the title on top, the date or date range under it, round buttons on the right, all in the same place.
- Round buttons in every screen header: back or close on the left, and on New habit, Edit habit and notes, Save is a round ✓ button. A habit's page has Edit, Archive and Delete buttons, and a note has a Delete button, instead of a ⋮ menu.

Fixed:

- On New habit, the icon next to the name sat higher than the name box.

## 1.0.0

New:

- Sprout speaks Hindi, Spanish, German, French, Portuguese (Brazil) and Japanese. Pick one in More → General → Language, or follow the phone's language. On Android 13 and later it's also in Settings → App languages.
- Dates follow the language too: "Montag, 28. September", "9月28日月曜日". English keeps "Monday, 28 September".
- The More tab has a tune icon, and "Week starts on" uses the same sheet style as Language.

Fixed:

- Scrolling Today could mark a habit done or skipped. A swipe now has to drag the card past half its width.
- Insights' "This week" ran to the end of the week and selected days that haven't happened yet. It now ends today.

## 0.3.1

New:

- Choose from 29 habit icons (Material Symbols) in a searchable "Choose icon" sheet; New habit shows 7 suggestions and a More tile. Existing habits keep a matching icon.
- Journal notes are grouped under "Today", "Yesterday" and dated headers, and each card shows the habit, that day's outcome ("Skipped", "8 of 20 pages", "Done · 45 min") and the time.
- The note editor is one clean card: the habit row on top and a plain text area that opens ready to type.
- Habits → Overall shows "X-day streak · best Y", and a habit's page shows "N days this month", instead of percentages. Notes on a habit's page use the Journal card style.

Fixed:

- Sheets could reach the top of the screen with a long list, and sat above the bottom edge. They now sit on the bottom edge and leave the top of the screen visible.
- On Android 8 and 9, opening a sheet with a text field popped up the keyboard by itself.
- The date sheet opened on next month when this week ends in it, with every day disabled.
- The "Add note" buttons had no label for TalkBack.

## 0.3.0

New:

- Edit any past day: tap a day in Habits → Week or in a habit's calendar to set Done, Partial or Skip, change the amount, add a note or clear it. Future days can't be edited.
- Done cards say when: "Done at 6:52 AM".
- Skipping a habit with "Ask for a note when I skip" on opens the note right away.
- Duration habits can be set in minutes or hours ("1.5 / 2 h", steps of a quarter hour).
- The note date, reminder times and week start are picked in bottom sheets instead of dialogs.

Fixed:

- On Android 8 and 9, the keyboard jumped back to the Name field after tapping an option in New habit.

## 0.2.0

New:

- Journal filters: pick a date range or one or more habits from the two buttons at the top. Filters always open in a bottom sheet.
- Insights shows any date range (this week by default) for all habits or the ones you pick.
- Swiping a done or skipped habit the other way undoes it. The press-and-hold sheet has an Undo button too.
- A Sponsor link in About, and in the Play build a Rate on Play Store link. After a week of use Sprout asks once whether you'd like to support it.
- Refreshed look from the new design: Today without the week strip, a clearer Undo snackbar, and the habit picker in a sheet when writing a note.

Fixed:

- Widgets showed old data after changes in the app. They now update right away.
- Marking a habit done from the Today widget sometimes did nothing.
- Widgets took a long time to appear when first added. They show a placeholder while loading.
- The Streak widget was cut off at small sizes.
- Widgets used the app's theme. They now follow the phone's colors and light or dark mode.
- Sunday was squeezed in the New habit day picker on narrow phones.
- The name field took focus back after changing a toggle or the reminder time.
- Color swatches looked different in New habit and in the accent picker; they now share one style.
- The smallest text size snapped back to Default.
- On Android 15 the keyboard pushed the New habit screen under the status bar.
- Floating buttons showed up in long (scrolling) screenshots.

## 0.1.0

First release.
