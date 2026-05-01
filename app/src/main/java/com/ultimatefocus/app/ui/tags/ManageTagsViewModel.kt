/**
 * ViewModel backing the Manage Tags screen. Exposes the live list of tags,
 * the set of colors already taken (so the editor can grey them out), and
 * imperative create/edit/delete operations that surface
 * TagOperationError as one-shot UI events.
 *
 * ULT-85 follow-up: also exposes the "Sync with system categories" flow
 * that wraps CategorySeeder — gather the list of detectable categories +
 * apps so the user can opt-in via checkboxes, then apply the selection.
 *
 * Covers: ULT-72, ULT-85
 */
package com.ultimatefocus.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.repository.TagRepository
import com.ultimatefocus.app.domain.service.CategorySeeder
import com.ultimatefocus.app.domain.service.SyncCategoryCandidate
import com.ultimatefocus.app.domain.service.SyncSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-friendly snapshot of the Manage Tags screen state. */
data class ManageTagsUiState(
    val tags: List<Tag> = emptyList(),
    val isAtTagLimit: Boolean = false,
    val takenColors: Set<TagColor> = emptySet(),
)

@HiltViewModel
class ManageTagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val categorySeeder: CategorySeeder,
) : ViewModel() {

    val uiState: StateFlow<ManageTagsUiState> = tagRepository.observeTags()
        .map { tags ->
            ManageTagsUiState(
                tags = tags,
                isAtTagLimit = tags.size >= MAX_TAGS,
                takenColors = tags.map { it.color }.toSet(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ManageTagsUiState(),
        )

    private val _errors = MutableSharedFlow<TagOperationError>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    fun createTag(name: String, color: TagColor) {
        viewModelScope.launch {
            tagRepository.createTag(name, color).onFailure { err ->
                if (err is TagOperationError) _errors.tryEmit(err)
            }
        }
    }

    fun updateTag(id: Long, name: String, color: TagColor) {
        viewModelScope.launch {
            tagRepository.updateTag(id, name, color).onFailure { err ->
                if (err is TagOperationError) _errors.tryEmit(err)
            }
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            tagRepository.deleteTag(id).onFailure { err ->
                if (err is TagOperationError) _errors.tryEmit(err)
            }
        }
    }

    suspend fun affectedAppCountForTag(tagId: Long): Int =
        tagRepository.observeAppsForTag(tagId).first().size

    /**
     * Computes the categories + matching apps the sync dialog should
     * surface. Pure read; never mutates the tag store.
     */
    suspend fun loadSyncCandidates(): List<SyncCategoryCandidate> =
        categorySeeder.gatherCandidates()

    /**
     * Applies the user's sync selection. Errors from the underlying
     * repository (cap reached etc.) are silently swallowed inside
     * CategorySeeder so a partial failure does not abort the whole
     * sync — observeTags() will reflect the partial result.
     */
    fun applySyncSelection(
        candidates: List<SyncCategoryCandidate>,
        selection: SyncSelection,
    ) {
        viewModelScope.launch {
            categorySeeder.applySelection(candidates, selection)
        }
    }
}
