package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The floating download bubble: WindowManager overlays hosting ComposeViews.
//
// THE TRAP THAT MAKES THIS NON-OBVIOUS. A ComposeView only works inside a window that provides
// three owners through the view tree -- a LifecycleOwner, a ViewModelStoreOwner and a
// SavedStateRegistryOwner. An Activity supplies all three; a Service supplies none, so a
// ComposeView added straight to an overlay window crashes on its FIRST COMPOSITION with a
// ViewTreeLifecycleOwner error. This service therefore implements all three itself and attaches
// them with setViewTreeOwners before any view is added.
//
// THREE WINDOWS, NOT ONE. The bubble, its panel and the drop-X are separate overlay windows.
// The tempting alternative -- one full-screen overlay laying all three out inside it -- does not
// work: an AndroidComposeView consumes the touches that reach it, so a full-screen one makes
// every app underneath untouchable. Keeping each piece in a window the size of the piece means
// the rest of the screen is simply not ours, which is the correct default for an overlay.
//
// IT DEGRADES TO NOTHING DANGEROUS. SYSTEM_ALERT_WINDOW is a special permission the user grants
// by hand in Settings and can revoke silently at any time, and on this Huawei ROM background
// services are killed aggressively. So the bubble is an ENHANCEMENT, never the only way to see
// progress (D-08): the existing download notification is untouched and remains the guaranteed
// surface. If the permission is missing or revoked the service stops itself immediately rather
// than throwing, and the user simply has the notification they always had.

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.junkfood.seal.MainActivity
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.TrawlLog
import kotlin.math.hypot
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.junkfood.seal.util.matchUrlFromSharedText
import com.junkfood.seal.QuickDownloadActivity
import android.view.MotionEvent
import android.view.KeyEvent
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue

