/**
 * Persistent bottom bar with two quick-access shortcut slots rendered as icons.
 * Left slot: Phone icon (defaults to the system dialer, reassignable).
 * Right slot: Camera icon (defaults to the system camera, reassignable).
 * Tap launches the assigned app; long-press delegates reassignment to the
 * caller (opens the app picker). Icons hug the horizontal edges so the bar
 * reads as two anchor points rather than as a row of labels.
 * Covers: ULT-13, ULT-14
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
            .testTag("quick_shortcuts_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutIcon(
            icon = Icons.Default.Phone,
            contentDescription = leftApp?.label ?: "Phone",
            onClick = { onTap(ShortcutSlot.LEFT) },
            onLongClick = { onLongPress(ShortcutSlot.LEFT) },
            testTag = "shortcut_slot_left",
        )
        ShortcutIcon(
            icon = Icons.Default.PhotoCamera,
            contentDescription = rightApp?.label ?: "Camera",
            onClick = { onTap(ShortcutSlot.RIGHT) },
            onLongClick = { onLongPress(ShortcutSlot.RIGHT) },
            testTag = "shortcut_slot_right",
        )
    }
}
