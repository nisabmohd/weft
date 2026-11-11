"""Builds the Play Store shots in release/phone-screenshots/ (1080 x 1920) and copies them to fastlane.

The same look as Burrow's store shots (tools/store_shots.py there; both apps are by mid apps and sit
side by side on Play, user 2026-11-08): a title and a one-line subtitle, then a phone frame with a soft
shadow, sized to fit under the tallest caption with 72 px left below it, so the navigation bar and
the bezel are never cut. The last shot (dark theme) sits on a dark page. Weft's colours: a rose-brown
page #733626 (Rust, the default accent, user 2026-11-09) with pale pink text.

usage: python3 tools/store_shots.py <folder with the raw shots> [out folder]
Raw shots are 1080 x 2400, named today.png, alarm.png (the ringing screen), amount-sheet.png, habits-week.png,
journal.png, insights.png, w-week.png (all four widgets, WidgetBoardActivity) and today-dark.png;
made by `python3 tools/verify/capture_docs.py store <folder>`. A missing one is skipped.
Needs Pillow."""
import os
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = sys.argv[1]
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "release", "phone-screenshots")
FASTLANE = os.path.join(ROOT, "fastlane", "metadata", "android", "en-US", "images", "phoneScreenshots")
os.makedirs(OUT, exist_ok=True)
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "lexend.ttf")

# raw name, out file, title, subtitle, dark page
shots = [
    ("today", "01-today.png", "Check off with a swipe", "Swipe, tap or hold. Every habit on one calm list.", False),
    # The alarm is 1.6's headline feature (user, 2026-11-11): second, right after Today. Play takes 8
    # phone shots at most, so habit detail made room for it.
    ("alarm", "02-alarm.png", "Alarms that really ring", "Full screen, even on a locked phone.", False),
    ("amount-sheet", "03-log-amount.png", "Log part of a habit", "15 of 20 pages still counts.", False),
    ("habits-week", "04-habits-week.png", "See your week at a glance", "Every habit, every day, in one look.", False),
    ("journal", "05-journal.png", "Write a note for any day", "Say how it went, or what got in the way.", False),
    ("insights", "06-insights.png", "See what's working", "Your best day, and the habit that needs focus.", False),
    ("w-week", "07-widgets.png", "Home screen widgets", "Today, your week and your streak at a glance.", False),
    ("today-dark", "08-today-dark.png", "Private. Yours.", "No ads, no tracking. Free and open source.", True),
]

W, H = 1080, 1920
TOP, GAP, BOTTOM = 150, 56, 72                        # caption top; caption → phone; phone → edge
BEZEL, OUTER_R, INNER_R = 14, 62, 48
RAW_W, RAW_H = 1080, 2400                             # emulator screenshot size
TITLE_SIZE, TITLE_LINE, SUB_SIZE, SUB_LINE = 68, 82, 36, 48

PAGE = (0x73, 0x36, 0x26)                             # Rust, the default accent (2026-11-09; rose-brown #77453F before)
PAGE_DARK = (0x2B, 0x15, 0x10)                        # the same hue, dark, for the dark-theme shot
TITLE = (0xFF, 0xDA, 0xD4)                            # pale pink
SUB = (0xE2, 0xBC, 0xB6)                              # pale pink, quieter
SHADOW = (0x2A, 0x10, 0x0E)


def font(size, weight):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([weight])
    return f


def wrap(draw, text, f, width):
    lines, cur = [], ""
    for word in text.split():
        t = (cur + " " + word).strip()
        if draw.textlength(t, font=f) <= width or not cur:
            cur = t
        else:
            lines.append(cur)
            cur = word
    return lines + [cur]


