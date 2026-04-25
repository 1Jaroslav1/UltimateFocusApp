/**
 * Confirmation dialog before deleting a tag. Caller passes the count of apps
 * that will be unassigned (computed from the cross-ref table) so the user
 * understands the blast radius before confirming.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun TagDeleteConfirmDialog(
    tagName: String,
    affectedAppCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tag_delete_confirm_dialog"),
        title = { Text(text = "Delete tag '$tagName'?") },
        text = {
            val suffix = when (affectedAppCount) {
                0 -> "This tag is not assigned to any apps."
                1 -> "It will be removed from 1 app."
                else -> "It will be removed from $affectedAppCount apps."
            }
            Text(text = suffix)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("tag_delete_confirm_button"),
            ) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tag_delete_cancel_button"),
            ) {
                Text(text = "Cancel")
            }
        },
    )
}
