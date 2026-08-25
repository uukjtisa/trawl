# Resolver probes

Runnable reference implementations of Trawl's two direct resolvers, in Python.

```bash
python tools/probe_twitter.py 1491475671058681863
python tools/probe_tiktok.py  <aweme id> --check-url
```

Standard library only — no `pip install`, Python 3.8+.

## Why these exist, and why they are Python

The app is Kotlin. These are Python for one reason: **every route was measured against the live
endpoints before a line of Kotlin was written**, and this is the record of that. Four TikTok routes
were tried and three rejected on evidence; the X resolver's token derivation was verified against
the real endpoint before being trusted. Keeping the probes runnable means those measurements are
repeatable rather than a claim in a commit message.

They earn their place at maintenance time. These resolvers depend on undocumented endpoints and
**will** eventually break. When a download fails, the probe answers the only question that matters
first — *did the endpoint change, or did the app?* — in seconds, without a build, a device, or a
debugger.

The Python is deliberately plain: no classes, no dependencies, no cleverness. It is meant to be
read next to the Kotlin.

## Kotlin ↔ Python mapping

### `probe_twitter.py` ↔ `app/.../util/TwitterCdn.kt`

| Python | Kotlin | Must agree exactly? |
|---|---|---|
| `base36()` | `TwitterCdn.base36()` | **Yes** — digit for digit. A wrong token is rejected by the endpoint. |
| `token()` | `TwitterCdn.token()` | **Yes** — `((id / 1e15) * π)` in base 36, zeros and the point stripped. |
| `STATUS` | `TwitterCdn.STATUS` | Behaviourally. Both accept `/i/web/status/`, `/i/status/` and `@user/status/`. |
| `SIZE_IN_PATH` | `TwitterCdn.SIZE_IN_PATH` | Yes — the `WIDTHxHEIGHT` path segment is the only size source. |
| `tier1()` | `TwitterCdn.fetchViaSyndication()` | Same endpoint, same headers, same tombstone check. |
| `variants_from_syndication()` | `TwitterCdn.collectVariants()` | Yes — MP4 only; both read `mediaDetails[]` then fall back to `video.variants[]`. |
| `tier2()` | `TwitterCdn.fetchViaMirror()` | Same endpoint and JSON path. |
| — | `TwitterCdn.contentLength()` | Not probed. The probe prints bitrates; only the app needs byte counts for its picker. |
| — | `TwitterCdn.buildTitle()` | Not probed. Filename shaping is app-side. |

### `probe_tiktok.py` ↔ `app/.../util/TikTokCdn.kt`

| Python | Kotlin | Must agree exactly? |
|---|---|---|
| `MOBILE_UA` | `TikTokCdn.MOBILE_UA` | **Yes** — load-bearing. A desktop UA gets a ~1.4 KB stub with no data. |
| `VIDEO_ID` | `TikTokCdn.VIDEO_ID` | Behaviourally. |
| `REHYDRATION` | the regex inside `TikTokCdn.fetch()` | Yes — same `<script id="__UNIVERSAL_DATA_FOR_REHYDRATION__">` block. |
| `resolve()` | `TikTokCdn.fetch()` | Same scope order: `webapp.reflow.video.detail`, then `webapp.video-detail`. |
| the cookie jar | `RecordingCookieJar` | **Yes in effect** — the four cookies are what make the URL work at all. |
| `fetch_head()` | `TikTokCdn.contentLength()` | Yes — a one-byte ranged GET. TikTok's edge does not answer `HEAD` usefully. |
| `--check-url` | *(no equivalent)* | Probe-only. It exists to demonstrate the session binding. |
| — | `TikTokCdn.videoId()` short-link following | Not probed — pass the full link or the id. |

## What `--check-url` demonstrates

The single least obvious fact about TikTok, and the one that dictates the app's design:

```
with cookies     HTTP 206  video/mp4  5207883 bytes
without cookies  HTTP 403   <- the URL is session-bound
```

A TikTok CDN URL is **not merely time-limited — it is bound to the session that fetched the
page.** It returns 403 to any client that did not also fetch it, `Referer` or not. This is why a
link copied out of a browser appears to "expire after a few hours": it was never portable.

That is why `TikTokCdn` returns **headers alongside the URL** and the downloader replays them, and
why its cache is two minutes rather than the X resolver's ten. A stale entry there is not a slow
download; it is a 403.

## If a probe fails

| Symptom | Reading |
|---|---|
| TikTok page ≈1.4 KB, no rehydration data | The mobile UA stopped working, or the endpoint moved. |
| TikTok `no itemStruct` | Private, removed or region-locked. Not a bug. |
| X `TweetTombstone` on tier 1 | Age-restricted or sensitive. Expected — tier 2 should pick it up. |
| X tier 1 non-200 | Rate limited, or the token derivation broke. Compare `token()` against `base36()`. |
| Probe succeeds, app fails | The bug is in the app, not the endpoint. Start with `adb logcat -b all \| grep Trawl`. |
