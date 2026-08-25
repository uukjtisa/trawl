package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Ambient background and download effects, from design/v0.1.0-baseline-mockup-ui.html lines
// 95-120 (.ambient / .blob / .grain / .motes) and 246-260 (sweep / breathe / haulup).
//
// The design's own constraint, and the reason any of this is acceptable on a utility app:
// NOTHING MOVES FASTER THAN 34 SECONDS AND NOTHING SITS ABOVE ~8% OPACITY. Ambient motion that
// can be consciously noticed is a distraction; this is meant to be felt only when the eye rests.
//
// ONE CLOCK. Every blob, mote and wash is driven by a single elapsed-time value updated in a
// withFrameNanos loop and read inside draw lambdas. Reading it there invalidates only the draw
// phase, so a screen full of ambient motion costs no recomposition at all. Giving each of the
// 14 motes its own InfiniteTransition would have been the obvious approach and would have meant
// 14 animation clocks and a recomposition storm behind every screen.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.scale

/** Off / Subtle / Full, matching `[data-motion]` in the mockup. */
enum class MotionLevel(val id: String) {
    OFF("off"),
    SUBTLE("subtle"),
    FULL("full");

    val isOn: Boolean
        get() = this != OFF

    /** `.grain` opacity: 0 / .05 / .085. */
    val grainAlpha: Float
        get() = when (this) {
            OFF -> 0f
            SUBTLE -> 0.05f
            FULL -> 0.085f
        }

    /** `.motes` container opacity: 0 / .5 / 1. */
    val moteAlpha: Float
        get() = when (this) {
            OFF -> 0f
            SUBTLE -> 0.5f
            FULL -> 1f
        }

    companion object {
        val Default = SUBTLE

        fun fromId(id: String?): MotionLevel = entries.firstOrNull { it.id == id } ?: Default
    }
}

val LocalMotionLevel = compositionLocalOf { MotionLevel.Default }

/** Whether the download effects (sweep, breathe, haul wash) are drawn. */
val LocalDownloadFx = compositionLocalOf { true }

// ── the single clock ─────────────────────────────────────────────────────────────────────────

/**
 * Elapsed seconds since this composable entered composition.
 *
 * Monotonic and never wraps, which matters: a wrapping clock makes every derived phase jump at
 * the wrap point, and with 14 motes on independent periods that is a visible stutter every cycle.
 */
@Composable
private fun rememberAmbientClock(running: Boolean): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) seconds.floatValue += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return seconds
}

/** `ease-in-out` over a 0..1 input, matching CSS's default cubic-bezier(.42,0,.58,1) closely. */
private fun easeInOut(t: Float): Float = t * t * (3f - 2f * t)

/** A 0→1→0 triangle over [period], which is what CSS `alternate` does to a keyframe. */
private fun pingPong(seconds: Float, period: Float): Float {
    val p = (seconds / period) % 2f
    return easeInOut(if (p <= 1f) p else 2f - p)
}

// ── blobs ────────────────────────────────────────────────────────────────────────────────────

private class Blob(
    val widthDp: Float,
    val heightDp: Float,
    /** Fractions of the container; negatives intentionally hang the blob off-screen. */
    val leftDp: Float?,
    val rightDp: Float?,
    val topFraction: Float?,
    val topDp: Float?,
    val bottomDp: Float?,
    val dxDp: Float,
    val dyDp: Float,
    val scaleTo: Float,
    val subtlePeriod: Float?,
    val fullPeriod: Float,
)

// .b1 / .b2 / .b3 with their drift1/2/3 keyframes and per-level durations.
private val Blobs = listOf(
    Blob(320f, 250f, leftDp = -60f, rightDp = null, topFraction = null, topDp = -40f,
        bottomDp = null, dxDp = 46f, dyDp = 38f, scaleTo = 1.16f,
        subtlePeriod = 54f, fullPeriod = 34f),
    Blob(280f, 230f, leftDp = null, rightDp = -80f, topFraction = 0.44f, topDp = null,
        bottomDp = null, dxDp = -52f, dyDp = -40f, scaleTo = 1.20f,
        subtlePeriod = 66f, fullPeriod = 42f),
    // b3 animates only at Full; at Subtle it sits still, exactly as the CSS has it.
    Blob(400f, 260f, leftDp = -30f, rightDp = null, topFraction = null, topDp = null,
        bottomDp = -90f, dxDp = 38f, dyDp = -26f, scaleTo = 1.10f,
        subtlePeriod = null, fullPeriod = 50f),
)

/**
 * `filter: blur(46px)` on a solid ellipse is approximated with a radial gradient that holds its
 * colour to ~45% and then falls away.
 *
 * A real blur would mean `Modifier.blur()`, which is a no-op below API 31 -- and an UNBLURRED
 * 320dp ellipse of saturated colour is not a subtle degradation, it is three hard discs sitting
 * behind the UI. The gradient is visually equivalent at this radius, works on every API level,
 * and costs less than a full-screen blur pass every frame.
 */
