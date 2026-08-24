package com.junkfood.seal.ui.common

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: publishes the selected Trawl palette and its extended tokens to the tree.

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.ui.theme.DEFAULT_SEED_COLOR
import com.junkfood.seal.ui.theme.FixedColorRoles
import com.junkfood.seal.ui.component.AnimStyle
import com.junkfood.seal.ui.theme.LocalDownloadFx
import com.junkfood.seal.ui.theme.LocalGlassLevel
import com.junkfood.seal.ui.theme.LocalMotionLevel
import com.junkfood.seal.ui.theme.LocalTrawlTheme
import com.junkfood.seal.ui.theme.LocalTrawlTokens
import com.junkfood.seal.ui.theme.tokens
import com.junkfood.seal.util.DarkThemePreference
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.paletteStyles
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes

val LocalDarkTheme = compositionLocalOf { DarkThemePreference() }
val LocalSeedColor = compositionLocalOf { DEFAULT_SEED_COLOR }
val LocalWindowWidthState = staticCompositionLocalOf { WindowWidthSizeClass.Compact }
val LocalDynamicColorSwitch = compositionLocalOf { false }
val LocalPaletteStyleIndex = compositionLocalOf { 0 }
val LocalGradientDarkMode = compositionLocalOf { false }
/** Whether the inherited palette is offered in the picker. See PreferenceUtil. */
val LocalShowSealTheme = compositionLocalOf { true }

/** Home screen preferences, published so components need no parameter threading. */
val LocalHeaderWordmark = compositionLocalOf { true }
val LocalShowMascot = compositionLocalOf { true }
val LocalFastDownload = compositionLocalOf { true }
val LocalRememberedQuality = compositionLocalOf { "1080p" }

/** Navigation preferences. */
val LocalAnimStyle = compositionLocalOf { AnimStyle.Default }
val LocalPinSwitcher = compositionLocalOf { false }
val LocalQuickGear = compositionLocalOf { false }
val LocalQuickHistory = compositionLocalOf { true }
val LocalShowIntro = compositionLocalOf { true }
val LocalFloatingBubble = compositionLocalOf { true }

val LocalFixedColorRoles = staticCompositionLocalOf {
    FixedColorRoles.fromColorSchemes(
        lightColors = lightColorScheme(),
        darkColors = darkColorScheme(),
    )
}

@Composable
fun SettingsProvider(windowWidthSizeClass: WindowWidthSizeClass, content: @Composable () -> Unit) {
    PreferenceUtil.AppSettingsStateFlow.collectAsState().value.run {
        val tonalPalettes =
            if (isDynamicColorEnabled && Build.VERSION.SDK_INT >= 31)
                dynamicDarkColorScheme(LocalContext.current).toTonalPalettes()
            else
                Color(seedColor)
                    .toTonalPalettes(
                        paletteStyles.getOrElse(paletteStyleIndex) { PaletteStyle.TonalSpot }
                    )

        CompositionLocalProvider(
            LocalDarkTheme provides darkTheme,
            LocalSeedColor provides seedColor,
            LocalPaletteStyleIndex provides paletteStyleIndex,
            LocalTonalPalettes provides tonalPalettes,
            LocalWindowWidthState provides windowWidthSizeClass,
            LocalDynamicColorSwitch provides isDynamicColorEnabled,
            LocalGradientDarkMode provides isGradientDarkModeEnabled,
            LocalTrawlTheme provides trawlTheme,
            LocalTrawlTokens provides trawlTheme.tokens(),
            LocalShowSealTheme provides showSealTheme,
            LocalGlassLevel provides glassLevel,
            LocalMotionLevel provides motionLevel,
            LocalDownloadFx provides downloadFx,
            LocalHeaderWordmark provides headerWordmark,
            LocalShowMascot provides showMascot,
            LocalFastDownload provides fastDownload,
            LocalRememberedQuality provides rememberedQuality,
            LocalAnimStyle provides animStyle,
            LocalPinSwitcher provides pinSwitcher,
            LocalQuickGear provides quickGear,
            LocalQuickHistory provides quickHistory,
            LocalShowIntro provides showIntro,
            LocalFloatingBubble provides floatingBubble,
            content = content,
        )
    }
}
