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

/** What became of the file this link produced. */
enum class LinkStatus {
    /** The recorded path still resolves to a file on disk. */
    SAVED,

    /** Downloaded once; the file is no longer where it was recorded. */
    MISSING,
}

private data class LinkEntry(val info: DownloadedVideoInfo, val status: LinkStatus)

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
                        val exists =
                            info.videoPath.isNotBlank() && runCatching {
                                File(info.videoPath).exists()
                            }.getOrDefault(false)
                        LinkEntry(info, if (exists) LinkStatus.SAVED else LinkStatus.MISSING)
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
                .sortedByDescending { it.info.downloadTimeMillis }
                .groupBy { dayKey(it.info.downloadTimeMillis) }
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

/** `.lrow` -- thumbnail, two-line title, monospace URL, status pill, and a re-download action. */
@Composable
private fun LinkRow(entry: LinkEntry, onRedownload: () -> Unit) {
    val tokens = LocalTrawlTokens.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.info.videoTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight(550),
                lineHeight = 17.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.info.videoUrl,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            StatusPill(entry.status, tokens.ok, tokens.warn)
        }
        // .lact -- 38dp, radius 11. One tap re-runs the link at the remembered quality.
        Box(
            Modifier.size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onRedownload),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.redownload),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
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
            Modifier.padding(top = 6.dp)
                .clip(RoundedCornerShape(50))
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

/** Today / Yesterday / a formatted date, so recent entries read in human terms. */
private fun dayKey(millis: Long): String {
    if (millis <= 0L) return "—"
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
