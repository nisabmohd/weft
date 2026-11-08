"""Redraws Weft's icons with Solar (Linear, by 480 Design, CC BY 4.0), keeping every file name.

Each drawable in ICONS is overwritten with the Solar Linear icon of the same meaning, black (Compose's
Icon and the widgets' colorFilter tint it), viewport 24. Brand marks (ic_weft, launcher, notification,
mid apps, shortcuts, widget backgrounds) are left alone.

usage: npm pack @iconify-json/solar && tar xzf iconify-json-solar-*.tgz   # outside the repo
       python3 tools/solar_icon.py <path>/package            # every icon
       python3 tools/solar_icon.py <path>/package ic_check   # one
"""
import json, os, sys
import xml.etree.ElementTree as ET

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res", "drawable")

# drawable name → Solar name (Linear style, without the "-linear" suffix)
ICONS = {
    # App chrome
    "ic_archive": "archive-down", "ic_unarchive": "archive-up",
    "ic_arrow_back": "arrow-left", "ic_bell_filled": "bell", "ic_calendar": "calendar",
    "ic_check": "check", "ic_chevron_down": "alt-arrow-down", "ic_chevron_left": "alt-arrow-left",
    "ic_chevron_right": "alt-arrow-right", "ic_chevron_up": "alt-arrow-up", "ic_close": "close",
    "ic_cloud": "cloud", "ic_cloud_done": "cloud-check", "ic_cloud_upload": "cloud-upload",
    "ic_code": "code", "ic_delete": "trash-bin-trash", "ic_download": "download-minimalistic",
    "ic_drag": "reorder", "ic_error": "danger-circle", "ic_filter": "filter", "ic_habit_pen": "pen-new-square",
    "ic_heart": "heart", "ic_minus": "minus", "ic_more_horiz": "menu-dots", "ic_note": "notes",
    "ic_open_external": "arrow-right-up", "ic_music_note": "music-note-2", "ic_volume_loud": "volume-loud", "ic_plus": "add", "ic_refresh": "refresh", "ic_search": "magnifier",
    "ic_share": "share", "ic_skip": "forward", "ic_smartphone": "smartphone", "ic_star": "star",
    "ic_thumb_up": "like", "ic_translate": "translation", "ic_trending_down": "graph-down",
    "ic_trophy": "cup-star", "ic_undo": "undo-left",
    "ic_switch": "transfer-horizontal", "ic_link": "link-minimalistic-2", "ic_pen": "pen",
    # Nav bar
    "ic_nav_today": "home-smile", "ic_nav_habits": "checklist-minimalistic", "ic_nav_journal": "notebook",
    "ic_nav_insights": "chart-square", "ic_nav_more": "tuning-2",
    # Habit icons
    "ic_sym_accessibility_new": "stretching", "ic_sym_account_balance_wallet": "wallet",
    "ic_sym_alarm": "alarm", "ic_sym_bed": "bed", "ic_sym_bedtime": "moon-sleep",
    "ic_sym_breakfast_dining": "donut-bitten", "ic_sym_brush": "palette", "ic_sym_call": "phone",
    "ic_sym_child_friendly": "balloon", "ic_sym_cleaning_services": "smart-vacuum-cleaner-2", "ic_sym_code": "code-square",
    "ic_sym_dentistry": "custom:tooth", "ic_sym_directions_bike": "bicycling",
    "ic_sym_directions_run": "running", "ic_sym_directions_walk": "walking", "ic_sym_edit_note": "pen-new-square",
    "ic_sym_elderly": "user-heart-rounded", "ic_sym_extension": "blocks", "ic_sym_favorite": "heart-pulse",
    "ic_sym_fitness_center": "dumbbell-large-minimalistic", "ic_sym_folded_hands": "hand-stars",
    "ic_sym_group": "users-group-rounded", "ic_sym_headphones": "headphones-round", "ic_sym_hiking": "hiking",
    "ic_sym_laptop_mac": "laptop", "ic_sym_local_cafe": "cup-hot", "ic_sym_local_laundry_service": "washing-machine",
    "ic_sym_mail": "letter", "ic_sym_medication": "pills", "ic_sym_menu_book": "book-minimalistic",
    "ic_sym_mobile_off": "slash:smartphone", "ic_sym_monitor_weight": "scale", "ic_sym_music_note": "music-note",
    "ic_sym_no_drinks": "slash:wineglass", "ic_sym_no_meals": "hourglass", "ic_sym_nutrition": "leaf",
    "ic_sym_park": "cloud-sun", "ic_sym_pets": "paw", "ic_sym_photo_camera": "camera", "ic_sym_piano": "music-notes",
    "ic_sym_pool": "swimming", "ic_sym_potted_plant": "custom:plant", "ic_sym_psychology": "brain",
    "ic_sym_recycling": "refresh-circle", "ic_sym_restaurant": "chef-hat-minimalistic",
    "ic_sym_savings": "money-bag", "ic_sym_school": "square-academic-cap",
    "ic_sym_self_improvement": "meditation", "ic_sym_sentiment_satisfied": "smile-circle",
    "ic_sym_shopping_cart": "cart-large-2", "ic_sym_shower": "bath", "ic_sym_skillet": "ladle",
    "ic_sym_smoke_free": "slash:custom:cigarette", "ic_sym_spa": "cosmetic", "ic_sym_sports_basketball": "basketball",
    "ic_sym_sports_cricket": "custom:cricket", "ic_sym_sports_martial_arts": "user-hand-up",
    "ic_sym_sports_soccer": "football", "ic_sym_sports_tennis": "tennis", "ic_sym_stairs": "custom:stairs",
    "ic_sym_task_alt": "check-circle", "ic_sym_translate": "earth", "ic_sym_tv_off": "slash:tv",
    "ic_sym_volunteer_activism": "hand-heart", "ic_sym_water_drop": "waterdrop", "ic_sym_wb_sunny": "sunrise",
    "ic_sym_yard": "trellis",
}

