package com.junkfood.seal.ui.theme

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: SealTheme now applies one of Trawl's seven palettes in dark mode and
// publishes the extended design tokens Material has no slot for. The inherited
// gradient branch is kept for the light/dynamic path but is no longer the default.

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.google.android.material.color.MaterialColors
import com.junkfood.seal.ui.common.LocalFixedColorRoles
import com.junkfood.seal.ui.common.LocalDynamicColorSwitch
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.dynamicColorScheme

fun Color.applyOpacity(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.62f)
}

@Composable
@ReadOnlyComposable
fun Color.harmonizeWith(other: Color) =
    Color(MaterialColors.harmonize(this.toArgb(), other.toArgb()))

@Composable
@ReadOnlyComposable
fun Color.harmonizeWithPrimary(): Color =
    this.harmonizeWith(other = MaterialTheme.colorScheme.primary)

@Composable
fun SealTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isHighContrastModeEnabled: Boolean = false,
    isGradientDarkEnabled: Boolean = LocalGradientDarkMode.current,
    trawlTheme: TrawlTheme = LocalTrawlTheme.current,
    isDynamicColorEnabled: Boolean = LocalDynamicColorSwitch.current,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    LaunchedEffect(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (darkTheme) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        }
    }

    // Trawl's palettes are DARK palettes -- every screen in the design contract is dark, and
    // there is no light variant to transcribe. So they apply in dark mode only; in light mode the
    // inherited Monet scheme is still the honest answer, because forcing these values onto a light
    // background would put unreadable text on screen rather than a different look.
    //
    // Dynamic colour, when the user has switched it on, wins over the picker. It is the more
    // specific request: "use my wallpaper" cannot be honoured and overridden at the same time.
    val useTrawlPalette = darkTheme && !isDynamicColorEnabled

    val colorScheme =
        if (useTrawlPalette) {
            trawlTheme.colorScheme().run {
                if (isHighContrastModeEnabled)
                    copy(
                        surface = Color.Black,
                        background = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = surfaceContainerLowest,
                        surfaceContainer = surfaceContainerLow,
                        surfaceContainerHigh = surfaceContainerLow,
                        surfaceContainerHighest = surfaceContainer,
                    )
                else this
            }
        } else {
            dynamicColorScheme(!darkTheme).run {
                when {
                    isGradientDarkEnabled && darkTheme ->
                        copy(
                            primary = GradientDarkColors.GradientPrimaryEnd,
                            onPrimary = GradientDarkColors.OnPrimary,
                            primaryContainer = GradientDarkColors.GradientPrimaryStart,
                            onPrimaryContainer = GradientDarkColors.OnPrimary,
                            secondary = GradientDarkColors.GradientSecondaryEnd,
                            onSecondary = GradientDarkColors.OnPrimary,
                            secondaryContainer = GradientDarkColors.GradientSecondaryStart,
                            onSecondaryContainer = GradientDarkColors.OnPrimary,
                            tertiary = GradientDarkColors.GradientAccentEnd,
                            onTertiary = GradientDarkColors.OnPrimary,
                            tertiaryContainer = GradientDarkColors.GradientAccentStart,
                            onTertiaryContainer = GradientDarkColors.OnPrimary,
                            background = GradientDarkColors.Background,
                            onBackground = GradientDarkColors.OnBackground,
                            surface = GradientDarkColors.Surface,
                            onSurface = GradientDarkColors.OnSurface,
                            surfaceVariant = GradientDarkColors.SurfaceVariant,
                            onSurfaceVariant = GradientDarkColors.OnSurface,
                            surfaceContainer = GradientDarkColors.SurfaceContainer,
                            surfaceContainerLow = GradientDarkColors.SurfaceContainerLow,
                            surfaceContainerHigh = GradientDarkColors.SurfaceContainerHigh,
                            surfaceContainerLowest = GradientDarkColors.Background,
                            surfaceContainerHighest = GradientDarkColors.SurfaceContainerHigh,
                            outline = GradientDarkColors.GlassWhiteBorder,
                            outlineVariant = GradientDarkColors.GlassSurface,
                        )
                    isHighContrastModeEnabled && darkTheme ->
                        copy(
                            surface = Color.Black,
                            background = Color.Black,
                            surfaceContainerLowest = Color.Black,
                            surfaceContainerLow = surfaceContainerLowest,
                            surfaceContainer = surfaceContainerLow,
                            surfaceContainerHigh = surfaceContainerLow,
                            surfaceContainerHighest = surfaceContainer,
                        )
                    else -> this
                }
            }
        }

    val textStyle =
        LocalTextStyle.current.copy(
            lineBreak = LineBreak.Paragraph,
            textDirection = TextDirection.Content,
        )

    val tonalPalettes = LocalTonalPalettes.current

    CompositionLocalProvider(
        LocalFixedColorRoles provides FixedColorRoles.fromTonalPalettes(tonalPalettes),
        LocalTextStyle provides textStyle,
        LocalTrawlTokens provides trawlTheme.tokens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            // Trawl's own scale and radii. The inherited Typography/Shapes were Material
            // defaults, which is to say Roboto and a generic corner ramp.
            typography = TrawlTypography,
            shapes = TrawlShapes,
            content = content,
        )
    }
}
