package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The glare that crosses the wordmark and the signature.
//
// WHY THIS REPLACED THE FIRST TWO ATTEMPTS. Both earlier sheens were technically running and
// could not be seen, for two different reasons:
//
//   1. The band was sized to a CONSTANT (1200px / 900px) against text ~400px wide, so the
//      highlight only ever grazed the glyphs. Fixed by measuring the text.
//   2. Even measured, one pass at 900ms across a wide soft ramp is a brightness change you
//      register as "did something happen?" rather than as a highlight travelling.
//
// So this is deliberately SLOW and NARROW: a tight streak that takes a couple of seconds to
// cross, which is what reads as light moving over a surface. A wide, fast, gentle gradient reads
// as nothing at all -- and an effect nobody can see is not a subtle effect, it is a bug.

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode

/** How long one pass takes. Slow enough to watch the streak travel. */
const val GLARE_SWEEP_MS = 2200

/** Dead time between passes, so it glints periodically instead of strobing. */
const val GLARE_REST_MS = 2600

/**
 * A 0..1 sweep that runs for [sweepMs], then holds at rest for [restMs], forever.
 *
 * Held at 1 rather than snapped to 0, because the brush resolves to plain text at both ends --
 * so the resting state is ordinary type and the loop has no visible seam.
 */
@Composable
fun rememberGlarePhase(
    sweepMs: Int = GLARE_SWEEP_MS,
    restMs: Int = GLARE_REST_MS,
    label: String = "glare",
): State<Float> {
    val total = sweepMs + restMs
    return rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = total
                        0f at 0 using LinearEasing
                        1f at sweepMs using LinearEasing
                        1f at total
                    }
            ),
        label = label,
    )
}

/**
 * A narrow bright streak crossing text of [width] px as [phase] runs 0 to 1.
 *
 * Both ends of the ramp are [base], so the frame at rest is indistinguishable from plain text --
 * which is what makes it safe to leave looping on a name that has to stay readable.
 */
fun glareBrush(
    base: Color,
    highlight: Color,
    phase: Float,
    width: Float,
): Brush {
    // No width yet means no layout pass yet; plain text is the honest answer for that frame.
    if (width <= 1f) return SolidColor(base)

    // Narrow on purpose -- a streak, not a wash. Wide bands read as the whole word changing
    // brightness rather than as something travelling across it.
    val band = width * 0.42f
    val start = -band + phase * (width + band * 2f)
    return Brush.linearGradient(
        0.00f to base,
        0.34f to base,
        0.46f to highlight,
        0.54f to highlight,
        0.66f to base,
        1.00f to base,
        start = Offset(start, 0f),
        end = Offset(start + band, 0f),
        tileMode = TileMode.Clamp,
    )
}

/**
 * The glare colour: the theme's accent pushed toward white.
 *
 * Straight accent is barely brighter than the cream text it crosses, so the streak vanishes into
 * it. Lifting toward white is what makes it read as a specular highlight rather than a tint.
 */
fun glareHighlight(accent: Color): Color =
    Color(
        red = (accent.red + 1f) / 2f,
        green = (accent.green + 1f) / 2f,
        blue = (accent.blue + 0.82f) / 2f,
        alpha = 1f,
    )

// ── touch glare ──────────────────────────────────────────────────────────────────────────────

/**
 * A glare that fires when something is pressed, on top of whatever it does at rest.
 *
 * Held at full brightness for as long as the finger is DOWN, then released as a sweep. Running
 * the sweep on press-down instead would finish while the finger is still there, which reads as
 * the thing having flashed at you rather than responding to you.
 */
@Stable
class TouchGlare internal constructor(
    val interactionSource: MutableInteractionSource,
    private val phaseState: State<Float>,
    private val pressedState: State<Boolean>,
) {
    val phase: Float
        get() = phaseState.value

    val pressed: Boolean
        get() = pressedState.value
}

@Composable
fun rememberTouchGlare(sweepMs: Int = 900): TouchGlare {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // While held: parked mid-sweep so the highlight sits on the glyphs. On release: runs out.
    val phase by
        animateFloatAsState(
            targetValue = if (pressed) 0.5f else 1f,
            animationSpec = tween(if (pressed) 220 else sweepMs, easing = LinearEasing),
            label = "touchGlare",
        )
    val pressedDerived = rememberUpdatedState(pressed)
    val phaseDerived = rememberUpdatedState(phase)
    return remember(source) { TouchGlare(source, phaseDerived, pressedDerived) }
}

/**
 * Press feedback for something that is not text -- the mark, the fish.
 *
 * A brief lift and brighten rather than a ripple: these sit on the background with no container,
 * and a ripple needs edges to look like anything.
 */
@Composable
fun Modifier.pressGlow(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 1.18f else 1f,
            animationSpec = tween(if (pressed) 160 else 420, easing = LinearEasing),
            label = "pressGlowScale",
        )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
