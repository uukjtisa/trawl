package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Direct TikTok video resolution, bypassing yt-dlp's TikTok extractor.
//
// WHY THIS EXISTS. yt-dlp's TikTok web path fails with "Unable to extract universal data for
// rehydration" because TikTok fingerprints the TLS handshake and wants curl_cffi impersonation,
// which the Android build of yt-dlp does not ship. Measured, not assumed:
//
//   * the desktop page over plain HTTP returns 200 with ~1.4 KB -- a stub, no data at all;
//   * the mobile API (api22-normal-c-...) returns 200 with an EMPTY BODY, or 429; it now
//     requires request signing (X-Gorgon), which is a moving target and a maintenance trap;
//   * oEmbed works but carries metadata only, no video URL;
//   * m.tiktok.com/v/<id>.html returns the FULL ~290 KB page with the rehydration JSON in it.
//
// So the mobile share page is the way in, and no WebView is needed -- which was the open
// question. A hidden WebView would have worked (a real Chromium engine has a real browser's TLS
// fingerprint) but it is a lot of lifecycle machinery to avoid, and this avoids it.
//
// THE PART THAT IS NOT OBVIOUS: THE URL IS SESSION-BOUND, NOT MERELY TEMPORARY. A playAddr
// fetched here returns 403 to any client that did not also fetch the page -- verified, including
// with a correct Referer. It is tied to the cookies TikTok sets on that page request
// (msToken, tt_chain_token, tt_csrf_token, ttwid). That is why a CDN link copied out of a browser
// "expires in a few hours": it was never portable in the first place.
//
// Consequently this returns HEADERS as well as a URL, and the download must send them. Resolve
// and download therefore have to happen close together, and the result is cached only briefly.
//
// THIS IS A BRIDGE, NOT A FEATURE. Undocumented and liable to change. Every failure path falls
// back to handing the original URL to yt-dlp, so the worst case is the behaviour we had before.

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "TikTokCdn"

/**
 * A resolved TikTok clip.
 *
 * [headers] is not optional decoration -- without the session cookies the URL is a 403 for
 * everyone, including yt-dlp. They travel together or not at all.
 */
data class TikTokMedia(
    /** The aweme id. A real identifier, unlike anything derivable from the signed CDN URL. */
    val id: String,
    val url: String,
    val headers: Map<String, String>,
    val title: String,
    val uploader: String,
    val thumbnail: String?,
    val width: Int,
    val height: Int,
    val durationSeconds: Int,
    val sizeBytes: Long,
) {
    /** The single rung, as the app's own Format type. */
    fun toFormats(): List<Format> =
        listOf(
            Format(
                formatId = FORMAT_ID,
                formatNote = if (height > 0) "${height}p" else null,
                ext = "mp4",
                // Stated rather than left null: an unknown vcodec is read as "audio only", which
                // is how these downloads used to come back as m4a files.
                vcodec = "h264",
                acodec = "mp4a.40.2",
                url = url,
                width = width.takeIf { it > 0 }?.toDouble(),
                height = height.takeIf { it > 0 }?.toDouble(),
                resolution = if (width > 0) "${width}x$height" else null,
                fileSize = sizeBytes.takeIf { it > 0 }?.toDouble(),
                fileSizeApprox = sizeBytes.takeIf { it > 0 }?.toDouble(),
            )
        )

    companion object {
        const val FORMAT_ID = "ttcdn-src"
    }
}

object TikTokCdn {

    /**
     * The page fetch needs a cookie jar, because the cookies it sets are what make the video URL
     * work. A stateless client would resolve a URL that is guaranteed to 403.
     */
    private val jar = RecordingCookieJar()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .cookieJar(jar)
            .followRedirects(true)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Cached far more briefly than the X resolver's ten minutes.
     *
     * These URLs are bound to a session AND time-limited, so a stale entry is not a slow download,
     * it is a 403. Two minutes covers "resolve for the sheet, then download when the user taps"
     * and little else, which is the whole intent.
     */
    private val cache = ConcurrentHashMap<String, Pair<Long, TikTokMedia>>()

    private const val CACHE_TTL_MS = 2 * 60 * 1000L

    /** `tiktok.com/@user/video/<id>`, with or without the usual tracking query. */
    private val VIDEO_ID =
        Regex("""tiktok\.com/(?:@[\w.\-]+/)?(?:video|photo|v)/(\d+)""", RegexOption.IGNORE_CASE)

