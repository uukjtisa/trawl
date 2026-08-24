# DECISIONS — Trawl

A **learning log**, not a changelog. Governed by Rule 18 in `CLAUDE_RULES.md`.

- **`D-nn`** — a decision Claire made.
- **`O-nn`** — a decision of Nic's that overruled Claire's, logged with an honest verdict.
- Every entry carries **Further reading**: real, searchable concepts and sources.

---

# D-01 · Fork from Seal Plus, not from the original Seal

**Date:** 2026-08-24 · **Status:** settled

**The call.** Base Trawl on `MaheshTechnicals/Sealplus`, not `JunkFood02/Seal`.

**Why.** The original's last substantive commit was **6 April 2025**. Everything after it
is an automated monthly `docs(readme): update sponsor info` bot — sixteen months of zero
development wearing the appearance of activity, with 28.4k stars and no archive notice to
warn anyone. Seal Plus is current: SDK 37, Kotlin 2.3.21, yt-dlp 2025.12.08.

Forking the original would mean reproducing that entire catch-up by hand — an Android SDK
jump, a Kotlin major version, and a yt-dlp bump across a codebase you did not write —
before writing a single line of your own. That work is already done and published under a
licence that explicitly permits taking it. Refusing to take it is not principle, it is
waste.

**The general lesson, which is the more valuable part.** *Repository activity is not
maintenance.* Stars, recent commit dates and an absent archive notice are all trivially
satisfied by a bot. Before depending on or forking any project, check the last commit that
**changed code**, not the last commit. This one was disguised well enough that the first
automated summary of the repo reported it as "actively maintained."

**What it costs.** Trawl inherits Seal Plus's decisions as well as its currency — its
theming choices, its added features, and any bugs introduced since the split. Since the
stated goal is to replace its UI anyway, much of that inheritance gets overwritten. Two
upstreams must now be tracked instead of one.

**Further reading.**
- Search: "software supply chain risk assessment", "abandonware detection open source",
  "bus factor", "how to evaluate a dependency before adopting it", "protestware and
  maintainer burnout"

---

# D-02 · GPL-3.0 is inherited, and §5(a) is the clause to actually obey

**Date:** 2026-08-24 · **Status:** settled, non-negotiable

**The call.** Trawl is GPL-3.0. Every file modified from upstream carries a prominent
notice that it was changed, and the date.

**Why.** GPL-3 is copyleft: a derivative work must carry the same licence. That much is
widely understood. The part forks routinely miss is **§5(a)** — modified files must state
that they were modified and when. It exists so that a reader can tell whose work they are
looking at, which matters precisely because this project's stated goal is to credit two
upstreams honestly.

There is a second, practical reason to obey it: it makes Trawl's own contribution
legible. A repo where the modified files are marked is one where a reader — a client, an
employer, or Nic in a year — can see exactly which parts are his.

**What flagship projects do.** Long-lived forks of copyleft software (LibreOffice from
OpenOffice, MariaDB from MySQL, Jenkins from Hudson) all keep upstream copyright headers
intact and add their own alongside rather than replacing them. Stripping headers is the
single most common licence violation in forked code, and it is trivially detectable.

**What it costs.** Trawl can never be closed-sourced or dual-licensed. Given it is a
personal customisation project, that costs nothing real.

**Further reading.**
- The GNU GPL v3 text, especially **§5** (conveying modified source versions), and the
  FSF's GPL FAQ on derivative works
- Search: "GPL v3 section 5a modified file notice", "copyright header preservation fork",
  "SPDX license identifiers", "REUSE specification"

---

# D-03 · UI-only scope, and settle the complaint before touching code

**Date:** 2026-08-24 · **Status:** settled

**The call.** The engine, downloader, yt-dlp integration and app architecture stay as
inherited. Trawl's contribution is the interface. And **before any code is written,
establish specifically what is wrong with Seal Plus's UI.**

**Why.** "I don't like the UI" spans at least four different projects with wildly different
costs:

| The real objection | The actual work |
|---|---|
| The gradient/glassmorphism theme | Swap a colour scheme. Hours |
| Component styling | Restyle shared composables. Days |
| Screen layout and density | Rework each screen. Weeks |
| Navigation and information architecture | Re-architecture. Much longer |

Seal Plus advertises its "gradient dark theme with glassmorphism" as a headline feature,
which makes a theme-level objection the most likely reading — and that is the cheapest of
the four by an order of magnitude. Starting to code before knowing which one this is
guarantees either over-building or a stall.

**The third option worth considering.** Seal's *original* Material You interface was widely
liked — it is much of why the project earned 28.4k stars. Reverting the UI while keeping
Seal Plus's current engine is legitimate, far cheaper than designing from scratch, and may
be exactly what "I don't like Mahesh's UI" means in practice.

**What it costs.** Staying out of the engine means inheriting its bugs and its release
cadence. Acceptable, and the correct trade for a personalisation project — the engine is
where the specialist knowledge lives, and it is maintained by people who have it.

**Further reading.**
- Search: "requirements elicitation — the stated problem vs the real one", "design system
  tokens vs component restyling", "Material You dynamic color", "Jetpack Compose theming",
  "information architecture vs visual design"

---

# O-01 · Fork it anyway, after deciding not to

**Date:** 2026-08-24 · **Overrules:** Claire's recommendation to just install Seal Plus and
stop

**What happened.** The stated problem was "I use this all the time but now it doesn't
work." Claire's recommendation was that installing an existing maintained fork solves that
in five minutes with zero engineering, and that forking to fix a broken install is
solving the wrong problem. Nic agreed, said "don't fork it" — then reversed on a different
basis: he dislikes Seal Plus's UI and wants a personalised version, possibly with his own
features.

**Verdict: the reversal is sound, because the justification changed.**

The original argument against forking still stands on its own terms — forking is a bad way
to fix a broken app, and installing Seal Plus was the right answer to *that* problem. But
"I want an interface I like, and maybe features nobody else will build" is a different
requirement, and forking is the correct answer to it. Nothing was contradicted; a new goal
replaced an old one, and the fix for the original complaint is already in hand.

This is worth recording because the two decisions look contradictory in a log and are not.
**Changing course because the goal changed is not indecision.** Changing course while the
goal stays the same is.

*One caution carried forward:* scope. A UI-only fork is a weekend or two. "Independent new
features" on top of an inherited codebase you did not write is open-ended, and the engine
half of this app — a bundled Python runtime, native libraries, ABI splits — is genuinely
intricate. D-03's scope boundary exists to keep that from happening by accident.

**Further reading.**
- Search: "sunk cost fallacy vs changed requirements", "scope creep in forks", "why
  maintainers abandon projects"


---

# D-04 · Named Trawl — deliberately not derived from "Seal"

**Date:** 2026-08-24 · **Status:** settled

**The call.** The project is **Trawl**, package `dev.niccc2007.trawl`. An earlier working
title, "NxSeal", was discarded.

**Why.** The `JunkFood02/Seal` README states, verbatim:

