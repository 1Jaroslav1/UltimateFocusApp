/**
 * ViewModel powering the TagPickerDialog. Operates in two modes:
 * - Single-app: caller passes one packageName; uiState.assignmentByTag
 *   reports each tag as Assigned or Unassigned.
 * - Bulk: caller passes a list of packageNames; assignmentByTag reports
 *   AllAssigned / SomeAssigned / NoneAssigned per tag, and toggle cycles
 *   AllAssigned ↔ NoneAssigned across the selection.
 * Toggles persist immediately (no Save button) by delegating to TagRepository.
 * Failures (5-tag cap, missing tag) bubble up via the errors SharedFlow.
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TagAssignmentState { AllAssigned, SomeAssigned, NoneAssigned }

data class TagPickerItem(
    val tag: Tag,
    val state: TagAssignmentState,
    /** When true, toggling this tag in single-app mode would exceed the per-app cap. */
    val disabled: Boolean,
)

data class TagPickerUiState(
    val items: List<TagPickerItem> = emptyList(),
    val packagesUnderEdit: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Standalone (not Hilt) so the dialog can be hosted inline by any screen.
 * Caller scopes lifetime to the dialog's composition; reads/writes go
 * through the same TagRepository singleton as ManageTagsViewModel.
 */
class TagPickerViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val packagesFlow = MutableStateFlow<List<String>>(emptyList())

    private val _errors = MutableSharedFlow<TagOperationError>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TagPickerUiState> = packagesFlow
        .flatMapLatest { packages ->
            combine(
                tagRepository.observeTags(),
                tagRepository.observeAppToTagsMap(),
            ) { tags, tagsByApp ->
                if (packages.isEmpty()) {
                    return@combine TagPickerUiState(items = emptyList(), isLoading = false)
                }
                val items = tags.map { tag ->
                    val assignedCount = packages.count { pkg ->
                        tagsByApp[pkg].orEmpty().any { it.id == tag.id }
                    }
                    val state = when (assignedCount) {
                        0 -> TagAssignmentState.NoneAssigned
                        packages.size -> TagAssignmentState.AllAssigned
                        else -> TagAssignmentState.SomeAssigned
                    }
                    val disabled = if (packages.size == 1) {
                        // Single-app mode: a tag not yet assigned is disabled when
                        // the app already has 5 tags.
                        state == TagAssignmentState.NoneAssigned &&
                            (tagsByApp[packages.first()].orEmpty().size >= 5)
                    } else false
                    TagPickerItem(tag = tag, state = state, disabled = disabled)
                }
                TagPickerUiState(items = items, packagesUnderEdit = packages, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TagPickerUiState(),
        )

    fun setPackages(packages: List<String>) {
        packagesFlow.value = packages
    }

    /**
     * Toggle a tag against the current package set.
     * - Single-app: assigned → unassign, unassigned → assign.
     * - Bulk: AllAssigned → unassign from every package; otherwise (Some or None)
     *   → assign to every package that doesn't already have it.
     */
    fun toggle(item: TagPickerItem) {
        val packages = packagesFlow.value
        if (packages.isEmpty()) return
        viewModelScope.launch {
            when (item.state) {
                TagAssignmentState.AllAssigned -> {
                    tagRepository.bulkUnassignTag(packages, item.tag.id)
                }
                TagAssignmentState.NoneAssigned,
                TagAssignmentState.SomeAssigned,
                -> {
                    tagRepository.bulkAssignTag(packages, item.tag.id)
                        .onFailure { err ->
                            if (err is TagOperationError) _errors.tryEmit(err)
                        }
                }
            }
        }
    }
}
