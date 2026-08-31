package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// The resolver for a link that is just a file -- or claims to be one.
//
// WHY THIS IS THE MOST PARANOID FILE IN THE PROJECT. Every other resolver gets its media URL from
// a page whose structure it understands: TikTok's rehydration blob, X's syndication JSON,
// Newgrounds' portal endpoint. This one is handed a bare URL by a stranger and asked to trust it.
// So the trust is earned rather than assumed, in this order:
//
//   THE PATH EXTENSION PROVES NOTHING.  Anyone can end a URL in ".mp4". It is used only to decide
//                                       whether this resolver is worth TRYING, never as evidence
//                                       about what comes back.
//   CONTENT-TYPE IS EVIDENCE.           A server states it, so it can lie, but a wrong one is
//                                       enough to stop.
//   THE FIRST BYTES ARE PROOF.          Container magic is produced by the file, not asserted by
//                                       the host. It is the only check a hostile or broken server
//                                       cannot talk its way past, and it is the one that decides.
//
// MEASURED 2026-08-31, and the measurements are why this file has three stages instead of one.
// A real example, start to finish:
//
//   https://t.co/<id>                    200 text/html  -> <meta refresh>
//   https://video2.twimg.<tld>/<id>.mp4  200 text/html  -> javascript location=
//   https://play.<host>.blog/<id>.mp4    200 text/html  -> <video><source src=...>
//   https://cdn2.<host>.co/<id>.mp4      206 video/mp4  44,698,822 bytes, ISO-BMFF  -> DOWNLOAD
//
// Three hops, two different redirect mechanisms neither of which is an HTTP 3xx, and a terminal
// URL ending in ".mp4" that serves a 5,979-byte HTML player. A resolver that trusted the
// extension would have downloaded that page and written it to the gallery as a video.
//
// The reference implementation is tools/probe_direct.py and it agrees with this file step for
// step. When a host changes, run the probe first.
//
// Returns null for every failure, which means "hand it to yt-dlp and carry on".

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "DirectFileCdn"

/** Extensions worth TRYING. Not evidence -- see the file header. */
private val MEDIA_EXTENSIONS =
    setOf(
        "mp4", "m4v", "mov", "webm", "mkv", "avi", "flv", "ts",
        "m4a", "mp3", "ogg", "oga", "opus", "flac", "wav", "aac",
    )

/**
 * Hosts whose whole job is to hide the destination.
 *
 * Claimed so the walker gets a chance at them. A shortener that resolves to something yt-dlp
 * handles better still ends up at yt-dlp, because the walk returns null when the chain does not
 * end in media.
 */
private val SHORTENER_HOSTS =
    setOf(
        "t.co", "bit.ly", "tinyurl.com", "ow.ly", "is.gd", "buff.ly",
        "cutt.ly", "shorturl.at", "rb.gy", "s.id", "rebrand.ly",
    )

/** Content types that could be media. `octet-stream` is included because CDNs are careless. */
private val MEDIA_CONTENT_TYPES =
    listOf("video/", "audio/", "application/octet-stream", "application/mp4")

/**
 * Container signatures, as (offset, magic, name).
 *
 * ISO-BMFF puts its brand at offset 4 rather than 0, which is why this carries an offset instead
 * of being a prefix test.
 */
internal val CONTAINER_SIGNATURES: List<Triple<Int, ByteArray, String>> =
    listOf(
        Triple(4, "ftyp".toByteArray(Charsets.US_ASCII), "mp4"),
        Triple(0, byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), "webm"),
        Triple(0, "OggS".toByteArray(Charsets.US_ASCII), "ogg"),
        Triple(0, "RIFF".toByteArray(Charsets.US_ASCII), "avi"),
        Triple(0, byteArrayOf(0x46, 0x4C, 0x56, 0x01), "flv"),
        Triple(0, "ID3".toByteArray(Charsets.US_ASCII), "mp3"),
        Triple(0, byteArrayOf(0xFF.toByte(), 0xFB.toByte()), "mp3"),
        Triple(0, byteArrayOf(0xFF.toByte(), 0xF1.toByte()), "aac"),
        Triple(0, "fLaC".toByteArray(Charsets.US_ASCII), "flac"),
    )

/** Deliberately low: a legitimate link resolves in one or two hops. */
internal const val MAX_HOPS = 6

/** Enough for the longest signature plus an ISO box header. */
private const val SNIFF_BYTES = 32

