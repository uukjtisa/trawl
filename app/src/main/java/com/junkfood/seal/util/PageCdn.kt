package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Three more direct resolvers, all of the same shape: fetch one public page or JSON endpoint,
// pull the progressive MP4s out of it, hand the URLs and the headers that make them work to
// yt-dlp. Where TikTok needed a cookie jar and X needed a derived token, these need nothing but
// the right request headers -- which is exactly why they were worth doing.
//
// EVERY CLAIM BELOW WAS MEASURED on 2026-08-25 against real public URLs, including the failure
// modes. The probes are in tools/. Nothing here is inherited from a summary.
//
// WHAT EACH ONE NEEDS, AND WHY IT IS NOT OBVIOUS
//
//   Newgrounds  `X-Requested-With: XMLHttpRequest` on /portal/video/{id}. Without that header the
//               same endpoint answers 403 with an age-gate page. With it, 200 and three quality
//               tiers -- ON AN AGE-RESTRICTED VIDEO, which is the interesting part: the gate is on
//               the HTML page, not on the media endpoint.
//
//   Facebook    A DESKTOP user agent and `Accept: text/html`. A mobile UA gets a flat 400 on every
//               attempt, and `Accept: */*` returns a page with the media keys missing -- which
//               cost a probe round, because it looks exactly like the resolver having broken.
//               /reel/<id> also 400s, so a reel URL has to be rewritten to watch/?v=<id>.
//
//   PornHub     html.unescape, and nothing else. This is the whole reason the earlier attempt
//               failed: the URLs are written into the page HTML-ESCAPED, so every `&` is `&amp;`.
//               Used verbatim the CDN answers 470; unescaped the same URL answers 206 video/mp4.
//               The signed URL also carries the requesting IP, so it works from the device that
//               resolved it and nowhere else -- resolve and download together, never share.
//
// All three return null for every failure, which means "hand it to yt-dlp and carry on".

import com.junkfood.seal.util.PreferenceUtil.getBoolean
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/** One playable rendition. */
data class PageVariant(val label: String, val url: String, val height: Int, val sizeBytes: Long)

/** What a page resolver hands back. Deliberately the same shape for all three. */
data class PageMedia(
    val id: String,
    val title: String,
    val uploader: String,
    val thumbnail: String?,
    val headers: Map<String, String>,
    val variants: List<PageVariant>,
) {
    val best: PageVariant
        get() = variants.maxByOrNull { it.height } ?: variants.first()

    fun byFormatId(id: String?): PageVariant? =
        id?.takeIf { it.isNotBlank() }?.let { wanted -> variants.firstOrNull { it.label == wanted } }

    fun toFormats(): List<Format> =
        variants.map { v ->
            Format(
                formatId = v.label,
                formatNote = if (v.height > 0) "${v.height}p" else null,
                ext = "mp4",
                // Stated, never left null: DownloadUtil reads a null vcodec as "audio only", which
                // is how direct downloads used to arrive as m4a files.
                vcodec = "h264",
                acodec = "mp4a.40.2",
                url = v.url,
                height = v.height.takeIf { it > 0 }?.toDouble(),
                fileSize = v.sizeBytes.takeIf { it > 0 }?.toDouble(),
                fileSizeApprox = v.sizeBytes.takeIf { it > 0 }?.toDouble(),
            )
        }
}

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

internal object PageHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /** Body as text, or null. Never throws: a resolver that throws cannot fall back. */
    fun text(url: String, headers: Map<String, String>): String? =
        runCatching {
                val b = Request.Builder().url(url)
                headers.forEach { (k, v) -> b.header(k, v) }
                client.newCall(b.build()).execute().use { r ->
                    if (!r.isSuccessful) null else r.body?.string()
                }
            }
            .getOrNull()

    /**
     * Real byte count via a one-byte ranged GET.
     *
     * Zero on failure. An absent size renders as blank in the picker, which is honest; a guessed
     * one is not.
     */
    fun size(url: String, headers: Map<String, String>): Long =
        runCatching {
                val b = Request.Builder().url(url).header("Range", "bytes=0-0")
                headers.forEach { (k, v) -> b.header(k, v) }
                client.newCall(b.build()).execute().use { r ->
                    r.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                        ?: r.header("Content-Length")?.toLongOrNull() ?: 0L
                }
            }
            .getOrDefault(0L)
}

/**
 * Undo HTML entity escaping in a URL pulled out of page markup.
 *
 * `&amp;` is the one that matters and the one that broke PornHub, but the others cost nothing to
 * handle and appear in the same markup.
 */
internal fun String.unescapeUrl(): String =
    replace("\\/", "/")
        .replace("&amp;", "&")
        .replace("&#038;", "&")
        .replace("&quot;", "\"")
        .replace("\\u0025", "%")
        .replace("\\u0026", "&")

// ---------------------------------------------------------------------------- Newgrounds

