#!/usr/bin/env python3
"""Probe a bare media URL, following whatever redirects stand between you and the file.

This is the reference implementation of what DirectFileCdn.kt performs on-device, and it exists
for the same reason the other probes do: when a link stops working you need to know in seconds
whether the host changed or Trawl did.

    python tools/probe_direct.py https://t.co/wXeDoaeC7O
    python tools/probe_direct.py https://example.cdn/clip.mp4 --bytes

WHAT THIS IS ACTUALLY FOR, AND WHY IT IS PARANOID BY DESIGN.

A "direct link" is the one route with no extractor, no API and no parsing -- which sounds like the
easy case and is in fact the dangerous one. Every other resolver in Trawl gets its URL from a page
it understands. This one gets it from a stranger. So the rules here are deliberately strict:

  1. THE PATH EXTENSION MEANS NOTHING. Anyone can put ".mp4" at the end of a URL. It is not
     evidence and it is never treated as any.
  2. CONTENT-TYPE IS EVIDENCE, BUT NOT PROOF. A server chooses what to claim.
  3. THE FIRST BYTES ARE PROOF. Container magic is produced by the file, not by the host, so it
     is the only check a hostile server cannot simply assert its way past. `--bytes` performs it.
  4. EVERY HOP IS RECORDED AND SHOWN. A chain that quietly crosses three unrelated domains is
     something the person downloading it should get to see, not something to resolve silently.

A verdict of DIRECT_MEDIA means: the chain terminated, the terminal response served bytes, and
(with --bytes) those bytes begin with a container signature. Anything less is not a download.

Standard library only, so it runs anywhere with Python 3.8+.
"""
import argparse
import binascii
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

# Usernames, titles and error pages are routinely non-ASCII, and a Windows console defaults to a
# codepage that cannot encode them -- so printing a result would crash the probe on the platform
# most likely to be running it. Replace rather than raise.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BROWSER_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)

# Deliberately low. A legitimate CDN link resolves in one or two hops; a chain that needs six is
# telling you something about itself.
MAX_HOPS = 6

# Read enough to cover the longest signature plus an ISO box header.
SNIFF_BYTES = 32

MEDIA_TYPES = ("video/", "audio/", "application/octet-stream", "application/mp4")

# HTML that redirects without a Location header. Both are real and both are used to keep a
# scanner on the first host while a browser ends up somewhere else.
META_REFRESH = re.compile(
    r"""<meta[^>]+http-equiv=['"]?refresh['"]?[^>]*content=['"][^'"]*url=([^'"]+)""", re.I
)
JS_LOCATION = re.compile(
    r"""(?:location\.(?:href|replace)\s*(?:=|\()\s*|window\.location\s*=\s*)['"]([^'"]+)['"]""",
    re.I,
)

# Container signatures. Offset, bytes, name. ISO-BMFF (mp4/m4a/mov) puts its brand at offset 4,
# which is why this is a list of (offset, magic) rather than a prefix check.
SIGNATURES = [
    (4, b"ftyp", "ISO-BMFF (mp4/m4v/m4a/mov)"),
    (0, b"\x1a\x45\xdf\xa3", "Matroska / WebM"),
    (0, b"OggS", "Ogg"),
    (0, b"RIFF", "RIFF (avi/wav)"),
    (0, b"FLV\x01", "FLV"),
    (0, b"ID3", "MP3 (ID3)"),
    (0, b"\xff\xfb", "MP3 (frame sync)"),
    (0, b"\xff\xf1", "AAC ADTS"),
    (0, b"fLaC", "FLAC"),
    (0, b"#EXTM3U", "HLS playlist (not a file)"),
]


class NoRedirect(urllib.request.HTTPRedirectHandler):
    """Stop urllib following redirects for us.

    The whole point of this probe is to SEE each hop. urllib's default handler resolves the chain
    silently and hands back only the destination, which is precisely the information being
    withheld from the user today.
    """

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


def request(url, method="GET", extra=None):
    """One hop. Returns (status, headers, body_head, error) and never raises."""
    headers = {"User-Agent": BROWSER_UA, "Accept": "*/*"}
    if extra:
        headers.update(extra)
    req = urllib.request.Request(url, headers=headers, method=method)
    try:
        with OPENER.open(req, timeout=20) as resp:
            body = resp.read(4096) if method == "GET" else b""
            return resp.status, dict(resp.headers), body, None
    except urllib.error.HTTPError as e:
        # A 3xx arrives here because NoRedirect refuses to follow it. That is the success path.
        body = b""
        try:
            body = e.read(4096)
        except Exception:
            pass
        return e.code, dict(e.headers or {}), body, None
    except Exception as e:  # noqa: BLE001 -- a diagnostic tool reports failures, it does not raise
        return 0, {}, b"", "%s: %s" % (type(e).__name__, e)


