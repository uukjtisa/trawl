package com.junkfood.seal.ui.bubble

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The floating download bubble: a WindowManager overlay hosting a ComposeView.
//
// THE TRAP THAT MAKES THIS NON-OBVIOUS. A ComposeView only works inside a window that provides
// three owners through the view tree -- a LifecycleOwner, a ViewModelStoreOwner and a
// SavedStateRegistryOwner. An Activity supplies all three; a Service supplies none, so a
// ComposeView added straight to an overlay window crashes on its FIRST COMPOSITION with a
// ViewTreeLifecycleOwner error. This service therefore implements all three itself and attaches
// them with setViewTreeOwners before the view is added.
//
// IT DEGRADES TO NOTHING DANGEROUS. SYSTEM_ALERT_WINDOW is a special permission the user grants
// by hand in Settings and can revoke silently at any time, and on this Huawei ROM background
// services are killed aggressively. So the bubble is an ENHANCEMENT, never the only way to see
// progress (D-08): the existing download notification is untouched and remains the guaranteed
// surface. If the permission is missing or revoked the service stops itself immediately rather
// than throwing, and the user simply has the notification they always had.

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
import com.junkfood.seal.R
import com.junkfood.seal.util.TrawlLog

class BubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var bubbleView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams

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

        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        addOverlay()
    }

    private fun addOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                // NOT_FOCUSABLE keeps the keyboard and back button working in whatever app is
                // underneath -- an overlay that steals focus makes every other app feel broken.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            )
                .apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 24
                    y = 320
                }

        val view =
            ComposeView(this).apply {
                // Without these three the first composition throws. See the file header.
                setViewTreeLifecycleOwner(this@BubbleService)
                setViewTreeViewModelStoreOwner(this@BubbleService)
                setViewTreeSavedStateRegistryOwner(this@BubbleService)
                setContent {
                    BubbleOverlay(
                        onMove = { dx, dy -> moveBy(dx, dy) },
                        onDismiss = { stopSelf() },
                        onOpenApp = { openApp() },
                    )
                }
            }

        bubbleView = view
        runCatching { wm.addView(view, params) }
            .onFailure {
                // A revoked permission surfaces here as a BadTokenException. Stopping is the
                // correct response: the notification is still doing its job.
                TrawlLog.e("Bubble: could not add overlay", it)
                stopSelf()
            }
    }

    private fun moveBy(dx: Float, dy: Float) {
        val wm = windowManager ?: return
        val view = bubbleView ?: return
        params.x += dx.toInt()
        params.y += dy.toInt()
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
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
        private const val CHANNEL_ID = "trawl_bubble"
        private const val NOTIFICATION_ID = 4210

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        /** Starts the bubble if it is allowed to exist. Silently does nothing if it is not. */
        fun start(context: Context) {
            if (!canDrawOverlays(context)) {
                TrawlLog.i("Bubble: not started, no overlay permission")
                return
            }
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }
}
