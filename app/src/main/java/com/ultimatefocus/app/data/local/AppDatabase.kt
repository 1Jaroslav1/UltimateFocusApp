/**
 * Room database definition. Declares all entities and exposes DAOs.
 * Version bumped to 4 to add tags + app_tags (app tagging system, ULT-72/73)
 * on top of v3 (blocking_schedules + blocking_schedule_apps, ULT-24);
 * paired with destructive fallback in AppModule since the app is pre-release MVP.
 * Covers: ULT-6, ULT-20, ULT-24, ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultimatefocus.app.data.local.dao.AppTagDao
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.dao.TagDao
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
import com.ultimatefocus.app.data.local.entity.FavoriteEntity
import com.ultimatefocus.app.data.local.entity.TagEntity

@Database(
    entities = [
        FavoriteEntity::class,
        BlockedAppEntity::class,
        BlockingScheduleEntity::class,
        BlockingScheduleAppEntity::class,
        TagEntity::class,
        AppTagCrossRef::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockingScheduleDao(): BlockingScheduleDao
    abstract fun tagDao(): TagDao
    abstract fun appTagDao(): AppTagDao
}
