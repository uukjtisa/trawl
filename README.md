<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="120" height="120" align="center" alt="Trawl">

# Trawl

**An Android media downloader that keeps working when a single extraction engine stops.**

[![Licence](https://img.shields.io/badge/Licence-GPL--3.0-orange?style=flat)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-7.0%2B-orange?style=flat&logo=android&logoColor=white)](#building)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-orange?style=flat&logo=kotlin&logoColor=white)](#building)

A personal fork of [Seal Plus](https://github.com/MaheshTechnicals/Seal-Plus), itself a fork of
[Seal](https://github.com/JunkFood02/Seal). Built by [**uukjtisa**](https://github.com/uukjtisa).

</div>

---

## Why this fork exists

Every app in this family is a front-end for [yt-dlp](https://github.com/yt-dlp/yt-dlp), which does
the genuinely hard part and does it well. But a front-end that can *only* ask yt-dlp inherits every
one of its bad days: when a site changes shape, or starts demanding a browser fingerprint a bundled
Python runtime cannot produce, the download fails and the user is told something unhelpful about
extractor errors.

**Trawl treats extraction as a question that can be asked more than one way.** Where a platform can
be resolved by a second, independent route, Trawl tries that route too — and falls back to yt-dlp
rather than replacing it.

yt-dlp remains the engine and the final fallback. It is simply no longer the only thing that gets
to decide whether a link is downloadable.

## Reliability

This is the feature. Everything else is an app around it.

### More than one way in

| Platform | Trawl tries | Then | Then |
|---|---|---|---|
| **X / Twitter** | X's own syndication endpoint — direct, full variant ladder | a public mirror, for age-restricted posts X refuses to serve signed-out clients | yt-dlp |
| **TikTok** | TikTok's mobile share page, with the session cookies that make its CDN URLs work | — | yt-dlp |
| Everything else | — | — | yt-dlp |

The **download** is still yt-dlp's job in every case. The resolvers replace *extraction* — the part
that decides which URL holds the video — not the transfer, and not the progress reporting, file
naming, history, notifications, resume or ffmpeg post-processing that come with it.

Both resolvers have a Settings switch, and both are off-switchable without losing the platform.

### Fail softly, never silently

Every resolver returns "no result" rather than an error, and "no result" means *carry on to the next
route*. The worst case is the behaviour the app had before the resolver existed. When something does
fail, the app names the reason where it knows it — "this post is age-restricted" rather than "no
video could be found".

### Nothing is refused outright

Upstream blocked repeat downloads using a hidden archive file the user could neither see nor reset,
with an error indistinguishable from a real failure. Duplicate handling belongs to the visible
history instead.

### Verified, not asserted

Every route in [`docs/RESOLVERS.md`](docs/RESOLVERS.md) was measured against the live endpoints
before it was written — including three TikTok routes that were tried and **rejected** on evidence.
The probes that produced those measurements ship in [`tools/`](tools/) and are runnable:

```bash
python tools/probe_twitter.py 1491475671058681863
python tools/probe_tiktok.py  <aweme id> --check-url
```

When a download breaks, they answer the only question that matters first — *did the endpoint change,
or did the app?* — in seconds, with no build and no device.

## The rest of it

- **A floating window** that outlives the app: per-download progress, pause and retry, a link field
  that pastes what you copied, and drag-to-dismiss.
- **Seven warm palettes**, optional glass on chrome, an ambient background and an intro — all of
  which can be turned off.
- **A window switcher** in place of a plain drawer.
- **A links history** with thumbnails, source badges, and one-tap re-download.
- **No ads, no analytics, no telemetry, no accounts.** Nothing is uploaded and nothing phones home.
  The donation, sponsor and crypto surfaces inherited from upstream were removed in full.

## Screenshots

<div align="center">
<!-- SCREENSHOTS -->
</div>

## Scope and intended use

Trawl saves media from public web pages for offline or personal use — the same thing a browser's
"save video" does, for platforms that do not offer one.

Please use it for material you have the right to keep: your own uploads, content you have permission
to save, media offered for download, or works whose licence allows it. Downloading or redistributing
someone else's work without permission may breach copyright law or a platform's terms of service
depending on where you are and what you do with it. That responsibility rests with the person using
the tool.

Trawl is **not** a way around paywalls, DRM or private accounts. The resolvers read what a
signed-out browser can already see; where a platform gates content behind an account, Trawl says so
rather than pretending otherwise.

## Building

```bash
git clone https://github.com/uukjtisa/trawl.git
cd trawl
./gradlew assembleGenericDebug
```

Android Studio opens it as-is — no local setup beyond a JDK 21 toolchain, which Gradle resolves.

| | |
|---|---|
| Min SDK | 24 (Android 7.0) |
| Target / compile SDK | 37 |
| Language | Kotlin, Jetpack Compose |
| Application id | `dev.niccc2007.trawl` |

## Credits

Trawl stands on other people's work, and the chain is recorded in full in
[**ATTRIBUTION.md**](ATTRIBUTION.md).

- **[yt-dlp](https://github.com/yt-dlp/yt-dlp)** — the download engine, and the genuinely hard part.
- **[youtubedl-android](https://github.com/yausername/youtubedl-android)** — Android bindings and
  the bundled Python runtime.
- **[Seal](https://github.com/JunkFood02/Seal)** by **JunkFood02** — the original app: architecture,
  Compose UI and the yt-dlp integration this is built on.
- **[Seal Plus](https://github.com/MaheshTechnicals/Seal-Plus)** by **MaheshTechnicals** — carried
  it forward when the original went quiet.
- **[FFmpeg](https://ffmpeg.org/)** — media post-processing.
- **[FixTweet](https://github.com/FixTweet/FixTweet)** — the public resolver used as tier 2 for
  restricted X posts.

Trawl is **not** an official build of Seal or Seal Plus and is not affiliated with either project.
It uses neither project's name, icon or branding in the app.

## Licence

[GPL-3.0](LICENSE), inherited and unchanged. The source is here, modified files carry a change
notice, and the attribution chain is intact.

## Documentation

| | |
|---|---|
| [docs/GOALS.md](docs/GOALS.md) | What Trawl is for, and how it differs from Seal and Seal Plus |
| [docs/RESOLVERS.md](docs/RESOLVERS.md) | How each resolver works, what it cannot do, and why |
| [tools/README.md](tools/README.md) | The probes, and how each maps to the Kotlin |
| [ATTRIBUTION.md](ATTRIBUTION.md) | The full upstream chain |
