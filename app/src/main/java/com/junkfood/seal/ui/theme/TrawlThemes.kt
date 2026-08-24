package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The seven Trawl palettes, transcribed from the design contract at
// design/v0.1.0-baseline-mockup-ui.html lines 45-91 (see the token table in
// design/v0.1.0-implementation-spec.html). Values are LITERAL HEX copied from the mockup, never
// re-derived by eye and never generated from a seed: the mockup is the arbiter of what the app
// should look like, so the only faithful translation is to copy the numbers across.
//
// Material's ColorScheme has no slot for several tokens the design uses -- the raised surface,
// the ambient wash stops, the mote colour, and the ok/warn/bad trio -- so those live in
// TrawlTokens beside it rather than being forced into a Material role that means something else.

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A Trawl colour theme.
 *
 * [SEAL_PLUS] is the inherited Seal Plus look, kept deliberately: the fork did not orphan the
 * extras it inherited, so the palette that came with them stays available. It is marked
 * [isLegacy] so the picker can hide it -- see `showSealTheme` in PreferenceUtil -- and its
 * display name is "Seal +" because that is attribution for a look someone else designed, not
 * Trawl branding. The app still titles itself Trawl while wearing it.
 */
enum class TrawlTheme(val id: String, val displayName: String, val isLegacy: Boolean = false) {
    EMBER("ember", "Ember"),
    HEARTH("hearth", "Hearth"),
    GROVE("grove", "Grove"),
    PLUM("plum", "Plum"),
    SNOW("snow", "Snow"),
    SLATE("slate", "Slate"),
    SEAL_PLUS("sealplus", "Seal +", isLegacy = true);

    companion object {
        /** Warm, low-blue dark is the house direction (D-05). */
        val Default = EMBER

        fun fromId(id: String?): TrawlTheme = entries.firstOrNull { it.id == id } ?: Default

        /** The picker's contents. Hiding the legacy theme must not strand anyone already on it. */
        fun visible(showLegacy: Boolean): List<TrawlTheme> =
            if (showLegacy) entries else entries.filterNot { it.isLegacy }
    }
}

/**
 * The design tokens Material has no home for.
 *
 * Kept as a separate [Immutable] holder provided through [LocalTrawlTokens] rather than crammed
 * into unrelated ColorScheme roles, because borrowing e.g. `surfaceTint` to mean "mote colour"
 * makes every later reader guess.
 */
@Immutable
data class TrawlTokens(
    /** The raised chrome surface -- `--surfhigh`. Above surfaceContainer, below a dialog. */
    val surfaceHigh: Color,
    /** The deeper end of the primary ramp -- `--primary2`. Gradients and pressed states. */
    val primaryDeep: Color,
    /** The decorative accent -- `--accent`. Distinct from primary; used for emphasis, not action. */
    val accent: Color,
    /** Glass tint as an opaque colour; the glass modifier supplies the alpha (step 7). */
    val glassTint: Color,
    /** Hairline for glass surfaces -- already carries its own low alpha. */
    val glassLine: Color,
    /** Ambient wash stops, brightest to darkest -- `--a1` / `--a2` / `--a3` (step 8). */
    val ambientTop: Color,
    val ambientMid: Color,
    val ambientLow: Color,
    /** Floating mote colour -- `--mote` (step 8). */
    val mote: Color,
    /** Status trio. Defined once on `.screen` in the mockup, so it is shared by every theme. */
    val ok: Color = Color(0xFF67C98C),
    val bad: Color = Color(0xFFE2685F),
    val warn: Color = Color(0xFFE0A44E),
)

/**
 * One theme's raw tokens, named after the CSS custom properties they came from so a reader can
 * diff this against the mockup line by line without a translation table.
 */
private data class Palette(
    val bg: Long,
    val surface: Long,
    val surfVar: Long,
    val surfCon: Long,
    val surfHigh: Long,
    val primary: Long,
    val primary2: Long,
    val accent: Long,
    val text: Long,
    val text2: Long,
    val outline: Long,
    val onPrimary: Long,
    val glassTint: Long,
    val glassLine: Long,
    val a1: Long,
    val a2: Long,
    val a3: Long,
    val mote: Long,
)

