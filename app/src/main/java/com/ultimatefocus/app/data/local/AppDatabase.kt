/**
 * Room database definition. Declares all entities and exposes DAOs.
 * Version bumped to 3 to add blocking_schedules + blocking_schedule_apps;
 * paired with a destructive fallback migration in AppModule since the app
 * is pre-release MVP — schema bumps wipe prior data.
 * Covers: ULT-6, ULT-20, ULT-24
 */
package com.ultimatefocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
import com.ultimatefocus.app.data.local.entity.FavoriteEntity

@Database(
    entities = [
        FavoriteEntity::class,
        BlockedAppEntity::class,
        BlockingScheduleEntity::class,
        BlockingScheduleAppEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockingScheduleDao(): BlockingScheduleDao
}
