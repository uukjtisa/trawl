package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The bubble itself, from design/v0.1.0-baseline-mockup-ui.html (.bubble / .bcore / .ring /
// .dropx and the panel below them).
//
// One conic ring per running download, up to four, then a count. Accent while running, green on
// completion, red with a pulse on error -- so a glance at a 60dp circle answers "is it fine?"
// without opening anything.

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.ui.theme.SealTheme

/** What the bubble is currently saying, in priority order. */
enum class BubbleState {
    RUNNING,
    DONE,
    ERROR,
}

/** One task's contribution to the rings. */
data class BubbleTask(val id: String, val progress: Float, val error: Boolean)

/** The design's cap: beyond four rings the circle stops being readable and shows a count. */
private const val MAX_RINGS = 4

@Composable
fun BubbleOverlay(
    onMove: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
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
        var dragging by remember { mutableStateOf(false) }

        val state =
            when {
                tasks.any { it.error } -> BubbleState.ERROR
                tasks.isNotEmpty() && tasks.all { it.progress >= 1f } -> BubbleState.DONE
                else -> BubbleState.RUNNING
            }

        val ringColor =
            when (state) {
                BubbleState.RUNNING -> scheme.primary
                BubbleState.DONE -> tokens.ok
                BubbleState.ERROR -> tokens.bad
            }

        val scale by
            animateFloatAsState(
                targetValue = if (dragging) 1.08f else 1f,
                animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)),
                label = "bubbleGrab",
            )

        Box(
            modifier =
                Modifier.size(60.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, drag ->
                            change.consume()
                            onMove(drag.x, drag.y)
                        }
                    }
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
                    Modifier.size(if (tasks.isEmpty()) 60.dp else (60 - MAX_RINGS * 5).dp)
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
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}