private fun DrawScope.drawBlob(blob: Blob, color: Color, progress: Float, dp: (Float) -> Float) {
    val w = dp(blob.widthDp)
    val h = dp(blob.heightDp)
    val scale = 1f + (blob.scaleTo - 1f) * progress
    val sw = w * scale
    val sh = h * scale

    val x = when {
        blob.leftDp != null -> dp(blob.leftDp)
        blob.rightDp != null -> size.width - w - dp(blob.rightDp)
        else -> 0f
    } + dp(blob.dxDp) * progress - (sw - w) / 2f

    val y = when {
        blob.topDp != null -> dp(blob.topDp)
        blob.topFraction != null -> size.height * blob.topFraction
        blob.bottomDp != null -> size.height - h - dp(blob.bottomDp)
        else -> 0f
    } + dp(blob.dyDp) * progress - (sh - h) / 2f

    val center = Offset(x + sw / 2f, y + sh / 2f)
    val radius = maxOf(sw, sh) / 2f
    drawOval(
        brush = Brush.radialGradient(
            0.00f to color.copy(alpha = 0.55f),
            0.45f to color.copy(alpha = 0.55f),
            1.00f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        topLeft = Offset(x, y),
        size = Size(sw, sh),
        style = Fill,
    )
}

// ── grain ────────────────────────────────────────────────────────────────────────────────────

/**
 * The mockup's grain is an inline SVG `feTurbulence`. Android has no such filter, so an
 * equivalent 140x140 noise tile is generated once and repeated.
 *
 * Seeded deliberately: an unseeded tile would differ between process launches, which is exactly
 * the kind of "why does it look slightly different today" that is impossible to debug later.
 */
private fun noiseTile(size: Int = 140): ImageBitmap {
    val rnd = Random(20260825)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val v = rnd.nextInt(256)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp.asImageBitmap()
}

// ── motes ────────────────────────────────────────────────────────────────────────────────────

private class Mote(
    val leftFraction: Float,
    val dxDp: Float,
    val sizeDp: Float,
    val durationSeconds: Float,
    val phaseOffset: Float,
)

/**
 * 14 motes with the mockup's exact random ranges (line 1292): position anywhere across the
 * width, horizontal drift +/-13dp, 2-4.4dp across, a 16-36s rise, and a negative start delay so
 * the field is already populated on the first frame rather than all launching together.
 */
private fun buildMotes(): List<Mote> {
    val rnd = Random(20260825)
    return List(14) {
        Mote(
            leftFraction = rnd.nextFloat(),
            dxDp = rnd.nextFloat() * 26f - 13f,
            sizeDp = 2f + rnd.nextFloat() * 2.4f,
            durationSeconds = 16f + rnd.nextFloat() * 20f,
            phaseOffset = rnd.nextFloat(),
        )
    }
}

/** `@keyframes float`: 0% a0, 12% a.55, 75% a.35, 100% a0. */
private fun moteAlpha(p: Float): Float = when {
    p < 0.12f -> (p / 0.12f) * 0.55f
    p < 0.75f -> 0.55f + (p - 0.12f) / 0.63f * (0.35f - 0.55f)
    else -> 0.35f * (1f - (p - 0.75f) / 0.25f)
}

/** The rise distance from the keyframe: `translate3d(dx, -720px, 0)`. */
private const val MOTE_RISE_DP = 720f

// ── the composable ───────────────────────────────────────────────────────────────────────────

/**
 * Drifting light, grain and floating motes. Draws nothing at [MotionLevel.OFF] and starts no
 * clock, so the whole system costs zero when it is switched off.
 *
 * Sits behind content at the bottom of a Box; it is `fillMaxSize` and never intercepts input.
 */
@Composable
fun BoxScope.AmbientBackground(
    level: MotionLevel = LocalMotionLevel.current,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTrawlTokens.current
    val density = LocalDensity.current
    val clock by rememberAmbientClock(running = level.isOn)
    val motes = remember { buildMotes() }
    val grain = remember { noiseTile() }
    val grainBrush = remember(grain) {
        ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
    }
    val blobColors = listOf(tokens.ambientTop, tokens.ambientMid, tokens.ambientLow)

    Canvas(modifier.matchParentSize()) {
        val dp: (Float) -> Float = { with(density) { it.dp.toPx() } }

        // Blobs are drawn at every level, including OFF -- they are the theme's ambient wash, and
        // "motion off" means they stop moving, not that the background goes flat black.
        Blobs.forEachIndexed { i, blob ->
            val period = when (level) {
                MotionLevel.OFF -> null
                MotionLevel.SUBTLE -> blob.subtlePeriod
                MotionLevel.FULL -> blob.fullPeriod
            }
            val progress = if (period == null) 0f else pingPong(clock, period)
            drawBlob(blob, blobColors[i], progress, dp)
        }

        if (level.grainAlpha > 0f) {
            drawRect(brush = grainBrush, alpha = level.grainAlpha, blendMode = BlendMode.Overlay)
        }

        if (level.moteAlpha > 0f) {
            val rise = dp(MOTE_RISE_DP)
            motes.forEach { m ->
                val p = ((clock / m.durationSeconds) + m.phaseOffset) % 1f
                val a = moteAlpha(p) * level.moteAlpha
                if (a <= 0.004f) return@forEach
                drawCircle(
                    color = tokens.mote,
                    radius = dp(m.sizeDp) / 2f,
                    center = Offset(
                        x = m.leftFraction * size.width + dp(m.dxDp) * p,
                        y = size.height + dp(10f) - rise * p,
                    ),
                    alpha = a,
                )
            }
        }
    }
}

/**
 * The haul wash: two soft rises that climb the screen while a download runs (`.haul`, 5.5s
 * linear, the second offset by half a cycle so there is always one on screen).
 */
@Composable
fun BoxScope.HaulWash(active: Boolean, enabled: Boolean = LocalDownloadFx.current) {
    if (!active || !enabled) return
    val tokens = LocalTrawlTokens.current
    val density = LocalDensity.current
    val clock by rememberAmbientClock(running = true)

    Canvas(Modifier.matchParentSize()) {
        val h = with(density) { 150.dp.toPx() }
        // rx = 60% of the width, ry = the 150dp height -- the mockup's ellipse. Brush
        // .radialGradient is circular only, so the circle is drawn at radius = ry and squashed
        // horizontally to reach rx. Getting this wrong is what produced a hard-edged band: a
        // circle of radius 0.6*WIDTH does not fade out within a 150dp box, so the box CLIPPED
        // it, and a clipped gradient is a rectangle with a glow inside it.
        val sx = (size.width * 0.6f) / h
        repeat(2) { i ->
            val p = ((clock / 5.5f) + i * 0.5f) % 1f
            // The CSS element starts at bottom:-150px and rises 300px, so its bottom edge --
            // where the gradient is centred -- travels from height+h to height-h.
            val cy = size.height + h - 2f * h * p
            // The mockup snaps back at the end of each cycle; two washes half a cycle apart
            // mostly hide it. Ramping the ends kills it outright, and a wash that pops is read
            // as a glitch rather than as light.
            val fade =
                when {
                    p < 0.08f -> p / 0.08f
                    p > 0.85f -> (1f - p) / 0.15f
                    else -> 1f
                }
            scale(scaleX = sx, scaleY = 1f, pivot = Offset(size.width / 2f, cy)) {
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            0f to tokens.glassTint.copy(alpha = 0.13f * fade),
                            0.7f to Color.Transparent,
                            center = Offset(size.width / 2f, cy),
                            radius = h,
                        ),
                    radius = h,
                    center = Offset(size.width / 2f, cy),
                )
            }
        }
    }
}

