/**
 * Single-row renderer shared between the schedule editor's inline top-15
 * picker and the "Browse all apps" filter sheet. Shows a checkbox, the
 * app's label, and a trailing weekly usage label so users have evidence of
 * which apps they actually distract themselves with.
 *
 * testTags:
 *   schedule_app_row_<package>            — the row container, click-to-toggle
 *   schedule_app_checkbox_<package>       — checkbox state
 *   schedule_app_usage_<package>          — trailing usage text
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.ui.blocking.AppPickerItem
import com.ultimatefocus.app.util.UsageDurationFormatter

@Composable
fun AppPickerRow(
    item: AppPickerItem,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(item.packageName) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("schedule_app_row_${item.packageName}"),
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle(item.packageName) },
            modifier = Modifier.testTag("schedule_app_checkbox_${item.packageName}"),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = UsageDurationFormatter.formatWeeklyUsage(item.weeklyUsageMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("schedule_app_usage_${item.packageName}"),
        )
    }
}
