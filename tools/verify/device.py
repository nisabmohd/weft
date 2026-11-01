"""Small helpers for driving the Weft app on an emulator or phone over adb.

Only the Python standard library; finds elements by their visible text or TalkBack label
(content description), so checks don't depend on screen coordinates.
"""
import os
import re
import subprocess
import time

PACKAGE = "app.sprout.habits"
ADB = os.environ.get("ADB") or os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")


def adb(*args, check=False):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=check).stdout


def shell(cmd):
    return adb("shell", cmd)


class Node:
    def __init__(self, raw):
        def attr(key):
            m = re.search(key + r'="([^"]*)"', raw)
            return m.group(1) if m else ""
        self.text = attr("text").replace("&amp;", "&")
        self.desc = attr("content-desc").replace("&amp;", "&")
        self.cls = attr("class")
        self.package = attr("package")
        self.clickable = attr("clickable") == "true"
        self.checkable = attr("checkable") == "true"
        self.checked = attr("checked") == "true"
        self.selected = attr("selected") == "true"
        self.scrollable = attr("scrollable") == "true"
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attr("bounds")))
        self.bounds = (x1, y1, x2, y2)

    @property
    def label(self):
        return self.text or self.desc

    @property
    def center(self):
        x1, y1, x2, y2 = self.bounds
        return (x1 + x2) // 2, (y1 + y2) // 2

    def __repr__(self):
        return f"Node({self.label!r} {self.bounds})"


def dump():
    """The current screen as a list of nodes."""
    shell("uiautomator dump /sdcard/ui.xml")
    xml = shell("cat /sdcard/ui.xml")
    return [Node(m.group(0)) for m in re.finditer(r"<node [^>]*>", xml)]


def find(label, contains=False, timeout=5.0):
    """Waits for a node whose text or description is [label] (or contains it)."""
    end = time.time() + timeout
    while True:
        for n in dump():
            if n.label == label or (contains and label in n.label):
                return n
        if time.time() > end:
            return None
        time.sleep(0.4)


def exists(label, contains=False, timeout=3.0):
    return find(label, contains, timeout) is not None


def tap(label, contains=False, timeout=5.0):
    n = find(label, contains, timeout)
    if n is None:
        raise AssertionError(f"not on screen: {label!r}")
    shell(f"input tap {n.center[0]} {n.center[1]}")
    time.sleep(0.8)
    return n


def tap_xy(x, y):
    shell(f"input tap {x} {y}")
    time.sleep(0.8)