# Solar has no tooth, cricket, stairs, potted plant or cigarette: drawn here in its Linear style
# (24 grid, 1.5 stroke, round caps and joins) (user, 2026-11-02: every habit icon must mean what it says).
_L = 'fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"'
CUSTOM = {
    "tooth": f'<path {_L} d="M7.5 3.5C5 3.5 3.75 5.5 3.75 8c0 2 .75 3.5 1.25 5 .6 1.8.75 4 1.25 5.75.35 1.2 1 2 1.75 2 1 0 1.25-1 1.5-2.5.3-1.8.75-3.5 2.5-3.5s2.2 1.7 2.5 3.5c.25 1.5.5 2.5 1.5 2.5.75 0 1.4-.8 1.75-2 .5-1.75.65-3.95 1.25-5.75.5-1.5 1.25-3 1.25-5 0-2.5-1.25-4.5-3.75-4.5-1.75 0-2.75 1-4.5 1s-2.75-1-4.5-1Z"/>',
    "cricket": f'<path {_L} d="M14.8 6.8l2.4 2.4-8.8 8.8a1.7 1.7 0 0 1-2.4 0 1.7 1.7 0 0 1 0-2.4Z"/><path {_L} d="M16 8l3.5-3.5"/><circle {_L} cx="17.5" cy="17.5" r="2.25"/>',
    "stairs": f'<path {_L} d="M3 20.5h5v-5h4.5v-5H17v-5h4"/>',
    "plant": f'<path {_L} d="M7 14h10l-1.25 6.25a1 1 0 0 1-1 .75h-5.5a1 1 0 0 1-1-.75Z"/><path {_L} d="M12 14V8.5"/><path {_L} d="M12 11c0-3-2.25-4.5-5.5-4.5 0 3 2.25 4.5 5.5 4.5Z"/><path {_L} d="M12 8.5c0-3.25 2-5 5.5-5 0 3.25-2 5-5.5 5Z"/>',
    "cigarette": f'<path {_L} d="M3 14.5h13.5v3H3Z"/><path {_L} d="M19 14.5v3M21 14.5v3M19 11.5c0-1.5 2-1.5 2-3s-1-2-1-2"/>',
}
# An "off" icon: the glyph is cut along the diagonal and a slash drawn through the gap.
SLASH_CLIP = "M2.1,0H24V21.9ZM0,2.1V24H21.9Z"
SLASH = "M3.5,3.5L20.5,20.5"

INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule")


def f(v):
    return f"{float(v):g}"


