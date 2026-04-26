/**
 * Modal dialog for creating or editing a tag, rendered inside the launcher's
 * "default app modal" container (black surface, white border, in-line header).
 * Inputs:
 * - Name TextField (required, trimmed; max 24 chars) — uses the dark
 *   modal palette so the text stays readable on black.
 * - 10-swatch color grid; swatches whose color is already used by another
 *   tag are greyed out and unselectable, except for the swatch matching the
 *   tag currently being edited (so editing without changing the color is OK).
 * Save is disabled until both inputs are valid; Save and Cancel render as
 * standard DefaultModalAction rows beneath a thin divider.
 * Stateless content — caller supplies the existing Tag (null = create mode)
 * and handlers.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.components.DefaultAppModal
import com.ultimatefocus.app.ui.components.DefaultModalAction

private const val NAME_CHAR_LIMIT = 24

@Composable
fun TagEditorDialog(
    existing: Tag?,
    takenColors: Set<TagColor>,
    onSave: (name: String, color: TagColor) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedColor by remember { mutableStateOf(existing?.color) }

    val canSave = remember(name, selectedColor) {
        name.trim().isNotEmpty() && selectedColor != null
    }

    DefaultAppModal(
        header = if (existing == null) "Create tag" else "Edit tag",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("tag_editor_dialog"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= NAME_CHAR_LIMIT) name = it },
                label = { Text(text = "Name", color = Color.White, fontSize = 12.sp) },
                singleLine = true,
                colors = darkTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_editor_name_field"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Color",
                fontSize = 12.sp,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TagColor.values().forEach { swatch ->
                    val isCurrentTagColor = existing?.color == swatch
                    val isUsedByAnother = swatch in takenColors && !isCurrentTagColor
                    val selected = selectedColor == swatch
                    TagSwatch(
                        color = swatch,
                        selected = selected,
                        disabled = isUsedByAnother,
                        onClick = { if (!isUsedByAnother) selectedColor = swatch },
                        modifier = Modifier.testTag(swatchTestTag(swatch, isUsedByAnother)),
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White, thickness = 1.dp)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            DefaultModalAction(
                text = "Cancel",
                onClick = onDismiss,
                fillWidth = false,
                modifier = Modifier.testTag("tag_editor_cancel_button"),
            )
            DefaultModalAction(
                text = "Save",
                onClick = {
                    val color = selectedColor ?: return@DefaultModalAction
                    onSave(name.trim(), color)
                },
                enabled = canSave,
                fillWidth = false,
                modifier = Modifier.testTag("tag_editor_save_button"),
            )
        }
    }
}

@Composable
private fun darkTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedIndicatorColor = Color.White,
    unfocusedIndicatorColor = Color.Gray,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color.White,
)

@Composable
private fun TagSwatch(
    color: TagColor,
    selected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatchColor = color.toComposeColor()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(36.dp)
                .background(
                    color = if (disabled) swatchColor.copy(alpha = 0.25f) else swatchColor,
                    shape = CircleShape,
                )
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else Color.Transparent,
                    shape = CircleShape,
                )
                .clickable(enabled = !disabled, onClick = onClick),
        ) {
            if (disabled) {
                Box(
                    modifier = Modifier
                        .size(2.dp, 24.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

internal fun swatchTestTag(color: TagColor, disabled: Boolean): String =
    if (disabled) "tag_editor_swatch_${color.name}_disabled"
    else "tag_editor_swatch_${color.name}"
