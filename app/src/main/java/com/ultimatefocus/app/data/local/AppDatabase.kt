/**
 * Room database definition. Declares all entities and exposes DAOs.
 * Covers: ULT-6
 */
package com.ultimatefocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