// --- transcribed from the mockup, one block per [data-theme] rule -----------------------------

private val EmberPalette = Palette(
    bg = 0xFF100B08, surface = 0xFF1A1310, surfVar = 0xFF221913, surfCon = 0xFF2A1E17,
    surfHigh = 0xFF34261C, primary = 0xFFE0925A, primary2 = 0xFFC96F3F, accent = 0xFFD9A05B,
    text = 0xFFF6EDE5, text2 = 0xFFAE9C8E, outline = 0xFF3B2C22, onPrimary = 0xFF1A1008,
    glassTint = 0xFFFFE8D0, glassLine = 0x1CFFD6B4,
    a1 = 0xFF4A2A14, a2 = 0xFF3A1D10, a3 = 0xFF241408, mote = 0xFFF0B87A,
)

private val HearthPalette = Palette(
    bg = 0xFF0D0906, surface = 0xFF171009, surfVar = 0xFF1F160D, surfCon = 0xFF271C11,
    surfHigh = 0xFF33251A, primary = 0xFFF0A868, primary2 = 0xFFD4783F, accent = 0xFFE8C07A,
    text = 0xFFF8EFE4, text2 = 0xFFB5A18C, outline = 0xFF3A2A1B, onPrimary = 0xFF180F06,
    glassTint = 0xFFFFE4C4, glassLine = 0x21FFCEA0,
    a1 = 0xFF5C3316, a2 = 0xFF47230E, a3 = 0xFF2A1608, mote = 0xFFFFC98A,
)

private val GrovePalette = Palette(
    bg = 0xFF08100C, surface = 0xFF101A14, surfVar = 0xFF16231B, surfCon = 0xFF1C2C22,
    surfHigh = 0xFF24382B, primary = 0xFF7FC99A, primary2 = 0xFF5AA478, accent = 0xFFA8D9BC,
    text = 0xFFE6F2EA, text2 = 0xFF93AA9C, outline = 0xFF24382B, onPrimary = 0xFF06150D,
    glassTint = 0xFFD6FFE6, glassLine = 0x1FBEF0D4,
    a1 = 0xFF153A26, a2 = 0xFF0F2C1D, a3 = 0xFF0A1C13, mote = 0xFF9FE0B8,
)

private val PlumPalette = Palette(
    bg = 0xFF0F0810, surface = 0xFF1A101C, surfVar = 0xFF231625, surfCon = 0xFF2C1D2F,
    surfHigh = 0xFF38263B, primary = 0xFFC88BC0, primary2 = 0xFFA566A0, accent = 0xFFDDA9D6,
    text = 0xFFF2E8F1, text2 = 0xFFAC94AB, outline = 0xFF38263B, onPrimary = 0xFF170A18,
    glassTint = 0xFFFAE0F8, glassLine = 0x1FE8C4E4,
    a1 = 0xFF3D1C3F, a2 = 0xFF2E142F, a3 = 0xFF1C0D1E, mote = 0xFFE0A8D8,
)

private val SnowPalette = Palette(
    bg = 0xFF080B12, surface = 0xFF101725, surfVar = 0xFF161F30, surfCon = 0xFF1B2639,
    surfHigh = 0xFF223046, primary = 0xFF7FB4E8, primary2 = 0xFF5E93CC, accent = 0xFF9FD0F0,
    text = 0xFFE9F1F9, text2 = 0xFF8FA2B9, outline = 0xFF26344A, onPrimary = 0xFF08131F,
    glassTint = 0xFFD6EAFF, glassLine = 0x21C6E4FF,
    a1 = 0xFF16324F, a2 = 0xFF122840, a3 = 0xFF0B1727, mote = 0xFFA8D4F5,
)

private val SlatePalette = Palette(
    bg = 0xFF0C0D0F, surface = 0xFF15171A, surfVar = 0xFF1C1F23, surfCon = 0xFF23272C,
    surfHigh = 0xFF2D3238, primary = 0xFFB9C2CC, primary2 = 0xFF8F99A4, accent = 0xFFD2DAE2,
    text = 0xFFE9ECF0, text2 = 0xFF969DA6, outline = 0xFF2D3238, onPrimary = 0xFF0C0D0F,
    glassTint = 0xFFE8EEF5, glassLine = 0x1CDCE6F0,
    a1 = 0xFF242A31, a2 = 0xFF1C2127, a3 = 0xFF131619, mote = 0xFFC6CFD8,
)

