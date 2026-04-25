/**
 * Dialog shown the first time the user commits a block without having
 * granted notification-listener access. Offers a deep link into the system
 * settings AND a "Continue without" branch so the block goes through
 * regardless — listener access is an enhancement, not a prerequisite.
 * Covers: ULT-22
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
fun NotificationPermissionPrompt(
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
            modifier = Modifier.testTag("notification_permission_prompt"),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Silence $appLabel notifications?",
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    text = "To suppress notifications from blocked apps, UltimateFocus " +
                        "needs Notification Access. Without it, $appLabel will still be " +
                        "unopenable, but its notifications will still appear.",
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
                        modifier = Modifier.testTag("notification_permission_continue"),
                    ) {
                        Text(text = "Continue without", color = Color.White, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            onGrantAccess()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("notification_permission_grant"),
                    ) {
                        Text(text = "Grant access", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
