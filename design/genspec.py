# Generates design/v0.1.0-implementation-spec.html from the baseline mockup.
# Every line citation is EXTRACTED from the mock, never hand-typed, so re-running this
# after a mock edit keeps the spec honest.
import io, re, os, html, datetime

MOCK = r"D:\Android-programs\Trawl\design\v0.1.0-baseline-mockup-ui.html"
OUT  = r"D:\Android-programs\Trawl\design\v0.1.0-implementation-spec.html"
MOCKNAME = "v0.1.0-baseline-mockup-ui.html"

src = io.open(MOCK, encoding="utf-8").read()
lines = src.split("\n")

def find(pat, flags=0):
    """first 1-based line number matching pat"""
    rx = re.compile(pat, flags)
    for i, ln in enumerate(lines, 1):
        if rx.search(ln):
            return i
    return None

def find_all(pat, flags=0):
    rx = re.compile(pat, flags)
    return [(i, ln) for i, ln in enumerate(lines, 1) if rx.search(ln)]

# ── keyframes ──
keyframes = {}
for i, ln in find_all(r"@keyframes\s+([A-Za-z0-9_]+)"):
    name = re.search(r"@keyframes\s+([A-Za-z0-9_]+)", ln).group(1)
    keyframes.setdefault(name, i)

# ── animation declarations, with the selector that owns them ──
# NOTE: a naive regex breaks here because cubic-bezier(a,b,c,d) contains commas, so scanning
# to the first comma truncates the value and silently loses every delay. Scan with paren
# awareness and split only on TOP-LEVEL commas.
def split_top(v, sep=","):
    out, depth, buf = [], 0, ""
    for ch in v:
        if ch == "(": depth += 1
        elif ch == ")": depth -= 1
        if ch == sep and depth == 0:
            out.append(buf); buf = ""
        else:
            buf += ch
    if buf.strip(): out.append(buf)
    return [x.strip() for x in out if x.strip()]

TIME = re.compile(r"^[0-9.]+m?s$")
EASE = re.compile(r"^(cubic-bezier\(.*\)|ease-in-out|ease-out|ease-in|linear|ease|steps\(.*\))$")

anims = []
cur_sel = "?"
sel_rx = re.compile(r"^\s*([^@\s][^{]*?)\{")
for i, ln in enumerate(lines, 1):
    m = sel_rx.match(ln)
    if m and "@keyframes" not in ln:
        cur_sel = m.group(1).strip()
    idx = ln.find("animation:")
    if idx < 0:
        continue
    # take the declaration value with paren awareness
    j, depth, val = idx + len("animation:"), 0, ""
    while j < len(ln):
        ch = ln[j]
        if ch == "(": depth += 1
        elif ch == ")": depth -= 1
        elif (ch == ";" or ch == "}") and depth == 0:
            break
        val += ch
        j += 1
    for one in split_top(val):
        toks = split_top(one, " ") or one.split()
        name = dur = delay = easing = ""
        flags = []
        times = []
        for t in toks:
            if TIME.match(t): times.append(t)
            elif EASE.match(t): easing = t
            elif t in ("both", "forwards", "backwards"): flags.append("fill:" + t)
            elif t == "infinite": flags.append("infinite")
            elif t == "alternate": flags.append("alternate")
            elif t.isdigit(): flags.append("x" + t)
            elif not name and re.match(r"^[A-Za-z][A-Za-z0-9_-]*$", t): name = t
        if times: dur = times[0]
        if len(times) > 1: delay = times[1]
        if not name:
            continue
        anims.append(dict(sel=cur_sel, name=name, dur=dur or "—", delay=delay or "0",
                          ease=easing or "(default)", extra=", ".join(flags) or "—", line=i))

# ── theme token blocks ──
THEMES = ["ember", "hearth", "grove", "plum", "snow", "slate", "sealplus"]
themes = {}
for t in THEMES:
    ln = find(r'\.screen\[data-theme="%s"\]' % t)
    block = []
    if ln:
        j = ln - 1
        depth = 0
        buf = []
        while j < len(lines):
            buf.append(lines[j])
            depth += lines[j].count("{") - lines[j].count("}")
            if depth <= 0 and len(buf) > 1: break
            j += 1
        block = "\n".join(buf)
    toks = dict(re.findall(r"--([a-z0-9]+)\s*:\s*([^;}]+)", block))
    themes[t] = dict(line=ln, toks=toks)

