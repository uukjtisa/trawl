# What Trawl is for

Trawl is a personal fork of [Seal Plus](https://github.com/MaheshTechnicals/Sealplus), which is
itself a fork of [Seal](https://github.com/JunkFood02/Seal). Both are excellent, and Trawl exists
because of them rather than in spite of them — see [ATTRIBUTION.md](../ATTRIBUTION.md).

## The goal, stated plainly

**A media downloader that keeps working when a single extraction engine stops working.**

Every app in this family is a front-end for [yt-dlp](https://github.com/yt-dlp/yt-dlp), which is
a remarkable piece of engineering and does the genuinely hard part. But a front-end that can only
ever ask yt-dlp inherits every one of its bad days: when a site changes shape, or starts requiring
a browser fingerprint that a bundled Python runtime cannot produce, the download simply fails and
the user is told something unhelpful about "extractor errors".

Trawl's answer is to treat extraction as a question that can be asked more than one way. Where a
platform can be resolved by a second, independent route, Trawl tries that route too — and falls
back to yt-dlp rather than replacing it. yt-dlp remains the engine and the final fallback; it is
simply no longer the only thing that gets to decide whether a link is downloadable.

Concretely, that means:

- **more than one way in.** X/Twitter and TikTok each have a direct resolver that does not use
  yt-dlp's extractor at all. See [RESOLVERS.md](RESOLVERS.md).
- **fail softly, never silently.** Every resolver returns "no result" rather than an error, and
  "no result" means *carry on to the next route*. The worst case is the behaviour the app had
  before the resolver existed.
- **say what happened.** When something does fail, the app should name the reason — "this post is
  age-restricted", not "no video could be found".

## Scope and intended use

Trawl downloads media from public web pages so it can be saved for offline or personal use — the
same thing a browser's "save video" does, for platforms that do not offer one.

Please use it for material you have the right to keep: your own uploads, content you have
permission to save, media offered for download, or works whose licence allows it. Downloading or
redistributing someone else's work without permission may breach copyright law or a platform's
terms of service, depending on where you are and what you do with it. That responsibility rests
with the person using the tool, and no part of this project is an invitation to ignore it.

Trawl has no ads, no analytics, no telemetry and no accounts. It does not upload anything, and it
does not phone home.

## How Trawl differs from Seal and Seal Plus

### Reliability

| | Seal / Seal Plus | Trawl |
|---|---|---|
| Extraction | yt-dlp only | direct resolvers first, yt-dlp as fallback |
| X / Twitter | yt-dlp's extractor | X's own syndication endpoint, then a public mirror for restricted posts, then yt-dlp |
| TikTok | yt-dlp's extractor | TikTok's mobile share page, then yt-dlp |
| On failure | the extractor's message | the reason, where the app knows it |

The download itself is still yt-dlp's job in every case. The resolvers replace *extraction* — the
part that decides which URL holds the video — not the transfer, and not the progress reporting,
file naming, history, notifications, resume or ffmpeg post-processing that come with it.

### Behaviour

- **Nothing is ever refused outright.** Upstream's download archive would block a repeat download
  with an error indistinguishable from a real failure, using a hidden file the user could neither
  see nor reset. Duplicate handling belongs to the visible history instead.
- **A floating window** that survives leaving the app, with its own downloads panel, per-task
  controls, and a link field that pastes what you copied.
- **No donation, sponsor or crypto surfaces.** Removed in full, along with the upstream project's
  website and funding metadata. Trawl is not monetised and asks for nothing.

### Interface

A warm, low-blue dark theme with seven palettes, an optional glass treatment on chrome, a window
switcher in place of a plain drawer, an ambient background, and an intro sequence — all of which
can be turned off. The design was specified as an interactive mockup before any of it was built.

## What Trawl is not

- **Not an official build of Seal or Seal Plus,** and not affiliated with either project. It does
  not use their names, icons or branding, and it will not be published to any store where it could
  be mistaken for them.
- **Not a way around paywalls, DRM, or private accounts.** The resolvers read what a signed-out
  browser can already see. Where a platform gates content behind an account, Trawl says so.
- **Not a general-purpose scraper.** It resolves one link at a time, at the pace a person taps.

## Licence

GPL-3.0, inherited and unchanged. The source is here, modified files are marked, and the
attribution chain is intact. See [LICENSE](../LICENSE) and [ATTRIBUTION.md](../ATTRIBUTION.md).
