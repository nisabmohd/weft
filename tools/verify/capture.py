#!/usr/bin/env python3
"""Recaptures the design PNGs in design/ from the app, the way they have been taken since 2026-10-19.

Sample data on Sunday 27 Sep, the status bar at 9:30 (demo mode), the default Rust accent and Lexend,
1080 x 2400 at 420 dpi. Needs a rootable emulator (a "default" system image, e.g. Sprout_API_35) to set
the date, and the play debug build (it has the debug-only SheetPreviewActivity and DriveStateActivity).

    ./gradlew :app:assemblePlayDebug
    python3 tools/verify/capture.py              # every screen this script knows
    python3 tools/verify/capture.py insights     # only the names that contain these words
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(__file__))
import device as d  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
APK = os.path.join(ROOT, "app/build/outputs/apk/play/debug/app-play-debug.apk")
# CAPTURE_DIR=<dir> saves elsewhere, e.g. to compare with a mockup before replacing it.
DESIGN = os.environ.get("CAPTURE_DIR") or os.path.join(ROOT, "design")
# The Google account shown on the Backup & restore captures.
ACCOUNT = "alex.rivera@gmail.com"

SCREENS = []


def screen(fn):
    SCREENS.append(fn)
    return fn


def save(name):
    """Saves the screen as design/<name>.png once the UI has settled."""
    time.sleep(1.2)
    with open(os.path.join(DESIGN, name + ".png"), "wb") as f:
        f.write(d.subprocess.run([d.ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print("saved", name)


def save_full_length(name, height):
    """A long screen in one capture: the display is made [height] px tall for the shot."""
    d.shell(f"wm size 1080x{height}")
    time.sleep(2.5)
    save(name)
    d.shell("wm size reset")
    time.sleep(2)


def demo_status_bar():
    # Gesture navigation (the thin handle), as on every earlier capture.
    d.shell("cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural")
    time.sleep(2)
    d.shell("settings put global sysui_demo_allowed 1")
    for cmd in (
        "-e command enter",
        "-e command clock -e hhmm 0930",
        "-e command battery -e level 100 -e plugged false",
        "-e command network -e wifi show -e level 4 -e fully true -e mobile hide",
        "-e command notifications -e visible false",
    ):
        d.shell(f"am broadcast -a com.android.systemui.demo {cmd}")


def setup():
    root = d.adb("root")
    assert "cannot run as root" not in root, "needs a rootable emulator image to set the date"
    time.sleep(2)
    d.adb("wait-for-device")
    d.shell("settings put global auto_time 0")
    d.shell("date 092709302026.00")  # Sunday 27 Sep 2026, 9:30
    demo_status_bar()
    d.adb("uninstall", d.PACKAGE)
    out = d.adb("install", APK)
    assert "Success" in out, f"install failed: {out}"
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    # Get started → Skip for now keeps the sample habits.
    d.tap("Get started", timeout=20)
    d.tap("Skip for now", timeout=10)
    if d.exists("Go to Today", timeout=4):
        d.tap("Go to Today")
    assert d.exists("Today", timeout=10)
    time.sleep(1.5)


def teardown():
    d.shell("am broadcast -a com.android.systemui.demo -e command exit")
    d.shell("settings put global auto_time 1")


# --- screens ----------------------------------------------------------------------------------


@screen
def welcome():
    """The first screen: the app's data is cleared (the sample data comes back on the next launch)."""
    d.shell(f"pm clear {d.PACKAGE}")
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    assert d.exists("Get started", timeout=20)
    save("welcome")
    d.tap("Get started")
    d.tap("Skip for now", timeout=10)
    if d.exists("Go to Today", timeout=4):
        d.tap("Go to Today")
    assert d.exists("Today", timeout=10)


@screen
def more():
    d.nav("More")
    d.scroll_to_top()
    save_full_length("more", 3800)


@screen
def about():
    d.nav("More")
    d.scroll_to("About Weft")
    d.tap("About Weft")
    time.sleep(1)
    save_full_length("about", 2778)
    d.back()


# design/navigation-bar.png is cut from the 2026-11-08 mockup (insights.png), not captured.


# design/notifications.png is a mockup (rendered from its HTML), not captured: there is no lock screen to capture it on.


@screen
def amount_sheet_duration():
    d.nav("Today")
    d.long_press("Workout")
    save("amount-sheet-duration")
    d.back()


@screen
def support_prompt_sheet():
    d.nav("Today")
    d.shell(f"am start -n {d.PACKAGE}/.SheetPreviewActivity --es sheet support")
    save("support-prompt-sheet")
    d.back()


@screen
def update_sheet():
    d.nav("Today")
    d.shell(f"am start -n {d.PACKAGE}/.SheetPreviewActivity --es sheet update")
    save("update-sheet")
    d.back()


@screen
def edit_day_sheet():
    d.nav("Habits")
    d.tap("Week")
    # Read's mark for Saturday 26 Sep (a partial day with a note in the sample data).
    marks = [n for n in d.dump() if n.label.startswith("Read") and "Saturday" in n.label]
    if not marks:
        marks = [n for n in d.dump() if "Saturday, 26 Sep" in n.label]
    assert marks, "no Saturday mark for Read"
    d.tap_xy(*marks[0].center)
    save("edit-day-sheet")
    d.back()


