/**
 * Modal that lets the user toggle tags on/off for one or more apps. In bulk
 * mode (>1 package) each row uses a tri-state checkbox: ✓ when all selected
 * apps already have the tag, ⊠ (intermediate) when some do, blank when none
 * — tapping cycles AllAssigned → NoneAssigned → AllAssigned.
 * Empty state ("No tags yet") shows a "Manage tags" button that hops to
 * the ManageTagsScreen so users without tags can create one without
 * leaving the flow manually.
 * Saves immediately — no Save button. Dismiss via outside tap or "Done".
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.domain.repository.TagRepository
import dagger.hilt.android.EntryPointAccessors

/**
 * Stateful host. Resolves a TagRepository from the application Hilt graph
 * and constructs a TagPickerViewModel inline so the dialog can be triggered
 * from any screen.
 */
@Composable
fun TagPickerDialog(
    packageNames: List<String>,
    onManageTags: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tagRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TagsEntryPoint::class.java,
        ).tagRepository()
    }
    val viewModel = remember(tagRepository) { TagPickerViewModel(tagRepository) }

    LaunchedEffect(packageNames) {
        viewModel.setPackages(packageNames)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TagPickerDialogContent(
        state = state,
        onToggle = viewModel::toggle,
        onManageTags = onManageTags,
        onDismiss = onDismiss,
    )
}

/**
 * Stateless content. Receives a fully-formed [TagPickerUiState] and emits
 * toggle events upward — easy to render in Compose UI tests without
 * EntryPointAccessors.
 */
@Composable
fun TagPickerDialogContent(
    state: TagPickerUiState,
    onToggle: (TagPickerItem) -> Unit,
    onManageTags: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tag_picker_dialog"),
        title = {
            val title = if (state.packagesUnderEdit.size > 1) {
                "Tags for ${state.packagesUnderEdit.size} apps"
            } else {
                "Tags"
            }
            Text(text = title)
        },
        text = {
            when {
                state.isLoading -> Text(text = "Loading tags…")
                state.items.isEmpty() -> EmptyTagsHint(onManageTags = onManageTags)
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightSafe()
                        .testTag("tag_picker_list"),
                ) {
                    items(items = state.items, key = { it.tag.id }) { item ->
                        TagPickerRow(
                            item = item,
                            isBulk = state.packagesUnderEdit.size > 1,
                            onClick = { onToggle(item) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tag_picker_done_button"),
            ) {
                Text(text = "Done")
            }
        },
    )
}

@Composable
private fun TagPickerRow(
    item: TagPickerItem,
    isBulk: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.disabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("tag_picker_row_${item.tag.id}"),
    ) {
        if (isBulk) {
            TriStateCheckbox(
                state = when (item.state) {
                    TagAssignmentState.AllAssigned -> ToggleableState.On
                    TagAssignmentState.NoneAssigned -> ToggleableState.Off
                    TagAssignmentState.SomeAssigned -> ToggleableState.Indeterminate
                },
                onClick = onClick,
                enabled = !item.disabled,
                modifier = Modifier.testTag("tag_picker_tristate_${item.tag.id}"),
            )
        } else {
            Checkbox(
                checked = item.state == TagAssignmentState.AllAssigned,
                onCheckedChange = { onClick() },
                enabled = !item.disabled,
                modifier = Modifier.testTag("tag_picker_check_${item.tag.id}"),
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = item.tag.color.toComposeColor(), shape = CircleShape),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.tag.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.disabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
            if (item.disabled) {
                Text(
                    text = "App already has the maximum 5 tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tag_picker_disabled_message_${item.tag.id}"),
                )
            }
        }
    }
}

@Composable
private fun EmptyTagsHint(onManageTags: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("tag_picker_empty_state"),
    ) {
        Text(
            text = "No tags yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Create a tag to start categorizing your apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onManageTags,
            modifier = Modifier.testTag("tag_picker_manage_tags_button"),
        ) {
            Text(text = "Manage tags")
        }
    }
}

private fun Modifier.heightSafe(): Modifier = this.then(
    Modifier.height(360.dp),
)
