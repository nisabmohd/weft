"""Builds the Play Store shots in release/phone-screenshots/ (1080 x 1920). Plain style (2026-11-01):
the logo's navy (#2E4A7A) as a flat page, a two-line white caption, and a plain phone.
The status bar is redrawn lower and pulled in from the edges so the frame's rounded corners never cut
the clock or the battery.

usage: python3 tools/store_shots.py <folder with the raw shots> [out folder]
       python3 tools/store_shots.py --reframe [out folder]
         (takes each phone screen from the existing release/phone-screenshots as it is and draws the
         page, text and frame again; for a style change without new raw shots)
Raw shots are 1080 x 2400, named today.png, amount-sheet.png, habits-week.png, habit-detail.png,
journal.png, insights.png, w-week.png (home screen with the This week widget) and today-dark.png.
Needs Pillow."""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFRAME = sys.argv[1] == "--reframe"
RAW = None if REFRAME else sys.argv[1]
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "release", "phone-screenshots")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "lexend.ttf")

# raw name, out file, caption (two lines)
shots = [
    ("today", "01-today.png", ("Check off habits", "with a swipe")),
    ("amount-sheet", "02-log-amount.png", ("Log part of a habit,", "like 15 of 20 pages")),
    ("habits-week", "03-habits-week.png", ("See your week", "at a glance")),
    ("habit-detail", "04-habit-detail.png", ("Streaks and a calendar", "for every habit")),
    ("journal", "05-journal.png", ("Write a note", "for any day")),
    ("insights", "06-insights.png", ("See which habits", "are working")),
    ("w-week", "07-widgets.png", ("Widgets for your", "home screen")),
    ("today-dark", "08-today-dark.png", ("Light and dark themes,", "your font and colors")),
]

W, H = 1080, 1920
FW, FH, BEZEL = 650, 1444, 14                       # phone frame
FX, FY = (W - FW) // 2, 452
SX, SY, SW, SH = FX + BEZEL, FY + BEZEL, FW - 2 * BEZEL, FH - 2 * BEZEL
OUTER_R, INNER_R = 64, 50
K = SW / 1080                                       # raw -> screen scale

NAVY = (46, 74, 122)                                # the logo's navy, #2E4A7A, flat
WHITE = (255, 255, 255)

# Where the screen sat in the first 1.5.0 shots (phone lower, flat page), for --reframe of those.
OLD_SX, OLD_SY = 229, 484


def font(size, weight):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([weight])
    return f


def tracked(draw, xy, text, f, fill, tracking):
    """Centred text with letter spacing (kerning kept: each glyph goes where the prefix ends)."""
    widths = [draw.textlength(text[:i], font=f) + tracking * i for i in range(len(text) + 1)]
    x0 = xy[0] - (widths[-1] - tracking) / 2
    for i, ch in enumerate(text):
        draw.text((x0 + widths[i], xy[1]), ch, font=f, fill=fill, anchor="ls")


def screen(raw):
    s = Image.open(raw).convert("RGB")
    bar_bg = s.getpixel((540, 4))
    body = s.crop((0, 80, 1080, 2400)).resize((SW, round(2320 * K)), Image.LANCZOS)
    out = Image.new("RGB", (SW, SH), bar_bg)
    out.paste(body, (0, SH - body.height))
    clock = s.crop((20, 0, 300, 70)).resize((round(280 * K), round(70 * K)), Image.LANCZOS)
    icons = s.crop((820, 0, 1060, 70)).resize((round(240 * K), round(70 * K)), Image.LANCZOS)
    top = SH - body.height - round(70 * K) - 4
    out.paste(clock, (30, top))
    out.paste(icons, (SW - icons.width - 30, top))
    return out


def rounded_mask(w, h, r, k=4):
    m = Image.new("L", (w * k, h * k), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w * k - 1, h * k - 1), r * k, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def phone(img, scr):
    sh = Image.new("L", (W, H), 0)
    ImageDraw.Draw(sh).rounded_rectangle((FX + 10, FY + 30, FX + FW - 10, FY + FH + 20), OUTER_R, fill=120)
    img.paste((16, 28, 50), (0, 0), sh.filter(ImageFilter.GaussianBlur(40)))
    k = 4
    body = Image.new("RGBA", (FW * k, FH * k), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle((0, 0, FW * k - 1, FH * k - 1), OUTER_R * k, fill=(84, 92, 108, 255))
    bd.rounded_rectangle((2 * k, 2 * k, FW * k - 1 - 2 * k, FH * k - 1 - 2 * k), (OUTER_R - 2) * k,
                         fill=(12, 13, 16, 255))
    body = body.resize((FW, FH), Image.LANCZOS)
    img.paste(body, (FX, FY), body)
    img.paste(scr, (SX, SY), rounded_mask(SW, SH, INNER_R))


for raw, out, (line1, line2) in shots:
    if REFRAME:
        old = Image.open(f"{OUT}/{out}").convert("RGB")
        # the first 1.5.0 shots had the page showing just above where this frame starts
        ox, oy = (OLD_SX, OLD_SY) if old.getpixel((W // 2, FY + 8))[2] > 60 else (SX, SY)
        scr = old.crop((ox, oy, ox + SW, oy + SH))
    else:
        scr = screen(f"{RAW}/{raw}.png")

    img = Image.new("RGB", (W, H), NAVY)
    d = ImageDraw.Draw(img)
    f = font(66, 600)
    tracked(d, (W / 2, 232), line1, f, WHITE, -1)
    tracked(d, (W / 2, 316), line2, f, WHITE, -1)
    phone(img, scr)
    img.save(f"{OUT}/{out}", optimize=True)
    print(out)
