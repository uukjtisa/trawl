package com.junkfood.seal.ui.page.settings.appearance

// NEW FILE (Trawl project, 2026-08-25). Not inherited from upstream.
//
// The mockup's `.setrow`: a label, a hint, and a control underneath. Upstream's PreferenceItem
// family all end in a trailing switch or a chevron, so none of them can host a segmented strip.

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrawlSettingRow(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        content()
    }
}
