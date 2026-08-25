package com.junkfood.seal.ui.page.links

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The links history screen, from design/v0.1.0-baseline-mockup-ui.html (.searchbar, .filters,
// .lrow / .lmeta / .lt / .lu / .lact, and the day grouping).
//
// WHY THIS SCREEN EXISTS. Upstream's history is a list of FILES: delete the file and the entry
// is gone with it. But the durable thing about a download is the LINK -- it is what you re-run,
// what you send someone, and what still means something after the file is deleted to save space.
// So this screen keeps the link and reports the file's status against it.

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.common.LocalShowMascot
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.home.QualityChip
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.util.DatabaseUtil
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.em
import com.junkfood.seal.ui.common.AsyncImageImpl

/** What became of the file this link produced. */
enum class LinkStatus {
    /** The recorded path still resolves to a file on disk. */
    SAVED,

    /** Downloaded once; the file is no longer where it was recorded. */
    MISSING,
}

/** Bucket for rows with no usable timestamp. */
private const val EARLIER = "Earlier"

/**
 * A history row plus the two facts the database does not carry: whether the file is still there,
 * and when it was actually downloaded.
 *
 * [dateMillis] is the file's mtime, because the entity has no download-date column --
 * `downloadTimeMillis` is a DURATION, and using it as a date is what produced "1 January 1970".
 * Zero when the file is gone and the date is therefore unknowable.
 */
private data class LinkEntry(
    val info: DownloadedVideoInfo,
    val status: LinkStatus,
    val dateMillis: Long,
)

enum class LinkFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    SAVED(R.string.filter_saved),
    MISSING(R.string.filter_missing),
}

/**
 * Every link Trawl has downloaded, with what happened to the file.
 *
 * The status check is real filesystem I/O across the whole history, so it runs once on
 * [Dispatchers.IO] in a [produceState] rather than per row during composition -- `File.exists()`
 * inside a `LazyColumn` item would hit the disk on the main thread on every scroll frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksHistoryPage(onNavigateBack: () -> Unit, onRedownload: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LinkFilter.ALL) }

    val entries by
        produceState(initialValue = emptyList<LinkEntry>()) {
            val history = DatabaseUtil.getVisibleDownloadHistoryFlow().first()
            value =
                withContext(Dispatchers.IO) {
                    history.map { info ->
                        val file = info.videoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                        val exists = runCatching { file?.exists() == true }.getOrDefault(false)
                        LinkEntry(
                            info = info,
                            status = if (exists) LinkStatus.SAVED else LinkStatus.MISSING,
                            // Already on an IO dispatcher and already stat-ing this file, so the
                            // mtime is free here and a main-thread hit anywhere else.
                            dateMillis =
                                if (exists) runCatching { file!!.lastModified() }.getOrDefault(0L)
                                else 0L,
                        )
                    }
                }
        }

    val visible =
        remember(entries, query, filter) {
            entries.filter { e ->
                val matchesFilter =
                    when (filter) {
                        LinkFilter.ALL -> true
                        LinkFilter.SAVED -> e.status == LinkStatus.SAVED
                        LinkFilter.MISSING -> e.status == LinkStatus.MISSING
                    }
                val q = query.trim()
                val matchesQuery =
                    q.isEmpty() ||
                        e.info.videoTitle.contains(q, ignoreCase = true) ||
                        e.info.videoAuthor.contains(q, ignoreCase = true) ||
                        e.info.videoUrl.contains(q, ignoreCase = true)
                matchesFilter && matchesQuery
            }
        }

    // Grouped by calendar day, newest first, so a long history reads as a timeline instead of an
    // undifferentiated wall.
    val grouped =
        remember(visible) {
            visible
                // By row id: autoincrement, so it is insertion order, and it stays correct for
                // rows whose file has since been deleted. Sorting by a date derived from a
                // missing file cannot.
                .sortedByDescending { it.info.id }
                .groupBy { dayKey(it.dateMillis) ?: EARLIER }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.links_history)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchBar(query = query, onQueryChange = { query = it })
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                LinkFilter.entries.forEach { f ->
                    QualityChip(label = stringResource(f.labelRes), selected = f == filter) {
                        filter = f
                    }
                }
            }

            if (visible.isEmpty()) {
                EmptyLinks(hasHistory = entries.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    grouped.forEach { (day, rows) ->
                        item(key = "h-$day") { DayHeader(day) }
                        items(rows, key = { it.info.id }) { entry ->
                            LinkRow(entry) { onRedownload(entry.info.videoUrl) }
                        }
                    }
                }
            }
        }
    }
}

/** `.searchbar` -- radius 16, outlined, with a leading magnifier. */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
            cursorBrush =
                androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_links_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun DayHeader(day: String) {
    Text(
        text = day.uppercase(),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.W700,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

/**
 * `.lrow` -- thumbnail, title, source badge, URL and status, with a re-download action.
 *
 * The thumbnail prefers the LOCAL FILE over the remote URL: for a saved download the video is
 * already on disk, so Coil can pull a frame from it without a network round trip, and it still
 * works offline. A missing file falls back to the stored thumbnail URL, and only if both are
 * absent does the mark stand in -- an empty grey square says "broken", the mark says "no
 * preview".
 */
@Composable
private fun LinkRow(entry: LinkEntry, onRedownload: () -> Unit) {
    val tokens = LocalTrawlTokens.current
    val scheme = MaterialTheme.colorScheme
    // Stored thumbnail FIRST. It is a small image the format sheet has usually already cached,
    // where the file path means decoding a frame out of a video that may be hundreds of
    // megabytes. The original order preferred the file and so never reached this at all.
    val model =
        remember(entry) {
            when {
                entry.info.thumbnailUrl.isNotBlank() -> entry.info.thumbnailUrl
                entry.status == LinkStatus.SAVED && entry.info.videoPath.isNotBlank() ->
                    File(entry.info.videoPath)
                else -> null
            }
        }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.outline, RoundedCornerShape(18.dp))
                .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(64.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (model != null) {
                AsyncImageImpl(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.trawl_mark),
                    contentDescription = null,
                    tint = scheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.size(26.dp),
                )
            }
            // A missing file still shows its thumbnail, dimmed, so the row stays recognisable
            // instead of becoming an anonymous grey block the moment you free up space.
            if (entry.status == LinkStatus.MISSING) {
                Box(Modifier.fillMaxSize().background(scheme.surface.copy(alpha = 0.55f)))
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = entry.info.videoTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight(550),
                lineHeight = 17.5.sp,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                SourceBadge(entry.info.videoUrl)
                StatusPill(entry.status, tokens.ok, tokens.warn)
            }
        }

        // .lact -- one tap re-runs the link at the remembered quality.
        Box(
            Modifier.size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tokens.surfaceHigh)
                .clickable(onClick = onRedownload),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.redownload),
                tint = scheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Where the link came from, as a word.
 *
 * "tiktok" is instantly readable; `https://www.tiktok.com/@toscaa.fgl/v...` is not, and it is
 * what the row used to lead with.
 */
