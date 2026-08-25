package com.junkfood.seal.ui.page.settings.about

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: page rebuilt to the design contract -- signature banner, portfolio and GitHub rows,
// the upstream credit card, licence and version rows. The donation entry points were removed in
// step 4. AutoUpdateUnavailableDialog and `weblate` are kept unchanged because UpdatePage and
// LanguagesPage import them.
//
// THE CREDIT CARD IS LOAD-BEARING. Crediting JunkFood02 and MaheshTechnicals is a project
// requirement, not merely licence compliance (see ATTRIBUTION.md). It may move; it may not
// shrink. This commit is where it lands, which is why it is also where the splash screen's
// "Powered by Mahesh Technicals" is removed -- attribution is never absent, not even for one
// commit.

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.App.Companion.packageInfo
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalShowMascot
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.ui.theme.glareBrush
import com.junkfood.seal.ui.theme.glareHighlight
import com.junkfood.seal.ui.theme.rememberGlarePhase
import com.junkfood.seal.ui.theme.LocalMotionLevel
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.ui.theme.touchGlareHighlight
import com.junkfood.seal.ui.theme.rememberTouchGlare
import com.junkfood.seal.ui.theme.glareTouch
import com.junkfood.seal.ui.theme.TrawlGold
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState

// Kept for LanguagesPage, which links contributors to the translation project these strings
// came from. Trawl inherits upstream's translations, so that link still improves what it ships.
const val weblate = "https://hosted.weblate.org/engage/seal/"

private const val PORTFOLIO_URL = "https://nicanoriiicariasa-portfolio.vercel.app/"
private const val PORTFOLIO_LABEL = "nicanoriiicariasa-portfolio.vercel.app"
private const val GITHUB_URL = "https://github.com/uukjtisa"
private const val GITHUB_LABEL = "github.com/uukjtisa"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

private const val SEAL_URL = "https://github.com/JunkFood02/Seal"
private const val SEAL_PLUS_URL = "https://github.com/MaheshTechnicals/Sealplus"
// Public because TemplateListPage links to it from the custom-command help. Kept at the
// same name so that import does not have to change.
const val YtdlpRepository = "https://github.com/yt-dlp/yt-dlp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit,
    onNavigateToUpdatePage: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { uriHandler.openUri(it) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item { SignatureBanner() }

            item {
                LinkRow(
                    title = stringResource(R.string.portfolio),
                    subtitle = PORTFOLIO_LABEL,
                    icon = { Icon(Icons.Outlined.Language, null, Modifier.size(18.dp)) },
                ) {
                    open(PORTFOLIO_URL)
                }
            }
            item {
                LinkRow(
                    title = stringResource(R.string.github),
                    subtitle = GITHUB_LABEL,
                    icon = { Icon(Icons.Outlined.Code, null, Modifier.size(18.dp)) },
                ) {
                    open(GITHUB_URL)
                }
            }

            item { SectionLabel(stringResource(R.string.built_on)) }
            item { CreditCard(onOpen = open) }

            item {
                LinkRow(
                    title = stringResource(R.string.licence),
                    subtitle = stringResource(R.string.licence_gpl3),
                    icon = { Icon(Icons.Outlined.Description, null, Modifier.size(18.dp)) },
                ) {
                    open(LICENCE_URL)
                }
            }
            item {
                LinkRow(
                    title = stringResource(R.string.credits),
                    subtitle = stringResource(R.string.credits_desc),
                    icon = { Icon(Icons.Outlined.Description, null, Modifier.size(18.dp)) },
                    onClick = onNavigateToCreditsPage,
                )
            }

            item { SectionLabel(stringResource(R.string.version)) }
            item {
                LinkRow(
                    // "v0.1.0 · niccc2007" -- his call name, and deliberately NOT "fork of Seal
                    // Plus". That belongs in the credit card above, where it is a statement of
                    // provenance, not a subtitle on the app's own version.
                    title = "v${packageInfo.versionName ?: "0.1.0"} · niccc2007",
                    subtitle = stringResource(R.string.check_for_updates),
                    icon = { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(18.dp)) },
                    onClick = onNavigateToUpdatePage,
                )
            }
        }
    }
}

/**
 * `.sig` -- the signature banner.
 *
 * A one-pass sheen crosses the name on entry; at Full motion it repeats every 9 seconds. The
 * watermark is the Trawl mark, oversized and rotated at 7% opacity, bleeding off the bottom-right
 * corner so the panel reads as printed rather than assembled.
 */
