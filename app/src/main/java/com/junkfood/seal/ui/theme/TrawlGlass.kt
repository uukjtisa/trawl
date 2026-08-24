package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The glass system, from design/v0.1.0-baseline-mockup-ui.html lines 88-92:
//
//   .screen                        --glassbg: var(--surfcon)               --blur: 0px
//   .screen[data-glass="subtle"]   --glassbg: rgba(--glasstint, .055)      --blur: 12px
//   .screen[data-glass="full"]     --glassbg: rgba(--glasstint, .075)      --blur: 26px
//   .gsurf                         background: --glassbg; border: 1px solid --gline
//
// THE HARD PART. CSS `backdrop-filter: blur()` blurs what is BEHIND an element. Compose has no
// equivalent: `Modifier.blur()` blurs a composable's OWN content, which for an app bar would
// smear its title rather than the artwork scrolling underneath. Compose 1.11 ships no backdrop
// API (checked: only androidx.compose.ui.draw.BlurKt).
//
// So the backdrop is built by hand out of the primitives that do exist. [GlassBackdrop] records
// the content behind the chrome into a GraphicsLayer and hangs a BlurEffect on it; a surface
// wearing [trawlGlass] draws that already-blurred layer, offset so the pixels line up with what
// is really behind it, clipped to its own shape. The blur samples beyond the clip, which is what
// makes the edges pull in their neighbours instead of fading to nothing.
//
// Everything degrades in the right direction. No backdrop in the tree, or below API 31 where
// RenderEffect does not exist, and a glass surface still paints the exact tint and hairline the
// mockup specifies -- it simply is not blurred. It never renders as an invisible or black panel.

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** Off / Subtle / Full, with the mockup's numbers attached. Default is [OFF] -- see D-06. */
enum class GlassLevel(val id: String, val tintAlpha: Float, val blurRadiusPx: Float) {
    OFF("off", 0f, 0f),
    SUBTLE("subtle", 0.055f, 12f),
    FULL("full", 0.075f, 26f);

    val isOn: Boolean
        get() = this != OFF

    companion object {
        val Default = OFF

        fun fromId(id: String?): GlassLevel = entries.firstOrNull { it.id == id } ?: Default
    }
}

val LocalGlassLevel = compositionLocalOf { GlassLevel.Default }

/** The recorded, already-blurred content behind the chrome. Null when no backdrop is in scope. */
internal val LocalGlassBackdrop = compositionLocalOf<GraphicsLayer?> { null }

/** Where the backdrop starts in root coordinates, so a glass surface can align its sample. */
internal val LocalGlassBackdropOrigin = compositionLocalOf { Offset.Zero }

/**
 * True only where a real blur can happen. RenderEffect is API 31+; below that a glass surface
 * still paints its tint and hairline, it just does not blur.
 */
private val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Records everything it wraps so glass chrome drawn ON TOP of it can sample it.
 *
 * Put the scrolling content inside this, and the app bar / sheet / bubble outside it in the same
 * Box. Wrapping the chrome too would make it sample itself -- the recording would already contain
 * the bar, and the bar would blur a picture of itself.
 *
 * When glass is off this is a plain Box: no layer, no recording, no per-frame capture cost.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val level = LocalGlassLevel.current
    if (!level.isOn || !blurSupported) {
        Box(modifier) { content() }
        return
    }

    val layer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }

    // The blur lives on the layer rather than on each sampler, so the full-screen content is
    // blurred once per frame no matter how many glass surfaces read it.
    //
    // Set in a SideEffect, not inline: composition can run more than once for a single frame and
    // may be abandoned, so mutating a shared object straight from the composable body is a
    // correctness bug even when it happens to look right.
    SideEffect {
        layer.renderEffect = BlurEffect(level.blurRadiusPx, level.blurRadiusPx, TileMode.Clamp)
    }

    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                // Draw the UNBLURRED content normally; only the sampled copy is blurred.
                drawContent()
            }
    ) {
        CompositionLocalProvider(
            LocalGlassBackdrop provides layer,
            LocalGlassBackdropOrigin provides origin,
            content = content,
        )
    }
}

/**
 * The mockup's `.gsurf`: tint, hairline, and -- where there is a backdrop to sample -- blur.
 *
 * With glass off this is the opaque `surfaceContainer` fill and `outline` hairline the mockup
 * specifies for the default state, so callers do not need to branch on the setting.
 *
 * Chrome only. The design never puts glass on list rows (D-05): translucency over a scrolling
 * list makes text sit on moving contrast, which is where "glassmorphism" stops being a look and
 * becomes a legibility bug.
 */
@Composable
fun Modifier.trawlGlass(shape: Shape, level: GlassLevel = LocalGlassLevel.current): Modifier {
    val tokens = LocalTrawlTokens.current
    val backdrop = LocalGlassBackdrop.current
    val origin = LocalGlassBackdropOrigin.current

    if (!level.isOn) {
        return this.clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
    }

    var selfOffset by remember { mutableStateOf(Offset.Zero) }

    return this.clip(shape)
        .onGloballyPositioned { selfOffset = it.positionInRoot() }
        .drawWithContent {
            if (backdrop != null) {
                val d = selfOffset - origin
                translate(-d.x, -d.y) { drawLayer(backdrop) }
            }
            drawContent()
        }
        .background(tokens.glassTint.copy(alpha = level.tintAlpha), shape)
        .border(1.dp, tokens.glassLine, shape)
}
