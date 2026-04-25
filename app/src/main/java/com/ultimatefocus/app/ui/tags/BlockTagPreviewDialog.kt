/**
 * Confirmation step shown before "Block by tag" commits. Displays the
 * tag name with its colored dot, a count of affected apps and the apps'
 * package names, and lets the user pick a BlockDuration. Choosing a
 * duration commits the block and dismisses the dialog. Mirrors the
 * "warning" tone of the standard BlockDurationPickerDialog used for
 * single-app blocks.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.Tag

@Composable
fun BlockTagPreviewDialog(
    tag: Tag,
    affectedPackages: List<String>,
    onConfirm: (BlockDuration) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("block_tag_preview_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = tag.color.toComposeColor(), shape = CircleShape),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Block tag '${tag.name}'")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val countLabel = when (affectedPackages.size) {
                    0 -> "No apps are tagged with '${tag.name}'."
                    1 -> "1 app will be blocked:"
                    else -> "${affectedPackages.size} apps will be blocked:"
                }
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (affectedPackages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("block_tag_preview_list"),
                    ) {
                        items(items = affectedPackages, key = { it }) { pkg ->
                            Text(
                                text = pkg,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pick a duration. Blocks cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BlockDuration.entries.forEach { duration ->
                        Button(
                            onClick = { onConfirm(duration) },
                            enabled = affectedPackages.isNotEmpty(),
                            modifier = Modifier.testTag("block_tag_duration_${duration.label}"),
                        ) {
                            Text(text = duration.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("block_tag_cancel_button"),
            ) {
                Text(text = "Cancel")
            }
        },
    )
}
