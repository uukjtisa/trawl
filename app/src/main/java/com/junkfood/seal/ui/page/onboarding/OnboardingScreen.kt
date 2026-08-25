package com.junkfood.seal.ui.page.onboarding

// REWRITTEN by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
//
// The inherited version was a reskin: four generic Material glyphs, marketing copy hardcoded in
// Kotlin ("Your ultimate video downloader", "Powerful Downloads"), and three background blobs
// softened with Modifier.blur.
//
// THE GLOW ARTEFACT. Modifier.blur defaults to BlurredEdgeTreatment.Rectangle, which clips the
// blur to the element's own bounds -- so blurring a circle by 80dp inside a 300dp box slices the
// soft edge off square. That was the "cropped out glow mist". It is also a RenderEffect, a silent
// no-op below API 31, so the same shapes rendered as hard discs on Android 7 to 11. Every shape
// here is painted with a radial gradient that ends at Color.Transparent instead: soft for free,
// no bounds artefact, identical on every API level.
//
// The animation follows the app's own intro rather than inventing a second vocabulary: a mask the
// title rises out of, a rule that draws itself, then the supporting text staggering in behind.
// Everything is keyed to the page BECOMING current, so it replays when you swipe back, and every
// start state lives in the animation's initial value -- so if animation is unavailable the page
// still renders assembled rather than invisible.

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.Icons
import com.junkfood.seal.util.DownloadProvider
import com.junkfood.seal.util.DirectResolvers
import com.junkfood.seal.util.BatteryUtil
import com.junkfood.seal.ui.bubble.BubbleService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.clickable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.Settings
import android.os.Build
import android.net.Uri
import android.content.pm.PackageManager
import android.content.Intent
import android.Manifest

/** What a permission row can be asked to do. */
private enum class Grant {
    /** Nothing to ask for: an install-time permission, already held. */
    NONE,
    NOTIFICATIONS,
    BATTERY,
    OVERLAY,
    STORAGE,
}

