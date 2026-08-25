package com.junkfood.seal.ui.page.home

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The home screen's parts, transcribed from design/v0.1.0-baseline-mockup-ui.html:
//
//   .brandhead   line 122   .urlbar / .fastpill / .go2 / .pasteb   lines 168-186
//   .fasttray    line 187   .qchip                                 line 190
//   .qstrip      line 194   .sechead                               line 213
//   .endfish     line 262
//
// Every size, radius and weight here is the mockup's own number. Where a value looks oddly
// specific -- 58dp, 13.5sp, -20dp -- it is because it IS specific, not because it was rounded
// from something tidier.

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.em
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.ui.theme.TrawlShape
import com.junkfood.seal.ui.theme.trawlGlass
import com.junkfood.seal.ui.theme.touchGlareHighlight
import com.junkfood.seal.ui.theme.rememberTouchGlare
import com.junkfood.seal.ui.theme.glareTouch
import com.junkfood.seal.ui.theme.glareBrush
import com.junkfood.seal.ui.theme.GlintIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf

/**
 * `.brandhead` -- the 32dp mark, the wordmark at 31sp/700 in the display face, and the fish.
 *
 * The fish is [showMascot]-gated because it is the one purely decorative element on the screen
 * and some people will not want it; the mark and wordmark are the app's identity and are not
 * separately switchable.
 */
@Composable
fun TrawlBrandHead(showMascot: Boolean, modifier: Modifier = Modifier) {
    val tokens = LocalTrawlTokens.current
    // The wordmark answers a finger: the streak sweeps in, parks under it, and runs out on
    // release. Nothing here navigates -- it is the app's own name, and the glint is the whole
    // point of touching it -- so this is press feedback, not a button (see Modifier.glareTouch).
    val glare = rememberTouchGlare()
    val wordWidth = remember { mutableFloatStateOf(0f) }
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlintIcon(
            painter = painterResource(R.drawable.trawl_mark),
            contentDescription = null,
            size = 32.dp,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_name),
            style =
                TextStyle(
                    fontFamily = FrauncesFamily,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.W700,
                    lineHeight = 31.sp,
                    // Measured, not guessed: a band sized to a constant against ~180px of text
                    // only ever grazes the glyphs. This is the same lesson as D-24.
                    brush =
                        glareBrush(
                            base = MaterialTheme.colorScheme.onSurface,
                            highlight = touchGlareHighlight(tokens.accent),
                            phase = glare.phase,
                            width = wordWidth.floatValue,
                        ),
                ),
            onTextLayout = { wordWidth.floatValue = it.size.width.toFloat() },
            modifier = Modifier.glareTouch(glare),
        )
        if (showMascot) {
            GlintIcon(
                painter = painterResource(R.drawable.ic_fish),
                contentDescription = null,
                size = 25.dp,
                tint = tokens.accent.copy(alpha = 0.9f),
            )
        }
    }
}

/**
 * `.urlbar` -- 58dp tall at radius 26, glass chrome, with the FAST pill, paste and go controls.
 *
 * A real text field rather than the mockup's static placeholder, obviously; the placeholder copy
 * and the filled-state treatment (monospace, brighter) both come from the contract.
 */
@Composable
fun TrawlUrlBar(
    value: String,
    onValueChange: (String) -> Unit,
    fastEnabled: Boolean,
    onToggleFast: () -> Unit,
    onPaste: () -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(58.dp)
                .trawlGlass(TrawlShape.UrlBar)
                .padding(start = 16.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.weight(1f)) { textField(Modifier.fillMaxWidth()) }
        FastPill(enabled = fastEnabled, onClick = onToggleFast)
        // .pasteb -- 34dp circle, muted; a secondary affordance next to the primary action.
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(50)).clickable(onClick = onPaste),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_paste_trawl),
                contentDescription = stringResource(R.string.paste),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        // .go2 -- 44dp, radius 14, filled with primary. The one unambiguous action on the screen.
        Box(
            Modifier.size(44.dp)
                .clip(TrawlShape.Tile)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onGo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_download_trawl),
                contentDescription = stringResource(R.string.download),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/** `.fastpill` -- 26dp tall, outlined when off, filled with primary when on. */
@Composable
private fun FastPill(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg =
        if (enabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier.height(26.dp)
                .clip(TrawlShape.Pill)
                .background(bg)
                .then(
                    if (enabled) Modifier
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, TrawlShape.Pill)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bolt_trawl),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.fast),
            fontSize = 10.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 0.06.em,
            color = fg,
        )
    }
}

