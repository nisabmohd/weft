"""Draws the home-screen widgets' picker previews (`previewImage`), like Burrow's tools/widget_previews.py:
each widget with the sample habits, in Lexend, light and dark, sized to its content with clear rounded
corners. Colours follow the widgets themselves (`WidgetTheme.kt`: the logo green's scheme, the widget
background from its secondaryContainer) and `habitColors`; sizes follow `WidgetLayout.kt`.

They used to be crops of a phone screenshot, which left the dark wallpaper in a ring along the
rounded corners and empty space below the content (user, 2026-11-11).

  app/src/main/res/drawable-nodpi/widget_preview_{today,week,strip,streak}.webp        light
  app/src/main/res/drawable-night-nodpi/widget_preview_{today,week,strip,streak}.webp  dark

usage: python3 tools/widget_previews.py   (headless Chrome, then Pillow for WebP)
"""
import math
import os
import re
import shutil
import subprocess
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
FONT = os.path.join(RES, "font", "lexend.ttf")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
SCALE = 3  # px per dp: crisp on xxhdpi, the picker scales it down
LOGO_HUE = 155


def hsl(h, s, l):
    return f"hsl({h} {s}% {l}%)"


def scheme(dark):
    """The colours the widgets use, from `seedColorScheme(LOGO_HUE)` (Color.kt)."""
    h = LOGO_HUE
    if dark:
        return dict(bg=hsl(h, 14, 15), primary=hsl(h, 86, 82), track=hsl(h, 12, 18), text=hsl(h, 10, 93),
                    muted=hsl(h, 8, 68), outline=hsl(h, 8, 45), skip=hsl(h, 8, 28), skip_ink=hsl(h, 8, 68))
    return dict(bg=hsl(h, 34, 92), primary="#2A6F52", track=hsl(h, 28, 91), text=hsl(h, 15, 11),
                muted=hsl(h, 8, 36), outline=hsl(h, 8, 60), skip=hsl(h, 14, 84), skip_ink=hsl(h, 8, 36))


def habit(hue, dark):
    """`habitColors` (Color.kt)."""
    if dark:
        return dict(solid=hsl(hue, 50, 66), soft=hsl(hue, 22, 19), mid=hsl(hue, 30, 30), on=hsl(hue, 40, 12), ink=hsl(hue, 60, 84))
    return dict(solid=hsl(hue, 48, 40), soft=hsl(hue, 55, 92), mid=hsl(hue, 48, 82), on="#fff", ink=hsl(hue, 50, 24))


def icon(name, colour, size):
    """A vector drawable as inline SVG in [colour] (Solar icons are strokes; some paths are fills)."""
    text = open(os.path.join(RES, "drawable", f"{name}.xml"), encoding="utf-8").read()
    vb = re.search(r'android:viewportWidth="([\d.]+)"', text).group(1)
    out = []
    for tag in re.findall(r"<path\b[^>]*>", text, re.S):
        d = re.search(r'android:pathData="([^"]+)"', tag).group(1)
        w = re.search(r'android:strokeWidth="([\d.]+)"', tag)
        if "strokeColor" in tag:
            out.append(f'<path d="{d}" fill="none" stroke="{colour}" stroke-width="{w.group(1) if w else 1.5}" '
                       f'stroke-linecap="round" stroke-linejoin="round"/>')
        if "fillColor" in tag:
            out.append(f'<path d="{d}" fill="{colour}"/>')
    return f'<svg width="{size}" height="{size}" viewBox="0 0 {vb} {vb}" style="flex:none;display:block">{"".join(out)}</svg>'


def mark(kind, hc, c, size, fraction=0.5):
    """A day mark (`drawDayMark` in DayMark.kt) as SVG."""
    r = size / 2
    g, gs = r * 0.42, r * 0.13
    body = ""
    if kind == "D":
        u = g / 7
        p = lambda x, y: f"{r + (x - 12) * u:.2f},{r + (y - 12.25) * u:.2f}"
        body = (f'<circle cx="{r}" cy="{r}" r="{r}" fill="{hc["solid"]}"/>'
                f'<polyline points="{p(5, 12.5)} {p(9.5, 17)} {p(19, 7.5)}" fill="none" stroke="{hc["on"]}" '
                f'stroke-width="{gs:.2f}" stroke-linecap="round" stroke-linejoin="round"/>')
    elif kind == "P":
        a = math.radians(-90 + 360 * fraction)
        x, y = r + r * math.cos(a), r + r * math.sin(a)
        body = (f'<circle cx="{r}" cy="{r}" r="{r}" fill="{hc["mid"]}"/>'
                f'<path d="M{r},{r} L{r},0 A{r},{r} 0 {1 if fraction > 0.5 else 0} 1 {x:.2f},{y:.2f} Z" fill="{hc["solid"]}"/>')
    elif kind == "S":
        body = (f'<circle cx="{r}" cy="{r}" r="{r}" fill="{c["skip"]}"/>'
                f'<line x1="{r - g * .6:.2f}" y1="{r}" x2="{r + g * .6:.2f}" y2="{r}" stroke="{c["skip_ink"]}" '
                f'stroke-width="{gs:.2f}" stroke-linecap="round"/>')
    elif kind == "O":
        w = r * 0.1
        body = (f'<circle cx="{r}" cy="{r}" r="{r - w / 2:.2f}" fill="none" stroke="{hc["solid"]}" stroke-width="{w:.2f}" '
                f'stroke-dasharray="{r * .28:.2f} {r * .2:.2f}"/>')
    elif kind == "N":
        body = f'<circle cx="{r}" cy="{r}" r="{r * 0.1:.2f}" fill="{c["outline"]}"/>'
    return f'<svg width="{size}" height="{size}" style="flex:none;display:block">{body}</svg>'