private data class Permission(
    // Material glyphs here, not Trawl's marks. The hero pages carry the branding; a permissions
    // list needs a reader to recognise "notification" and "folder" at 18dp, and two different
    // shield outlines are indistinguishable at that size -- which is exactly how the first cut
    // drew Notifications and Display-over-other-apps.
    val icon: ImageVector,
    val name: Int,
    val why: Int,
    val optional: Boolean,
    val grant: Grant,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = 4
    val pagerState = rememberPagerState(pageCount = { pages })
    val scope = rememberCoroutineScope()
    val tokens = LocalTrawlTokens.current

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientWash()
        SwimmingFish()

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                // Keyed to arrival, so swiping back replays the entrance rather than showing a
                // page that already happened.
                val current = pagerState.currentPage == page
                when (page) {
                    0 -> IdentityPage(current)
                    1 -> RoutesPage(current)
                    2 -> PermissionsPage(current)
                    else -> ReadyPage(current)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages) { i ->
                        val on = pagerState.currentPage == i
                        Box(
                            Modifier.height(4.dp)
                                .width(if (on) 20.dp else 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (on) tokens.accent
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                                )
                        )
                    }
                }
                if (pagerState.currentPage < pages - 1) {
                    TextButton(onClick = onFinish) { Text(stringResource(R.string.onboard_skip)) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent),
                    ) {
                        Text(stringResource(R.string.onboard_next))
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent),
                    ) {
                        Text(stringResource(R.string.onboard_start))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- pages

@Composable
private fun IdentityPage(active: Boolean) {
    PageScaffold {
        val mark = stagger(active, delayMs = 0)
        val rule = stagger(active, delayMs = 620)
        val sub = stagger(active, delayMs = 780)
        val by = stagger(active, delayMs = 980)

        Box(
            Modifier.size(112.dp).graphicsLayer {
                alpha = mark
                // Settles rather than pops: 0.82 -> 1.0 across the same curve the text rises on.
                val s = 0.82f + 0.18f * mark
                scaleX = s
                scaleY = s
            },
            contentAlignment = Alignment.Center,
        ) {
            Halo()
            Icon(
                painter = painterResource(R.drawable.trawl_mark),
                contentDescription = null,
                tint = LocalTrawlTokens.current.accent,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(Modifier.height(20.dp))

        MaskRise(active = active, delayMs = 260) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(10.dp))
        // A pseudo-rule that scales, because a border width cannot be animated and a scaleX on a
        // box can. Same trick the portfolio intro uses.
        Box(
            Modifier.height(2.dp)
                .width(96.dp)
                .graphicsLayer {
                    scaleX = rule
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .clip(RoundedCornerShape(50))
                .background(LocalTrawlTokens.current.accent)
        )
        Spacer(Modifier.height(18.dp))

        Body(stringResource(R.string.onboard_1_body), sub)
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.onboard_by),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.alpha(by),
        )
    }
}

@Composable
private fun RoutesPage(active: Boolean) {
    PageScaffold {
        GlyphBadge(R.drawable.ic_bolt_trawl, stagger(active, 0))
        Spacer(Modifier.height(20.dp))
        MaskRise(active = active, delayMs = 200) { Title(stringResource(R.string.onboard_2_title)) }
        Spacer(Modifier.height(14.dp))
        Body(stringResource(R.string.onboard_2_body), stagger(active, 460))

        Spacer(Modifier.height(24.dp))
        // Rendered from the resolver registry, never from a string. Add a resolver and this line
        // grows on its own; the old copy named two platforms and would have gone stale silently.
        RouteRow(
            label = DirectResolvers.platforms().joinToString(", "),
            engine = DownloadProvider.TRAWL_DIRECT.label,
            alpha = stagger(active, 640),
        )
        Spacer(Modifier.height(8.dp))
        RouteRow(
            label = stringResource(R.string.onboard_route_everything_else),
            engine = DownloadProvider.YT_DLP.label,
            alpha = stagger(active, 780),
        )
    }
}

@Composable
private fun PermissionsPage(active: Boolean) {
    val context = LocalContext.current
    // Bumped by every launcher callback and by ON_RESUME, because three of these four are granted
    // on a system screen we do not control and there is no callback that carries the answer.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // "Grant all" cannot fire four system screens at once, so it queues them and advances as each
    // one returns. Empty means nothing is in flight.
    var queue by remember { mutableStateOf(listOf<Grant>()) }

    val notifyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            refresh++
            queue = queue.drop(1)
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refresh++
            queue = queue.drop(1)
        }

    val perms =
        listOf(
            Permission(Icons.Outlined.Public, R.string.perm_internet, R.string.perm_internet_why, false, Grant.NONE),
            Permission(Icons.Outlined.Notifications, R.string.perm_notify, R.string.perm_notify_why, true, Grant.NOTIFICATIONS),
            Permission(Icons.Outlined.BatteryChargingFull, R.string.perm_battery, R.string.perm_battery_why, true, Grant.BATTERY),
            Permission(Icons.Outlined.PictureInPictureAlt, R.string.perm_overlay, R.string.perm_overlay_why, true, Grant.OVERLAY),
            Permission(Icons.Outlined.Folder, R.string.perm_storage, R.string.perm_storage_why, true, Grant.STORAGE),
        )

    fun granted(g: Grant): Boolean =
        when (g) {
            Grant.NONE -> true
            Grant.NOTIFICATIONS ->
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
            Grant.BATTERY -> BatteryUtil.isIgnoringBatteryOptimizations(context)
            Grant.OVERLAY -> BubbleService.canDrawOverlays(context)
            // Scoped storage means there is nothing to ask for on 10 and up; the manifest caps
            // the permission at API 29 for exactly that reason.
            Grant.STORAGE ->
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) == PackageManager.PERMISSION_GRANTED
        }

    /** Not applicable on this OS version, so showing it as "missing" would be a lie. */
    fun relevant(g: Grant): Boolean =
        when (g) {
            Grant.NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            Grant.STORAGE -> Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
            Grant.NONE -> false
            else -> true
        }

    fun ask(g: Grant) {
        when (g) {
            Grant.NONE -> {}
            Grant.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else queue = queue.drop(1)
            Grant.STORAGE -> {
                // The launcher contract wants a permission string; below Q that is the classic
                // storage grant, above it there is nothing to request.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    notifyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else queue = queue.drop(1)
            }
            Grant.BATTERY -> {
                val ok = BatteryUtil.launchBatterySettings(context) { settingsLauncher.launch(it) }
                if (!ok) queue = queue.drop(1)
            }
            Grant.OVERLAY ->
                runCatching {
                        settingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName),
                            )
                        )
                    }
                    .onFailure { queue = queue.drop(1) }
        }
    }

    // Drives the queue: whenever its head changes, ask for that one.
    LaunchedEffect(queue.firstOrNull()) { queue.firstOrNull()?.let { ask(it) } }

    val outstanding =
        remember(refresh) { perms.map { it.grant }.filter { relevant(it) && !granted(it) } }

    PageScaffold(scroll = true) {
        MaskRise(active = active, delayMs = 120) { Title(stringResource(R.string.onboard_3_title)) }
        Spacer(Modifier.height(10.dp))
        Body(stringResource(R.string.onboard_3_body), stagger(active, 320))
        Spacer(Modifier.height(18.dp))

        val actions = stagger(active, 420)
        Row(
            modifier = Modifier.fillMaxWidth().alpha(actions),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { queue = outstanding },
                enabled = outstanding.isNotEmpty() && queue.isEmpty(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LocalTrawlTokens.current.accent
                    ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (outstanding.isEmpty()) stringResource(R.string.perm_all_set)
                    else stringResource(R.string.perm_grant_all)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.perm_none_required),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(actions),
        )
        Spacer(Modifier.height(18.dp))

        perms.forEachIndexed { i, p ->
            val isRelevant = p.grant == Grant.NONE || relevant(p.grant)
            if (isRelevant) {
                PermissionRow(
                    p = p,
                    alpha = stagger(active, 520 + i * 100),
                    granted = remember(refresh) { granted(p.grant) },
                    onGrant = { if (queue.isEmpty()) queue = listOf(p.grant) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ReadyPage(active: Boolean) {
    PageScaffold {
        GlyphBadge(R.drawable.ic_fish, stagger(active, 0))
        Spacer(Modifier.height(20.dp))
        MaskRise(active = active, delayMs = 200) { Title(stringResource(R.string.onboard_4_title)) }
        Spacer(Modifier.height(14.dp))
        Body(stringResource(R.string.onboard_4_body), stagger(active, 460))
    }
}

// ---------------------------------------------------------------------------- parts

@Composable
private fun PageScaffold(scroll: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (scroll) Arrangement.Top else Arrangement.Center,
        content = content,
    )
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Body(text: String, alpha: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(alpha).graphicsLayer { translationY = (1f - alpha) * 14.dp.toPx() },
    )
}

@Composable
private fun Halo() {
    val accent = LocalTrawlTokens.current.accent
    Box(
        Modifier.size(150.dp)
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.08f), Color.Transparent)
                )
            )
    )
}