> "Except for the source code licensed under the GPLv3 license, all other parties are
> prohibited from using Seal's name as a downloader app, and the same is true for Seal's
> derivatives. Derivatives include but are not limited to forks and unofficial builds."

Note the construction: the name is carved **out of** the GPL grant deliberately. The licence
conveys the code; the name is withheld separately. This is legitimate and explicitly
contemplated by GPL §7(e), which permits declining to grant trademark rights alongside code.
The position upstream takes is: take all the code, do not call it Seal.

Whether that restriction is *enforceable* is a separate question from whether to respect it.
"Seal" is a common English word with no apparent registration behind it, so a trademark claim
would be weak, and Seal Plus has carried the name for over a year without consequence. The
realistic exposure is a GitHub takedown request, which is a low-probability annoyance rather
than a threat.

**It was respected anyway, for a reason that is not legal.** Crediting both upstream authors
properly is a stated requirement of this project (`ATTRIBUTION.md` exists solely for it).
Publishing a document thanking an author while ignoring the single thing that author asked of
forks is incoherent. The naming line is the one request they made; honouring it costs a word.

**Why Trawl specifically.** A trawl is a net dragged to haul things up — it describes what the
app does, needs no explanation, is instantly searchable, and shares no morpheme with "Seal".
It also avoids competing with Seal and Seal Plus for every search query, which a Seal-derived
name guarantees.

**The mythology detour, and the lesson in it.** Greek and Norse sea deities were considered:
**Proteus** (the Old Man of the Sea who herded Poseidon's seals in *Odyssey* Bk 4 — a literal
seal-keeper, and *protean* means shape-shifting, apt for a 1,700-site extractor), **Rán** (the
Norse goddess who catches things in her net), **Njord**, **Portunus**, **Glaucus**.

Proteus was the cleverest and was rejected on a practical ground worth generalising:
**Labcenter Proteus** is well-known circuit-simulation and PCB software, which Nic will
encounter constantly as an ECE student. *Check name collisions inside your own field, not just
inside your industry.* A name that is distinctive on GitHub can still be the second result in
the domain you actually work in.

Plain-language names beat mythological ones for products: people misspell mythological names,
mispronounce them, and miss the reference. If the reference is wanted, the industry pattern is
to ship the plain name and keep the clever one as an internal codename.

**What it costs.** Nothing. The rename happened while the project was four markdown files.
Had it happened after a published release, it would have meant a new package id, a broken
upgrade path for every installed user, and a dead store listing.

**Further reading.**
- GPL v3 **§7(e)** — additional terms declining to grant trademark rights
- Search: "trademark vs copyright in open source", "nominative fair use", "why projects ask
  forks to rename", "Firefox / Iceweasel Debian trademark dispute" (the canonical case of
  exactly this situation), "product naming collision search"

---

# O-02 · Re-skin Mahesh's home screen instead of reverting to Seal's

**Date:** 2026-08-24 · **Overrules:** Claire's recommendation to restore the original
`DownloadPageV2` as Trawl's home screen

**What happened.** Surveying the fork turned up a cheap escape hatch: Seal Plus replaced the
original home screen with a 2,828-line `NewHomePage`, but **left `DownloadPageV2` in the tree,
still compiling, and simply never called it** (`AppEntry.kt` routes `Route.HOME` to
`NewHomePage`; `DownloadPageV2` has zero call sites). Reinstating Seal's original home is
therefore a ~5-line edit, and Claire recommended exactly that: take the free revert, drop 2,828
lines of someone else's design opinion, and build from the widely-liked Material You original.

Nic rejected it: *"no we are not orphaning them, we're just changing the UI."*

**Verdict: correct, and Claire was optimising for the wrong thing.**

The revert is cheaper *to Claire*, not better *for Nic*. The 2,828 lines are not merely a home
screen — they are the only entry point to the four More Tools features (batch URL import,
thumbnail, comment and video-info downloaders, ~6,000 lines). Reverting orphans all of it, and
the "cheap" path then quietly acquires the cost of rebuilding navigation to reach them again.
An argument that is only cheap because it discards working features is not cheap; it is a
smaller project wearing the same name.

The deeper error: the revert answers *"what is the least work that removes what I dislike?"*
when the actual goal is *"an interface that is mine."* Seal's Material You home is not Nic's
design either — it is a third party's, merely an older and better-liked one. Trading Mahesh's
opinion for JunkFood02's opinion is lateral motion dressed as progress. **When the goal is
authorship, inheriting a different stranger's design is not a shortcut to it.**

**What it costs — and this is real.** Re-skinning means Trawl now owns ~2,800 lines of
inherited UI it did not write and must keep working. Every future `git merge sealplus/main`
that touches `NewHomePage.kt` is a conflict Trawl has to resolve by hand, where the revert
would have made those conflicts irrelevant. D-03's scope boundary now matters more, not less:
the engine stays untouched precisely because the UI surface just got much larger.

**Further reading.**
- Search: "Chesterton's fence", "dead code vs unreachable code", "the cheapest change is not
  the cheapest outcome", "fork maintenance burden upstream merge conflicts", "design ownership
  vs design reuse"

---

# D-05 · Warm-brown "Ember", and glassmorphism rationed to three surfaces

**Date:** 2026-08-24 · **Status:** proposed, awaiting mock sign-off

**The call.** Trawl's identity is a **warm, low-blue, lamp-lit dark theme** ("Ember", amber
`#E0925A` on `#100B08`), with **real** backdrop blur applied to exactly three surfaces — the
app bar, the URL field and tools strip, and the active-download card. Everything else is a
solid surface. A cold-blue alternative ("Snow") is mocked alongside it for comparison.

**Why.** Nic asked for "all that -phism shit" and simultaneously ticked *strip
gradient/glassmorphism as default*. Both readings are satisfiable because they refer to
different implementations: Mahesh's gradient theme goes, Trawl's own glass arrives.

Three reasons for restraint over uniform glass:

1. **Uniform blur is the trend being escaped.** Blur-on-everything dates a UI to ~2020, and it
   is the specific thing Seal Plus already does. Copying the failure mode in a different hue
   is not a redesign.
2. **Blur costs frames.** Android's `RenderEffect` blur is API 31+ and genuinely expensive; a
   blurred *scrolling list* is the classic way to drop frames on mid-range hardware. Glass
   therefore goes on static chrome, never on list items.
3. **Warm dark is differentiated.** Effectively every app ships a blue-tinted dark mode. A
   low-blue, warm ground is rare, suits the stated "cozy" brief, and is easier on the eyes at
   night — which is when a download app actually gets used.

**A finding that decided the tone.** Seal Plus's `GlassCard` is documented as *"Glassmorphism
Card with backdrop blur"* and accepts a `blurRadius: Dp = 16.dp` parameter that **appears
nowhere in its body**. There is no blur in the app at all — it is a 5%-white fill with a
border. Trawl's glass is real, which is itself a visible differentiator.

**What flagship products do.** Apple's materials (iOS/macOS "vibrancy") and Windows 11's Mica
and Acrylic both restrict blur to *chrome* — sidebars, title bars, command surfaces — and
explicitly warn against applying it to scrolling content, for the same performance reason.
Material 3 does not ship glass at all; it uses tonal elevation. The pattern is consistent:
glass marks the layer that floats, and floats rarely.

