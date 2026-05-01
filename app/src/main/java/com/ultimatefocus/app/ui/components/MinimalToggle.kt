/**
 * Centralized toggle used everywhere the launcher previously used a
 * Material3 Switch (settings page, In-App Reminder controls). Wraps Switch
 * in Modifier.scale(0.8f) so the visible chrome is ~20% smaller per the
 * ULT-82 #5 feedback round.
 *
 * The touch target is intentionally NOT shrunk — Modifier.scale only
 * affects rendering, so the Switch keeps its original 48x48 dp hit area
 * to remain accessible. Callers should pass the surface-specific testTag
 * via the `modifier` parameter (preserves existing test selectors).
 *
 * Covers: ULT-82
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

private const val TOGGLE_SCALE: Float = 0.8f

@Composable
fun MinimalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.scale(TOGGLE_SCALE),
    )
}
