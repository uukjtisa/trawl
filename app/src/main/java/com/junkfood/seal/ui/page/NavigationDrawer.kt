package com.junkfood.seal.ui.page

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: drawer header now shows Trawl's mark and name instead of Seal Plus's.

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalAnimStyle
import com.junkfood.seal.ui.common.LocalPinSwitcher
import com.junkfood.seal.ui.component.AnimStyle
import com.junkfood.seal.ui.component.TrawlSwitcher
import com.junkfood.seal.ui.component.menuItemEntrance
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.common.ThemedIconColors
import com.junkfood.seal.ui.page.downloadv2.DownloadPageImplV2
import com.junkfood.seal.ui.page.security.LockScreen
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    windowWidth: WindowWidthSizeClass = LocalWindowWidthState.current,
    currentRoute: String? = null,
    currentTopDestination: String? = null,
    showQuickSettings: Boolean = true,
    onNavigateToRoute: (String) -> Unit,
    onDismissRequest: suspend () -> Unit,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showHiddenContentAuthScreen by remember { mutableStateOf(false) }
    var hiddenContentAuthDone by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    when (windowWidth) {
        WindowWidthSizeClass.Compact,
        WindowWidthSizeClass.Medium -> {
            // Phone-sized. Fancy needs the content itself transformed, which
            // ModalNavigationDrawer cannot do -- it only draws a sheet on top -- so that style
            // gets its own container. Simple keeps the platform-conventional drawer.
            if (LocalAnimStyle.current == AnimStyle.FANCY) {
                TrawlSwitcher(
                    open = drawerState.isOpen,
                    style = AnimStyle.FANCY,
                    gesturesEnabled = gesturesEnabled,
                    onDismiss = { scope.launch { onDismissRequest() } },
                    menu = {
                        NavigationDrawerSheetContent(
                            modifier = Modifier,
                            currentRoute = currentRoute,
                            showQuickSettings = showQuickSettings,
                            onNavigateToRoute = onNavigateToRoute,
                            onDismissRequest = onDismissRequest,
                            onShowHiddenContentAuth = {
                                hiddenContentAuthDone = false
                                showHiddenContentAuthScreen = true
                            },
                        )
                    },
                    content = content,
                )
            } else {
                ModalNavigationDrawer(
                    gesturesEnabled = gesturesEnabled,
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerState = drawerState,
                            modifier = modifier.width(360.dp),
                        ) {
                            NavigationDrawerSheetContent(
                                modifier = Modifier,
                                currentRoute = currentRoute,
                                showQuickSettings = showQuickSettings,
                                onNavigateToRoute = onNavigateToRoute,
                                onDismissRequest = onDismissRequest,
                                onShowHiddenContentAuth = {
                                    hiddenContentAuthDone = false
                                    showHiddenContentAuthScreen = true
                                },
                            )
                        }
                    },
                    content = content,
                )
            }
        }
        WindowWidthSizeClass.Expanded -> {
            ModalNavigationDrawer(
                gesturesEnabled = drawerState.isOpen,
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(drawerState = drawerState, modifier = modifier.width(360.dp)) {
                        NavigationDrawerSheetContent(
                            modifier = Modifier,
                            currentRoute = currentRoute,
                            showQuickSettings = showQuickSettings,
                            onNavigateToRoute = onNavigateToRoute,
                            onDismissRequest = onDismissRequest,
                            onShowHiddenContentAuth = {
                                hiddenContentAuthDone = false
                                showHiddenContentAuthScreen = true
                            },
                        )
                    }
                },
            ) {
                Row {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.zIndex(1f),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight().systemBarsPadding().width(92.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(8.dp))
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Icon(Icons.Outlined.Menu, null, tint = ThemedIconColors.primary)
                            }
                            Spacer(Modifier.weight(1f))
                            NavigationRailContent(
                                modifier = Modifier,
                                currentTopDestination = currentTopDestination,
                                onNavigateToRoute = onNavigateToRoute,
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    content()
                }
            }
        }
    }

    // Full-screen LockScreen overlay — rendered outside the narrow ModalDrawerSheet
    // so it covers the entire display.
    if (showHiddenContentAuthScreen && !hiddenContentAuthDone) {
        Box(modifier = Modifier.fillMaxSize()) {
            LockScreen(
                onUnlocked = {
                    hiddenContentAuthDone = true
                    showHiddenContentAuthScreen = false
                    scope.launch { onDismissRequest() }
                        .invokeOnCompletion { onNavigateToRoute(Route.HIDDEN_CONTENT) }
                },
                useBiometric = AuthenticationManager.useBiometric()
            )
        }
    }
    } // end outer Box
}

