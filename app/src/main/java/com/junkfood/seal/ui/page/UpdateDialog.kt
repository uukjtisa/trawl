package com.junkfood.seal.ui.page

// MODIFIED (Trawl project, 2026-09-06). Upstream file, rewritten.
// GPL-3.0 s5(a): the release-note rendering, the layout and the metadata row are new work; the
// download/install wiring is upstream's and is unchanged in behaviour.
//
// The old dialog was an AlertDialog whose body was `Text(release.body)`. A GitHub release body is
// Markdown with HTML in it, so the reader got `<div align="center">`, three raw <img> tags, `##`
// and `**` as literal characters. Parsing lives in util/ReleaseNotes.kt and painting in
// component/ReleaseNotesView.kt; this file is the container and the metadata around it.

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.NoteBlockView
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.util.ReleaseNotes
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.toFileSizeText
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(onDismissRequest: () -> Unit, release: UpdateUtil.Release) {
    var currentDownloadStatus by remember {
        mutableStateOf(UpdateUtil.DownloadStatus.NotYet as UpdateUtil.DownloadStatus)
    }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = release.name.orEmpty().ifBlank { release.tagName.orEmpty() },
        subtitle = release.metaLine(),
        onConfirmUpdate = {
            scope.launch(Dispatchers.IO) {
                runCatching {
                        UpdateUtil.downloadApk(release = release).collect { downloadStatus ->
                            currentDownloadStatus = downloadStatus
                            if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    UpdateUtil.installLatestApk()
                                }
                            }
                        }
                    }
                    .onFailure {
                        it.printStackTrace()
                        currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                        App.applicationScope.launch(Dispatchers.Main) {
                            context.makeToast(R.string.app_update_failed)
                        }
                        return@launch
                    }
            }
        },
        releaseNote = release.body.orEmpty(),
        downloadStatus = currentDownloadStatus,
    )
}

/**
 * "6 Sep 2026 · 88.4 MB" — whichever APK this device will actually be given.
 *
 * The asset is chosen the same way [UpdateUtil.downloadApk] chooses it: the first supported ABI,
 * falling back to a universal build. Showing the size of a different asset than the one that
 * downloads would be worse than showing no size at all.
 */
@Composable
private fun UpdateUtil.Release.metaLine(): String {
    val abi = Build.SUPPORTED_ABIS.firstOrNull()
    val asset =
        assets?.firstOrNull { a -> abi != null && a.name?.contains(abi, ignoreCase = true) == true }
            ?: assets?.firstOrNull { a ->
                a.name?.contains("universal", ignoreCase = true) == true ||
                    a.name?.endsWith(".apk", ignoreCase = true) == true
            }

    val size = asset?.size?.toFileSizeText()
    val date = publishedAt?.let { formatIsoDate(it) }
    return listOfNotNull(date, size).joinToString("  ·  ")
}

/**
 * GitHub sends `2026-09-06T13:12:00Z`.
 *
 * Parsed with SimpleDateFormat rather than java.time because minSdk is 24 and this module does not
 * enable core-library desugaring — java.time here would compile and then crash on older phones,
 * which is the worst of both.
 */
private fun formatIsoDate(iso: String): String? =
    runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = parser.parse(iso) ?: return null
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed)
        }
        .getOrNull()

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String = "",
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
    downloadStatus: UpdateUtil.DownloadStatus,
) {
    val scheme = MaterialTheme.colorScheme
    // Parsing is pure and depends only on the string, so it is cached rather than re-run on every
    // recomposition -- and the download progress recomposes this dialog many times a second.
    val blocks = remember(releaseNote) { ReleaseNotes.parse(releaseNote) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val downloading = downloadStatus is UpdateUtil.DownloadStatus.Progress

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier.padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.86f),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {

                // ---------------------------------------------------------------- header
                Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.trawl_mark),
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.update_available).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = 0.09.em,
                            color = scheme.primary,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = title,
                        fontFamily = FrauncesFamily,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.W700,
                        color = scheme.onSurface,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
                HorizontalDivider(color = scheme.outline.copy(alpha = 0.18f))

                // ----------------------------------------------------------------- notes
                // Lazy because a release body runs to dozens of blocks and this dialog is
                // recomposed on every progress tick while the APK downloads.
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 22.dp,
                            end = 22.dp,
                            top = 6.dp,
                            bottom = 14.dp,
                        ),
                ) {
                    items(blocks) { block -> NoteBlockView(block) }
                }

                HorizontalDivider(color = scheme.outline.copy(alpha = 0.18f))

                // --------------------------------------------------------------- actions
                AnimatedVisibility(visible = downloading) {
                    LinearProgressIndicator(
                        progress = {
                            ((downloadStatus as? UpdateUtil.DownloadStatus.Progress)?.percent ?: 0) /
                                100f
                        },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.dismiss), color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (!downloading) onConfirmUpdate() },
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text =
                                when (downloadStatus) {
                                    is UpdateUtil.DownloadStatus.Progress ->
                                        "${downloadStatus.percent} %"
                                    else -> stringResource(R.string.update)
                                },
                            fontWeight = FontWeight.W600,
                            modifier = Modifier.animateContentSize(),
                        )
                    }
                }
            }
        }
    }
}

/** Kept so the dialog can be exercised without a network round trip. */
@Composable
private fun UpdateDialogPreviewBody() {
    Box {
        UpdateDialogImpl(
            onDismissRequest = {},
            title = "Trawl v0.1.1",
            subtitle = "6 Sep 2026  ·  88.4 MB",
            onConfirmUpdate = {},
            releaseNote =
                """
                ## What this release is

                The first one shaped by **using** it — every fix came from something going
                wrong in ordinary use.

                - Follows **HTTP redirects** and `<meta refresh>`
                - Verifies the container signature before saving

                > Install one build or the other, not both.

                See [docs/STATUS.md](https://example.com) for the honest list.
                """
                    .trimIndent(),
            downloadStatus = UpdateUtil.DownloadStatus.NotYet,
        )
    }
}
