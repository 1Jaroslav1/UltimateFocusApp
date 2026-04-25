/**
 * Modal multi-select picker used by BlockingScheduleEditorScreen to assign
 * apps to a schedule. Reuses AppRepository.getInstalledApps()-derived data
 * passed in from the editor's ViewModel so this composable stays a stateless
 * leaf and instrumented tests can drive it directly.
 *
 * Each row's testTag (`schedule_app_row_<packageName>`) lets tests toggle a
 * specific package without depending on installed-app order.
 *
 * Covers: ULT-24
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ultimatefocus.app.domain.model.AppInfo

@Composable
fun AppMultiSelectSheet(
    apps: List<AppInfo>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            ),
            modifier = Modifier.testTag("schedule_apps_sheet"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
            ) {
                Text(
                    text = "Select apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(app.packageName) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("schedule_app_row_${app.packageName}"),
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggle(app.packageName) },
                                modifier = Modifier.testTag("schedule_app_checkbox_${app.packageName}"),
                            )
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