// The inherited look. These values are identical to GradientDarkColors -- the mockup's sealplus
// block was transcribed FROM that file, so the two agreeing is the point, not a coincidence.
private val SealPlusPalette = Palette(
    bg = 0xFF0A0A0F, surface = 0xFF14141F, surfVar = 0xFF1A1A2E, surfCon = 0xFF1E1E2F,
    surfHigh = 0xFF25253A, primary = 0xFF5B47E5, primary2 = 0xFF8B5CF6, accent = 0xFFA855F7,
    text = 0xFFFAFAFA, text2 = 0xFFA9A9BC, outline = 0xFF35354A, onPrimary = 0xFFFFFFFF,
    glassTint = 0xFFFFFFFF, glassLine = 0x1AFFFFFF,
    a1 = 0xFF191434, a2 = 0xFF2A1140, a3 = 0xFF12102A, mote = 0xFF8B5CF6,
)

private val TrawlTheme.palette: Palette
    get() = when (this) {
        TrawlTheme.EMBER -> EmberPalette
        TrawlTheme.HEARTH -> HearthPalette
        TrawlTheme.GROVE -> GrovePalette
        TrawlTheme.PLUM -> PlumPalette
        TrawlTheme.SNOW -> SnowPalette
        TrawlTheme.SLATE -> SlatePalette
        TrawlTheme.SEAL_PLUS -> SealPlusPalette
    }

/**
 * The palette as a Material [ColorScheme].
 *
 * The mapping is one place on purpose. `--surfhigh` deliberately fills BOTH
 * surfaceContainerHigh and surfaceContainerHighest: the design has one raised step, and inventing
 * a second by lightening it would put a colour on screen that appears nowhere in the contract.
 */
fun TrawlTheme.colorScheme(): ColorScheme = with(palette) {
    darkColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primary2),
        onPrimaryContainer = Color(text),
        inversePrimary = Color(primary2),
        secondary = Color(primary2),
        onSecondary = Color(onPrimary),
        secondaryContainer = Color(surfCon),
        onSecondaryContainer = Color(text),
        tertiary = Color(accent),
        onTertiary = Color(onPrimary),
        tertiaryContainer = Color(surfHigh),
        onTertiaryContainer = Color(text),
        background = Color(bg),
        onBackground = Color(text),
        surface = Color(surface),
        onSurface = Color(text),
        surfaceVariant = Color(surfVar),
        onSurfaceVariant = Color(text2),
        surfaceTint = Color(primary),
        inverseSurface = Color(text),
        inverseOnSurface = Color(bg),
        outline = Color(outline),
        outlineVariant = Color(outline),
        scrim = Color(0xFF000000),
        surfaceBright = Color(surfHigh),
        surfaceDim = Color(bg),
        surfaceContainerLowest = Color(bg),
        surfaceContainerLow = Color(surfVar),
        surfaceContainer = Color(surfCon),
        surfaceContainerHigh = Color(surfHigh),
        surfaceContainerHighest = Color(surfHigh),
        error = Color(0xFFE2685F),
        onError = Color(0xFF1A0806),
        errorContainer = Color(0xFF4A1F1B),
        onErrorContainer = Color(text),
    )
}

/** The tokens Material cannot carry. */
fun TrawlTheme.tokens(): TrawlTokens = with(palette) {
    TrawlTokens(
        surfaceHigh = Color(surfHigh),
        primaryDeep = Color(primary2),
        accent = Color(accent),
        glassTint = Color(glassTint),
        glassLine = Color(glassLine),
        ambientTop = Color(a1),
        ambientMid = Color(a2),
        ambientLow = Color(a3),
        mote = Color(mote),
    )
}

/**
 * Defaulted to Ember rather than left null, so a composable that reads tokens outside a
 * [SealTheme] renders in the house style instead of crashing or rendering black-on-black.
 */
val LocalTrawlTokens = staticCompositionLocalOf { TrawlTheme.Default.tokens() }

val LocalTrawlTheme = staticCompositionLocalOf { TrawlTheme.Default }
