package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Direct X / Twitter video resolution, bypassing yt-dlp's Twitter extractor.
//
// WHY THIS EXISTS. yt-dlp's Twitter extractor needs a guest token and increasingly an
// authenticated session; on a phone with no cookies it fails often and confusingly. But the video
// files themselves sit on video.twimg.com and are served without auth to anyone who knows the
// URL. X's own embed widget looks that URL up through a public syndication endpoint, and so can
// we -- no login, no guest token, no rate-limited GraphQL.
//
// WHAT "NON-YT-DLP" MEANS HERE, EXACTLY. The EXTRACTION is ours: the tweet is resolved to a set
// of direct MP4s by this file alone, with no Twitter extractor involved, and the format ladder
// the user picks from is built from X's own variant list rather than guessed by yt-dlp. The BYTES
// are still fetched by yt-dlp's generic HTTP path, because that is the part that was never
// broken -- and it is what gives the download its progress reporting, file naming, history row,
// notification, resume and ffmpeg audio extraction. Rewriting all of that to save one HTTP GET
// would trade a working system for a worse copy of it.
//
// WHY THE LADDER MATTERS AND IS NOT COSMETIC. Handed a bare MP4 URL, yt-dlp's generic extractor
// reports ONE format with no codec fields and no size. The app then classified it as audio-only
// and downloaded it with -x, so an X video arrived as an audio file. Publishing the real
// variants -- 480x270 through 2560x1440, with bitrates and sizes -- is what makes format
// selection mean something and what stops a video coming back as an m4a.
//
// THIS ENDPOINT IS UNDOCUMENTED AND WILL EVENTUALLY ROT. It is a bridge, not a feature. Every
// failure path falls back to handing the original URL to yt-dlp, so the worst case is the
// behaviour we had before rather than a broken download. If X changes it, delete this file and
// its toggle together.

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "TwitterCdn"

/** One rung of X's own ladder for a clip. */
data class TweetVariant(
    val url: String,
    val bitrate: Int,
    val width: Int,
    val height: Int,
    /** Content-Length, or 0 when the HEAD did not answer. Never guessed from bitrate. */
    val sizeBytes: Long,
) {
    /**
     * The id the format chooser hands back to us.
     *
     * Prefixed so the download path can tell "the user picked an X variant, resolve to its URL"
     * from "the user picked a yt-dlp format id, pass it to -f". Those need opposite handling and
     * confusing them silently downloads the wrong thing.
     */
    val formatId: String
        get() = "$FORMAT_PREFIX$bitrate"

    companion object {
        const val FORMAT_PREFIX = "xcdn-"
    }
}

/** What a resolved tweet gives us that yt-dlp's generic extractor could not work out itself. */
data class TweetMedia(
    val variants: List<TweetVariant>,
    val title: String,
    val uploader: String,
    val thumbnail: String?,
) {
    /** Highest bitrate. X serves the same clip at several rungs; this is the one the site plays. */
    val best: TweetVariant?
        get() = variants.maxByOrNull { it.bitrate }

    fun byFormatId(id: String): TweetVariant? = variants.firstOrNull { it.formatId == id }

    /**
     * The ladder as the app's own Format type.
     *
     * Codecs are stated rather than left null on purpose. `null` means UNKNOWN and the app has to
     * guess; these are X's standard H.264/AAC MP4 renditions and we know that, so saying so
     * removes the guess entirely.
     */
    fun toFormats(): List<Format> =
        variants
            .sortedBy { it.bitrate }
            .map { v ->
                Format(
                    formatId = v.formatId,
                    formatNote = if (v.height > 0) "${v.height}p" else null,
                    ext = "mp4",
                    vcodec = "avc1",
                    acodec = "mp4a.40.2",
                    url = v.url,
                    width = v.width.takeIf { it > 0 }?.toDouble(),
                    height = v.height.takeIf { it > 0 }?.toDouble(),
                    resolution = if (v.width > 0) "${v.width}x${v.height}" else null,
                    tbr = v.bitrate / 1000.0,
                    fileSize = v.sizeBytes.takeIf { it > 0 }?.toDouble(),
                    fileSizeApprox = v.sizeBytes.takeIf { it > 0 }?.toDouble(),
                )
            }
            .asReversed()
}

