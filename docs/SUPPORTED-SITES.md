# Supported sites

Trawl downloads from **everything yt-dlp supports** -- well over a thousand sites. That list is
yt-dlp's, not Trawl's, and it is maintained here:
<https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md>

This page covers the smaller thing Trawl adds on top: the sites it can resolve **without** yt-dlp's
extractor, and the ones measured and rejected.

---

## Table of Contents

- [Trawl's own resolvers](#trawls-own-resolvers)
- [Measured and rejected](#measured-and-rejected)
- [Everything else](#everything-else)
- [Adult sites](#adult-sites)

---

## Trawl's own resolvers

Each of these has been driven end to end on a real device against a real public URL. The header
quirks are load-bearing; they are not decoration.

| Site | Rungs it yields | What makes it work |
|---|---|---|
| **TikTok** | Two renders: clean and watermarked | The mobile page, replaying the session cookies its CDN requires |
| **X / Twitter** | The full MP4 ladder with real bitrates | X's syndication endpoint, then a public mirror for restricted posts |
| **Facebook** | HD and SD | A desktop user agent **and** `Accept: text/html`. A mobile UA gets a flat `400`, and `Accept: */*` returns the page with the media keys missing |
| **Newgrounds** | 360p / 720p / 1080p | `X-Requested-With: XMLHttpRequest` on `/portal/video/{id}`. Without it the same URL answers `403` behind an age gate; with it, `200` and every rung **including on age-restricted entries** |

Every one of them falls back to yt-dlp when it comes up empty, and every one has an off switch in
Settings.

## Measured and rejected

Recorded so the work is not repeated. Both were requested by name.

**Instagram.** Not viable signed out. `instagram.com/<user>/` returns the same ~617 KB JavaScript
shell for every account, carrying zero post data -- no shortcodes, no `video_url`. The `/embed/`
surface, which exists for third-party sites and should answer anonymously, returns that same shell
for a real reel. The feed arrives over an authenticated GraphQL call, so there is nothing for a
logged-out resolver to read. yt-dlp keeps Instagram.

**One large adult tube site.** Rejected on quality, not on principle. Its `mediaDefinitions` offers
240, 480, 720 and 1080 and every one is `"format":"hls"`. The URLs *look* progressive --
`.../1080P_4000K_<id>.mp4/master.m3u8` -- but that `.mp4` is a path segment, not the file, so a
naive scan counts thirty "progressive" links that are all playlists. The only genuine progressive
file is 240p. A direct resolver could therefore only ever return 240p where yt-dlp muxes the HLS
and returns 1080p, so registering it would be a downgrade dressed as a feature. yt-dlp keeps it,
and handles it well.

## Everything else

Anything not listed above goes to yt-dlp, which is most things: YouTube, Reddit, Twitch, Vimeo,
Dailymotion, Streamable, Bilibili, SoundCloud, and the rest of a very long list. A resolver is only
worth writing where yt-dlp's own extractor is unreliable on Android. A site yt-dlp handles well is
not a gap.

Candidates being considered are in [ROADMAP.md](ROADMAP.md), all marked unprobed until someone
runs a probe against them.

## Adult sites

Several are in yt-dlp's catalogue and therefore work in Trawl, through yt-dlp like any other site.
None has a Trawl resolver, and none is promoted anywhere in the app or the README.

Sites that mix general and adult content -- Newgrounds being the obvious one -- are listed by name
above, because that is what they are.

If you need the specific list, it is yt-dlp's, linked at the top of this page. Trawl neither
curates nor extends it.
