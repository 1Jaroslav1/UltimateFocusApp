/**
 * Single row in the app drawer list. Renders the app name as a plain text label
 * with no icon, badge, or thumbnail. Favorites are shown in bold.
 * Supports tap (launch) and long-press (context menu).
 * Covers: ULT-7
 */
package com.ultimatefocus.app.ui.drawer.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.AppInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
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
            text = appInfo.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = if (appInfo.isFavorite) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
