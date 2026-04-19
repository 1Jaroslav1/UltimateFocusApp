/**
 * Two-stage duration picker shown after the user taps "Block app…" in the
 * context menu.
 *  Stage 1: 8 rows, one per BlockDuration (1h..30d), each tagged
 *           `block_duration_{label}`.
 *  Stage 2: an explicit confirmation ("Are you sure?") step — final guardrail
 *           before committing a binding block since there's no self-unblock.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ultimatefocus.app.domain.model.BlockDuration

@Composable
fun BlockDurationPickerDialog(
    appLabel: String,
    onConfirm: (BlockDuration) -> Unit,
    onDismiss: () -> Unit,
) {
    var pending by remember { mutableStateOf<BlockDuration?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.testTag("block_duration_dialog"),
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
                        text = if (pending == null) "Block $appLabel for\u2026" else "Are you sure?",
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.testTag("block_duration_title"),
                    )
                }

                HorizontalDivider(color = Color.White, thickness = 1.dp)

                val current = pending
                if (current == null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        BlockDuration.entries.forEach { duration ->
                            DurationRow(
                                label = duration.label,
                                testTag = "block_duration_${duration.label}",
                                onClick = { pending = duration },
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "You won't be able to open $appLabel for ${current.label}. " +
                                "Blocks cannot be undone.",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("block_duration_confirm_message"),
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextButton(
                                onClick = { pending = null },
                                modifier = Modifier.testTag("block_duration_cancel"),
                            ) {
                                Text(text = "Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            TextButton(
                                onClick = {
                                    onConfirm(current)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("block_duration_confirm"),
                            ) {
                                Text(text = "Confirm", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationRow(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    TextButton(
        onClick = onClick,
        interactionSource = interaction,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (pressed) Color.Gray else Color.Transparent,
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
