# Direct resolvers

How Trawl resolves X/Twitter and TikTok links without yt-dlp's extractors, why each route was
chosen, and what each one cannot do.

Every claim below was measured against the live endpoints before the code was written. The probes
that produced those measurements are in [`tools/`](../tools/) and can be re-run at any time — see
[Verifying this yourself](#verifying-this-yourself).

---

## The shape of it

Both resolvers do the same job and neither is a downloader:

```
link ──► resolver ──► direct media URL (+ headers, if the CDN needs them)
                          │
                          └──► yt-dlp fetches the bytes
```

**Extraction is ours. The transfer is still yt-dlp's**, and deliberately so. The transfer was never
the broken part, and it is what carries progress reporting, file naming, the history row,
notifications, resume and ffmpeg post-processing. Replacing all of that to save one HTTP GET would
trade a working system for a worse copy of it.

Both resolvers return `null` for every failure — switched off, not a supported link, no media,
endpoint down, endpoint changed. `null` means *hand the original URL to yt-dlp and carry on*, so
the worst case is the behaviour the app had before the resolver existed.

Both are **bridges, not features**. They depend on undocumented endpoints and will eventually rot.
Each has a Settings switch that turns it off.

---

## X / Twitter

`app/src/main/java/com/junkfood/seal/util/TwitterCdn.kt`

### Why

yt-dlp's Twitter extractor needs a guest token and increasingly an authenticated session. On a
phone with no cookies it fails often, and confusingly.

### Tier 1 — X's syndication endpoint

X's own embed widget resolves a post through a public endpoint, and so can we:

```
GET https://cdn.syndication.twimg.com/tweet-result?id=<status_id>&token=<token>&lang=en
```

The `token` is derived from the status id — `((id / 1e15) * π)` rendered in base 36 with zeros and
the decimal point stripped. It is computed rather than hard-coded, because a fixed token is
refused for ids it was not derived from. Kotlin has no fractional base-36 formatter, hence the
long-hand conversion in `base36()`.

A browser `User-Agent` is required; without one the endpoint returns 403.

The response carries the full variant ladder — every MP4 rendition X serves, with bitrates. Trawl
publishes all of them as selectable formats, so the picker shows real resolutions and real byte
counts instead of the single nameless entry yt-dlp's generic extractor would report for a direct
file. Sizes come from a `HEAD` per variant, on a short timeout: an absent size is honest, a guessed
one is not.

HLS (`application/x-mpegURL`) variants are ignored — a playlist needs a player, not a downloader.

### Tier 2 — a public mirror, for restricted posts

Age-restricted and sensitive posts come back from the syndication endpoint as a tombstone with no
media at all:

```json
{ "__typename": "TweetTombstone" }
```

That is X gating content server-side for signed-out clients. There is nothing in the response to
parse and no cleverness that changes it.

For those, Trawl falls back to the public [FixTweet](https://github.com/FixTweet/FixTweet)
resolver, which does answer for them and returns the same `video.twimg.com` renditions plus a
variant ladder.

**Order matters, and so does the privacy cost.** Tier 1 is a direct request to X and involves
nobody else, so it stays first. The mirror is consulted only after X itself has refused — and even
then only the *lookup* crosses it. The video bytes still come straight from `video.twimg.com`.

> On "how did an assistant inside X get the link when the app could not": it was signed in. That
> is an account, not a technique, and it is not reproducible from an app that is not. The mirror
> is the reproducible equivalent.

### What it cannot do

- Protected (private) accounts. Neither tier can see those.
- Photo-only posts — no video means no result, and the link goes to yt-dlp, which does handle
  images.

---

## TikTok

`app/src/main/java/com/junkfood/seal/util/TikTokCdn.kt`

### Why

yt-dlp's TikTok web path fails with *"Unable to extract universal data for rehydration"* because
TikTok fingerprints the TLS handshake and expects `curl_cffi` impersonation, which the Android
build of yt-dlp does not ship.

### The route, and the three that were rejected

Measured, not assumed:

| Route | Result |
|---|---|
| Desktop page, plain HTTP | `200` with ~1.4 KB — a stub. No data at all. |
| Mobile API (`api22-normal-c-…`) | `200` with an **empty body**, or `429`. Requires request signing (`X-Gorgon`). |
| oEmbed | Works, but metadata only. No video URL. |
| **`m.tiktok.com/v/<id>.html`** | **The full ~290 KB page, rehydration JSON included.** |

So the mobile share page is the way in, and **no WebView is needed** — which was the open question.
A hidden WebView would have worked (a real Chromium engine has a real browser's TLS fingerprint)
but it is a great deal of lifecycle machinery to avoid, and this avoids it.

The **mobile user agent is load-bearing**. The same request with a desktop UA gets the stub.

The video URL is read from `__UNIVERSAL_DATA_FOR_REHYDRATION__`, under
`webapp.reflow.video.detail` on the mobile page (the desktop page uses `webapp.video-detail`; both
are read, so a change of entry point does not break it).

### The part that is not obvious: the URL is session-bound

A `playAddr` fetched this way returns **403 to any client that did not also fetch the page** —
verified, including with a correct `Referer`. It is tied to the cookies TikTok sets on that page
request: `msToken`, `tt_chain_token`, `tt_csrf_token`, `ttwid`.

This is why a TikTok CDN link copied out of a browser appears to "expire in a few hours". It was
never portable in the first place.

Consequently the resolver returns **headers alongside the URL**, and the download replays them.
Resolution and download therefore have to happen close together, and the result is cached for two
minutes rather than X's ten — a stale entry here is not a slow download, it is a 403.

### What it cannot do

- Private, removed or region-locked posts — the page has no `itemStruct` and the resolver says so.
- Photo posts (slideshows). No video, no result.

---

## Shared behaviour

### Format ladders

Handed a bare MP4, yt-dlp's generic extractor reports **one** format with no codec fields and no
size. That is not merely ugly: the app classified such a format as audio-only and downloaded it
with `-x`, so videos arrived as audio files. Publishing the real variants — with stated codecs — is
what makes format selection mean anything.

The underlying bug was in shared code and is fixed for every extractor:

```kotlin
// before
fun isAudioOnly(): Boolean = vcodec == null || vcodec == "none"
```

`"none"` means a stream is **absent**; `null` means yt-dlp does not **know**. Conflating them made
whole MP4s classify as audio-only. Unknown now falls back to the container.

### Identity

Resolved downloads state their own id — the status id, the aweme id — rather than letting yt-dlp
derive one from a signed CDN URL. Without this the temp directory was named after the URL's query
string, and the output filename exceeded the filesystem's limit.

### Duplicates

Resolved downloads are never refused. Upstream's download archive keys on the extractor's id,
which for a direct file differs per quality rung, so one successful download refused every later
attempt at the same post. Duplicate handling belongs to the visible history instead.

---

## Verifying this yourself

The probes in [`tools/`](../tools/) are Python, and they exist because **every one of these routes
was tested before it was written in Kotlin.** They are the record of that, and they are runnable:
if a resolver breaks, running the matching probe tells you within seconds whether the endpoint
changed or the app did.

```bash
python tools/probe_twitter.py 1491475671058681863
python tools/probe_tiktok.py  <aweme id>
```

They are Python and the app is Kotlin, so
[`tools/README.md`](../tools/README.md) maps every step of each probe to the function that
implements it, line for line. Where the two must agree exactly — the base-36 token derivation, the
JSON paths, the variant filtering — the mapping table says so explicitly.