# ── component index (curated selectors, auto line numbers) ──
COMPONENTS = [
 ("App bar",            r"^\.appbar\{",                    "TopAppBar; 56dp tall, transparent unless glass is on."),
 ("App-bar brand",      r"^\.appbar \.brand\{",            "Row(mark 21dp + display-face wordmark 20sp)."),
 ("Switcher button",    r"^\.iconbtn\.switcher\{",         "IconButton; tinted primary; scales .86 + rotates -8deg while open."),
 ("Brand header",       r"^\.brandhead\{",                 "Optional home header. Mark 32dp + 31sp display + fish 25dp."),
 ("URL bar",            r"^\.urlbar\{",                    "58dp tall, 26dp radius, elevation via shadow."),
 ("FAST pill",          r"^\.fastpill\{",                  "26dp pill toggle inside the URL bar."),
 ("Fast tray",          r"^\.fasttray\{",                  "Drops from under the URL bar; -20dp overlap, bottom radius 18dp."),
 ("Quick-tools strip",  r"^\.qstrip\{",                    "Single surface, 20dp radius, 4 equal cells."),
 ("Section heading",    r"^\.sechead\{",                   "12sp, .075em tracking, uppercase, secondary colour."),
 ("List card",          r"^\.card\{",                      "18dp radius, 1dp outline, 12dp padding, 56dp thumb."),
 ("Active download",    r"^\.active\{",                    "Same shell as card + progress bar + haul wash."),
 ("Progress bar",       r"^\.bar\{",                       "5dp track, 99dp radius, width transition .3s linear."),
 ("FAB",                r"^\.fab\{",                       "60dp, 19dp radius, primary container."),
 ("Links row",          r"^\.lrow\{",                      "History row: 52dp thumb, title, mono URL, status pill."),
 ("Status pill",        r"^\.pill\{",                      "Saved / Missing / Failed. 9.5sp, 700, uppercase."),
 ("Bottom sheet",       r"^\.sheet\{",                     "30dp top radius, max 84% height."),
 ("Drawer",             r"^\.drawer\{",                    "292dp wide in Simple; full-bleed in Fancy."),
 ("Drawer item",        r"^\.ditem\{",                     "48dp tall, 24dp radius pill when active."),
 ("Settings row",       r"^\.setrow\{",                    "14dp vertical padding, 1dp top divider."),
 ("Segmented control",  r"^\.seg\{",                       "12dp radius, equal flex, primary fill on selection."),
 ("Toggle",             r"^\.toggle\{",                    "46x27dp track, 21dp thumb, .2s transition."),
 ("Signature banner",   r"^\.sig\{",                       "About header. 24dp radius, watermark, sheened wordmark."),
 ("Link row (About)",   r"^\.linkrow\{",                   "16dp radius, 36dp icon tile."),
 ("Credit card",        r"^\.creditcard\{",                "Upstream attribution. REQUIRED — see D-10."),
 ("Floating bubble",    r"^\.bubble\{",                    "60dp overlay window; rings are conic gradients."),
 ("Bubble ring",        r"^\.ring\{",                      "3dp arc, masked circle, one per download, 6dp inset step."),
 ("Bubble panel",       r"^\.bpanel\{",                    "300dp wide, 22dp radius, task list."),
 ("Drag-to-close X",    r"^\.dropx\{",                     "58dp target, bottom centre, swells 1.28x when hot."),
 ("Quick download",     r"^\.qd\{",                        "The share-intent dialog. 26dp radius."),
 ("Permission dialog",  r"^\.perm\{",                      "Overlay-permission request. 26dp radius."),
 ("Toast",              r"^\.toast\{",                     "13dp radius, bottom 104dp, 1.9s dwell."),
 ("Ambient blobs",      r"^\.blob\{",                      "Radial gradients, 46px blur, opacity .55."),
 ("Film grain",         r"^\.grain\{",                     "SVG fractalNoise, overlay blend, ≤8.5% opacity."),
 ("Motes",              r"^\.mote\{",                      "14 particles, 16-36s rise, opacity ≤.55."),
 ("End-of-list mascot", r"^\.endfish\{",                   "Fish + 'that's the whole catch'."),
 ("Pinned card ring",   r"^\.pinhint\{",                   "Active-window hint in Keep-switcher-open mode."),
]
comps = []
for label, pat, note in COMPONENTS:
    ln = find(pat, re.M)
    comps.append((label, ln, note))

