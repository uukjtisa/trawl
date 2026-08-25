package com.junkfood.seal.ui.page.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.theme.LocalTrawlTokens

/**
 * Shared dark-mode palette for the "More Tools" family of screens (Batch URL Import,
 * Video Info Download, etc). Keeping this in one place means every tool page looks and
 * feels consistent, and any future palette tweak only needs to happen here.
 */
/** The tools' accent gradient, from the active theme rather than a fixed purple pair. */
val ToolGradientBrush: Brush
    @Composable get() =
        Brush.horizontalGradient(
            listOf(MaterialTheme.colorScheme.primary, LocalTrawlTokens.current.accent)
        )

/**
 * Resolved set of colors for a tool page: [ToolColors] in dark mode, or the app's
 * Material theme roles in light mode. Every tool screen should derive its colors from
 * this so behaviour stays consistent with the rest of the app.
 */
@Immutable
data class ToolPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chipSelectedBg: Color,
    val chipSelectedBorder: Color,
    val chipUnselectedBorder: Color,
    val warning: Color,
    val error: Color,
    val success: Color,
    val isDarkMode: Boolean,
)

@Composable
fun rememberToolPalette(): ToolPalette {
    val isDarkMode = LocalDarkTheme.current.isDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    // BOTH branches derive from the theme now. The dark branch used to be a hardcoded
    // near-black + #7C4DFF purple set, which is why every More Tools screen stayed Seal Plus
    // violet while the rest of the app was Ember -- and why fixing it screen by screen would
    // have been five chances to miss one.
    //
    // ok / warn / bad still come from TrawlTokens rather than the ColorScheme, because Material
    // has no role for "this succeeded" and inventing one would mean each page picking its own.
    val tokens = LocalTrawlTokens.current
    return ToolPalette(
        background = colorScheme.background,
        surface = colorScheme.surface,
        surfaceVariant = colorScheme.surfaceVariant,
        border = colorScheme.outline,
        primary = colorScheme.primary,
        textPrimary = colorScheme.onSurface,
        textSecondary = colorScheme.onSurfaceVariant,
        chipSelectedBg = colorScheme.primary.copy(alpha = 0.14f),
        chipSelectedBorder = colorScheme.primary,
        chipUnselectedBorder = colorScheme.outline,
        warning = tokens.warn,
        error = tokens.bad,
        success = tokens.ok,
        isDarkMode = isDarkMode,
    )
}
