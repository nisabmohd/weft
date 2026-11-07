"""Builds the Play Store feature graphic (1024 x 500) in the style of the store shots: the logo's rose-brown
as a flat page, the Weft mark and name with one line under it, and one plain phone (Today) running off
the bottom.

usage: python3 tools/feature_graphic.py <folder with the raw shots> [out file]
       python3 tools/feature_graphic.py --reframe [out file]
         (takes the phone screen from release/phone-screenshots/01-today.png,
         as made by tools/store_shots.py; no raw shots needed)
Raw shot: today.png, 1080 x 2400 (the same ones as tools/store_shots.py).
Writes release/feature-graphic-1024x500.png and the fastlane copy. Needs Pillow; the
Weft mark is taken from release/icon-512.png (tools/weft_mark.py)."""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFRAME = sys.argv[1] == "--reframe"
RAW = None if REFRAME else sys.argv[1]
OUTS = [sys.argv[2]] if len(sys.argv) > 2 else [
    os.path.join(ROOT, "release", "feature-graphic-1024x500.png"),
    os.path.join(ROOT, "fastlane", "metadata", "android", "en-US", "images", "featureGraphic.png"),
]
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "lexend.ttf")
W, H = 1024, 500
PAGE = (0x77, 0x45, 0x3F)   # the icon's tile, #77453F (2026-11-08; it was navy)
INK = (0xFF, 0xDA, 0xD4)    # pale pink for text on the rose-brown page
SUB = (0xE2, 0xBC, 0xB6)    # the line under the name: the ink, quieter
SHADOW = (0x3A, 0x1C, 0x19, 255)
LOGO = (0x2A, 0x6F, 0x52)   # the logo's green tile (weft-icon.zip)

# the phone screen in the store shots (tools/store_shots.py): 622 x 1416 at (229, 466)
SHOT_SX, SHOT_SY, SHOT_SW, SHOT_SH = 229, 466, 622, 1416
SS = 2                      # drawn at twice the size, then scaled down


def font(size, weight):
    f = ImageFont.truetype(FONT, size * SS)
    f.set_variation_by_axes([weight])
    return f


def tracked(draw, xy, text, f, fill, tracking):
    widths = [draw.textlength(text[:i], font=f) + tracking * SS * i for i in range(len(text) + 1)]
    for i, ch in enumerate(text):
        draw.text((xy[0] * SS + widths[i], xy[1] * SS), ch, font=f, fill=fill, anchor="ls")
    return (widths[-1] - tracking * SS) / SS


def page():
    return Image.new("RGBA", (W * SS, H * SS), PAGE + (255,))


def screen_from_raw(raw):
    """The phone screen from a raw 1080 x 2400 shot, as tools/store_shots.py draws it (same BAR band)."""
    BAR = 110
    s = Image.open(raw).convert("RGB")
    K = SHOT_SW / 1080
    body = s.crop((0, BAR, 1080, 2400)).resize((SHOT_SW, round((2400 - BAR) * K)), Image.LANCZOS)
    out = Image.new("RGB", (SHOT_SW, SHOT_SH), s.getpixel((540, 4)))
    out.paste(body, (0, SHOT_SH - body.height))
    clock = s.crop((20, 0, 300, BAR)).resize((round(280 * K), round(BAR * K)), Image.LANCZOS)
    icons = s.crop((820, 0, 1060, BAR)).resize((round(240 * K), round(BAR * K)), Image.LANCZOS)
    top = SHOT_SH - body.height - round(BAR * K) + 6
    out.paste(clock, (30, top))
    out.paste(icons, (SHOT_SW - icons.width - 30, top))
    return out


