/**
 * In-memory stand-in for TagDao used by JVM unit tests so Room (which needs
 * an Android runtime) doesn't need to be booted to exercise repository logic.
 * Mirrors the real DAO's ordering: name COLLATE NOCASE ASC.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTagDao : TagDao {

    private val rows = MutableStateFlow<Map<Long, TagEntity>>(emptyMap())
    private var nextId: Long = 1L

    val size: Int get() = rows.value.size

    fun snapshot(): List<TagEntity> = rows.value.values
        .sortedBy { it.name.lowercase() }

    override fun observeTags(): Flow<List<TagEntity>> =
        rows.map { m -> m.values.sortedBy { it.name.lowercase() } }

    override suspend fun getById(id: Long): TagEntity? = rows.value[id]

    override suspend fun getByColor(hex: String): TagEntity? =
        rows.value.values.firstOrNull { it.colorHex.equals(hex, ignoreCase = true) }

    override suspend fun count(): Int = rows.value.size

    override suspend fun insert(entity: TagEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id.also { nextId = maxOf(nextId, it + 1) }
        rows.value = rows.value + (id to entity.copy(id = id))
        return id
    }

    override suspend fun update(entity: TagEntity) {
        if (entity.id !in rows.value) return
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value - id
    }
}
