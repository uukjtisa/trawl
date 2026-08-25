#!/usr/bin/env python3
"""Probe TikTok resolution the same way the app does.

Reference implementation of TikTokCdn.kt. Run it when a TikTok download fails, to tell whether
TikTok changed or Trawl did:

    python tools/probe_tiktok.py <aweme id>
    python tools/probe_tiktok.py https://www.tiktok.com/@user/video/<aweme id>

Pass --check-url to prove the session-binding claim: the resolved URL is fetched twice, once with
the page's cookies and once without. Standard library only.

See tools/README.md for the line-by-line mapping to the Kotlin.
"""
import http.cookiejar
import json
import re
import sys
import urllib.request

# Usernames and captions are routinely non-ASCII, and a Windows console defaults to a codepage
# that cannot encode them -- so printing a result would crash the probe on the platform most
# likely to be running it. Replace rather than raise: a mangled character is a cosmetic problem,
# a traceback in a diagnostic tool is not.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

# Load-bearing. The same request with a DESKTOP user agent returns a ~1.4 KB stub with no data.
MOBILE_UA = (
    "Mozilla/5.0 (Linux; Android 12; NCO-LX1) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
)

VIDEO_ID = re.compile(r"tiktok\.com/(?:@[\w.\-]+/)?(?:video|photo|v)/(\d+)", re.I)
REHYDRATION = re.compile(
    r'<script id="__UNIVERSAL_DATA_FOR_REHYDRATION__"[^>]*>(.*?)</script>', re.S
)


def video_id(url_or_id):
    if url_or_id.isdigit():
        return url_or_id
    m = VIDEO_ID.search(url_or_id)
    return m.group(1) if m else None


def resolve(vid):
    """Kotlin: TikTokCdn.fetch. Returns (media dict, cookie header) or (None, '')."""
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    page_url = "https://m.tiktok.com/v/%s.html" % vid
    req = urllib.request.Request(
        page_url, headers={"User-Agent": MOBILE_UA, "Accept": "text/html"}
    )
    try:
        html = opener.open(req, timeout=25).read().decode("utf-8", "replace")
    except Exception as exc:  # noqa: BLE001
        print("  page fetch failed: %s" % exc)
        return None, ""

    print("  page             %d bytes" % len(html))
    m = REHYDRATION.search(html)
    if not m:
        print("  no rehydration data -- the stub page, or the format changed")
        return None, ""

    scope = json.loads(m.group(1)).get("__DEFAULT_SCOPE__", {})
    # The mobile share page uses the "reflow" scope; the desktop page uses webapp.video-detail.
    detail = scope.get("webapp.reflow.video.detail") or scope.get("webapp.video-detail") or {}
    item = (detail.get("itemInfo") or {}).get("itemStruct")
    if not item:
        print("  no itemStruct -- private, removed or region-locked")
        return None, ""

    video = item.get("video") or {}
    # downloadAddr preferred: the source file rather than the streaming rendition.
    addr = video.get("downloadAddr") or video.get("playAddr")
    if not addr:
        print("  no playable address (photo post?)")
        return None, ""

    cookies = "; ".join("%s=%s" % (c.name, c.value) for c in jar)
    print("  cookies          %s" % ", ".join(sorted(c.name for c in jar)))
    author = item.get("author") or {}
    return (
        {
            "id": vid,
            "url": addr,
            "who": author.get("uniqueId"),
            "nick": author.get("nickname"),
            "width": video.get("width"),
            "height": video.get("height"),
            "duration": video.get("duration"),
        },
        cookies,
    )


def fetch_head(url, cookies):
    """A one-byte ranged GET. TikTok's edge does not answer HEAD usefully."""
    headers = {"User-Agent": MOBILE_UA, "Referer": "https://www.tiktok.com/", "Range": "bytes=0-0"}
    if cookies:
        headers["Cookie"] = cookies
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            total = (r.headers.get("Content-Range") or "").split("/")[-1]
            return r.status, r.headers.get("Content-Type"), total
    except Exception as exc:  # noqa: BLE001
        return getattr(exc, "code", "ERR"), None, None


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    vid = video_id(sys.argv[1])
    if not vid:
        print("not a tiktok video link")
        return 1

    print("aweme %s" % vid)
    media, cookies = resolve(vid)
    if not media:
        print("\nUNRESOLVED -- the app would hand this to yt-dlp.")
        return 1

    code, ctype, total = fetch_head(media["url"], cookies)
    print("\nRESOLVED  @%s (%s)  %sx%s  %ss" % (
        media["who"], media["nick"], media["width"], media["height"], media["duration"]))
    print("  with cookies     HTTP %s  %s  %s bytes" % (code, ctype, total))
    print("  %s" % media["url"][:100])

    if "--check-url" in sys.argv:
        # The claim under test: this URL is SESSION-BOUND, not merely time-limited. Without the
        # cookies the page handed out, it is a 403 for everyone -- which is why a link copied out
        # of a browser appears to "expire".
        bare, _, _ = fetch_head(media["url"], "")
        print("  without cookies  HTTP %s   <- expected 403; the URL is session-bound" % bare)
    return 0


if __name__ == "__main__":
    sys.exit(main())