**What it costs.** A hand-built palette leaves Material You dynamic colour behind, so Trawl
will no longer follow the system wallpaper — a real feature of original Seal that some people
like. Mitigation: keep dynamic colour selectable, with Ember as the default.

**Further reading.**
- Search: "Apple Human Interface Guidelines materials vibrancy", "Windows 11 Mica vs Acrylic
  layering", "Material 3 tonal elevation vs translucency", "Android RenderEffect blur
  performance", "glassmorphism accessibility contrast", "dark mode blue light warm palette",
  "design tokens colour ramp construction"

---

# D-06 · Appearance is a settings surface, not a hard-coded look — and glass ships OFF

**Date:** 2026-08-24 · **Status:** proposed, mock-approved pending

**The call.** Trawl's look is driven by four user preferences, not by a compiled-in theme:
**Theme** (7 palettes), **Glass surfaces** (Off / Subtle / Full, default **Off**), **Ambient
motion** (Off / Subtle / Full), and **Download effects** (on/off). A ~2.4 s intro sequence
runs at launch and is itself a preference.

**Why glass defaults to off.** This partly reverses the round-1 position, which put glass on
three surfaces unconditionally. Nic asked for glassmorphism *and* for solid to be the base
look; those are only contradictory if glass is a look rather than a setting. Making it a
setting satisfies both and is better engineering anyway:

- Android's `RenderEffect` blur is **API 31+**, and `minSdk` here is **24**. A hard-coded
  glass design would need a fallback path for every device below Android 12 regardless — so
  the solid rendering has to exist and be good. Shipping it as the default means the fallback
  is the *tested* path, not the neglected one.
- Blur is the most expensive thing on the page. Default-off means the app is fast for
  everyone and pretty for whoever opts in on hardware that can afford it.

**Why the effects and motion are rationed by construction.** "Vibey but non-distracting" is
only achievable as a set of numeric limits, so they are written down: nothing cycles faster
than 34 s, nothing exceeds 8% opacity, ambient layers stay behind content at z-index 0–1, and
motion is decorative-only — never the sole carrier of state. The download effects are the one
place motion is allowed to be quick, because there it *means* something: the sweep along the
progress bar encodes "still moving", which is the exact question a user opens the app to ask.

**Why the intro is 2.4 s, not the portfolio's 4.7 s.** The choreography is deliberately the
same — mask-rise, sheen, a measured FLIP travel, staggered entrance — because a shared motion
vocabulary across Nic's work is worth having. The duration is not. A portfolio is visited once
and wants to make an impression; a downloader is opened dozens of times a week, and a 4.7 s
gate in front of "paste a link" converts charm into an obstacle by roughly the third launch.

**What flagship products do.** Material 3 exposes exactly this shape — dynamic colour, theme,
and contrast as user settings rather than a fixed skin. Android 12+ ships a system-level
"Remove animations" toggle, and both iOS and Android treat reduced-motion as an accessibility
guarantee, which is why every animation here lives inside
`@media (prefers-reduced-motion: no-preference)` and the *assembled* UI is the default state —
a dropped stylesheet or a reduced-motion user gets a finished screen, never a blank one.
Splash-screen guidance from Google's own Core Splashscreen library caps the branded window at
roughly one second for the same reason the intro got cut.

**What it costs.** Seven themes times three glass levels is a real testing matrix, and every
new surface must be built from tokens rather than literal colours or it will break in four
palettes at once. It also means a preference store, a theme provider, and migration handling
for stored values — genuine plumbing that a hard-coded look would not need.

**Further reading.**
- Search: "Material 3 dynamic color and theming", "Android RenderEffect blur API 31",
  "androidx.core.splashscreen guidance", "prefers-reduced-motion accessibility",
  "design tokens vs hard-coded styles", "WCAG 2.2 animation from interactions",
  "jank and frame budget 16ms Android", "progressive enhancement in UI effects"

---

# D-07 · The mark is a net; the mascot is a fish, and never a marine mammal

**Date:** 2026-08-24 · **Status:** proposed, mock-approved pending

**The call.** Trawl's launcher icon is a **geometric trawl net**, mouth open, hauling upward —
strokes only, no face, shipped in square, round and monochrome variants. The **mascot is a
small round fish**, used in empty states and the tail of the intro, and never in the launcher
icon.

**Why the mascot is not an animal of the obvious kind.** Nic asked for "a cute animal thingy
mascot" and immediately questioned it himself — *"idk if this is appropriate though since our
app is named after a trawl not an animal."* The instinct was right, for a sharper reason than
he gave. **D-04 spent the project's naming decision specifically to carry nothing of "Seal"**,
because the one thing upstream asked of forks was not to use that name. A seal, otter, walrus
or any marine-mammal mascot walks that straight back: it re-creates the association the rename
was bought to avoid, and it does so in front of exactly the audience that recognises the
lineage. Honouring a request in the wordmark and undoing it in the artwork is not honouring it.

A fish resolves it cleanly. A trawl catches fish, so the mascot is **the catch, not the
catcher** — it explains the product in one glyph, it is unambiguously cute, and it shares
nothing with the animal Trawl replaced.

**Why the mascot stays out of the launcher icon.** A face is the first thing to turn to mush at
48 px, and to a smear at the 16 px favicon/notification size. Character mascots work as
*illustration* — onboarding, empty states, error screens — while app icons need a silhouette
that survives being shrunk, tinted and masked into a circle by the launcher. Splitting the two
roles is why Duolingo can put Duo everywhere and still ship a flat green owl silhouette as the
icon, and why Android's own adaptive-icon guidance asks for a simple shape inside a safe zone
rather than detail.

**What flagship products do.** Android 8+ requires **adaptive icons** (separate foreground and
background layers, arbitrarily masked by the launcher), and Android 13+ adds an optional
**monochrome** layer that the system tints to the user's wallpaper — hence the three variants.
GitHub's Octocat, Duolingo's Duo, and Firefox's fox all follow the same split: expressive
character in the product, reduced mark in the icon.

**What it costs.** Two pieces of artwork to maintain instead of one, and a mascot invites
scope — illustrated states, animations, stickers — that a personal fork does not need. The
guard is that the mascot has exactly two sanctioned homes (empty states, intro tail) and any
third use is a decision, not a drift.

**Further reading.**
- Android developer docs: **adaptive icons**, **themed app icons** (monochrome layer),
  icon design safe zones
- Search: "brand mark vs mascot", "logo reduction test small sizes", "silhouette test logo
  design", "Material Design product icon guidelines", "trade dress and fork branding"

---

# D-08 · The floating download bubble is an enhancement, never the only way to see progress

**Date:** 2026-08-24 · **Status:** proposed, mock-approved pending

