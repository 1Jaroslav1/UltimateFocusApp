/**
 * Modal app browser opened from the schedule editor's "Browse all apps"
 * row. Renders the full app list (sorted by weekly usage descending,
 * alphabetical tiebreak) with a leading filter input that case-insensitively
 * matches against the app label.
 *
 * Each row is an AppPickerRow so selection state, label, and weekly usage
 * are rendered identically to the inline top-15 in the editor.
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ultimatefocus.app.ui.components.AppFilterField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ultimatefocus.app.ui.blocking.AppPickerItem

@Composable
fun AppBrowseSheet(
    items: List<AppPickerItem>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else items.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            ),
            modifier = Modifier.testTag("schedule_apps_browse_sheet"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
            ) {
                Text(
                    text = "Select apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                AppFilterField(
                    query = query,
                    onQueryChange = { query = it },
                    testTag = "schedule_apps_filter_field",
                )

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No matches.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .testTag("schedule_apps_browse_empty"),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("schedule_apps_browse_list"),
                        ) {
                            items(filtered, key = { it.packageName }) { item ->
                                AppPickerRow(
                                    item = item,
                                    isSelected = item.packageName in selected,
                                    onToggle = onToggle,
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("schedule_apps_done_button"),
                    ) {
                        Text(text = "Done")
                    }
                }
            }
        }
    }
}