def marks(row, hc, c, size):
    return "".join(f'<div style="flex:1;display:flex;justify-content:center">{mark(k, hc, c, size, f)}</div>'
                   for k, f in row)


# The sample habits (SampleData.kt) on Sunday, the 7th day: 5 of 7 due today are done.
HABITS = [
    ("Wake up at 7", "ic_sym_alarm", 38, "DDSDDDD"),
    ("Read", "ic_sym_menu_book", 215, "DPDDPDD"),
    ("Drink water", "ic_sym_water_drop", 192, "PDPDDPD"),
    ("No sugar", "ic_sym_no_sugar", 275, "SDSDDSD"),
    ("Workout", "ic_sym_fitness_center", 12, "DSDSDSD"),
    ("Vitamins", "ic_sym_medication", 95, "DDDDDDO"),
    ("Walk outside", "ic_sym_directions_walk", 150, "DPDDSNN"),
    ("Evening journal", "ic_sym_edit_note", 330, "DDDSDDO"),
]
FRACTION = {"P": 0.6}


def row_marks(pattern):
    return [(k, FRACTION.get(k, 0.5)) for k in pattern]


CSS = """
* { box-sizing: border-box; margin: 0; }
.w { font-family: Lexend; font-variation-settings: "wght" 400; border-radius: 28px; position: relative; overflow: hidden; }
.b { font-variation-settings: "wght" 560; }
"""


def ring(progress, size, stroke, colour, track):
    r = (size - stroke) / 2
    circ = 2 * math.pi * r
    return (f'<svg width="{size}" height="{size}" style="position:absolute;inset:0;transform:rotate(-90deg)">'
            f'<circle cx="{size / 2}" cy="{size / 2}" r="{r}" fill="none" stroke="{track}" stroke-width="{stroke}"/>'
            f'<circle cx="{size / 2}" cy="{size / 2}" r="{r}" fill="none" stroke="{colour}" stroke-width="{stroke}" '
            f'stroke-linecap="round" stroke-dasharray="{circ * progress:.2f} {circ:.2f}"/></svg>')


def today(c, dark):
    # WidgetLayout.today at 360 x 164: 16 dp padding, a 104 dp ring, "Up next" and two 48 dp rows.
    w, h = 360, 164
    rows = ""
    for name, _, hue, _ in [x for x in HABITS if x[0] in ("Vitamins", "Evening journal")]:
        hc = habit(hue, dark)
        rows += f'''<div style="height:48px;margin-top:8px;border-radius:16px;background:{hc['soft']};display:flex;align-items:center;padding-left:14px">
  <div class="b" style="flex:1;min-width:0;font-size:16px;color:{hc['ink']};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{name}</div>
  <div style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">
    <div style="width:28px;height:28px;border-radius:50%;border:2.5px solid {hc['solid']}"></div></div>
</div>'''
    body = f'''
<div class="w" style="width:{w}px;height:{h}px;background:{c['bg']};padding:16px;display:flex;align-items:center;gap:16px">
  <div style="width:104px;height:104px;position:relative;flex:none;display:flex;flex-direction:column;align-items:center;justify-content:center">
    {ring(5 / 7, 104, 10, c['primary'], c['track'])}
    <div class="b" style="font-size:28px;line-height:1.1;color:{c['text']};position:relative">5/7</div>
    <div style="font-size:12px;color:{c['muted']};position:relative">today</div>
  </div>
  <div style="flex:1;min-width:0">
    <div class="b" style="font-size:14px;line-height:20px;color:{c['muted']}">Up next</div>
    {rows}
  </div>
</div>'''
    return body, w, h