**The call.** A draggable floating bubble shows every running download as a concentric ring —
accent while running, **green when all complete, red with a pulsing glow on error**. Tap
expands a task panel with per-task progress, pause and retry. Drag reveals an X target at the
bottom centre; dropping on it dismisses. On by default, and switchable off both in Settings
**and from inside the bubble's own panel**. Three downloads run at once; the rest queue and
promote automatically.

**Why the "off switch inside the thing itself" matters.** An overlay that floats above every
other app is, by construction, the most intrusive surface the app owns. If the only way to
remove it is to find it in a settings tree, the first reaction of an irritated user is to
uninstall. Nic asked for the in-bubble control unprompted, and it is the single most important
detail in the feature.

**Why it cannot be the primary progress UI.** The bubble requires
**`SYSTEM_ALERT_WINDOW`** ("Display over other apps") — a *special* permission on Android, not
a normal runtime one. It cannot be requested with a standard permission dialog; it has to send
the user to a system settings screen, Play Store policy treats it as sensitive, and some OEM
ROMs deny or silently kill it. A design where progress is only visible in the bubble is
therefore a design that breaks for a meaningful share of users. Trawl already has a foreground
service notification carrying the same information, so the bubble is strictly additive — and
that ordering is the durable part of this decision.

**Why rings rather than a number.** The question a user opens a download app to answer is "is
it still moving?" A ring answers it pre-attentively, without reading. Four concentric rings
cover the common case; beyond that the centre count carries it, because more than four
simultaneous arcs stops being parseable at 60 px.

**What flagship products do.** Facebook Messenger's chat heads established the pattern —
including the drag-to-a-bottom-centre-X dismissal, which is now the platform idiom users
already know, and is why it was copied here rather than invented. Android 11+ ships a
**sanctioned Bubbles API** built on notifications, which needs no overlay permission but
constrains appearance and is tied to conversation-style notifications; it is worth evaluating
as the compliant path before shipping a raw overlay.

**What it costs.** A foreground service plus an overlay window is real battery and lifecycle
surface, and overlay windows are a common source of ANRs and leaks if the view is not torn down
with the service. It also adds a permission request to onboarding, which is friction on first
run — mitigated by only asking the first time a download actually starts, never at launch.

**Further reading.**
- Android docs: `SYSTEM_ALERT_WINDOW` / `Settings.canDrawOverlays`, the **Bubbles API**
  (Android 11+), foreground services and `FOREGROUND_SERVICE_DATA_SYNC`, notification progress
- Search: "chat heads UX pattern", "Play Store policy display over other apps", "Android 14
  foreground service types", "pre-attentive processing visual encoding", "progressive
  disclosure in progress UI", "graceful degradation when a permission is denied"

---

# D-09 · One motion setting with two personalities, and the transition is a shared vocabulary

**Date:** 2026-08-25 · **Status:** proposed, mock-approved pending

**The call.** Trawl ships **one** appearance setting for motion character — **Simple** or
**Fancy** — and it governs *every* transition in the app, not just the drawer. Simple is the
ordinary Material slide-over with a scrim. Fancy shrinks the whole page into a rounded card,
pushes it off to the right, and reveals a full-bleed menu behind it; sheets rise with a slight
scale, screen changes use a shared-axis slide, and the quick-download dialog pops with a
spring.

**Why one setting rather than a switch per surface.** The obvious alternative is per-animation
toggles (fancy drawer, simple dialogs, and so on). That is worse. Motion is a *voice*: the
speed, easing and distance of a transition tell the user how the app carries itself, and mixing
registers reads as inconsistency rather than choice. It also multiplies the test matrix by a
factor per switch for no user benefit — nobody wants a bouncy drawer and a clinical dialog.
One knob, two coherent personalities.

**Why the fancy drawer is a flat scale, not a 3D rotation.** The reference prototype Nic
supplied does a **scale + translate only**. The instinct when building this pattern is to add
`rotateY` and a perspective — it looks more impressive in isolation. It matched the reference
worse, and it costs more: a rotated layer forces a 3D rendering context and larger composited
surfaces, on the one transition the user triggers most often. **When mimicking a reference,
copy what it does, not what the technique could do.**

**Why the pushed card must be tappable.** Fancy has no scrim, because the whole point is that
the page itself becomes the backdrop. That removes the standard "tap outside to dismiss"
affordance, so the card takes over that job, with an explicit X in the menu column as the
discoverable fallback. A drawer with no obvious way out is a trap, however good it looks.

**What flagship products do.** The scale-and-push drawer is a long-standing pattern (Google's
own older Inbox app, countless Flutter/React Native menu packages). Material 3 formalises the
generic version as **shared-axis** and **container transform** transitions, and the platform
convention is that motion *style* is a system-level preference — Android's "Remove animations"
and `prefers-reduced-motion` on the web — which is why both styles here still sit under a
reduced-motion guard and the assembled UI remains the default state.

**What it costs.** The pushed card requires the page to live inside a single transformable
wrapper (`.stagewrap` in the mock), which means the overlay surfaces — drawer, bubble, dialogs
— must sit *outside* that wrapper or they get scaled with it. That is a structural constraint
on the Compose side too: the drawer cannot be a child of the content scaffold. Getting this
wrong is invisible until the first overlay appears mid-transition and shrinks with the page.

**Further reading.**
- Material 3 motion: **shared axis**, **container transform**, **fade through**; Android
  `prefers-reduced-motion` / "Remove animations" accessibility setting
- Search: "scale and push navigation drawer pattern", "Compose Modifier.graphicsLayer scale
  translation", "why 3D transforms cost more to composite", "motion as brand voice",
  "dismiss affordance without a scrim"

---

# D-10 · The drawer is a window switcher, and attribution moves to About without weakening

**Date:** 2026-08-25 · **Status:** proposed, mock-approved pending

**The call.** The navigation drawer stops behaving like a menu and starts behaving like a
**window switcher**: choosing a destination swaps the shrunken card's contents *while it is
still pushed back*, then zooms it in. The hamburger becomes a stacked-cards glyph, the close X
is gone (tapping the card is the way back), the gear leaves the top bar behind an opt-in, and
the upstream credit block moves out of the drawer onto a proper **About** page.

**Why the content swaps before the zoom, not after.** The naive implementation closes the
drawer and then renders the new screen. That reads as "menu dismissed, page changed" — two
events. Swapping first means the user watches the *same card* become a different window, which
is one event and is what the switcher metaphor promises. The cost is that the close animation
and the screen change are now coupled: the drawer must be held open for a beat after the state
change, so the transition owns a short window in which the app is in a mixed state. Guarded
with a `switching` latch so a second tap cannot interleave.

**Why removing the X is right.** It duplicated an affordance that already existed and, in the
reference layout, it physically sat on top of the pushed card. Two ways to do one thing where
one of them occludes content is worse than one way. This only holds because the card itself is
tappable — see D-09; drop that and the X has to come back.

**Why the credit moved but did not shrink.** `ATTRIBUTION.md` makes crediting JunkFood02 and
MaheshTechnicals a **project requirement**, not merely GPL compliance, so "the drawer looks
cramped" is not on its own a licence to reduce it. Moving it to About is an *upgrade*: in the
drawer it was four lines of small text competing with navigation; on About it gets a card per
upstream, each saying what that project actually contributed, plus the "a fork, not an official
build" line and the GPL-3 statement. **The test for relocating attribution is whether it ends
up more prominent and more informative, not less.** It did.

