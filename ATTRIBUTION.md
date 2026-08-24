# Attribution

Trawl stands on other people's work. This file records the chain honestly and completely.
It is a **project requirement**, not merely licence compliance.

---

## The chain

```
  yt-dlp                          the actual download engine
     |                            github.com/yt-dlp/yt-dlp  ·  Unlicense
     v
  youtubedl-android               Android bindings + bundled Python runtime
     |                            github.com/yausername/youtubedl-android
     v
  Seal            by JunkFood02   the original app: Kotlin, Compose, Material You
     |                            github.com/JunkFood02/Seal  ·  GPL-3.0
     |                            last substantive commit 2025-04-06
     v
  Seal Plus       by MaheshTechnicals
     |                            github.com/MaheshTechnicals/Sealplus  ·  GPL-3.0
     |                            SDK 37, Kotlin 2.3.21, yt-dlp 2025.12.08
     v
  Trawl          this fork       UI rework and personalisation only
```

---

## Credit

**[JunkFood02](https://github.com/JunkFood02)** wrote [Seal](https://github.com/JunkFood02/Seal)
— the original application. The architecture, the Compose UI foundation, the Material You
theming and the yt-dlp integration are all theirs. Roughly 28,000 people starred it because
it was good. Everything here descends from that work.

**[MaheshTechnicals](https://github.com/MaheshTechnicals)** maintains
[Seal Plus](https://github.com/MaheshTechnicals/Sealplus), the fork this one is based on.
When the original went quiet after April 2025, they carried it forward — Android SDK 37,
Kotlin 2.3.21, and a current yt-dlp. That dependency catch-up is the reason a working app
exists to fork at all, and it was substantial, unglamorous work.

**[yt-dlp](https://github.com/yt-dlp/yt-dlp) and its contributors** do the genuinely hard
part. Every app in this chain is a user interface over their extractors, which they keep
working against sites that actively try to break them.

**All [original Seal contributors](https://github.com/JunkFood02/Seal/graphs/contributors).**

**Trawl's own contribution is the interface and personal customisation.** The engine, the
downloader, the yt-dlp integration and the app's architecture are inherited. Nothing in
this fork should be read as a claim over any of it.

---

## Licence obligations

Trawl is **GPL-3.0**, inherited from both upstreams. That is not optional and not a
choice — it is what the licence requires of a derivative work.

Concrete duties:

- [ ] `LICENSE` (GPL-3.0) kept intact and unmodified
- [ ] All existing copyright headers preserved — never stripped from inherited files
- [ ] **GPL §5(a)** — every modified file carries a prominent notice that it was changed,
      and the date. This is the clause forks most often miss
- [ ] The whole derivative work released under GPL-3.0
- [ ] Corresponding source available wherever binaries are distributed
- [ ] Application id changed from `com.maheshtechnicals.sealplus` so the app neither
      collides on device nor misattributes itself
- [ ] Attribution present in the README, the app's about screen, and this file

## Naming

The original project's README states:

> "Except for the source code licensed under the GPLv3 license, all other parties are
> prohibited from using Seal's name as a downloader app, and the same is true for Seal's
> derivatives. Derivatives include but are not limited to forks and unofficial builds."
>
> — `JunkFood02/Seal` README, verified verbatim 2026-08-24

Note the construction: the name is deliberately carved **out of** the GPL grant. The licence
conveys the code; the name is handled separately. This is legitimate — GPL §7(e) expressly
permits declining to grant trademark rights alongside the code. The position is: take all
the code, do not call it Seal.

**Trawl uses none of that name.** The project is named for the fishing method — a net
dragged to haul things up — not for the animal, and it carries no Seal wordmark, icon or
branding. The request above is honoured in full.

For the record, an earlier working title ("NxSeal") did contain it and was changed for
exactly this reason, before any code or release existed. Seal Plus still contains the name;
that makes it common practice among forks, not permitted.

## What Trawl must never do

- Present itself as Seal, Seal Plus, or an official build of either
- Use either project's icon, branding or store listing imagery
- Remove or obscure upstream attribution
- Ship without source availability
