/**
 * Confirmation dialog before deleting a tag, rendered in the launcher's
 * default app modal style. Caller passes the count of apps that will be
 * unassigned (computed from the cross-ref table) so the user understands
 * the blast radius before confirming.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimatefocus.app.ui.components.DefaultAppModal
import com.ultimatefocus.app.ui.components.DefaultModalAction

@Composable
fun TagDeleteConfirmDialog(
    tagName: String,
    affectedAppCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultAppModal(
        header = "Delete tag '$tagName'?",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("tag_delete_confirm_dialog"),
    ) {
        val suffix = when (affectedAppCount) {
            0 -> "This tag is not assigned to any apps."
            1 -> "It will be removed from 1 app."
            else -> "It will be removed from $affectedAppCount apps."
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Text(
                text = suffix,
                fontSize = 14.sp,
                color = Color.White,
            )
        }

        HorizontalDivider(color = Color.White, thickness = 1.dp)

        DefaultModalAction(
            text = "Delete",
            onClick = onConfirm,
            modifier = Modifier.testTag("tag_delete_confirm_button"),
        )
        DefaultModalAction(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.testTag("tag_delete_cancel_button"),
        )
    }
}