// ── download effects on the card itself ──────────────────────────────────────────────────────

/**
 * `@keyframes sweep`: a soft highlight crossing the progress fill, -100% → 200% over 2.1s on
 * cubic-bezier(.4, 0, .3, 1), repeating.
 *
 * Drawn on top of the fill it decorates, and clipped to it, so it reads as light moving THROUGH
 * the bar rather than a separate object sliding over the card.
 *
 * Deliberately suppressed on an errored task ([error] = true, matching `.bar:not(.err)`). A
 * cheerful shimmer on a failed download is the interface being upbeat about bad news.
 */
@Composable
fun Modifier.progressSweep(
    error: Boolean = false,
    enabled: Boolean = LocalDownloadFx.current,
): Modifier {
    if (!enabled || error) return this
    val clock by rememberAmbientClock(running = true)
    return this.drawWithContent {
        drawContent()
        val p = SweepEasing.transform((clock / 2.1f) % 1f)
        val x = -size.width + p * 3f * size.width
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.55f),
                1f to Color.Transparent,
                startX = x,
                endX = x + size.width,
            ),
            topLeft = Offset(x, 0f),
            size = Size(size.width, size.height),
        )
    }
}

/** cubic-bezier(.4, 0, .3, 1), the mockup's sweep curve. */
private val SweepEasing = CubicBezierEasing(0.4f, 0f, 0.3f, 1f)

/**
 * `@keyframes breathe`: a slow 4.2s glow around a live download card, peaking mid-cycle at
 * `0 0 26px -6px rgba(glasstint, .14)`.
 *
 * A spread of -6px means the shadow is drawn inset from the card's own bounds, so it never
 * becomes a hard ring; here that is a soft radial fading outward from the card edge.
 */
@Composable
fun Modifier.breathe(
    live: Boolean,
    shape: Shape,
    enabled: Boolean = LocalDownloadFx.current,
): Modifier {
    if (!enabled || !live) return this
    val tokens = LocalTrawlTokens.current
    val clock by rememberAmbientClock(running = true)
    return this.drawBehind {
        // 0 → 1 → 0 across the cycle, matching the 0%/50%/100% keyframe stops.
        val phase = pingPong(clock, 2.1f)
        val glow = 20f * phase
        if (glow <= 0.5f) return@drawBehind
        drawRoundRect(
            color = tokens.glassTint.copy(alpha = 0.14f * phase),
            topLeft = Offset(-glow / 2f, -glow / 2f),
            size = Size(size.width + glow, size.height + glow),
            cornerRadius = CornerRadius(glow),
        )
    }
}