def rounded_mask(w, h, r, k=4):
    m = Image.new("L", (w * k, h * k), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w * k - 1, h * k - 1), r * k, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def phone(scr, width):
    """A plain phone with a thin light rim around the screen; RGBA, `width` px wide."""
    bezel = round(width * 0.022)
    sw = width - 2 * bezel
    s = scr.resize((sw, round(scr.height * sw / scr.width)), Image.LANCZOS)
    h = s.height + 2 * bezel
    ro = round(width * 0.1)
    k = 4
    body = Image.new("RGBA", (width * k, h * k), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    rim = max(1, round(width * 0.005))
    bd.rounded_rectangle((0, 0, width * k - 1, h * k - 1), ro * k, fill=(118, 128, 148, 255))
    bd.rounded_rectangle((rim * k, rim * k, (width - rim) * k - 1, (h - rim) * k - 1), (ro - rim) * k,
                         fill=(30, 33, 40, 255))
    bd.rounded_rectangle((2 * rim * k, 2 * rim * k, (width - 2 * rim) * k - 1, (h - 2 * rim) * k - 1),
                         (ro - 2 * rim) * k, fill=(8, 9, 12, 255))
    ph = body.resize((width, h), Image.LANCZOS)
    ph.paste(s, (bezel, bezel), rounded_mask(sw, s.height, ro - bezel))
    return ph


def place(img, ph, x, y, angle):
    """Rotate the phone, drop a soft shadow under it and paste it with its top-left near (x, y)."""
    rot = ph.rotate(angle, resample=Image.BICUBIC, expand=True)
    a = rot.getchannel("A")
    sh = Image.new("L", img.size, 0)
    sh.paste(a.point(lambda v: v * 0.75), (x + 10 * SS, y + 26 * SS))
    img.paste(SHADOW, (0, 0), sh.filter(ImageFilter.GaussianBlur(26 * SS)))
    sh = Image.new("L", img.size, 0)
    sh.paste(a.point(lambda v: v * 0.45), (x + 2 * SS, y + 6 * SS))
    img.paste(SHADOW, (0, 0), sh.filter(ImageFilter.GaussianBlur(6 * SS)))
    img.alpha_composite(rot, (x, y)) if x >= 0 and y >= 0 else img.paste(rot, (x, y), rot)


def mark(px, color):
    """The Weft mark, `px` wide, from release/icon-512.png (white on the green tile) so it is the store icon's drawing."""
    icon = Image.open(os.path.join(ROOT, "release", "icon-512.png")).convert("L")
    lo = icon.getpixel((4, 4))
    hi = icon.getextrema()[1]  # the mark's own lightness: full opacity there, whatever its colour
    mask = icon.point(lambda v: max(0, min(255, round((v - lo) * 255 / (hi - lo)))))
    out = Image.new("RGBA", (px, px), color + (0,))
    out.putalpha(mask.resize((px, px), Image.LANCZOS))
    return out


if REFRAME:
    shots = os.path.join(ROOT, "release", "phone-screenshots")
    screens = [Image.open(os.path.join(shots, n)).convert("RGB").crop(
        (SHOT_SX, SHOT_SY, SHOT_SX + SHOT_SW, SHOT_SY + SHOT_SH)) for n in ("01-today.png", "05-journal.png")]
else:
    screens = [screen_from_raw(os.path.join(RAW, n)) for n in ("today.png", "journal.png")]

img = page()
# one phone, upright, running off the bottom
place(img, phone(screens[0], 330 * SS), 618 * SS, 64 * SS, 0)

d = ImageDraw.Draw(img)
# the logo as on the launcher (the green tile with the white mark), the name, then one line
tx, ty, ts = 96, 188, 64
img.paste(LOGO + (255,), (tx * SS, ty * SS), rounded_mask(ts * SS, ts * SS, round(ts * 0.3 * SS)))
m = mark(ts * SS, (255, 255, 255))
img.alpha_composite(m, ((tx * SS + ts * SS // 2 - m.width // 2), (ty * SS + ts * SS // 2 - m.height // 2)))
d = ImageDraw.Draw(img)
tracked(d, (tx + ts + 18, ty + 49), "Weft", font(54, 600), INK, -1)
tracked(d, (tx, ty + 112), "Habit tracker and journal", font(26, 400), SUB, 0)

final = img.convert("RGB").resize((W, H), Image.LANCZOS)
for out in OUTS:
    final.save(out, optimize=True)
    print(out)
