package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Which engine is going to handle a link, decided BEFORE the configure sheet opens so the sheet
// can offer the right choices for it.
//
// WHY THIS IS A HOST CHECK AND NOT A NETWORK PROBE. The obvious design is to resolve the link up
// front and report what came back. That would be honest but it would put a full page fetch in
// front of every configure sheet, including the thousand-plus sites where the answer is always
// "yt-dlp" -- seconds of latency to learn something a regex already knows.
//
// So the route is a claim, not a result: it says which engine will be TRIED first. The real
// resolve still happens on download, and still falls back to yt-dlp when it comes up empty. The
// UI's wording carries that distinction.
//
// ONE REGISTRY, READ BY EVERYTHING. DirectResolvers.all is the only place a supported platform is
// named. The router iterates it, the onboarding renders it, and anything else that needs the list
// asks for it. Adding a resolver is one entry here plus the resolver itself -- nothing else has a
// hardcoded "X and TikTok" in it to fall out of date, which is exactly what the first version of
// the onboarding copy did.

import com.junkfood.seal.util.PreferenceUtil.getBoolean

/** The engines a link can be handed to. */
enum class DownloadProvider(val label: String) {
    /** One of Trawl's own resolvers. */
    TRAWL_DIRECT("Trawl direct"),

    /** Everything else, which is most things. */
    YT_DLP("yt-dlp"),
}

/**
 * Every platform Trawl can resolve without yt-dlp's extractor.
 *
 * The single source of truth. If you are about to write a platform name in a string resource or a
 * when-branch, add it here instead and read it from here.
 */
object DirectResolvers {

    /**
     * @param platform what a person calls the site, shown in the UI verbatim.
     * @param claims whether this resolver handles the URL. Host matching only, no I/O.
     * @param enabled the user's switch for this resolver, read fresh each time.
     */
    data class Entry(
        val platform: String,
        val claims: (String) -> Boolean,
        val enabled: () -> Boolean,
    )

    val all: List<Entry> =
        listOf(
            Entry(
                platform = "TikTok",
                claims = { TikTokCdn.isTikTok(it) },
                enabled = { TIKTOK_CDN_FIRST.getBoolean() },
            ),
            Entry(
                platform = "X / Twitter",
                claims = { TwitterCdn.isTweet(it) },
                enabled = { X_CDN_FIRST.getBoolean() },
            ),
            Entry(
                platform = "Facebook",
                claims = { FacebookCdn.isFacebook(it) },
                enabled = { FACEBOOK_CDN_FIRST.getBoolean() },
            ),
            Entry(
                platform = "Newgrounds",
                claims = { NewgroundsCdn.isNewgrounds(it) },
                enabled = { NEWGROUNDS_CDN_FIRST.getBoolean() },
            ),
        )

    /** The platform names, for anywhere that wants to list them. */
    fun platforms(): List<String> = all.map { it.platform }
}

sealed interface DownloadRoute {

    /**
     * A Trawl resolver claims this host and is switched on.
     *
     * [audioConvertible] is what the configure sheet keys its audio section off. Both current
     * resolvers hand back a progressive MP4, which ffmpeg can transcode to anything, so both are
     * convertible. A future resolver returning an HLS playlist would set this false rather than
     * offering an option that cannot work.
     */
    data class Direct(
        val platform: String,
        val audioConvertible: Boolean = true,
    ) : DownloadRoute

    /** No resolver of ours claims it, or the user turned the one that does off. */
    data object YtDlp : DownloadRoute

    val isDirect: Boolean
        get() = this is Direct
}

object DownloadRoutes {

    /**
     * Which engine will be tried first for [url].
     *
     * Pure and instant: no I/O, so it is safe to call from composition on every recomposition.
     * Honours each resolver's switch, because a user who turned one off should see the plain
     * yt-dlp sheet rather than a promise the download will not keep.
     */
    fun of(url: String): DownloadRoute =
        DirectResolvers.all
            .firstOrNull { it.claims(url) && it.enabled() }
            ?.let { DownloadRoute.Direct(it.platform) } ?: DownloadRoute.YtDlp
}
