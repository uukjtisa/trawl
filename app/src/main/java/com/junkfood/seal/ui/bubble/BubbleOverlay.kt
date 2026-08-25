package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The bubble, its panel and its drop target, from design/v0.1.0-baseline-mockup-ui.html
// (.bubble / .bcore / .ring, .bpanel / .bh / .bpaste / .btask / .bfoot, .dropx).
//
// WHY THREE COMPOSABLES AND NOT ONE. Each of these is a SEPARATE OVERLAY WINDOW (see
// BubbleService). They cannot be one composable in one window, because a window big enough to
// hold a panel anchored anywhere on screen is a window covering the whole screen -- and a
// full-screen ComposeView swallows every touch meant for the app underneath it. So the bubble
// stays a 60dp window you can drag, the panel is its own wrap-content window that only exists
// while it is open, and the X is a third, deliberately untouchable one.
//
// The first cut of this file had only detectDragGestures. It moved and it drew, and that was
// all -- which is exactly what he saw: "the flaoting window aint een clickable its liek a
// design".

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.ui.theme.SealTheme
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.CLIPBOARD_AUTOPASTE

/** What the bubble is currently saying, in priority order. */
enum class BubbleState {
    RUNNING,
    DONE,
    ERROR,
}

/** The design's cap: beyond four rings the circle stops being readable and shows a count. */
private const val MAX_RINGS = 4

/** Panel width from the contract (`.bpanel{width:300px}`), needed by the service to place it. */
const val BUBBLE_PANEL_WIDTH_DP = 300

/** The bubble's own diameter, likewise needed for placement maths. */
const val BUBBLE_SIZE_DP = 60

/**
 * The two facts the bubble's window and the X's window both need to agree on.
 *
 * They are different windows with different compositions, so this is held by the Service and
 * handed to both. Snapshot state, so a change in the bubble's gesture loop recomposes the X.
 */
class BubbleUi {
    var dragging by mutableStateOf(false)
    var hot by mutableStateOf(false)
}

// ── the bubble ───────────────────────────────────────────────────────────────────────────────

