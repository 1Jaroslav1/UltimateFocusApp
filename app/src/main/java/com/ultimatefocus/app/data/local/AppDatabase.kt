/**
 * Room database definition. Declares all entities and exposes DAOs.
 * Version bumped to 2 to add blocked_apps; paired with a destructive
 * fallback migration in AppModule since the app is pre-release MVP.
 * Covers: ULT-6, ULT-20
 */
package com.ultimatefocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import com.ultimatefocus.app.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class, BlockedAppEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun blockedAppDao(): BlockedAppDao
}