@Composable
private fun GlyphBadge(@DrawableRes icon: Int, alpha: Float) {
    Box(
        Modifier.size(108.dp).graphicsLayer {
            this.alpha = alpha
            val s = 0.84f + 0.16f * alpha
            scaleX = s
            scaleY = s
        },
        contentAlignment = Alignment.Center,
    ) {
        Halo()
        Box(
            Modifier.size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = LocalTrawlTokens.current.accent,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun RouteRow(label: String, engine: String, alpha: Float) {
    val tokens = LocalTrawlTokens.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .alpha(alpha)
                .graphicsLayer { translationX = (1f - alpha) * (-18).dp.toPx() }
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = engine,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.accent,
        )
    }
}

@Composable
private fun PermissionRow(p: Permission, alpha: Float, granted: Boolean, onGrant: () -> Unit) {
    val tappable = !granted && p.grant != Grant.NONE
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .alpha(alpha)
                .graphicsLayer { translationY = (1f - alpha) * 12.dp.toPx() }
                .clip(RoundedCornerShape(10.dp))
                // Selective grant: a row that still needs something asks for that one thing.
                // A granted row is inert, because a tap that reopens a settings screen you have
                // already dealt with reads as the app not having noticed.
                .then(if (tappable) Modifier.clickable { onGrant() } else Modifier)
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = p.icon,
            contentDescription = null,
            tint = LocalTrawlTokens.current.accent,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(p.name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (p.optional) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.perm_optional),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            Text(
                text = stringResource(p.why),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
        }
        Spacer(Modifier.width(10.dp))
        if (granted) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = LocalTrawlTokens.current.ok,
                modifier = Modifier.size(18.dp),
            )
        } else if (p.grant != Grant.NONE) {
            Text(
                text = stringResource(R.string.perm_grant),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LocalTrawlTokens.current.accent,
            )
        }
    }
}