Related: the version line dropped "fork of Seal Plus" in favour of `v0.1.0 · niccc2007`. That is
safe only *because* the fork status is now stated plainly on About; had it been removed from
both, that would have been a real problem.

**Why the Seal Plus theme still says "Trawl".** The replica screen previously rendered the
"Seal+" wordmark. As a picture of upstream that was accurate; as a *theme the user can select*
it was the app calling itself Seal, which is the one thing `JunkFood02/Seal` asks forks not to
do (D-04). The title is now "Trawl" with a faint "Seal + theme" subtitle crediting where the
look came from — which is both compliant and more honest about what the user is looking at.

**What flagship products do.** Android's own Recents screen, and every OS task switcher, uses
scale-back-and-swap to say "these are windows". Material 3's *container transform* is the
formal name for morphing one surface into another rather than crossfading two. On attribution,
long-lived copyleft forks (LibreOffice, MariaDB, Jenkins) all keep upstream credit on a
dedicated About/Credits surface rather than in navigation chrome, for exactly the room reason.

**What it costs.** About is now a required screen, not an optional nicety — it carries the
licence obligations, so it can never be cut for space. And the switcher's held-open beat means
any future screen added to the drawer must render correctly at 0.76 scale, since the user sees
it small before they see it full size.

**Further reading.**
- Material 3 **container transform**; Android Recents / task-switcher interaction model
- GPL-3 §5(a) and the FSF GPL FAQ on preserving notices; the **REUSE specification**
- Search: "task switcher metaphor mobile navigation", "affordance duplication UI", "about
  screen open source attribution conventions", "trademark vs code in forks"

---

# D-11 · Navigation state is two variables, not one — and shortcuts hide where they lead

**Date:** 2026-08-25 · **Status:** proposed, mock-approved pending

**The call.** Split navigation state into **which screen is showing** and **whether the switcher
is open**. Add a *Keep switcher open* mode where picking a window makes it active and previews it
in the shrunken card, and tapping the card enters it. App-bar shortcuts hide themselves on the
screen they point at.

**Why the split was forced, not cosmetic.** The drawer had been modelled as a *screen*
(`screen === 'drawer'`). That is fine while a drawer is a modal menu, and impossible the moment
it becomes a switcher: previewing Settings inside the card requires being on Settings **and** in
the switcher at the same time, which a single enum cannot express. This is the recurring shape of
the mistake — **encoding two independent facts in one variable works right up until they need to
vary independently**, and then no amount of special-casing rescues it. The fix also removed a bug
nobody had reported: closing the switcher returned you to Home rather than to the screen you were
on, because "not in the drawer" had to resolve to *something*.

**Why select-then-enter is a mode rather than the default.** Two taps to change screen is strictly
slower than one, so it cannot be the default for an app whose main job is "paste, download". But it
is the honest expression of a task switcher, and it makes the previewed card meaningful rather than
decorative. Shipping it as an option keeps the fast path fast and lets the metaphor be complete for
whoever wants it.

**Why shortcuts vanish where they lead.** A control that cannot do anything is worse than an absent
one: it costs bar space, invites a tap that produces no feedback, and teaches the user that some
buttons are dead. Hiding beats disabling here because the destination stays reachable from the
switcher, so nothing becomes unreachable.

**What flagship products do.** Android Recents and macOS App Exposé both use select-then-enter with
a highlighted active window. Material 3's navigation guidance says the current destination should be
*indicated* rather than *offered* — bottom-nav marks the active item, while a redundant toolbar
shortcut is conventionally omitted.

**What it costs.** Two variables mean two things to keep in sync, and every future navigation entry
point must set both. The preview mode also means any screen reachable from the switcher must render
legibly at 0.76 scale before the user commits to it.

**Further reading.**
- Search: "orthogonal state modelling", "boolean blindness / enum conflation", "state machines for
  UI navigation", "Android Recents interaction model", "Material 3 navigation drawer current
  destination", "hide vs disable controls usability"

---

# D-12 · De-branding is a correctness job, not a find-and-replace

**Date.** 2026-08-25 · **Step.** 3 of the v0.1.0 plan

**The instruction.** "all must be come Trawl even the trawl is downloading etc.. make sure no seal
plus or seal is left. unless its in the settings or about page etc."

**What that sounded like.** Rename a product string in about a dozen places.

**What it actually was.** Two of the "Seal" occurrences were not branding at all — they were
defects that a purely cosmetic sweep would have shipped:

1. **`UpdateUtil` pointed the auto-updater at `MaheshTechnicals/Sealplus`.** Trawl would have
   checked upstream's releases, told its user an update was available, downloaded Seal Plus's APK
   and offered to install it. A different application, arriving through Trawl's own update prompt.
   This is the single worst bug that was sitting in the tree, and nothing about it looks like a bug
   in a grep — it reads as a constant with the wrong word in it.
2. **`ic_stat_seal` was upstream's logo as a full-colour PNG.** Wrong twice: it put Seal Plus's
   mark in the status bar for every notification Trawl posts, *and* Android alpha-masks and tints
   the notification small icon, so a photographic raster renders as a featureless white blob. The
   slot wants a monochrome silhouette. Upstream's icon was broken on its own terms; ours is a
   vector drawn for the size it is displayed at.

**The generalisation.** When a fork inherits a name, the name is load-bearing in places that are
not text: update endpoints, issue trackers, storage paths, keystore aliases, wake-lock tags, theme
style IDs, notification assets. **A rebrand sweep must ask of every hit "what does this actually
do?", because a fraction of them are wired to the internet or the filesystem and will keep serving
the original product.** The dangerous ones are exactly the ones that look most boring.

**Where the line was drawn.** He scoped it himself: *"unless its in the settings or about page"* —
so attribution stays. Concretely:

| Kept | Why |
|---|---|
| `AboutPage` upstream links, credit card | Attribution. It is a project requirement, not merely a licence one — it may move, it may not shrink. |
| The Weblate link on the Languages page | Trawl's translations *are* upstream's, contributed through that project; sending a translator there still improves the strings Trawl ships. |
| `namespace com.junkfood.seal`, `Route.SEALPLUS_EXTRAS`, `NOTIFICATION_GROUP_ID` | Never rendered. Renaming them buys nothing and breaks `git merge` from both upstreams forever. |

| Changed | Why it was not cosmetic |
|---|---|
| Update endpoint | Would have installed a different app over Trawl. |
| Issue tracker link | Files Trawl's bugs on a project that cannot fix them, and spams a maintainer who did not ask for it. |
| `Downloads/SealPlus`, `.SealPlus` private dir | User-visible folders on their own storage. |
| `Theme.SealPlus` style IDs | Not user-visible, but step 5 rebuilds theming and should not do so under the upstream product's name. |
| Keystore alias, wake-lock tag | Safe to change *only* because `applicationId` changed too, so every install is new — there is no existing key or lock to orphan. Worth stating, because on a plain rename this would be a data-loss bug. |

