/**
 * Room-backed implementation of TagRepository. All write paths return
 * Result<...> with a TagOperationError on failure so the UI can surface
 * exact reasons (cap reached, color in use, app cap reached, currently
 * blocked) without try/catch. Reads expose Flow so the UI auto-refreshes.
 * Cross-ref deletion happens via FK cascade in AppTagCrossRef when a tag
 * row is removed; we also wipe the cross-ref explicitly inside deleteTag
 * so the contract is testable without relying on FK cascade simulation.
 * Every WRITE method (update, delete, assign, bulkAssign) refuses while a
 * tag-block on the target tag is active (ULT-75 follow-up): mirrors the
 * no-self-unblock invariant — users cannot weaken an active tag-block by
 * editing the tag, deleting it, or unassigning its apps. Unassign methods
 * silently no-op while blocked (the picker UI gates user input; the
 * repository early-return is defense in depth).
 * Covers: ULT-72, ULT-73, ULT-75
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.data.local.dao.AppTagDao
import com.ultimatefocus.app.data.local.dao.BlockedTagDao
import com.ultimatefocus.app.data.local.dao.TagDao
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import com.ultimatefocus.app.data.local.entity.TagEntity
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.MAX_TAGS_PER_APP
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.repository.TagRepository
import com.ultimatefocus.app.domain.service.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val appTagDao: AppTagDao,
    private val blockedTagDao: BlockedTagDao,
    private val clock: Clock,
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeTagsForApp(packageName: String): Flow<List<Tag>> =
        combine(tagDao.observeTags(), appTagDao.observeTagIdsForApp(packageName)) { tags, ids ->
            val idSet = ids.toSet()
            tags.mapNotNull { it.toDomain() }.filter { it.id in idSet }
        }

    override fun observeAppToTagsMap(): Flow<Map<String, List<Tag>>> =
        combine(tagDao.observeTags(), appTagDao.observeAll()) { tags, refs ->
            val byId = tags.mapNotNull { it.toDomain() }.associateBy { it.id }
            refs.groupBy { it.packageName }
                .mapValues { (_, rs) ->
                    rs.mapNotNull { byId[it.tagId] }
                        .sortedBy { it.name.lowercase() }
                }
        }

    override fun observeAppsForTag(tagId: Long): Flow<List<String>> =
        appTagDao.observePackagesForTag(tagId)

    override suspend fun getTag(id: Long): Tag? = tagDao.getById(id)?.toDomain()

    override suspend fun createTag(name: String, color: TagColor): Result<Tag> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(TagOperationError.NameBlank(name))
        }
        if (tagDao.count() >= MAX_TAGS) {
            return Result.failure(TagOperationError.TagLimitReached)
        }
        if (tagDao.getByColor(color.hex) != null) {
            return Result.failure(TagOperationError.ColorAlreadyUsed)
        }
        val newId = tagDao.insert(
            TagEntity(
                name = trimmed,
                colorHex = color.hex,
                createdAtEpochMs = clock.now(),
            ),
        )
        return Result.success(Tag(id = newId, name = trimmed, color = color))
    }

    override suspend fun updateTag(id: Long, name: String, color: TagColor): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(TagOperationError.NameBlank(name))
        }
        val existing = tagDao.getById(id) ?: return Result.failure(TagOperationError.TagNotFound)
        if (isTagBlocked(id)) {
            return Result.failure(TagOperationError.TagCurrentlyBlocked)
        }
        val collision = tagDao.getByColor(color.hex)
        if (collision != null && collision.id != id) {
            return Result.failure(TagOperationError.ColorAlreadyUsed)
        }
        tagDao.update(existing.copy(name = trimmed, colorHex = color.hex))
        return Result.success(Unit)
    }

    override suspend fun deleteTag(id: Long): Result<Unit> {
        if (tagDao.getById(id) == null) {
            return Result.failure(TagOperationError.TagNotFound)
        }
        if (isTagBlocked(id)) {
            return Result.failure(TagOperationError.TagCurrentlyBlocked)
        }
        // Explicit cleanup of cross-refs so the contract is testable without
        // relying on Room's FK ON DELETE CASCADE; the FK still provides a
        // safety net at the storage layer.
        appTagDao.deleteAllForTag(id)
        tagDao.deleteById(id)
        return Result.success(Unit)
    }

    override suspend fun assignTag(packageName: String, tagId: Long): Result<Unit> {
        if (appTagDao.isAssigned(packageName, tagId) != null) return Result.success(Unit)
        if (appTagDao.countForApp(packageName) >= MAX_TAGS_PER_APP) {
            return Result.failure(TagOperationError.AppTagLimitReached)
        }
        if (tagDao.getById(tagId) == null) {
            return Result.failure(TagOperationError.TagNotFound)
        }
        if (isTagBlocked(tagId)) {
            return Result.failure(TagOperationError.TagCurrentlyBlocked)
        }
        appTagDao.insert(AppTagCrossRef(packageName, tagId))
        return Result.success(Unit)
    }

    override suspend fun unassignTag(packageName: String, tagId: Long) {
        // Silent no-op while blocked — the picker UI disables blocked rows so
        // the user cannot reach this branch through normal interaction.
        if (isTagBlocked(tagId)) return
        appTagDao.delete(packageName, tagId)
    }

    override suspend fun bulkAssignTag(
        packageNames: List<String>,
        tagId: Long,
    ): Result<Unit> {
        if (tagDao.getById(tagId) == null) {
            return Result.failure(TagOperationError.TagNotFound)
        }
        if (isTagBlocked(tagId)) {
            return Result.failure(TagOperationError.TagCurrentlyBlocked)
        }
        // Per-app cap is enforced individually so that an over-cap app does not
        // poison the whole batch — apps with room get the tag, apps at the cap
        // get skipped. The caller is told if at least one app was skipped.
        var anySkipped = false
        for (pkg in packageNames) {
            if (appTagDao.isAssigned(pkg, tagId) != null) continue
            if (appTagDao.countForApp(pkg) >= MAX_TAGS_PER_APP) {
                anySkipped = true
                continue
            }
            appTagDao.insert(AppTagCrossRef(pkg, tagId))
        }
        return if (anySkipped) {
            Result.failure(TagOperationError.AppTagLimitReached)
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun bulkUnassignTag(packageNames: List<String>, tagId: Long) {
        if (packageNames.isEmpty()) return
        if (isTagBlocked(tagId)) return
        appTagDao.deleteByPackagesForTag(packageNames, tagId)
    }

    private suspend fun isTagBlocked(tagId: Long): Boolean {
        val row = blockedTagDao.getByTag(tagId) ?: return false
        return row.blockedUntilEpochMs > clock.now()
    }

    private fun TagEntity.toDomain(): Tag? {
        val color = TagColor.fromHex(colorHex) ?: return null
        return Tag(id = id, name = name, color = color)
    }
}