def screen(raw, sw, sh):
    """The raw shot at screen size, status bar pulled in from the corners so they never cut it."""
    s = Image.open(raw).convert("RGB")
    k = sw / RAW_W
    out = s.resize((sw, sh), Image.LANCZOS)
    bar = round(110 * k)                                   # status bar, tall enough for the clock
    clock = s.crop((0, 0, 360, 110)).resize((round(360 * k), bar), Image.LANCZOS)
    icons = s.crop((720, 0, RAW_W, 110)).resize((round(360 * k), bar), Image.LANCZOS)
    # Over the app the bar is one colour: fill it and pull the clock and icons in. Over a wallpaper
    # (the widgets shot) leave it as it is, so no flat band shows.
    flat = len({s.getpixel((x, y)) for x in range(400, 680, 20) for y in range(8, 110, 10)}) == 1
    if flat:
        ImageDraw.Draw(out).rectangle((0, 0, sw, bar), fill=s.getpixel((540, 8)))
        inset, drop = 14, 8
        out.paste(clock, (inset, drop))
        out.paste(icons, (sw - icons.width - inset, drop))
    return out


def caption(d, title, sub, tf, sf, tc, sc, dry=False):
    """Draws the title and line; returns the y under them."""
    y = TOP
    for line in wrap(d, title, tf, 960):
        if not dry:
            d.text((W / 2, y), line, font=tf, fill=tc, anchor="mt")
        y += TITLE_LINE
    y += 12
    for line in wrap(d, sub, sf, 920):
        if not dry:
            d.text((W / 2, y), line, font=sf, fill=sc, anchor="mt")
        y += SUB_LINE
    return y


tf, sf = font(TITLE_SIZE, 600), font(SUB_SIZE, 400)
probe = ImageDraw.Draw(Image.new("RGB", (W, H)))
# One phone size for every shot: under the tallest caption, with room below for the shadow.
FY = max(caption(probe, t, s, tf, sf, None, None, dry=True) for _, _, t, s, _ in shots) + GAP
FH = H - BOTTOM - FY
SH = FH - 2 * BEZEL
SW = round(SH * RAW_W / RAW_H)
FW = SW + 2 * BEZEL
FX = (W - FW) // 2
SX, SY = FX + BEZEL, FY + BEZEL

for n, (raw, out, title, sub, dark) in enumerate(shots, start=1):
    if not os.path.exists(os.path.join(RAW, raw + ".png")):
        print(f"skip {out}: no {raw}.png")
        continue
    img = Image.new("RGB", (W, H), PAGE_DARK if dark else PAGE)
    d = ImageDraw.Draw(img)
    caption(d, title, sub, tf, sf, TITLE, SUB)

    # soft shadow under the phone
    sh = Image.new("L", (W, H), 0)
    ImageDraw.Draw(sh).rounded_rectangle((FX, FY + 12, FX + FW, FY + FH + 12), OUTER_R, fill=120 if not dark else 160)
    sh = sh.filter(ImageFilter.GaussianBlur(28))
    img.paste((0, 0, 0) if dark else SHADOW, (0, 0), sh)

    d = ImageDraw.Draw(img)
    d.rounded_rectangle((FX, FY, FX + FW - 1, FY + FH - 1), OUTER_R, fill=(58, 54, 50) if dark else (22, 20, 18))
    if dark:  # a thin light rim so the dark phone reads on the dark page
        d.rounded_rectangle((FX, FY, FX + FW - 1, FY + FH - 1), OUTER_R, outline=(84, 62, 58), width=2)
        d.rounded_rectangle((FX + 2, FY + 2, FX + FW - 3, FY + FH - 3), OUTER_R - 2, fill=(14, 13, 12))

    scr = screen(f"{RAW}/{raw}.png", SW, SH)
    k = 4
    m = Image.new("L", (SW * k, SH * k), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, SW * k - 1, SH * k - 1), INNER_R * k, fill=255)
    img.paste(scr, (SX, SY), m.resize((SW, SH), Image.LANCZOS))
    img.save(f"{OUT}/{out}", optimize=True)
    if OUT == os.path.join(ROOT, "release", "phone-screenshots") and os.path.isdir(FASTLANE):
        shutil.copy(f"{OUT}/{out}", f"{FASTLANE}/{n}.png")
    print(out)
print(f"phone {FW} x {FH} at y {FY}, {H - FY - FH} px below")