@Composable
fun DrawerHeader(modifier: Modifier = Modifier) {
    // The contract's drawer header: mark, name, and the version line. Small on purpose -- in a
    // window switcher the panel's job is to list windows, and a header big enough to be a
    // splash screen pushes the actual navigation down the page.
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 22.dp, end = 20.dp, top = 26.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.trawl_mark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "v${App.packageInfo.versionName} \u00b7 niccc2007",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun NavigationDrawerSheetContent(
    modifier: Modifier = Modifier,
    currentRoute: String? = null,
    showQuickSettings: Boolean = true,
    onNavigateToRoute: (String) -> Unit,
    onDismissRequest: suspend () -> Unit,
    onShowHiddenContentAuth: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // "Keep the switcher open" turns a menu tap from GO INTO into PREVIEW: the screen behind the
    // card changes, the switcher stays up, and a second tap on the card enters. Two taps instead
    // of one, which is why it is not the default -- but it is the honest expression of a task
    // switcher, and it makes the previewed card meaningful rather than decorative.
    val pinned = LocalPinSwitcher.current
    val navigateOrPreview: (String) -> Unit = { route ->
        if (pinned) {
            onNavigateToRoute(route)
        } else {
            scope.launch { onDismissRequest() }.invokeOnCompletion { onNavigateToRoute(route) }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
    ) {
        // Modern gradient header
        DrawerHeader()
        
        Spacer(Modifier.height(16.dp))

        // Group 1: Primary Destinations
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.home)) },
                    icon = { Icon(Icons.Filled.Download, null, tint = ThemedIconColors.primary) },
                    onClick = { navigateOrPreview(Route.HOME) },
                    selected = currentRoute == Route.HOME,
                    modifier = Modifier.menuItemEntrance(0).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.links_history)) },
                    icon = { Icon(Icons.Rounded.Link, null, tint = ThemedIconColors.primary) },
                    onClick = { navigateOrPreview(Route.LINKS_HISTORY) },
                    selected = currentRoute == Route.LINKS_HISTORY,
                    modifier = Modifier.menuItemEntrance(1).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.downloads_history)) },
                    icon = { Icon(Icons.Outlined.Subscriptions, null, tint = ThemedIconColors.secondary) },
                    onClick = { navigateOrPreview(Route.DOWNLOADS) },
                    selected = currentRoute == Route.DOWNLOADS,
                    modifier = Modifier.menuItemEntrance(2).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.hidden_content)) },
                    icon = { Icon(Icons.Outlined.VisibilityOff, null, tint = ThemedIconColors.secondary) },
                    onClick = {
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            onShowHiddenContentAuth()
                        } else {
                            context.makeToast(R.string.hidden_content_requires_app_lock)
                        }
                    },
                    selected = false,
                    modifier = Modifier.menuItemEntrance(3).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.custom_command)) },
                    icon = { Icon(Icons.Outlined.Terminal, null, tint = ThemedIconColors.tertiary) },
                    onClick = { navigateOrPreview(Route.TASK_LIST) },
                    selected = currentRoute == Route.TASK_LIST,
                    modifier = Modifier.menuItemEntrance(4).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.more_tools)) },
                    icon = { Icon(Icons.Outlined.Build, null, tint = ThemedIconColors.primary) },
                    onClick = { navigateOrPreview(Route.MORE_TOOLS) },
                    selected = currentRoute == Route.MORE_TOOLS,
                    modifier = Modifier.menuItemEntrance(5).padding(vertical = 2.dp)
                )
            }
        }

        // Divider between groups
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Group 2: Utilities & Support
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    icon = { Icon(Icons.Outlined.Settings, null, tint = ThemedIconColors.primary) },
                    onClick = { navigateOrPreview(Route.SETTINGS) },
                    selected = currentRoute == Route.SETTINGS,
                    modifier = Modifier.menuItemEntrance(6).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.trouble_shooting)) },
                    icon = { Icon(Icons.Rounded.BugReport, null, tint = ThemedIconColors.secondary) },
                    onClick = { navigateOrPreview(Route.TROUBLESHOOTING) },
                    selected = currentRoute == Route.TROUBLESHOOTING,
                    modifier = Modifier.menuItemEntrance(7).padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    icon = { Icon(Icons.Rounded.Info, null, tint = ThemedIconColors.primary) },
                    onClick = { navigateOrPreview(Route.ABOUT) },
                    selected = currentRoute == Route.ABOUT,
                    modifier = Modifier.menuItemEntrance(8).padding(vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun NavigationRailItemVariant(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit),
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.large)
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                )
                .selectable(selected = selected, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            icon()
        }
    }
}

