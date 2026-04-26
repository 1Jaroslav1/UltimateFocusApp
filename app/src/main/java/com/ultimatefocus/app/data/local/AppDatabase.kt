/**
 * Room database definition. Declares all entities and exposes DAOs.
 * Version timeline: v2 added blocked_apps (ULT-20). v3 added
 * blocking_schedules + blocking_schedule_apps (ULT-24). v4 added tags +
 * app_tags (ULT-72/73). v5 adds blocked_tags (ULT-75). Paired with the
 * destructive fallback in AppModule since the app is pre-release MVP, so
 * each schema bump simply wipes prior data on first run.
 * Covers: ULT-6, ULT-20, ULT-24, ULT-72, ULT-73, ULT-75
 */
package com.ultimatefocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultimatefocus.app.data.local.dao.AppTagDao
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.BlockedTagDao
import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.dao.TagDao
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import com.ultimatefocus.app.data.local.entity.BlockedTagEntity
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
        BlockedTagEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockingScheduleDao(): BlockingScheduleDao
    abstract fun tagDao(): TagDao
    abstract fun appTagDao(): AppTagDao
    abstract fun blockedTagDao(): BlockedTagDao
}
