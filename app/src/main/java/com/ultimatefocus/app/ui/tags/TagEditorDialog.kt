/**
 * Modal dialog for creating or editing a tag. Inputs:
 * - Name TextField (required, trimmed; max 24 chars)
 * - 10-swatch color grid; swatches whose color is already used by another
 *   tag are greyed out and unselectable, except for the swatch matching the
 *   tag currently being edited (so editing without changing the color is OK).
 * Save is disabled until both inputs are valid. Stateless content — caller
 * supplies the existing Tag (null = create mode) and handlers.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor

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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tag_editor_dialog"),
        title = {
            Text(text = if (existing == null) "Create tag" else "Edit tag")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= NAME_CHAR_LIMIT) name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_editor_name_field"),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
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
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val color = selectedColor ?: return@TextButton
                    onSave(name.trim(), color)
                },
                modifier = Modifier.testTag("tag_editor_save_button"),
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tag_editor_cancel_button"),
            ) {
                Text(text = "Cancel")
            }
        },
    )
}

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
                    color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                    shape = CircleShape,
                )
                .clickable(enabled = !disabled, onClick = onClick),
        ) {
            if (disabled) {
                // Mark used-color swatches with a faint cross via overlapping bars.
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