object TwitterCdn {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Resolutions are cached because the URL is needed at least TWICE per download -- once to
     * fetch info and once to download -- and hitting an undocumented endpoint repeatedly for one
     * video is both slower and a better way to get rate-limited.
     */
    private val cache = ConcurrentHashMap<String, Pair<Long, TweetMedia>>()

    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    /** `x.com/user/status/123`, `twitter.com/...`, with or without a query string. */
    private val STATUS =
        Regex(
            """https?://(?:www\.|mobile\.)?(?:twitter|x)\.com/[^/]+/status(?:es)?/(\d+)""",
            RegexOption.IGNORE_CASE,
        )

    private val SIZE_IN_PATH = Regex("""/(\d+)x(\d+)/""")

    fun isTweet(url: String): Boolean = STATUS.containsMatchIn(url)

    fun statusId(url: String): String? = STATUS.find(url)?.groupValues?.get(1)

    /**
     * The syndication endpoint's token.
     *
     * X's embed widget derives it from the status id: `((id / 1e15) * PI)` rendered in base 36
     * with zeros and the decimal point stripped. Reimplemented rather than hard-coded because a
     * fixed token is refused for ids it was not derived from. Verified against the live endpoint.
     *
     * Kotlin has no fractional base-36 formatter, hence the long-hand conversion.
     */
    internal fun token(id: String): String {
        val n = (id.toDoubleOrNull() ?: return "a") / 1e15 * Math.PI
        return base36(n).replace(Regex("(0+|\\.)"), "").ifBlank { "a" }
    }

    private fun base36(value: Double): String {
        val digits = "0123456789abcdefghijklmnopqrstuvwxyz"
        val v = abs(value)
        var whole = floor(v).toLong()
        var frac = v - whole
        val sb = StringBuilder()
        if (whole == 0L) {
            sb.append('0')
        } else {
            val tmp = StringBuilder()
            while (whole > 0) {
                tmp.append(digits[(whole % 36).toInt()])
                whole /= 36
            }
            sb.append(tmp.reverse())
        }
        sb.append('.')
        var i = 0
        while (frac > 0.0 && i < 20) {
            frac *= 36
            val d = floor(frac).toInt().coerceIn(0, 35)
            sb.append(digits[d])
            frac -= d
            i++
        }
        return sb.toString()
    }

    /**
     * Resolves a tweet URL to its media, or null if anything at all goes wrong.
     *
     * Null is not an error to report -- it is the signal to hand the original URL to yt-dlp and
     * carry on exactly as before. A user should never see a failure that came from an
     * optimisation.
     */
    fun resolve(url: String): TweetMedia? {
        val id = statusId(url) ?: return null
        cache[id]?.let { (at, media) ->
            if (System.currentTimeMillis() - at < CACHE_TTL_MS) return media
            cache.remove(id)
        }
        val media = runCatching { fetch(id) }.getOrNull()
        if (media != null) cache[id] = System.currentTimeMillis() to media
        return media
    }

