package com.ultimatefocus.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ultimatefocus.app.domain.model.AppInfo

@Composable
fun FavoriteContextMenuDialog(
    app: AppInfo,
    onToggleFavorite: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = null,
        confirmButton = {
            TextButton(onClick = {
                onToggleFavorite(app)
                onDismiss()
            }) {
                Text(
                    if (app.isFavorite) "Remove from Favorites"
                    else "Add to Favorites",
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
