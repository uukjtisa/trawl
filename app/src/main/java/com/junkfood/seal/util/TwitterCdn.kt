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

    /**
     * Size probes get their own, impatient client.
     *
     * A Content-Length is a nicety; the download works without it. Sharing the 12s client meant
     * one unresponsive edge node could stall the whole sheet for a minute, so this one gives up
     * quickly and the variant simply shows no size.
     */
    private val probeClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
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

    /**
     * Any x.com / twitter.com link that names a status.
     *
     * Deliberately loose about what sits between the host and `status`: the X app shares
     * `x.com/i/web/status/<id>` and `x.com/i/status/<id>` as readily as `x.com/<user>/status/<id>`,
     * and the original pattern -- which demanded exactly one path segment -- silently refused
     * those. A link the resolver never attempts looks identical to a link it cannot resolve.
     */
    private val STATUS =
        Regex(
            """https?://(?:[\w.-]+\.)?(?:twitter|x)\.com/\S*?status(?:es)?/(\d+)""",
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

    /**
     * Tier 1 then tier 2. See the file header for why the order is not negotiable.
     */
    private fun fetch(id: String): TweetMedia? =
        fetchViaSyndication(id) ?: fetchViaMirror(id)

    private fun fetchViaSyndication(id: String): TweetMedia? {
        val body = request(id, token(id)) ?: request(id, "a") ?: return null
        val root =
            runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: run {
                    TrawlLog.i("$TAG: $id did not return JSON")
                    return null
                }

        // Age-restricted and sensitive posts answer with a tombstone and no media. That is a real
        // limitation of an unauthenticated endpoint, not a parse failure, and it should read as
        // one in the log rather than looking like the extractor is broken.
        val typeName = root["__typename"]?.jsonPrimitive?.contentOrNull
        if (typeName != null && typeName != "Tweet") {
            // Restricted or sensitive. X gates these server-side for signed-out clients, so
            // there is nothing to parse -- but the mirror can usually still answer.
            TrawlLog.i("$TAG: $id came back as $typeName (restricted), trying the mirror")
            return null
        }

        val variants = collectVariants(root)
        if (variants.isEmpty()) {
            // A photo-only post lands here. Falling through to yt-dlp is deliberate: its Twitter
            // extractor does handle images, and refusing outright would remove a capability the
            // app already had.
            val photos = (root["mediaDetails"] as? JsonArray)?.size ?: 0
            TrawlLog.i("$TAG: $id carries no video here (mediaDetails=$photos)")
            return null
        }
        TrawlLog.i("$TAG: $id resolved ${variants.size} variants")

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


    /**
     * Tier 2: the public FixTweet resolver.
     *
     * Reached only when X itself refused -- a tombstoned (age-restricted or sensitive) post, or a
     * response we could not parse. It answers for those, which is the entire reason it is here.
     *
     * Only the LOOKUP crosses a third party; the video bytes still come from video.twimg.com. That
     * is a real privacy cost and it is why this is second rather than first.
     */
    private fun fetchViaMirror(id: String): TweetMedia? {
        val req =
            Request.Builder()
                .url("https://api.fxtwitter.com/status/$id")
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "application/json")
                .build()
        val body =
            runCatching {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            TrawlLog.i("$TAG: mirror returned HTTP ${resp.code} for $id")
                            null
                        } else resp.body?.string()
                    }
                }
                .getOrElse {
                    TrawlLog.i("$TAG: mirror request failed for $id - ${it.message}")
                    null
                } ?: return null

        val tweet =
            runCatching { json.parseToJsonElement(body).jsonObject["tweet"]?.jsonObject }
                .getOrNull() ?: return null

        val video =
            (tweet["media"]?.jsonObject?.get("videos") as? JsonArray)?.firstOrNull()?.jsonObject
                ?: run {
                    TrawlLog.i("$TAG: mirror found no video on $id")
                    return null
                }

        // The variant list carries the same video.twimg.com renditions the syndication endpoint
        // would have, so the ladder survives. `url` is the single best one and backstops it.
        val raw = mutableListOf<Pair<String, Int>>()
        (video["variants"] as? JsonArray)?.forEach { v ->
            val o = v.jsonObject
            val type =
                (o["content_type"] ?: o["type"])?.jsonPrimitive?.contentOrNull.orEmpty()
            val link = o["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            // HLS playlists need a player, not a downloader.
            if (link.isNotBlank() && (type.contains("mp4") || link.contains(".mp4"))) {
                val bitrate =
                    o["bitrate"]?.jsonPrimitive?.intOrNull
                        ?: SIZE_IN_PATH.find(link)?.let {
                            it.groupValues[1].toInt() * it.groupValues[2].toInt()
                        }
                        ?: 0
                raw += link to bitrate
            }
        }
        if (raw.isEmpty()) {
            video["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                val px =
                    SIZE_IN_PATH.find(it)?.let { m ->
                        m.groupValues[1].toInt() * m.groupValues[2].toInt()
                    } ?: 0
                raw += it to px
            }
        }
        if (raw.isEmpty()) return null

        val variants =
            raw.distinctBy { it.first }
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

        val author = tweet["author"]?.jsonObject
        val handle = author?.get("screen_name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val name = author?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val text = tweet["text"]?.jsonPrimitive?.contentOrNull.orEmpty()

        TrawlLog.i("$TAG: mirror resolved $id, ${variants.size} variants")
        return TweetMedia(
            variants = variants,
            title = buildTitle(handle, text, id),
            uploader = name.ifBlank { handle },
            thumbnail = video["thumbnail_url"]?.jsonPrimitive?.contentOrNull,
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
                probeClient.newCall(req).execute().use { resp ->
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

        // A quote-post keeps its own media at the root and the quoted post's under `quoted_tweet`.
        // Someone sharing a quote of a video expects the video, so look there before giving up.
        if (raw.isEmpty()) {
            (root["quoted_tweet"] as? JsonObject)?.let { quoted ->
                (quoted["mediaDetails"] as? JsonArray)?.forEach { media ->
                    val vi = media.jsonObject["video_info"]?.jsonObject ?: return@forEach
                    (vi["variants"] as? JsonArray)?.forEach { v ->
                        val o = v.jsonObject
                        val type = o["content_type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val link = o["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val bitrate = o["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
                        if (type == "video/mp4" && link.isNotBlank()) raw += link to bitrate
                    }
                }
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
        // Filename-safe at the source. yt-dlp sanitises these characters when it writes the
        // file, and the post-download scan matches the file BY this title -- so a title
        // containing one would no longer match the file it produced.
        .replace(Regex("""[/\\:*?"<>|\r\n\t]"""), "_")
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
