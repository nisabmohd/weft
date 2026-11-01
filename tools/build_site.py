"""Builds the GitHub Pages site (https://nisabmohd.github.io/weft/) into _site/: the home page from
site/, three app screenshots from docs/screenshots/, and the privacy policy rendered from PRIVACY.md,
so the repo keeps one copy of the policy. The Pages workflow (.github/workflows/pages.yml) runs it on
every push to main that touches these files.

usage: python3 tools/build_site.py [out folder]     (default: _site, which is gitignored)
Standard library only: the Markdown in PRIVACY.md is simple (headings, paragraphs, lists, bold,
italics, code, links), and this converts just that."""
import html
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "_site")
SCREENS = {"today.png": "02-today.png", "journal.png": "07-journal.png", "insights.png": "09-insights.png"}


def inline(text):
    t = html.escape(text, quote=False)
    t = re.sub(r"`([^`]+)`", r"<code>\1</code>", t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", t)
    t = re.sub(r"(?<![\w*])_([^_]+)_(?![\w*])", r"<em>\1</em>", t)
    t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', t)
    t = re.sub(r"(?<![\"=>])(https?://[^\s<,)]+[^\s<,).])", r'<a href="\1">\1</a>', t)
    t = re.sub(r"(?<![\w.:/])([\w.+-]+@[\w-]+\.[\w.]+\w)", r'<a href="mailto:\1">\1</a>', t)
    return t


def markdown(md):
    """Headings, paragraphs and (nested, two-space) bullet lists."""
    out, para, depth = [], [], 0

    def flush():
        if para:
            out.append(f"<p>{inline(' '.join(para))}</p>")
            para.clear()

    def close_to(level):
        nonlocal depth
        while depth > level:
            out.append("</li></ul>")
            depth -= 1

    for line in md.splitlines():
        m = re.match(r"^( *)- (.*)$", line)
        if m:
            flush()
            level = len(m.group(1)) // 2 + 1
            if level > depth:
                while depth < level:
                    out.append("<ul><li>")
                    depth += 1
            else:
                close_to(level)
                out.append("</li><li>")
            out.append(inline(m.group(2)))
            continue
        if not line.strip():
            flush()
            continue
        close_to(0)
        h = re.match(r"^(#{1,3}) (.*)$", line)
        if h:
            flush()
            n = len(h.group(1))
            out.append(f"<h{n}>{inline(h.group(2))}</h{n}>")
        else:
            para.append(line.strip())
    flush()
    close_to(0)
    return "\n".join(out)


def page(title, body, prefix):
    """The privacy page, with the home page's header and footer (`prefix` leads back to the root)."""
    index = open(os.path.join(ROOT, "site", "index.html"), encoding="utf-8").read()
    head = index[: index.index("<main")]
    foot = index[index.index("<footer") :]
    head = head.replace("<title>Weft: Habit Tracker</title>", f"<title>{title} · Weft</title>")
    for attr in ("href", "src"):
        head = re.sub(rf'{attr}="(?!https?:|mailto:|#)([^"]*)"', lambda m: f'{attr}="{prefix}{m.group(1)}"', head)
        foot = re.sub(rf'{attr}="(?!https?:|mailto:|#)([^"]*)"', lambda m: f'{attr}="{prefix}{m.group(1)}"', foot)
    head = head.replace(f'href="{prefix}./"', f'href="{prefix}"')
    return f'{head}<main class="wrap policy">\n{body}\n  </main>\n\n  {foot}'


if os.path.isdir(OUT):
    shutil.rmtree(OUT)
shutil.copytree(os.path.join(ROOT, "site"), OUT)
os.makedirs(os.path.join(OUT, "screens"))
for name, src in SCREENS.items():
    shutil.copy(os.path.join(ROOT, "docs", "screenshots", src), os.path.join(OUT, "screens", name))
policy = open(os.path.join(ROOT, "PRIVACY.md"), encoding="utf-8").read()
os.makedirs(os.path.join(OUT, "privacy"))
with open(os.path.join(OUT, "privacy", "index.html"), "w", encoding="utf-8") as f:
    f.write(page("Privacy policy", markdown(policy), "../"))
# Pages serves the files as they are (no Jekyll).
open(os.path.join(OUT, ".nojekyll"), "w").close()
print(OUT)