**What the method was.** Every replacement carried an **expected hit count**, and the script failed
loudly on a mismatch. A find-and-replace that silently matches zero times is indistinguishable from
one that worked, and that is how a fork ships upstream's name in the one string nobody re-grepped —
the exact failure mode already recorded in this folder's history for a wrong degree name.

**A side-effect worth recording.** The sweep silently rewrote CRLF to LF on 41 files. Git normalises
on commit, so *the commits were never wrong* — which is precisely why it would have gone unnoticed
until some later byte-for-byte check disagreed with itself. The whole Trawl contribution, 61 files,
had drifted to LF against a 373-file CRLF tree. Fixed, and pinned in `.gitattributes` so the
convention no longer depends on one machine's `core.autocrlf`.

**What it costs.** Fifteen inherited files now carry §5(a) notices and will conflict on any upstream
merge that touches the same lines. That is the price of the fork being honest about what it changed,
and D-02 already accepted it.

**Further reading.**
- Search: "GPL v3 section 5(a) modified files notice", "trademark vs copyright in open source forks",
  "Android notification small icon alpha mask", "adaptive icon monochrome layer",
  "git core.autocrlf gitattributes text=auto", "rebranding a fork checklist".

---

# D-13 · Seven palettes as literal hex, and a machine that checks them

**Date.** 2026-08-25 · **Step.** 5 of the v0.1.0 plan

## Copy the numbers; never re-derive them

Material's colour system wants a *seed* — give it one colour and it generates a scheme. That is
the wrong tool here. The mockup specifies 18 tokens per theme, chosen by eye against each other;
a generator handed `#E0925A` would produce a different, plausible, wrong set. So the palettes are
**literal hex transcribed from the mockup**, in a `Palette` holder whose fields are named after
the CSS custom properties they came from (`surfvar` → `surfVar`), so the two files can be diffed
line by line without a translation table.

**And then the transcription is verified mechanically.** `design/verify_tokens.py` parses the CSS
blocks out of the mockup and the `Palette` declarations out of the Kotlin and compares all
**112 tokens**. A human checking 7 themes × 16 values by eye will miss a digit; a wrong digit in
a dark palette is invisible until someone notices a surface is subtly off. This is the same
discipline as the hit-count assertions in D-12: *an unverified claim of fidelity is just a hope.*

The verifier immediately earned its keep by catching an edge it could not parse — `sealplus`
writes `--onprimary:#fff` in three-digit shorthand. The Kotlin was right; the checker was
incomplete. Fixed there, because **a verifier that cries wolf gets switched off**, and then it
protects nothing.

## Tokens Material has no slot for

Five of the design's tokens — the raised surface, three ambient wash stops, and the mote colour —
have no Material role. Two bad options were available: bend an unrelated role (`surfaceTint`
pressed into service as "mote colour"), or drop them. Both are worse than a second holder.
`TrawlTokens` sits beside `ColorScheme` in its own CompositionLocal. **The cost is that themable
colour now lives in two places and both must be updated together**; the benefit is that no future
reader has to discover that one Material role secretly means something else.

The ok/warn/bad trio is defined once on `.screen` in the mockup rather than per theme, so it is a
default on `TrawlTokens` rather than a field of `Palette` — matching where the contract puts it.

## Dark only, and saying so out loud

Every screen in the contract is dark. There is no light variant to transcribe, so **the palettes
apply in dark mode only**; light mode keeps the inherited Monet scheme. Forcing `#100B08` text
values onto a light background would not be "the theme in light mode", it would be unreadable.

Likewise **dynamic colour wins over the picker when it is on.** "Use my wallpaper" and "use Ember"
are contradictory instructions and the more specific one should win; dynamic colour is opt-in,
the theme is a default.

The important part is the third one: when either condition makes the picker inert, **the picker
says so** ("Trawl's palettes apply in dark mode") instead of letting someone tap seven swatches
and watch nothing happen. A control that silently does nothing teaches the user that the app is
broken, and they are not wrong.

## The inherited gradient becomes a theme, not a rival switch

Seal Plus's gradient look was a *separate boolean* (`GRADIENT_DARK_MODE`, defaulting **on**) that
overrode everything else. That default is what made the inherited look the app's identity. It is
now simply `TrawlTheme.SEAL_PLUS`, one of seven, and the standalone toggle is gone — **two
controls for one thing is one too many**, and they would have fought.

Worth recording: the mockup's `sealplus` token block matches `GradientDarkColors` *exactly*,
because the mockup was transcribed from that file. The two agreeing is the check passing, not a
coincidence.

Keeping it at all is deliberate, and it is the same call as O-02: the fork did not orphan the
extras it inherited, so it does not orphan the palette that came with them. Its display name is
**"Seal +"** — attribution for a look someone else designed. The app still titles itself Trawl
while wearing it.

## Hiding a theme must not strand whoever is wearing it

`switchShowSealTheme(false)` also moves anyone currently on the legacy palette back to the
default. Without that, the picker would render a list that does not contain the active selection
— **a picker that cannot show what is currently chosen is lying about what the app is doing**, and
the user has no way back except guessing which switch did it.

**What it costs.** Seven palettes are seven things to update whenever the design moves, and the
verifier makes that cost *visible* rather than optional — change the mockup and the check fails
until the Kotlin follows. That is the intent.

**Further reading.**
- Search: "Material 3 ColorScheme roles", "design tokens vs generated palettes",
  "CompositionLocal for design tokens Compose", "dynamic color vs custom theme precedence",
  "why disabled controls should explain themselves".

---

# D-14 · The fonts have to be in the APK, and they have to keep their weight axis

**Date.** 2026-08-25 · **Step.** 6 of the v0.1.0 plan

## Downloadable fonts were never an option here

Android's normal answer for Google Fonts is the *downloadable font provider* — you declare the
font, the system fetches it. That provider **is part of Google Play Services**, and the target
device (HUAWEI NCO-LX1) **has no GMS**.

The failure mode is what makes this worth writing down: a downloadable font that cannot resolve
does not error. It **silently falls back to the system face**. Every string in the app would have
rendered in Roboto, the entire type half of the design would be gone, and nothing anywhere would
have said so. It would have looked like the fonts "didn't take" and cost an afternoon.

So the faces are bundled in `res/font`. That is the only mechanism that works on a device without
Play Services.

## Variable, not static — because the design uses half-steps

The mockup asks for **seven body weights: 450, 500, 550, 600, 650, 700, 800.** The obvious
approach — ship four static instances (Regular/Medium/SemiBold/Bold) — would snap each of those to
the nearest hundred and quietly flatten every one of the design's half-steps. Nobody would notice
it as a bug; the app would just look slightly less considered than the mockup, for no stated
reason.

Keeping the `wght` axis preserves them exactly. Compose needs both halves of this to work:

```kotlin
Font(resId, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
```

