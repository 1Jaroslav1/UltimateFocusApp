/**
 * In-memory stand-in for SessionTimerAppDao used by JVM unit tests so the
 * repository logic and any consumers can be exercised without booting Room.
 *
 * Covers: ULT-77
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.SessionTimerAppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionTimerAppDao : SessionTimerAppDao {

    private val rows = MutableStateFlow<Map<String, SessionTimerAppEntity>>(emptyMap())

    val size: Int get() = rows.value.size

    override fun observeAll(): Flow<List<SessionTimerAppEntity>> =
        rows.map { m -> m.values.sortedBy { it.packageName } }

    override fun observePackages(): Flow<List<String>> =
        rows.map { m -> m.keys.toList() }

    override suspend fun isEnabled(packageName: String): Boolean =
        packageName in rows.value

    override suspend fun upsert(entity: SessionTimerAppEntity) {
        rows.value = rows.value + (entity.packageName to entity)
    }

    override suspend fun deleteByPackage(packageName: String) {
        rows.value = rows.value - packageName
    }
}
