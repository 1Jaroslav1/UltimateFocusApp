package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .combinedClickable(
                indication = ripple(bounded = false, radius = 21.dp, color = Color.Gray),
                interactionSource = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp),
        )
    }
}
