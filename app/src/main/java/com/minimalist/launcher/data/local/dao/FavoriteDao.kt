/**
 * Room DAO for reading and writing favorite app entries.
 * All queries that observe data return Flow so the UI reacts to changes automatically.
 * Covers: ULT-6
 */
package com.minimalist.launcher.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minimalist.launcher.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY displayOrder ASC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT packageName FROM favorites")
    fun getFavoritePackageNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE packageName = :packageName)")
    suspend fun isFavorite(packageName: String): Boolean

    @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM favorites")
    suspend fun getNextOrder(): Int
}
