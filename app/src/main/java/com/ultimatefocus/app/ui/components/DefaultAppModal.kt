/**
 * Reusable container for the launcher's "default app modal" visual style:
 * black background, 1dp white border, rounded corners, an in-line header
 * row with a 1dp white divider beneath, and a content area below.
 *
 * The pattern was introduced by AppContextMenuDialog (long-press app menu)
 * and is now applied to every other modal so the launcher's dark, text-only
 * aesthetic stays consistent across surfaces — tag editor, tag long-press
 * menu, block-by-tag preview, search-provider picker, etc.
 *
 * DefaultModalAction renders a single tappable text row with a press-state
 * gray fill, mirroring the action rows inside AppContextMenuDialog so
 * consumers can compose menu-style modals without re-deriving the visual
 * details every time.
 *
 * Covers: ULT-72, ULT-73, ULT-75
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DefaultAppModal(
    header: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    headerTestTag: String? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = properties) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White),
            modifier = modifier,
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
                        text = header,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        modifier = if (headerTestTag != null) {
                            Modifier.testTag(headerTestTag)
                        } else {
                            Modifier
                        },
                    )
                }

                HorizontalDivider(color = Color.White, thickness = 1.dp)

                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * Tappable text row used inside [DefaultAppModal]. Press gives a gray
 * background flash; disabled rows render in faded gray and ignore taps.
 */
@Composable
fun DefaultModalAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState()
    TextButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (pressed.value) Color.Gray else Color.Transparent,
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Read-only text row inside [DefaultAppModal]. Same height + alignment as
 * an action row so a list of actions interleaved with status text reads
 * cleanly without wobble.
 */
@Composable
fun DefaultModalStatusRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White,
        )
    }
}