# ── state flags ──
mstate = re.search(r"const D=\{(.*?)\};", src, re.S)
state_line = find(r"const D=\{")
flags = []
if mstate:
    for k, v in re.findall(r"([A-Za-z]+)\s*:\s*('[^']*'|true|false)", mstate.group(1)):
        flags.append((k, v))

# ── request log (chronological; authored, mapped to citations) ──
REQUESTS = [
 ("1", "&ldquo;lets do this&rdquo; — start from <code>CLAUDE.md</code>",
  "Clone Seal Plus as baseline, both upstreams as remotes, verify it builds.",
  "Repo <code>HEAD</code> = <code>811328ac</code> (pristine). Build verified.", "D-01, D-02"),
 ("2", "Scope answers (multi-select round)",
  "Keep <b>all</b> Seal Plus extras — do not orphan them. Strip the donation nag, crypto page, "
  "sponsor pages and the gradient default. Portfolio link in About. Rename app + package, "
  "GPL §5(a) notices, README/About attribution, new launcher icon.",
  "Donation surfaces absent throughout; About carries the portfolio link; icon designed.", "O-02"),
 ("3", "&ldquo;always use the android studio SDK&rdquo;",
  "Build with Studio's SDK and its bundled JDK, not the box default.",
  "SDK <code>D:/Android-SDK</code>; Gradle JVM = Studio JBR 21.", "—"),
 ("4", "Mock round 1 — cozy dark, warm brown or blue, glassmorphism",
  "Replicate the current UI first, then propose Trawl directions.",
  "Seal Plus replica + Ember/Snow.", "D-05"),
 ("5", "Intro like <code>portfolio.html</code>; glass optional (solid first); all themes; "
       "a vibey animated theme; download effects",
  "Add a launch sequence, demote glass to a setting, expand the palette set, add ambient motion "
  "and progress effects.",
  "Intro §5; glass tokens; 7 themes; ambient + FX.", "D-06"),
 ("6", "Own icon + cute mascot; header wordmark (default on, disableable); Seal Plus theme "
       "disable switch; links history; better fast downloader; floating bubble",
  "Identity artwork, a history you can re-download from, a one-tap path, and a draggable "
  "overlay with per-download rings, red on error, green when done, multi-queue, "
  "drag-to-X dismissal.",
  "Icon + fish; brand header; links history; fast tray; bubble.", "D-07, D-08"),
 ("7", "Sidebar animation; download from the bubble; overlay permission; mimic the Figma "
       "slide menu; Simple/Fancy toggle",
  "A motion system with two personalities, quick-download reachable from the overlay, and the "
  "real Android permission.",
  "Simple/Fancy; quick-download dialog; permission gate; manifest entry.", "D-09"),
 ("8", "Reference screenshot of the slide menu",
  "Flat scale + translate, full-bleed menu behind — <b>no 3D rotation</b>.",
  "Fancy drawer transform.", "D-09"),
 ("9", "Remove the X; move &ldquo;Built on&rdquo;; banner + portfolio on About; drop &ldquo;fork of "
       "Seal Plus&rdquo; from the version line; restyle the sidebar button; window-switch feel; "
       "remove the gear (with an opt-in)",
  "Turn the drawer into a window switcher and give the About page the identity work.",
  "Switcher glyph; switchTo(); About page; credit card.", "D-10"),
 ("10", "Intro fish barely visible; mascot elsewhere, animated, toggleable",
  "Reposition and animate the mascot; give it a home outside the intro.",
  "Fish paired with the mark; end-of-list mascot; Mascot toggle.", "D-07"),
 ("11", "Seal Plus theme must still say <b>Trawl</b>, with a faint &ldquo;Seal +&rdquo; subtitle",
  "The app never calls itself Seal, even wearing the inherited theme.",
  "Brand block title + subtitle.", "D-04, D-10"),
 ("12", "&ldquo;the sidebar toggle aint working, it just flickers&rdquo;",
  "Bug: the switcher button sits inside the pushed card, so its click bubbled to the "
  "tap-to-close handler and shut the drawer in the same event.",
  "<code>stopPropagation</code> on bar controls + <code>.iconbtn</code> guard.", "—"),
 ("13", "&ldquo;why is the underline under Trawl gone, why is the fish oddly placed&rdquo;",
  "Regression: <code>introline</code> and its keyframe were dropped in a wholesale rewrite; "
  "the rule also drew after the fade began.",
  "Rule restored and retimed to finish at 1230ms.", "—"),
 ("14", "History button behaves like the gear; hide the gear on Settings; option to keep the "
        "switcher open (select a window, then enter it)",
  "Context-aware shortcuts and a manual task-switcher mode.",
  "navVia(); context hiding; Keep switcher open.", "D-11"),
 ("15", "&ldquo;list every prompt and decision, cite where each is implemented, 1:1&rdquo;",
  "This document, plus the <code>ui-mock-first</code> skill updated to require it.",
  "This spec.", "—"),
]