@Composable
private fun SignatureBanner() {
    val tokens = LocalTrawlTokens.current
    val scheme = MaterialTheme.colorScheme
    val motionOn = LocalMotionLevel.current.isOn
    // Measured width of the name, so the sheen band can be sized to it (see D-24).
    val nameWidth = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // Nine seconds between glints, which is the contract's own number. The first cut ran one
    // every 4.8s: on a page you sit and read, that stops being "catches the light occasionally"
    // and becomes a blinking element in the corner of your eye.
    val ambient by rememberGlarePhase(sweepMs = 2600, restMs = 6400, label = "sigGlare")
    // Touching the name overrides the clock. `active` stays true through the release sweep, so
    // control hands back to the ambient loop only once the streak has finished leaving -- taking
    // it back mid-streak would cut the highlight off in the middle of the glyphs.
    val touch = rememberTouchGlare()
    val phase =
        when {
            touch.active -> touch.phase
            motionOn -> ambient
            else -> 1f
        }
    val highlight =
        if (touch.active) touchGlareHighlight(tokens.accent) else glareHighlight(tokens.accent)

    // The rule draws ONCE, on arrival. It used to be sized by the glare's phase, so every loop
    // collapsed it to zero and redrew it -- an entrance animation wired to a forever-loop.
    var ruleIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ruleIn = true }
    val reveal by
        animateFloatAsState(
            targetValue = if (ruleIn || !motionOn) 1f else 0f,
            animationSpec = tween(1100, delayMillis = 120, easing = FastOutSlowInEasing),
            label = "sigRule",
        )

    Box(
        Modifier.fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    0.00f to tokens.surfaceHigh,
                    0.46f to scheme.surfaceContainer,
                    1.00f to scheme.surface,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            )
            .border(1.dp, tokens.glassLine, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.trawl_mark),
            contentDescription = null,
            tint = scheme.primary,
            modifier =
                Modifier.size(158.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 26.dp, y = 34.dp)
                    .rotate(-12f)
                    .alpha(0.07f),
        )

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trawl_mark),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = stringResource(R.string.built_by).uppercase(),
                    fontSize = 9.5.sp,
                    letterSpacing = 0.22.em,
                    fontWeight = FontWeight.W700,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = "niccc2007",
                style =
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = FrauncesFamily,
                        fontSize = 37.sp,
                        fontWeight = FontWeight.W700,
                        lineHeight = 37.sp,
                        letterSpacing = 0.005.em,
                        brush =
                            glareBrush(
                                base = scheme.onSurface,
                                highlight = highlight,
                                phase = phase,
                                width = nameWidth.floatValue,
                            ),
                    ),
                modifier = Modifier.padding(top = 9.dp).glareTouch(touch),
                onTextLayout = { nameWidth.floatValue = it.size.width.toFloat() },
            )
            SignatureFlourish(
                reveal = reveal,
                lit = touch.active,
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            )
            Text(
                text = stringResource(R.string.signature_subtitle),
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = scheme.onSurfaceVariant,
            )
        }

        if (LocalShowMascot.current) {
            Icon(
                painter = painterResource(R.drawable.ic_fish),
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(34.dp).align(Alignment.TopEnd).alpha(0.9f),
            )
        }
    }
}



/**
 * `.sigrule`, rebuilt as an engraved flourish rather than a 64dp bar.
 *
 * A tapering rule with a lozenge and diminishing dots -- the shape a struck line takes on a
 * printed plate, which is the register the rest of this card is already in (the watermark, the
 * display face, the sheen). It draws left to right ONCE on arrival, because an entrance is a
 * thing that happens on arrival; the old one was tied to the glare's loop and redrew itself every
 * few seconds.
 *
 * Drawn rather than composed out of Boxes because the taper is a gradient along the rule's own
 * length, and a border-width cannot be animated at all.
 */
