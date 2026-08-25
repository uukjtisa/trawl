package com.junkfood.seal.ui.page.intro

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The intro, from design/v0.1.0-baseline-mockup-ui.html lines 445-495.
//
// THE THING THAT MATTERS MOST HERE IS THAT IT CANNOT STRAND ANYONE. It replaces the whole app
// until it says it is done, so a sequence that never finishes is not a cosmetic bug -- the app
// is simply gone. Three independent guarantees, in order of how much they are trusted:
//
//   1. It never starts at all if the setting is off or the system is set to reduced motion.
//   2. A tap anywhere ends it immediately.
//   3. A hard failsafe fires onFinished after a fixed ceiling regardless of what the animation
//      is doing, and onFinished is latched so it can only ever run once.
//
// Guarantee 3 is the one that counts, because it does not depend on the timeline being correct.
//
// TIMELINE, transcribed (ms):
//   wordmark rise  760 @ 90     sheen 900 @ 700      travel 900 @ 1280
//   mark           700 @ 420    fade  320 @ 1280
//   fish           820 @ 520    flap  560 @ 1180 x2
//   rule           500 @ 730    fade  300 @ 1280
//   tagline        560 @ 820    fade  300 @ 1280
//   glare          1500 @ 620   travel 900 @ 2180   curtain lifts 620 @ 2320

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalShowMascot
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.ui.theme.glareBrush
import com.junkfood.seal.ui.theme.glareHighlight
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import kotlinx.coroutines.isActive

private val Rise = CubicBezierEasing(0.16f, 0.84f, 0.28f, 1f)
private val Sheen = CubicBezierEasing(0.36f, 0f, 0.22f, 1f)
private val Travel = CubicBezierEasing(0.62f, 0.02f, 0.2f, 1f)
private val Pop = CubicBezierEasing(0.2f, 0.85f, 0.3f, 1.15f)
private val Swim = CubicBezierEasing(0.3f, 0.8f, 0.3f, 1f)
private val Draw = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
private val Curtain = CubicBezierEasing(0.6f, 0f, 0.25f, 1f)

/** When the last thing on screen has finished. */
private const val SEQUENCE_MS = 3000f

/**
 * The hard ceiling. Deliberately longer than the sequence and deliberately independent of it:
 * if the timeline above is ever edited wrongly, this is what still gets the user into the app.
 */
private const val FAILSAFE_MS = 5200L

/** 0..1 across [dur] starting at [delay], eased. Values outside the window clamp. */
private fun phase(t: Float, delay: Float, dur: Float, easing: Easing): Float {
    val raw = ((t - delay) / dur).coerceIn(0f, 1f)
    return easing.transform(raw)
}

/**
 * Trawl's opening sequence.
 *
 * [onFinished] is latched by the caller pattern below -- it is invoked exactly once whether the
 * sequence completed, the user tapped, or the failsafe fired.
 */