# ── behaviour rules ──
RULES = [
 ("Reduced motion", "Every animation lives inside <code>@media (prefers-reduced-motion: no-preference)</code>. "
  "The assembled UI is the default state — a reduced-motion user gets a finished screen, never a blank one.",
  find(r"@media \(prefers-reduced-motion")),
 ("Intro can never strand", "The curtain is removed from the box tree outside the intro, and two independent "
  "timers tear the sequence down (3000ms, 4300ms).", find(r"setTimeout\(end,3000\)")),
 ("Glass defaults OFF", "<code>RenderEffect</code> blur is API 31+; <code>minSdk</code> is 24. The solid path "
  "must be the tested one.", find(r'\.screen\{--glassbg')),
 ("Bubble needs permission", "<code>SYSTEM_ALERT_WINDOW</code> is a special permission. Progress must stay "
  "visible in the notification when it is denied.", find(r"ov-perm")),
 ("Never blur list rows", "Glass applies to app bar, URL bar, tool strip, sheet, drawer, bubble — never to "
  "scrolling list items.", find(r"^\.gsurf\{", re.M)),
 ("Screen ≠ drawer state", "<code>D.screen</code> and <code>D.drawerOpen</code> are independent; conflating "
  "them makes window preview impossible.", state_line),
 ("Attribution is required", "The credit card on About is a project requirement, not optional chrome. It may "
  "move; it may not shrink or disappear.", find(r"creditcard")),
 ("Shortcut hides at its destination", "No gear on Settings, no clock on Links history.",
  find(r"a shortcut to the screen you are already on")),
]

# ═══════════ emit ═══════════
def esc(x): return html.escape(str(x)) if x is not None else "—"

def cite(n):
    return '<span class="cite">%s:%s</span>' % (MOCKNAME, n) if n else '<span class="cite miss">not found</span>'

theme_rows = ""
TOK_ORDER = ["bg","surface","surfvar","surfcon","surfhigh","primary","primary2","accent","text","text2","outline","onprimary"]
for t in THEMES:
    d = themes[t]
    cells = "".join('<td><code>%s</code></td>' % esc(d["toks"].get(k, "—").strip()) for k in TOK_ORDER)
    sw = d["toks"].get("primary", "#888").strip()
    bg = d["toks"].get("bg", "#111").strip()
    theme_rows += ('<tr><th><span class="sw" style="background:linear-gradient(135deg,%s 50%%,%s 50%%)"></span>%s'
                   '<br>%s</th>%s</tr>' % (bg, sw, t, cite(d["line"]), cells))

anim_rows = ""
for a in sorted(anims, key=lambda x: x["line"]):
    anim_rows += ("<tr><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td>"
                  "<td><code>%s</code></td><td>%s</td><td>%s</td></tr>" % (
        esc(a["name"]), esc(a["sel"][:52]), esc(a["dur"]), esc(a["delay"]),
        esc(a["ease"]), esc(a["extra"]), cite(a["line"])))

kf_rows = "".join("<tr><td><code>@keyframes %s</code></td><td>%s</td></tr>" % (esc(k), cite(v))
                  for k, v in sorted(keyframes.items()))

comp_rows = "".join("<tr><th>%s</th><td>%s</td><td>%s</td></tr>" % (esc(l), cite(n), note)
                    for l, n, note in comps)

req_rows = ""
for n, ask, meaning, impl, dec in REQUESTS:
    req_rows += ('<tr><td class="num">%s</td><td class="ask">%s</td><td>%s</td><td>%s</td>'
                 '<td><code>%s</code></td></tr>' % (n, ask, meaning, impl, dec))

rule_rows = "".join("<tr><th>%s</th><td>%s</td><td>%s</td></tr>" % (esc(t), b, cite(n))
                    for t, b, n in RULES)

flag_rows = "".join("<tr><td><code>%s</code></td><td><code>%s</code></td></tr>" % (esc(k), esc(v))
                    for k, v in flags)