@screen
def note_actions_sheet():
    d.nav("Journal")
    d.long_press("Only 8 pages", contains=True)
    save("note-actions-sheet")
    d.back()


@screen
def drive_account_sheet():
    # The device's clock (27 Sep), not this computer's: "Backed up today, 9:12 AM".
    now = int(d.shell("date +%s").strip())
    hour_ago = (now - 18 * 60) * 1000
    d.shell(f"am start -n {d.PACKAGE}/.DriveStateActivity --es account {ACCOUNT} --el lastBackupAt {hour_ago} --ei count 7")
    time.sleep(1)
    d.nav("More")
    d.tap("Backups", contains=True)
    d.scroll_to("Google account")
    d.tap("Google account")
    save("drive-account-sheet")
    d.back()
    d.back()


@screen
def filter_sheet():
    d.nav("Journal")
    d.tap("Filter by habit")
    save("filter-sheet")
    d.back()


@screen
def journal_empty():
    """The Journal filtered to Vitamins, which has no notes (design 2026-11-09)."""
    d.nav("Journal")
    d.tap("Filter by habit")
    d.tap("Vitamins")
    d.tap("Show notes for 1 habit", contains=True)
    assert d.exists("No Vitamins notes yet", timeout=4)
    save("journal-empty")
    d.tap("Show all notes")


@screen
def date_range_sheet():
    d.nav("Journal")
    d.tap("Filter by date")
    save("date-range-sheet")
    d.back()


@screen
def reminder_time_sheet():
    d.nav("More")
    d.scroll_to("Reminders & alarms")
    d.tap("Reminders & alarms")
    d.tap("Default reminder")
    save("reminder-time-sheet")
    d.back()
    d.back()


@screen
def icon_picker():
    d.nav("Today")
    d.tap("New habit")
    d.tap("More icons", contains=True, timeout=5)
    save("icon-picker")
    d.back()
    d.back()


@screen
def drive_restore_sheet():
    now = int(d.shell("date +%s").strip())
    d.shell(f"am start -n {d.PACKAGE}/.DriveStateActivity --es account {ACCOUNT} --el lastBackupAt {(now - 18 * 60) * 1000} --ei count 7")
    time.sleep(1)
    d.nav("More")
    d.tap("Backups", contains=True)
    d.tap("Restore from Drive")
    save("drive-restore-sheet")
    d.back()
    d.back()


@screen
def habit_detail():
    """Read's page (edit, archive and delete buttons; recaptured 2026-11-08 for the new Edit / Archive icons)."""
    db = f"/data/data/{d.PACKAGE}/databases/sprout.db"
    hid = d.shell(f'sqlite3 {db} "select id from habit where name=\'Read\'"').strip()
    d.shell(f"am start -a android.intent.action.VIEW -d weft://habit/{hid} {d.PACKAGE}")
    assert d.exists("Read", timeout=8)
    save("habit-detail")
    d.back()


@screen
def new_habit():
    d.nav("Today")
    d.tap("New habit")
    save("new-habit")
    d.back()


@screen
def alarm_ringing():
    """The ringing screen for Wake up at 7 (made an alarm at 6:30), opened over the lock screen."""
    db = f"/data/data/{d.PACKAGE}/databases/sprout.db"
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell(f'sqlite3 {db} "update habit set reminderType=\'ALARM\', reminderMinutes=390 where name=\'Wake up at 7\'"')
    day = d.shell("date +%s").strip()
    hid = d.shell(f'sqlite3 {db} "select id from habit where name=\'Wake up at 7\'"').strip()
    d.shell(f'sqlite3 {db} "delete from entry where habitId={hid} and date={int(day) // 86400}"')
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT allow")
    d.shell("date 092706302026.00")  # 6:30, so the screen shows the alarm's own time
    demo_status_bar()
    d.shell("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0630")
    d.shell("input keyevent KEYCODE_SLEEP")
    time.sleep(1.5)
    d.shell(f"am broadcast -n {d.PACKAGE}/.notify.ReminderReceiver -a app.sprout.habits.action.REMINDER --el habitId {hid}")
    time.sleep(4)
    save("alarm-3-ringing")
    d.shell(f"am broadcast -n {d.PACKAGE}/.notify.ReminderActionReceiver -a app.sprout.habits.action.ALARM_DISMISS --el habitId {hid}")
    d.shell("date 092709302026.00")
    d.shell("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930")
    d.shell("input keyevent KEYCODE_WAKEUP")
    d.shell("wm dismiss-keyguard")
    time.sleep(1.5)


