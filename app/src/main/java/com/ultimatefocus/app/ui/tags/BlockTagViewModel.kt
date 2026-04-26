/**
 * ViewModel for the Block-by-Tag flow. Exposes a snapshot of the apps that
 * will be blocked when the user picks a duration (preview), and commits
 * the block via BlockedTagRepository.blockTag. Also exposes the live set
 * of blocked tag ids + expiry so the Manage Tags row can show its lock
 * badge.
 *
 * preview() resolves package names to user-visible app labels via the
 * AppRepository so the confirmation dialog shows e.g. "Telegram" instead
 * of "org.telegram.messenger" — addresses the iteration feedback that the
 * raw packageName was unfriendly. Falls back to packageName when an app
 * isn't (or is no longer) installed.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedTag
import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-friendly pairing of packageName + display label for the block-tag preview. */
data class BlockTagPreviewApp(
    val packageName: String,
    val label: String,
)

@HiltViewModel
class BlockTagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val blockedTagRepository: BlockedTagRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    /** tagId → BlockedTag for every active tag block. Empty when nothing is blocked. */
    val activeTagBlocks: StateFlow<Map<Long, BlockedTag>> =
        blockedTagRepository.observeActiveTagBlocks()
            .map { list -> list.associateBy { it.tagId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyMap(),
            )

    suspend fun preview(tagId: Long): List<BlockTagPreviewApp> {
        val packages = tagRepository.observeAppsForTag(tagId).first()
        if (packages.isEmpty()) return emptyList()
        val labelByPackage = appRepository.getInstalledApps().first()
            .associate { it.packageName to it.label }
        return packages
            .map { pkg ->
                BlockTagPreviewApp(
                    packageName = pkg,
                    label = labelByPackage[pkg] ?: pkg,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun blockTag(tagId: Long, duration: BlockDuration) {
        viewModelScope.launch {
            blockedTagRepository.blockTag(tagId, duration)
        }
    }
}
