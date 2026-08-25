package com.junkfood.seal.ui.component

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The window switcher, from design/v0.1.0-baseline-mockup-ui.html lines 537-572.
//
// SIMPLE keeps Material's ModalNavigationDrawer: a menu slides over a scrim, which is the
// platform-conventional behaviour and the right default for anyone who does not want a show.
//
// FANCY cannot use it. ModalNavigationDrawer draws a sheet ON TOP of its content and has no way
// to transform what is behind it (verified in NavigationDrawer.kt) -- and the whole point of
// Fancy is that the current screen shrinks to a card and is pushed aside, revealing a full-bleed
// menu underneath. So Fancy is a custom container: the menu is simply the background, and the
// content sits above it in a graphicsLayer that animates.
//
// The numbers are the contract's: translateX(50%) scale(.78), radius 26, over 500ms on
// cubic-bezier(.5,.05,.15,1), with menu rows staggered in from -30dp at 46ms intervals.
//
// NO 3D ROTATION. An earlier round tried a rotationY and he rejected it against his reference:
// this is a flat scale and translate. A perspective tilt reads as a card trick; a flat push
// reads as the screen genuinely moving out of the way.

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.theme.LocalTrawlTokens

/** Simple or Fancy, matching `[data-anim]`. */
enum class AnimStyle(val id: String) {
    SIMPLE("simple"),
    FANCY("fancy");

    companion object {
        val Default = FANCY

        fun fromId(id: String?): AnimStyle = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** `cubic-bezier(.5,.05,.15,1)` -- the push. Slow to leave, decisive to arrive. */
private val PushEasing = CubicBezierEasing(0.5f, 0.05f, 0.15f, 1f)

/** `cubic-bezier(.2,.85,.3,1)` -- the menu rows. */
internal val MenuItemEasing = CubicBezierEasing(0.2f, 0.85f, 0.3f, 1f)

private const val PUSH_MS = 500
internal const val MENU_ITEM_MS = 460
internal const val MENU_STAGGER_MS = 50
internal const val MENU_FIRST_DELAY_MS = 60

/**
 * Holds the current screen and the menu behind it.
 *
 * [open] is deliberately independent of which screen is showing (D-11). Encoding both in one
 * value works right up until they have to vary independently -- which is exactly what
 * "keep the switcher open and preview a screen inside the card" requires.
 */
@Composable
fun TrawlSwitcher(
    open: Boolean,
    style: AnimStyle,
    onDismiss: () -> Unit,
    gesturesEnabled: Boolean,
    menu: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    if (style == AnimStyle.SIMPLE) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        LaunchedEffect(open) { if (open) drawerState.open() else drawerState.close() }
        LaunchedEffect(drawerState.currentValue) {
            if (drawerState.currentValue == DrawerValue.Closed && open) onDismiss()
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled,
            drawerContent = { menu() },
            content = content,
        )
        return
    }

    val tokens = LocalTrawlTokens.current
    val density = LocalDensity.current
    val progress by
        animateFloatAsState(
            targetValue = if (open) 1f else 0f,
            animationSpec = tween(durationMillis = PUSH_MS, easing = PushEasing),
            label = "switcherPush",
        )

    Box(Modifier.fillMaxSize()) {
        // The menu IS the background here -- there is no sheet and no scrim. The gradient is the
        // contract's 150deg ramp; approximated as a linear gradient across the diagonal.
        //
        // Its ALPHA follows the push. Painted unconditionally it is an opaque full-screen layer
        // sitting over everything behind the switcher for the entire life of the app -- which is
        // what was hiding the ambient background even after that was wired up. A backdrop that
        // is only needed while the drawer is open should only be painted while it is open.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = progress }
                .background(
                    Brush.linearGradient(
                        0.00f to tokens.surfaceHigh,
                        0.45f to MaterialTheme.colorScheme.surfaceContainer,
                        1.00f to MaterialTheme.colorScheme.background,
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                )
        ) {
            // Composed only while it can be seen or is on its way out; there is no point running
            // the menu's own staggered entrance behind an opaque full-screen card.
            if (progress > 0.001f) {
                menu()
                // The one thing that teaches the interaction. Nothing else on screen says that
                // the shrunken card is tappable, and a switcher nobody knows how to exit is
                // just a menu that took the page away.
                Text(
                    text = stringResource(R.string.tap_window_to_open).uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 0.14.em,
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 30.dp)
                            .alpha(0.85f * progress),
                )
            }
        }

        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    // translateX(50%) is 50% of the element's OWN width, not the viewport's.
                    translationX = size.width * 0.5f * progress
                    val s = 1f - (1f - 0.78f) * progress
                    scaleX = s
                    scaleY = s
                    shadowElevation = 22f * progress
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            with(density) { (26.dp.toPx() * progress) }
                        )
                    clip = progress > 0.001f
                }
        ) {
            content()
            // While pushed back, the card is a single target that returns you to it. There is no
            // X to close -- he asked for it gone, and a whole screen is a bigger, more obvious
            // target than a 24dp glyph anyway.
            if (open) {
                Box(
                    Modifier.fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                )
            }
        }
    }
}

/**
 * `@keyframes menuItem` -- each row fades in from -30dp, staggered by [index].
 *
 * Applied as a modifier rather than baked into the row so the same stagger can be reused by
 * anything that appears in the menu.
 */
@Composable
fun Modifier.menuItemEntrance(index: Int): Modifier {
    // Self-triggering on first composition rather than taking a `visible` flag: the Fancy
    // container only composes the menu while it can be seen, so entering composition IS the
    // cue. That also keeps every call site to a single argument.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val p by
        animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = MENU_ITEM_MS,
                    delayMillis = MENU_FIRST_DELAY_MS + index * MENU_STAGGER_MS,
                    easing = MenuItemEasing,
                ),
            label = "menuItem$index",
        )
    val density = LocalDensity.current
    return this.graphicsLayer {
        alpha = p
        translationX = with(density) { -30.dp.toPx() } * (1f - p)
    }
}