CHECK = [
 "Every colour is read from the token table, never eyeballed.",
 "Every animation's duration, delay and easing matches the motion index exactly.",
 "Fancy drawer is a flat scale+translate — no rotationY, no perspective.",
 "Glass defaults to OFF and never applies to list rows.",
 "Intro is ≤2.4s, skipped under reduced motion, and cannot leave a curtain up.",
 "Header wordmark, Seal Plus theme, mascot, bubble, fast download, quick gear, quick history and "
 "Keep-switcher-open are all real settings with the stated defaults.",
 "Bubble: one ring per download, accent/green/red states, ≤4 rings then a count, drag-to-X dismiss, "
 "off-switch inside its own panel.",
 "Shortcut buttons hide on the screen they point at.",
 "Seal Plus theme titles the app 'Trawl'.",
 "About carries the signature banner, portfolio link, GitHub link and the full upstream credit.",
 "Every modified upstream file carries a dated GPL §5(a) notice.",
]
check_rows = "".join("<li>%s</li>" % c for c in CHECK)

TPL = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<title>Trawl — v0.1.0 Implementation Spec</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Fraunces:opsz,wght@9..144,600;9..144,700&display=swap" rel="stylesheet">
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
:root{--bg:#0F0B08;--panel:#181109;--line:#2E2118;--text:#F6EDE5;--dim:#AE9C8E;--acc:#E0925A;--ok:#67C98C;--bad:#E2685F;
 --font:'Inter',system-ui,sans-serif;--disp:'Fraunces',Georgia,serif}
body{background:var(--bg);color:var(--text);font-family:var(--font);line-height:1.6;padding:38px 26px 80px}
.wrap{max-width:1180px;margin:0 auto}
h1{font-family:var(--disp);font-size:34px;font-weight:700;letter-spacing:-.01em}
.sub{color:var(--dim);font-size:14px;margin-top:8px;max-width:80ch}
h2{font-family:var(--disp);font-size:22px;font-weight:600;margin:44px 0 6px;padding-top:22px;border-top:1px solid var(--line)}
h2 .n{color:var(--acc);font-size:14px;font-family:var(--font);font-weight:700;letter-spacing:.1em;display:block;margin-bottom:4px}
h3{font-size:14px;font-weight:650;margin:22px 0 8px;color:var(--acc)}
p{color:var(--dim);font-size:13.5px;margin:8px 0;max-width:88ch}
code{font-family:ui-monospace,Consolas,monospace;font-size:12px;background:#241A12;padding:1.5px 5px;border-radius:5px;color:#E8C9A0}
table{width:100%;border-collapse:collapse;margin:12px 0 6px;font-size:12.5px}
th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line);vertical-align:top}
thead th{font-size:10.5px;letter-spacing:.09em;text-transform:uppercase;color:var(--dim);font-weight:700;
 border-bottom:1px solid var(--acc);white-space:nowrap}
tbody th{font-weight:600;color:var(--text);white-space:nowrap}
tr:hover td,tr:hover th{background:rgba(224,146,90,.045)}
td{color:var(--dim)}
.cite{font-family:ui-monospace,Consolas,monospace;font-size:10.5px;color:var(--ok);white-space:nowrap}
.cite.miss{color:var(--bad)}
.num{color:var(--acc);font-weight:700;font-family:ui-monospace,monospace}
.ask{color:var(--text)}
.sw{display:inline-block;width:13px;height:13px;border-radius:4px;margin-right:7px;vertical-align:-2px;
 border:1px solid rgba(255,255,255,.18)}
.box{background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:16px 18px;margin:14px 0}
.box b{color:var(--text)}
.box.warn{border-color:rgba(226,104,95,.4)}
ul{margin:8px 0 8px 20px}li{font-size:13px;color:var(--dim);margin:5px 0}
li b{color:var(--text)}
.scroll{overflow-x:auto}
.foot{margin-top:50px;padding-top:18px;border-top:1px solid var(--line);color:var(--dim);font-size:12px}
</style></head><body><div class="wrap">

<h1>Trawl &mdash; v0.1.0 Implementation Spec</h1>
<p class="sub">The contract between <code>__MOCK__</code> and the Compose code. Every citation below is the
<b>file and line in the mockup</b> that defines the behaviour, extracted programmatically &mdash; not typed by
hand &mdash; so re-running the generator after a mock edit keeps this honest. Where the two disagree,
<b>the mockup wins</b>.</p>

<div class="box warn"><b>Rule for the implementer.</b> Copy values; do not match them by eye. If a
behaviour here is awkward in Compose, build it anyway or raise it explicitly &mdash; never substitute
silently, and never report it done when it was simplified.</div>

