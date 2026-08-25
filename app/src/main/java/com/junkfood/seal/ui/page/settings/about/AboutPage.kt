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

    // A slow, repeating glare rather than a single quick sheen. This is his signature card --
    // it should keep catching the light, the way an embossed name does when you tilt it.
    val glare by rememberGlarePhase(label = "sigGlare")
    val phase = if (motionOn) glare else 1f

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
                                highlight = glareHighlight(tokens.accent),
                                phase = phase,
                                width = nameWidth.floatValue,
                            ),
                    ),
                modifier = Modifier.padding(top = 9.dp),
                onTextLayout = { nameWidth.floatValue = it.size.width.toFloat() },
            )
            // .sigrule -- draws left-to-right on entry. A border-width cannot be animated, which
            // is why this is a drawn box rather than an underline.
            Box(
                Modifier.padding(top = 12.dp, bottom = 10.dp)
                    .height(2.dp)
                    .width(64.dp * phase.coerceAtLeast(0.001f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(scheme.primary.copy(alpha = 0.85f))
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