class BubbleService :
    Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, KoinComponent {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    // Injected here rather than routed through the Activity. DownloaderV2 is a process-scoped
    // Koin singleton, and the whole point of the bubble is that it works while the app's UI is
    // gone -- an action posted to a composition that may not exist is an action that silently
    // does nothing.
    private val downloader: DownloaderV2 by inject()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private var windowManager: WindowManager? = null

    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var dropView: ComposeView? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private val ui = BubbleUi()
    private var settleAnim: ValueAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private val screenW: Int
        get() = resources.displayMetrics.widthPixels

    private val screenH: Int
        get() = resources.displayMetrics.heightPixels

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Checked here rather than trusted from the caller: the user can revoke this in Settings
        // while the app is running, and the first we would otherwise hear about it is a crash.
        if (!canDrawOverlays(this)) {
            TrawlLog.w("Bubble: overlay permission missing, falling back to the notification")
            stopSelf()
            return
        }

        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        addBubble()
    }

    /** Every overlay in here shares these flags; only size, gravity and touchability differ. */
    private fun overlayParams(extraFlags: Int = 0): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // NOT_FOCUSABLE keeps the keyboard and the back button working in whatever app is
            // underneath -- an overlay that steals focus makes every other app feel broken. It
            // also implies NOT_TOUCH_MODAL, so touches outside our windows are not ours either.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or extraFlags,
            PixelFormat.TRANSLUCENT,
        )

    private fun compose(content: @androidx.compose.runtime.Composable () -> Unit): ComposeView =
        ComposeView(this).apply {
            // Without these three the first composition throws. See the file header.
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeViewModelStoreOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            setContent(content)
        }

    private fun addBubble() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        bubbleParams =
            overlayParams(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenW - dp(BUBBLE_SIZE_DP + 16)
                y = screenH / 2
            }

        val view =
            compose {
                BubbleOverlay(
                    ui = ui,
                    onMove = ::moveBy,
                    onDragStart = ::startDrag,
                    onDragEnd = ::endDrag,
                    onTap = ::togglePanel,
                )
            }

        bubbleView = view
        runCatching { wm.addView(view, bubbleParams) }
            .onFailure {
                // A revoked permission surfaces here as a BadTokenException. Stopping is the
                // correct response: the notification is still doing its job.
                TrawlLog.e("Bubble: could not add overlay", it)
                stopSelf()
            }
    }

    // ── dragging ─────────────────────────────────────────────────────────────────────────────

    private fun startDrag() {
        settleAnim?.cancel()
        ui.dragging = true
        // A panel anchored to a bubble that is moving would have to chase it. Closing is both
        // simpler and what the mockup does.
        closePanel()
        showDropTarget()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val wm = windowManager ?: return
        val view = bubbleView ?: return
        bubbleParams.x = (bubbleParams.x + dx.toInt()).coerceIn(-dp(8), screenW - dp(52))
        bubbleParams.y = (bubbleParams.y + dy.toInt()).coerceIn(dp(24), screenH - dp(72))
        runCatching { wm.updateViewLayout(view, bubbleParams) }
        ui.hot = distanceToDropTarget() < dp(62)
    }

    private fun endDrag() {
        ui.dragging = false
        hideDropTarget()
        if (ui.hot) {
            ui.hot = false
            // Dismissed for THIS run only. The bubble is a per-download surface; killing it for
            // good is a decision that belongs to the panel's "Turn off" and to Settings, not to
            // a drag someone made to get it out of the way of one video.
            TrawlLog.i("Bubble: dismissed by drop")
            stopSelf()
            return
        }
        snapToEdge()
    }

    /** Distance from the bubble's centre to the X's centre, both in screen pixels. */
    private fun distanceToDropTarget(): Float {
        val bx = bubbleParams.x + dp(BUBBLE_SIZE_DP) / 2f
        val by = bubbleParams.y + dp(BUBBLE_SIZE_DP) / 2f
        val tx = screenW / 2f
        val ty = screenH - dp(DROP_BOTTOM_DP) - dp(DROP_SIZE_DP) / 2f
        return hypot(bx - tx, by - ty)
    }

    /**
     * Settles to whichever edge it was released nearer.
     *
     * Left free-floating, a bubble ends up parked over the middle of whatever the user is
     * reading. Snapping is what makes it liveable.
     */
    private fun snapToEdge() {
        val wm = windowManager ?: return
        val view = bubbleView ?: return
        val target =
            if (bubbleParams.x + dp(BUBBLE_SIZE_DP) / 2 < screenW / 2) dp(8)
            else screenW - dp(BUBBLE_SIZE_DP + 8)
        settleAnim?.cancel()
        settleAnim =
            ValueAnimator.ofInt(bubbleParams.x, target).apply {
                duration = 280
                addUpdateListener {
                    bubbleParams.x = it.animatedValue as Int
                    runCatching { wm.updateViewLayout(view, bubbleParams) }
                }
                start()
            }
    }

    private fun showDropTarget() {
        val wm = windowManager ?: return
        if (dropView != null) return
        val params =
            overlayParams(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                // The composable centres a 58dp circle inside a 90dp box, so half the difference
                // comes back off the bottom inset to land the circle where the contract puts it.
                y = dp(DROP_BOTTOM_DP - (DROP_BOX_DP - DROP_SIZE_DP) / 2)
            }
        val view = compose { BubbleDropTarget(ui) }
        dropView = view
        runCatching { wm.addView(view, params) }.onFailure { dropView = null }
    }

    private fun hideDropTarget() {
        val wm = windowManager ?: return
        dropView?.let { v ->
            runCatching { wm.removeView(v) }
            v.disposeComposition()
        }
        dropView = null
    }

    // ── the panel ────────────────────────────────────────────────────────────────────────────

    /**
     * Tapping the bubble opens the panel, and taps it shut again.
     *
     * The debounce is not a nicety. The panel window watches for outside touches, and a tap on
     * the BUBBLE is outside the panel -- so it has already closed by the time this runs, and
     * without the guard the tap would immediately open a fresh one. Two correct behaviours
     * cancelling out into "tapping it while expanded does nothing".
     */
    private fun togglePanel() {
        if (System.currentTimeMillis() - closedByOutsideTouchAt < REOPEN_GUARD_MS) return
        if (panelView != null) closePanel() else openPanel()
    }

    /** When an outside touch last closed the panel. See [togglePanel]. */
    private var closedByOutsideTouchAt = 0L

    private fun openPanel() {
        val wm = windowManager ?: return
        if (panelView != null) return
        panelParams =
            // NOT overlayParams(): this one must be FOCUSABLE, because it hosts a text field and
            // an unfocusable window gets no keyboard. That is also what makes the clipboard
            // readable here -- since Android 10 only the focused app may read it, which is why
            // the first version had to bounce the paste to MainActivity instead.
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                    // NOT_TOUCH_MODAL so the rest of the screen still belongs to the app
                    // underneath; WATCH_OUTSIDE_TOUCH so a tap out there closes the panel
                    // instead of leaving a focus-stealing window stranded on someone's screen.
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT,
                )
                .apply {
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                gravity = Gravity.TOP or Gravity.START
                // Centred on the bubble, then clamped inside the screen -- the mockup's own
                // placement maths (renderPanel()).
                x =
                    (bubbleParams.x + dp(BUBBLE_SIZE_DP) / 2 - dp(BUBBLE_PANEL_WIDTH_DP) / 2)
                        .coerceIn(dp(10), (screenW - dp(BUBBLE_PANEL_WIDTH_DP + 10)).coerceAtLeast(dp(10)))
                // Below the bubble by default; above it when there is not room below, so the
                // panel never opens off the bottom of the screen.
                y =
                    (if (bubbleParams.y + dp(70 + PANEL_EST_H_DP) > screenH)
                            bubbleParams.y - dp(PANEL_EST_H_DP + 6)
                        else bubbleParams.y + dp(70))
                        .coerceAtLeast(dp(38))
                panelAnchorY = y
            }
        val view =
            compose {
                BubblePanel(
                    onAction = ::runAction,
                    onDownload = ::startDownload,
                    onReadClipboard = ::readClipboard,
                    onInputFocus = ::liftForKeyboard,
                    onClose = ::closePanel,
                    onHide = {
                        closePanel()
                        stopSelf()
                    },
                    onTurnOff = {
                        closePanel()
                        // Off means off: the preference is what the next download consults, so
                        // turning it off here without writing it would bring the bubble straight
                        // back on the next link.
                        PreferenceUtil.switchFloatingBubble(false)
                        stopSelf()
                    },
                )
            }
        // A focusable overlay receives the BACK key. Unhandled, it would reach whatever app is
        // underneath -- so the user's "close this" would navigate someone else's app instead.
        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, code, event ->
            if (code == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                closePanel()
                true
            } else false
        }
        // ACTION_OUTSIDE arrives because of FLAG_WATCH_OUTSIDE_TOUCH. The touch itself still goes
        // to the app below; we only learn that it happened, which is all that is needed to close.
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                closedByOutsideTouchAt = System.currentTimeMillis()
                closePanel()
                true
            } else false
        }
        panelView = view
        runCatching {
                wm.addView(view, panelParams)
                view.requestFocus()
            }
            .onFailure { panelView = null }
    }

    /**
     * Lifts the panel clear of the keyboard while the field has focus, and puts it back after.
     *
     * SOFT_INPUT_ADJUST_RESIZE cannot save a window at a fixed y that the IME simply covers, and
     * a bubble parked low on the screen anchors its panel low by definition.
     */
    private fun liftForKeyboard(focused: Boolean) {
        val wm = windowManager ?: return
        val view = panelView ?: return
        panelParams.y = if (focused) dp(96) else panelAnchorY
        runCatching { wm.updateViewLayout(view, panelParams) }
    }

    /** Where the panel sits when nothing is being typed -- remembered so it can go back. */
    private var panelAnchorY = 0

    private fun readClipboard(): String {
        val clip = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val text =
            clip?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        // Links only, deliberately. The raw-text fallback that used to be here meant copying a
        // paragraph put a paragraph in the field -- and with the watcher on, it would have tried
        // to download one.
        return matchUrlFromSharedText(text).orEmpty().takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        } ?: ""
    }

    /**
     * Hands the link to QuickDownloadActivity -- the app's own share-intent entry point.
     *
     * The bubble deliberately does NOT enqueue the download itself: choosing format, quality and
     * playlist handling is a whole screen's worth of decisions, and reimplementing it inside a
     * 300dp overlay would be a second, worse copy of a flow that already exists.
     */
    private fun startDownload(url: String) {
        val link = url.trim()
        if (link.isBlank()) return
        closePanel()
        val intent =
            Intent(this, QuickDownloadActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, link)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { startActivity(intent) }
            .onFailure { TrawlLog.e("Bubble: could not open the download sheet", it) }
    }

    private fun closePanel() {
        val wm = windowManager ?: return
        panelView?.let { v ->
            runCatching { wm.removeView(v) }
            v.disposeComposition()
        }
        panelView = null
    }

    private fun runAction(taskId: String, action: BubbleAction) {
        when (action) {
            BubbleAction.PAUSE -> downloader.pause(taskId)
            BubbleAction.RESUME -> downloader.resume(taskId)
            BubbleAction.CANCEL -> downloader.cancel(taskId)
            // restart() wants the Task itself, and the id is all the overlay has -- it is
            // deliberately holding a flat snapshot, not live domain objects.
            BubbleAction.RETRY ->
                downloader.getTaskStateMap().keys.firstOrNull { it.id == taskId }?.let {
                    downloader.restart(it)
                }
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.bubble_channel_name),
                    // LOW: this notification exists because a foreground service is required to
                    // have one, not because it has anything to say. It should never make a sound.
                    NotificationManager.IMPORTANCE_LOW,
                )
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trawl)
            .setContentTitle(getString(R.string.bubble_running))
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        settleAnim?.cancel()
        closePanel()
        hideDropTarget()
        bubbleView?.let { view ->
            runCatching { windowManager?.removeView(view) }
            view.disposeComposition()
        }
        bubbleView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    companion object {
        /** Whether the overlay is up, so the home screen's control can say so. */
        var isRunning by mutableStateOf(false)
            private set

        private const val CHANNEL_ID = "trawl_bubble"
        private const val NOTIFICATION_ID = 4210

        /**
         * How long after an outside-touch close a bubble tap is treated as part of that same
         * gesture rather than as a fresh "open".
         */
        private const val REOPEN_GUARD_MS = 350L

        /** `.dropx{bottom:26px}` and its 58px circle, inside the composable's 90dp box. */
        private const val DROP_BOTTOM_DP = 26
        private const val DROP_SIZE_DP = 58
        private const val DROP_BOX_DP = 90

        /** Enough of an estimate to decide "below the bubble" from "above it". */
        private const val PANEL_EST_H_DP = 250

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        /**
         * True when the user turned the bubble on themselves rather than a download raising it.
         *
         * The distinction is the whole difference between an indicator and a tool: an indicator
         * may disappear when there is nothing to indicate, a tool may not.
         */
        var summoned by mutableStateOf(false)
            private set

        /** Starts the bubble if it is allowed to exist. Silently does nothing if it is not. */
        fun start(context: Context, summoned: Boolean = false) {
            if (!canDrawOverlays(context)) {
                TrawlLog.i("Bubble: not started, no overlay permission")
                return
            }
            // Sticky: a bubble raised by a download and then kept open by the user stays a
            // tool. It is only cleared by an explicit stop.
            if (summoned) Companion.summoned = true
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            summoned = false
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }
}
