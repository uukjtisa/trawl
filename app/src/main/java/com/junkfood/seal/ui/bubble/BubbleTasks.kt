package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
// CHANGED 2026-08-26: per-row dismissal, reconciliation against the app's own
// history, an active-only count, and an age-out for finished rows.
//
// The bubble lives in its own window, owned by a Service, with no access to the
// Activity's composition. So the app publishes what it is downloading here and the
// overlay collects it.
//
// A process-scoped StateFlow rather than a Binder or a broadcast: both sides are in the same
// process, the payload is a handful of fields per task, and the overlay needs the CURRENT value
// the moment it composes -- which is exactly what a StateFlow gives and a broadcast does not.
//
// WHY THIS FILE GREW A VOCABULARY FOR FORGETTING. The first version assigned the live list
// straight in, so every row vanished the instant it finished. The fix was to merge by id and
// keep what had been seen -- which overcorrected into a list that could never forget ANYTHING.
// Deleting a download in the app left its row here forever, and the badge counted it, which is
// how a cleared queue still read "+5". A cache needs both verbs, not one.

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What one row of the bubble's panel says.
 *
 * This grew from `(id, progress, error)` when the panel arrived. A ring only ever needed a
 * fraction and a colour; a ROW has to name the thing and say what is happening to it, and it has
 * to offer the right verb -- pause a running task, resume a paused one, retry a failed one. That
 * is not derivable from a float, so the state is published rather than guessed.
 */
data class BubbleTask(
    val id: String,
    val title: String,
    val progress: Float,
    val state: BubbleTaskState,
    /** Size / speed, already formatted by the publisher. Blank when there is nothing to say. */
    val detail: String = "",
    /**
     * Where the finished file landed, so a completed row can be tapped to play it.
     *
     * Null while the download is still running, and null for anything that failed. The row uses
     * its presence as the test for "is this tappable", which is stricter than checking DONE: a
     * task can complete without the app learning a usable path.
     */
    val filePath: String? = null,
    /**
     * The task's source URL.
     *
     * Carried purely so [BubbleTasks.reconcile] can ask the app whether this download still
     * exists. Rows are keyed by task id, but the app's history is keyed by URL and path -- a row
     * with no URL therefore cannot be matched against history, and is never auto-dropped.
     */
    val url: String = "",
    /**
     * When this row reached a terminal state, for [BubbleTasks.ageOut]. Zero while it is running.
     */
    val settledAtMillis: Long = 0L,
    /**
     * When this row was first published, for [BubbleTasks.reconcile].
     *
     * A row the app cannot account for is usually dead, but it might simply be newer than the
     * app's task map -- reconcile runs from the home screen's composition and can land in that
     * gap. Age is what tells those two apart.
     */
    val firstSeenAtMillis: Long = 0L,
) {
    val error: Boolean
        get() = state == BubbleTaskState.ERROR

    val finished: Boolean
        get() = state == BubbleTaskState.DONE

    /** Running or waiting to run -- the only rows the bubble's badge should count. */
    val active: Boolean
        get() = state == BubbleTaskState.RUNNING || state == BubbleTaskState.QUEUED

    /** Nothing more will happen to this row on its own. */
    val settled: Boolean
        get() = state == BubbleTaskState.DONE || state == BubbleTaskState.ERROR
}

enum class BubbleTaskState {
    RUNNING,
    QUEUED,
    PAUSED,
    ERROR,
    DONE,
}

/** What the panel's action button should do to a given row. */
enum class BubbleAction {
    PAUSE,
    RESUME,
    RETRY,
    CANCEL,
}

object BubbleTasks {
    private val _tasks = MutableStateFlow<List<BubbleTask>>(emptyList())

    /** What the bubble should be drawing right now. */
    val tasks: StateFlow<List<BubbleTask>> = _tasks.asStateFlow()

    /**
     * Ids the user has explicitly swiped away.
     *
     * Remembered separately because [publish] merges rather than assigns: without this, the very
     * next publish would put a dismissed row straight back.
     */
    private val dismissed = mutableSetOf<String>()

    /** A finished row is worth reading for a while, and clutter after that. */
    const val AGE_OUT_MS: Long = 5 * 60 * 1000L

    /**
     * How long an unsettled row is allowed to be unknown to the app before it is treated as dead.
     *
     * Long enough to cover the gap between a download being created and the app's task map showing
     * it; short enough that a genuine orphan does not sit in the panel for the rest of the session.
     */
    const val ORPHAN_GRACE_MS: Long = 30 * 1000L

    /**
     * Merge what is live into what this session has already seen.
     *
     * The publisher only knows about ACTIVE downloads, so a straight assignment dropped every row
     * the moment it finished. Retaining by id keeps finished rows visible, and still lets a live
     * row overwrite its own earlier state as it progresses.
     *
     * Order is insertion order with the newest last, because the panel scrolls and the thing you
     * just started is the thing you are looking for.
     */
    fun publish(list: List<BubbleTask>) {
        val merged = LinkedHashMap<String, BubbleTask>()
        _tasks.value.forEach { merged[it.id] = it }
        list.forEach { incoming ->
            if (incoming.id in dismissed) return@forEach
            val previous = merged[incoming.id]
            // Stamp the moment a row settles, once. Re-stamping on every publish would keep
            // pushing the age-out deadline forward and the row would never expire.
            val settledAt =
                when {
                    !incoming.settled -> 0L
                    previous != null && previous.settledAtMillis > 0L -> previous.settledAtMillis
                    else -> System.currentTimeMillis()
                }
            merged[incoming.id] =
                incoming.copy(
                    settledAtMillis = settledAt,
                    // Stamped once, on the first publish, and carried forward after that -- the
                    // point is how long we have known about the row, not when we last heard.
                    firstSeenAtMillis =
                        previous?.firstSeenAtMillis?.takeIf { it > 0L }
                            ?: System.currentTimeMillis(),
                )
        }
        _tasks.value = merged.values.toList()
    }

