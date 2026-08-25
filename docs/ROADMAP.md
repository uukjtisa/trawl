# Roadmap: candidate platforms for direct resolution

**Nothing on this page is a commitment.** It is a list of platforms that *might* be resolvable
without yt-dlp's extractor, kept so the idea is not lost. A platform earns a resolver only after
someone probes it and the route survives contact with the live endpoints. Several will not.

Trawl already downloads from every one of these through yt-dlp. A resolver is only worth writing
where yt-dlp's own extractor is unreliable on Android, which is the same reason X and TikTok got
one. A platform yt-dlp handles well is not a gap.

---

## Table of Contents

- [Status key](#status-key)
- [Shipped](#shipped)
- [Candidates](#candidates)
- [What would have to be true](#what-would-have-to-be-true)
- [Known dead ends](#known-dead-ends)

---

## Status key

| Mark | Meaning |
|---|---|
| **Shipped** | Resolver written, measured, in the app |
| **Probed** | Endpoints tested by hand; findings recorded below |
| **Unprobed** | Plausible on paper. Nobody has run a single request yet |

Anything marked *Unprobed* is a guess, including the variant columns. Treat those as questions to
answer, not features to expect.

---

## Shipped

| Platform | Route | Variants it actually yields |
|---|---|---|
| **X / Twitter** | `cdn.syndication.twimg.com/tweet-result`, then FixTweet for restricted posts | The full MP4 ladder X publishes, with real bitrates and sizes. No watermark |
| **TikTok** | `m.tiktok.com/v/<id>.html`, rehydration JSON, session cookies replayed | Two renders: `playAddr` (clean) and `downloadAddr` (watermarked). Measured on a real post: 1.76 MB at 576 kbps clean, 3.08 MB at 936 kbps watermarked, both 576x768 |

**On TikTok "HD" specifically.** Third-party wrapper APIs advertise `play`, `hdplay` and `wmplay`
tiers, and `bitrateInfo` does exist in TikTok's own data model. Neither is reachable from the route
Trawl uses. Measured 2026-08-25:

- the mobile reflow page carries `playAddr` and `downloadAddr` and **no `bitrateInfo` array**;
- the desktop page returns a 1462-byte stub with no rehydration block at all;
- `www.tiktok.com/api/item/detail/` answers `200` with an **empty body**.

The ladder lives behind request signing (`X-Gorgon` / `X-Argus`), which was rejected earlier on
cost. So Trawl offers the two renders that exist and does not pretend there is a third.

---

## Candidates

| Platform | Why it might work | Variants to look for | Status |
|---|---|---|---|
| **Douyin** | Same company as TikTok, so probably the same page-parse shape. Mobile UA likely load-bearing again | Clean vs watermarked, as TikTok | Unprobed |
| **Instagram** Reels/posts | The `/embed/` surface is the one anonymous route Instagram keeps on purpose, for third-party sites | Source MP4, carousels return several items | **Partly probed, see below** |
| **Reddit** | `v.redd.it` is plain DASH, no signing | Video and audio arrive as **separate streams** and must be merged | Unprobed. The merge makes this more work than it looks |
| **Streamable** | Small service, historically a plain JSON endpoint | One or two progressive MP4s | Unprobed. Cheapest thing on this list to test |
| **Facebook** public video/Reels | HD and SD progressive links appear in public page markup | HD / SD | **Partly probed, see below** |
| **Dailymotion, Vimeo** | Documented-ish player configs | Progressive plus adaptive | Unprobed. yt-dlp already handles both well, so the payoff is small |
| **Twitch clips** | Clips are progressive MP4, unlike VODs | One or two renditions | Unprobed |
| **Snapchat Spotlight** | Public items expose a direct MP4 | Single rendition | Unprobed |
| **Bilibili, Weibo, Kuaishou, Xiaohongshu** | Active open-source parsers exist to learn from | Varies; some tiers are account-gated | Unprobed. Region and login walls likely |

YouTube is deliberately absent. It serves adaptive DASH/HLS rather than one progressive MP4, so a
resolver would have to reimplement stream selection and merging, which is precisely the part yt-dlp
is best at. It stays on yt-dlp.

---

### Instagram and Facebook: what the first probes found (2026-08-25)

Requested, so measured. Neither is written yet and neither is ruled out; what follows is how far
an anonymous request gets.

**Neither shows a login wall.** Instagram's post, reel and embed URLs all answer `200` to a
signed-out request, and Facebook's `/watch/` page does too. That was the first thing worth knowing
and it is the good news.

**Instagram no longer server-renders anything.** `instagram.com/<user>/` returns a ~617 KB
JavaScript shell for every account tried, byte-for-byte the same size within a few hundred bytes,
containing zero post data -- no shortcodes, no `video_url`, nothing. The feed arrives later over an
authenticated GraphQL call. So the profile route is dead for an anonymous client, and the only
surface still worth testing is `/p/<shortcode>/embed/`, which exists for third-party embedding and
is therefore *meant* to answer without a session.

That last test needs a real shortcode, which cannot be discovered from the shell. It is the next
step, not a conclusion.

**Facebook's mobile hosts reject the probe outright.** `mbasic.facebook.com` and
`m.facebook.com/reel/` both return `400` with a 3.6 KB body. The desktop `/watch/` page answers
`200` with ~498 KB. Whether that page carries `browser_native_hd_url` for a real public video is
untested, again for want of a known-public id.

**Neither resolver will be written before a real public URL of each is put through a probe.** That
rule is why three TikTok routes were discarded on evidence instead of after a build, and it is not
being relaxed because these two were asked for by name.

## What would have to be true

A candidate is worth writing when all five hold. Check them in this order, cheapest first:

1. **A logged-out request returns the media URL.** No account, no signing. If it needs `X-Gorgon`
   or an equivalent, stop.
2. **yt-dlp's own extractor is actually unreliable on Android.** Otherwise there is nothing to fix.
3. **The URL is fetchable** with headers Trawl can replay. Session-bound is fine, TikTok already
   works that way. Unfetchable is not.
4. **Failure is detectable**, so the resolver can return nothing and let yt-dlp take over.
5. **A probe can be written** that reproduces the route in Python. Every shipped resolver has one
   in [`tools/`](../tools/), and that is what tells you later whether the site changed or Trawl did.

Write the probe first. Both shipped resolvers were measured before a line of Kotlin existed, and
three TikTok routes were discarded at that stage rather than after being built.

---

## Known dead ends

Recorded so nobody spends an afternoon rediscovering them.

| Route | What happened |
|---|---|
| TikTok desktop page | 1462-byte stub, no rehydration block |
| TikTok mobile API `api22-normal-c-*` | `200` with an empty body, or `429`. Needs request signing |
| TikTok `api/item/detail/` | `200`, empty body |
| TikTok oEmbed | Works, but metadata only. No video URL |
| X syndication on restricted posts | Returns `TweetTombstone` with no media. Server-side gate, nothing to parse. FixTweet covers it |
| Instagram profile HTML, signed out | ~617 KB JS shell, identical across accounts, zero post data |
| `mbasic.facebook.com`, `m.facebook.com/reel/` | `400` with a 3.6 KB body |
