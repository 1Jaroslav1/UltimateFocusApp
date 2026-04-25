/**
 * Modal that lets the user toggle tags on/off for one or more apps,
 * rendered in the launcher's default app modal style (black surface,
 * 1dp white border, in-line header) so it matches the long-press app
 * menu users already know.
 *
 * Selection state is expressed text-only (no Material checkboxes) so
 * the dark theme stays clean:
 *   ✓   AllAssigned     (every app in the selection has the tag)
 *   ◐   SomeAssigned    (bulk mode only; some apps have it, some don't)
 *   ·   NoneAssigned    (no app in the selection has the tag)
 *
 * Tapping a row delegates to TagPickerViewModel.toggle which cycles:
 *   - single-app: assigned ↔ unassigned
 *   - bulk: AllAssigned → unassign-all, otherwise → assign-all
 *
 * Empty state ("No tags yet") shows a "Manage tags" action that hops to
 * the ManageTagsScreen so users without tags can create one without
 * leaving the flow manually.
 *
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.ui.components.DefaultAppModal
import com.ultimatefocus.app.ui.components.DefaultModalAction
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
    val isBulk = state.packagesUnderEdit.size > 1
    val header = if (isBulk) "Tags for ${state.packagesUnderEdit.size} apps" else "Tags"

    DefaultAppModal(
        header = header,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("tag_picker_dialog"),
    ) {
        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                ) {
                    Text(text = "Loading tags…", color = Color.White, fontSize = 14.sp)
                }
            }
            state.items.isEmpty() -> EmptyTagsHint(onManageTags = onManageTags)
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .testTag("tag_picker_list"),
            ) {
                items(items = state.items, key = { it.tag.id }) { item ->
                    TagPickerRow(
                        item = item,
                        isBulk = isBulk,
                        onClick = { onToggle(item) },
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White, thickness = 1.dp)

        DefaultModalAction(
            text = "Done",
            onClick = onDismiss,
            modifier = Modifier.testTag("tag_picker_done_button"),
        )
    }
}

@Composable
private fun TagPickerRow(
    item: TagPickerItem,
    isBulk: Boolean,
    onClick: () -> Unit,
) {
    val statePrefix = when (item.state) {
        TagAssignmentState.AllAssigned -> "✓ "
        TagAssignmentState.SomeAssigned -> if (isBulk) "◐ " else "  "
        TagAssignmentState.NoneAssigned -> "  "
    }
    val labelColor = if (item.disabled) Color.Gray else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.disabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("tag_picker_row_${item.tag.id}"),
    ) {
        Text(
            text = statePrefix,
            color = labelColor,
            fontSize = 14.sp,
            modifier = Modifier.testTag("tag_picker_state_${item.tag.id}"),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = item.tag.color.toComposeColor(), shape = CircleShape),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.tag.name,
                color = labelColor,
                fontSize = 14.sp,
            )
            if (item.disabled) {
                Text(
                    text = "App already has the maximum 5 tags.",
                    color = Color.Gray,
                    fontSize = 11.sp,
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
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .testTag("tag_picker_empty_state"),
    ) {
        Text(
            text = "No tags yet",
            color = Color.White,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Create a tag to start categorizing your apps.",
            color = Color.Gray,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        DefaultModalAction(
            text = "Manage tags",
            onClick = onManageTags,
            fillWidth = false,
            modifier = Modifier.testTag("tag_picker_manage_tags_button"),
        )
    }
}