**Without `variationSettings` every entry renders at the file's default instance** and the family
looks like a single weight — a trap, because it compiles and runs and simply looks wrong. And
Compose only names the hundreds, so 450/550/650 need the numeric `FontWeight(450)` constructor;
declaring them explicitly is what lets a style asking for 550 match exactly rather than resolve to
the nearest declared neighbour.

## Pin the axes nobody varies

Shipped as-downloaded, the two files are 1,208 KB. Fraunces carries `SOFT`, `WONK` and `opsz`;
Inter carries `opsz`. **The design varies none of them.** Pinning those and keeping only `wght`:

| | before | after |
|---|---|---|
| Inter | 856 KB | **620 KB** |
| Fraunces | 352 KB | **127 KB** |

Carrying axes nothing sets is dead weight in every copy of the APK forever.

`opsz` needed an actual decision rather than a default. Fraunces' optical size axis is what keeps
a serif from looking spindly, and browsers pick it automatically from the font size via
`font-optical-sizing` — **Android does not.** Pinned at 32, matching the ~20–30sp range Fraunces
is used at. Left at its default of 9 it would have been drawn for caption-sized text and looked
thin everywhere it actually appears.

## API 24 gets the default instance, and that is the right failure

Variable axes need API 26; `minSdk` is 24. On 24–25 both faces render at their default instance —
a slightly-wrong weight, not a missing font, not a crash. Accepted knowingly: the design targets
modern devices, and there is no version of this where "correct on two dead API levels" outranks
"correct on everything since 2017".

## The licence has to travel

Both faces are SIL OFL 1.1. The OFL requires the licence text ship with the font, so `licenses/`
holds both verbatim and `ATTRIBUTION.md` records them. Two things worth being precise about,
because they are commonly got wrong:

- **OFL does not infect the application.** A bundled font is an aggregate work, not a derivative
  of the app. Trawl stays GPL-3.0; the fonts stay OFL. No conflict.
- **OFL §3 forbids distributing a *modified* copy under the Reserved Font Name.** Pinning axes is
  a modification. As app resources this is fine, but if these files are ever re-derived and shipped
  as standalone fonts they must be renamed. Recorded so nobody discovers it later.

## Reading the mockup's pixels

The frame is 393px with a 9px drawn bezel, so the screen is 375px. The bezel is an artifact of
drawing a phone inside a browser, not canvas. **1 mockup px = 1 dp**, against a ~393dp reference
handset. The test device is 423dp (1080px at an overridden 408dpi) — rescaling to it would bake
one specific phone into the type scale, which is the opposite of what a scale is for.

Radii got the same treatment, mapped by which component uses them rather than by inventing a
geometric ramp. Material has five shape slots and the design uses nine radii, so the leftovers
(11, 14, 20, 26, 32) are named constants in `TrawlShape`. A component needing 26dp should reference
`TrawlShape.UrlBar`, not write `RoundedCornerShape(26.dp)` and become a number nobody can trace.

**Further reading.**
- Search: "Android downloadable fonts requires Play Services", "Compose FontVariation.Settings
  variable font weight", "fontTools varLib instancer pin axis", "Fraunces optical size axis",
  "SIL OFL reserved font name modified copy", "OFL GPL compatibility aggregate work".

---

# D-15 · Compose has no backdrop blur, so the backdrop is built by hand

**Date.** 2026-08-25 · **Step.** 7 of the v0.1.0 plan

## The mismatch nobody warns you about

The mockup's glass is CSS `backdrop-filter: blur()` — it blurs **what is behind** an element.
Compose's `Modifier.blur()` blurs the composable's **own content**. They sound like the same
feature and are opposites: applied to an app bar, `Modifier.blur()` smears the bar's own title
while leaving the artwork scrolling underneath perfectly sharp.

Compose 1.11.2 ships no backdrop API — checked, not assumed: the only blur class in
`androidx.compose.ui` is `draw/BlurKt`. The usual answer is the third-party `haze` library, which
exists precisely because this gap does.

## Built from the primitives instead of taking the dependency

`GraphicsLayer` and `RenderEffect` *are* both present, and they are what haze is made of, so:

- `GlassBackdrop` wraps the content behind the chrome, records it into a `GraphicsLayer`, and
  hangs a `BlurEffect` on that layer.
- `Modifier.trawlGlass()` draws the already-blurred layer, translated by its own offset from the
  backdrop's origin so the blurred pixels line up with what is genuinely behind it, clipped to its
  own shape.

The blur samples **beyond** the clip, which is what makes the edges pull in their neighbours
rather than fading to nothing — a detail that separates real frosted glass from a grey rectangle.

The structural rule this creates is easy to get wrong: **chrome goes outside `GlassBackdrop`, in
the same Box.** Wrap the chrome too and the recording already contains the bar, so the bar blurs
a picture of itself. Documented at the call site because there is no way for the type system to
prevent it.

Rejected `haze` because one modifier is not worth a dependency in an app whose whole pitch is
that its source is inspectable — and a dependency here would have to be carried, updated and
licence-audited forever for a flourish that ships **off**.

## Every degradation lands in the right place

This is the part that mattered most, because a decorative effect must never be able to make the
app unusable:

| Situation | Result |
|---|---|
| Glass off (the default) | Opaque `surfaceContainer` + `outline` hairline — exactly the mockup's default state |
| API < 31 (no RenderEffect) | Exact tint and hairline, no blur |
| No `GlassBackdrop` in the tree | Exact tint and hairline, no blur |

It can never render as an invisible panel or a black rectangle. Callers do not branch on the
setting either — `trawlGlass` is correct at every level, so a screen cannot forget to handle
"off".

**Cost, stated plainly:** when glass is on, a full-screen layer is recorded every frame. That is
simply what backdrop blur costs; it is why it is opt-in, and why `GlassBackdrop` degenerates to a
plain `Box` — no layer, no recording — the moment the setting is off.

## A correctness bug worth naming

`layer.renderEffect = ...` was first written straight in the composable body. That is wrong even
though it works: **composition can run several times for one frame and can be abandoned**, so
mutating a shared object from the body is a race that happens to look fine. Moved into a
`SideEffect`, which is exactly the primitive for "apply this to a non-Compose object once the
composition has actually committed".

## Honest status

`trawlGlass` **has not rendered anywhere yet.** Its first real consumers are the app bar and URL
field (step 9), the switcher (step 11) and the bubble (step 14). The system and the setting are
done and compile; a runtime bug in the sampling maths would surface at those steps, not this one.
Recorded rather than glossed, because "step 7 complete" should not be read as "seen working".

**Further reading.**
- Search: "Compose backdrop blur GraphicsLayer record", "haze library Compose why",
  "RenderEffect createBlurEffect API 31", "Compose SideEffect vs composition side effects",
  "backdrop-filter performance cost".

---

# D-16 · One clock for all ambient motion, and a gradient where a blur would break

**Date.** 2026-08-25 · **Step.** 8 of the v0.1.0 plan

## The constraint is the feature

