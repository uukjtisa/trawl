package com.junkfood.seal.ui.component

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Two small labels that answer "where did this come from, and how did we get it?" on every
// download card.
//
// The second question is the interesting one. Trawl's whole premise is that extraction can be
// asked more than one way -- so when the direct resolver worked, the app should say so, and when
// it fell back to yt-dlp it should say that too. A reliability claim nobody can see is a claim.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.util.TRAWL_DIRECT

/**
 * The platform, from the URL rather than from the stored extractor name.
 *
 * The URL is the one field every row has had since the app's first version, so this labels old
 * downloads correctly too -- where an extractor name would read "Unknown" for anything saved
 * before the column existed.
 */
fun platformLabel(url: String): String {
    val host =
        runCatching { java.net.URI(url).host.orEmpty() }
            .getOrDefault("")
            .removePrefix("www.")
            .removePrefix("m.")
            .lowercase()
    return when {
        host.isEmpty() -> "LINK"
        host.startsWith("x.") || host.contains("twitter") -> "X"
        host.contains("tiktok") -> "TIKTOK"
        host.contains("youtu") -> "YOUTUBE"
        host.contains("facebook") || host.contains("fb.watch") -> "FACEBOOK"
        host.contains("instagram") -> "INSTAGRAM"
        host.contains("reddit") -> "REDDIT"
        host.contains("twitch") -> "TWITCH"
        host.contains("vimeo") -> "VIMEO"
        host.contains("soundcloud") -> "SOUNDCLOUD"
        host.contains("dailymotion") -> "DAILYMOTION"
        // Everything else gets its own name rather than a shrug: the second-level label is what
        // a person would call the site.
        else -> host.substringBeforeLast('.').substringAfterLast('.').uppercase()
    }
}

/** Whether a history row was produced by a Trawl resolver rather than by yt-dlp. */
fun wasDirect(extractor: String?): Boolean = extractor == TRAWL_DIRECT

/** Where it came from. Outlined and muted -- context, not a headline. */
@Composable
fun PlatformBadge(url: String, modifier: Modifier = Modifier) {
    val label = remember(url) { platformLabel(url) }
    Text(
        text = label,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.06.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * How it was fetched.
 *
 * Filled and accent-tinted when a Trawl resolver did it, outlined and quiet when yt-dlp did. Not
 * because one is better -- yt-dlp is the engine underneath either way -- but because the direct
 * path is the thing this fork exists to add, and it should be visible when it worked.
 */
@Composable
fun ToolBadge(direct: Boolean, modifier: Modifier = Modifier) {
    val tokens = LocalTrawlTokens.current
    val scheme = MaterialTheme.colorScheme
    Text(
        text = if (direct) "DIRECT" else "YT-DLP",
        fontSize = 8.5.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.06.em,
        color = if (direct) scheme.onPrimary else scheme.onSurfaceVariant,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .then(
                    if (direct)
                        Modifier.background(tokens.accent)
                    else Modifier.border(1.dp, scheme.outline, RoundedCornerShape(50))
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
