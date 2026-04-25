/**
 * Confirmation step shown before "Block by tag" commits. Rendered in the
 * launcher's default app modal style (black surface, white border, in-line
 * header) for visual consistency with the long-press app menu.
 *
 * Displays the tag name in the header (with the tag's colored dot) and a
 * scrollable list of the affected apps by their human-readable labels —
 * "Telegram" rather than "org.telegram.messenger". Falls back to packageName
 * if a label could not be resolved (e.g. an app uninstalled between the
 * preview lookup and dialog render).
 *
 * Eight BlockDuration buttons live below in a FlowRow; choosing one commits
 * the block and dismisses. Disabled when there are no apps to block.
 *
 * Mirrors the "warning" tone of the standard BlockDurationPickerDialog used
 * for single-app blocks.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.ui.components.DefaultAppModal
import com.ultimatefocus.app.ui.components.DefaultModalAction

@Composable
fun BlockTagPreviewDialog(
    tag: Tag,
    affectedApps: List<BlockTagPreviewApp>,
    onConfirm: (BlockDuration) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultAppModal(
        header = "Block tag '${tag.name}'",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("block_tag_preview_dialog"),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = tag.color.toComposeColor(), shape = CircleShape),
                )
                Spacer(modifier = Modifier.size(8.dp))
                val countLabel = when (affectedApps.size) {
                    0 -> "No apps are tagged with '${tag.name}'."
                    1 -> "1 app will be blocked:"
                    else -> "${affectedApps.size} apps will be blocked:"
                }
                Text(
                    text = countLabel,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }

            if (affectedApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("block_tag_preview_list"),
                ) {
                    items(items = affectedApps, key = { it.packageName }) { app ->
                        Text(
                            text = app.label,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .testTag("block_tag_preview_app_${app.packageName}"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pick a duration. Blocks cannot be undone.",
                fontSize = 13.sp,
                color = Color.White,
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
                        enabled = affectedApps.isNotEmpty(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.LightGray,
                        ),
                        modifier = Modifier.testTag("block_tag_duration_${duration.label}"),
                    ) {
                        Text(text = duration.label, fontSize = 12.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White, thickness = 1.dp)

        DefaultModalAction(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.testTag("block_tag_cancel_button"),
        )
    }
}