<h2><span class="n">Section 1</span>Request log &mdash; in order</h2>
<p>Every instruction given, chronologically, with what it demanded and where it landed. Reversals are
visible on purpose.</p>
<div class="scroll"><table>
<thead><tr><th>#</th><th>The ask</th><th>What it meant</th><th>Where it landed</th><th>Decision</th></tr></thead>
<tbody>__REQS__</tbody></table></div>

<h2><span class="n">Section 2</span>Design tokens &mdash; all seven themes</h2>
<p>Literal values. These become a Compose <code>ColorScheme</code> per theme plus an extended token object
for the ones Material does not model (<code>surfhigh</code>, <code>outline</code>, ambient stops).</p>
<div class="scroll"><table>
<thead><tr><th>Theme</th>__TOKHEAD__</tr></thead>
<tbody>__THEMES__</tbody></table></div>

<h2><span class="n">Section 3</span>Component index</h2>
<p>Each row cites the selector that defines the component's geometry. CSS <code>px</code> maps 1:1 to
Compose <code>dp</code>; font <code>px</code> maps to <code>sp</code>.</p>
<div class="scroll"><table>
<thead><tr><th>Component</th><th>Defined at</th><th>Implementation note</th></tr></thead>
<tbody>__COMPS__</tbody></table></div>

<h2><span class="n">Section 4</span>Motion index</h2>
<p>The part most often lost in translation. A CSS <code>cubic-bezier(a,b,c,d)</code> becomes a Compose
<code>CubicBezierEasing(a,b,c,d)</code>; a duration in <code>ms</code> becomes <code>tween(durationMillis=…,
delayMillis=…, easing=…)</code>. <code>fill:both</code> means the element must already be at its start state
before the animation runs &mdash; in Compose that is an initial state, not a side effect.</p>
<div class="scroll"><table>
<thead><tr><th>Animation</th><th>Applied to</th><th>Duration</th><th>Delay</th><th>Easing</th><th>Flags</th><th>Defined at</th></tr></thead>
<tbody>__ANIMS__</tbody></table></div>

<h3>Keyframe definitions</h3>
<div class="scroll"><table>
<thead><tr><th>Keyframe</th><th>Defined at</th></tr></thead><tbody>__KFS__</tbody></table></div>

<h2><span class="n">Section 5</span>State model</h2>
<p>The mock's single state object is the shape the Compose <code>ViewModel</code> / preference store should
mirror. Defaults shown are the shipping defaults.</p>
<div class="scroll"><table>
<thead><tr><th>Flag</th><th>Default</th></tr></thead><tbody>__FLAGS__</tbody></table></div>
<p>Declared at __STATELINE__.</p>

<h2><span class="n">Section 6</span>Behaviour rules that a screenshot cannot show</h2>
<div class="scroll"><table>
<thead><tr><th>Rule</th><th>Why</th><th>Defined at</th></tr></thead><tbody>__RULES__</tbody></table></div>

<h2><span class="n">Section 7</span>Acceptance checklist</h2>
<p>Diff the implementation against this, item by item, before calling any screen done.</p>
<ul>__CHECKS__</ul>

<div class="foot">Generated __DATE__ from <code>__MOCK__</code> (__NLINES__ lines).
Regenerate with <code>design/genspec.py</code> after any mock edit &mdash; a stale citation is worse than none.
Decision rationale lives in <code>DECISIONS.md</code> (D-01…D-11, O-01…O-02).</div>
</div></body></html>
"""

_vals = dict(
    MOCK=MOCKNAME,
    REQS=req_rows,
    TOKHEAD="".join("<th>%s</th>" % k for k in TOK_ORDER),
    THEMES=theme_rows,
    COMPS=comp_rows,
    ANIMS=anim_rows,
    KFS=kf_rows,
    FLAGS=flag_rows,
    STATELINE=cite(state_line),
    RULES=rule_rows,
    CHECKS=check_rows,
    DATE=datetime.date.today().isoformat(),
    NLINES=str(len(lines)),
)
doc = TPL
for k, v in _vals.items():
    doc = doc.replace("__%s__" % k, v)

io.open(OUT, "w", encoding="utf-8", newline="\n").write(doc)
print("spec written: %s (%d bytes)" % (os.path.basename(OUT), len(doc)))
print("  themes:%d  components:%d  animations:%d  keyframes:%d  flags:%d"
      % (len(themes), len(comps), len(anims), len(keyframes), len(flags)))
missing = [l for l, n, _ in comps if not n]
print("  UNRESOLVED component citations:", missing or "none")