def sniff(url):
    """Fetch the first bytes and identify the container.

    A ranged GET rather than a HEAD: HEAD is optional and widely mis-implemented on CDN edges,
    and the bytes are the only evidence a lying Content-Type cannot fake.
    """
    status, headers, body, err = request(
        url, "GET", {"Range": "bytes=0-%d" % (SNIFF_BYTES - 1)}
    )
    if err:
        return None, None, err
    if status not in (200, 206):
        return None, None, "sniff returned HTTP %s" % status
    for offset, magic, name in SIGNATURES:
        if body[offset : offset + len(magic)] == magic:
            return name, body[:SNIFF_BYTES], None
    return None, body[:SNIFF_BYTES], None


def html_redirect(body, base):
    """A redirect expressed in the page rather than in a header."""
    text = body.decode("utf-8", "replace")
    for pattern, kind in ((META_REFRESH, "meta refresh"), (JS_LOCATION, "javascript")):
        m = pattern.search(text)
        if m:
            return urllib.parse.urljoin(base, m.group(1).strip()), kind
    return None, None


def follow(start, do_sniff):
    """Walk the chain, recording every hop. Returns (verdict, chain, detail)."""
    url = start
    chain = []
    seen = set()

    for hop in range(MAX_HOPS + 1):
        if url in seen:
            return "REDIRECT_LOOP", chain, "already visited %s" % url
        seen.add(url)

        if hop > MAX_HOPS:
            return "TOO_MANY_HOPS", chain, "gave up after %d hops" % MAX_HOPS

        status, headers, body, err = request(url, "GET", {"Range": "bytes=0-2047"})
        ctype = (headers.get("Content-Type") or "").split(";")[0].strip().lower()
        clen = headers.get("Content-Range") or headers.get("Content-Length") or ""
        chain.append(
            {
                "url": url,
                "status": status,
                "type": ctype,
                "len": clen,
                "host": urllib.parse.urlparse(url).netloc,
                "error": err,
            }
        )
        if err:
            return "UNREACHABLE", chain, err

        if status in (301, 302, 303, 307, 308):
            loc = headers.get("Location")
            if not loc:
                return "BROKEN_REDIRECT", chain, "%s with no Location" % status
            url = urllib.parse.urljoin(url, loc)
            chain[-1]["next"] = url
            chain[-1]["via"] = "HTTP %s" % status
            continue

        if status not in (200, 206):
            return "HTTP_%s" % status, chain, "terminal response was HTTP %s" % status

        if ctype.startswith("text/html"):
            nxt, kind = html_redirect(body, url)
            if nxt:
                url = nxt
                chain[-1]["next"] = url
                chain[-1]["via"] = kind
                continue
            return "HTML_NO_MEDIA", chain, "terminal page is HTML and carries no redirect"

        looks_media = any(ctype.startswith(t) for t in MEDIA_TYPES)
        if not looks_media and ctype:
            return "NOT_MEDIA", chain, "terminal Content-Type is %s" % ctype

        if do_sniff:
            name, head, serr = sniff(url)
            chain[-1]["magic"] = name
            chain[-1]["head"] = binascii.hexlify(head or b"").decode()
            if serr:
                return "UNVERIFIED", chain, serr
            if name is None:
                return "BYTES_NOT_MEDIA", chain, "first bytes match no container signature"
            if "playlist" in name:
                return "PLAYLIST", chain, name
        return "DIRECT_MEDIA", chain, chain[-1].get("magic") or ctype

    return "TOO_MANY_HOPS", chain, "gave up after %d hops" % MAX_HOPS


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("url")
    ap.add_argument(
        "--bytes",
        dest="do_sniff",
        action="store_true",
        help="fetch the first bytes and confirm the container signature (recommended)",
    )
    args = ap.parse_args()

    verdict, chain, detail = follow(args.url, args.do_sniff)

    print("chain:")
    for i, hop in enumerate(chain):
        print("  %d. %s" % (i + 1, hop["url"]))
        bits = ["HTTP %s" % hop["status"]]
        if hop["type"]:
            bits.append(hop["type"])
        if hop["len"]:
            bits.append(hop["len"])
        if hop.get("magic"):
            bits.append("magic=%s" % hop["magic"])
        print("     %s" % "  ".join(bits))
        if hop.get("error"):
            print("     error: %s" % hop["error"])
        if hop.get("next"):
            print("     -> via %s" % hop["via"])

    hosts = []
    for hop in chain:
        if hop["host"] and hop["host"] not in hosts:
            hosts.append(hop["host"])
    if len(hosts) > 1:
        print("\ncrosses %d hosts: %s" % (len(hosts), " -> ".join(hosts)))

    print("\nverdict: %s" % verdict)
    if detail:
        print("detail:  %s" % detail)
    if verdict == "DIRECT_MEDIA" and not args.do_sniff:
        print("note:    Content-Type only. Re-run with --bytes to confirm from the file itself.")
    return 0 if verdict == "DIRECT_MEDIA" else 1


if __name__ == "__main__":
    sys.exit(main())
