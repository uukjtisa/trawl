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
- [x] **9 — Home screen**
- [x] **10 — Links history**
- [x] **11 — Motion system + window switcher**
- [x] **12 — About page**
- [x] **13 — Intro sequence**
- [x] **14 — Quick download + floating bubble**
- [~] **15 — Ship v0.1.0** (build, install, verify against the spec)

---

## Acceptance checklist (70 items)

Ticked only when the behaviour is real in the app, not merely coded.

### Identity — step 2/3
- [x] 1 App name → Trawl
- [x] 2 `applicationId` → `dev.niccc2007.trawl`
- [x] 3 Launcher icon = trawl-net mark (square / round / monochrome)
- [x] 4 Mascot = the fish
- [x] 5 Version line `v0.1.0 · niccc2007`
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
- [x] 15 Download effects: progress sweep, breathing card, haul wash
      — all three built and settable; they attach to the download card in step 9.

### Intro — step 13
- [~] 16 Curtain → mask rise → sheen → FLIP travel → stagger
      — all present except a MEASURED FLIP: the intro replaces the app, so the app bar it
      would travel toward does not exist to measure. Uses the mockup's own declared
      fallback offsets, which exist for exactly that path. See D-21.
- [x] 17 ~2.4 s, reduced-motion safe, cannot strand
- [x] 18 Mark + fish paired; fish swims up and flaps
- [x] 19 Accent rule draws under "Trawl"
- [x] 20 Tagline "hauls it up"
- [x] 21 Intro on/off setting

### Home — step 9
- [x] 22 Brand header, default on, switchable off
- [x] 23 URL bar with FAST pill
- [x] 24 Fast tray: one-tap qualities + More…
- [x] 25 Quick-tools strip, one surface, four labelled cells
- [x] 26 Downloading section, multi-task with queue
- [x] 27 Recent list
- [x] 28 End-of-list mascot
- [x] 29 FAB

### Links history — step 10
- [x] 30 Screen: thumbnail, title, URL, status pill
- [~] 31 Filters All / Saved / Missing / Failed
      — THREE ship: All / Saved / Missing. **Failed is deliberately absent**: the history
      table is written on success, so a failed download has no row and the filter could
      never match anything. Recording failures is a schema + engine change, not a UI one.
      See D-18. When it exists, the fourth filter is two lines.
- [x] 32 One-tap re-download at remembered quality
- [x] 33 Day grouping
- [x] 34 Empty state with mascot
- [x] 35 "All links ›" entry from Home

### Switcher — step 11
- [x] 36 Drawer behaves as a window switcher
- [x] 37 Transition style Simple / Fancy
- [x] 38 Fancy = flat scale + translate, no 3D rotation
- [x] 39 Menu items stagger from the left
- [x] 40 Tap the pushed-back card to return — no X
- [x] 41 Swap content while small, then zoom in
- [x] 42 Stacked-cards switcher glyph
- [x] 43 Gear removed from bar, opt-in to restore
- [x] 44 Restored gear replays the switcher move
- [x] 45 History button behaves identically, own toggle
- [~] 46 Shortcuts hide on the screen they point at
      — trivially satisfied today: both shortcuts live only on Home's bar and neither
      points at Home. Re-check when the bar is shared by more screens.
- [x] 47 Keep switcher open: pick → preview → tap to enter
- [x] 48 Menu highlights the active window

### About — step 12
- [x] 49 Signature banner: niccc2007, sheen, watermark, rule, mascot
- [x] 50 Portfolio link
- [x] 51 GitHub link
- [x] 52 Upstream credit card (required, may not shrink)
- [x] 53 Licence row
- [x] 54 Version row

### Quick download — step 14
- [~] 55 Dialog matching the real share-intent flow
      — the mockup's centred card was NOT built. The app's is a bottom sheet shared with
      the in-app flow (format selection, playlists, preferences); replacing it wholesale is
      a core-path change, not a restyle. Scoped to the mark + title + source badge. D-22.
- [ ] 56 Reachable from the bubble
- [~] 57 Source badge, URL chip, preview, quality chips, More…
      — source badge done; the rest belongs to the centred-card rebuild above.

### Floating bubble — step 14
- [x] 58 Draggable overlay, default on
- [x] 59 One conic ring per download, ≤4 then a count
- [x] 60 Accent running · green done · red + pulse on error
- [ ] 61 Expandable panel: progress, pause, retry
- [ ] 62 Drag onto the bottom-centre X to dismiss
- [~] 63 Off-switch in its own panel and in Settings
      — Settings switch done (and it reflects the PERMISSION, not just the preference).
      The in-panel switch belongs with the expandable panel, item 61.
- [ ] 64 Multi-queue: 3 concurrent, rest queued
- [x] 65 Permission gate degrading to the notification

### Removals — step 4
- [x] 66 Home donation dialog
- [x] 67 Crypto donation page
- [x] 68 Sponsor / Support Developer pages
- [x] 69 Gradient theme as default
- [x] 70 "Built on" out of the drawer (→ About)
      — done as a PAIR in step 12: the credit card was added to About and the splash's
      "Powered by Mahesh Technicals" removed in the SAME commit, so in-app attribution was
      never weaker than what the fork inherited. Verified present in the built APK.

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
| 9 | done — home reskinned to the contract's order and components (brand lockup, URL
bar + FAST pill + fast tray, labelled tool strip, section head, end-of-list fish, FAB).
ALL inherited state logic kept — the dedup/pruning comments each document a real bug.
Download effects attached. 238 lines of orphaned composables deleted. |
| 10 | done — LinksHistoryPage: search, filters, day grouping, status pill, one-tap
re-download, two distinct empty states. Status is computed once on Dispatchers.IO, not
per row. Failed filter omitted with reason (D-18). |
| 11 | done — TrawlSwitcher: Simple keeps ModalNavigationDrawer, Fancy is a custom
container (flat scale+translate, full-bleed menu, staggered rows, tap-card-to-return).
animStyle / pinSwitcher / quickGear / quickHistory prefs + rows. Switcher glyph replaces
the hamburger; drawer rows now highlight the active route; the gear replays the move. |
| 12 | done — About rebuilt: signature banner (Fraunces 37sp, brush sheen, drawn rule,
rotated watermark, fish), portfolio + GitHub rows, the upstream credit card with links,
licence, credits and version rows. Splash attribution removed IN THE SAME COMMIT.
OPEN: the mockup says github.com/niccc2007, the identity table says uukjtisa — the
updater (step 3) points at uukjtisa/Trawl and neither repo exists yet. Needs his call. |
| 13 | done — TrawlIntro replaces SplashScreen (deleted, 258 lines). Three independent
anti-stranding guarantees: never starts under reduced motion or with the setting off, tap
to skip, and a 4s failsafe that does not depend on the timeline being correct. |
| 14 | done — BubbleService: overlay window hosting a ComposeView, with the three view-
tree owners a Service does not provide (without them it crashes on first composition).
Conic rings, drag, permission gate that degrades to the notification, Settings switch that
reflects the permission. Dialog restyle scoped to the header + source badge (D-22).
NOT DONE: expandable task panel (61), drag-to-X dismiss (62), 3-concurrent queue (64),
centred-card dialog (55/57). These need the runtime pass in step 15 first. |