@Composable
private fun SourceBadge(url: String) {
    val host =
        remember(url) {
            runCatching { java.net.URI(url).host.orEmpty() }
                .getOrDefault("")
                .removePrefix("www.")
                .removePrefix("m.")
                .substringBefore('.')
                .ifBlank { "link" }
        }
    Text(
        text = host.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.06.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusPill(status: LinkStatus, ok: Color, warn: Color) {
    val (label, color) =
        when (status) {
            LinkStatus.SAVED -> stringResource(R.string.filter_saved) to ok
            LinkStatus.MISSING -> stringResource(R.string.filter_missing) to warn
        }
    Text(
        text = label.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.W700,
        color = color,
        modifier =
            Modifier.clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Two different empty states, because they mean different things: nothing downloaded yet, versus
 * a filter that excludes everything. Showing "no links yet" to someone with 200 links and the
 * Missing filter on would be a lie.
 */
@Composable
private fun EmptyLinks(hasHistory: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (LocalShowMascot.current) {
            Icon(
                painter = painterResource(R.drawable.ic_fish),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp).alpha(0.55f),
            )
        }
        Text(
            text =
                if (hasHistory) stringResource(R.string.links_no_matches)
                else stringResource(R.string.links_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/**
 * Today / Yesterday / a date -- or nothing at all.
 *
 * Fed the file's mtime, not `downloadTimeMillis` -- that column is a DURATION, and handing a
 * duration to `Date(...)` puts every row in January 1970. Rows whose file is gone have no
 * knowable date and group under "Earlier" rather than claiming one.
 */
private fun dayKey(millis: Long): String? {
    if (millis <= 0L) return null
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay(cal, today) -> "Today"
        sameDay(cal, yesterday) -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }
}
