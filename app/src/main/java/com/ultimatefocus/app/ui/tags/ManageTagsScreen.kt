/**
 * Manage Tags screen — full-screen list of user-defined tags with a "Create
 * tag" CTA at the top. Tapping a tag row opens TagEditorDialog in edit mode.
 * Long-pressing a row opens an action menu offering Edit, Delete, and
 * Block-by-Tag (ULT-75). When a tag has an active block, its row shows a
 * lock badge with the remaining duration.
 * The Create CTA is disabled when 10 tags exist and shows a sub-line
 * explaining the limit. Empty state encourages first-tag creation.
 * Covers: ULT-72, ULT-75
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.util.BlockedTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun ManageTagsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageTagsViewModel = hiltViewModel(),
    blockTagViewModel: BlockTagViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTagBlocks by blockTagViewModel.activeTagBlocks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val nowMs = System.currentTimeMillis()

    var dialogState by remember { mutableStateOf<TagDialogState>(TagDialogState.Hidden) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.errors.collect { err ->
            errorMessage = err.toUiMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("manage_tags_screen"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Manage tags",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        CreateTagRow(
            isAtLimit = state.isAtTagLimit,
            onClick = { dialogState = TagDialogState.Editor(existing = null) },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        if (state.tags.isEmpty()) {
            EmptyTagsHint()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manage_tags_list"),
            ) {
                items(items = state.tags, key = { it.id }) { tag ->
                    val activeBlock = activeTagBlocks[tag.id]
                    TagRow(
                        tag = tag,
                        blockedRemainingMs = activeBlock?.let { it.blockedUntilEpochMs - nowMs },
                        onClick = { dialogState = TagDialogState.Editor(existing = tag) },
                        onLongPress = { dialogState = TagDialogState.RowMenu(tag) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    )
                }
            }
        }
    }

    when (val s = dialogState) {
        TagDialogState.Hidden -> Unit
        is TagDialogState.Editor -> {
            TagEditorDialog(
                existing = s.existing,
                takenColors = state.takenColors,
                onSave = { name, color ->
                    if (s.existing == null) viewModel.createTag(name, color)
                    else viewModel.updateTag(s.existing.id, name, color)
                    dialogState = TagDialogState.Hidden
                },
                onDismiss = { dialogState = TagDialogState.Hidden },
            )
        }
        is TagDialogState.RowMenu -> {
            TagRowActionMenu(
                tag = s.tag,
                isBlocked = s.tag.id in activeTagBlocks,
                onEdit = { dialogState = TagDialogState.Editor(existing = s.tag) },
                onDelete = {
                    scope.launch {
                        val count = viewModel.affectedAppCountForTag(s.tag.id)
                        dialogState = TagDialogState.DeleteConfirm(s.tag, count)
                    }
                },
                onBlock = {
                    scope.launch {
                        val packages = blockTagViewModel.preview(s.tag.id)
                        dialogState = TagDialogState.BlockPreview(s.tag, packages)
                    }
                },
                onDismiss = { dialogState = TagDialogState.Hidden },
            )
        }
        is TagDialogState.DeleteConfirm -> {
            TagDeleteConfirmDialog(
                tagName = s.tag.name,
                affectedAppCount = s.affectedAppCount,
                onConfirm = {
                    viewModel.deleteTag(s.tag.id)
                    dialogState = TagDialogState.Hidden
                },
                onDismiss = { dialogState = TagDialogState.Hidden },
            )
        }
        is TagDialogState.BlockPreview -> {
            BlockTagPreviewDialog(
                tag = s.tag,
                affectedPackages = s.affectedPackages,
                onConfirm = { duration: BlockDuration ->
                    blockTagViewModel.blockTag(s.tag.id, duration)
                    dialogState = TagDialogState.Hidden
                },
                onDismiss = { dialogState = TagDialogState.Hidden },
            )
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            modifier = Modifier.testTag("tag_error_dialog"),
            title = { Text("Couldn't save") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(
                    onClick = { errorMessage = null },
                    modifier = Modifier.testTag("tag_error_dismiss_button"),
                ) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun CreateTagRow(isAtLimit: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAtLimit, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("create_tag_button"),
    ) {
        Text(
            text = "+ Create tag",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isAtLimit) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            fontWeight = FontWeight.Medium,
        )
        if (isAtLimit) {
            Text(
                text = "Tag limit reached ($MAX_TAGS / $MAX_TAGS).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("create_tag_limit_message"),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TagRow(
    tag: Tag,
    blockedRemainingMs: Long?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("tag_row_${tag.id}"),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = tag.color.toComposeColor(), shape = CircleShape)
                .testTag("tag_row_dot_${tag.id}"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (blockedRemainingMs != null && blockedRemainingMs > 0L) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "🔒 ${BlockedTimeFormatter.formatRemaining(blockedRemainingMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("tag_lock_indicator_${tag.id}"),
            )
        }
    }
}

@Composable
private fun EmptyTagsHint() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .testTag("manage_tags_empty_state"),
    ) {
        Text(
            text = "No tags yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first tag to organize apps into categories.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private sealed class TagDialogState {
    data object Hidden : TagDialogState()
    data class Editor(val existing: Tag?) : TagDialogState()
    data class RowMenu(val tag: Tag) : TagDialogState()
    data class DeleteConfirm(val tag: Tag, val affectedAppCount: Int) : TagDialogState()
    data class BlockPreview(val tag: Tag, val affectedPackages: List<String>) : TagDialogState()
}

private fun TagOperationError.toUiMessage(): String = when (this) {
    TagOperationError.TagLimitReached -> "Tag limit reached ($MAX_TAGS / $MAX_TAGS)."
    TagOperationError.ColorAlreadyUsed -> "That color is already in use by another tag."
    TagOperationError.AppTagLimitReached -> "App already has the maximum 5 tags."
    TagOperationError.TagNotFound -> "That tag no longer exists."
    is TagOperationError.NameBlank -> "Tag name cannot be blank."
}

@Composable
private fun TagRowActionMenu(
    tag: Tag,
    isBlocked: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tag_row_menu"),
        title = { Text(text = tag.name) },
        text = {
            Text(
                text = if (isBlocked) {
                    "This tag is currently blocking its apps."
                } else {
                    "Choose an action."
                },
            )
        },
        confirmButton = {
            Column {
                TextButton(
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("tag_row_menu_edit"),
                ) { Text(text = "Edit") }
                if (!isBlocked) {
                    TextButton(
                        onClick = {
                            onBlock()
                        },
                        modifier = Modifier.testTag("tag_row_menu_block"),
                    ) { Text(text = "Block by tag…") }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.testTag("tag_row_menu_delete"),
            ) { Text(text = "Delete") }
        },
    )
}
