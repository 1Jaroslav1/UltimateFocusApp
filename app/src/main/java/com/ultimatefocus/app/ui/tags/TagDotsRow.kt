/**
 * Renders up to 5 colored dots representing the tags assigned to an app.
 * Dots are 8.dp circles with 4.dp spacing between them, sized in dp so font
 * scaling does not deform them. Tapping a dot shows a small popup with the
 * tag name (auto-dismisses after 1500 ms or on outside touch); the popup's
 * tap also stops propagation so the underlying app row does not launch.
 * Apps with no tags render nothing — the layout collapses with no
 * placeholder gap (per ULT-74 AC).
 * Covers: ULT-74
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ultimatefocus.app.domain.model.Tag
import kotlinx.coroutines.delay

private const val POPUP_AUTO_DISMISS_MS = 1500L

@Composable
fun TagDotsRow(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return

    var popupTag by remember { mutableStateOf<Tag?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag("tag_dots_row"),
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = tag.color.toComposeColor(), shape = CircleShape)
                    .clickable { popupTag = tag }
                    .testTag("tag_dot_${tag.id}"),
            )
        }
    }

    val activePopupTag = popupTag
    if (activePopupTag != null) {
        LaunchedEffect(activePopupTag) {
            delay(POPUP_AUTO_DISMISS_MS)
            popupTag = null
        }
        Popup(
            onDismissRequest = { popupTag = null },
            properties = PopupProperties(focusable = false),
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.testTag("tag_dot_popup_${activePopupTag.id}"),
            ) {
                Text(
                    text = activePopupTag.name,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
