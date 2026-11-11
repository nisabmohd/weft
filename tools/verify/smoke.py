#!/usr/bin/env python3
"""End-to-end smoke check of every Weft feature on a connected emulator or phone.

Installs a fresh debug build (which seeds the sample habits from SampleData on first launch), then runs one short
check per feature and prints PASS/FAIL. Exit code is the number of failures.

    ./gradlew :app:assemblePlayDebug
    python3 tools/verify/smoke.py            # all checks
    python3 tools/verify/smoke.py today notes  # only checks whose name contains these words

Checks use visible text and TalkBack labels, so they also catch missing labels.
"""
import os
import re
import sys
import time
import traceback

sys.path.insert(0, os.path.dirname(__file__))
import device as d  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
# SMOKE_APK=app/build/outputs/apk/play/release/app-play-release.apk runs the checks on a release build:
# R8 only runs there (1.6.0's widgets never drew in release; the debug runs passed).
APK = os.environ.get("SMOKE_APK") or os.path.join(ROOT, "app/build/outputs/apk/play/debug/app-play-debug.apk")

CHECKS = []


def check(fn):
    CHECKS.append(fn)
    return fn


def tap_lowest(label):
    """Taps the bottom-most node with [label]: a sheet's button when the screen behind has the same label."""
    nodes = [n for n in d.dump() if n.label == label]
    assert nodes, f"not on screen: {label!r}"
    n = max(nodes, key=lambda n: n.center[1])
    d.tap_xy(*n.center)


def fresh_install():
    d.adb("uninstall", d.PACKAGE)
    out = d.adb("install", APK)
    assert "Success" in out, f"install failed: {out}"
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    time.sleep(3)


# --- checks, in the order a new user meets the features -----------------------------------


@check
def welcome_shows_once():
    assert d.exists("Get started", timeout=12), "welcome screen missing on first launch"
    d.tap("Get started")
    # Get started has two steps since 1.3.8. Step 1: five suggestions and "Add your own habit".
    # The first launch after a fresh install is slow: wait for the step to settle before reading it.
    assert d.exists("Pick a few habits", timeout=10), "pick-habits step missing"
    time.sleep(1)
    for name in ("Wake up at 7", "Read", "Drink water", "Workout", "Evening journal"):
        assert d.scroll_to(name), f"suggestion {name} missing"
    assert d.scroll_to("Add your own habit"), "add-your-own row missing"
    d.tap("Add your own habit")
    assert d.exists("Your own habit") and d.exists("Add this habit"), "own-habit sheet missing"
    # Back until the sheet is gone: once for the keyboard (if it came up), once for the sheet.
    for _ in range(3):
        if not d.exists("Add this habit", timeout=1):
            break
        d.back()
    # Skipping the pick step keeps the sample habits (later checks use them).
    d.tap("Skip for now")
    # Step 2: the practice card and the ways to log, Undo and the Journal included.
    assert d.exists("Try it on this card", timeout=10), "practice step missing"
    time.sleep(1)
    # Every change shows Today's Undo bar; its Undo puts the card back and ticks the Undo row.
    d.tap("Mark Read done")
    # The bar goes after ~4 s and a dump takes 1-2 s: find the bar and its Undo in one dump. The
    # bar's Undo, not the "Undo" row, is the last "Undo" (the bar is drawn last).
    undo, end = None, time.time() + 4
    while undo is None and time.time() < end:
        nodes = d.dump()
        if any(n.label == "Read marked done" for n in nodes):
            undo = [n for n in nodes if n.label == "Undo"][-1]
    assert undo, "no Undo bar after a change on the practice card"
    d.tap_xy(*undo.center)
    assert d.exists("Mark Read done", timeout=3), "Undo didn't put the practice card back"
    assert sum(n.label == "Tried" for n in d.dump()) == 2, "tapping the circle and Undo weren't both ticked"
    for row in ("Swipe right", "Swipe left", "Tap the circle", "Press and hold", "Undo", "Add a note"):
        assert d.scroll_to(row), f"practice row {row} missing"
    d.tap("Go to Today")
    # The first launch after a fresh install is slow (no baseline profile compiled yet).
    assert d.exists("Today", timeout=10), "Today not shown after Get started"
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    time.sleep(2.5)
    assert not d.exists("Get started", timeout=1.5), "welcome shown again after restart"


@check
def today_shows_score_and_habits():
    assert d.exists("of today", timeout=5), "score card missing"
    assert d.exists("Wake up at 7"), "seeded habit missing"
    assert d.exists("done", contains=True), "summary line missing"


@check
def sample_data_on_first_launch():
    # A fresh install starts with sample habits, a card to remove them, and notes on several days.
    d.nav("Today")
    d.scroll_to_top()
    assert d.exists("Sample habits") and d.exists("Remove"), "sample habits row missing on Today"
    d.nav("Journal")
    assert d.exists("Yesterday"), "sample notes don't cover earlier days"


@check
def today_circle_toggles_done():
    d.nav("Today")  # the check before this one ends on Journal
    assert d.scroll_to("Mark Vitamins done"), "Vitamins not on Today"
    d.tap("Mark Vitamins done")
    assert d.exists("Mark Vitamins not done"), "circle did not mark done"
    d.tap("Mark Vitamins not done")
    assert d.exists("Mark Vitamins done"), "circle did not unmark"
    # Logging a habit hides the sample banner for good; More has no sample row.
    d.scroll_to_top()
    assert not d.exists("Sample habits", timeout=1.0), "sample banner still there after logging a habit"


@check
def today_swipe_skip_and_undo():
    n = d.scroll_to("Workout")
    assert n, "Workout not on Today"
    w, _ = d.screen_size()
    y = n.center[1]
    d.swipe(int(w * 0.62), y, int(w * 0.02), y, 250)
    # One dump for both: the snackbar only stays about 4 seconds.
    nodes = d.dump()
    assert any(n.label == "Workout skipped" for n in nodes), "no Undo snackbar after swipe left"
    undo = next(n for n in nodes if n.label == "Undo")
    d.tap_xy(*undo.center)
    assert d.exists("Mark Workout done"), "undo did not restore Workout"


@check
def today_opposite_swipe_undoes():
    w, _ = d.screen_size()
    def swipe(label, right):
        d.scroll_to_top()
        n = d.scroll_to(label)
        assert n, f"{label} not on Today"
        y = n.center[1]
        # Left swipes start mid-card: on short screens the Add note button covers the card's right end.
        a, b = (int(w * 0.12), int(w * 0.88)) if right else (int(w * 0.62), int(w * 0.02))
        d.swipe(a, y, b, y, 250)
        time.sleep(1)
    # Done card: swipe left undoes instead of skipping.
    swipe("Wake up at 7", right=False)
    assert d.exists("Mark Wake up at 7 done"), "swipe left on a done card didn't undo it"
    swipe("Wake up at 7", right=True)
    assert d.exists("Mark Wake up at 7 not done"), "swipe right didn't mark it done again"
    # Skipped card: swipe right undoes instead of marking done.
    swipe("Workout", right=False)
    assert d.exists("Skipped", timeout=2), "swipe left didn't skip Workout"
    swipe("Workout", right=True)
    assert d.exists("Mark Workout done"), "swipe right on a skipped card didn't undo it"


@check
def today_long_press_sheet():
    d.nav("Today")
    d.scroll_to_top()  # the swipe checks before this one leave a short screen scrolled past Read
    d.long_press("Read")
    assert d.exists("Goal 20 pages", contains=True), "amount sheet missing its goal"
    assert d.exists("Partial"), "outcome buttons missing"
    d.tap("Cancel")


@check
def new_habit_is_created():
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("Name")
    d.type_text("Smoke test habit")
    d.back()  # hide the keyboard
    d.tap("Save")
    time.sleep(1)
    assert d.scroll_to("Smoke test habit"), "new habit not on Today"


@check
def reminder_time_wheel():
    # The one time picker (1.3.8): the wheel sheet. Tap the 10 o'clock row and set it.
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    assert d.scroll_to("9:00", contains=True), "reminder time missing on New habit"
    d.tap("9:00", contains=True)
    # New habit's Reminder sheet (1.6.0): the time wheel, Notification / Alarm, Set reminder.
    assert d.exists("Reminder") and d.exists("Set reminder"), "time wheel sheet missing"
    assert d.exists("AM") and d.exists("PM"), "AM/PM column missing"
    d.tap("10")
    time.sleep(0.6)
    d.tap("Set reminder")
    assert d.exists("10:00", contains=True), "picked time not shown on New habit"
    d.tap("Close")
    if d.exists("Discard", timeout=1.5):
        d.tap("Discard")


@check
def measure_with_step():
    # Measure habit in hours with a quarter-hour step: − / + and the slider move by the step.
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("Name")
    d.type_text("Deep work")
    d.back()
    d.tap("Measure")
    d.tap_xy(*d.scroll_to("Target").center)
    d.type_text("1.5")
    d.back()
    d.tap("Unit")
    d.type_text("h")
    d.back()
    d.tap_xy(*d.find("Step").center)
    d.shell("input keyevent KEYCODE_MOVE_END")
    d.shell("input keyevent KEYCODE_DEL")
    d.type_text("0.25")
    d.back()
    d.tap("Save")
    time.sleep(1)
    n = d.scroll_to("Deep work")
    assert n and d.exists("1.5 h"), "measure habit doesn't show its goal"
    d.long_press("Deep work")
    assert d.exists("Goal 1.5 h · today"), "sheet header missing the goal"
    d.tap("More")
    d.tap("More")
    assert d.exists("0.5", contains=True), "stepper doesn't move by the habit's step"
    d.tap("Save")
    assert d.exists("0.5 / 1.5 h"), "partial amount not shown on the card"


@check
def today_done_shows_time():
    d.nav("Today")
    d.scroll_to_top()
    # Earlier checks toggle Wake up at 7, so its time is whenever that ran.
    assert d.exists("Done at ", contains=True), "done card doesn't say when it was logged"
    n = d.scroll_to("Mark Workout done")  # below the fold on a small screen
    assert n, "Workout not on Today"
    d.tap_xy(*n.center)
    assert d.exists("45 / 45 min"), "Workout not done"
    d.tap("Mark Workout not done")


@check
def skip_opens_note_when_asked():
    import datetime
    d.nav("Today")
    d.tap_xy(*d.scroll_to("Vitamins").center)
    d.tap("Edit habit")
    d.scroll_to("Ask for a note when I skip")
    d.tap("Ask for a note when I skip")
    d.tap("Save")
    time.sleep(1)
    d.back()
    n = d.scroll_to("Vitamins")
    w, _ = d.screen_size()
    d.swipe(int(w * 0.62), n.center[1], int(w * 0.02), n.center[1], 250)
    # The note field asks the skip's question (1.6.0 filled note field).
    assert d.exists("What got in the way?", timeout=4), "skipping a habit that asks for a note didn't open the note sheet"
    d.type_text("Left them at home")
    d.tap("Save")
    assert d.exists("Skipped · note added", contains=True), "note from the skip sheet wasn't saved"