def swipe(x1, y1, x2, y2, ms=300):
    shell(f"input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(0.8)


def long_press(label, contains=False):
    n = find(label, contains)
    if n is None:
        raise AssertionError(f"not on screen: {label!r}")
    x, y = n.center
    swipe(x, y, x, y, 900)


def scroll_to(label, contains=False, max_swipes=6):
    """Scrolls down the current list until [label] is on screen; returns its node or None."""
    w, h = screen_size()
    for _ in range(max_swipes + 1):
        n = find(label, contains, timeout=0.5)
        # Off-screen nodes can still be listed, with empty bounds; they aren't on screen yet.
        if n and n.bounds[3] <= n.bounds[1]:
            n = None
        if n and n.center[1] < h * 0.7:
            return n
        if n:
            # Found low on screen, where a nav bar or button may cover it: bring it up.
            for _ in range(3):
                swipe(w // 2, int(h * 0.6), w // 2, int(h * 0.4), 300)
                n = find(label, contains, timeout=1.0) or n
                if n.center[1] < h * 0.7:
                    break
            return n
        swipe(w // 2, int(h * 0.75), w // 2, int(h * 0.35), 300)
    return None


def scroll_to_top(swipes=4):
    """Scrolls the current list back to its top (headers there, like +, may be scrolled away)."""
    w, h = screen_size()
    for _ in range(swipes):
        swipe(w // 2, int(h * 0.35), w // 2, int(h * 0.85), 200)


def type_text(text):
    shell("input text " + text.replace(" ", "%s").replace("'", "\\'"))
    time.sleep(0.4)


def back():
    shell("input keyevent KEYCODE_BACK")
    time.sleep(0.8)


def _read(cmd, pattern):
    """Runs a `wm` query, retrying: adb now and then returns nothing."""
    for _ in range(5):
        out = shell(cmd).strip()
        m = re.search(pattern, out.splitlines()[-1] if out else "")
        if m:
            return m
        time.sleep(0.5)
    raise AssertionError(f"no answer from `{cmd}`")


def screen_size():
    m = _read("wm size", r"(\d+)x(\d+)")
    return int(m.group(1)), int(m.group(2))


def density():
    return int(_read("wm density", r"(\d+)").group(1)) / 160


def nav(tab):
    """
    Taps a bottom-bar tab by its label (the bar's labels are always the last matches). If Weft is
    on a screen without the bar (a habit, Manage habits, Backup & restore, left there by an earlier
    check), goes back to it first.
    """
    nodes = []
    for attempt in range(4):
        end = time.time() + (3 if attempt == 0 else 1.5)
        while True:  # retry briefly: the bar may still be sliding in after a page transition
            nodes = [n for n in dump() if n.label == tab and n.package == PACKAGE]
            if nodes or time.time() > end:
                break
            time.sleep(0.4)
        if nodes or attempt == 3:
            break
        back()
    if not nodes:
        raise AssertionError(f"tab not found: {tab}")
    n = max(nodes, key=lambda n: n.bounds[1])
    shell(f"input tap {n.center[0]} {n.center[1]}")
    time.sleep(1.0)


def small_targets(min_dp=48):
    """Interactive app nodes smaller than [min_dp], ignoring ones clipped by a scrolling list."""
    d = density()
    nodes = dump()
    # A node touching the top or bottom edge of a scrolling container is cut off, not small.
    edges = {y for n in nodes if n.scrollable for y in (n.bounds[1], n.bounds[3])}
    # Compose reports only the uncovered part of a node, so anything under a floating button
    # (e.g. "Add note") looks small; its hidden edge meets the covering control's edge.
    covers = [n.bounds for n in nodes if n.package == PACKAGE and n.clickable]
    out = []
    for n in nodes:
        if n.package != PACKAGE or not (n.clickable or n.checkable):
            continue
        x1, y1, x2, y2 = n.bounds
        # Clipped by a list edge (allow a few pixels of rounding), or only a sliver still visible.
        if any(abs(y1 - e) <= 8 or abs(y2 - e) <= 8 for e in edges) or (y2 - y1) < 12 * d:
            continue
        # Sticking out past the top or bottom of a list it sits in: scrolled partly out of view.
        if any(sx1 <= x1 and x2 <= sx2 and (y1 < sy1 or y2 > sy2) for sx1, sy1, sx2, sy2 in
               (m.bounds for m in nodes if m.scrollable)):
            continue
        if any(c != n.bounds and c[0] < x2 and x1 < c[2] and (abs(y2 - c[1]) <= 2 or abs(y1 - c[3]) <= 2) for c in covers):
            continue
        w, h = (x2 - x1) / d, (y2 - y1) / d
        if w < min_dp - 1.5 or h < min_dp - 1.5:
            out.append(f"{n.label or n.cls} {w:.0f}x{h:.0f}dp")
    return out


# --- home screen and widgets -----------------------------------------------------------------

def launcher():
    """The home app's package (Launcher3 on AOSP images, the Pixel launcher on Play images)."""
    out = shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
    return out.strip().splitlines()[-1].split("/")[0]


def home():
    shell("input keyevent KEYCODE_HOME")
    time.sleep(1.5)


def open_app():
    shell(f"am start -W -n {PACKAGE}/.MainActivity")
    time.sleep(1.5)


def pin_widget(kind):
    """
    Empties the home screen and pins one Weft widget there ([kind]: week, today, strip, streak),
    through the debug build's PinWidgetActivity. Clearing the launcher resets its layout, so only
    run this on a test emulator.

    After `pm clear` the launcher rebuilds its default home screen for a few seconds; a pin request
    accepted during that time is dropped on a slow emulator. So it waits for the home screen to
    settle, checks that the widget really arrived, and tries once more if it didn't.
    """
    home_app = launcher()
    for attempt in range(3):
        shell(f"pm clear {home_app}")
        home()
        _wait_for_home(home_app)
        shell(f"am start -n {PACKAGE}/.PinWidgetActivity --es widget {kind}")
        end = time.time() + 15
        while time.time() < end:
            add = next((n for n in dump() if n.label.lower() in ("add to home screen", "add automatically")), None)
            if add:
                shell(f"input tap {add.center[0]} {add.center[1]}")
                break
            time.sleep(0.5)
        else:
            raise AssertionError(f"launcher didn't offer to add the {kind} widget")
        time.sleep(1.5)
        home()
        # Kept only if it's still there once the launcher has finished loading its layout.
        time.sleep(8)
        if widget_nodes():
            return
    raise AssertionError(f"the {kind} widget didn't stay on the home screen")


def _wait_for_home(home_app, timeout=40.0, still=5.0):
    """Waits until the launcher's home screen has stayed the same for [still] seconds."""
    end = time.time() + timeout
    last, since = None, time.time()
    while time.time() < end:
        now = sorted(n.label for n in dump() if n.package == home_app and n.label)
        if now != last:
            last, since = now, time.time()
        elif now and time.time() - since >= still:
            return
        time.sleep(1.0)


def widget_nodes():
    """Weft's widget views on the home screen (the launcher hosts them under Weft's package)."""
    return [n for n in dump() if n.package == PACKAGE]


def show_widget_page():
    """
    The launcher may add a widget to another home screen page (when the first is full), and Home
    always goes back to the first page. If no Weft widget is on screen, swipes across the pages to
    it, and back to the first page if none has one.
    """
    if widget_nodes():
        return True
    w, h = screen_size()
    for _ in range(3):
        shell(f"input swipe {int(w * 0.85)} {h // 2} {int(w * 0.15)} {h // 2} 250")
        time.sleep(0.8)
        if widget_nodes():
            return True
    for _ in range(3):
        shell(f"input swipe {int(w * 0.15)} {h // 2} {int(w * 0.85)} {h // 2} 250")
        time.sleep(0.4)
    return False


def screen_size():
    m = re.search(r"(\d+)x(\d+)", shell("wm size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


def widget_wait(predicate, timeout=4.0):
    """Waits until [predicate] holds for the widget labels; returns them, or None on timeout."""
    show_widget_page()
    end = time.time() + timeout
    while True:
        labels = [n.label for n in widget_nodes() if n.label]
        if predicate(labels):
            return labels
        if time.time() > end:
            return None
        time.sleep(0.4)


def widget_tap(label, contains=False):
    show_widget_page()
    n = next((n for n in widget_nodes() if n.label == label or (contains and label in n.label)), None)
    if n is None:
        raise AssertionError(f"not on the widget: {label!r}")
    shell(f"input tap {n.center[0]} {n.center[1]}")
    time.sleep(1.0)


def brightness(bounds):
    """Average brightness (0–255) of a screen rectangle, from a screenshot (standard library PNG decode)."""
    import struct
    import zlib
    png = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout
    pos, chunks, width, height, ctype = 8, [], 0, 0, 0
    while pos < len(png):
        length, kind = struct.unpack(">I4s", png[pos:pos + 8])
        data = png[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, _, ctype = struct.unpack(">IIBB", data[:10])
        elif kind == b"IDAT":
            chunks.append(data)
        pos += 12 + length
    bpp = 4 if ctype == 6 else 3
    raw = zlib.decompress(b"".join(chunks))
    stride = width * bpp
    rows, prev = [], bytearray(stride)
    for y in range(height):
        f, line = raw[y * (stride + 1)], bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b, c = prev[i], (prev[i - bpp] if i >= bpp else 0)
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    x1, y1, x2, y2 = bounds
    total = count = 0
    for y in range(y1, y2, 4):
        for x in range(x1, x2, 4):
            r, g, b = rows[y][x * bpp:x * bpp + 3]
            total += (r + g + b) / 3
            count += 1
    return total / max(count, 1)
