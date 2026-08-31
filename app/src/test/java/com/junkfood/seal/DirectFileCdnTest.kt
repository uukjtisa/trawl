package com.junkfood.seal

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// The first real tests in this repo. docs/STATUS.md recorded that the only test files were
// upstream's two generated stubs, and that is what this changes.
//
// DirectFileCdn is the one resolver that takes its URL from a stranger, so its decisions are the
// ones worth pinning down. Everything here is pure -- no network, no Android -- which is exactly
// why these functions were separated out of the network stages in the first place.
//
// The fixtures are REAL SHAPES measured on 2026-08-31 with tools/probe_direct.py. The hosts are
// deliberately not the ones that were probed: a test does not need to name someone's site, and a
// checked-in link rots. What matters is the shape, and the shape is reproduced exactly.

import com.junkfood.seal.util.DirectFileCdn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectFileCdnTest {

    // ------------------------------------------------------------------ claiming

    @Test
    fun `claims a bare media url`() {
        assertTrue(DirectFileCdn.isDirectCandidate("https://cdn.example.com/abc123.mp4"))
        assertTrue(DirectFileCdn.isDirectCandidate("https://cdn.example.com/a.webm"))
        assertTrue(DirectFileCdn.isDirectCandidate("https://cdn.example.com/song.m4a"))
    }

    @Test
    fun `claims a known shortener, because the destination cannot be known without asking`() {
        assertTrue(DirectFileCdn.isDirectCandidate("https://t.co/wXeDoaeC7O"))
        assertTrue(DirectFileCdn.isDirectCandidate("https://bit.ly/abcdef"))
    }

    @Test
    fun `does not claim an ordinary page`() {
        assertFalse(DirectFileCdn.isDirectCandidate("https://www.youtube.com/watch?v=jNQXAC9IVRw"))
        assertFalse(DirectFileCdn.isDirectCandidate("https://example.com/some/article"))
        assertFalse(DirectFileCdn.isDirectCandidate("https://example.com/image.png"))
    }

    @Test
    fun `does not claim a non-http scheme`() {
        assertFalse(DirectFileCdn.isDirectCandidate("ftp://example.com/a.mp4"))
        assertFalse(DirectFileCdn.isDirectCandidate("magnet:?xt=urn:btih:abc"))
        assertFalse(DirectFileCdn.isDirectCandidate(""))
    }

    @Test
    fun `a query string does not hide the extension, and does not invent one`() {
        assertTrue(DirectFileCdn.isDirectCandidate("https://cdn.example.com/a.mp4?k=NzI4MjQy"))
        assertFalse(DirectFileCdn.isDirectCandidate("https://example.com/watch?file=a.mp4"))
    }

    // ------------------------------------------------------------------ url mechanics

    @Test
    fun `extension is read from the path only`() {
        assertEquals("mp4", DirectFileCdn.extensionOf("https://h/x/y.MP4?a=1#b"))
        assertNull(DirectFileCdn.extensionOf("https://h/x/y"))
        assertNull(DirectFileCdn.extensionOf("https://h"))
    }

    @Test
    fun `host is read without scheme, path, or port`() {
        assertEquals("cdn.example.com", DirectFileCdn.hostOf("https://cdn.example.com:8443/a.mp4"))
        assertEquals("t.co", DirectFileCdn.hostOf("https://T.CO/abc"))
    }

    @Test
    fun `relative and protocol-relative targets resolve against the page`() {
        val base = "https://play.example.com/dir/page.mp4"
        assertEquals("https://other.com/a.mp4", DirectFileCdn.absolutise("https://other.com/a.mp4", base))
        assertEquals("https://play.example.com/root.mp4", DirectFileCdn.absolutise("/root.mp4", base))
        assertEquals("https://cdn.example.com/a.mp4", DirectFileCdn.absolutise("//cdn.example.com/a.mp4", base))
        assertEquals("https://play.example.com/dir/rel.mp4", DirectFileCdn.absolutise("rel.mp4", base))
    }

    // ------------------------------------------------------------------ redirects in the body

    @Test
    fun `finds a meta refresh redirect`() {
        val html = """<html><head><meta http-equiv="refresh" content="0;url=https://next.example/a.mp4"></head></html>"""
        assertEquals(
            "https://next.example/a.mp4",
            DirectFileCdn.htmlRedirectTarget(html, "https://start.example/x"),
        )
    }

    @Test
    fun `finds a javascript location redirect`() {
        val html = """<script>window.location = "https://next.example/b.mp4";</script>"""
        assertEquals(
            "https://next.example/b.mp4",
            DirectFileCdn.htmlRedirectTarget(html, "https://start.example/x"),
        )
    }

    @Test
    fun `plain html carries no redirect`() {
        assertNull(DirectFileCdn.htmlRedirectTarget("<html><body>hello</body></html>", "https://a/b"))
    }

    // ------------------------------------------------------------------ player pages

    @Test
    fun `lifts the real file out of a player page`() {
        // The measured shape: a URL ending .mp4 that serves HTML wrapping the actual CDN file.
        val html =
            """<html><body><video controls autoplay playsinline>
               <source src="https://cdn2.example.co/iEmjOTJb1.mp4" type="video/mp4"></video></body></html>"""
        assertEquals(
            "https://cdn2.example.co/iEmjOTJb1.mp4",
            DirectFileCdn.playerPageMedia(html, "https://media.example.pizza/iEmjOTJb1.mp4"),
        )
    }

    @Test
    fun `reads a src on the video element itself`() {
        val html = """<video src="/files/clip.mp4" controls></video>"""
        assertEquals(
            "https://play.example.com/files/clip.mp4",
            DirectFileCdn.playerPageMedia(html, "https://play.example.com/watch"),
        )
    }

    @Test
    fun `a player page with no media source yields nothing`() {
        val html = """<video id="player" controls autoplay playsinline></video>"""
        assertNull(DirectFileCdn.playerPageMedia(html, "https://play.example.com/x.mp4"))
    }

    @Test
    fun `ignores a source that is not media`() {
        val html = """<source src="https://cdn.example.com/poster.jpg">"""
        assertNull(DirectFileCdn.playerPageMedia(html, "https://a/b"))
    }

    // ------------------------------------------------------------------ the proof stage

    @Test
    fun `recognises an ISO-BMFF header, whose brand sits at offset four`() {
        val head = byteArrayOf(0, 0, 0, 0x20) + "ftypisom".toByteArray(Charsets.US_ASCII)
        assertEquals("mp4", DirectFileCdn.containerOf(head))
    }

    @Test
    fun `recognises matroska, ogg and flac`() {
        assertEquals(
            "webm",
            DirectFileCdn.containerOf(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 0, 0)),
        )
        assertEquals("ogg", DirectFileCdn.containerOf("OggS____".toByteArray(Charsets.US_ASCII)))
        assertEquals("flac", DirectFileCdn.containerOf("fLaC____".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `html served under a mp4 name is not a container`() {
        // This is the case the whole verifier exists for: 5,979 bytes of player page at a URL
        // ending in .mp4. If this ever returns non-null, Trawl will write a web page to the
        // gallery and call it a video.
        val head = "<!DOCTYPE html><html><head><title>Video".toByteArray(Charsets.US_ASCII)
        assertNull(DirectFileCdn.containerOf(head))
    }

    @Test
    fun `a truncated response is not a container`() {
        assertNull(DirectFileCdn.containerOf(byteArrayOf()))
        assertNull(DirectFileCdn.containerOf(byteArrayOf(0, 0, 0)))
    }

    // ------------------------------------------------------------------ content type

    @Test
    fun `content type is read without its parameters`() {
        assertTrue(DirectFileCdn.looksLikeMedia("video/mp4"))
        assertTrue(DirectFileCdn.looksLikeMedia("audio/mpeg; charset=binary"))
        assertTrue(DirectFileCdn.looksLikeMedia("application/octet-stream"))
    }

    @Test
    fun `html and json are not media`() {
        assertFalse(DirectFileCdn.looksLikeMedia("text/html; charset=utf-8"))
        assertFalse(DirectFileCdn.looksLikeMedia("application/json"))
        assertFalse(DirectFileCdn.looksLikeMedia(null))
        assertFalse(DirectFileCdn.looksLikeMedia(""))
    }
}
