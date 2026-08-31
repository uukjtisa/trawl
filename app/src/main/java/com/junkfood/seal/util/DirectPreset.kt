package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// Per-platform, per-download-type presets for the direct resolvers.
//
// WHY THE RUNGS ARE NAMED THIS WAY, AND WHY THERE IS NO PLAIN "MID". A resolver's ladder is
// whatever the platform published, and that is usually two rungs, sometimes three, occasionally
// one. A "medium" setting over a two-rung ladder has no meaning -- there is no middle -- so the
// choice a person actually has to make is which way to round when the ladder is too short to
// honour the request. MID_HIGH rounds toward best, MID_LOW rounds toward lowest, and both are
// honest about being a direction rather than a position.
//
// The alternative was a percentage or a target resolution, and both lie the same way: they imply
// the ladder is continuous when it has two entries.

import com.junkfood.seal.util.PreferenceUtil.getString

/** Where on a resolver's ladder a preset lands, and which way it rounds when it cannot land there. */
enum class QualityRung(val id: String) {
    /** The top of whatever the platform published. */
    BEST("best"),

    /** Toward the top. On a two-rung ladder this IS best; the name says which way it leans. */
    MID_HIGH("mid_high"),

    /** Toward the bottom. On a two-rung ladder this is the lower rung. */
    MID_LOW("mid_low"),

    /** The smallest the platform published. */
    LOWEST("lowest");

    /**
     * The index this rung selects on a ladder of [size] entries ordered best first.
     *
     * | size | BEST | MID_HIGH | MID_LOW | LOWEST |
     * |------|------|----------|---------|--------|
     * | 1    | 0    | 0        | 0       | 0      |
     * | 2    | 0    | 0        | 1       | 1      |
     * | 3    | 0    | 1        | 1       | 2      |
     * | 4    | 0    | 1        | 2       | 3      |
     *
     * The two middle rungs converge wherever the ladder has an odd number of entries, which is
     * correct: there is a real middle then, and both directions round to it.
     */
    fun indexIn(size: Int): Int {
        if (size <= 1) return 0
        val last = size - 1
        return when (this) {
            BEST -> 0
            LOWEST -> last
            MID_HIGH -> last / 2
            MID_LOW -> (last + 1) / 2
        }
    }

    /** The entry this rung selects, or null for an empty ladder. */
    fun <T> pick(ladder: List<T>): T? =
        if (ladder.isEmpty()) null else ladder[indexIn(ladder.size).coerceIn(0, ladder.lastIndex)]

    companion object {
        val Default = BEST

        fun fromId(id: String?): QualityRung = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * One saved preset: where on the ladder, and what to convert to.
 *
 * [container] is only meaningful for audio and is one of the CONVERT_* constants; a video preset
 * carries whatever is stored and ignores it.
 */
data class DirectPreset(val rung: QualityRung, val container: Int) {
    fun serialise(): String = "${rung.id}|$container"

    companion object {
        val Default = DirectPreset(QualityRung.Default, CONVERT_M4A)

        fun parse(raw: String?): DirectPreset {
            val parts = raw?.split('|') ?: return Default
            if (parts.size != 2) return Default
            val container = parts[1].toIntOrNull() ?: return Default
            return DirectPreset(QualityRung.fromId(parts[0]), container)
        }
    }
}

/**
 * Storage for the presets.
 *
 * Keyed by platform AND download type, because they are genuinely different questions: the best
 * rung of TikTok's two renders is the clean one, and the best rung for audio off the same post is
 * whichever render carries the better bitrate. One global "quality" setting cannot answer both.
 *
 * The platform string comes from DirectResolvers.Entry.platform -- the same single source of truth
 * the router and the onboarding read, so a new resolver gets presets for free.
 */
object DirectPresets {

    /** `direct_preset_TikTok_audio`. Normalised so a platform label with a slash still keys. */
    internal fun keyFor(platform: String, audio: Boolean): String {
        val slug = platform.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "direct_preset_${slug}_${if (audio) "audio" else "video"}"
    }

    fun get(platform: String, audio: Boolean): DirectPreset =
        DirectPreset.parse(keyFor(platform, audio).getString().ifBlank { null })

    fun set(platform: String, audio: Boolean, preset: DirectPreset) {
        PreferenceUtil.encodeString(keyFor(platform, audio), preset.serialise())
    }
}
