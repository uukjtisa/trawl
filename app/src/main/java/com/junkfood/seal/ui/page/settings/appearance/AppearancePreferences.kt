package com.junkfood.seal.ui.page.settings.appearance

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: added the seven-swatch Trawl theme picker and the switch that hides the
// inherited palette. The standalone 'Gradient Dark' toggle is gone -- that look is now
// one of the seven themes, and two controls for one thing is one too many.

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import com.junkfood.seal.R
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.google.android.material.color.DynamicColors
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalDynamicColorSwitch
import com.junkfood.seal.ui.common.LocalPaletteStyleIndex
import com.junkfood.seal.ui.common.LocalSeedColor
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.component.PreferenceSwitchWithDivider
import com.junkfood.seal.ui.page.downloadv2.ActionButton
import com.junkfood.seal.ui.page.downloadv2.CardStateIndicator
import com.junkfood.seal.ui.page.downloadv2.VideoCardV2
import com.junkfood.seal.util.DarkThemePreference.Companion.OFF
import com.junkfood.seal.util.DarkThemePreference.Companion.ON
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.STYLE_MONOCHROME
import com.junkfood.seal.util.STYLE_TONAL_SPOT
import com.junkfood.seal.util.paletteStyles
import com.junkfood.seal.util.toDisplayName
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.a1
import com.kyant.monet.a2
import com.kyant.monet.a3
import io.material.hct.Hct
import java.util.Locale
import kotlinx.coroutines.Job
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.junkfood.seal.ui.common.LocalShowSealTheme
import com.junkfood.seal.ui.theme.LocalTrawlTheme
import com.junkfood.seal.ui.theme.TrawlTheme
import com.junkfood.seal.ui.theme.colorScheme
import com.junkfood.seal.ui.component.TrawlSegmented
import com.junkfood.seal.ui.theme.GlassLevel
import com.junkfood.seal.ui.theme.LocalGlassLevel
import androidx.compose.material.icons.outlined.AutoAwesome
import com.junkfood.seal.ui.theme.LocalDownloadFx
import com.junkfood.seal.ui.theme.LocalMotionLevel
import com.junkfood.seal.ui.theme.MotionLevel
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Title
import com.junkfood.seal.ui.common.LocalHeaderWordmark
import com.junkfood.seal.ui.common.LocalShowMascot
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import com.junkfood.seal.ui.common.LocalAnimStyle
import com.junkfood.seal.ui.common.LocalPinSwitcher
import com.junkfood.seal.ui.common.LocalQuickGear
import com.junkfood.seal.ui.common.LocalQuickHistory
import com.junkfood.seal.ui.component.AnimStyle
import androidx.compose.material.icons.outlined.Animation
import com.junkfood.seal.ui.common.LocalShowIntro
import com.junkfood.seal.ui.common.LocalShowRecentSection
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.ui.bubble.BubbleService
import com.junkfood.seal.ui.common.LocalFloatingBubble
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val ColorList =
    ((4..10) + (1..3)).map { it * 35.0 }.map { Color(Hct.from(it, 40.0, 40.0).toInt()) }

