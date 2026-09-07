package com.junkfood.seal.ui.component

// NEW FILE (Trawl project, 2026-09-06). Not inherited from upstream.
//
// Draws the blocks ReleaseNotes.parse() produces. The parsing lives in util/ReleaseNotes.kt with
// no Compose in it, so the grammar can be tested on the JVM; this file is only paint.
//
// It deliberately does NOT look like GitHub. It reads in the app's own serif headings, warm
// palette and rounded surfaces, because it is the last screen a user sees before choosing to
// install an APK -- it should look like the app is talking, not like a web page leaked into it.

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.util.NoteBlock
import com.junkfood.seal.util.NoteRun

/**
 * The height of a screenshot strip.
 *
 * Fixed rather than derived from the images, because their real size is unknown until they load
 * over the network -- and a row that resizes when they arrive shoves the notes down mid-read.
 * Each image fits inside this box, so a landscape shot and a phone screenshot both behave.
 */
private val ImageStripHeight = 208.dp

@Composable
fun NoteBlockView(block: NoteBlock, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    when (block) {
        is NoteBlock.Heading -> {
            val big = block.level <= 2
            Row(
                modifier = modifier.fillMaxWidth().padding(top = if (big) 20.dp else 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (big) {
                    // The same accent dot the section labels use elsewhere in the app, so a
                    // release note reads as part of the app rather than as imported text.
                    Box(
                        Modifier.padding(end = 9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(scheme.primary)
                            .width(5.dp)
                            .height(5.dp)
                    )
                }
                Text(
                    text = block.spans.annotated(),
                    fontFamily = if (big) FrauncesFamily else FontFamily.Default,
                    fontSize = if (big) 18.sp else 14.5.sp,
                    fontWeight = FontWeight.W700,
                    color = scheme.onSurface,
                )
            }
        }

        is NoteBlock.Paragraph ->
            Text(
                text = block.spans.annotated(),
                modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
            )

        is NoteBlock.Bullet ->
            Row(modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
                Text(
                    text = if (block.marker == "-") "•" else block.marker,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = scheme.primary,
                    modifier = Modifier.width(if (block.marker == "-") 16.dp else 24.dp),
                )
                Text(
                    text = block.spans.annotated(),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = scheme.onSurfaceVariant,
                )
            }

        is NoteBlock.Quote ->
            Row(
                modifier =
                    modifier.fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHigh)
                        .height(IntrinsicSize.Min)
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(scheme.primary))
                Text(
                    text = block.spans.annotated(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = scheme.onSurfaceVariant,
                )
            }

        is NoteBlock.CodeBlock ->
            Box(
                modifier =
                    modifier.fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHighest)
                        .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = block.text,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = scheme.onSurface,
                )
            }

        is NoteBlock.ImageRow ->
            Row(
                modifier = modifier.fillMaxWidth().padding(vertical = 10.dp).height(ImageStripHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.images.forEach { image ->
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                // Painted so the strip has its shape before anything downloads,
                                // instead of three holes that fill in one at a time.
                                .background(scheme.surfaceContainerHigh)
                    ) {
                        AsyncImageImpl(
                            model = image.url,
                            contentDescription = image.alt.ifBlank { null },
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

        is NoteBlock.Table ->
            Column(
                modifier =
                    modifier.fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainer)
            ) {
                TableRow(cells = block.header, header = true)
                block.rows.forEach { row ->
                    HorizontalDivider(color = scheme.outline.copy(alpha = 0.18f))
                    TableRow(cells = row, header = false)
                }
            }

        NoteBlock.Rule ->
            HorizontalDivider(
                modifier = modifier.padding(vertical = 16.dp),
                color = scheme.outline.copy(alpha = 0.22f),
            )
    }
}

@Composable
private fun TableRow(cells: List<List<NoteRun>>, header: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(if (header) scheme.surfaceContainerHighest else scheme.surfaceContainer)
    ) {
        cells.forEach { cell ->
            Text(
                text = cell.annotated(),
                modifier = Modifier.weight(1f).padding(horizontal = 11.dp, vertical = 9.dp),
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                fontWeight = if (header) FontWeight.W700 else FontWeight.Normal,
                color = if (header) scheme.onSurface else scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Fold styled runs into one AnnotatedString.
 *
 * Links go through LinkAnnotation rather than a click listener, which is what makes a Text() with
 * a link in it actually open the link and announce itself as one to a screen reader.
 */
@Composable
private fun List<NoteRun>.annotated(): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    val linkStyles =
        TextLinkStyles(
            style = SpanStyle(color = scheme.primary, textDecoration = TextDecoration.Underline)
        )
    return buildAnnotatedString {
        this@annotated.forEach { run ->
            val style =
                SpanStyle(
                    fontWeight = if (run.bold) FontWeight.W700 else null,
                    fontStyle = if (run.italic) FontStyle.Italic else null,
                    fontFamily = if (run.code) FontFamily.Monospace else null,
                    fontSize = if (run.code) 12.5.sp else TextUnit.Unspecified,
                    color = if (run.code) scheme.onSurface else Color.Unspecified,
                    background =
                        if (run.code) scheme.surfaceContainerHighest else Color.Unspecified,
                )
            val link = run.link
            if (link != null) {
                withLink(LinkAnnotation.Url(url = link, styles = linkStyles)) {
                    withStyle(style) { append(run.text) }
                }
            } else {
                withStyle(style) { append(run.text) }
            }
        }
    }
}

/** Convenience for callers that just want the whole body drawn. */
@Composable
fun ReleaseNotesColumn(blocks: List<NoteBlock>, modifier: Modifier = Modifier) {
    Column(modifier) {
        blocks.forEach { NoteBlockView(it) }
        Spacer(Modifier.height(4.dp))
    }
}
