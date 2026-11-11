#!/usr/bin/env python3
"""Recaptures the README shots (docs/screenshots/) and the raw shots for the store images.

    ./gradlew :app:assemblePlayDebug
    python3 tools/verify/capture_docs.py docs            # docs/screenshots/*.png (Rust, the default)
    python3 tools/verify/capture_docs.py store <dir>     # raw shots for tools/store_shots.py and
                                                         # tools/feature_graphic.py, in the store colours

Same set-up as tools/verify/capture.py: a rootable emulator (Sprout_API_35), sample data on Sunday
27 Sep, the status bar at 9:30. The store set uses the app's default accent, Rust, like the store
page around it (user, 2026-11-09; before, Dynamic color seeded from the rose-brown #77453F). The
widget shots are cut out of the home screen by the widget's own bounds; this clears the launcher.
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(__file__))
import capture as c  # noqa: E402
import device as d  # noqa: E402

ROOT = c.ROOT


def shot(path):
    time.sleep(1.2)
    with open(path, "wb") as f:
        f.write(d.subprocess.run([d.ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print("saved", os.path.relpath(path, ROOT))


def install_fresh():
    """Like capture.setup(), but stops on Welcome so it can be captured."""
    c.d.adb("root")
    time.sleep(2)
    d.adb("wait-for-device")
    d.shell("settings put global auto_time 0")
    d.shell("date 092709302026.00")
    c.demo_status_bar()
    d.adb("uninstall", d.PACKAGE)
    assert "Success" in d.adb("install", c.APK)
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    assert d.exists("Get started", timeout=20)
    time.sleep(1.5)


def to_today():
    d.tap("Get started")
    d.tap("Skip for now", timeout=10)
    if d.exists("Go to Today", timeout=4):
        d.tap("Go to Today")
    assert d.exists("Today", timeout=10)
    time.sleep(1.5)


def front():
    d.shell(f"am start -n {d.PACKAGE}/.MainActivity")
    time.sleep(1.5)


def widget(kind, path):
    """Pins one widget on an empty home screen and saves just the widget."""
    d.pin_widget(kind)
    d.show_widget_page()
    time.sleep(2)
    nodes = d.widget_nodes()
    x1 = min(n.bounds[0] for n in nodes); y1 = min(n.bounds[1] for n in nodes)
    x2 = max(n.bounds[2] for n in nodes); y2 = max(n.bounds[3] for n in nodes)
    full = path + ".full.png"
    shot(full)
    from PIL import Image  # noqa: PLC0415
    Image.open(full).crop((x1, y1, x2, y2)).save(path)
    os.remove(full)
    print("cropped", os.path.relpath(path, ROOT), (x1, y1, x2, y2))


def widget_board(full_path, crops=None):
    """
    All four widgets on one screen (the debug WidgetBoardActivity hosts them over the wallpaper),
    with six habits so the week widget is full but not crowded, and more of today logged. Changes the
    sample data, so it runs last. [crops]: name -> path for each widget on its own.
    """
    db = f"/data/data/{d.PACKAGE}/databases/sprout.db"
    q = lambda sql: d.shell(f'sqlite3 {db} "{sql}"')  # noqa: E731
    day = int(d.shell("date +%s").strip()) // 86400
    d.shell(f"am force-stop {d.PACKAGE}")
    q("update habit set archived=1 where name in ('No sugar','Walk outside')")
    # Wake up at 7 again too: the alarm capture clears its day.
    for name, amount in (("Wake up at 7", 1), ("Workout", 45), ("Vitamins", 1)):
        q(f"insert or replace into entry(habitId, date, status, amount, loggedAt) "
          f"select id, {day}, 'DONE', {amount}, {day * 86400000 + 8 * 3600000} from habit where name='{name}'")
    d.shell(f"appwidget grantbind --package {d.PACKAGE}")
    d.home()
    time.sleep(1)
    d.shell(f"am start -n {d.PACKAGE}/.WidgetBoardActivity")
    time.sleep(8)
    shot(full_path)
    if crops:
        from PIL import Image  # noqa: PLC0415
        nodes = {n.label: n.bounds for n in d.dump() if n.label.startswith("widget-")}
        for name, path in crops.items():
            Image.open(full_path).crop(nodes[name]).save(path)
            print("cropped", os.path.relpath(path, ROOT))
    d.back()


def long_press_read():
    d.nav("Today")
    d.long_press("Read")


def docs():
    out = os.path.join(ROOT, "docs", "screenshots")
    p = lambda name: os.path.join(out, name + ".png")  # noqa: E731
    install_fresh()
    shot(p("01-welcome"))
    to_today()
    shot(p("02-today"))
    long_press_read(); shot(p("03-log-amount")); d.back()
    d.nav("Habits"); d.tap("Week"); shot(p("04-habits-week"))
    d.tap("Overall"); shot(p("05-habits-overall")); d.tap("Week")
    d.nav("Today"); d.tap("Read"); shot(p("06-habit-detail")); d.back()
    d.nav("Journal"); shot(p("07-journal"))
    d.tap("Filter by habit"); shot(p("08-filter-by-habit")); d.back()
    d.nav("Insights"); d.scroll_to_top(); shot(p("09-insights"))
    d.nav("Journal"); d.tap("Filter by date"); shot(p("10-date-range")); d.back()
    d.nav("More"); d.scroll_to_top(); shot(p("11-more"))
    d.scroll_to("Font"); d.tap("Font"); shot(p("18-font-sheet")); d.back()
    d.nav("Today"); d.tap("New habit"); shot(p("12-new-habit")); d.back()
    # Google Drive connected, backed up a few minutes ago (the device's clock).
    now = int(d.shell("date +%s").strip())
    d.shell(f"am start -n {d.PACKAGE}/.DriveStateActivity --es account {c.ACCOUNT} --el lastBackupAt {(now - 18 * 60) * 1000} --ei count 7")
    time.sleep(1)
    front(); d.nav("More"); d.scroll_to_top(); d.tap("Backups"); shot(p("19-drive-backup")); d.back()
    d.shell("cmd uimode night yes"); time.sleep(2)
    front(); d.nav("Today"); d.scroll_to_top(); shot(p("13-today-dark"))
    d.shell("cmd uimode night no"); time.sleep(2)
    front()
    c.DESIGN = out  # the capture goes to docs/, never over design/'s mockup
    c.alarm_ringing()
    os.replace(os.path.join(out, "alarm-3-ringing.png"), p("20-alarm"))
    # Last: it trims the sample data to five habits.
    widget_board(p("21-widgets"), {
        "widget-today": p("14-widget-today"), "widget-week": p("15-widget-week"),
        "widget-strip": p("16-widget-strip"), "widget-streak": p("17-widget-streak"),
    })


def store(out):
    os.makedirs(out, exist_ok=True)
    p = lambda name: os.path.join(out, name + ".png")  # noqa: E731
    install_fresh()
    to_today()
    # The store shots don't show the sample banner (it goes once a habit is logged): tap Workout's
    # circle twice (Done, then not done), so the data stays the sample's.
    w, _ = d.screen_size()
    for _ in range(2):
        # Found again each time: the banner goes after the first tap and the list moves up.
        n = d.find("Workout")
        d.tap_xy(int(w * 0.86), n.center[1])
        time.sleep(1.5)
    assert not d.exists("Sample habits", timeout=1), "the sample banner is still there"
    # The app in its default accent, Rust (user, 2026-11-09), like the page around it.
    front()
    d.nav("Today"); d.scroll_to_top(); shot(p("today"))
    long_press_read(); shot(p("amount-sheet")); d.back()
    d.nav("Habits"); d.tap("Week"); shot(p("habits-week"))
    d.nav("Today"); d.tap("Read"); shot(p("habit-detail")); d.back()
    d.nav("Journal"); shot(p("journal"))
    d.nav("Insights"); d.scroll_to_top(); shot(p("insights"))
    d.shell("cmd uimode night yes"); time.sleep(2)
    front(); d.nav("Today"); d.scroll_to_top(); shot(p("today-dark"))
    d.shell("cmd uimode night no"); time.sleep(2)
    # The alarm ringing screen (the store's second shot, 2026-11-11). It clears Wake up at 7's day,
    # so it comes after the shots that show Today.
    front()
    c.DESIGN = out
    c.alarm_ringing()
    os.replace(os.path.join(out, "alarm-3-ringing.png"), p("alarm"))
    # All four widgets on one screen (last: it trims the sample data to five habits).
    widget_board(p("w-week"))


if __name__ == "__main__":
    mode = sys.argv[1]
    try:
        docs() if mode == "docs" else store(sys.argv[2])
    finally:
        c.teardown()
