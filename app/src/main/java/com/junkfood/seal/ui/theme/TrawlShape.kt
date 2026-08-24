package com.junkfood.seal.ui.theme

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// Corner radii read straight off design/v0.1.0-baseline-mockup-ui.html, mapped to the Material
// slots by which components actually use them rather than by guessing at a geometric ramp:
//
//   .qchip     9    .seg / .thumb  12    .searchbar / .lrow  16
//   .card      18   .qstrip        20    .urlbar             26
//   .sheet     30   pills          99 (fully rounded)
//
// Material only has five slots and the design uses nine radii, so the four that do not fit are
// named constants below. That is on purpose: a component that needs 26dp should reference
// TrawlShape.UrlBar, not write `RoundedCornerShape(26.dp)` and become a number nobody can trace
// back to the contract.

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object TrawlShape {
    /** Small inline chips -- .qchip / .chip / .lact. */
    val Chip = RoundedCornerShape(11.dp)
    /** Icon buttons and launcher tiles -- .go2 / .launcher. */
    val Tile = RoundedCornerShape(14.dp)
    /** The quick-tools strip -- .qstrip. */
    val ToolStrip = RoundedCornerShape(20.dp)
    /** The URL bar -- .urlbar. */
    val UrlBar = RoundedCornerShape(26.dp)
    /** The focused URL field -- .urlfield. */
    val UrlField = RoundedCornerShape(32.dp)
    /** Anything the design draws as a pill: FAST pill, toggles, grabber, status bars. */
    val Pill = RoundedCornerShape(percent = 50)
}

val TrawlShapes =
    Shapes(
        extraSmall = RoundedCornerShape(9.dp),   // .qchip
        small = RoundedCornerShape(12.dp),       // .seg, .thumb
        medium = RoundedCornerShape(16.dp),      // .searchbar, .lrow
        large = RoundedCornerShape(18.dp),       // .card, .active
        extraLarge = RoundedCornerShape(30.dp),  // .sheet
    )