    /** One row, swiped away by hand. The only removal that needs no justification. */
    fun dismiss(id: String) {
        dismissed += id
        _tasks.value = _tasks.value.filterNot { it.id == id }
    }

    /**
     * Drop settled rows the app can no longer account for.
     *
     * [knownUrls] is every URL the app still has a live task or a visible history row for. A
     * finished row whose URL is absent describes a download that has been deleted -- which is
     * exactly the "it still says 5+ after I deleted those" case. The panel had no way to hear
     * about a deletion, because the publisher only ever describes what is live.
     *
     * Running rows are never dropped: their task exists by definition, and a row that vanished
     * mid-download would look like a crash. Rows with no identifier at all are never dropped
     * either, since absence from [knownUrls] would be meaningless rather than informative.
     *
     * MATCHED ON THE PATH AS WELL AS THE URL, and that is not belt-and-braces. A task carries the
     * URL the user PASTED; the history row stores the one the download resolved to. For a
     * youtu.be link those are different strings for the same video, so a URL-only test read
     * "absent" and quietly deleted a row that had just completed -- the panel showed "nothing left
     * in the queue" seconds after a successful download. The file path is the one identifier both
     * sides agree on, because only one of them invents it.
     *
     * Absence is therefore only believed when EVERY identifier the row has is unaccounted for.
     */
    fun reconcile(knownUrls: Set<String>, nowMillis: Long = System.currentTimeMillis()) {
        val kept =
            _tasks.value.filter { row ->
                val ids = listOfNotNull(row.url.ifBlank { null }, row.filePath?.ifBlank { null })
                // Nothing to match on: keep it. A row with no URL and no path cannot be checked
                // against the app, and guessing would delete real downloads.
                if (ids.isEmpty()) return@filter true
                if (ids.any { it in knownUrls }) return@filter true

                // Not accounted for by the app. A settled row goes at once -- that is the
                // "I deleted it but the badge still counts it" case.
                //
                // An UNSETTLED row used to be kept unconditionally, on the assumption that
                // unsettled means alive. It does not: a task the app has dropped while it was
                // running, queued or paused leaves a row that never settles, so neither
                // forgetSettled() nor ageOut() will ever touch it, and it outlives the session
                // showing 0% forever with a button that does nothing. It gets a grace period
                // instead, then goes the same way.
                !row.settled && nowMillis - row.firstSeenAtMillis < ORPHAN_GRACE_MS
            }
        if (kept.size != _tasks.value.size) _tasks.value = kept
    }

    /**
     * Retire finished rows older than [AGE_OUT_MS].
     *
     * Errors are kept: a failure the user never saw is the one row that has to wait for them.
     */
    fun ageOut(nowMillis: Long = System.currentTimeMillis()) {
        val kept =
            _tasks.value.filter { row ->
                !row.finished ||
                    row.settledAtMillis <= 0L ||
                    nowMillis - row.settledAtMillis < AGE_OUT_MS
            }
        if (kept.size != _tasks.value.size) _tasks.value = kept
    }

    /**
     * What the bubble's badge should say.
     *
     * Running plus queued, NOT `tasks.size`. The badge counted every row the session had ever
     * seen, so a finished-and-forgotten download kept inflating a number that is supposed to mean
     * "work outstanding".
     */
    val activeCount: Int
        get() = _tasks.value.count { it.active }

    /**
     * Forget every settled row. Called when the floating window is torn down.
     *
     * A finished or failed row is a receipt: worth seeing while the window is open, meaningless
     * once it has closed -- and keeping them is how the badge ended up counting downloads that had
     * been deleted days earlier. Nothing here is written to disk, so these rows only ever survived
     * because the overlay's own service kept the process alive.
     *
     * THE PART THAT IS NOT OBVIOUS, and that a first attempt got wrong: emptying the list is not
     * enough. [publish] merges from the downloader's task list, which still holds finished and
     * failed tasks, so the next publish after the window reopened put every one of them straight
     * back. They have to be suppressed at the source, and [dismissed] is the mechanism that
     * already exists for precisely that -- publish() honours it.
     *
     * Anything NOT settled survives, which means running, queued AND paused. Paused is the reason
     * this filters on `settled` rather than `active`: `active` is running-or-queued only, so it
     * would drop a paused download, and with it the button that resumes it.
     */
    fun forgetSettled() {
        val (settled, kept) = _tasks.value.partition { it.settled }
        if (settled.isEmpty()) return
        dismissed += settled.map { it.id }
        _tasks.value = kept
    }

    /** The Clear button. Forgets the rows, and that any were dismissed. */
    fun clear() {
        dismissed.clear()
        _tasks.value = emptyList()
    }
}
