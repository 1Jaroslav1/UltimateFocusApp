/**
 * Persistent bottom bar with two quick-access shortcut slots rendered as plain
 * text labels (no icons). Left slot defaults to "Phone", right to "Camera"
 * until the ViewModel resolves the current assignments. Tap launches the
 * assigned app; long-press delegates reassignment to the caller (opens the
 * app picker). Always pinned at the bottom of the home screen.
 * Covers: ULT-13, ULT-14
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import com.ultimatefocus.app.domain.model.ShortcutSlot

@Composable
fun QuickShortcutsBar(
    leftApp: AppInfo?,
    rightApp: AppInfo?,
    onTap: (ShortcutSlot) -> Unit,
    onLongPress: (ShortcutSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("quick_shortcuts_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutLabel(
            label = leftApp?.label ?: "Phone",
            onClick = { onTap(ShortcutSlot.LEFT) },
            onLongClick = { onLongPress(ShortcutSlot.LEFT) },
            testTag = "shortcut_slot_left",
        )
        ShortcutLabel(
            label = rightApp?.label ?: "Camera",
            onClick = { onTap(ShortcutSlot.RIGHT) },
            onLongClick = { onLongPress(ShortcutSlot.RIGHT) },
            testTag = "shortcut_slot_right",
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutLabel(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    testTag: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Normal,
        modifier = Modifier
            .combinedClickable(
                indication = ripple(color = Color.Gray),
                interactionSource = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .testTag(testTag),
    )
}