/**
 * `.fasttray` -- the one-tap quality row.
 *
 * It tucks UNDER the URL bar by -20dp with only its bottom corners rounded, so the two read as
 * one object with a drawer pulled out of it rather than two stacked cards. That negative offset
 * plus 27dp of top padding is exactly how the mockup hides the seam.
 */
@Composable
fun TrawlFastTray(
    visible: Boolean,
    qualities: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = Modifier.offset(y = (-20).dp),
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                    )
                    .padding(start = 13.dp, end = 13.dp, top = 27.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.one_tap_remembered),
                fontSize = 10.sp,
                letterSpacing = 0.09.em,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                qualities.forEach { q ->
                    QualityChip(label = q, selected = q == selected) { onSelect(q) }
                }
                QualityChip(
                    label = stringResource(R.string.more_ellipsis),
                    selected = false,
                    ghost = true,
                    onClick = onMore,
                )
            }
        }
    }
}

/** `.qchip` -- radius 9, outlined; `.on` fills with primary, `.ghost` dashes its border. */
@Composable
fun QualityChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.W600,
        color = fg,
        maxLines = 1,
        modifier =
            modifier
                .clip(RoundedCornerShape(9.dp))
                .background(bg)
                .then(
                    if (selected) Modifier
                    else
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                                .copy(alpha = if (ghost) 0.6f else 1f),
                            RoundedCornerShape(9.dp),
                        )
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

/**
 * `.qstrip` -- the four tools as ONE surface with four labelled cells.
 *
 * Labels are not optional here. The inherited version was icon-only, which turned four distinct
 * capabilities into four glyphs nobody could tell apart without tapping them.
 */
@Composable
fun TrawlToolStrip(cells: List<Triple<Painter, String, () -> Unit>>, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .trawlGlass(TrawlShape.ToolStrip)
                .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { (painter, label, onClick) ->
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** `.sechead` -- an uppercase tracked label with an optional trailing action. */
@Composable
fun TrawlSectionHead(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.075.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.02.em,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/**
 * `.endfish` -- the end-of-list marker.
 *
 * It exists so a short list ends deliberately instead of just stopping, which is the difference
 * between "that is everything" and "did it fail to load the rest?".
 */
@Composable
fun TrawlEndFish(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fish),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp).alpha(0.75f),
        )
        Text(
            text = stringResource(R.string.whole_catch).uppercase(),
            fontSize = 10.5.sp,
            letterSpacing = 0.14.em,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.75f),
        )
    }
}

/**
 * The URL bar and its fast tray as one unit.
 *
 * The tray only appears when the fast path is on AND the field is empty: once someone has typed
 * a link they have already chosen the slower, more deliberate route through the configure sheet,
 * and a row of one-tap qualities underneath would be offering to discard what they just typed.
 *
 * Typed text renders in a MONOSPACE face at 12.5sp, per `.urlbar .ph.filled`. That is not
 * decoration -- URLs are strings people proofread, and a proportional font makes `l`/`1`/`I` and
 * `0`/`O` ambiguous exactly where a typo costs a failed download.
 */
@Composable
fun TrawlUrlSection(
    value: String,
    onValueChange: (String) -> Unit,
    fastEnabled: Boolean,
    onToggleFast: () -> Unit,
    onPaste: () -> Unit,
    onGo: () -> Unit,
    qualities: List<String>,
    rememberedQuality: String,
    onQuickDownload: (String) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        TrawlUrlBar(
            // Painted above the tray. The tray tucks 20dp up underneath it, so without this the
            // later sibling wins and the tray's edge cuts across the pill and the go button.
            modifier = Modifier.zIndex(1f),
            value = value,
            onValueChange = onValueChange,
            fastEnabled = fastEnabled,
            onToggleFast = onToggleFast,
            onPaste = onPaste,
            onGo = onGo,
        ) { fieldModifier ->
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = FontFamily.Monospace,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = fieldModifier,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.paste_a_link_to_start),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight(450),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
        }
        TrawlFastTray(
            visible = fastEnabled && value.isEmpty(),
            qualities = qualities,
            selected = rememberedQuality,
            onSelect = onQuickDownload,
            onMore = onMore,
        )
    }
}
