/**
 * Composable that renders the user's pinned favorite apps as plain text labels.
 * No icons, thumbnails, or badges — each item is a tappable text string only.
 * Long-pressing an item triggers the context menu callback for management.
 * Blocked apps (AppInfo.blockedUntilEpochMs != null) get the same 🔒 prefix,
 * strikethrough, and faded color applied in the drawer (ULT-20).
 * Covers: ULT-6, ULT-20
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.HomeAlignment

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesList(
    favorites: List<AppInfo>,
    alignment: HomeAlignment,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textAlign = when (alignment) {
        HomeAlignment.LEFT -> TextAlign.Start
        HomeAlignment.CENTER -> TextAlign.Center
        HomeAlignment.RIGHT -> TextAlign.End
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("favorites_list"),
    ) {
        favorites.forEach { app ->
            val isBlocked = app.blockedUntilEpochMs != null
            Text(
                text = if (isBlocked) "\uD83D\uDD12 ${app.label}" else app.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isBlocked) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        indication = ripple(color = Color.Gray),
                        interactionSource = null,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                    )
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .testTag("favorite_item_${app.packageName}"),
            )
        }
    }
}