// ---------------------------------------------------------------------------- motion

/**
 * 0 to 1 once [active] turns true, after [delayMs].
 *
 * Returns 1f immediately when the page is not active AND has been seen, so a page that is swiped
 * away does not animate out from under the finger. The initial value is 0f only for the page
 * being entered.
 */
@Composable
private fun stagger(active: Boolean, delayMs: Int): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            anim.snapTo(0f)
            delay(delayMs.toLong())
            anim.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        } else {
            anim.snapTo(1f)
        }
    }
    return anim.value
}

/**
 * The title rises out of its own mask, the way the app's intro does it.
 *
 * The mask is a clipToBounds on a box exactly as tall as the content, and the content translates
 * up into it from 120% of its own height -- 120 rather than 100 because a descender hangs below
 * the line box and peeks over the mask edge at exactly 100.
 */
@Composable
private fun MaskRise(active: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    val p = stagger(active, delayMs)
    Box(Modifier.clipToBounds()) {
        Box(Modifier.graphicsLayer { translationY = (1f - p) * 1.2f * size.height }) { content() }
    }
}

/** A soft wash behind everything. Radial gradients, never a blur -- see the file header. */
@Composable
private fun AmbientWash() {
    val tokens = LocalTrawlTokens.current
    Box(Modifier.fillMaxSize().alpha(0.5f)) {
        Box(
            Modifier.size(340.dp)
                .offset(x = 150.dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        listOf(tokens.accent.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier.size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-110).dp, y = 120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

/** The mascot, crossing the screen and bobbing, because a still fish is a logo. */
@Composable
private fun SwimmingFish() {
    val t = rememberInfiniteTransition(label = "fish")
    val x by
        t.animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec =
                infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
            label = "x",
        )
    val bob by
        t.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bob",
        )
    // Positioned by a fraction of the container rather than the icon: the icon's own size is not
    // the travel distance, the screen's width is.
    Box(Modifier.fillMaxSize().alpha(0.13f)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationX = (x - 0.5f) * size.width
                translationY = bob * 10.dp.toPx() + size.height * 0.34f
            },
            contentAlignment = Alignment.TopCenter,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fish),
                contentDescription = null,
                tint = LocalTrawlTokens.current.accent,
                modifier =
                    Modifier.size(52.dp).graphicsLayer {
                        // Tilts into the bob, so it reads as swimming rather than sliding.
                        rotationZ = bob * 7f
                    },
            )
        }
    }
}