private val DrawableList =
    listOf(R.drawable.sample, R.drawable.sample1, R.drawable.sample2, R.drawable.sample3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(onNavigateBack: () -> Unit, onNavigateTo: (String) -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    val index by remember { mutableIntStateOf(DrawableList.indices.random()) }

    val image by remember(index) { mutableIntStateOf(DrawableList[index]) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.look_and_feel))
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(it)) {
                val downloadState = Task.DownloadState.Running(Job(), "", 0.8f)
                VideoCardV2(
                    modifier = Modifier.padding(18.dp).clearAndSetSemantics {},
                    title = stringResource(R.string.video_title_sample_text),
                    uploader = stringResource(R.string.video_creator_sample_text),
                    thumbnailModel = image,
                    stateIndicator = {
                        CardStateIndicator(modifier = Modifier, downloadState = downloadState)
                    },
                    actionButton = {
                        ActionButton(modifier = Modifier, downloadState = downloadState) {}
                    },
                ) {}
                val pageCount = ColorList.size + 1

                val pagerState =
                    rememberPagerState(
                        initialPage =
                            if (LocalPaletteStyleIndex.current == STYLE_MONOCHROME) pageCount
                            else
                                ColorList.indexOf(Color(LocalSeedColor.current)).run {
                                    if (this == -1) 0 else this
                                }
                    ) {
                        pageCount
                    }

                HorizontalPager(
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { page ->
                    if (page < pageCount - 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtons(ColorList[page])
                        }
                    } else {
                        // ColorButton for Monochrome theme
                        val isSelected =
                            LocalPaletteStyleIndex.current == STYLE_MONOCHROME &&
                                !LocalDynamicColorSwitch.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtonImpl(
                                modifier = Modifier,
                                isSelected = { isSelected },
                                tonalPalettes =
                                    Color.Black.toTonalPalettes(PaletteStyle.Monochrome),
                                onClick = {
                                    PreferenceUtil.switchDynamicColor(enabled = false)
                                    PreferenceUtil.modifyThemeSeedColor(
                                        Color.Black.toArgb(),
                                        STYLE_MONOCHROME,
                                    )
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier =
                        Modifier.clearAndSetSemantics {}
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                        )
                    }
                }
                if (DynamicColors.isDynamicColorAvailable()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_color),
                        description = stringResource(id = R.string.dynamic_color_desc),
                        icon = Icons.Outlined.Colorize,
                        isChecked = LocalDynamicColorSwitch.current,
                        onClick = { PreferenceUtil.switchDynamicColor() },
                    )
                }
                TrawlThemePicker()
                TrawlSettingRow(
                    title = stringResource(R.string.glass_surfaces),
                    description = stringResource(R.string.glass_surfaces_desc),
                ) {
                    TrawlSegmented(
                        options =
                            listOf(
                                GlassLevel.OFF to stringResource(R.string.glass_off),
                                GlassLevel.SUBTLE to stringResource(R.string.glass_subtle),
                                GlassLevel.FULL to stringResource(R.string.glass_full),
                            ),
                        selected = LocalGlassLevel.current,
                        onSelect = { PreferenceUtil.modifyGlassLevel(it) },
                    )
                }
                val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    isChecked = isDarkTheme,
                    description = LocalDarkTheme.current.getDarkThemeDesc(),
                    onChecked = {
                        PreferenceUtil.modifyDarkThemePreference(if (isDarkTheme) OFF else ON)
                    },
                    onClick = { onNavigateTo(Route.DARK_THEME) },
                )
                TrawlSettingRow(
                    title = stringResource(R.string.ambient_motion),
                    description = stringResource(R.string.ambient_motion_desc),
                ) {
                    TrawlSegmented(
                        options =
                            listOf(
                                MotionLevel.OFF to stringResource(R.string.glass_off),
                                MotionLevel.SUBTLE to stringResource(R.string.glass_subtle),
                                MotionLevel.FULL to stringResource(R.string.glass_full),
                            ),
                        selected = LocalMotionLevel.current,
                        onSelect = { PreferenceUtil.modifyMotionLevel(it) },
                    )
                }
                PreferenceSwitch(
                    title = stringResource(R.string.download_effects),
                    description = stringResource(R.string.download_effects_desc),
                    icon = Icons.Outlined.AutoAwesome,
                    isChecked = LocalDownloadFx.current,
                    onClick = { PreferenceUtil.switchDownloadFx() },
                )
                // The switch reflects BOTH the preference and the permission, because a
                // toggle that reads "on" while nothing appears is the interface lying. Tapping
                // it without the permission sends the user to grant it rather than silently
                // storing an intention that cannot take effect.
                val bubbleCtx = LocalContext.current
                // Re-checked on ON_RESUME. The permission is granted in ANOTHER app, so nothing
                // here recomposes on the way back -- which is why the row used to stay "off"
                // until some unrelated setting forced a recomposition.
                var bubbleAllowed by remember {
                    mutableStateOf(BubbleService.canDrawOverlays(bubbleCtx))
                }
                val bubbleOwner = LocalLifecycleOwner.current
                DisposableEffect(bubbleOwner) {
                    val obs = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            bubbleAllowed = BubbleService.canDrawOverlays(bubbleCtx)
                            // Honour an intention that could not take effect at the time.
                            if (bubbleAllowed && PreferenceUtil.AppSettingsStateFlow.value
                                    .floatingBubble
                            ) {
                                BubbleService.start(bubbleCtx)
                            }
                        }
                    }
                    bubbleOwner.lifecycle.addObserver(obs)
                    onDispose { bubbleOwner.lifecycle.removeObserver(obs) }
                }
                // Read outside the click lambda: that lambda is not a composable scope.
                val bubbleOn = LocalFloatingBubble.current
                PreferenceSwitch(
                    title = stringResource(R.string.floating_bubble),
                    description =
                        if (bubbleAllowed) stringResource(R.string.floating_bubble_desc)
                        else stringResource(R.string.grant_overlay_permission),
                    icon = Icons.Outlined.BubbleChart,
                    isChecked = bubbleOn && bubbleAllowed,
                    onClick = {
                        if (!bubbleAllowed) {
                            bubbleCtx.startActivity(
                                Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + bubbleCtx.packageName),
                                    )
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            val next = !bubbleOn
                            PreferenceUtil.switchFloatingBubble(next)
                            if (next) BubbleService.start(bubbleCtx)
                            else BubbleService.stop(bubbleCtx)
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.show_intro),
                    description = stringResource(R.string.show_intro_desc),
                    icon = Icons.Outlined.Animation,
                    isChecked = LocalShowIntro.current,
                    onClick = { PreferenceUtil.switchShowIntro() },
                )
                // SHOW_RECENT_SECTION. Off is a real way to use the app: All links is the actual
                // record, so the home section is a convenience and some people would rather have
                // the space back.
                PreferenceSwitch(
                    title = stringResource(R.string.show_recent_section),
                    description = stringResource(R.string.show_recent_section_desc),
                    icon = Icons.Outlined.History,
                    isChecked = LocalShowRecentSection.current,
                    onClick = { PreferenceUtil.switchShowRecentSection() },
                )
                TrawlSettingRow(
                    title = stringResource(R.string.transition_style),
                    description = stringResource(R.string.transition_style_desc),
                ) {
                    TrawlSegmented(
                        options =
                            listOf(
                                AnimStyle.SIMPLE to stringResource(R.string.transition_simple),
                                AnimStyle.FANCY to stringResource(R.string.transition_fancy),
                            ),
                        selected = LocalAnimStyle.current,
                        onSelect = { PreferenceUtil.modifyAnimStyle(it) },
                    )
                }
                PreferenceSwitch(
                    title = stringResource(R.string.keep_switcher_open),
                    description = stringResource(R.string.keep_switcher_open_desc),
                    icon = Icons.Outlined.PushPin,
                    isChecked = LocalPinSwitcher.current,
                    onClick = { PreferenceUtil.switchPinSwitcher() },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.quick_history),
                    description = stringResource(R.string.quick_history_desc),
                    icon = Icons.Outlined.History,
                    isChecked = LocalQuickHistory.current,
                    onClick = { PreferenceUtil.switchQuickHistory() },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.quick_gear),
                    description = stringResource(R.string.quick_gear_desc),
                    icon = Icons.Outlined.Settings,
                    isChecked = LocalQuickGear.current,
                    onClick = { PreferenceUtil.switchQuickGear() },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.header_wordmark),
                    description = stringResource(R.string.header_wordmark_desc),
                    icon = Icons.Outlined.Title,
                    isChecked = LocalHeaderWordmark.current,
                    onClick = { PreferenceUtil.switchHeaderWordmark() },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.show_mascot),
                    description = stringResource(R.string.show_mascot_desc),
                    icon = Icons.Outlined.Pets,
                    isChecked = LocalShowMascot.current,
                    onClick = { PreferenceUtil.switchShowMascot() },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.show_seal_theme),
                    description = stringResource(R.string.show_seal_theme_desc),
                    icon = Icons.Outlined.Palette,
                    isChecked = LocalShowSealTheme.current,
                    onClick = { PreferenceUtil.switchShowSealTheme() },
                )
                PreferenceItem(
                    title = stringResource(R.string.language),
                    icon = Icons.Outlined.Language,
                    description = Locale.getDefault().toDisplayName(),
                ) {
                    onNavigateTo(Route.LANGUAGES)
                }
            }
        },
    )
}

