# HANDOFF — Trawl

Written 2026-08-24. **Placeholder only. Nothing has been cloned, forked or built.**

---

## 1. Goal

A personal fork of the **Seal** Android video/audio downloader (a yt-dlp frontend), taken
from the **Seal Plus** fork for its up-to-date engine, with **a UI reworked to Nic's own
taste** and possibly independent features later.

This is explicitly a *personalisation* project, not a competing product and not a rescue
mission for the community. The engine work is already done by others; the value being
added is the interface.

**Crediting JunkFood02 and MaheshTechnicals is a stated requirement of this project**, not
just a licence obligation. See `ATTRIBUTION.md`.

---

## 2. State

**Nothing built.** This folder contains planning documents only:

- `HANDOFF.md` — this file
- `ATTRIBUTION.md` — the credit chain and GPL-3 obligations
- `CLAUDE.md` — project ground truth
- `DECISIONS.md` — decision log (Rule 18)

No clone, no git repo, no code, no gradle project.

---

## 3. Key upstream facts (verified 2026-08-24)

### Original — `JunkFood02/Seal`

| | |
|---|---|
| Stars | 28.4k |
| Licence | **GPL-3.0** |
| Stack | Kotlin, Jetpack Compose, Material You |
| **Last real code commit** | **6 April 2025** |
| Commits since | Only an automated monthly `docs(readme): update sponsor info` bot |
| Status | **Effectively abandoned**, but with no archive notice — it looks maintained at a glance |
| Naming restriction | Derivatives, "including but not limited to forks and unofficial builds", may not use the Seal name as a downloader application. **Honoured — see D-04** |

### Chosen base — `MaheshTechnicals/Sealplus`

| | |
|---|---|
| Licence | **GPL-3.0** |
| Last updated | 30 July 2026 |
| compileSdk / targetSdk | **37** |
| Kotlin | **2.3.21** |
| yt-dlp | **2025.12.08** |
| Package id | `com.maheshtechnicals.sealplus` |
| Attributes upstream? | **Yes, properly** — links JunkFood02 and the original contributor graph in the README |
| Separate NOTICE file | No — README only |

### Why it broke for Nic in the first place

Seal is a GUI over yt-dlp, which needs frequent updates because video sites keep changing
their extractors. Sixteen months without a dependency bump is enough to break YouTube
downloads outright.

---

## 4. Fragile spots and obligations

**GPL-3.0 is inherited and non-negotiable.** Trawl must be GPL-3.0 too. Concretely:

1. Keep `LICENSE` (GPL-3.0) intact and unmodified
2. Preserve all existing copyright notices — do not strip headers
3. **§5(a): every modified file must carry a prominent notice that it was changed, and the
   date.** This is the clause forks most often miss
4. If binaries are distributed, corresponding source must be available (a public GitHub
   repo satisfies this)

**The package id must change.** `com.maheshtechnicals.sealplus` cannot ship as-is — it
would collide on device and misattribute the app. Suggest `dev.niccc2007.trawl` to match
the Argus convention.

**The name is settled and compliant.** The `JunkFood02/Seal` README says, verbatim:

> "Except for the source code licensed under the GPLv3 license, all other parties are
> prohibited from using Seal's name as a downloader app, and the same is true for Seal's
> derivatives. Derivatives include but are not limited to forks and unofficial builds."
>
> — `JunkFood02/Seal` README, verified verbatim 2026-08-24

The name is carved out of the GPL grant deliberately: the licence conveys the code, not the
name. **Trawl uses none of it** — named for the fishing method, not the animal. No Seal
wordmark, icon or branding anywhere. See D-04.

**Do not fork from the original.** Basing on `JunkFood02/Seal` means redoing SDK 37,
Kotlin 2.3 and the yt-dlp bump by hand — sixteen months of catch-up already done and
published under a licence that permits taking it.

**Two upstreams to track, not one.** Fixes may land in either project. Keep both as git
remotes so changes can be pulled selectively.

---

## 5. Decisions already made

| | Decision |
|---|---|
| Base | **Fork from `MaheshTechnicals/Sealplus`**, not from `JunkFood02/Seal` — the Seal Plus tree is current, the original is 16 months stale |
| Licence | **GPL-3.0**, inherited and mandatory |
| Scope | UI rework first. Engine, downloader and yt-dlp integration stay as inherited |
| Attribution | Both JunkFood02 and MaheshTechnicals credited, in README **and** a dedicated `ATTRIBUTION.md`. This is a project requirement, not just compliance |
| Name / package | **Trawl**, `dev.niccc2007.trawl` — deliberately not Seal-derived (D-04) |
| Distribution | Personal use first. Any public release triggers the full GPL §5 obligations above |

---

## 6. Open questions

1. **What is actually wrong with Mahesh's UI?** "Gradient dark theme with glassmorphism"
   is the headline feature Seal Plus advertises. Knowing whether the objection is the
   theme, the layout, the navigation or the density decides whether this is a re-theme or
   a re-architecture — and they are very different amounts of work.
2. **Restore the original Seal UI, or design something new?** Seal's Material You interface
   was widely liked. Reverting the UI while keeping Seal Plus's engine is a legitimate and
   much cheaper third option than designing from scratch.
3. **Which independent features**, if any. None specified yet.


---

## 7. Immediate next actions

1. Answer §6.1 — look at Seal Plus's UI and name what's wrong with it specifically.
2. `git clone` Seal Plus into this folder, add both upstreams as remotes, rename the
   package, apply attribution, and make the first commit an unmodified baseline so the
   diff of Nic's own work is legible afterwards.

*No credentials or secrets appear in this document.*