@check
def edit_past_day_from_week():
    import datetime
    day = datetime.date.today() - datetime.timedelta(days=1)
    name = day.strftime("%A %-d %B")
    d.nav("Habits")
    d.tap("Week")
    n = d.find("Read, " + name, contains=True)
    if n is None:  # yesterday was last week (today is the first day of the week)
        d.tap("Previous week")
        n = d.find("Read, " + name, contains=True)
    assert n, "no day mark for yesterday"
    before = n.label
    target, want, message = ("Done", "done", "Read marked done · ") if before.endswith("skipped") else ("Skip", "skipped", "Read skipped · ")
    d.tap_xy(*n.center)
    assert d.exists(day.strftime("%A, %-d %b") + " · goal 20 pages"), "edit sheet header missing the date"
    d.tap(target)
    d.tap("Save")
    nodes = d.dump()
    assert any(x.label.startswith(message) for x in nodes), "no Undo snackbar after editing a past day"
    assert any(x.label == f"Read, {name}: {want}" for x in nodes), "day mark didn't change"
    d.tap_xy(*next(x for x in nodes if x.label == "Undo").center)
    assert d.exists(before), "undo didn't restore the past day"


@check
def edit_past_day_from_detail_calendar():
    import datetime
    day = datetime.date.today() - datetime.timedelta(days=1)
    name = day.strftime("%A %-d %B")
    d.nav("Habits")
    d.tap("Week")
    d.tap("Wake up at 7")
    assert d.exists("best streak"), "habit detail didn't open"
    time.sleep(0.8)  # let the screen transition finish before tapping a day
    if day.month != datetime.date.today().month:
        d.tap("Previous month")
    n = d.find(name + ":", contains=True)
    assert n, "calendar day missing"
    was = n.label.split(": ")[1]
    target, want = ("Done", "done") if was == "skipped" else ("Skip", "skipped")
    d.tap_xy(*n.center)
    assert d.exists("Save"), "calendar day didn't open the log sheet"
    d.tap(target)
    d.tap("Save")
    time.sleep(0.5)
    nodes = d.dump()  # one dump: the snackbar only stays about 4 seconds
    try:
        assert any(x.label == f"{name}: {want}" for x in nodes), "calendar day didn't change"
        d.tap_xy(*next(x for x in nodes if x.label == "Undo").center)
        assert d.exists(f"{name}: {was}"), "undo didn't restore the day"
    finally:
        d.back()


@check
def habits_week_and_overall():
    d.nav("Habits")
    assert d.exists("complete this week", contains=True), "week view missing"
    d.tap("Overall")
    assert d.exists("Last 26 weeks"), "overall heatmap missing"
    assert d.exists("Best streak · ", contains=True), "overall card missing its best streak line"
    assert d.exists("-day streak", contains=True), "overall card missing its current streak"
    assert not any(n.label.endswith("%") for n in d.dump()), "overall still shows a percentage"
    d.tap("Week")


@check
def habit_detail_opens():
    d.tap("Read", timeout=5)
    assert d.exists("best streak"), "detail stats missing"
    assert d.exists("this month"), "detail's first stat isn't \"this month\""
    assert not any(n.label.endswith("%") for n in d.dump()), "detail still shows a percentage"
    assert d.exists("Edit habit"), "edit action missing"
    d.back()


@check
def note_suggestions():
    # Sample data: Drink water is Partial today and has a "Busy day..." note from before. Add note's
    # habit depends on what earlier checks logged, so pick Drink water here.
    d.nav("Today")
    d.tap("Add note")
    time.sleep(1)
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap("Choose habit")
    time.sleep(1)
    d.tap("Drink water")
    assert d.exists("Quick start", timeout=4), "no Quick start chips on an empty note"
    assert d.exists("Your recent notes"), "no recent notes for the habit"
    d.tap("Note")
    d.type_text("Busy d")
    assert d.exists("Finish with", timeout=4), "no completion while typing a past sentence"
    assert d.exists("Similar notes"), "no similar notes while typing"
    d.tap("Suggestion: day, forgot the bottle at home.")
    assert d.exists("Busy day, forgot the bottle at home.", timeout=3), "the ending wasn't added"
    d.back()
    d.tap("Close")


@check
def journal_lists_and_adds_notes():
    d.nav("Journal")
    assert d.exists("Office birthday cake", contains=True), "seeded note missing"
    d.tap("Add note")
    # Add note starts on the habit logged last today, which is Deep work after measure_with_step;
    # journal_filter_by_habit needs Deep work without notes, so write this one on Read (no note today).
    time.sleep(1)
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap("Choose habit")
    time.sleep(1)
    d.tap("Read")
    d.tap("Note")
    d.type_text("Smoke test note")
    d.tap("Save")
    # A note card reads as one item ("Read, Skipped, 9:12 AM. <note>", design 2026-11-11b): match inside it.
    assert d.exists("Smoke test note", contains=True, timeout=4), "new note not in Journal"


@check
def journal_cards_and_new_note():
    d.nav("Journal")
    assert d.exists("Today"), "no date header over today's notes"
    # Note cards (design 2026-11-11): name and mark on top, the note, then the time; TalkBack reads
    # the outcome with them ("No sugar, Skipped, Office birthday cake…, 9:12 AM").
    assert d.exists(", Skipped, ", contains=True), "note card doesn't say the day's outcome and time"
    d.tap("Add note")
    # Since 1.3.6 Add note starts on the habit logged last today that has no note yet, so the note the
    # check before this one wrote is not what opens: the editor is empty. (Opening a day that has a
    # note is covered by note_shows_the_saved_one_for_that_day.)
    assert d.exists("New note", timeout=4), "Add note didn't open New note"
    assert not d.exists("Smoke test note", timeout=1), "Add note opened a habit that already has today's note"
    time.sleep(1)
    assert "mInputShown=true" in d.shell("dumpsys input_method"), "new note didn't open with the keyboard"
    d.back()
    d.tap("Close")


@check
def icon_picker():
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("More icons")
    assert d.exists("Choose icon"), "icon picker didn't open"
    _, h = d.screen_size()
    title = d.find("Choose icon")
    assert title.bounds[1] > h * 0.08, "sheet reaches the top of the screen"
    use = d.find("Use this icon")
    assert use.bounds[3] > h * 0.85, "sheet doesn't sit on the bottom edge"
    d.tap("Search icons")
    d.type_text("pho")
    assert d.exists("Less phone") and not d.exists("Code", timeout=1), "search doesn't filter icons"
    d.back()  # keyboard
    d.tap("Less phone")
    d.tap("Use this icon")
    n = d.find("Less phone")
    assert n and (n.selected or n.checked), "picked icon isn't selected in the row"
    d.tap("Close")


@check
def journal_long_press_actions():
    d.nav("Journal")
    d.long_press("Office birthday cake. Had a small slice and stopped there.", contains=True)
    for label in ["Edit note", "Share", "Delete note"]:
        assert d.exists(label), f"long-press sheet missing {label}"
    d.back()


@check
def detail_header_buttons():
    d.nav("Habits")
    d.tap("Wake up at 7")
    for label in ["Back", "Edit habit", "Archive", "Delete"]:
        assert d.find(label), f"habit page header missing {label}"
    d.tap("Back")


@check
def journal_filter_by_habit():
    d.nav("Journal")
    d.tap("Filter by habit")
    assert d.exists("Show all notes"), "filter sheet didn't open"
    d.tap("No sugar")
    d.tap("Show notes for 1 habit")
    time.sleep(1)
    assert d.exists("Office birthday cake", contains=True), "No sugar's note missing when filtered"
    assert not d.exists("Smoke test note", contains=True, timeout=1), "another habit's note shown under the filter"
    d.tap("Filter by habit")
    d.tap("Clear")
    # Made by measure_with_step; no sample note and no check writes one. Last in the sheet's list.
    n = d.scroll_to("Deep work")
    assert n, "Deep work not in the filter sheet"
    d.tap_xy(*n.center)
    d.tap("Show notes for 1 habit")
    time.sleep(1)
    assert d.exists("No Deep work notes yet"), "empty filter message missing (it names the habit, 2026-11-09)"
    d.tap("Show all notes")
    assert d.exists("Office birthday cake", contains=True), "filter didn't reset"


@check
def journal_filter_by_date():
    import datetime
    d.nav("Journal")
    d.tap("Filter by date")
    assert d.exists("All dates"), "date sheet didn't open with the All dates shortcut"
    today = datetime.date.today()
    d.tap(str(today.day))
    d.tap("Show", contains=True)
    time.sleep(1)
    assert d.exists("Office birthday cake", contains=True), "today's note missing under a today-only range"
    assert d.exists(f"{today.day} ", contains=True), "date chip doesn't show the range"
    d.tap("Clear date filter")
    assert not d.exists("Clear date filter", timeout=1.0), "the chip's ✕ didn't clear the range"
    # No preset range chips: the Journal is back on every note.
    assert not d.exists("7 days", timeout=1.0) and not d.exists("All", timeout=0.5), "preset range chips are back"
    assert d.scroll_to("Yesterday"), "older notes hidden after clearing the range"
    d.scroll_to_top()
    d.tap("Filter by date")
    d.tap("All dates")
    assert not d.exists("Clear date filter", timeout=1.0), "All dates left a custom range chip"


@check
def note_date_in_a_sheet():
    import datetime
    today = datetime.date.today()
    if today.day == 1:
        return  # no earlier day in this month to pick
    d.nav("Journal")
    d.tap("Add note")
    time.sleep(1)
    # Hide the keyboard, but only once it's up: Back without it would close the screen.
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap(today.strftime("%A, ") + str(today.day), contains=True)
    assert d.exists("Use ", contains=True), "note date didn't open the date sheet"
    d.tap(str(today.day - 1))
    d.tap("Use ", contains=True)
    yesterday = today - datetime.timedelta(days=1)
    assert d.exists(yesterday.strftime("%A, ") + str(yesterday.day), contains=True), "note date didn't change"
    # Saved today about yesterday: the Journal says when it was written.
    # The field by its label: since 1.4.0 the card is short and suggestions sit under it.
    d.tap("Note")
    d.type_text("Late note")
    d.tap("Save")
    assert d.scroll_to("written ", contains=True), "a note written a day later doesn't say when it was written"


@check
def note_shows_the_saved_one_for_that_day():
    # The sample data has a No sugar note for today.
    d.nav("Journal")
    d.tap("Add note")
    time.sleep(1)
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap("Choose habit")
    time.sleep(1)
    d.tap("No sugar")
    assert d.exists("Office birthday cake", contains=True), "the day's saved note isn't shown in the form"
    assert d.exists("Edit note"), "the form didn't switch to editing the saved note"
    d.tap("Close")


