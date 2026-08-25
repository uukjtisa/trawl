package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The bubble lives in its own window, owned by a Service, with no access to the Activity's
// composition. So the app publishes what it is downloading here and the overlay collects it.
//
// A process-scoped StateFlow rather than a Binder or a broadcast: both sides are in the same
// process, the payload is three fields per task, and the overlay needs the CURRENT value the
// moment it composes -- which is exactly what a StateFlow gives and a broadcast does not.

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BubbleTasks {
    private val _tasks = MutableStateFlow<List<BubbleTask>>(emptyList())

    /** What the bubble should be drawing right now. */
    val tasks: StateFlow<List<BubbleTask>> = _tasks.asStateFlow()

    fun publish(list: List<BubbleTask>) {
        _tasks.value = list
    }

    fun clear() {
        _tasks.value = emptyList()
    }
}