The design states its own limit: **nothing moves faster than 34 seconds, nothing sits above ~8%
opacity.** That is not timidity, it is the whole thesis — ambient motion you can consciously
notice on a utility app is a distraction, and a downloader is something people open to do one
thing quickly. This is meant to be felt only when the eye rests.

Every number here is transcribed rather than invented: drift periods 34/42/50s at Full and
54/66s at Subtle, grain at .05/.085, 14 motes rising over 16-36s each.

## One clock, not fourteen

The obvious Compose approach is `rememberInfiniteTransition` per animated thing. With 14 motes,
3 blobs and 2 washes that is 19 animation clocks, each driving recomposition, permanently, behind
every screen.

Instead there is **a single elapsed-seconds value** updated in a `withFrameNanos` loop and read
**inside draw lambdas**. A state read in a draw scope invalidates only the draw phase, so all the
ambient motion in the app costs **zero recompositions**. At `MotionLevel.OFF` the loop never
starts, so the cost is not merely small, it is nothing.

**The clock is monotonic and never wraps.** This mattered: a wrapping clock (which is what
`rememberInfiniteTransition` gives you) makes every derived phase jump at the wrap point, and
with 14 motes on 14 independent periods that is a visible stutter across the whole field every
cycle. Accumulating elapsed time avoids the problem rather than hiding it.

## Where a faithful translation would have been the wrong translation

The blobs are `filter: blur(46px)` on solid ellipses. The literal port is `Modifier.blur(46.dp)`,
and it would have been a bad bug: **`Modifier.blur()` is a no-op below API 31.** `minSdk` is 24.
On those devices the "subtle ambient wash" would render as **three hard-edged discs of saturated
colour sitting behind the interface** — not a degraded effect, a broken screen.

So the blobs are drawn as radial gradients that hold their colour to ~45% and then fall away.
Visually equivalent at this radius, correct on every API level, and cheaper than a full-screen
blur pass every frame. **A translation that is literal but wrong is not fidelity.**

The grain got the same treatment for a different reason: it is an inline SVG `feTurbulence`, and
Android has no such filter. A 140x140 noise tile is generated once and repeated — **seeded**, so
it is identical on every launch. An unseeded tile would differ run to run, which is exactly the
kind of "why does it look slightly different today" that nobody can ever reproduce.

## Two small refusals

- **The sweep is suppressed on a failed download** (`.bar:not(.err)` in the mockup, honoured
  here). A cheerful shimmer travelling across a failed task is the interface being upbeat about
  bad news, and it undermines the error state sitting right next to it.
- **Blobs still draw at `MotionLevel.OFF`.** "Motion off" means *stop moving*, not "go flat
  black" — the wash is the theme's ambient colour, not an animation. Turning off motion should
  not silently also turn off the palette.

## A Kotlin trap worth remembering

`Easing` is a `fun interface`, so `SweepEasing(x)` does not compile — there is no `invoke`
operator, and the error surfaces as **"Unresolved reference"**, which points at the name rather
than the call. It is `SweepEasing.transform(x)`.

**Further reading.**
- Search: "Compose withFrameNanos animation loop", "state read in draw phase skips
  recomposition", "Modifier.blur API 31 minSdk", "feTurbulence equivalent Android",
  "Compose fun interface Easing transform".

---

# D-17 · Reskin the home screen; do not rewrite what it knows

**Date.** 2026-08-25 · **Step.** 9 of the v0.1.0 plan

## The 2,800 lines are not all UI

`NewHomePage.kt` is inherited and large, and the tempting move was to replace it. Reading it
first was the right call, because a third of it is **state derivation carrying scar tissue**:

- de-duplicating a URL that has both a live task and a database row, during the Running →
  Completed transition where it briefly has both;
- pruning Completed tasks out of the process-scoped `DownloaderV2` map once the DB row is
  confirmed, because otherwise a later delete resurrects the task as a ghost card;
- an optimistic hidden-set so a card disappears on tap instead of waiting for the DB flow.

Each of those has a comment describing a bug someone already fixed. **A rewrite would have
thrown away the fixes and kept the bugs**, and they would have come back one at a time as "weird
duplicate card" reports with no obvious cause.

So: every line of state logic stays, and only the rendering is replaced. That is what "rework"
means here.

## What actually changed on screen

Order follows the contract: brand lockup → URL bar + fast tray → tool strip → downloading →
recent → end marker. The inherited order had the tool row *above* the URL field, which put four
secondary features in front of the one thing the app is for.

- **The tool strip gets labels.** It was icon-only, which turned four distinct capabilities
  (batch, thumbnail, info, comments) into four glyphs indistinguishable without tapping them.
- **Typed URLs render monospace.** Not decoration: a URL is a string people proofread, and a
  proportional face makes `l`/`1`/`I` and `0`/`O` ambiguous exactly where a typo costs a failed
  download.
- **The fast tray hides once you type.** Someone who has typed a link has already chosen the
  deliberate route; offering one-tap qualities underneath would be offering to discard it.
- **Three qualities, not five.** A fast path is scanned, not read. Everything else is one tap
  further behind *More*, which opens the full sheet.
- **An end-of-list marker.** So a short list *ends* rather than just stopping — the difference
  between "that is everything" and "did the rest fail to load?".

The download effects landed here too, with a refusal on each: **breathe only while Running** (a
paused or failed card sits still, so motion means "this is working"), and **sweep only on the
determinate bar** (an indeterminate bar is already in constant motion; a second moving highlight
on top reads as two unrelated things).

238 lines of now-unreachable composables were deleted rather than left to rot.

## Two Compose traps, both of which compiled fine until they did not

1. **`LazyListScope` is not a composable scope.** `item { }` bodies are composable, the builder
   lambda around them is not — so `LocalX.current` cannot be read at that level. The fix is to
   read every local *before* the `LazyColumn` and close over the values.
2. **An inserted top-level declaration silently stole an annotation.** Anchoring an insertion on
   `"@Composable
fun NewHomePage("` put a new `val` between `@OptIn(ExperimentalMaterial3Api)`
   and the function it applied to. The annotation then applied to the `val`, and the errors
   surfaced *hundreds of lines away* as "this material API is experimental". **When inserting
   before a declaration, anchor above its annotations, not between them.**

## The tooling lesson, third time now

An unescaped `'` in `that's the whole catch` broke resource compilation — and aapt2 reports it as
**"Invalid unicode escape sequence"**, which sends you hunting for a bad `\u` that does not
exist. That cost several minutes of looking in the wrong place.

Worse, **two attempts to fix it silently did nothing**: a `sed` with `\\\\'` and an inline
`python -c` both passed through shell quoting layers that ate the backslash, reported success,
and changed the file not at all. It only landed when written to a script file using `chr(92)`
explicitly and **reading the file back from disk to prove it**.

The rule this reinforces, already in this project's history: **for anything involving backslashes
or quotes, write a script file — never an inline heredoc — and verify by re-reading the artifact,
not by trusting the exit code.**

**Further reading.**
- Search: "Compose CompositionLocal LazyListScope not composable", "Kotlin annotation applies to
  wrong declaration", "aapt2 unescaped apostrophe string resource", "Android string resource
  escaping rules".
