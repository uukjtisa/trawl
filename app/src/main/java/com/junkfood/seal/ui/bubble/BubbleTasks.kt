package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The bubble lives in its own window, owned by a Service, with no access to the Activity's
// composition. So the app publishes what it is downloading here and the overlay collects it.
//
// A process-scoped StateFlow rather than a Binder or a broadcast: both sides are in the same
// process, the payload is a handful of fields per task, and the overlay needs the CURRENT value
// the moment it composes -- which is exactly what a StateFlow gives and a broadcast does not.

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
) {
    val error: Boolean
        get() = state == BubbleTaskState.ERROR

    val finished: Boolean
        get() = state == BubbleTaskState.DONE
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
     * Merge what is live into what this session has already seen.
     *
     * The publisher only knows about ACTIVE downloads, so a straight assignment dropped every row
     * the moment it finished -- which is exactly the "the list empties itself" complaint. Retaining
     * by id keeps finished rows visible until Clear, and still lets a live row overwrite its own
     * earlier state as it progresses.
     *
     * Order is insertion order with the newest last, because the panel scrolls and the thing you
     * just started is the thing you are looking for.
     */
    fun publish(list: List<BubbleTask>) {
        if (list.isEmpty()) return
        val merged = LinkedHashMap<String, BubbleTask>()
        _tasks.value.forEach { merged[it.id] = it }
        list.forEach { merged[it.id] = it }
        _tasks.value = merged.values.toList()
    }

    /** The Clear button, and the only thing that forgets. */
    fun clear() {
        _tasks.value = emptyList()
    }
}