private const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/** A redirect expressed in the page body rather than in a header. Both are real, both are used. */
private val META_REFRESH =
    Regex("""<meta[^>]+http-equiv=['"]?refresh['"]?[^>]*content=['"][^'"]*url=([^'"]+)""", RegexOption.IGNORE_CASE)

private val JS_LOCATION =
    Regex(
        """(?:location\.(?:href|replace)\s*(?:=|\()\s*|window\.location\s*=\s*)['"]([^'"]+)['"]""",
        RegexOption.IGNORE_CASE,
    )

/** `<source src>` first: a `<video>` with children states its sources there. */
private val SOURCE_SRC =
    Regex("""<source[^>]+src=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

private val VIDEO_SRC =
    Regex("""<video[^>]+src=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

object DirectFileCdn {

    /**
     * A client that does NOT follow redirects.
     *
     * The whole point is to SEE each hop, both to cap them and to show the user where a link
     * actually went. OkHttp's default resolves the chain silently and reports only the
     * destination, which is exactly the information worth having.
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    // ---------------------------------------------------------------- pure, and unit tested

    /** The lowercased extension of a URL's path, ignoring query and fragment. */
    internal fun extensionOf(url: String): String? {
        val path = url.substringAfter("://", url).substringAfter('/', "").substringBefore('?').substringBefore('#')
        // No missing-delimiter default here. `substringAfterLast('/', "")` returns "" when there
        // is no second slash, which is the case for every single-segment path -- so
        // https://cdn/abc.mp4 reported NO extension and the resolver refused to claim the exact
        // shape it exists for. The default overload keeps the whole string, which is correct.
        val last = path.substringAfterLast('/')
        if (!last.contains('.')) return null
        return last.substringAfterLast('.').lowercase().takeIf { it.isNotBlank() }
    }

    internal fun hostOf(url: String): String =
        url.substringAfter("://", "").substringBefore('/').substringBefore(':').lowercase()

    /**
     * Whether this resolver is worth trying. A claim about the URL's SHAPE, never about its
     * content -- see the file header.
     */
    fun isDirectCandidate(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        if (hostOf(url) in SHORTENER_HOSTS) return true
        return extensionOf(url) in MEDIA_EXTENSIONS
    }

    /** Resolve a relative or protocol-relative target against the page it was found on. */
    internal fun absolutise(target: String, base: String): String {
        val t = target.trim()
        return when {
            t.startsWith("http://", true) || t.startsWith("https://", true) -> t
            t.startsWith("//") -> base.substringBefore("://") + ":" + t
            t.startsWith("/") -> {
                val scheme = base.substringBefore("://")
                "$scheme://${hostOf(base)}$t"
            }
            else -> base.substringBeforeLast('/', base) + "/" + t
        }
    }

    /** A redirect the page performs itself. Returns the absolute target, or null. */
    internal fun htmlRedirectTarget(html: String, base: String): String? {
        META_REFRESH.find(html)?.groupValues?.get(1)?.let { return absolutise(it, base) }
        JS_LOCATION.find(html)?.groupValues?.get(1)?.let { return absolutise(it, base) }
        return null
    }

    /**
     * The media a player page is wrapping.
     *
     * This is the stage the whole feature turned on: the pages measured all serve `text/html` at a
     * URL ending in `.mp4`, with the real CDN file named inside a `<source>` element.
     */
    internal fun playerPageMedia(html: String, base: String): String? {
        for (m in SOURCE_SRC.findAll(html)) {
            val u = absolutise(m.groupValues[1], base)
            if (extensionOf(u) in MEDIA_EXTENSIONS) return u
        }
        for (m in VIDEO_SRC.findAll(html)) {
            val u = absolutise(m.groupValues[1], base)
            if (extensionOf(u) in MEDIA_EXTENSIONS) return u
        }
        return null
    }

    /** The container the first bytes actually describe, or null if they describe none. */
    internal fun containerOf(head: ByteArray): String? =
        CONTAINER_SIGNATURES.firstOrNull { (offset, magic, _) ->
                head.size >= offset + magic.size &&
                    magic.indices.all { head[offset + it] == magic[it] }
            }
            ?.third

    internal fun looksLikeMedia(contentType: String?): Boolean {
        val t = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return MEDIA_CONTENT_TYPES.any { t.startsWith(it) }
    }

    // ---------------------------------------------------------------- the network stages

    private fun headers(): Map<String, String> =
        mapOf("User-Agent" to BROWSER_UA, "Accept" to "*/*")

    /**
     * Walk the chain to whatever it actually ends at.
     *
     * Returns the terminal URL, or null when the chain loops, runs long, breaks, or ends somewhere
     * that is not media and carries no onward pointer.
     */
    private fun walk(start: String): String? {
        var url = start
        val seen = linkedSetOf<String>()

        repeat(MAX_HOPS + 1) {
            if (!seen.add(url)) {
                TrawlLog.i("$TAG: redirect loop at $url")
                return null
            }
            val req =
                Request.Builder().url(url).header("Range", "bytes=0-4095").apply {
                    headers().forEach { (k, v) -> header(k, v) }
                }
            val outcome =
                runCatching {
                        client.newCall(req.build()).execute().use { r ->
                            val ctype = r.header("Content-Type")
                            when {
                                r.code in 300..399 -> {
                                    val loc = r.header("Location") ?: return@use null
                                    Hop.Redirect(absolutise(loc, url))
                                }
                                !r.isSuccessful -> {
                                    TrawlLog.i("$TAG: $url returned HTTP ${r.code}")
                                    null
                                }
                                ctype?.startsWith("text/html", true) == true -> {
                                    val body = r.body?.string().orEmpty()
                                    val onward =
                                        htmlRedirectTarget(body, url) ?: playerPageMedia(body, url)
                                    if (onward == null) {
                                        TrawlLog.i("$TAG: $url is HTML carrying no media and no redirect")
                                        null
                                    } else Hop.Redirect(onward)
                                }
                                looksLikeMedia(ctype) -> Hop.Terminal(url)
                                else -> {
                                    TrawlLog.i("$TAG: $url served $ctype, which is not media")
                                    null
                                }
                            }
                        }
                    }
                    .getOrElse {
                        TrawlLog.i("$TAG: $url failed - ${it.message}")
                        null
                    } ?: return null

            when (outcome) {
                is Hop.Terminal -> return outcome.url
                is Hop.Redirect -> url = outcome.url
            }
        }
        TrawlLog.i("$TAG: gave up after $MAX_HOPS hops from $start")
        return null
    }

    private sealed interface Hop {
        data class Redirect(val url: String) : Hop
        data class Terminal(val url: String) : Hop
    }

    /**
     * The check that decides. Reads the first bytes and matches container magic.
     *
     * Returns the container name and the total size, or null. A server that claims `video/mp4` and
     * serves an HTML error page fails here, which is the entire reason this stage exists.
     */
    private fun verify(url: String): Pair<String, Long>? {
        val b = Request.Builder().url(url).header("Range", "bytes=0-${SNIFF_BYTES - 1}")
        headers().forEach { (k, v) -> b.header(k, v) }
        return runCatching {
                client.newCall(b.build()).execute().use { r ->
                    if (r.code != 200 && r.code != 206) return@use null
                    val head = r.body?.bytes() ?: return@use null
                    val container = containerOf(head) ?: return@use null
                    val total =
                        r.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                            ?: r.header("Content-Length")?.toLongOrNull() ?: 0L
                    container to total
                }
            }
            .getOrNull()
    }

    /**
     * Resolve [url] to a single playable file, or null to leave it to yt-dlp.
     *
     * One variant, always. A bare file has exactly one rendition -- there is no ladder to choose
     * from, and offering one would be an invention.
     */
    fun resolve(url: String): PageMedia? {
        if (!isDirectCandidate(url)) return null

        val terminal = walk(url) ?: return null
        val (container, size) = verify(terminal) ?: run {
            TrawlLog.i("$TAG: $terminal did not begin with a container signature, refusing it")
            return null
        }

        val name = terminal.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
        val title = name.substringBeforeLast('.').replace(Regex("""[/\\:*?"<>|\r\n\t]"""), "_").take(70)

        TrawlLog.i("$TAG: $url resolved to $terminal ($container, $size bytes)")
        return PageMedia(
            id = title.ifBlank { "direct" },
            title = title.ifBlank { "download" },
            uploader = hostOf(terminal),
            thumbnail = null,
            headers = headers(),
            variants =
                listOf(
                    PageVariant(
                        // The label is what the picker shows. "Source" rather than a resolution,
                        // because the height is genuinely unknown here -- claiming 1080p from a
                        // byte count would be a guess dressed as a measurement.
                        label = "source",
                        url = terminal,
                        height = 0,
                        sizeBytes = size,
                    )
                ),
        )
    }
}
