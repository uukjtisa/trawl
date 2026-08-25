# Status

What works, what is half-built, and what is missing on purpose. Written at v0.1.0.

I keep this file because a feature list without one is a wish list. Everything below is checked
against the code, not remembered.

---

## Table of Contents

- [Works](#works)
- [Half-built](#half-built)
- [Not started](#not-started)
- [Deliberately absent](#deliberately-absent)
- [Known rough edges](#known-rough-edges)

---

## Works

| Area | State |
|---|---|
| X / Twitter resolver | Two tiers, then yt-dlp. Full variant ladder with real sizes |
| TikTok resolver | Mobile page plus session cookies, then yt-dlp. Cached two minutes |
| Fallback chain | A resolver that cannot answer returns nothing, and nothing means "try yt-dlp" |
| Everything yt-dlp supports | Unchanged from Seal Plus |
| `DIRECT` / `YT-DLP` badge | On every history row and Recent card |
| Media scanner | Downloads appear in the gallery and in music players |
| Delete | Asks whether to remove the file too, and remembers the answer |
| Floating window | Overlay, drag to dismiss, link field, clipboard auto-paste, session list, clear |
| Themes, glass, ambient motion, intro | Seven palettes, all effects switchable |
| Links history | Thumbnails, platform and tool labels, status, one-tap re-download |

## Half-built

**Audio.** Extracting audio through yt-dlp works, as it always did. What is not finished:

- Converted audio carries no artwork or tags. Doing it properly needs `--load-info-json`, which
  means writing the info JSON during the download and reading it back in post-processing.
- There is no "extract audio from something I already downloaded". Right now that means
  downloading the video a second time.
- One-tap MP3 and M4A from a *resolved* download works in the picker, but I have only tested it on
  a handful of posts. M4A is worth preferring where the source is already AAC: `-c copy` is
  instant and lossless, where MP3 re-encodes.

**Duplicate handling.** The old behaviour, a hidden archive file that refused the download with an
error you could not tell from a real failure, is gone. Downloads are never blocked now. The
replacement is not built: Links history should notice you already have a completed file still on
disk and ask whether you want it again. Until then, re-downloading a post silently makes a second
copy.

**Resolution trace.** When a download is resolving, the dialog shows a spinner. It should show
what is actually happening: which resolver ran, why it gave up, and what yt-dlp said afterwards.
The information exists in the log already, so this is a display job.

## Not started

- **Resolvers beyond X and TikTok.** Instagram, Facebook, Reddit and the rest go to yt-dlp. Each
  new resolver is a self-contained file behind its own switch, so this is additive work, but none
  of it is written.
- **Photo posts.** X photo-only posts and TikTok slideshows return no result and fall through to
  yt-dlp. Gathering every image from an album is not implemented in either resolver.
- **Cookie import for the resolvers.** The app can import cookies for yt-dlp, but neither resolver
  uses them, so protected accounts and private posts stay out of reach.
- **Tests.** The only test files are upstream's two generated stubs. The resolvers are covered by
  the Python probes in [`tools/`](../tools/), which is not the same thing as a test suite.
- **A release build.** No signed APK, no GitHub release, no F-Droid or IzzyOnDroid listing. Debug
  builds only.
- **Playlists and batch downloads** are inherited from Seal and untested against the resolvers.
- **aria2c** is inherited and off by default. It almost certainly cannot carry TikTok's
  session-bound headers, so leave it off for resolved downloads.

## Deliberately absent

- **Translations.** Upstream shipped forty-odd locales and fastlane metadata for F-Droid. Both are
  gone. I cannot keep translations honest for strings I rewrite every day, and stale translations
  are worse than English.
- **Donations, sponsors, crypto.** Deleted rather than hidden. Not my work to collect on.
- **Analytics, telemetry, crash reporting to a server.** None, and none planned.
- **Anything that defeats a paywall, DRM or a private account.** Out of scope. See the README.

## Known rough edges

- **Huawei and other aggressive ROMs** kill the foreground service, which takes the floating
  window with it. Trawl asks for a battery-optimisation exemption on first launch, but that only
  covers the standard Android setting. On Huawei the deep link into the ROM's own startup manager
  is refused with a `SecurityException`, so the auto-launch and secondary-launch toggles there have
  to be set by hand. The dialog says so; it cannot do it for you.
- **Installing the debug and release builds side by side breaks downloads for whichever one did not
  create the folder.** They are separate packages sharing `Download/Trawl`, and MediaStore records
  the owner per path, so the second app gets `Postprocessing: Error opening input files: Permission
  denied` on any file the first one already downloaded. It looks like a broken release build and is
  not one. Install one or the other, or give them different download directories.
- **Clipboard auto-paste only fires when the app or the floating panel has focus.** Since Android
  10 only the focused app may read the clipboard, so a true background watcher cannot exist. The
  panel reads it when you open it, which is the closest honest version.
- **`NewHomePage.kt` is 2,800 inherited lines** and I edited rather than replaced it. Every future
  merge from upstream on that file will be a manual conflict.
- **History rows written before the `DIRECT` badge existed** stored yt-dlp's extractor names, so
  they claimed yt-dlp did work the resolvers actually did. A one-off backfill relabels them at
  startup. If you saw a TikTok row marked `YT-DLP`, that was the bug, and it is fixed.
