/**
 * Dialog shown the first time the user commits a block without having
 * enabled BlockedAppAccessibilityService. Explains why accessibility access
 * is the primary enforcement mechanism (without it, the block can be
 * bypassed through Settings / recents / notifications) and offers two
 * paths: deep-link to system Accessibility settings, or proceed without
 * (launcher-layer enforcement still applies).
 * Covers: ULT-20, ULT-21
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
fun AccessibilityPermissionPrompt(
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
            modifier = Modifier.testTag("accessibility_permission_prompt"),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Enforce the block on $appLabel?",
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    text = "For the block to apply everywhere — Settings, recent apps, " +
                        "notifications, shortcuts — UltimateFocus needs Accessibility " +
                        "access. Without it, the block only applies inside this launcher. " +
                        "No data leaves the device.",
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
                        modifier = Modifier.testTag("accessibility_permission_continue"),
                    ) {
                        Text(text = "Continue without", color = Color.White, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            onGrantAccess()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("accessibility_permission_grant"),
                    ) {
                        Text(text = "Open Accessibility settings", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
