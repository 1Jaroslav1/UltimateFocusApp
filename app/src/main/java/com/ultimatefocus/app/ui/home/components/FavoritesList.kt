/**
 * Composable that renders the user's pinned favorite apps as plain text labels.
 * No icons, thumbnails, or badges — each item is a tappable text string only.
 * Long-pressing an item triggers the context menu callback for management.
 * Blocked apps (AppInfo.blockedUntilEpochMs != null) get the same 🔒 prefix,
 * strikethrough, and faded color applied in the drawer (ULT-20).
 * Tag indicators (colored dots) appear to the right of each label when the
 * app has any assigned tags. The label ellipsizes with "…" if it would
 * otherwise overlap the dots (ULT-74 AC).
 * Covers: ULT-6, ULT-20, ULT-74
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.ui.tags.TagDotsRow

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
    val rowArrangement = when (alignment) {
        HomeAlignment.LEFT -> Arrangement.Start
        HomeAlignment.CENTER -> Arrangement.Center
        HomeAlignment.RIGHT -> Arrangement.End
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("favorites_list"),
    ) {
        favorites.forEach { app ->
            val isBlocked = app.blockedUntilEpochMs != null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = rowArrangement,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        indication = ripple(color = Color.Gray),
                        interactionSource = null,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                    )
                    // Merge child semantics so onNodeWithText("AppLabel") still
                    // resolves to this clickable node — preserves the existing
                    // FavoritesList tests that select rows by their text label.
                    .semantics(mergeDescendants = true) { }
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .testTag("favorite_item_${app.packageName}"),
            ) {
                Text(
                    text = if (isBlocked) "🔒 ${app.label}" else app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBlocked) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    fontWeight = FontWeight.Normal,
                    textAlign = textAlign,
                    textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false so dots hug the label rather than being pushed
                    // to the far edge of the row; the weight share still caps width
                    // so a long name ellipsizes before colliding with the dots.
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (app.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TagDotsRow(tags = app.tags)
                }
            }
        }
    }
}
