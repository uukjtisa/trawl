# Trawl v0.1.0 — build progress

Live task list for the 1:1 implementation of `v0.1.0-baseline-mockup-ui.html`.
Updated at the end of **every** step. Steps run in strict order; step N does not start until
N−1 is complete and compiles.

Legend: `[ ]` not started · `[~]` in progress · `[x]` done

> **Resuming cold (e.g. after a compaction)?** Everything needed is on disk: the approved plan
> at `.claude/plans/now-hte-plan-in-wondrous-cake.md`, the UI contract at
> `design/v0.1.0-implementation-spec.html`, the baseline at
> `design/v0.1.0-baseline-mockup-ui.html`, rationale in `DECISIONS.md`, and this tracker.
> Work happens on branch **`trawl`** — `main` stays a clean upstream mirror, so
> `git diff main..trawl` is exactly Trawl's own contribution. Pick up at the first step below
> that is not `[x]`, in order, and do not skip. Build gate:
> `./gradlew assembleGenericDebug -PnoSplits -Dorg.gradle.java.home="D:\Program Files\Android-Studio\jbr"`
> Device: HUAWEI NCO-LX1, Android 12 / API 31, arm64-v8a. One install only, at step 15.


---

## Steps

- [x] **1 — Progress tracker + logging tag**
- [x] **2 — Identity** (app name, applicationId, launcher icon)
- [x] **3 — De-brand sweep** (no user-visible "Seal"/"Seal Plus")
- [x] **4 — Strip donation surfaces**
- [x] **5 — Theme foundation** (7 palettes + extended tokens)
- [x] **6 — Typography, shape, bundled fonts**
- [x] **7 — Glass system** (Off/Subtle/Full, default Off)
- [x] **8 — Ambient motion + download effects**
- [~] **9 — Home screen**
- [ ] **10 — Links history**
- [ ] **11 — Motion system + window switcher**
- [ ] **12 — About page**
- [ ] **13 — Intro sequence**
- [ ] **14 — Quick download + floating bubble**
- [ ] **15 — Ship v0.1.0** (build, install, verify against the spec)

---

## Acceptance checklist (70 items)

Ticked only when the behaviour is real in the app, not merely coded.

### Identity — step 2/3
- [x] 1 App name → Trawl
- [x] 2 `applicationId` → `dev.niccc2007.trawl`
- [x] 3 Launcher icon = trawl-net mark (square / round / monochrome)
- [ ] 4 Mascot = the fish
- [ ] 5 Version line `v0.1.0 · niccc2007`
- [~] 6 No user-visible "Seal"/"Seal Plus" outside attribution contexts
      — zero Seal text ships in any of the 62 locales (verified in the APK, not the
      source). Outstanding only inside About, which step 12 rebuilds: upstream release /
      README / issue URLs (attribution, staying) and `sealplus.in` shown as "Website"
      (misleading, must go).

### Themes — step 5
- [x] 7 Seven themes: Ember (default), Hearth, Grove, Plum, Snow, Slate, Seal Plus
- [x] 8 "Show Seal Plus theme" toggle removes it from the picker
- [ ] 9 Seal Plus theme still titles the app Trawl + faint "Seal + theme"
- [x] 10 Warm low-blue dark is the house direction

### Glass — step 7
- [x] 11 Glass setting Off / Subtle / Full, default **Off**
- [ ] 12 Glass on chrome only — never list rows
      — the system exists but has NOT rendered anywhere yet; first consumers are the app
      bar / URL field (step 9), switcher (step 11) and bubble (step 14). A runtime bug in
      the sampling maths surfaces there, not here.

### Ambient + FX — step 8
- [x] 13 Ambient motion Off / Subtle / Full (blobs, grain, motes)
- [x] 14 Nothing faster than 34 s, nothing above 8% opacity
- [~] 15 Download effects: progress sweep, breathing card, haul wash
      — all three built and settable; they attach to the download card in step 9.

### Intro — step 13
- [ ] 16 Curtain → mask rise → sheen → FLIP travel → stagger
- [ ] 17 ~2.4 s, reduced-motion safe, cannot strand
- [ ] 18 Mark + fish paired; fish swims up and flaps
- [ ] 19 Accent rule draws under "Trawl"
- [ ] 20 Tagline "hauls it up"
- [ ] 21 Intro on/off setting

### Home — step 9
- [ ] 22 Brand header, default on, switchable off
- [ ] 23 URL bar with FAST pill
- [ ] 24 Fast tray: one-tap qualities + More…
- [ ] 25 Quick-tools strip, one surface, four labelled cells
- [ ] 26 Downloading section, multi-task with queue
- [ ] 27 Recent list
- [ ] 28 End-of-list mascot
- [ ] 29 FAB

