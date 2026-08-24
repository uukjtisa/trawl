package com.junkfood.seal.ui.component

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The mockup's `.seg` control, from design/v0.1.0-baseline-mockup-ui.html lines 311-312:
//
//   .seg      display:flex; margin-top:11px; border:1px solid var(--outline);
//             border-radius:12px; overflow:hidden
//   .seg div  flex:1; text-align:center; padding:9px 0; font-size:12.5px;
//             font-weight:550; color:var(--text2)
//   .seg div.on   background:var(--primary); color:var(--onprimary)
//
// Built once because the design uses it three times -- Glass surfaces, Ambient motion, and the
// transition style in step 11. Three hand-rolled copies would drift, and the third would be the
// one that ends up slightly wrong.
//
// Deliberately NOT Material3's SegmentedButton: that component draws its own container, its own
// selected-state check icon and its own corner treatment, none of which match the contract. It
// would have to be fought at every property, which is slower than 40 lines.

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SegShape = RoundedCornerShape(12.dp)

@Composable
private fun RowScope.Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by
        animateColorAsState(
            targetValue =
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "segBg",
        )
    val fg by
        animateColorAsState(
            targetValue =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "segFg",
        )
    Text(
        text = label,
        textAlign = TextAlign.Center,
        fontSize = 12.5.sp,
        // 550 is one of the design's half-steps; the bundled variable font can hit it exactly.
        fontWeight = FontWeight(550),
        color = fg,
        maxLines = 1,
        modifier =
            Modifier.weight(1f)
                .background(bg)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 9.dp),
    )
}

/**
 * A single-choice strip of equal-width options.
 *
 * [options] is (value, label). Selection is by value equality, so callers pass enum entries
 * directly rather than tracking indices -- an index is one refactor away from selecting the
 * wrong thing.
 */
@Composable
fun <T> TrawlSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.fillMaxWidth()
                .padding(top = 11.dp)
                .clip(SegShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, SegShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (value, label) ->
            Segment(label = label, selected = value == selected) { onSelect(value) }
        }
    }
}