def path_data(kind, a):
    if kind == "path":
        return a["d"]
    if kind in ("circle", "ellipse"):
        cx, cy = float(a["cx"]), float(a["cy"])
        rx, ry = float(a.get("r", a.get("rx"))), float(a.get("r", a.get("ry")))
        return f"M{f(cx - rx)},{f(cy)}a{f(rx)},{f(ry)} 0 1,0 {f(2 * rx)},0a{f(rx)},{f(ry)} 0 1,0 {f(-2 * rx)},0Z"
    if kind == "rect":
        x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height"))
        r = min(float(a.get("rx", a.get("ry", 0))), w / 2, h / 2)
        if not r:
            return f"M{f(x)},{f(y)}h{f(w)}v{f(h)}h{f(-w)}Z"
        return (f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(r)}v{f(h - 2 * r)}"
                f"a{f(r)},{f(r)} 0 0 1 {f(-r)},{f(r)}h{f(-(w - 2 * r))}a{f(r)},{f(r)} 0 0 1 {f(-r)},{f(-r)}"
                f"v{f(-(h - 2 * r))}a{f(r)},{f(r)} 0 0 1 {f(r)},{f(-r)}Z")
    raise ValueError(f"unknown element {kind}")


def shapes(body):
    out = []

    def walk(el, alpha, paint):
        a = el.attrib
        if "transform" in a or "clip-path" in a or "mask" in a or "stroke-dasharray" in a:
            raise ValueError(f"unsupported attribute on <{el.tag}>")
        alpha *= float(a.get("opacity", 1))
        paint = {**paint, **{k: a[k] for k in INHERITED if k in a}}
        if el.tag == "g":
            for c in el:
                walk(c, alpha, paint)
        elif el.tag in ("path", "circle", "ellipse", "rect"):
            out.append((path_data(el.tag, a), {**paint, "alpha": alpha}))
        elif el.tag != "defs":
            raise ValueError(f"unknown element <{el.tag}>")

    for el in ET.fromstring(f"<svg>{body}</svg>"):
        walk(el, 1.0, {"fill": "currentColor"})
    return out


def drawable(solar, els, slash=False):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        (f'<!-- Drawn for Weft in Solar Linear style ({solar}). Tinted where it is drawn. -->' if "custom:" in solar
         else f'<!-- Solar {solar} (Linear) by 480 Design, CC BY 4.0. Tinted where it is drawn. -->'),
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">',
    ]
    if slash:
        lines.append(f'    <group>\n    <clip-path android:pathData="{SLASH_CLIP}" />')
    for d, p in els:
        attrs = []
        if p.get("fill", "none") != "none":
            attrs.append('android:fillColor="#FF000000"')
            if p.get("fill-rule") == "evenodd":
                attrs.append('android:fillType="evenOdd"')
            if p["alpha"] != 1:
                attrs.append(f'android:fillAlpha="{f(p["alpha"])}"')
        if p.get("stroke", "none") != "none":
            attrs.append('android:strokeColor="#FF000000"')
            attrs.append(f'android:strokeWidth="{p.get("stroke-width", "1")}"')
            if p.get("stroke-linecap") in ("round", "square"):
                attrs.append(f'android:strokeLineCap="{p["stroke-linecap"]}"')
            if p.get("stroke-linejoin") in ("round", "bevel"):
                attrs.append(f'android:strokeLineJoin="{p["stroke-linejoin"]}"')
            if p["alpha"] != 1:
                attrs.append(f'android:strokeAlpha="{f(p["alpha"])}"')
        if attrs:
            lines.append(f'    <path {" ".join(attrs)} android:pathData="{d}" />')
    if slash:
        lines.append("    </group>")
        lines.append(f'    <path android:strokeColor="#FF000000" android:strokeWidth="1.5" android:strokeLineCap="round" android:pathData="{SLASH}" />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def main(pkg, names):
    icons = json.load(open(os.path.join(os.path.expanduser(pkg), "icons.json")))["icons"]
    problems = []
    for name in names or sorted(ICONS):
        solar = ICONS[name]
        slash = solar.startswith("slash:")
        base = solar.removeprefix("slash:")
        key = f"{base}-linear"
        try:
            if base.startswith("custom:"):
                els = shapes(CUSTOM[base.removeprefix("custom:")])
            elif key not in icons:
                raise ValueError("not in the set")
            else:
                els = shapes(icons[key]["body"])
        except ValueError as e:
            problems.append(f"{name}: {key}: {e}")
            continue
        open(os.path.join(OUT, f"{name}.xml"), "w").write(drawable(solar, els, slash))
    if problems:
        sys.exit("\n".join(problems))


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2:])
