/**
 * Repository contract for managing tags and their assignments to apps.
 * Hosts the create/edit/delete API for ULT-72 and the assign/unassign
 * (single + bulk) API for ULT-73; observes both for the UI layer.
 * Domain-level invariants: max 10 tags total, max 5 tags per app, color
 * uniqueness across tags.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import kotlinx.coroutines.flow.Flow

/**
 * Sealed error returned by repository methods that can fail for predictable,
 * user-presentable reasons. The UI translates each variant into copy.
 */
sealed class TagOperationError(message: String) : Throwable(message) {
    data object TagLimitReached : TagOperationError("Tag limit reached (max 10 tags).")
    data object ColorAlreadyUsed : TagOperationError("That color is already used by another tag.")
    data object AppTagLimitReached : TagOperationError("App already has the maximum 5 tags.")
    data object TagNotFound : TagOperationError("Tag not found.")
    data class NameBlank(val raw: String) : TagOperationError("Tag name cannot be blank.")
}

interface TagRepository {

    fun observeTags(): Flow<List<Tag>>

    fun observeTagsForApp(packageName: String): Flow<List<Tag>>

    /** package-name -> ordered list of tags assigned to that app. */
    fun observeAppToTagsMap(): Flow<Map<String, List<Tag>>>

    /** Stream of package names assigned to a tag (used for "Block by Tag" preview). */
    fun observeAppsForTag(tagId: Long): Flow<List<String>>

    suspend fun getTag(id: Long): Tag?

    suspend fun createTag(name: String, color: TagColor): Result<Tag>

    suspend fun updateTag(id: Long, name: String, color: TagColor): Result<Unit>

    suspend fun deleteTag(id: Long): Result<Unit>

    suspend fun assignTag(packageName: String, tagId: Long): Result<Unit>

    suspend fun unassignTag(packageName: String, tagId: Long)

    suspend fun bulkAssignTag(packageNames: List<String>, tagId: Long): Result<Unit>

    suspend fun bulkUnassignTag(packageNames: List<String>, tagId: Long)
}