@Composable
fun BubbleOverlay(
    ui: BubbleUi,
    onMove: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
) {
    // Collected rather than passed: the overlay is composed by the Service, which has no access
    // to the Activity's state.
    val tasks by BubbleTasks.tasks.collectAsState()
    // The overlay is its own window, outside the activity's composition, so it must establish
    // the theme itself -- otherwise it renders with Material defaults against the user's chosen
    // palette and looks like a different app's widget.
    SealTheme {
        val tokens = LocalTrawlTokens.current
        val scheme = MaterialTheme.colorScheme
        val live = tasks.filterNot { it.finished }

        val state =
            when {
                tasks.any { it.error } -> BubbleState.ERROR
                tasks.isNotEmpty() && live.isEmpty() -> BubbleState.DONE
                else -> BubbleState.RUNNING
            }

        val ringColor =
            when (state) {
                BubbleState.RUNNING -> scheme.primary
                BubbleState.DONE -> tokens.ok
                BubbleState.ERROR -> tokens.bad
            }

        // Grabbing swells it; hovering the X shrinks it, so the bubble reads as being about to
        // go down the drain rather than as merely overlapping a red circle.
        val scale by
            animateFloatAsState(
                targetValue =
                    when {
                        ui.hot -> 0.82f
                        ui.dragging -> 1.08f
                        else -> 1f
                    },
                animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)),
                label = "bubbleGrab",
            )

        Box(
            modifier =
                Modifier.size(BUBBLE_SIZE_DP.dp)
                    .graphicsScale(scale)
                    .bubbleGestures(
                        onMove = onMove,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onTap = onTap,
                    )
                    .drawBehind {
                        // Rings sit outside the core, innermost first, 3dp thick with a 2dp gap.
                        val visible = tasks.take(MAX_RINGS)
                        visible.forEachIndexed { i, task ->
                            val inset = i * 5.dp.toPx()
                            val stroke = 3.dp.toPx()
                            val d = size.minDimension - inset * 2f - stroke
                            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                            // Track first, then the filled sweep, so an empty ring still reads
                            // as "a download exists" rather than as nothing at all.
                            drawArc(
                                color = Color.White.copy(alpha = 0.09f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(d, d),
                                style = Stroke(width = stroke),
                            )
                            drawArc(
                                color = if (task.error) tokens.bad else ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * task.progress.coerceIn(0f, 1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(d, d),
                                style = Stroke(width = stroke),
                            )
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier.size(
                            if (tasks.isEmpty()) BUBBLE_SIZE_DP.dp
                            else (BUBBLE_SIZE_DP - MAX_RINGS * 5).dp
                        )
                        .clip(CircleShape)
                        .background(scheme.surfaceContainer)
                        .border(1.dp, tokens.glassLine, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (tasks.size > MAX_RINGS) {
                    Text(
                        text = "+${tasks.size - MAX_RINGS}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W800,
                        color = scheme.onSurface,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.trawl_mark),
                        contentDescription = stringResource(R.string.floating_bubble),
                        tint = scheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}


/**
 * One gesture loop that tells a TAP from a DRAG.
 *
 * Two stacked detectors (`detectTapGestures` plus `detectDragGestures`) would be shorter, but
 * whether the tap detector survives a drag depends on which of them consumes the move first --
 * an ordering dependency that is invisible in the source and breaks silently. This is explicit:
 * one down, watch for slop, and on the up decide which of the two things happened.
 */
private fun Modifier.bubbleGestures(
    onMove: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            // DISTANCE travelled, not net displacement. The bubble's window follows the finger,
            // so the pointer's position within the view keeps snapping back toward where it
            // started -- a signed sum hovers around zero and the gesture never registers as a
            // drag, which is why dragging kept opening the panel instead of moving it.
            var travelled = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUp()) break
                val delta = change.positionChange()
                travelled += hypot(delta.x, delta.y)
                if (!moved && travelled > viewConfiguration.touchSlop) {
                    moved = true
                    onDragStart()
                }
                if (moved) {
                    change.consume()
                    onMove(delta.x, delta.y)
                }
            }
            if (moved) onDragEnd() else onTap()
        }
    }

// ── the drop target ──────────────────────────────────────────────────────────────────────────

/**
 * `.dropx` -- the X at bottom centre that appears while the bubble is being dragged.
 *
 * Purely a picture. Its window is FLAG_NOT_TOUCHABLE and the hit test is done by distance in the
 * Service, exactly as the mockup does it -- which also means the X can never eat a touch meant
 * for whatever app is underneath.
 */
@Composable
fun BubbleDropTarget(ui: BubbleUi) {
    SealTheme {
        val tokens = LocalTrawlTokens.current
        val scale by
            animateFloatAsState(
                targetValue =
                    when {
                        !ui.dragging -> 0.7f
                        ui.hot -> 1.28f
                        else -> 1f
                    },
                animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)),
                label = "dropxScale",
            )
        val alpha by
            animateFloatAsState(
                targetValue = if (ui.dragging) 1f else 0f,
                animationSpec = tween(200),
                label = "dropxAlpha",
            )
        Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier.size(58.dp)
                        .graphicsScale(scale, alpha)
                        .clip(CircleShape)
                        .background(tokens.bad.copy(alpha = if (ui.hot) 0.45f else 0.18f))
                        .border(1.dp, tokens.bad.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.bubble_dismiss),
                    tint = if (ui.hot) Color.White else tokens.bad,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ── the panel ────────────────────────────────────────────────────────────────────────────────

/**
 * `.bpanel` -- what a tap on the bubble opens.
 *
 * The rows carry the verb that fits the row's own state. A single "cancel" for everything would
 * be simpler and would also be the wrong control on three states out of four: you pause a
 * running download, you resume a paused one, and a failed one wants retrying, not deleting.
 */
@Composable
fun BubblePanel(
    onAction: (String, BubbleAction) -> Unit,
    onDownload: (String) -> Unit,
    onReadClipboard: () -> String,
    onInputFocus: (Boolean) -> Unit,
    onClose: () -> Unit,
    onHide: () -> Unit,
    onTurnOff: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var autoPaste by remember { mutableStateOf(CLIPBOARD_AUTOPASTE.getBoolean()) }

    // Opening the panel is the only moment the clipboard is legally readable -- since Android 10
    // an unfocused app gets null, and this window has just taken focus. The link is pasted, not
    // acted on: it lands where it was going anyway and the decision stays with the download
    // button. The small delay lets focus actually arrive.
    LaunchedEffect(autoPaste) {
        if (!autoPaste) return@LaunchedEffect
        delay(400)
        if (url.isEmpty()) {
            onReadClipboard().takeIf { it.isNotBlank() }?.let { url = it }
        }
    }
    val tasks by BubbleTasks.tasks.collectAsState()
    SealTheme {
        val tokens = LocalTrawlTokens.current
        val scheme = MaterialTheme.colorScheme
        val live = tasks.filterNot { it.finished }
        Column(
            modifier =
                Modifier.width(BUBBLE_PANEL_WIDTH_DP.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.surfaceContainer)
                    .border(1.dp, tokens.glassLine, RoundedCornerShape(22.dp))
                    .padding(13.dp)
        ) {
            // .bh
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trawl_mark),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text =
                        if (live.isEmpty()) stringResource(R.string.bubble_all_done)
                        else stringResource(R.string.bubble_downloading, live.size),
                    fontSize = 13.sp,
                    fontWeight = FontWeight(650),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // `.bpaste`, but a real launcher rather than a shortcut into the app. A link
            // pasted here starts a download from here; the only thing handed off is the
            // CONFIGURE step, which is QuickDownloadActivity's whole job and would be a bad
            // thing to reimplement inside a 300dp overlay.
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(scheme.surface)
                        .dashedOutline(scheme.outline, 13.dp)
                        .padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (url.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bubble_paste),
                            fontSize = 12.sp,
                            fontWeight = FontWeight(650),
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        textStyle =
                            LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight(650),
                                color = scheme.onSurface,
                            ),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                        keyboardActions = KeyboardActions(onGo = { onDownload(url) }),
                        modifier =
                            Modifier.fillMaxWidth().onFocusChanged {
                                onInputFocus(it.isFocused)
                            },
                    )
                }
                // Only present when there is something to clear: a permanently visible X on an
                // empty field is a control that does nothing most of the time.
                if (url.isNotEmpty()) {
                    Box(
                        Modifier.size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { url = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                // Reading the clipboard is legal here only because this window takes focus --
                // since Android 10 an unfocused app gets null, silently. That is exactly why
                // the first cut had to bounce to the Activity instead.
                Box(
                    Modifier.size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onReadClipboard().takeIf { it.isNotBlank() }?.let { url = it } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_paste_trawl),
                        contentDescription = stringResource(R.string.paste),
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Box(
                    Modifier.size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (url.isBlank()) tokens.surfaceHigh else scheme.primary
                        )
                        .clickable(enabled = url.isNotBlank()) { onDownload(url) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download_trawl),
                        contentDescription = stringResource(R.string.download),
                        tint =
                            if (url.isBlank()) scheme.onSurfaceVariant else scheme.onPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            if (live.isEmpty()) {
                // .bempty
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fish),
                        contentDescription = null,
                        tint = tokens.ok,
                        modifier = Modifier.size(60.dp),
                    )
                    Text(
                        text = stringResource(R.string.bubble_empty),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                // Scrolls rather than truncates. `take(4)` hid the rest of the queue, and letting
                // the column grow instead would push the footer -- "Hide bubble" and "Turn off" --
                // off the bottom of the screen, which is where the way out lives.
                Column(
                    modifier =
                        Modifier.heightIn(max = 208.dp)
                            .verticalScroll(rememberScrollState())
                ) {
                    live.forEach { task -> BubbleTaskRow(task, onAction) }
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable {
                            autoPaste = !autoPaste
                            PreferenceUtil.updateValue(CLIPBOARD_AUTOPASTE, autoPaste)
                        }
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_paste_trawl),
                    contentDescription = null,
                    tint = if (autoPaste) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.clipboard_autopaste),
                    fontSize = 11.sp,
                    fontWeight = FontWeight(550),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(if (autoPaste) R.string.state_on else R.string.state_off),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.06.em,
                    color = if (autoPaste) tokens.ok else scheme.onSurfaceVariant,
                )
            }

            // .bfoot -- "hide" is for this download, "turn off" is for good. Two different
            // intentions that a single X would collapse into one ambiguous gesture.
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 10.dp)
                        .drawTopRule(scheme.outline),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FootButton(
                    text = stringResource(R.string.bubble_hide),
                    color = scheme.onSurfaceVariant,
                    border = scheme.outline,
                    onClick = onHide,
                    modifier = Modifier.weight(1f),
                )
                FootButton(
                    text = stringResource(R.string.bubble_turn_off),
                    color = tokens.bad,
                    border = tokens.bad.copy(alpha = 0.35f),
                    onClick = onTurnOff,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** `.btask` -- name, status, a 3dp progress hairline, and the verb that fits its state. */
@Composable
private fun BubbleTaskRow(task: BubbleTask, onAction: (String, BubbleAction) -> Unit) {
    val tokens = LocalTrawlTokens.current
    val scheme = MaterialTheme.colorScheme
    val bad = task.state == BubbleTaskState.ERROR
    val (icon, action) =
        when (task.state) {
            BubbleTaskState.ERROR -> Icons.Rounded.Refresh to BubbleAction.RETRY
            BubbleTaskState.PAUSED -> Icons.Rounded.PlayArrow to BubbleAction.RESUME
            // Pausing something that has not started is a no-op dressed as a control; a queued
            // task's only meaningful verb is "drop it".
            BubbleTaskState.QUEUED -> Icons.Rounded.Close to BubbleAction.CANCEL
            else -> Icons.Rounded.Pause to BubbleAction.PAUSE
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title.ifBlank { stringResource(R.string.unknown) },
                fontSize = 12.sp,
                fontWeight = FontWeight(550),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = task.statusLine(),
                fontSize = 10.5.sp,
                fontWeight = if (bad) FontWeight.W600 else FontWeight.Normal,
                color = if (bad) tokens.bad else scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // .mini -- a failed row fills red rather than showing how far it got, because how
            // far it got is not the news.
            Box(
                Modifier.fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    Modifier.fillMaxWidth(
                            if (bad) 1f else task.progress.coerceIn(0f, 1f)
                        )
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (bad) tokens.bad else scheme.primary)
                )
            }
        }
        Box(
            Modifier.size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tokens.surfaceHigh)
                .clickable { onAction(task.id, action) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = action.name,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun BubbleTask.statusLine(): String =
    when (state) {
        BubbleTaskState.QUEUED -> stringResource(R.string.bubble_queued)
        BubbleTaskState.PAUSED -> stringResource(R.string.status_paused)
        BubbleTaskState.ERROR -> stringResource(R.string.bubble_failed)
        BubbleTaskState.DONE -> stringResource(R.string.completed)
        BubbleTaskState.RUNNING -> {
            val pct = "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%"
            if (detail.isBlank()) pct else "$pct · $detail"
        }
    }

@Composable
private fun FootButton(
    text: String,
    color: Color,
    border: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 11.5.sp, fontWeight = FontWeight.W600, color = color)
    }
}

// ── small drawing helpers ────────────────────────────────────────────────

/** Scale (and optionally fade) without disturbing layout -- these all sit in fixed-size windows. */
private fun Modifier.graphicsScale(scale: Float, alpha: Float = 1f): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
    this.alpha = alpha
}

/**
 * A dashed rounded outline.
 *
 * `Modifier.border` cannot dash -- it takes a Brush, not a PathEffect -- so the paste row's
 * "empty slot" border has to be drawn. Dashed rather than solid on purpose: it is the one row in
 * the panel that holds nothing yet.
 */
private fun Modifier.dashedOutline(color: Color, radius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style =
            Stroke(
                width = 1.dp.toPx(),
                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                        0f,
                    ),
            ),
    )
}

/** `.bfoot`'s separator. A top border on a Row would also draw down the sides. */
private fun Modifier.drawTopRule(color: Color): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx(),
    )
}