    private fun fetch(id: String): TweetMedia? {
        val body = request(id, token(id)) ?: request(id, "a") ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null

        val variants = collectVariants(root)
        if (variants.isEmpty()) {
            // A photo-only post lands here. Falling through to yt-dlp is deliberate: its Twitter
            // extractor does handle images, and refusing outright would be us removing a
            // capability the app already had.
            TrawlLog.i("$TAG: tweet $id carries no video, leaving it to yt-dlp")
            return null
        }

        val user = root["user"]?.jsonObject
        val handle = user?.get("screen_name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val name = user?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val text = root["text"]?.jsonPrimitive?.contentOrNull.orEmpty()

        return TweetMedia(
            variants = variants,
            title = buildTitle(handle, text, id),
            uploader = name.ifBlank { handle },
            thumbnail =
                root["mediaDetails"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("media_url_https")
                    ?.jsonPrimitive
                    ?.contentOrNull,
        )
    }

    private fun request(id: String, token: String): String? {
        val url = "https://cdn.syndication.twimg.com/tweet-result?id=$id&token=$token&lang=en"
        val req =
            Request.Builder()
                .url(url)
                // The endpoint serves the embed widget and answers accordingly; without a
                // browser UA it refuses.
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "application/json")
                .header("Referer", "https://platform.twitter.com/")
                .build()
        return runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        TrawlLog.i("$TAG: $id returned HTTP ${resp.code}")
                        null
                    } else resp.body?.string()
                }
            }
            .getOrElse {
                TrawlLog.i("$TAG: $id request failed - ${it.message}")
                null
            }
    }

    /**
     * Real byte counts, by asking. The alternative -- bitrate x duration -- is an estimate, and an
     * estimate shown as "12.4 MB" in a picker is a number the user will believe.
     */
    private fun contentLength(url: String): Long {
        val req = Request.Builder().url(url).head().header("User-Agent", BROWSER_UA).build()
        return runCatching {
                client.newCall(req).execute().use { resp ->
                    resp.header("Content-Length")?.toLongOrNull() ?: 0L
                }
            }
            .getOrDefault(0L)
    }

    /**
     * The response carries variants in one of two shapes depending on the tweet, so both are
     * read: `mediaDetails[].video_info.variants[]` (with bitrates) and the flatter
     * `video.variants[]` (without). Only MP4 is taken -- the `application/x-mpegURL` entry in
     * there is an HLS playlist, which needs a player rather than a downloader.
     */
    private fun collectVariants(root: JsonObject): List<TweetVariant> {
        val raw = mutableListOf<Pair<String, Int>>()

        (root["mediaDetails"] as? JsonArray)?.forEach { media ->
            val vi = media.jsonObject["video_info"]?.jsonObject ?: return@forEach
            (vi["variants"] as? JsonArray)?.forEach { v ->
                val o = v.jsonObject
                val type = o["content_type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val link = o["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val bitrate = o["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
                if (type == "video/mp4" && link.isNotBlank()) raw += link to bitrate
            }
        }

        if (raw.isEmpty()) {
            (root["video"] as? JsonObject)?.let { video ->
                (video["variants"] as? JsonArray)?.forEach { v ->
                    val o = v.jsonObject
                    val type = o["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val link = o["src"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (type == "video/mp4" && link.isNotBlank()) {
                        // This shape carries no bitrate; the pixel count in the path is the next
                        // best ordering key, and it keeps the rungs in the right order.
                        val px =
                            SIZE_IN_PATH.find(link)?.let {
                                it.groupValues[1].toInt() * it.groupValues[2].toInt()
                            } ?: 0
                        raw += link to px
                    }
                }
            }
        }

        return raw
            .distinctBy { it.first }
            .map { (link, bitrate) ->
                val m = SIZE_IN_PATH.find(link)
                TweetVariant(
                    url = link,
                    bitrate = bitrate,
                    width = m?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                    height = m?.groupValues?.get(2)?.toIntOrNull() ?: 0,
                    sizeBytes = contentLength(link),
                )
            }
    }

    /**
     * A filename a human can find later.
     *
     * yt-dlp's generic extractor would name this from the CDN path -- something like
     * `ZxQ3n1Kd7.mp4`, useless in a gallery six weeks later. The handle plus the first line of the
     * tweet is what the user would have called it.
     */
    private fun buildTitle(handle: String, text: String, id: String): String {
        val firstLine =
            text.lineSequence()
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
                // Trailing t.co links are on nearly every tweet and say nothing.
                .replace(Regex("""https?://\S+"""), "")
                .trim()
                .take(70)
                .trim()
        return when {
            handle.isNotBlank() && firstLine.isNotBlank() -> "$handle - $firstLine"
            handle.isNotBlank() -> "$handle - $id"
            firstLine.isNotBlank() -> firstLine
            else -> id
        }
    }

    private const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
}
