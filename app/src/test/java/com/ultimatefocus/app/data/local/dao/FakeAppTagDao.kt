/**
 * In-memory stand-in for AppTagDao used by JVM unit tests so Room doesn't
 * need to be booted. Cascade behavior on tag deletion is the real DAO's
 * concern (FK ON DELETE CASCADE) — for unit tests, callers either invoke
 * deleteByTagId or rely on the repository's coordinated cleanup.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppTagDao : AppTagDao {

    private val refs = MutableStateFlow<Set<AppTagCrossRef>>(emptySet())

    fun snapshot(): Set<AppTagCrossRef> = refs.value

    override fun observeAll(): Flow<List<AppTagCrossRef>> =
        refs.map { s -> s.toList() }

    override fun observeTagIdsForApp(packageName: String): Flow<List<Long>> =
        refs.map { s -> s.filter { it.packageName == packageName }.map { it.tagId } }

    override fun observePackagesForTag(tagId: Long): Flow<List<String>> =
        refs.map { s -> s.filter { it.tagId == tagId }.map { it.packageName }.sorted() }

    override suspend fun countForApp(packageName: String): Int =
        refs.value.count { it.packageName == packageName }

    override suspend fun isAssigned(packageName: String, tagId: Long): Int? =
        if (refs.value.contains(AppTagCrossRef(packageName, tagId))) 1 else null

    override suspend fun insert(ref: AppTagCrossRef) {
        refs.value = refs.value + ref
    }

    override suspend fun insertAll(refs: List<AppTagCrossRef>) {
        this.refs.value = this.refs.value + refs
    }

    override suspend fun delete(packageName: String, tagId: Long) {
        refs.value = refs.value - AppTagCrossRef(packageName, tagId)
    }

    override suspend fun deleteByPackagesForTag(packageNames: List<String>, tagId: Long) {
        val pkgSet = packageNames.toSet()
        refs.value = refs.value.filterNot { it.tagId == tagId && it.packageName in pkgSet }.toSet()
    }

    override suspend fun deleteAllForTag(tagId: Long) {
        refs.value = refs.value.filterNot { it.tagId == tagId }.toSet()
    }
}
