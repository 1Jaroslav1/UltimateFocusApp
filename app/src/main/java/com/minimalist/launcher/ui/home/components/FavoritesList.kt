/**
 * Composable that renders the user's pinned favorite apps as plain text labels.
 * No icons, thumbnails, or badges — each item is a tappable text string only.
 * Long-pressing an item triggers the context menu callback for management.
 * Covers: ULT-6
 */
package com.minimalist.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minimalist.launcher.domain.model.AppInfo
import com.minimalist.launcher.domain.model.HomeAlignment

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
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                    )
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .testTag("favorite_item_${app.packageName}"),
            )
        }
    }
}