def alarm_permissions(notifications, exact, full_screen, restart=True):
    """Sets what alarms are allowed (1.6.2's Set up alarms), on a phone that passes for a OnePlus."""
    if restart:
        d.shell(f"am force-stop {d.PACKAGE}")
        d.shell("setprop debug.weft.maker oneplus")
    d.shell(f"pm {'grant' if notifications else 'revoke'} {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    # Off the battery allow-list (root puts the app there): allow-listed apps may always set exact alarms.
    d.shell(f"dumpsys deviceidle whitelist -{d.PACKAGE}")
    d.shell(f"appops set {d.PACKAGE} SCHEDULE_EXACT_ALARM {'allow' if exact else 'deny'}")
    d.shell(f"appops set {d.PACKAGE} USE_FULL_SCREEN_INTENT {'allow' if full_screen else 'deny'}")
    if restart:
        d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
        time.sleep(1.5)
    else:
        # Weft re-reads them when it comes back to the front, as after the system settings page.
        d.shell("input keyevent KEYCODE_HOME")
        time.sleep(1)
        d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
        time.sleep(1.5)


def alarm_permissions_reset():
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell("setprop debug.weft.maker ''")
    alarm_permissions(True, True, True, restart=False)


@screen
def alarm_setup():
    """Set up alarms (alarm-9a/b/c) and the Reminder card warning (alarm-10), from New habit."""
    alarm_permissions(False, False, False)
    d.nav("Today")
    d.tap("New habit")
    d.scroll_to("tap to set a time", contains=True)
    d.tap("tap to set a time", contains=True)
    d.tap("Alarm")  # nothing is allowed: Set up alarms opens over the Reminder sheet
    assert d.exists("Set up alarms", timeout=5)
    d.back()
    d.tap("Set reminder")
    # Set reminder asks for notifications; say no, to keep "nothing allowed".
    if d.exists("DON’T ALLOW", timeout=3):
        d.tap("DON’T ALLOW")
    # The warning opens it over New habit itself, as in the design.
    d.scroll_to("tap to fix", contains=True)
    d.tap("tap to fix", contains=True)
    assert d.exists("Set up alarms", timeout=5)
    save("alarm-9a-setup-sheet-none")
    alarm_permissions(True, True, False, restart=False)
    assert d.exists("Set up alarms", timeout=5)
    save("alarm-9b-setup-sheet-partly")
    d.back()
    d.scroll_to_top()
    d.tap("Name")
    d.shell("input text 'Wake%sup%sat%s7'")
    d.back()  # the keyboard
    time.sleep(0.5)
    d.scroll_to("Tap the reminder to change its time and type.")
    save("alarm-10-reminder-card-warning")
    d.tap("tap to fix", contains=True)
    assert d.exists("Set up alarms", timeout=5)
    alarm_permissions(True, True, True, restart=False)
    assert d.exists("Done", timeout=5)
    save("alarm-9c-setup-sheet-all-set")
    d.tap("Done")
    d.back()
    if d.exists("Discard", contains=True, timeout=1):
        d.tap("Discard", contains=True)
    alarm_permissions_reset()


@screen
def more_reminders_alarms():
    """More with its Reminders & alarms row (full screen can't ring), the screen, the Alarm sound
    sheet and Extra phone settings (design 2026-11-09), on a phone that passes for a OnePlus."""
    alarm_permissions(True, True, False)
    d.nav("More")
    d.scroll_to_top()
    save_full_length("more-reminders-alarms-row", 3800)
    d.scroll_to("Reminders & alarms")
    d.tap("Reminders & alarms")
    assert d.exists("Allow Weft to", timeout=5)
    save("more-reminders-alarms-screen")
    d.tap("Extra phone settings")
    assert d.exists("Turn both on in Weft’s app info", timeout=5)
    save("extra-phone-settings-sheet")
    d.tap("Done")
    d.scroll_to("Alarm sound")
    d.tap("Alarm sound")
    d.tap("Weft")  # plays it: the row says "Playing"
    save("alarm-11-sound-sheet")
    d.back()
    d.back()
    alarm_permissions_reset()


@screen
def archive_habit_sheet():
    """Archive asks first (2026-11-11, docs/DESIGN-RULES.md 10): the prompt over Habit detail."""
    d.nav("Habits")
    d.tap("Read")
    d.tap("Archive")  # the header's round button (its description)
    assert d.exists("Archive Read?", timeout=5)
    save("archive-habit-sheet")
    d.tap("Cancel")
    d.back()


@screen
def insights():
    d.nav("Insights")
    d.scroll_to_top()
    save_full_length("insights", 3760)


def main(words):
    picked = [s for s in SCREENS if not words or any(w in s.__name__ for w in words)]
    # The alarm capture clears Wake up at 7's day in the sample data: take it last.
    picked.sort(key=lambda s: s is alarm_ringing)
    setup()
    try:
        for s in picked:
            try:
                # Each screen starts from Weft in front (a sheet's Back may have left the app).
                d.shell(f"am start -n {d.PACKAGE}/.MainActivity")
                time.sleep(1.5)
                s()
            except Exception as e:  # keep going: one screen failing shouldn't stop the rest
                print("FAILED", s.__name__, e)
                d.shell(f"am start -n {d.PACKAGE}/.MainActivity")
                time.sleep(2)
    finally:
        teardown()


if __name__ == "__main__":
    main(sys.argv[1:])