object NewgroundsCdn {
    private const val TAG = "NewgroundsCdn"
    private val ID = Regex("""newgrounds\.com/portal/view/(\d+)""", RegexOption.IGNORE_CASE)

    fun isNewgrounds(url: String): Boolean = ID.containsMatchIn(url)

    fun portalId(url: String): String? = ID.find(url)?.groupValues?.get(1)

    fun resolve(url: String): PageMedia? {
        if (!NEWGROUNDS_CDN_FIRST.getBoolean()) return null
        val id = portalId(url) ?: return null

        // The header is the whole trick. Without it this same URL returns 403 and an age-gate.
        val headers =
            mapOf(
                "User-Agent" to DESKTOP_UA,
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to "https://www.newgrounds.com/",
                "Accept" to "application/json, text/javascript, */*",
            )
        val body =
            PageHttp.text("https://www.newgrounds.com/portal/video/$id", headers)
                ?: run {
                    TrawlLog.i("$TAG: $id portal/video returned nothing")
                    return null
                }

        val mediaHeaders =
            mapOf("User-Agent" to DESKTOP_UA, "Referer" to "https://www.newgrounds.com/")

        // The JSON nests sources by quality label ("1080p", "720p", "360p"), each with a src.
        val variants =
            Regex(""""(\d{3,4})p"\s*:\s*\[\s*\{[^}]*?"src"\s*:\s*"([^"]+)"""")
                .findAll(body)
                .map { m ->
                    val h = m.groupValues[1].toIntOrNull() ?: 0
                    val u = m.groupValues[2].unescapeUrl()
                    PageVariant("ng-${h}p", u, h, PageHttp.size(u, mediaHeaders))
                }
                .toList()
                .ifEmpty {
                    // Fallback: any src at all, if the shape changed but the field did not.
                    Regex(""""src"\s*:\s*"([^"]+\.mp4[^"]*)"""")
                        .findAll(body)
                        .mapIndexed { i, m ->
                            val u = m.groupValues[1].unescapeUrl()
                            PageVariant("ng-$i", u, 0, PageHttp.size(u, mediaHeaders))
                        }
                        .toList()
                }

        if (variants.isEmpty()) {
            TrawlLog.i("$TAG: $id had no sources")
            return null
        }
        val title =
            Regex(""""title"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)?.unescapeUrl()
                ?: "Newgrounds $id"
        val author =
            Regex(""""author"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)?.unescapeUrl()
                .orEmpty()
        TrawlLog.i("$TAG: $id resolved ${variants.size} rungs, best ${variants.maxOf { it.height }}p")
        return PageMedia(id, title, author, null, mediaHeaders, variants.sortedBy { it.height })
    }
}

// ---------------------------------------------------------------------------- Facebook

object FacebookCdn {
    private const val TAG = "FacebookCdn"
    private val ID =
        Regex(
            """facebook\.com/(?:reel/(\d+)|watch/?\?v=(\d+)|[\w.\-]+/videos/(?:[\w.\-]+/)?(\d+))""",
            RegexOption.IGNORE_CASE,
        )
    private val SHORT = Regex("""fb\.watch/[\w-]+""", RegexOption.IGNORE_CASE)

    fun isFacebook(url: String): Boolean = ID.containsMatchIn(url) || SHORT.containsMatchIn(url)

    fun videoId(url: String): String? =
        ID.find(url)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }

    fun resolve(url: String): PageMedia? {
        if (!FACEBOOK_CDN_FIRST.getBoolean()) return null
        val id = videoId(url) ?: return null

        // Desktop UA and Accept: text/html are BOTH load-bearing. A mobile UA gets 400 every time,
        // and Accept: */* returns the page with the media keys stripped out -- which reads as the
        // resolver being broken rather than as a header problem, and cost a probe round to find.
        val headers =
            mapOf(
                "User-Agent" to DESKTOP_UA,
                "Accept" to "text/html,application/xhtml+xml,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.9",
            )
        // /reel/<id> answers 400. watch/?v=<id> answers 200 for the same id.
        val body =
            PageHttp.text("https://www.facebook.com/watch/?v=$id", headers)
                ?: run {
                    TrawlLog.i("$TAG: $id watch page returned nothing")
                    return null
                }

        fun pick(key: String, label: String, height: Int): PageVariant? =
            Regex(""""$key"\s*:\s*"([^"]+)"""")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.unescapeUrl()
                ?.takeIf { it.startsWith("http") }
                ?.let { PageVariant(label, it, height, PageHttp.size(it, headers)) }

        // Heights are nominal: Facebook does not state them here, and the labels are what the
        // picker shows. HD before SD so `best` picks HD.
        val variants =
            listOfNotNull(
                pick("browser_native_sd_url", "fb-sd", 480),
                pick("browser_native_hd_url", "fb-hd", 720),
            )
        if (variants.isEmpty()) {
            TrawlLog.i("$TAG: $id carried no browser_native urls (private, removed or gated)")
            return null
        }
        val title =
            Regex("""<meta property="og:title" content="([^"]+)"""")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.unescapeUrl() ?: "Facebook $id"
        TrawlLog.i("$TAG: $id resolved ${variants.size} rungs")
        return PageMedia(id, title, "", null, headers, variants)
    }
}

// ---------------------------------------------------------------------------- PornHub

/**
 * NOT REGISTERED, and deliberately so. Kept because the measurements are worth not repeating.
 *
 * Measured 2026-08-25: `mediaDefinitions` offers 240, 480, 720 and 1080, and EVERY ONE of them is
 * `"format":"hls"`. The URLs read like progressive files -- `.../1080P_4000K_<id>.mp4/master.m3u8`
 * -- which is a trap: `.mp4` there is a path segment, not the file. A naive scan for `.mp4` counts
 * thirty "progressive" URLs that are all playlists, which is exactly the mistake made here first.
 *
 * The one genuine progressive file is 240p, on ev.phncdn.com. So a direct resolver could only ever
 * hand back 240p where yt-dlp muxes the HLS and gets 1080p. Registering it would be a downgrade
 * dressed as a feature, so it is not registered; yt-dlp keeps this site.
 *
 * The `&amp;` finding below is still real and still bit us -- it is just moot while this is unused.
 */
object PornHubCdn {
    private const val TAG = "PornHubCdn"
    private val KEY = Regex("""pornhub\.(?:com|org|net)/view_video\.php\?viewkey=(\w+)""", RegexOption.IGNORE_CASE)

    fun isPornHub(url: String): Boolean = KEY.containsMatchIn(url)

    fun viewKey(url: String): String? = KEY.find(url)?.groupValues?.get(1)

    fun resolve(url: String): PageMedia? {
        if (!PORNHUB_CDN_FIRST.getBoolean()) return null
        val key = viewKey(url) ?: return null

        val headers =
            mapOf(
                "User-Agent" to DESKTOP_UA,
                "Accept" to "text/html,application/xhtml+xml,*/*;q=0.8",
                "Referer" to "https://www.pornhub.com/",
                // The page gates on an age cookie rather than a login.
                "Cookie" to "age_verified=1; platform=pc",
            )
        val body =
            PageHttp.text("https://www.pornhub.com/view_video.php?viewkey=$key", headers)
                ?: run {
                    TrawlLog.i("$TAG: $key page returned nothing")
                    return null
                }

        // THE BUG THAT MADE THIS LOOK IMPOSSIBLE. These URLs sit in the markup HTML-escaped, so
        // every & is &amp;. Verbatim the CDN answers 470; unescaped the identical URL answers 206
        // video/mp4. It was never an HLS problem.
        //
        // The quality is in the filename: 1080P_4000K_<id>.mp4.
        val seen = LinkedHashMap<Int, String>()
        Regex("""https?://[^"'\\\s]+?/(\d{3,4})P_\d+K_\d+\.mp4[^"'\\\s]*""")
            .findAll(body)
            .forEach { m ->
                val h = m.groupValues[1].toIntOrNull() ?: return@forEach
                val u = m.value.unescapeUrl()
                if (!seen.containsKey(h)) seen[h] = u
            }
        if (seen.isEmpty()) {
            TrawlLog.i("$TAG: $key exposed no progressive renditions")
            return null
        }
        val variants =
            seen.entries.sortedBy { it.key }.map { (h, u) ->
                PageVariant("ph-${h}p", u, h, PageHttp.size(u, headers))
            }
        val title =
            Regex("""<meta property="og:title" content="([^"]+)"""")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.unescapeUrl() ?: key
        TrawlLog.i("$TAG: $key resolved ${variants.size} rungs, best ${variants.maxOf { it.height }}p")
        return PageMedia(key, title, "", null, headers, variants)
    }
}

/**
 * Any of the page resolvers, or null.
 *
 * One entry point so DownloadUtil gains a single branch rather than one per platform.
 */
object PageResolvers {
    fun resolve(url: String): PageMedia? =
        when {
            NewgroundsCdn.isNewgrounds(url) -> NewgroundsCdn.resolve(url)
            FacebookCdn.isFacebook(url) -> FacebookCdn.resolve(url)
            // LAST, deliberately. DirectFileCdn claims by URL SHAPE rather than by host, so left
            // earlier it would swallow a Facebook video URL ending in .mp4 and resolve it as an
            // anonymous file -- losing the title, uploader and thumbnail the site-specific
            // resolver above knows how to get. Generic goes after specific, always.
            directFileEnabled() && DirectFileCdn.isDirectCandidate(url) ->
                DirectFileCdn.resolve(url)
            else -> null
        }

    fun claims(url: String): Boolean =
        NewgroundsCdn.isNewgrounds(url) ||
            FacebookCdn.isFacebook(url) ||
            (directFileEnabled() && DirectFileCdn.isDirectCandidate(url))

    private fun directFileEnabled(): Boolean = DIRECT_FILE_FIRST.getBoolean()
}