@Composable
fun RowScope.ColorButtons(color: Color) {
    paletteStyles.subList(STYLE_TONAL_SPOT, STYLE_MONOCHROME).forEachIndexed { index, style ->
        ColorButton(color = color, index = index, tonalStyle = style)
    }
}

@Composable
fun RowScope.ColorButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    index: Int = 0,
    tonalStyle: PaletteStyle = PaletteStyle.TonalSpot,
) {
    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(tonalStyle)) }
    val isSelect =
        !LocalDynamicColorSwitch.current &&
            LocalSeedColor.current == color.toArgb() &&
            LocalPaletteStyleIndex.current == index
    ColorButtonImpl(modifier = modifier, tonalPalettes = tonalPalettes, isSelected = { isSelect }) {
        PreferenceUtil.switchDynamicColor(enabled = false)
        PreferenceUtil.modifyThemeSeedColor(color.toArgb(), index)
    }
}

@Composable
fun RowScope.ColorButtonImpl(
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    tonalPalettes: TonalPalettes,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {},
) {

    val containerSize by animateDpAsState(targetValue = if (isSelected.invoke()) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (isSelected.invoke()) 16.dp else 0.dp)

    Surface(
        modifier =
            modifier
                .padding(4.dp)
                .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
                .weight(1f, false)
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        onClick = onClick,
    ) {
        CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
            val color1 = 80.a1
            val color2 = 90.a2
            val color3 = 60.a3
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(color1) }
                            .align(Alignment.Center)
                ) {
                    Surface(
                        color = color2,
                        modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                    ) {}
                    Surface(
                        color = color3,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                    ) {}
                    Box(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .clip(CircleShape)
                                .size(containerSize)
                                .drawBehind { drawCircle(containerColor) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// --- Trawl theme picker ------------------------------------------------------------------------
// 1:1 with design/v0.1.0-baseline-mockup-ui.html lines 304-309 (.swatches / .swatch) and the
// renderer at line 1411: a 52dp cell, a 44dp chip at radius 13, a 2dp border that is transparent
// until selected, and a hard diagonal split at 55% between the theme's background and its primary.
// The split is what makes two dark themes distinguishable at this size -- a single flat primary
// chip would make Ember and Hearth look identical.

private val SwatchCell = 52.dp
private val SwatchChip = 44.dp
private val SwatchRadius = 13.dp

@Composable
private fun ThemeSwatch(theme: TrawlTheme, selected: Boolean, onClick: () -> Unit) {
    val scheme = remember(theme) { theme.colorScheme() }
    val border by
        animateColorAsState(
            targetValue =
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "swatchBorder",
        )
    Column(
        modifier =
            Modifier.width(SwatchCell)
                .clip(RoundedCornerShape(SwatchRadius))
                .clickable(onClick = onClick)
                .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(SwatchChip)
                    .clip(RoundedCornerShape(SwatchRadius))
                    .background(
                        Brush.linearGradient(
                            0.55f to scheme.background,
                            0.55f to scheme.primary,
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        )
                    )
                    .border(2.dp, border, RoundedCornerShape(SwatchRadius))
        )
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color =
                if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrawlThemePicker() {
    val current = LocalTrawlTheme.current
    val showLegacy = LocalShowSealTheme.current
    val isDark = LocalDarkTheme.current.isDarkTheme()
    val dynamic = LocalDynamicColorSwitch.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.theme),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            // Say plainly when the picker cannot take effect, rather than letting someone tap
            // seven swatches and watch nothing happen.
            text =
                if (!isDark || dynamic) stringResource(R.string.theme_light_mode_note)
                else stringResource(R.string.theme_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TrawlTheme.visible(showLegacy).forEach { theme ->
                ThemeSwatch(theme = theme, selected = theme == current) {
                    PreferenceUtil.modifyTrawlTheme(theme)
                }
            }
        }
    }
}