@Composable
private fun SignatureFlourish(reveal: Float, lit: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val gold = TrawlGold
    val ink by
        animateColorAsState(
            targetValue = if (lit) gold else primary,
            animationSpec = tween(if (lit) 200 else 620, easing = LinearEasing),
            label = "flourishInk",
        )
    // 210dp, not fillMaxWidth. Run across the whole card and the ornament bunches into the
    // first sixth of a long bare line -- it reads as a tape measure. An engraved rule has to be
    // short enough that the cluster IS the rule.
    Canvas(modifier.width(210.dp).height(14.dp)) {
        val w = size.width
        val midY = size.height / 2f
        val cut = w * reveal.coerceIn(0f, 1f)
        if (cut <= 0f) return@Canvas
        clipRect(right = cut) {
            // The rule: full strength at the name's left edge, gone by the right margin, so it
            // reads as an underline that trails off rather than a bar that stops.
            drawLine(
                brush =
                    Brush.horizontalGradient(
                        0.00f to ink,
                        0.34f to ink.copy(alpha = 0.62f),
                        0.72f to ink.copy(alpha = 0.16f),
                        1.00f to Color.Transparent,
                    ),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            // The left cap -- a short upright tick. Without it the rule looks cut off by the
            // padding instead of started at the name.
            drawLine(
                color = ink,
                start = Offset(0.8.dp.toPx(), midY - 4.dp.toPx()),
                end = Offset(0.8.dp.toPx(), midY + 4.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            // The lozenge, rotated 45 degrees so it reads as a diamond rather than a box.
            val d = 4.4.dp.toPx()
            val cx = w * 0.24f
            drawPath(
                path =
                    Path().apply {
                        moveTo(cx, midY - d)
                        lineTo(cx + d, midY)
                        lineTo(cx, midY + d)
                        lineTo(cx - d, midY)
                        close()
                    },
                color = ink,
            )
            // Dots either side, then three more diminishing away to the right -- the thing that
            // makes it read as ornament rather than as punctuation.
            listOf(
                0.14f to 0.85f,
                0.34f to 0.85f,
                0.50f to 0.50f,
                0.63f to 0.32f,
                0.74f to 0.18f,
            )
                .map { (f, a) -> w * f to a }
                .forEach { (x, a) ->
                    if (x < w) drawCircle(ink.copy(alpha = a), 1.6.dp.toPx(), Offset(x, midY))
                }
            // A hairline echo under the first third, the way a plate leaves a second impression.
            drawLine(
                brush =
                    Brush.horizontalGradient(
                        0.00f to ink.copy(alpha = 0.30f),
                        1.00f to Color.Transparent,
                        startX = 0f,
                        endX = w * 0.42f,
                    ),
                start = Offset(0f, midY + 4.5.dp.toPx()),
                end = Offset(w * 0.42f, midY + 4.5.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.5.sp,
        letterSpacing = 0.1.em,
        fontWeight = FontWeight.W700,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 6.dp),
    )
}

/** `.linkrow` -- a 36dp tinted icon tile, a title, a muted subtitle and a chevron. */
@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val tokens = LocalTrawlTokens.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tokens.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    MaterialTheme.colorScheme.primary
            ) {
                icon()
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * `.creditcard` -- who Trawl is built on.
 *
 * Required by ATTRIBUTION.md and by the project's own rules; it may move but it may not shrink.
 * Each name links to its source, because a credit a reader cannot follow is decoration.
 */
@Composable
private fun CreditCard(onOpen: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        CreditRow(
            name = "Seal",
            by = "JunkFood02",
            desc = stringResource(R.string.credit_seal),
            top = false,
        ) {
            onOpen(SEAL_URL)
        }
        CreditRow(
            name = "Seal Plus",
            by = "MaheshTechnicals",
            desc = stringResource(R.string.credit_seal_plus),
        ) {
            onOpen(SEAL_PLUS_URL)
        }
        CreditRow(
            name = "yt-dlp",
            by = stringResource(R.string.and_contributors),
            desc = stringResource(R.string.credit_ytdlp),
        ) {
            onOpen(YtdlpRepository)
        }
        Box(
            Modifier.padding(top = 14.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Text(
            text = stringResource(R.string.fork_notice),
            fontSize = 11.5.sp,
            lineHeight = 17.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun CreditRow(
    name: String,
    by: String,
    desc: String,
    top: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .padding(top = if (top) 12.dp else 0.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = by,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = desc,
            fontSize = 11.5.sp,
            lineHeight = 17.3.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}


@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun AutoUpdateUnavailableDialog(onDismissRequest: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    val hyperLinkText = stringResource(id = R.string.switch_to_github_builds)
    val text = stringResource(id = R.string.auto_update_disabled_msg, "F-Droid", hyperLinkText)

    val annotatedString = buildAnnotatedString {
        append(text)
        val startIndex = text.indexOf(hyperLinkText)
        val endIndex = startIndex + hyperLinkText.length
        addUrlAnnotation(
            UrlAnnotation("https://github.com/uukjtisa/Trawl/releases/latest"),
            start = startIndex,
            end = endIndex,
        )
        addStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                textDecoration = TextDecoration.Underline,
            ),
            start = startIndex,
            end = endIndex,
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(stringResource(id = R.string.got_it)) { onDismissRequest() }
        },
        icon = {
            Icon(Icons.Outlined.UpdateDisabled, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = stringResource(id = R.string.feature_unavailable),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            ClickableText(
                text = annotatedString,
                onClick = { index ->
                    annotatedString.getUrlAnnotations(index, index).firstOrNull()?.let {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        uriHandler.openUri(it.item.url)
                    }
                },
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            )
        },
    )
}
