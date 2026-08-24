# Trawl — project ground truth

**Status: PLACEHOLDER. Nothing cloned, forked or built.** Planning documents only.
Read `HANDOFF.md` first, then `ATTRIBUTION.md`.

---

## What this is

A **personal fork** of the Seal Android video/audio downloader (a yt-dlp frontend), based
on the **Seal Plus** fork for its current engine, with the UI reworked to Nic's taste and
possibly independent features later.

It is a personalisation project. It is not a competing product, not a community rescue,
and not an attempt to out-maintain either upstream.

## Non-negotiables

| Rule | Detail |
|---|---|
| **GPL-3.0** | Inherited from both upstreams. Not a choice — the licence requires it of derivatives |
| **Credit JunkFood02 and MaheshTechnicals** | A stated project requirement, not just compliance. README, about screen, and `ATTRIBUTION.md` |
| **GPL §5(a)** | Every modified file carries a notice that it changed, and the date. The clause forks most often miss |
| **Change the package id** | `com.maheshtechnicals.sealplus` must not ship as-is. Proposed: `dev.niccc2007.trawl` |
| **Name** | **Trawl** — the fishing method, not the animal. Deliberately not Seal-derived, honouring upstream's request. Package `dev.niccc2007.trawl` |
| **Never impersonate** | Not Seal, not Seal Plus, not an official build of either. No upstream icons or branding |

## Base

**Fork from `MaheshTechnicals/Sealplus`, not from `JunkFood02/Seal`.**

The original's last substantive commit was **6 April 2025** — everything since is an
automated monthly README sponsor bot, so it looks maintained and is not. Seal Plus is
current as of 30 July 2026 with SDK 37, Kotlin 2.3.21 and yt-dlp 2025.12.08. Starting from
the original means redoing sixteen months of dependency catch-up by hand.

Keep **both** upstreams as git remotes — fixes may land in either.

## Scope

**UI first.** The engine, downloader, yt-dlp integration and app architecture stay as
inherited. What is being added is the interface.

Before any code: establish *what specifically* is wrong with Seal Plus's UI. Its headline
feature is a "gradient dark theme with glassmorphism". Whether the objection is the theme,
the layout, the navigation or the density decides whether this is a re-theme or a
re-architecture — very different amounts of work.

**A cheap third option exists:** Seal's original Material You UI was widely liked. Reverting
the UI while keeping Seal Plus's engine is legitimate, much cheaper than designing from
scratch, and may be exactly what "I don't like Mahesh's UI" actually means.

## Open

- [ ] What is specifically wrong with the Seal Plus UI
- [ ] Restore original Seal UI, or design new
- [ ] Which independent features, if any


## Related

`D:\Android-programs\Argus\` — the other Kotlin + Compose project from the same session.
Shares a stack, so Compose patterns learned in one transfer to the other.
