/**
 * Two-stage delete confirmation for BlockingScheduleEditorScreen, mirroring
 * the minimalist black/white aesthetic of BlockDurationPickerDialog. The
 * extra "Are you sure?" stage exists because deleting a recurring schedule
 * is irreversible — the user has lost their automation if they deleted by
 * accident — and the active-window guard already blocked any in-window
 * deletes upstream (ULT-25).
 *
 * Used only from the edit branch of the editor; the create branch hides
 * the Delete button entirely (ULT-24 has no delete affordance).
 *
 * Covers: ULT-25
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DeleteScheduleConfirmDialog(
    scheduleName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.testTag("delete_schedule_dialog"),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Delete \"$scheduleName\"?",
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.testTag("delete_schedule_title"),
                    )
                }
                HorizontalDivider(color = Color.White, thickness = 1.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Deleting a schedule removes its automation. " +
                            "This cannot be undone.",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_schedule_message"),
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("delete_schedule_cancel"),
                        ) {
                            Text(text = "Cancel", color = Color.White, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = onConfirm,
                            modifier = Modifier.testTag("delete_schedule_confirm"),
                        ) {
                            Text(text = "Delete", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