@Composable
fun NavigationRailContent(
    modifier: Modifier = Modifier,
    currentTopDestination: String? = null,
    onNavigateToRoute: (String) -> Unit,
) {
    Column(
        modifier = modifier.selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val scope = rememberCoroutineScope()
        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.HOME) Icons.Filled.Download
                    else Icons.Outlined.Download,
                    stringResource(R.string.home),
                    tint = ThemedIconColors.primary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.HOME,
            onClick = { onNavigateToRoute(Route.HOME) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.DOWNLOADS) Icons.Filled.Subscriptions
                    else Icons.Outlined.Subscriptions,
                    stringResource(R.string.downloads_history),
                    tint = ThemedIconColors.secondary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.DOWNLOADS,
            onClick = { onNavigateToRoute(Route.DOWNLOADS) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.TASK_LIST) Icons.Filled.Terminal
                    else Icons.Outlined.Terminal,
                    stringResource(R.string.custom_command),
                    tint = ThemedIconColors.tertiary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.TASK_LIST,
            onClick = { onNavigateToRoute(Route.TASK_LIST) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.MORE_TOOLS) Icons.Filled.Build
                    else Icons.Outlined.Build,
                    stringResource(R.string.more_tools),
                    tint = ThemedIconColors.primary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.MORE_TOOLS,
            onClick = { onNavigateToRoute(Route.MORE_TOOLS) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.SETTINGS_PAGE) Icons.Filled.Settings
                    else Icons.Outlined.Settings,
                    stringResource(R.string.settings),
                    tint = ThemedIconColors.primary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.SETTINGS_PAGE,
            onClick = { onNavigateToRoute(Route.SETTINGS_PAGE) },
        )
    }
}

@Preview(device = "spec:width=673dp,height=841dp")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun ExpandedPreview() {
    val widthDp = LocalConfiguration.current.screenWidthDp
    var currentRoute = remember { mutableStateOf(Route.HOME) }

    CompositionLocalProvider(
        LocalWindowWidthState provides
            if (widthDp > 480) WindowWidthSizeClass.Expanded
            else if (widthDp > 360) WindowWidthSizeClass.Medium else WindowWidthSizeClass.Compact
    ) {
        Row {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            NavigationDrawer(
                currentRoute = currentRoute.value,
                currentTopDestination = currentRoute.value,
                drawerState = drawerState,
                onNavigateToRoute = { currentRoute.value = it },
                onDismissRequest = {},
            ) {
                DownloadPageImplV2(taskDownloadStateMap = remember { mutableStateMapOf() }) { _, _
                    ->
                }
            }
        }
    }
}
