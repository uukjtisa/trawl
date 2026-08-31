package com.junkfood.seal.ui.page.downloadv2.configure

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// The format screen for a route one of Trawl's own resolvers claims.
//
// WHY THIS IS NOT yt-dlp's FORMAT PAGE. The two describe genuinely different things. yt-dlp
// publishes a ladder of STREAMS to be combined -- pick a video rung, pick an audio rung, let it
// merge them. A resolver publishes whole FILES: TikTok's two renders are complete videos, X's
// ladder is complete videos, and a direct link is one file with nothing to choose at all.
//
// Feeding the second through the first is exactly what produced the audio dead end. Asked for
// audio, yt-dlp probed the resolver's progressive MP4, found no separate audio stream in it --
// because there is none, the audio is muxed in -- and presented an empty list with no way
// forward. The screen was answering a question the route cannot be asked.
//
// So this screen asks the two questions the route CAN answer:
//
//   SOURCE      which of the platform's renditions to pull from. Real byte counts, measured by
//               the resolver with a ranged GET rather than estimated.
//   CONVERT TO  for audio, which container to end up in. One ffmpeg flag, four options.
//
// For video the second section is absent, because there is nothing to convert to.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.util.CONVERT_M4A
import com.junkfood.seal.util.CONVERT_MP3
import com.junkfood.seal.util.CONVERT_OGG
import com.junkfood.seal.util.CONVERT_OPUS
import com.junkfood.seal.util.DirectPreset
import com.junkfood.seal.util.DirectPresets
import com.junkfood.seal.util.DirectResolution
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.QualityRung
import com.junkfood.seal.util.toFileSizeText

private val CONTAINERS =
    listOf(CONVERT_M4A to "M4A", CONVERT_MP3 to "MP3", CONVERT_OPUS to "Opus", CONVERT_OGG to "Ogg")

/**
 * One rendition, described by what it actually is rather than by a format id nobody chose.
 *
 * The size is the resolver's measurement, not an estimate: every shipped resolver takes the byte
 * count from a one-byte ranged GET, so a blank here means the CDN would not say, which is honest.
 */
@Composable
private fun SourceRow(format: Format, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = format.formatNote?.takeIf { it.isNotBlank() } ?: format.formatId.orEmpty(),
                fontWeight = FontWeight.W600,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val size = format.fileSize ?: format.fileSizeApprox
            Text(
                text =
                    buildString {
                        append(format.ext?.uppercase() ?: "MP4")
                        if (size != null && size > 0) append("  ·  ${size.toLong().toFileSizeText()}")
                    },
                fontSize = 12.5.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectFormatPage(
    state: DownloadDialogViewModel.SelectionState.DirectFormatSelection,
    onDismissRequest: () -> Unit,
    onDownload: (format: Format, container: Int) -> Unit,
) {
    val resolution: DirectResolution = state.resolution
    val scheme = MaterialTheme.colorScheme

    // Seeded from the platform's saved preset, then owned by the composition. Reading the
    // preference back per frame does not work -- MMKV is not observable, which is the same trap
    // the old container chips fell into.
    val saved = remember(resolution.platform, state.audio) {
        DirectPresets.get(resolution.platform, state.audio)
    }
    var selected by remember(resolution) {
        mutableStateOf(saved.rung.pick(resolution.formats) ?: resolution.formats.first())
    }
    var container by remember(resolution) { mutableIntStateOf(saved.container) }
    var showPreset by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                // Painted explicitly. The M2 sheet variant this sits in does not supply a surface
                // of its own, so without this the screen behind it reads straight through and the
                // whole sheet looks like a rendering fault.
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = resolution.title,
            fontWeight = FontWeight.W700,
            fontSize = 19.sp,
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.route_direct_chip, resolution.platform),
            fontSize = 12.5.sp,
            color = scheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )

        SectionLabel(text = stringResource(R.string.direct_source))
        resolution.formats.forEach { f ->
            SourceRow(format = f, selected = f.formatId == selected.formatId) { selected = f }
        }

        if (state.audio) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(text = stringResource(R.string.direct_convert_to))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CONTAINERS.forEach { (code, label) ->
                    FilterChip(
                        selected = container == code,
                        onClick = { container = code },
                        label = { Text(label) },
                    )
                }
            }
            // M4A off an MP4 source is a remux rather than a re-encode, so it is both faster and
            // lossless. Worth saying, because "MP3" looks like the safe default and is not.
            if (container == CONVERT_M4A) {
                Text(
                    text = stringResource(R.string.direct_m4a_note),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPreset = true }
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text =
                    stringResource(
                        R.string.direct_edit_preset,
                        resolution.platform,
                        stringResource(if (state.audio) R.string.audio else R.string.video),
                    ),
                fontSize = 13.sp,
                color = scheme.primary,
            )
        }

        Spacer(Modifier.height(6.dp))
        ActionButtonsRow(
            onCancel = onDismissRequest,
            onConfirm = { onDownload(selected, container) },
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showPreset) {
        DirectPresetDialog(
            platform = resolution.platform,
            audio = state.audio,
            initial = DirectPreset(saved.rung, container),
            ladderSize = resolution.formats.size,
            onDismissRequest = { showPreset = false },
            onSave = { preset ->
                DirectPresets.set(resolution.platform, state.audio, preset)
                preset.rung.pick(resolution.formats)?.let { selected = it }
                container = preset.container
                showPreset = false
            },
        )
    }
}

@Composable
private fun ActionButtonsRow(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            Modifier.weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.weight(2f)
                .height(52.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.download),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

/**
 * The per-platform, per-type preset editor.
 *
 * [ladderSize] is shown back to the reader because it is the whole reason the middle rungs are
 * named by direction rather than by position: over a two-rung ladder "mid-high" IS best, and the
 * only honest thing to do is say which way the setting leans when it cannot land where asked.
 */
@Composable
private fun DirectPresetDialog(
    platform: String,
    audio: Boolean,
    initial: DirectPreset,
    ladderSize: Int,
    onDismissRequest: () -> Unit,
    onSave: (DirectPreset) -> Unit,
) {
    var rung by remember { mutableStateOf(initial.rung) }
    var container by remember { mutableIntStateOf(initial.container) }
    val scheme = MaterialTheme.colorScheme

    com.junkfood.seal.ui.component.SealDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                stringResource(
                    R.string.direct_edit_preset,
                    platform,
                    stringResource(if (audio) R.string.audio else R.string.video),
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.direct_preset_explainer, ladderSize),
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                QualityRung.entries.forEach { r ->
                    val lands = r.indexIn(ladderSize) + 1
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { rung = r }
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(rung.labelRes(r)),
                                fontWeight = FontWeight.W600,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.direct_preset_lands, lands, ladderSize),
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (rung == r) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
                if (audio) {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel(text = stringResource(R.string.direct_convert_to))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CONTAINERS.forEach { (code, label) ->
                            FilterChip(
                                selected = container == code,
                                onClick = { container = code },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.junkfood.seal.ui.component.ConfirmButton {
                onSave(DirectPreset(rung, container))
            }
        },
        dismissButton = { com.junkfood.seal.ui.component.DismissButton { onDismissRequest() } },
    )
}

/** The four rungs, named for humans. */
private fun QualityRung.labelRes(r: QualityRung): Int =
    when (r) {
        QualityRung.BEST -> R.string.rung_best
        QualityRung.MID_HIGH -> R.string.rung_mid_high
        QualityRung.MID_LOW -> R.string.rung_mid_low
        QualityRung.LOWEST -> R.string.rung_lowest
    }