    /** vm./vt. short links carry no id at all and have to be followed first. */
    private val SHORT = Regex("""https?://(?:vm|vt)\.tiktok\.com/\w+""", RegexOption.IGNORE_CASE)

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 12; NCO-LX1) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    fun isTikTok(url: String): Boolean =
        VIDEO_ID.containsMatchIn(url) || SHORT.containsMatchIn(url)

    fun videoId(url: String): String? {
        VIDEO_ID.find(url)?.let { return it.groupValues[1] }
        // A short link has to be followed. HEAD is enough -- we only want the Location.
        val short = SHORT.find(url)?.value ?: return null
        return runCatching {
                val req = Request.Builder().url(short).head().header("User-Agent", MOBILE_UA).build()
                client.newCall(req).execute().use { resp ->
                    VIDEO_ID.find(resp.request.url.toString())?.groupValues?.get(1)
                }
            }
            .getOrNull()
    }

    /**
     * Resolves a TikTok URL to a playable direct URL plus the headers that make it work, or null
     * to leave the link to yt-dlp.
     */
    fun resolve(url: String): TikTokMedia? {
        val id = videoId(url) ?: return null
        cache[id]?.let { (at, media) ->
            if (System.currentTimeMillis() - at < CACHE_TTL_MS) return media
            cache.remove(id)
        }
        val media = runCatching { fetch(id) }.getOrNull()
        if (media != null) cache[id] = System.currentTimeMillis() to media
        return media
    }

    private fun fetch(id: String): TikTokMedia? {
        val pageUrl = "https://m.tiktok.com/v/$id.html"
        val req =
            Request.Builder()
                .url(pageUrl)
                // The MOBILE user agent is load-bearing. The desktop page answers this client
                // with a 1.4 KB stub; the mobile share page answers with the whole thing.
                .header("User-Agent", MOBILE_UA)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

        val html =
            runCatching {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            TrawlLog.i("$TAG: $id page returned HTTP ${resp.code}")
                            null
                        } else resp.body?.string()
                    }
                }
                .getOrElse {
                    TrawlLog.i("$TAG: $id page fetch failed - ${it.message}")
                    null
                } ?: return null

        val blob =
            Regex(
                    """<script id="__UNIVERSAL_DATA_FOR_REHYDRATION__"[^>]*>(.*?)</script>""",
                    RegexOption.DOT_MATCHES_ALL,
                )
                .find(html)
                ?.groupValues
                ?.get(1)
                ?: run {
                    TrawlLog.i("$TAG: $id page carried no rehydration data (${html.length} bytes)")
                    return null
                }

        val root = runCatching { json.parseToJsonElement(blob).jsonObject }.getOrNull() ?: return null
        val scope = root["__DEFAULT_SCOPE__"]?.jsonObject ?: return null

        // The mobile share page uses the "reflow" scope; the desktop page uses
        // "webapp.video-detail". Both are read so a change of entry point does not break this.
        val item =
            (scope["webapp.reflow.video.detail"]?.jsonObject ?: scope["webapp.video-detail"]?.jsonObject)
                ?.get("itemInfo")
                ?.jsonObject
                ?.get("itemStruct")
                ?.jsonObject
                ?: run {
                    TrawlLog.i("$TAG: $id had no itemStruct (private, removed or region-locked)")
                    return null
                }

        val video = item["video"]?.jsonObject ?: return null
        // downloadAddr preferred where present: it is the source file rather than the streaming
        // rendition. playAddr is the reliable fallback and is what the reflow page always carries.
        val addr =
            video["downloadAddr"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: video["playAddr"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: run {
                    TrawlLog.i("$TAG: $id carried no playable address")
                    return null
                }

        val cookieHeader = jar.headerFor(pageUrl)
        val headers =
            mapOf(
                "User-Agent" to MOBILE_UA,
                "Referer" to "https://www.tiktok.com/",
                "Cookie" to cookieHeader,
            )

        val author = item["author"]?.jsonObject
        val handle = author?.get("uniqueId")?.jsonPrimitive?.contentOrNull.orEmpty()
        val nick = author?.get("nickname")?.jsonPrimitive?.contentOrNull.orEmpty()
        val desc = item["desc"]?.jsonPrimitive?.contentOrNull.orEmpty()

        val media =
            TikTokMedia(
                id = id,
                url = addr,
                headers = headers,
                title = buildTitle(handle, desc, id),
                uploader = nick.ifBlank { handle },
                thumbnail = video["cover"]?.jsonPrimitive?.contentOrNull,
                width = video["width"]?.jsonPrimitive?.intOrNull ?: 0,
                height = video["height"]?.jsonPrimitive?.intOrNull ?: 0,
                durationSeconds = video["duration"]?.jsonPrimitive?.intOrNull ?: 0,
                sizeBytes = contentLength(addr, headers),
            )
        TrawlLog.i("$TAG: $id resolved ${media.width}x${media.height}, ${media.sizeBytes} bytes")
        return media
    }

    /**
     * Real byte count, via a ranged GET.
     *
     * A HEAD is the obvious choice and TikTok's edge does not answer it usefully; a one-byte
     * ranged GET comes back 206 with the full length in Content-Range, which is what the size in
     * the picker is built from. Zero on failure -- an absent size is honest, a guessed one is not.
     */
    private fun contentLength(url: String, headers: Map<String, String>): Long {
        val builder = Request.Builder().url(url).header("Range", "bytes=0-0")
        headers.forEach { (k, v) -> if (v.isNotBlank()) builder.header(k, v) }
        return runCatching {
                client.newCall(builder.build()).execute().use { resp ->
                    resp.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                        ?: resp.header("Content-Length")?.toLongOrNull()
                        ?: 0L
                }
            }
            .getOrDefault(0L)
    }

    private fun buildTitle(handle: String, desc: String, id: String): String {
        val line =
            desc.lineSequence()
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
                .replace(Regex("""https?://\S+"""), "")
                .trim()
                .take(70)
                .trim()
        return when {
            handle.isNotBlank() && line.isNotBlank() -> "$handle - $line"
            handle.isNotBlank() -> "$handle - $id"
            line.isNotBlank() -> line
            else -> id
        }
    }
}

/**
 * A cookie jar that remembers what TikTok set, so those cookies can be replayed to the CDN.
 *
 * OkHttp's default jar discards everything, which for this host means resolving a URL that is
 * guaranteed to 403.
 */
private class RecordingCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { store[it.name] = it }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = store.values.toList()

    /** The cookies as a single request header, which is what yt-dlp will be handed. */
    fun headerFor(@Suppress("UNUSED_PARAMETER") url: String): String =
        store.values.joinToString("; ") { "${it.name}=${it.value}" }
}
