/**
 * Minimalist long-press context menu for an app. Replaces the earlier
 * FavoriteContextMenuDialog with a block-aware variant used on both the
 * home favorites list and the full drawer.
 * - Unblocked: "Add/Remove Favorite", "Block app…", "Manage tags…", "App Info"
 * - Blocked:   "Add/Remove Favorite", read-only "Blocked — {remaining}",
 *              "Manage tags…", "App Info". Deliberately NO "Unblock" row —
 *              the repository interface has no unblock method, satisfying
 *              the ULT-19 "no self-unblock escape hatch" requirement at the
 *              architecture layer.
 * Reuses existing test tags (`context_menu_toggle_favorite`, `context_menu_app_info`)
 * so existing instrumentation tests keep passing. Adds new tags
 * `context_menu_block_app`, `context_menu_block_status`, and
 * `context_menu_manage_tags` (ULT-73).
 * Covers: ULT-6, ULT-20, ULT-73
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.util.BlockedTimeFormatter

@Composable
fun AppContextMenuDialog(
    app: AppInfo,
    nowEpochMs: Long,
    onToggleFavorite: (AppInfo) -> Unit,
    onBlockApp: (AppInfo) -> Unit,
    onManageTags: (AppInfo) -> Unit = {},
    onSelectMultiple: (AppInfo) -> Unit = {},
    onAppInfo: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val favoriteInteraction = remember { MutableInteractionSource() }
    val blockInteraction = remember { MutableInteractionSource() }
    val tagsInteraction = remember { MutableInteractionSource() }
    val selectInteraction = remember { MutableInteractionSource() }
    val infoInteraction = remember { MutableInteractionSource() }
    val favoritePressed = favoriteInteraction.collectIsPressedAsState()
    val blockPressed = blockInteraction.collectIsPressedAsState()
    val tagsPressed = tagsInteraction.collectIsPressedAsState()
    val selectPressed = selectInteraction.collectIsPressedAsState()
    val infoPressed = infoInteraction.collectIsPressedAsState()

    val blockedUntil = app.blockedUntilEpochMs
    val isBlocked = blockedUntil != null && blockedUntil > nowEpochMs
    val remainingMs = if (isBlocked) blockedUntil!! - nowEpochMs else 0L

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

                    if (isBlocked) {
                        // Read-only status row. No onClick — the repository has
                        // no unblock method, so the UI cannot offer an override.
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 12.dp)
                                .testTag("context_menu_block_status"),
                        ) {
                            Text(
                                text = "Blocked — ${BlockedTimeFormatter.formatRemaining(remainingMs)} remaining",
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                onBlockApp(app)
                                onDismiss()
                            },
                            interactionSource = blockInteraction,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (blockPressed.value) Color.Gray else Color.Transparent,
                            ),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("context_menu_block_app"),
                        ) {
                            Text(
                                text = "Block app\u2026",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onManageTags(app)
                            onDismiss()
                        },
                        interactionSource = tagsInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (tagsPressed.value) Color.Gray else Color.Transparent,
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("context_menu_manage_tags"),
                    ) {
                        Text(
                            text = "Manage tags…",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    TextButton(
                        onClick = {
                            onSelectMultiple(app)
                            onDismiss()
                        },
                        interactionSource = selectInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selectPressed.value) Color.Gray else Color.Transparent,
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("context_menu_select_multiple"),
                    ) {
                        Text(
                            text = "Select multiple…",
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
