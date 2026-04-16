/**
 * Minimalist context menu dialog shown on long-press of an app.
 * Displays the app name at the top, followed by action buttons:
 * "Add to Favorites" / "Remove from Favorites" and "App Info".
 * Uses a custom Dialog for full styling control — no default AlertDialog chrome.
 * Covers: ULT-6
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ultimatefocus.app.domain.model.AppInfo

@Composable
fun FavoriteContextMenuDialog(
    app: AppInfo,
    onToggleFavorite: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val favoriteInteraction = remember { MutableInteractionSource() }
    val infoInteraction = remember { MutableInteractionSource() }
    val favoritePressed = favoriteInteraction.collectIsPressedAsState()
    val infoPressed = infoInteraction.collectIsPressedAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.testTag("context_menu_dialog"),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = app.label,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.testTag("context_menu_app_name"),
                    )
                }


                HorizontalDivider(color = Color.White, thickness = 1.dp)

                // Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    TextButton(
                        onClick = {
                            onToggleFavorite(app)
                            onDismiss()
                        },
                        interactionSource = favoriteInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (favoritePressed.value) Color.Gray else Color.Transparent,
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("context_menu_toggle_favorite"),
                    ) {
                        Text(
                            text = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    TextButton(
                        onClick = {
                            onAppInfo(app)
                            onDismiss()
                        },
                        interactionSource = infoInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (infoPressed.value) Color.Gray else Color.Transparent,
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("context_menu_app_info"),
                    ) {
                        Text(
                            text = "App Info",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