@check
def insights_render():
    d.nav("Insights")
    assert d.exists("average"), "score card missing"
    assert d.exists("Weekly rhythm"), "bar chart missing"
    # Opens on this week with no preset range chips and no chip row.
    assert not d.exists("30 days", timeout=1.0) and not d.exists("Clear date filter", timeout=0.5), "preset range chips or a range chip on open"
    d.tap("Change date range")
    assert d.exists("Date range"), "date range sheet didn't open"
    d.tap("1")  # the 1st of this month starts a new range
    d.tap("Show 1", contains=True)
    time.sleep(1)
    assert d.exists("average"), "insights missing after picking a range"
    d.tap("Filter by habit")
    d.tap("Read")
    d.tap("Show 1 habit")
    assert d.exists("Remove habit filter"), "habit filter chip missing"
    d.tap("Remove habit filter")
    assert not d.exists("Remove habit filter", timeout=1.0), "chip's ✕ didn't clear the habit filter"
    assert d.exists("Clear date filter"), "date chip has no ✕ for a custom range"
    d.tap("Change date range")
    d.tap("This week")
    assert not d.exists("Clear date filter", timeout=1.0), "This week left a range chip"


@check
def more_settings_and_about():
    d.nav("More")
    assert d.exists("Backups"), "Backups row missing"
    assert d.exists("Theme"), "appearance section missing"
    d.tap("Dark")
    assert d.exists("Dark"), "theme option missing"
    d.tap("System")
    w, h = d.screen_size()
    for _ in range(3):
        d.swipe(w // 2, int(h * 0.8), w // 2, int(h * 0.3))
    d.tap("About Weft")
    assert d.scroll_to("Open-source licences"), "About screen incomplete"
    # The developer row (1.3.8): mid apps and the contact address.
    assert d.scroll_to("mid apps"), "mid apps row missing"
    assert d.exists("Contact: hello.midapps@gmail.com"), "contact address missing"
    d.back()


@check
def more_language_sheet():
    # Pick Spanish, see the app switch at once, then go back to the phone's language.
    d.nav("More")
    assert d.scroll_to("Language"), "Language row missing"
    d.tap("Language")
    assert d.exists("Help translate Weft"), "Language sheet missing"
    assert d.scroll_to("日本語"), "Japanese not listed"
    d.tap("Español")
    assert d.exists("Más", timeout=8), "app did not switch to Spanish"
    assert d.scroll_to("Idioma"), "Language row not translated"
    d.tap("Idioma")
    d.tap("Predeterminado del sistema")
    assert d.exists("More", timeout=8), "app did not return to the phone's language"


# --- widgets: pinned on the home screen, they must follow every change at once -----------------
# "At once" = within WIDGET_S seconds of the change (the app debounces 300 ms, then redraws).
WIDGET_S = 4.0


def today_ratio(labels):
    """(done, total) from the Today widget's "3/7"."""
    m = next((re.fullmatch(r"(\d+)/(\d+)", l) for l in labels if re.fullmatch(r"(\d+)/(\d+)", l)), None)
    assert m, f"Today widget shows no done/total: {labels}"
    return int(m.group(1)), int(m.group(2))


def ratio_is(done, total):
    return lambda labels: f"{done}/{total}" in labels


def up_next(labels):
    """Habits the Today widget offers to mark done."""
    return [l[len("Mark "):-len(" done")] for l in labels
            if l.startswith("Mark ") and l.endswith(" done") and not l.endswith(" not done")]


def today_widget():
    """Goes home and waits for the Today widget's labels (a failed check may have left the app open)."""
    d.home()
    labels = d.widget_wait(lambda l: any(re.fullmatch(r"\d+/\d+", x) for x in l), timeout=8)
    assert labels, "Today widget not on the home screen"
    return labels


def app_today():
    d.open_app()
    d.nav("Today")
    d.scroll_to_top()


@check
def widgets_can_shrink():
    """Each widget may be resized below its starting size (its min/max resize sizes are set and
    loaded); WidgetLayoutTest checks every size in between fits."""
    out = d.shell("dumpsys appwidget")
    for kind in ("Week", "Today", "Strip", "Streak"):
        m = re.search(kind + r"WidgetReceiver\}\}\s+min=\((\d+)x(\d+)\)\s+minResize=\((\d+)x(\d+)\)", out)
        assert m, f"{kind} widget isn't registered"
        # Packed TypedValue dimensions: the dp value is in the upper bits.
        min_w, min_h, rw, rh = (int(v) >> 8 for v in m.groups())
        assert rw < min_w or rw <= 110, f"{kind} widget can't be made narrower than {min_w} dp"
        assert rh < min_h or rh <= 50, f"{kind} widget can't be made shorter than {min_h} dp"


@check
def widget_today_renders():
    d.pin_widget("today")
    labels = d.widget_wait(lambda l: "Up next" in l or "All done for today" in l, timeout=8)
    assert labels, "Today widget didn't render"
    done, total = today_ratio(labels)
    assert f"{done} of {total} done today" in labels, "ring has no TalkBack label"
    assert total > 0, "Today widget counts no habits"
    if "Up next" in labels:
        assert up_next(labels), "Up next lists no habits with a circle"


@check
def widget_follows_app_done_and_undo():
    labels = today_widget()
    done, total = today_ratio(labels)
    habit = up_next(labels)[0]
    app_today()
    assert d.scroll_to(f"Mark {habit} done"), f"{habit} not on Today"
    d.tap(f"Mark {habit} done")
    d.home()
    assert d.widget_wait(ratio_is(done + 1, total), WIDGET_S), f"widget didn't count {habit} done from the app"
    assert habit not in up_next(d.widget_wait(lambda l: True)), f"{habit} still in Up next after it was done"
    # Undo in the app (the circle again) must take it back off the widget.
    app_today()
    assert d.scroll_to(f"Mark {habit} not done"), f"{habit} not done on Today"
    d.tap(f"Mark {habit} not done")
    d.home()
    assert d.widget_wait(ratio_is(done, total), WIDGET_S), "widget didn't follow the undo"


@check
def widget_follows_snackbar_undo_and_skip():
    labels = today_widget()
    done, total = today_ratio(labels)
    habit = up_next(labels)[0]
    app_today()
    n = d.scroll_to(habit)
    assert n, f"{habit} not on Today"
    w, _ = d.screen_size()
    y = n.center[1]
    # Swipe right = Done, then the snackbar's Undo, read from one dump (it lasts about 4 s).
    d.swipe(int(w * 0.12), y, int(w * 0.88), y, 250)
    undo = next((x for x in d.dump() if x.label == "Undo"), None)
    assert undo, "no Undo snackbar after swipe right"
    d.tap_xy(*undo.center)
    d.home()
    assert d.widget_wait(ratio_is(done, total), WIDGET_S), "widget kept the Done the snackbar undid"
    assert habit in up_next(d.widget_wait(lambda l: True)), f"{habit} didn't come back to Up next"
    # Swipe left = Skip: off Up next, not counted done.
    app_today()
    n = d.scroll_to(habit)
    d.swipe(int(w * 0.62), n.center[1], int(w * 0.02), n.center[1], 250)
    d.home()
    labels = d.widget_wait(lambda l: habit not in up_next(l), WIDGET_S)
    assert labels, f"skipped {habit} still in Up next"
    assert today_ratio(labels)[0] == done, "a Skip was counted as done"
    app_today()
    n = d.scroll_to(habit)
    d.swipe(int(w * 0.12), n.center[1], int(w * 0.88), n.center[1], 250)  # right on a skip = undo
    d.home()
    assert d.widget_wait(lambda l: habit in up_next(l), WIDGET_S), f"{habit} didn't return after undoing the skip"


@check
def widget_circle_marks_done_everywhere():
    labels = today_widget()
    done, total = today_ratio(labels)
    habit = up_next(labels)[0]
    d.widget_tap(f"Mark {habit} done")
    assert d.widget_wait(ratio_is(done + 1, total), WIDGET_S), "widget circle didn't update the widget"
    app_today()
    assert d.scroll_to(f"Mark {habit} not done"), "app didn't show the widget's Done"
    d.tap(f"Mark {habit} not done")
    d.home()
    assert d.widget_wait(ratio_is(done, total), WIDGET_S), "widget didn't follow undo after a widget tap"


@check
def widget_circle_works_after_process_death():
    """Android kills a background app to free memory; the widget's circle must still work."""
    done, total = today_ratio(today_widget())
    habit = up_next(today_widget())[0]
    pid = d.shell(f"pidof {d.PACKAGE}").strip()
    if pid:
        d.shell(f"kill -9 {pid}")  # needs adb root; falls back to am kill below
        d.shell(f"am kill {d.PACKAGE}")
        time.sleep(1)
    d.widget_tap(f"Mark {habit} done")
    # The tap has to start the process first, so allow a little longer.
    assert d.widget_wait(ratio_is(done + 1, total), WIDGET_S + 4), "widget circle did nothing after the process died"
    app_today()
    assert d.scroll_to(f"Mark {habit} not done"), "app (cold start) didn't show the widget's Done"
    d.tap(f"Mark {habit} not done")
    d.home()
    assert d.widget_wait(ratio_is(done, total), WIDGET_S), "widget didn't follow the undo"


@check
def widget_comes_back_after_force_stop():
    """
    Force stop (Settings → app → Force stop) makes Android put widgets back to their loading
    layout, and nothing of a stopped app may run until it is opened. Opening Weft must redraw
    every widget at once.
    """
    done, total = today_ratio(today_widget())
    d.shell(f"am force-stop {d.PACKAGE}")
    time.sleep(2)
    d.open_app()
    d.home()
    assert d.widget_wait(ratio_is(done, total), WIDGET_S), "widget stayed blank after the app was reopened"
    assert up_next(today_widget()), "widget's circles didn't come back after a force stop"


@check
def widget_habit_name_opens_habit():
    habit = up_next(today_widget())[0]
    # Cold: the app isn't running, as after Android freed its memory. (Not a force stop: since
    # Android 15 a force-stopped app's widgets show their loading layout until it's opened; see
    # widget_comes_back_after_force_stop.)
    d.home()
    d.shell(f"am kill {d.PACKAGE}")
    time.sleep(1)
    d.widget_tap(habit)
    assert d.exists("Edit habit", timeout=8) and d.exists(habit), f"cold start didn't open {habit}"
    # Warm: the app is open on another tab (singleTop, so this arrives in onNewIntent).
    d.back()
    d.nav("More")
    d.home()
    d.widget_tap(habit)
    assert d.exists("Edit habit", timeout=6) and d.exists(habit), f"widget didn't open {habit} over the More tab"
    # Warm, on the same habit's page already: still that page, not a second copy.
    d.home()
    d.widget_tap(habit)
    assert d.exists("Edit habit", timeout=6), "second tap lost the habit page"
    d.back()
    assert not d.exists("Edit habit", timeout=1.5), "the habit page opened twice"
    d.home()


@check
def widget_ring_opens_today():
    today_widget()
    d.open_app()
    d.nav("More")
    d.home()
    d.widget_tap("done today", contains=True)
    assert d.exists("Today", timeout=6) and d.exists("of today", contains=True), "ring didn't open the app"
    d.home()


@check
def widget_follows_archive_and_restore():
    labels = today_widget()
    done, total = today_ratio(labels)
    habit = up_next(labels)[0]
    d.widget_tap(habit)
    assert d.exists("Edit habit", timeout=8), f"{habit} page didn't open"
    d.tap("Archive")
    # Archive asks first (2026-11-11): confirm with the sheet's button, the lowest "Archive" on screen.
    assert d.exists(f"Archive {habit}?", timeout=4), "Archive didn't ask first"
    tap_lowest("Archive")
    d.home()
    assert d.widget_wait(lambda l: f"{done}/{total - 1}" in l and habit not in up_next(l), WIDGET_S), \
        f"archived {habit} still on the widget"
    # Archiving closes the habit's page; it comes back from Habits → Manage habits → Archived.
    d.open_app()
    d.nav("Habits")
    d.tap("Manage habits")
    assert d.scroll_to("Restore"), f"archived {habit} not under Manage habits"
    d.tap("Restore")
    d.home()
    assert d.widget_wait(lambda l: f"{done}/{total}" in l, WIDGET_S), f"restored {habit} didn't come back"


@check
def widget_week_renders_and_follows():
    d.pin_widget("week")
    labels = d.widget_wait(lambda l: "This week" in l, timeout=8)
    assert labels, "Week widget didn't render"
    rng = labels[labels.index("This week") + 1]
    # "21 – 27 Sep" in one month, "26 Oct – 1 Nov" across two (it once said "26 – 1 Nov").
    assert re.fullmatch(r"\d+ – \d+ \w+|\d+ \w+ – \d+ \w+", rng), f"bad week range {rng!r}"
    first, last = rng.split(" – ")
    if first.isdigit():
        assert int(first) < int(last.split()[0]), f"range {rng!r} spans two months but names one"
    pct = next((l for l in labels if re.fullmatch(r"\d+%", l)), None)
    assert pct, "Week widget has no %"
    habit = next(l[:-len(" this week")] for l in labels if l.endswith(" this week"))
    app_today()
    # One pass for either label: looking for "done" first scrolls past a card that says "not done".
    assert d.scroll_to(f"Mark {habit} ", contains=True), f"{habit} not on Today"
    mark = f"Mark {habit} done" if d.exists(f"Mark {habit} done", timeout=1) else f"Mark {habit} not done"
    d.tap(mark)
    d.home()
    assert d.widget_wait(lambda l: next((x for x in l if re.fullmatch(r"\d+%", x)), None) != pct, WIDGET_S), \
        "Week widget % didn't change after a log"
    app_today()
    d.scroll_to(habit)
    d.tap(f"Mark {habit} not done" if mark == f"Mark {habit} done" else f"Mark {habit} done")
    d.home()
    assert d.widget_wait(lambda l: pct in l, WIDGET_S), "Week widget % didn't go back after undo"


@check
def widget_strip_follows():
    d.pin_widget("strip")
    labels = d.widget_wait(lambda l: any(re.fullmatch(r"\d+ of \d+ today", x) for x in l), timeout=8)
    assert labels, "Strip widget didn't render"
    line = next(x for x in labels if re.fullmatch(r"\d+ of \d+ today", x))
    done, total = map(int, re.findall(r"\d+", line))
    assert any(x.startswith("Next: ") or x == "All done" for x in labels), "strip has no Next line"
    app_today()
    habit = next(x.label[len("Mark "):-len(" done")] for x in d.dump() if x.label.startswith("Mark ") and x.label.endswith(" done") and " not " not in x.label)
    d.tap(f"Mark {habit} done")
    d.home()
    assert d.widget_wait(lambda l: f"{done + 1} of {total} today" in l, WIDGET_S), "strip didn't count the Done"
    app_today()
    d.scroll_to(f"Mark {habit} not done")
    d.tap(f"Mark {habit} not done")
    d.home()
    assert d.widget_wait(lambda l: f"{done} of {total} today" in l, WIDGET_S), "strip didn't follow the undo"


@check
def widget_streak_follows():
    d.pin_widget("streak")
    labels = d.widget_wait(lambda l: "day streak" in l or "days streak" in l, timeout=8)
    assert labels, "Streak widget didn't render"
    habit = labels[0]
    count = next(x for x in labels if x.isdigit())
    app_today()
    n = d.scroll_to(f"Mark {habit} ", contains=True)  # either label, in one pass
    assert n, f"{habit} not on Today"
    was_done = "not done" in n.label
    d.tap(n.label)
    d.home()
    changed = d.widget_wait(lambda l: count not in l, WIDGET_S)
    taps = 1
    if not changed and not was_done:
        # Today was Partial (the sample Read is 15 of 20): Partial and Done both count, so the
        # streak stays. A second tap clears the day, which must change it.
        app_today()
        assert d.scroll_to(f"Mark {habit} not done"), f"{habit} not done on Today"
        d.tap(f"Mark {habit} not done")
        d.home()
        changed = d.widget_wait(lambda l: count not in l, WIDGET_S)
        taps = 2
    assert changed, "streak didn't change after today's log"
    if taps == 1:
        app_today()
        d.scroll_to(habit)
        d.tap(f"Mark {habit} done" if was_done else f"Mark {habit} not done")
        d.home()
        assert d.widget_wait(lambda l: count in l, WIDGET_S), "streak didn't go back after undo"


@check
def widget_rolls_over_at_midnight():
    """Needs a rootable emulator (adb root) to move the clock; restores it afterwards."""
    if "cannot run as root" in d.adb("root") or "production" in d.adb("root"):
        print("      skipped: emulator can't run adb as root")
        return
    time.sleep(2)
    d.pin_widget("today")
    labels = today_widget()
    habit = up_next(labels)[0]
    d.widget_tap(f"Mark {habit} done")  # so today has a Done that tomorrow mustn't show
    assert d.widget_wait(lambda l: today_ratio(l)[0] > 0, WIDGET_S)
    start = time.time()
    real = int(d.shell("date +%s").strip())
    d.shell("settings put global auto_time 0")
    try:
        # 20 s before local midnight. Setting the clock is itself a TIME_SET the app must handle.
        tomorrow = int(d.shell("date -d \"$(date +%Y-%m-%d) 00:00:00\" +%s").strip()) + 86400
        d.shell(f"date @{tomorrow - 20}")
        time.sleep(2)
        d.home()
        assert d.widget_wait(lambda l: today_ratio(l)[0] > 0, WIDGET_S), "a clock change dropped today's Done"
        # The midnight alarm: no log, no app open, the widget must move to the new day by itself.
        assert d.widget_wait(lambda l: today_ratio(l)[0] == 0, 60), "widget still shows yesterday after midnight"
        # A clock change back (TIME_SET) must bring yesterday's day back.
        d.shell(f"date @{real + int(time.time() - start)}")
        assert d.widget_wait(lambda l: today_ratio(l)[0] > 0, WIDGET_S + 4), "widget didn't follow the clock back"
    finally:
        d.shell(f"date @{real + int(time.time() - start)}")
        d.shell("settings put global auto_time 1")
    app_today()
    if d.scroll_to(f"Mark {habit} not done"):
        d.tap(f"Mark {habit} not done")

@check
def widget_nothing_due_today():
    """With no habit due (all archived), the Today widget says so instead of "All done"."""
    d.pin_widget("today")
    today_widget()
    d.open_app()
    d.nav("Habits")
    d.tap("Manage habits")
    for _ in range(20):
        n = d.find("Archive", timeout=1.5)
        if not n:
            break
        d.tap_xy(*n.center)
    try:
        d.home()
        labels = d.widget_wait(lambda l: "Nothing due today" in l, WIDGET_S)
        assert labels, "Today widget with no habits doesn't say \"Nothing due today\""
        assert "0/0" in labels and "All done for today" not in labels, f"empty day reads as done: {labels}"
    finally:
        d.open_app()
        for _ in range(20):
            n = d.scroll_to("Restore")
            if not n:
                break
            d.tap_xy(*n.center)
        d.back()
    d.home()
    assert d.widget_wait(lambda l: any(re.fullmatch(r"\d+/[1-9]\d*", x) for x in l), WIDGET_S), "habits didn't come back"


@check
def widget_streak_follows_an_old_day():
    """Changing a day more than a week back (habit calendar) moves the Streak widget."""
    import datetime
    d.pin_widget("streak")
    labels = d.widget_wait(lambda l: "day streak" in l or "days streak" in l, timeout=8)
    assert labels, "Streak widget didn't render"
    habit, count = labels[0], next(x for x in labels if x.isdigit())
    if int(count) < 10:
        print(f"      skipped: {habit}'s streak is {count}, needs 10+ to reach 9 days back")
        return
    day = datetime.date.today() - datetime.timedelta(days=9)
    name = day.strftime("%A %-d %B")
    d.open_app()
    d.nav("Habits")
    d.tap("Week")
    d.tap(habit)
    assert d.exists("best streak"), "habit detail didn't open"
    time.sleep(0.8)
    if day.month != datetime.date.today().month:
        d.tap("Previous month")
    n = d.scroll_to(name + ":", contains=True)
    assert n, "calendar day 9 days back missing"
    d.tap_xy(*n.center)
    d.tap("Skip")
    d.tap("Save")
    d.home()
    assert d.widget_wait(lambda l: count not in l, WIDGET_S), "Streak widget ignored a change 9 days back"
    d.open_app()
    n = d.scroll_to(name + ":", contains=True)
    d.tap_xy(*n.center)
    d.tap("Done")
    d.tap("Save")
    d.back()
    d.home()
    assert d.widget_wait(lambda l: count in l, WIDGET_S), "Streak widget didn't go back"


@check
def widget_follows_dark_mode():
    """Rings and marks are drawn into bitmaps; a dark-mode switch must redraw them."""
    d.pin_widget("today")
    labels = today_widget()
    ring = next(n for n in d.widget_nodes() if n.label.endswith(" done today"))
    d.shell("cmd uimode night no")
    time.sleep(3)
    light = d.brightness(ring.bounds)
    try:
        d.shell("cmd uimode night yes")
        time.sleep(4)
        ring = next(n for n in d.widget_nodes() if n.label.endswith(" done today"))
        dark = d.brightness(ring.bounds)
        assert dark < light - 30, f"ring didn't turn dark (brightness {light:.0f} → {dark:.0f})"
    finally:
        d.shell("cmd uimode night no")
        time.sleep(2)


@check
def widget_follows_language():
    """A language picked in Weft reaches the widgets at once."""
    d.pin_widget("today")
    today_widget()
    d.open_app()
    d.nav("More")
    d.scroll_to("Language")
    d.tap("Language")
    d.scroll_to("Español")
    d.tap("Español")
    assert d.exists("Más", timeout=8), "app did not switch to Spanish"
    try:
        d.home()
        assert d.widget_wait(lambda l: "hoy" in l, WIDGET_S + 2), "widget stayed in English after switching to Spanish"
    finally:
        d.open_app()
        d.nav("Más")
        d.scroll_to("Idioma")
        d.tap("Idioma")
        d.tap("Predeterminado del sistema")
        d.exists("More", timeout=8)
    d.home()
    assert d.widget_wait(lambda l: "today" in l, WIDGET_S + 2), "widget stayed in Spanish after going back"


# --- Backup & restore and Google Drive backup (play and github builds). A real backup needs Play
# services, a Google account and the OAuth client; these check the screens and what fails without
# them. The debug build's DriveStateActivity sets the Drive state without an account (it closes
# Weft's task, so every check opens Weft again after it). ------------------------------------

def drive_state(account=None, last_backup_ms=None, failed=None, count=None):
    extras = ""
    if account:
        extras += f" --es account {account}"
        if last_backup_ms:
            extras += f" --el lastBackupAt {last_backup_ms}"
        if count is not None:
            extras += f" --ei count {count}"
        if failed:
            extras += f" --es failed {failed}"
    d.shell(f"am start -W -f 0x10008000 -n {d.PACKAGE}/.DriveStateActivity{extras}")
    time.sleep(1)


def backup_screen():
    """More → Backups → Backup & restore (or stays there, if Weft is already on it)."""
    d.open_app()
    if d.exists("Backup & restore", timeout=1) and not d.exists("Appearance", timeout=0.5):
        d.scroll_to_top()
        return
    d.nav("More")
    d.scroll_to_top()
    d.tap("Backups")
    assert d.exists("Backup & restore", timeout=3), "Backups didn't open Backup & restore"


@check
def backup_row_follows_drive():
    drive_state()
    d.open_app()
    d.nav("More")
    d.scroll_to_top()
    assert d.exists("Backups") and d.exists("Google Drive not connected"), "Backups row isn't 'not connected'"
    for gone in ["Export to file", "Import from file", "Back up now"]:
        assert not d.exists(gone, timeout=1), f"{gone!r} still on More"
    drive_state("tester@example.com", int(time.time() * 1000) - 3600_000)
    d.open_app()
    d.nav("More")
    d.scroll_to_top()
    assert d.exists("Last backup today, ", contains=True) or d.exists("Last backup yesterday, ", contains=True), \
        "Backups row doesn't show the last backup"
    drive_state("tester@example.com", failed="FULL")
    d.open_app()
    d.nav("More")
    d.scroll_to_top()
    assert d.exists("Last backup failed"), "Backups row doesn't show the failure"
    drive_state()


@check
def drive_not_connected():
    drive_state()
    backup_screen()
    for label in ["Back up to Google Drive", "Connect Google Drive", "Backup to a file", "Export to file", "Import from file"]:
        assert d.exists(label, timeout=3), f"Backup & restore (not connected) misses {label!r}"
    for label in ["Back up now", "Frequency", "Wi-Fi only", "Restore from Drive", "Google account"]:
        assert not d.exists(label, timeout=1), f"{label!r} shown before connecting"


@check
def drive_connected_rows():
    drive_state("tester@example.com", int(time.time() * 1000) - 3600_000, count=7)
    backup_screen()
    for label in ["Google Drive · tester@example.com", "Back up now", "Automatic backup", "Frequency", "Daily", "Wi-Fi only",
                  "Don't use mobile data", "Restore", "Restore from Drive", "Import from file", "JSON backup", "Other",
                  "Export to file", "Google account", "Switch account or disconnect"]:
        assert d.scroll_to(label), f"connected screen misses {label!r}"
    assert d.exists("7 backups, newest ", contains=True), "Restore from Drive doesn't count the backups"
    d.scroll_to_top()
    assert d.exists("Backed up today, ", contains=True) or d.exists("Backed up yesterday, ", contains=True), \
        "status card doesn't show the last backup"
    wifi = next(n for n in d.dump() if n.label == "Wi-Fi only")
    sw = next((n for n in d.dump() if n.checkable and n.bounds[1] <= wifi.center[1] <= n.bounds[3]), None)
    assert sw is None or sw.checked, "Wi-Fi only isn't on by default"


@check
def drive_failed_card():
    for reason, text in [("FULL", "Drive is full"), ("NEEDS_CONSENT", "Google needs you to allow Weft again"),
                         ("FAILED", "Couldn't reach Google Drive")]:
        drive_state("tester@example.com", failed=reason)
        backup_screen()
        assert d.exists("Last backup failed") and d.exists(text), f"failed card misses {text!r}"
        assert d.exists("Try again") and not d.exists("Back up now", timeout=1), "failed card has no Try again"
    drive_state("tester@example.com")


@check
def drive_frequency_sheet_schedules():
    drive_state("tester@example.com")
    backup_screen()
    d.tap("Frequency")
    for label in ["Backup frequency", "Every night while charging", "Sunday night", "On the 1st", "Only when you tap Back up now"]:
        assert d.exists(label, timeout=3), f"frequency sheet misses {label!r}"
    d.tap("Weekly")
    assert d.exists("Weekly"), "row didn't take the new frequency"
    jobs = d.shell("dumpsys jobscheduler")
    assert "/2000003101: " in jobs and ".data.backup.DriveBackupJob" in jobs, "no scheduled Drive backup job"
    d.tap("Frequency")
    d.tap("Off")
    time.sleep(1)
    # Pending jobs are listed as "JOB #u0a…/2000003101: …"; Off must leave none.
    jobs = d.shell("dumpsys jobscheduler")
    assert not re.search(r"JOB #u0a\d+/2000003101: ", jobs), "Off left a Drive job scheduled"
    d.tap("Frequency")
    d.tap("Daily")


@check
def drive_export_format_sheet():
    drive_state("tester@example.com")
    backup_screen()
    assert d.scroll_to("Export to file"), "Export to file missing"
    d.tap("Export to file")
    for label in ["File format", "JSON", "A backup you can import again", "CSV", "Opens in any spreadsheet"]:
        assert d.exists(label, timeout=3), f"format sheet misses {label!r}"
    d.back()


@check
def drive_account_sheet():
    drive_state("tester@example.com")
    backup_screen()
    assert d.scroll_to("Google account"), "Google account row missing"
    d.tap("Google account")
    for label in ["tester@example.com", "Switch account", "Disconnect", "Delete backups from Drive"]:
        assert d.exists(label, timeout=3), f"account sheet misses {label!r}"
    d.tap("Delete backups from Drive")
    assert d.exists("Delete backups from Drive?"), "delete has no confirmation"
    d.tap("Cancel")
    d.tap("Google account")
    d.tap("Disconnect")
    # Disconnect asks first (2026-11-11); there's no Undo for it.
    assert d.exists("Disconnect Google Drive?", timeout=4), "Disconnect didn't ask first"
    d.tap("Disconnect")
    assert d.exists("Connect Google Drive", timeout=6), "Disconnect didn't go back to Connect Google Drive"


@check
def drive_offline_says_so():
    drive_state("tester@example.com")
    d.shell("cmd connectivity airplane-mode enable")
    time.sleep(3)
    try:
        for label in ["Back up now", "Restore from Drive"]:
            backup_screen()
            assert d.scroll_to(label), f"{label} missing"
            d.tap(label)
            assert d.exists("No internet connection", contains=True, timeout=4), f"{label} offline didn't say so"
            assert label != "Restore from Drive" or not d.exists("Your current habits and notes", contains=True, timeout=1), \
                "restore sheet stayed open offline"
            assert not d.exists("Last backup failed", timeout=1), "an offline backup was marked failed"
            time.sleep(4.5)  # let the snackbar go
        drive_state()
        backup_screen()
        d.tap("Connect Google Drive")
        assert d.exists("No internet connection", contains=True, timeout=4), "Connect offline didn't say so"
    finally:
        d.shell("cmd connectivity airplane-mode disable")
        time.sleep(3)


@check
def drive_without_google_fails_cleanly():
    """No Play services or no OAuth client: a clear message, no crash, and the screen still works."""
    drive_state("tester@example.com")
    backup_screen()
    d.tap("Back up now")
    # One loop for both outcomes: the result is a short snackbar, gone before a second full read.
    seen = None
    end = time.time() + 25
    while time.time() < end and seen is None:
        for l in (n.label for n in d.dump()):
            if "Choose an account" in l or "Sign in" in l:
                seen = "google"
            elif "Couldn't reach Google Drive" in l or "Wi-Fi only is on" in l:
                seen = "message"
    if seen == "google":
        print("      skipped: Google's account screen opened (this phone has Google); check by hand")
        d.back()
        return
    assert seen == "message", "no result message after Back up now"
    assert d.exists("Backup & restore"), "app left Backup & restore"
    drive_state()


@check
def drive_absent_in_foss():
    """The foss APK (if built) has no Drive code."""
    apk = os.path.join(ROOT, "app/build/outputs/apk/foss/debug/app-foss-debug.apk")
    if not os.path.exists(apk):
        print("      skipped: no foss debug APK built")
        return
    import zipfile
    with zipfile.ZipFile(apk) as z:
        dex = b"".join(z.read(n) for n in z.namelist() if n.endswith(".dex"))
    assert b"googleapis.com/drive" not in dex and b"com/google/android/gms/auth" not in dex, "foss APK contains Drive code"


# --- Launcher shortcuts and weft:// links ---------------------------------------------------

def open_link(uri):
    d.shell(f"am start -W -a android.intent.action.VIEW -d {uri} {d.PACKAGE}")
    time.sleep(1.5)


@check
def links_open_each_screen():
    cases = [("weft://journal", "Filter by habit"), ("weft://insights", "Change date range"),
             ("weft://habits", "Manage habits"), ("weft://more", "Backups"), ("weft://today", "of today")]
    for uri, marker in cases:
        open_link(uri)
        assert d.exists(marker, contains=True, timeout=4), f"{uri} didn't open its tab"
    open_link("weft://note/new")
    assert d.exists("New note", timeout=4), "weft://note/new didn't open New note"
    d.tap("Close")
    open_link("weft://habit/new")
    assert d.exists("New habit", timeout=4), "weft://habit/new didn't open New habit"
    d.tap("Close")
    open_link("sprout://journal")  # the name before the rename to Weft still works
    assert d.exists("Filter by habit", contains=True, timeout=4), "sprout://journal didn't open the Journal"


@check
def links_ignore_bad_input():
    open_link("weft://today")
    for uri in ["weft://habit/999999", "weft://habit/abc", "weft://nothing", "weft://habit/-1"]:
        open_link(uri)
        assert d.exists("of today", contains=True, timeout=3) and not d.exists("Edit habit", timeout=1), f"{uri} opened something"


@check
def link_opens_a_habit():
    open_link("weft://habit/1")
    assert d.exists("Edit habit", timeout=4), "weft://habit/1 didn't open the habit page"
    d.back()


@check
def launcher_shortcuts():
    """Long-press Weft's icon: Add note and New habit, each opening its screen."""
    for name, screen in [("Add note", "New note"), ("New habit", "New habit")]:
        d.home()
        w, h = d.screen_size()
        d.swipe(w // 2, int(h * 0.85), w // 2, int(h * 0.2), 300)
        icon = d.find("Weft", timeout=4)
        assert icon, "Weft not in the launcher's app list"
        x, y = icon.center
        d.swipe(x, y, x, y, 1200)
        assert d.exists(name, timeout=4), f"long-press menu has no {name!r}"
        d.tap(name)
        assert d.exists(screen, timeout=6), f"shortcut {name!r} didn't open {screen}"
        d.tap("Close")


@check
def launcher_long_press_lists_widgets():
    """Long-press Weft's icon in the app list: the launcher's Widgets entry lists all four."""
    d.home()
    w, h = d.screen_size()
    d.swipe(w // 2, int(h * 0.85), w // 2, int(h * 0.2), 300)  # open all apps
    icon = d.find("Weft", timeout=4)
    assert icon, "Weft not in the launcher's app list"
    x, y = icon.center
    d.swipe(x, y, x, y, 1200)
    shortcut = d.find("Widgets", timeout=4)
    assert shortcut, "long-press menu has no Widgets entry"
    d.tap_xy(*shortcut.center)
    # Each entry is labelled "Today widget, 4 wide by 2 high …", even when only partly on screen.
    for name in ["This week", "Today", "Today strip", "Streak"]:
        assert d.exists(f"{name} widget, ", contains=True, timeout=4), f"long-press widget list misses {name}"
    d.home()


@check
def reminders_are_scheduled():
    alarms = d.shell("dumpsys alarm")
    assert "app.sprout.habits.action.REMINDER" in alarms, "no habit reminder alarms scheduled"


@check
def touch_targets_48dp():
    problems = []
    d.open_app()
    for tab in ["Today", "Habits", "Journal", "Insights", "More"]:
        d.nav(tab)
        problems += [f"{tab}: {p}" for p in d.small_targets()]
    assert not problems, "small touch targets: " + "; ".join(problems)


# --- 1.6.0: notification buttons and alarm reminders --------------------------------------------
# These read and set the database with sqlite3, so they need a rootable emulator (a "default"
# system image such as Sprout_API_35); elsewhere they are skipped. Run with animations off:
#   for k in window_animation_scale transition_animation_scale animator_duration_scale; do adb shell settings put global $k 0; done


class Skip(Exception):
    pass


DB = f"/data/data/{d.PACKAGE}/databases/sprout.db"
RECEIVER = f"{d.PACKAGE}/.notify.ReminderReceiver"
ACTIONS = f"{d.PACKAGE}/.notify.ReminderActionReceiver"


def root():
    if "root" not in d.shell("whoami"):
        d.adb("root")
        time.sleep(2)
        d.adb("wait-for-device")
    if "root" not in d.shell("whoami"):
        raise Skip("needs a rootable emulator")
    # Alarms fired here by broadcast aren't alarm-clock alarms, so Android wouldn't let them start
    # AlarmSoundService (a real one may) and would freeze the app; allow-listing Weft behaves like the real thing.
    d.shell(f"dumpsys deviceidle whitelist +{d.PACKAGE}")


def sql(query):
    return d.shell(f'sqlite3 {DB} "{query}"').strip()


def _epoch_day():
    import datetime
    y, m, dd = map(int, d.shell("date +%Y-%m-%d").strip().split("-"))
    return (datetime.date(y, m, dd) - datetime.date(1970, 1, 1)).days


def habit_id(name):
    return int(sql(f"select id from habit where name='{name}'"))


def entry(habit, day=None):
    return sql(f"select status, amount from entry where habitId={habit} and date={day or _epoch_day()}")


def make_alarm(name="Wake up at 7"):
    """The habit as an alarm reminder with today not logged; the app restarts so Room reads it fresh."""
    root()
    d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM allow")
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT allow")
    d.shell(f"am force-stop {d.PACKAGE}")
    # Today's snooze counts (3 a day, 2026-11-11) start from zero in every check.
    d.shell(f"rm -f /data/data/{d.PACKAGE}/shared_prefs/snoozes.xml")
    h = habit_id(name)
    sql(f"update habit set reminderType='ALARM', reminderMinutes=390 where id={h}; delete from entry where habitId={h} and date={_epoch_day()}")
    return h


def fire(h, action="app.sprout.habits.action.REMINDER"):
    d.shell(f"am broadcast -n {RECEIVER} -a {action} --el habitId {h}")
    time.sleep(3)


def press_action(h, action):
    d.shell(f"am broadcast -n {ACTIONS} -a app.sprout.habits.action.{action} --el habitId {h} --el day {_epoch_day()}")
    time.sleep(2)


def ringing():
    return "channel=alarms" in d.shell("dumpsys notification --noredact")


def sound_playing():
    return "AlarmSoundService" in d.shell("dumpsys activity services app.sprout.habits")


def top_activity():
    return d.shell("dumpsys activity activities | grep -m1 topResumedActivity")


def screen(on):
    d.shell("input keyevent " + ("KEYCODE_WAKEUP" if on else "KEYCODE_SLEEP"))
    time.sleep(1.5)
    if on:
        d.shell("wm dismiss-keyguard")
        time.sleep(1)


def find_ci(label, timeout=5.0):
    end = time.time() + timeout
    while time.time() < end:
        for n in d.dump():
            if n.label.lower() == label.lower():
                return n
        time.sleep(0.4)
    return None


@check
def reminder_sheet_offers_notification_and_alarm():
    root()
    d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM allow")
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT allow")
    d.open_app()
    d.nav("Today")
    d.tap("New habit")
    assert d.scroll_to("Reminder"), "no Reminder section in New habit"
    assert d.exists("Tap the reminder to change its time and type."), "hint under the reminder card missing"
    d.tap("tap to set a time", contains=True)
    for label in ("Reminder", "Notification", "Alarm", "Set reminder"):
        assert d.exists(label), f"reminder sheet is missing {label!r}"
    d.tap("Alarm")
    if d.exists("Set up alarms", timeout=1):
        d.back()  # something isn't allowed yet (1.6.2: Set up alarms opens over the sheet)
    d.tap("Set reminder")
    assert d.exists("Alarm · on the days above · tap to change", timeout=3), "card doesn't say Alarm after picking it"
    d.back()
    d.back()


@check
def alarm_heads_up_while_phone_in_use():
    h = make_alarm()
    screen(True)
    d.home()
    fire(h)
    assert ringing(), "alarm notification not posted"
    assert sound_playing(), "the alarm is silent"
    assert "AlarmActivity" not in top_activity(), "full screen opened while the phone was in use"
    # The heads-up hides after a few seconds: open the shade to reach its buttons.
    d.shell("cmd statusbar expand-notifications")
    time.sleep(1.5)
    n = find_ci("Dismiss")
    assert n, "no Dismiss button on the alarm notification"
    d.tap_xy(*n.center)
    time.sleep(1.5)
    d.shell("cmd statusbar collapse")
    assert not ringing(), "Dismiss didn't stop the alarm"
    assert not sound_playing(), "Dismiss didn't stop the sound"
    assert entry(h) == "", "Dismiss logged something"


@check
def alarm_full_screen_when_locked_and_slide_marks_done():
    h = make_alarm()
    screen(False)
    fire(h)
    assert "AlarmActivity" in top_activity(), "ringing screen didn't open over the lock screen"
    assert d.exists("Slide to mark done", timeout=5) and d.exists("Snooze 10 min") and d.exists("Dismiss"), "ringing screen incomplete"
    w, hgt = d.screen_size()
    bar = d.find("Slide to mark done")
    y = bar.center[1]
    d.swipe(int(w * 0.12), y, int(w * 0.95), y, 600)
    time.sleep(2)
    assert entry(h).startswith("DONE"), f"slide didn't mark done: {entry(h)!r}"
    assert not ringing(), "alarm still ringing after slide"
    screen(True)


@check
def alarm_snooze_rings_again_later():
    h = make_alarm()
    screen(False)
    fire(h)
    assert d.exists("Snooze 10 min", timeout=5), "no Snooze on the ringing screen"
    d.tap("Snooze 10 min")
    time.sleep(2)
    assert not ringing(), "Snooze didn't stop the alarm"
    assert "REMINDER_SNOOZED" in d.shell("dumpsys alarm"), "no snooze alarm scheduled"
    assert entry(h) == "", "Snooze logged something"
    fire(h, "app.sprout.habits.action.REMINDER_SNOOZED")
    assert ringing(), "snoozed alarm didn't ring again"
    press_action(h, "ALARM_DISMISS")
    assert not ringing()
    screen(True)


@check
def alarm_snooze_stops_after_three():
    """User, 2026-11-11: an alarm snoozes three times a day; the fourth ring offers Dismiss (and
    sliding it done, Skip today) but no Snooze, and a Snooze from an old card counts as Dismiss."""
    h = make_alarm()
    screen(False)
    for i in range(3):
        # Locked each time, as it would be: on an unlocked phone the alarm is a heads-up card instead.
        screen(False)
        fire(h) if i == 0 else fire(h, "app.sprout.habits.action.REMINDER_SNOOZED")
        assert ringing(), f"ring {i + 1} didn't start"
        assert d.exists("Snooze 10 min", timeout=5), f"no Snooze on ring {i + 1} of 3"
        d.tap("Snooze 10 min")
        time.sleep(2)
        assert not ringing(), f"Snooze {i + 1} didn't stop the alarm"
    screen(False)
    fire(h, "app.sprout.habits.action.REMINDER_SNOOZED")
    assert ringing(), "the third snooze didn't ring again"
    assert d.exists("Dismiss", timeout=5), "no Dismiss after three snoozes"
    assert d.exists("Slide to mark done"), "the slide bar went with Snooze"
    assert not d.exists("Snooze 10 min", timeout=1), "Snooze offered a fourth time"
    # A Snooze press from an old card past the cap is a Dismiss.
    press_action(h, "REMINDER_SNOOZE")
    assert not ringing(), "a fourth Snooze kept the alarm ringing"
    assert entry(h) == "", "the capped Snooze logged something"
    screen(True)


@check
def alarm_volume_and_power_keys_silence():
    h = make_alarm()
    screen(False)
    fire(h)
    assert "AlarmActivity" in top_activity()
    assert sound_playing(), "alarm sound didn't start"
    # Volume only stops the sound: the ringing screen and its card stay to be answered.
    d.shell("input keyevent KEYCODE_VOLUME_DOWN")
    time.sleep(2)
    assert not sound_playing(), "volume key didn't stop the sound"
    assert "AlarmActivity" in top_activity(), "volume key closed the ringing screen"
    assert ringing(), "volume key dismissed the alarm"
    press_action(h, "ALARM_DISMISS")
    assert not ringing()
    # Also as a heads-up (screen on and unlocked): the power button still silences it.
    screen(True)
    d.home()
    fire(h)
    assert ringing()
    d.shell("input keyevent KEYCODE_SLEEP")
    time.sleep(2)
    assert not ringing(), "power button didn't silence the alarm"
    assert entry(h) == "", "silencing logged something"
    screen(True)


@check
def alarm_does_not_ring_when_done_or_skipped():
    h = make_alarm()
    for status in ("DONE", "SKIP"):
        sql(f"insert or replace into entry(habitId, date, status, amount) values({h}, {_epoch_day()}, '{status}', 1)")
        fire(h)
        assert not ringing(), f"alarm rang although today is {status}"
    sql(f"delete from entry where habitId={h} and date={_epoch_day()}")


@check
def alarm_unanswered_posts_missed():
    h = make_alarm()
    screen(True)
    d.home()
    fire(h)
    assert ringing()
    fire(h, "app.sprout.habits.action.ALARM_TIMEOUT")
    assert not ringing(), "alarm kept ringing after its 10 minutes"
    assert "missed at" in d.shell("dumpsys notification --noredact"), "no missed-alarm notification"


@check
def alarm_falls_back_without_full_screen_permission():
    h = make_alarm()
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT deny")
    try:
        screen(False)
        fire(h)
        assert "AlarmActivity" not in top_activity(), "full screen opened without the permission"
        assert ringing(), "no heads-up alarm without the permission"
        press_action(h, "ALARM_DISMISS")
        assert not ringing()
    finally:
        d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT allow")
        screen(True)


@check
def alarm_without_exact_alarms_opens_setup():
    root()
    # Off the battery allow-list (root() puts it there): Android lets allow-listed apps set exact alarms.
    d.shell(f"dumpsys deviceidle whitelist -{d.PACKAGE}")
    d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM deny")
    d.shell(f"am force-stop {d.PACKAGE}")
    try:
        d.open_app()
        d.nav("Today")
        d.tap("New habit")
        d.scroll_to("Reminder")
        d.tap("tap to set a time", contains=True)
        d.tap("Alarm")
        # 1.6.2: picking Alarm opens Set up alarms with every need, not a settings page.
        assert d.exists("Set up alarms", timeout=3), "picking Alarm without exact alarms doesn't open Set up alarms"
        assert d.exists("Ring alarms on time"), "Set up alarms has no Ring alarms on time row"
        assert d.exists("Use a notification instead"), "Set up alarms offers no way out while something is missing"
        d.back()  # keep Alarm: the card warns
        d.tap("Set reminder")
        assert d.exists("may ring late · tap to fix", contains=True, timeout=3), "Reminder card doesn't warn about exact alarms"
        d.tap("may ring late · tap to fix", contains=True)
        assert d.exists("Set up alarms", timeout=3), "the warning doesn't open Set up alarms"
        d.tap("Use a notification instead")
        assert d.exists("Notification · on the days above · tap to change", timeout=3), "Use a notification instead didn't switch the reminder"
        d.back()
    finally:
        d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM allow")


@check
def more_reminders_alarms_screen():
    """More → Reminders & alarms (design 2026-11-09): one row, then Reminders, Allow Weft to and Sound."""
    d.open_app()
    d.nav("More")
    assert not d.exists("Default reminder", timeout=1), "Default reminder is still in More → General"
    assert d.scroll_to("Reminders & alarms"), "no Reminders & alarms row in More"
    d.tap("Reminders & alarms")
    for label in ("Reminders", "Default reminder", "Allow Weft to", "Send notifications", "Ring alarms on time",
                  "Show alarms full screen", "Sound", "Alarm sound", "Vibrate", "Notification sound"):
        assert d.scroll_to(label), f"Reminders & alarms is missing {label!r}"
    d.tap("Alarm sound")
    for label in ("Weft", "Weft · Soft", "Phone’s alarm sound", "Choose from phone"):
        assert d.exists(label, timeout=3), f"Alarm sound sheet is missing {label!r}"
    d.tap("Weft · Soft")
    assert d.exists("Playing", timeout=2), "tapping a sound doesn't preview it"
    d.back()
    assert d.exists("Weft · Soft", timeout=2), "the Alarm sound row doesn't show the pick"
    d.tap("Weft · Soft")
    d.tap("Weft")
    d.back()
    d.back()


@check
def alarm_is_an_alarm_clock_and_survives_reboot():
    make_alarm()
    d.open_app()  # the scheduler applies the alarm type on start
    time.sleep(3)
    d.shell(f"am broadcast -a android.intent.action.BOOT_COMPLETED -n {d.PACKAGE}/.notify.RescheduleReceiver")
    time.sleep(3)
    alarms = d.shell("dumpsys alarm")
    # An alarm clock carries a showIntent (what the status bar's clock opens): Weft's.
    assert f"{d.PACKAGE} startActivity" in alarms, "the alarm reminder isn't scheduled as an alarm clock"


@check
def late_reminder_switches_phone_to_alarm_clocks():
    """Any phone that holds back an exact reminder (not only the known makers) gets every reminder
    as an alarm clock from then on (user, 2026-11-09)."""
    root()
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell(f"rm -f /data/data/{d.PACKAGE}/shared_prefs/device.xml")
    sql("update habit set reminderType='NOTIFICATION', reminderMinutes=1200")
    d.open_app()
    time.sleep(3)
    assert f"{d.PACKAGE} startActivity" not in d.shell("dumpsys alarm"), "reminders were alarm clocks before any came late"
    read = habit_id("Read")
    late = int(time.time() * 1000) - 5 * 60_000
    d.shell(f"am broadcast -n {RECEIVER} -a app.sprout.habits.action.REMINDER --el habitId {read} --el exactAt {late}")
    time.sleep(3)
    assert f"{d.PACKAGE} startActivity" in d.shell("dumpsys alarm"), "a late reminder didn't switch reminders to alarm clocks"
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell(f"rm -f /data/data/{d.PACKAGE}/shared_prefs/device.xml")
    d.shell("cmd notification cancel_all 2>/dev/null")


@check
def notification_buttons_log_and_offer_undo():
    root()
    d.shell(f"am force-stop {d.PACKAGE}")
    read, vit = habit_id("Read"), habit_id("Vitamins")
    sql(f"update habit set reminderType='NOTIFICATION' where id in ({read},{vit}); delete from entry where habitId in ({read},{vit}) and date={_epoch_day()}")
    d.home()
    fire(read)
    press_action(read, "REMINDER_ADD_STEP")
    assert entry(read).startswith("PARTIAL|1"), f"+step didn't log one step: {entry(read)!r}"
    assert "to go" in d.shell("dumpsys notification --noredact"), "reminder didn't update to the new progress"
    fire(vit)
    press_action(vit, "REMINDER_DONE")
    assert entry(vit).startswith("DONE"), "Done button didn't mark done"
    d.open_app()
    # The Undo bar shows for about 4 s: tap it first, then check what it undid.
    d.tap("Undo", timeout=6)
    for _ in range(10):  # the write lands a moment after the tap
        if entry(vit) == "":
            break
        time.sleep(0.5)
    assert entry(vit) == "", "Undo didn't put the day back"
    # A notification reminder has Dismiss, not Snooze (only alarms snooze; user, 2026-11-08).
    press_action(read, "ALARM_DISMISS")  # alone in the shade, its buttons show
    d.home()
    fire(vit)
    d.shell("cmd statusbar expand-notifications")
    time.sleep(1.5)
    labels = [n.label.lower() for n in d.dump()]
    assert not any(l.startswith("snooze") for l in labels), "a notification reminder offers Snooze"
    n = find_ci("Dismiss")
    assert n, "no Dismiss on the notification reminder"
    d.tap_xy(*n.center)
    time.sleep(1.5)
    d.shell("cmd statusbar collapse")
    assert "Vitamins" not in d.shell("dumpsys notification --noredact | grep -A3 'pkg=app.sprout.habits'"), "Dismiss didn't clear the reminder"
    assert entry(vit) == "", "Dismiss logged something"


def sound_playing():
    """True when something plays on the alarm stream right now (the alarm can be heard). Alarms
    fired here by a broadcast ring through the fallback Ringtone (Android doesn't let a broadcast
    start the sound service); real alarm-clock alarms ring through AlarmSoundService."""
    out = d.shell("dumpsys audio")
    return any("USAGE_ALARM" in l and "state:started" in l for l in out.splitlines())


def alarm_cards():
    """How many alarm notifications are posted (alarms ringing together are one card)."""
    out = d.shell("dumpsys notification --noredact")
    return sum(1 for l in out.splitlines() if "NotificationRecord(" in l and d.PACKAGE in l and "channel=alarms_popup" in l)


def make_alarms(*names):
    root()
    d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM allow")
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT allow")
    d.shell(f"am force-stop {d.PACKAGE}")
    ids = [habit_id(n) for n in names]
    sql(f"update habit set reminderType='ALARM', reminderMinutes=390 where id in ({','.join(map(str, ids))}); "
        f"delete from entry where habitId in ({','.join(map(str, ids))}) and date={_epoch_day()}")
    return ids


@check
def alarm_check_off_skip_today():
    h = make_alarm("Wake up at 7")
    screen(False)
    fire(h)
    assert d.exists("Skip today", timeout=5), "no Skip today on a check-off alarm"
    assert not d.exists("Log amount", timeout=1), "Log amount on a check-off alarm"
    assert d.exists("Open Weft"), "no Open Weft"
    d.tap("Skip today")
    time.sleep(2)
    assert entry(h).startswith("SKIP"), f"Skip today didn't log Skip: {entry(h)!r}"
    assert not ringing(), "alarm still ringing after Skip today"
    screen(True)


@check
def alarm_measured_log_amount():
    h = make_alarm("Read")
    screen(False)
    fire(h)
    assert d.exists("Goal 20 pages", timeout=5), "no goal line on a measured alarm"
    d.tap("Log amount")
    time.sleep(1.5)
    assert not sound_playing(), "Log amount didn't stop the sound"
    assert d.exists("Save") and d.exists("Partial"), "amount sheet didn't open"
    # Cancel: back to the ringing screen, still quiet, nothing logged.
    d.tap("Cancel")
    time.sleep(1.5)
    assert d.exists("Slide to mark done"), "Cancel didn't return to the ringing screen"
    assert not sound_playing() and entry(h) == "", "Cancel rang again or logged"
    # Log amount again: + three times, Save logs Partial and closes the alarm.
    d.tap("Log amount")
    time.sleep(1.5)
    for _ in range(3):
        d.tap("More")
    d.tap("Save")
    time.sleep(2)
    assert entry(h).startswith("PARTIAL|3"), f"Save didn't log 3 pages: {entry(h)!r}"
    assert not ringing(), "alarm still there after Save"
    screen(True)


@check
def alarm_open_weft():
    h = make_alarm("Read")
    screen(False)
    fire(h)
    assert d.exists("Open Weft", timeout=5)
    d.tap("Open Weft")
    time.sleep(3)
    assert not ringing(), "Open Weft didn't stop the alarm"
    assert "MainActivity" in top_activity(), "Open Weft didn't open the app"
    assert d.exists("Every day · 20 pages", timeout=5) or d.exists("Read", timeout=1), "the app didn't open on the habit"
    assert entry(h) == "", "Open Weft logged something"
    d.back()


@check
def alarms_at_the_same_time_queue():
    wake, workout = make_alarms("Wake up at 7", "Workout")
    screen(False)
    fire(wake)
    fire(workout)
    assert sound_playing() and alarm_cards() == 1, f"not one ringing alarm: sound {sound_playing()}, cards {alarm_cards()}"
    assert d.exists("1 of 2", timeout=5), "no 1 of 2 chip"
    assert d.exists("Next: Workout"), "no Next line"
    assert d.exists("Wake up at 7"), "first habit (in list order) not on screen"
    # Answer the first: the second takes the screen at once, without ringing again.
    d.tap("Dismiss")
    time.sleep(2)
    assert d.exists("Workout", timeout=5) and d.exists("Slide to mark done"), "next alarm didn't take the screen"
    assert d.exists("2 of 2"), "no 2 of 2 chip"
    assert not d.exists("Next:", contains=True, timeout=1), "Next line on the last alarm"
    assert not sound_playing(), "the next alarm rang again"
    assert "AlarmActivity" in top_activity(), "screen closed with an alarm left"
    d.tap("Snooze 10 min")
    time.sleep(2)
    assert not ringing(), "alarms left after answering both"
    assert "REMINDER_SNOOZED" in d.shell("dumpsys alarm"), "Snooze on the last one didn't snooze"
    assert entry(wake) == "" and entry(workout) == ""
    screen(True)


@check
def alarms_at_the_same_time_while_phone_in_use():
    """User, 2026-11-08: two alarms at 6:45 came as two silent cards. One card, and it rings."""
    wake, workout = make_alarms("Wake up at 7", "Workout")
    screen(True)
    d.home()
    fire(workout)
    fire(wake)
    assert alarm_cards() == 1, f"alarms at the same time aren't one card: {alarm_cards()}"
    assert sound_playing(), "the alarm is silent"
    out = d.shell("dumpsys notification --noredact")
    assert "1 of 2" in out and "Next: Workout" in out, "the card doesn't say 1 of 2 · Next: Workout"
    d.shell("cmd statusbar expand-notifications")
    time.sleep(1.5)
    for label in ("Done", "Snooze 10 min", "Dismiss"):
        assert d.exists(label), f"alarm card is missing {label!r}"
    d.tap("Dismiss")
    time.sleep(2)
    assert alarm_cards() == 1 and ringing(), "the next alarm didn't show after Dismiss"
    assert not sound_playing(), "the next alarm rang again"
    assert "2 of 2" in d.shell("dumpsys notification --noredact"), "the next card doesn't say 2 of 2"
    d.tap("Dismiss")
    time.sleep(2)
    d.shell("cmd statusbar collapse")
    assert not ringing(), "alarms left after answering both"
    assert entry(wake) == "" and entry(workout) == ""


def reminder_records():
    """The posted reminder notifications' record lines. A quiet one (NotificationCompat.setSilent)
    is in the "silent" group, which never alerts: groupKey=silent."""
    out = d.shell("dumpsys notification --noredact")
    return [l for l in out.splitlines() if "NotificationRecord(" in l and d.PACKAGE in l and "channel=reminders_popup" in l]


@check
def notification_and_alarm_at_the_same_time():
    """User, 2026-11-11: a notification reminder and another habit's alarm at the same minute both
    popped up, a second apart. The alarm alone pops up and sounds; the reminder waits in the shade
    quietly, whichever arrives first."""
    (wake,) = make_alarms("Wake up at 7")
    read = habit_id("Read")
    sql(f"update habit set reminderType='NOTIFICATION', reminderMinutes=390 where id={read}; "
        f"delete from entry where habitId={read} and date={_epoch_day()}")
    screen(True)
    d.home()
    # The notification a second before the alarm (what the user saw).
    fire(read)
    records = reminder_records()
    assert records, "the reminder isn't in the shade"
    assert "groupKey=silent" in records[0], f"the reminder popped up although an alarm is due now: {records[0]}"
    fire(wake)
    assert alarm_cards() == 1 and sound_playing(), "the alarm didn't ring"
    d.shell("cmd statusbar expand-notifications")
    time.sleep(1.5)
    d.tap("Dismiss")
    time.sleep(2)
    d.shell("cmd statusbar collapse")
    # The other order: the alarm is ringing when the reminder arrives.
    d.shell(f"am force-stop {d.PACKAGE}")
    fire(wake)
    assert sound_playing(), "the alarm didn't ring the second time"
    fire(read)
    records = reminder_records()
    assert records and "groupKey=silent" in records[0], f"the reminder popped up over a ringing alarm: {records}"
    d.shell("cmd statusbar expand-notifications")
    time.sleep(1.5)
    d.tap("Dismiss")
    time.sleep(2)
    d.shell("cmd statusbar collapse")
    # Alone, the same reminder still pops up.
    d.shell(f"am force-stop {d.PACKAGE}")
    sql(f"update habit set reminderType='NOTIFICATION' where id={wake}; delete from entry where habitId={wake} and date={_epoch_day()}")
    fire(read)
    records = reminder_records()
    assert records and "groupKey=silent" not in records[0], f"a reminder with no alarm due was quiet: {records}"
    assert entry(wake) == "" and entry(read) == ""


@check
def alarms_power_button_silences_all():
    wake, workout = make_alarms("Wake up at 7", "Workout")
    screen(True)
    d.home()
    fire(wake)
    fire(workout)
    # Both ring as one card (the one on top says "1 of 2"); the power button silences both.
    assert alarm_cards() == 1 and sound_playing(), "the two alarms aren't one ringing card"
    d.shell("input keyevent KEYCODE_SLEEP")
    time.sleep(2.5)
    assert not ringing() and not sound_playing(), "the power button left an alarm ringing"
    screen(True)


@check
def alarm_stops_when_logged_in_app_partial():
    h = make_alarm("Read")
    screen(True)
    d.home()
    fire(h)
    assert ringing()
    # A Partial log in the app: Today → hold Read → + → Save.
    d.open_app()
    d.nav("Today")
    d.long_press("Read")
    d.tap("More")
    d.tap("Save")
    time.sleep(2.5)
    assert entry(h).startswith("PARTIAL"), f"not logged as Partial: {entry(h)!r}"
    assert not ringing(), "a Partial log in the app didn't stop the alarm"
    sql(f"delete from entry where habitId={h} and date={_epoch_day()}")


@check
def alarm_still_rings_when_partly_done():
    h = make_alarm("Read")
    sql(f"insert or replace into entry(habitId, date, status, amount, loggedAt) values({h}, {_epoch_day()}, 'PARTIAL', 5, 1000)")
    d.open_app()
    time.sleep(2)
    screen(False)
    fire(h)
    assert ringing(), "a partly done habit didn't ring"
    press_action(h, "ALARM_DISMISS")
    sql(f"delete from entry where habitId={h} and date={_epoch_day()}")
    screen(True)


@check
def get_started_opposite_swipe_ticks_undo():
    """Get started's Undo row is taught by the opposite swipe, as on Today (user, 2026-11-08)."""
    d.shell(f"pm clear {d.PACKAGE}")
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    d.tap("Get started", timeout=20)
    d.tap("Skip for now", timeout=10)
    assert d.exists("Changed your mind? Swipe the other way", timeout=10), "Undo row doesn't teach the opposite swipe"
    w, _ = d.screen_size()
    n = d.find("Read")
    d.swipe(int(w * 0.15), n.center[1], int(w * 0.9), n.center[1], 250)  # right: Done
    time.sleep(5)  # let the Undo bar go, so only the swipe can tick Undo
    n = d.find("Read")
    d.swipe(int(w * 0.9), n.center[1], int(w * 0.15), n.center[1], 250)  # back: undo
    time.sleep(1)
    assert d.exists("Mark Read done", timeout=3), "the opposite swipe didn't put the card back"
    # Two rows ticked now: Swipe right, and Undo (the opposite swipe isn't a Swipe left).
    ticks = [x for x in d.dump() if x.label == "Tried"]
    assert len(ticks) == 2, f"expected Swipe right and Undo ticked, got {len(ticks)}"
    d.tap("Go to Today", timeout=5) if d.exists("Go to Today", timeout=1) else recover()


def recover():
    """After a failure, back out of sheets and inner screens to the tabs so later checks run."""
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    # A failure during Get started leaves the app there after a restart: walk through it, keeping the samples.
    if d.exists("Get started", timeout=2):
        d.tap("Get started")
        d.tap("Skip for now", timeout=10)
        d.tap("Go to Today", timeout=10)
    for _ in range(4):
        if d.exists("Today", timeout=1) and d.exists("Insights", timeout=1):
            return
        d.back()


def main(filters):
    selected = [c for c in CHECKS if not filters or any(f in c.__name__ for f in filters)]
    if not os.path.exists(APK):
        sys.exit(f"Build first: ./gradlew :app:assemblePlayDebug (missing {APK})")
    fresh_install()
    if welcome_shows_once not in selected and d.exists("Get started", timeout=3):
        d.tap("Get started")
        d.tap("Skip for now")
        d.tap("Go to Today")
    failures = 0
    for c in selected:
        try:
            c()
            print(f"PASS  {c.__name__}")
        except Skip as e:
            print(f"SKIP  {c.__name__}: {e}")
        except Exception as e:  # noqa: BLE001 - report and continue
            failures += 1
            print(f"FAIL  {c.__name__}: {e}")
            # FAIL_SHOTS=<dir>: keep a screenshot of what the screen showed when the check failed.
            if os.environ.get("FAIL_SHOTS"):
                with open(os.path.join(os.environ["FAIL_SHOTS"], c.__name__ + ".png"), "wb") as f:
                    serial = ["-s", os.environ["ANDROID_SERIAL"]] if os.environ.get("ANDROID_SERIAL") else []
                    f.write(d.subprocess.run([d.ADB, *serial, "exec-out", "screencap", "-p"], capture_output=True).stdout)
            if os.environ.get("VERBOSE"):
                traceback.print_exc()
            recover()
    print(f"\n{len(selected) - failures}/{len(selected)} passed")
    return failures


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