### Links history — step 10
- [ ] 30 Screen: thumbnail, title, URL, status pill
- [ ] 31 Filters All / Saved / Missing / Failed
- [ ] 32 One-tap re-download at remembered quality
- [ ] 33 Day grouping
- [ ] 34 Empty state with mascot
- [ ] 35 "All links ›" entry from Home

### Switcher — step 11
- [ ] 36 Drawer behaves as a window switcher
- [ ] 37 Transition style Simple / Fancy
- [ ] 38 Fancy = flat scale + translate, no 3D rotation
- [ ] 39 Menu items stagger from the left
- [ ] 40 Tap the pushed-back card to return — no X
- [ ] 41 Swap content while small, then zoom in
- [ ] 42 Stacked-cards switcher glyph
- [ ] 43 Gear removed from bar, opt-in to restore
- [ ] 44 Restored gear replays the switcher move
- [ ] 45 History button behaves identically, own toggle
- [ ] 46 Shortcuts hide on the screen they point at
- [ ] 47 Keep switcher open: pick → preview → tap to enter
- [ ] 48 Menu highlights the active window

### About — step 12
- [ ] 49 Signature banner: niccc2007, sheen, watermark, rule, mascot
- [ ] 50 Portfolio link
- [ ] 51 GitHub link
- [ ] 52 Upstream credit card (required, may not shrink)
- [ ] 53 Licence row
- [ ] 54 Version row

### Quick download — step 14
- [ ] 55 Dialog matching the real share-intent flow
- [ ] 56 Reachable from the bubble
- [ ] 57 Source badge, URL chip, preview, quality chips, More…

### Floating bubble — step 14
- [ ] 58 Draggable overlay, default on
- [ ] 59 One conic ring per download, ≤4 then a count
- [ ] 60 Accent running · green done · red + pulse on error
- [ ] 61 Expandable panel: progress, pause, retry
- [ ] 62 Drag onto the bottom-centre X to dismiss
- [ ] 63 Off-switch in its own panel and in Settings
- [ ] 64 Multi-queue: 3 concurrent, rest queued
- [ ] 65 Permission gate degrading to the notification

### Removals — step 4
- [x] 66 Home donation dialog
- [x] 67 Crypto donation page
- [x] 68 Sponsor / Support Developer pages
- [x] 69 Gradient theme as default
- [ ] 70 "Built on" out of the drawer (→ About)
      — PAIRED WITH STEP 12, do not do it earlier. The real app's equivalent is
      "Powered by Mahesh Technicals" on the splash screen, which is currently the most
      explicit upstream credit anywhere in the UI. It may move to About; it may not be
      absent in between. Step 12 must add the credit card and remove the splash line in
      the same commit.

---

## Log

| Step | Result |
|---|---|
| 1 | done — `TrawlLog` (tag `Trawl`) + this tracker. Compiles. |
| 2 | done — appId `dev.niccc2007.trawl`, v0.1.0, net-mark icon (vector + monochrome + rasters). `Trawl-0.1.0-arm64-v8a.apk` builds. |
| 3 | done — 62 locales + 15 Kotlin files swept; auto-updater and notification icon
repointed off upstream; Seal artwork, splash logo and dead demo file deleted; `.gitattributes`
added. APK verified: no Seal text in any locale. |
| 4 | done — 7 pages/dialogs deleted (2,944 lines), 8 files edited, 227 string
elements dropped across 39 locales. SponsorBlock (a yt-dlp video feature, unrelated to
donations) verified intact. APK 94.5 MB → 87.6 MB. |
| 5 | done — 7 palettes as literal hex + TrawlTokens for the 5 tokens Material has no
slot for; themeId/showSealTheme prefs; swatch picker in Look and feel; gradient default
flipped off and folded in as one theme. `design/verify_tokens.py` proves all 112 tokens
match the mockup exactly. |
| 6 | done — Inter + Fraunces bundled as variable fonts with unused axes pinned
(1,208 KB → 747 KB), TrawlTypography + TrawlShapes wired into MaterialTheme, superseded
Type.kt/Shape.kt deleted, OFL licences added to licenses/ and ATTRIBUTION.md. |
| 7 | done — GlassBackdrop records the content behind chrome into a GraphicsLayer and
blurs it; Modifier.trawlGlass samples it aligned and clipped. Compose has no backdrop API,
so this is built from GraphicsLayer + RenderEffect rather than taking a dependency.
Degrades to the exact tint + hairline with glass off, below API 31, or with no backdrop.
Reusable TrawlSegmented control added (used again in steps 8 and 11). |
| 8 | done — AmbientBackground (3 drifting blobs, seeded grain tile, 14 motes) plus
HaulWash, Modifier.progressSweep and Modifier.breathe, all on ONE monotonic clock read in
draw lambdas so ambient motion costs zero recompositions. motionLevel + downloadFx prefs
and their settings rows. Blobs are radial gradients, not blur: Modifier.blur is a no-op
below API 31 and minSdk is 24. |
