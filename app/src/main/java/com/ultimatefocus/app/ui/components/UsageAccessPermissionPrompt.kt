/**
 * Dialog shown when the user commits a block without having granted Usage
 * access. Usage access backs ForegroundPoller, the accessibility-free
 * enforcement fallback (ULT-86): it is what keeps blocks working when the
 * OS suspends the accessibility service, and what catches a blocked app
 * being resumed from the recents switcher — a path that does not reliably
 * emit an accessibility window-state event.
 *
 * Mirrors AccessibilityPermissionPrompt's shape and copy idiom so the
 * permission chain reads consistently.
 *
 * Covers: ULT-86
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun UsageAccessPermissionPrompt(
    appLabel: String,
    onGrantAccess: () -> Unit,
    onContinueWithout: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.testTag("usage_access_permission_prompt"),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Keep the block on $appLabel reliable?",
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    text = "Usage access lets UltimateFocus re-check which app is open " +
                        "even when the system pauses Accessibility access in the " +
                        "background. Without it, blocks can be bypassed by reopening an " +
                        "app from recent apps. No data leaves the device.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            onContinueWithout()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("usage_access_permission_continue"),
                    ) {
                        Text(text = "Continue without", color = Color.White, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            onGrantAccess()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("usage_access_permission_grant"),
                    ) {
                        Text(text = "Open Usage access settings", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
