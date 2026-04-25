/**
 * In-memory test double for TagRepository. Provides only the read-side
 * methods needed by BlockedAppChecker plus simple seed/assign helpers so
 * tests can populate the cross-ref. Wraps observeAppToTagsMap reactively
 * via a MutableStateFlow so tests can swap state mid-test if needed.
 * Covers: ULT-72, ULT-73, ULT-75
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTagRepository : TagRepository {

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    private val _assignments = MutableStateFlow<Map<String, Set<Long>>>(emptyMap())

    fun seedTag(id: Long, name: String, color: TagColor) {
        _tags.value = _tags.value + Tag(id = id, name = name, color = color)
    }

    fun seedAssignment(packageName: String, tagId: Long) {
        val current = _assignments.value
        val pkgTags = current[packageName].orEmpty().toMutableSet()
        pkgTags += tagId
        _assignments.value = current + (packageName to pkgTags)
    }

    override fun observeTags(): Flow<List<Tag>> = _tags

    override fun observeTagsForApp(packageName: String): Flow<List<Tag>> =
        _assignments.map { map ->
            val ids = map[packageName].orEmpty()
            _tags.value.filter { it.id in ids }
        }

    override fun observeAppToTagsMap(): Flow<Map<String, List<Tag>>> =
        _assignments.map { map ->
            val byId = _tags.value.associateBy { it.id }
            map.mapValues { (_, ids) -> ids.mapNotNull { byId[it] } }
        }

    override fun observeAppsForTag(tagId: Long): Flow<List<String>> =
        _assignments.map { map ->
            map.filterValues { tagId in it }.keys.sorted()
        }

    override suspend fun getTag(id: Long): Tag? = _tags.value.firstOrNull { it.id == id }

    override suspend fun createTag(name: String, color: TagColor): Result<Tag> {
        val tag = Tag(id = (_tags.value.maxOfOrNull { it.id } ?: 0L) + 1, name = name, color = color)
        _tags.value = _tags.value + tag
        return Result.success(tag)
    }

    override suspend fun updateTag(id: Long, name: String, color: TagColor): Result<Unit> {
        _tags.value = _tags.value.map {
            if (it.id == id) it.copy(name = name, color = color) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteTag(id: Long): Result<Unit> {
        _tags.value = _tags.value.filterNot { it.id == id }
        _assignments.value = _assignments.value.mapValues { (_, ids) -> ids - id }
        return Result.success(Unit)
    }

    override suspend fun assignTag(packageName: String, tagId: Long): Result<Unit> {
        seedAssignment(packageName, tagId)
        return Result.success(Unit)
    }

    override suspend fun unassignTag(packageName: String, tagId: Long) {
        val current = _assignments.value
        val pkgTags = current[packageName].orEmpty().toMutableSet()
        pkgTags -= tagId
        _assignments.value = current + (packageName to pkgTags)
    }

    override suspend fun bulkAssignTag(packageNames: List<String>, tagId: Long): Result<Unit> {
        packageNames.forEach { seedAssignment(it, tagId) }
        return Result.success(Unit)
    }

    override suspend fun bulkUnassignTag(packageNames: List<String>, tagId: Long) {
        packageNames.forEach { unassignTag(it, tagId) }
    }
}
