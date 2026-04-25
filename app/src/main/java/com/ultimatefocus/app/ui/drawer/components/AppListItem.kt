/**
 * Single row in the app drawer list. Renders the app name as a plain text label
 * with no icon, badge, or thumbnail. Favorites are shown in bold.
 * Blocked apps (AppInfo.blockedUntilEpochMs != null) get a 🔒 prefix,
 * strikethrough, and a faded color so the user sees at a glance the app is
 * currently unavailable (ULT-20 visual indicator AC).
 * Tag indicators (colored dots) are rendered to the right of the label
 * via TagDotsRow when the app has any assigned tags. The label uses
 * Modifier.weight(1f) + ellipsis so a long name is truncated with "…"
 * before it can overlap the dots (ULT-74 AC).
 * In selection mode (ULT-73 bulk tagging) the row shows a subtle selected
 * background when isSelected is true.
 * Supports tap (launch / toggle selection) and long-press (context menu /
 * enter selection mode).
 * Covers: ULT-7, ULT-20, ULT-73, ULT-74
 */
package com.ultimatefocus.app.ui.drawer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.ui.tags.TagDotsRow

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
            // Merge child semantics so existing onNodeWithText() lookups by
            // app label resolve to this clickable Row instead of the inner
            // Text — preserves backward compatibility with drawer tests.
            .semantics(mergeDescendants = true) { }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (appInfo.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            TagDotsRow(tags = appInfo.tags)
        }
    }
}
