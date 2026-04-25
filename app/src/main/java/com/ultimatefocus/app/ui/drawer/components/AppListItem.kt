/**
 * Single row in the app drawer list. Renders the app name as a plain text label
 * with no icon, badge, or thumbnail. Favorites are shown in bold.
 * Blocked apps (AppInfo.blockedUntilEpochMs != null) get a 🔒 prefix,
 * strikethrough, and a faded color so the user sees at a glance the app is
 * currently unavailable (ULT-20 visual indicator AC).
 * In selection mode (ULT-73 bulk tagging) the row shows a subtle selected
 * background when isSelected is true.
 * Supports tap (launch / toggle selection) and long-press (context menu /
 * enter selection mode).
 * Covers: ULT-7, ULT-20, ULT-73
 */
package com.ultimatefocus.app.ui.drawer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.AppInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val isBlocked = appInfo.blockedUntilEpochMs != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(
                indication = ripple(color = Color.Gray),
                interactionSource = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("drawer_item_${appInfo.packageName}"),
    ) {
        Text(
            text = if (isBlocked) "🔒 ${appInfo.label}" else appInfo.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (isBlocked) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            fontWeight = if (appInfo.isFavorite) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}
