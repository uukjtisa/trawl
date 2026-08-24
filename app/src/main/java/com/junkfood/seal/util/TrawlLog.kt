package com.junkfood.seal.util

import android.util.Log

/**
 * Trawl's logging front door. NEW FILE, added by the Trawl project 2026-08-25 — not inherited
 * from Seal Plus, so it carries no GPL-3 section 5(a) change notice.
 *
 * Everything Trawl logs uses the single tag [TAG] so the developer and any Logcat window can
 * filter the whole app's own output with `tag:Trawl` and see identical streams. Do not introduce
 * per-class tags: the point is that one filter catches everything.
 */
object TrawlLog {
    const val TAG = "Trawl"

    fun v(msg: String) = Log.v(TAG, msg)

    fun d(msg: String) = Log.d(TAG, msg)

    fun i(msg: String) = Log.i(TAG, msg)

    fun w(msg: String, tr: Throwable? = null) =
        if (tr != null) Log.w(TAG, msg, tr) else Log.w(TAG, msg)

    fun e(msg: String, tr: Throwable? = null) =
        if (tr != null) Log.e(TAG, msg, tr) else Log.e(TAG, msg)

    /** Marks a UI surface as having been composed, so the shared console shows the walk-through. */
    fun screen(name: String) = Log.i(TAG, "screen → $name")
}