@Composable
fun TrawlIntro(onFinished: () -> Unit) {
    val tokens = LocalTrawlTokens.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val showMascot = LocalShowMascot.current

    // Actual rendered width of the wordmark, so the sheen can be sized to it.
    val wordWidth = remember { mutableFloatStateOf(0f) }
    val done = remember { mutableFloatStateOf(0f) }
    val finish = remember {
        {
            if (done.floatValue == 0f) {
                done.floatValue = 1f
                onFinished()
            }
        }
    }

    // Independent of the frame loop on purpose: a stalled or broken animation must not be able
    // to hold the app hostage.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(FAILSAFE_MS)
        finish()
    }

    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                clock.floatValue = (now - start) / 1_000_000f
            }
            if (clock.floatValue >= SEQUENCE_MS) {
                finish()
                break
            }
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(scheme.background)
                // Tap to skip. Someone who has seen it forty times should not have to sit
                // through it a forty-first, and there is no reason to make them find a button.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = finish,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val t = clock.floatValue

        // Curtain: the background fades once the lockup has landed.
        val curtain = 1f - phase(t, 2320f, 620f, Curtain)

        // The wordmark travels toward where the app bar's brand will be. The mockup measures
        // this with a FLIP; here the intro fully replaces the app, so there is nothing on screen
        // to measure against -- these are the mockup's own declared fallbacks, which exist for
        // exactly this case.
        val travel = phase(t, 2180f, 900f, Travel)
        val rise = phase(t, 90f, 760f, Rise)
        // 1500ms, not 900 -- a streak that crosses a 54sp word in under a second is a
        // flicker. This is the effect he could not see.
        val sheenP = phase(t, 620f, 1500f, Sheen)
        val markP = phase(t, 420f, 700f, Pop)
        val markFade = 1f - phase(t, 2180f, 320f, Draw)
        val fishP = phase(t, 520f, 820f, Swim)
        val ruleP = phase(t, 730f, 500f, Draw)
        val tagP = phase(t, 820f, 560f, Draw)
        val tailFade = 1f - phase(t, 2180f, 300f, Draw)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(curtain),
        ) {
            // mark + fish, paired above the wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(markFade),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trawl_mark),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier =
                        Modifier.size(54.dp).graphicsLayer {
                            alpha = markP
                            translationY = with(density) { 16.dp.toPx() } * (1f - markP)
                            val s = 0.6f + 0.4f * markP
                            scaleX = s
                            scaleY = s
                        },
                )
                if (showMascot) {
                    // The fish swims UP INTO the net, then flaps twice. It arrives from below and
                    // to the left rather than fading in, so it reads as caught rather than placed.
                    val flapT = ((t - 1180f) / 620f).coerceAtLeast(0f)
                    val flap =
                        if (flapT in 0f..2f)
                            kotlin.math.sin(flapT * Math.PI.toFloat()) else 0f
                    Icon(
                        painter = painterResource(R.drawable.ic_fish),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier =
                            Modifier.size(30.dp).graphicsLayer {
                                alpha = (fishP / 0.6f).coerceIn(0f, 1f)
                                translationX = with(density) { (-30).dp.toPx() } * (1f - fishP)
                                translationY =
                                    with(density) { 74.dp.toPx() } * (1f - fishP) -
                                        with(density) { 6.dp.toPx() } * flap
                                rotationZ = -24f * (1f - fishP) + 5f * flap - 2.5f
                                val s = 0.4f + 0.6f * fishP
                                scaleX = s
                                scaleY = s
                            },
                    )
                }
            }

            Text(
                text = stringResource(R.string.app_name),
                style =
                    TextStyle(
                        fontFamily = FrauncesFamily,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.W700,
                        lineHeight = 58.sp,
                        letterSpacing = (-0.01).em,
                        textAlign = TextAlign.Center,
                        brush =
                            glareBrush(
                                base = scheme.onSurface,
                                highlight = glareHighlight(tokens.accent),
                                phase = sheenP,
                                width = wordWidth.floatValue,
                            ),
                    ),
                modifier =
                    Modifier.graphicsLayer {
                        // rise: up from 130% of its own height, with a slight settle
                        alpha = (rise / 0.55f).coerceIn(0f, 1f)
                        translationY = size.height * 1.3f * (1f - rise)
                        rotationZ = 2.2f * (1f - rise)
                        // travel: toward the app bar's eventual position
                        translationX += with(density) { (-120).dp.toPx() } * travel
                        translationY += with(density) { (-330).dp.toPx() } * travel
                        val s = 1f - (1f - 0.39f) * travel
                        scaleX = s
                        scaleY = s
                    },
                onTextLayout = { wordWidth.floatValue = it.size.width.toFloat() },
            )

            // .introline -- draws out from the centre under the wordmark.
            Canvas(Modifier.size(width = 104.dp, height = 2.dp).alpha(tailFade)) {
                val w = size.width * ruleP
                drawRoundRect(
                    color = scheme.primary.copy(alpha = 0.8f),
                    topLeft = Offset((size.width - w) / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }

            Text(
                text = stringResource(R.string.intro_tagline),
                fontSize = 13.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.08.em,
                color = scheme.onSurfaceVariant,
                modifier =
                    Modifier.alpha(tailFade).graphicsLayer {
                        alpha = tagP
                        translationY = with(density) { 18.dp.toPx() } * (1f - tagP)
                    },
            )
        }
    }
}

