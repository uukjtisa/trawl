@file:OptIn(ExperimentalTextApi::class)

package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The two bundled faces and the type scale, from design/v0.1.0-baseline-mockup-ui.html
// (--font / --display at lines 15-16, sizes throughout).
//
// WHY BUNDLED, NOT DOWNLOADABLE: the target device has no Google Play Services, so Android's
// downloadable-font provider cannot resolve. Requested that way, every string would silently
// render in Roboto and the design would be gone with no error to notice. res/font is the only
// mechanism that works on a device without GMS.
//
// WHY VARIABLE: the mockup uses seven body weights -- 450, 500, 550, 600, 650, 700, 800. Static
// instances would snap each to the nearest of four and quietly flatten the design's half-steps.
// The bundled files keep their `wght` axis (all other axes were pinned at build time, see the
// prep script recorded in DECISIONS D-14) so those weights are exact on API 26+.
//
// ON API 24-25 variable axes are unsupported and both faces render at their default instance.
// That is a deliberate, non-crashing degradation: minSdk is 24, but the design targets modern
// devices and a slightly-wrong weight is a far better failure than a missing font.

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R

/**
 * Declares one variable font at [weight].
 *
 * Weights the design uses that Compose has no named constant for (450, 550, 650) are built
 * with the numeric constructor. Declaring them explicitly is what lets a TextStyle asking for
 * FontWeight(550) match exactly instead of being resolved to the nearest hundred.
 *
 * `variationSettings` is what makes the axis actually move; without it every entry would render
 * at the file's default instance and the family would look like a single weight.
 */
private fun variableFont(resId: Int, weight: FontWeight) =
    Font(
        resId = resId,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

/**
 * Body face. Every weight the mockup asks for is declared, including the half-steps (450/550/650)
 * that a four-weight static family could not express.
 */
val InterFamily =
    FontFamily(
        variableFont(R.font.inter, FontWeight.W400),
        variableFont(R.font.inter, FontWeight(450)),
        variableFont(R.font.inter, FontWeight.W500),
        variableFont(R.font.inter, FontWeight(550)),
        variableFont(R.font.inter, FontWeight.W600),
        variableFont(R.font.inter, FontWeight(650)),
        variableFont(R.font.inter, FontWeight.W700),
        variableFont(R.font.inter, FontWeight.W800),
    )

/** Display face -- the wordmark, page headings and the app bar brand. Only 600/700 are used. */
val FrauncesFamily =
    FontFamily(
        variableFont(R.font.fraunces, FontWeight.W600),
        variableFont(R.font.fraunces, FontWeight.W700),
    )

/**
 * Sizes are the mockup's px read as sp.
 *
 * The frame is 393px wide with a 9px drawn bezel, so the screen itself is 375px; the bezel is an
 * artifact of rendering a phone inside a browser, not part of the canvas. Treating 1px as 1dp
 * against a ~393dp reference phone is the mapping that keeps the numbers meaningful -- rescaling
 * by the specific test device's 423dp would bake one handset into the type scale.
 */
object TrawlType {
    val Wordmark = 54.sp      // intro / brand lockup
    val PageTitle = 26.sp     // .page-head h1
    val AppBarBrand = 20.sp   // .appbar .brand
    val SectionTitle = 15.sp
    val Body = 13.5.sp
    val BodyTight = 12.5.sp
    val Label = 11.5.sp
    val Caption = 10.5.sp
    val Micro = 9.5.sp
}

private fun TextStyle.body() =
    copy(
        fontFamily = InterFamily,
        lineBreak = LineBreak.Paragraph,
        textDirection = TextDirection.Content,
    )

private fun TextStyle.display() =
    copy(fontFamily = FrauncesFamily, textDirection = TextDirection.Content)

/**
 * Fraunces carries display, headline and titleLarge -- the places the design uses a serif. Every
 * other role is Inter. titleMedium and below stay on the body face on purpose: a serif at 14sp
 * inside a list row reads as a rendering bug, not as character.
 */
val TrawlTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.display().copy(fontSize = TrawlType.Wordmark),
            displayMedium = displayMedium.display(),
            displaySmall = displaySmall.display(),
            headlineLarge = headlineLarge.display().copy(fontSize = TrawlType.PageTitle),
            headlineMedium = headlineMedium.display(),
            headlineSmall = headlineSmall.display(),
            titleLarge =
                titleLarge.display().copy(
                    fontSize = TrawlType.AppBarBrand,
                    fontWeight = FontWeight.W600,
                ),
            titleMedium = titleMedium.body(),
            titleSmall = titleSmall.body(),
            bodyLarge = bodyLarge.body(),
            bodyMedium = bodyMedium.body(),
            bodySmall = bodySmall.body(),
            labelLarge = labelLarge.body(),
            labelMedium = labelMedium.body(),
            labelSmall = labelSmall.body(),
        )
    }
