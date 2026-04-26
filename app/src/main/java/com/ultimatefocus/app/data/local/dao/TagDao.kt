/**
 * Room DAO for tag CRUD. Color-uniqueness violations surface as Room
 * SQLiteConstraintException via the unique index on TagEntity.colorHex —
 * the repository checks first to surface a domain-level error message,
 * but the index is the safety net.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ultimatefocus.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE colorHex = :hex LIMIT 1")
    suspend fun getByColor(hex: String): TagEntity?

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: TagEntity): Long

    @Update
    suspend fun update(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
}