def week(c, dark):
    # WidgetLayout.week at 360 dp wide: header, day letters, eight 38 dp rows; as tall as that. The
    # marks take 7 x 22 dp so the names fit in Lexend (wider than the phone font the widget uses).
    w = 360
    row_h = 38
    marks_w = 7 * 22
    h = 16 + 48 + 8 + 16 + row_h * len(HABITS) + 16
    letters = "".join(
        f'<div class="b" style="flex:1;text-align:center;font-size:11px;color:{c["text"] if i == 6 else c["muted"]}">{l}</div>'
        for i, l in enumerate("MTWTFSS"))
    rows = ""
    for name, ic, hue, pattern in HABITS:
        hc = habit(hue, dark)
        rows += f'''<div style="height:{row_h}px;display:flex;align-items:center">
  <div style="width:28px;height:28px;border-radius:9px;background:{hc['soft']};display:flex;align-items:center;justify-content:center;flex:none">{icon(ic, hc['ink'], 16)}</div>
  <div class="b" style="flex:1;min-width:0;padding:0 8px 0 10px;font-size:15px;color:{c['text']};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{name}</div>
  <div style="width:{marks_w}px;display:flex;flex:none">{marks(row_marks(pattern), hc, c, 19)}</div>
</div>'''
    body = f'''
<div class="w" style="width:{w}px;height:{h}px;background:{c['bg']};padding:16px">
  <div style="display:flex;align-items:center;height:48px">
    <div style="flex:1;min-width:0">
      <div class="b" style="font-size:22px;line-height:28px;color:{c['text']};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">This week</div>
      <div style="font-size:14px;line-height:20px;color:{c['muted']};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">21 – 27 Sep</div>
    </div>
    <div class="b" style="font-size:28px;color:{c['primary']}">86%</div>
  </div>
  <div style="display:flex;margin-top:8px;height:16px"><div style="flex:1"></div><div style="width:{marks_w}px;display:flex">{letters}</div></div>
  {rows}
</div>'''
    return body, w, h


def strip(c, dark):
    # WidgetLayout.strip at 360 x 72: the count, "Next: …" and one bar per habit due today.
    w, h = 360, 72
    due = [x for x in HABITS if x[3][6] != "N"]
    bars = ""
    for _, _, hue, pattern in due:
        hc = habit(hue, dark)
        fill = hc["solid"] if pattern[6] == "D" else "transparent"
        bars += f'<div style="flex:1;height:10px;border-radius:5px;background:{c["outline"]}59;overflow:hidden"><div style="height:100%;background:{fill}"></div></div>'
    bars = bars.replace(f'{c["outline"]}59', f'color-mix(in srgb, {c["outline"]} 35%, transparent)')
    body = f'''
<div class="w" style="width:{w}px;height:{h}px;background:{c['bg']};padding:0 18px;display:flex;flex-direction:column;justify-content:center">
  <div style="display:flex;align-items:center;gap:12px">
    <div class="b" style="font-size:14px;color:{c['text']};white-space:nowrap">5 of 7 today</div>
    <div style="flex:1;min-width:0;text-align:right;font-size:14px;color:{c['muted']};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">Next: Vitamins, 9:00 PM</div>
  </div>
  <div style="display:flex;gap:4px;margin-top:10px">{bars}</div>
</div>'''
    return body, w, h


def streak(c, dark):
    # WidgetLayout.streak at 176 x 176 (not compact): 18 dp padding, the habit, the streak, 7 marks.
    w = h = 176
    name, ic, hue, _ = HABITS[5]  # Vitamins, like the Welcome panel's streak card
    hc = habit(hue, dark)
    body = f'''
<div class="w" style="width:{w}px;height:{h}px;background:{hc['soft']};color:{hc['ink']};padding:18px;display:flex;flex-direction:column">
  <div style="display:flex;align-items:center;gap:8px">{icon(ic, hc['ink'], 24)}<div class="b" style="flex:1;min-width:0;text-align:right;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{name}</div></div>
  <div style="flex:1"></div>
  <div class="b" style="font-size:40px;line-height:1.1">11</div>
  <div class="b" style="font-size:14px">day streak</div>
  <div style="flex:1"></div>
  <div style="display:flex;margin-top:10px">{marks([("D", 0)] * 7, hc, c, 18)}</div>
</div>'''
    return body, w, h


def page(body, w, h):
    return f'''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: Lexend; src: url("file://{FONT}"); font-weight: 100 900; }}
html, body {{ margin: 0; width: {w}px; height: {h}px; overflow: hidden; background: transparent; }}
{CSS}</style></head><body>{body}</body></html>'''


def render(html, w, h, out_png):
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "page.html")
        open(path, "w", encoding="utf-8").write(html)
        shot = os.path.join(tmp, "shot.png")
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        f"--force-device-scale-factor={SCALE}", f"--window-size={w},{h}",
                        "--default-background-color=00000000", f"--screenshot={shot}", "file://" + path],
                       check=True, capture_output=True)
        shutil.copy(shot, out_png)


def main():
    from PIL import Image
    for dark in (False, True):
        c = scheme(dark)
        folder = os.path.join(RES, "drawable-night-nodpi" if dark else "drawable-nodpi")
        os.makedirs(folder, exist_ok=True)
        for name, draw in (("today", today), ("week", week), ("strip", strip), ("streak", streak)):
            body, w, h = draw(c, dark)
            with tempfile.TemporaryDirectory() as tmp:
                png = os.path.join(tmp, "w.png")
                render(page(body, w, h), w, h, png)
                out = os.path.join(folder, f"widget_preview_{name}.webp")
                Image.open(png).save(out, "WEBP", lossless=True)
                print(os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main()
