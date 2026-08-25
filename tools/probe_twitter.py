#!/usr/bin/env python3
"""Probe X/Twitter resolution the same way the app does.

This is the reference implementation of what TwitterCdn.kt performs on-device. It exists so that
when a download fails you can tell, in seconds, whether X changed or Trawl did:

    python tools/probe_twitter.py 1491475671058681863          # a NASA post, safe to re-run
    python tools/probe_twitter.py <any restricted post id>     # falls through to tier 2

Standard library only, so it runs anywhere with Python 3.8+.

See tools/README.md for the line-by-line mapping to the Kotlin.
"""
import json
import math
import re
import sys
import urllib.request

# Usernames and captions are routinely non-ASCII, and a Windows console defaults to a codepage
# that cannot encode them -- so printing a result would crash the probe on the platform most
# likely to be running it. Replace rather than raise: a mangled character is a cosmetic problem,
# a traceback in a diagnostic tool is not.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BROWSER_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)

DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"

STATUS = re.compile(
    r"https?://(?:[\w.-]+\.)?(?:twitter|x)\.com/\S*?status(?:es)?/(\d+)", re.I
)
SIZE_IN_PATH = re.compile(r"/(\d+)x(\d+)/")


def base36(value):
    """Kotlin: TwitterCdn.base36 -- must agree digit for digit."""
    v = abs(value)
    whole, frac = int(math.floor(v)), v - int(math.floor(v))
    out = ""
    if whole == 0:
        out = "0"
    else:
        tmp = ""
        while whole > 0:
            tmp += DIGITS[whole % 36]
            whole //= 36
        out = tmp[::-1]
    out += "."
    i = 0
    while frac > 0 and i < 20:
        frac *= 36
        d = int(math.floor(frac))
        out += DIGITS[d]
        frac -= d
        i += 1
    return out


def token(status_id):
    """Kotlin: TwitterCdn.token. Derived, never hard-coded -- a fixed token is refused."""
    n = (float(status_id) / 1e15) * math.pi
    return re.sub(r"(0+|\.)", "", base36(n)) or "a"


def status_id(url_or_id):
    if url_or_id.isdigit():
        return url_or_id
    m = STATUS.search(url_or_id)
    return m.group(1) if m else None


def get(url, accept="application/json", referer=None):
    headers = {"User-Agent": BROWSER_UA, "Accept": accept}
    if referer:
        headers["Referer"] = referer
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except Exception as exc:  # noqa: BLE001 -- a probe reports failures, it does not raise
        return getattr(exc, "code", "ERR"), str(exc)


def variants_from_syndication(root):
    """Kotlin: TwitterCdn.collectVariants. mp4 only -- HLS needs a player, not a downloader."""
    out = []
    for media in root.get("mediaDetails") or []:
        for v in ((media.get("video_info") or {}).get("variants") or []):
            if v.get("content_type") == "video/mp4" and v.get("url"):
                out.append((v["url"], v.get("bitrate") or 0))
    if not out:
        for v in ((root.get("video") or {}).get("variants") or []):
            if v.get("type") == "video/mp4" and v.get("src"):
                m = SIZE_IN_PATH.search(v["src"])
                px = int(m.group(1)) * int(m.group(2)) if m else 0
                out.append((v["src"], px))
    return out


def tier1(sid):
    """Direct to X. Involves nobody else, so it goes first."""
    url = "https://cdn.syndication.twimg.com/tweet-result?id=%s&token=%s&lang=en" % (
        sid, token(sid))
    code, body = get(url, referer="https://platform.twitter.com/")
    print("  tier 1  syndication      HTTP %s" % code)
    if code != 200:
        return None
    try:
        root = json.loads(body)
    except ValueError:
        print("          not JSON")
        return None

    kind = root.get("__typename")
    if kind and kind != "Tweet":
        # Age-restricted / sensitive / removed. X gates these server-side for signed-out
        # clients; there is nothing in the response to parse.
        print("          %s -- restricted, falling through to the mirror" % kind)
        return None

    variants = variants_from_syndication(root)
    if not variants:
        print("          no video (mediaDetails=%d)" % len(root.get("mediaDetails") or []))
        return None
    user = root.get("user") or {}
    return {"who": user.get("screen_name"), "variants": variants, "via": "syndication"}


def tier2(sid):
    """Public FixTweet mirror. Only the LOOKUP crosses it; the bytes still come from X's CDN."""
    code, body = get("https://api.fxtwitter.com/status/%s" % sid)
    print("  tier 2  mirror           HTTP %s" % code)
    if code != 200:
        return None
    try:
        tweet = json.loads(body).get("tweet") or {}
    except ValueError:
        return None
    videos = (tweet.get("media") or {}).get("videos") or []
    if not videos:
        print("          mirror found no video")
        return None
    video = videos[0]
    out = []
    for v in video.get("variants") or []:
        link = v.get("url") or ""
        kind = v.get("content_type") or v.get("type") or ""
        if link and ("mp4" in kind or ".mp4" in link):
            m = SIZE_IN_PATH.search(link)
            out.append((link, v.get("bitrate") or (int(m.group(1)) * int(m.group(2)) if m else 0)))
    if not out and video.get("url"):
        out.append((video["url"], 0))
    if not out:
        return None
    return {
        "who": (tweet.get("author") or {}).get("screen_name"),
        "variants": out,
        "via": "mirror",
    }


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    sid = status_id(sys.argv[1])
    if not sid:
        print("not an x.com/twitter.com status link")
        return 1

    print("status %s  token %s" % (sid, token(sid)))
    result = tier1(sid) or tier2(sid)
    if not result:
        print("\nUNRESOLVED -- the app would hand this to yt-dlp.")
        return 1

    print("\nRESOLVED via %s  (@%s)" % (result["via"], result["who"]))
    for link, bitrate in sorted(result["variants"], key=lambda x: x[1]):
        m = SIZE_IN_PATH.search(link)
        size = "%sx%s" % (m.group(1), m.group(2)) if m else "?"
        print("   %-11s %9s bps  %s" % (size, bitrate, link[:78]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
