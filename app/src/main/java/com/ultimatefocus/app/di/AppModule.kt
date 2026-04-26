/**
 * Hilt DI modules that wire together the data layer:
 * - DatabaseModule  : provides Room database and DAOs
 * - RepositoryModule: binds AppRepository + UserPreferencesSource + SystemActions
 *                     + BlockedAppRepository + BlockingScheduleRepository
 *                     + TagRepository + BlockedTagRepository + Clock + ZoneIdProvider
 * Database uses fallbackToDestructiveMigration because the app is pre-release
 * MVP — schema bumps (ULT-20 added blocked_apps, ULT-24 added blocking_schedules,
 * ULT-72/73 added tags + app_tags, ULT-75 added blocked_tags) wipe prior data.
 * Also binds AppUsageStatsRepository (UsageStatsManager-backed) so the
 * schedule editor can sort the app picker by weekly foreground usage.
 * Also binds SessionTimerRepository — the In-App Reminder feature surface
 * (global feature flags + per-app opt-in) — so the settings page and the
 * launch interceptors share a single source of truth.
 * Covers: ULT-5, ULT-16, ULT-17, ULT-18, ULT-20, ULT-24, ULT-72, ULT-73, ULT-75, ULT-76
 */
package com.ultimatefocus.app.di

import android.content.Context
import androidx.room.Room
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.dao.AppTagDao
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.BlockedTagDao
import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.dao.TagDao
import com.ultimatefocus.app.data.preferences.UserPreferencesDataStore
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.data.repository.AppRepositoryImpl
import com.ultimatefocus.app.data.repository.AppUsageStatsRepositoryImpl
import com.ultimatefocus.app.data.repository.BlockedAppRepositoryImpl
import com.ultimatefocus.app.data.repository.BlockedTagRepositoryImpl
import com.ultimatefocus.app.data.repository.BlockingScheduleRepositoryImpl
import com.ultimatefocus.app.data.repository.SessionTimerRepositoryImpl
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
import com.ultimatefocus.app.data.system.SystemActionsImpl
import com.ultimatefocus.app.domain.repository.AppUsageStatsRepository
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.domain.service.SystemClock
import com.ultimatefocus.app.domain.service.SystemZoneIdProvider
import com.ultimatefocus.app.domain.service.ZoneIdProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "minimalist_launcher.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideBlockedAppDao(database: AppDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideBlockingScheduleDao(database: AppDatabase): BlockingScheduleDao =
        database.blockingScheduleDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideAppTagDao(database: AppDatabase): AppTagDao = database.appTagDao()

    @Provides
    fun provideBlockedTagDao(database: AppDatabase): BlockedTagDao = database.blockedTagDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesSource(impl: UserPreferencesDataStore): UserPreferencesSource

    @Binds
    @Singleton
    abstract fun bindSystemActions(impl: SystemActionsImpl): SystemActions

    @Binds
    @Singleton
    abstract fun bindBlockedAppRepository(impl: BlockedAppRepositoryImpl): BlockedAppRepository

    @Binds
    @Singleton
    abstract fun bindBlockingScheduleRepository(
        impl: BlockingScheduleRepositoryImpl,
    ): BlockingScheduleRepository

    @Binds
    @Singleton
    abstract fun bindAppUsageStatsRepository(
        impl: AppUsageStatsRepositoryImpl,
    ): AppUsageStatsRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindBlockedTagRepository(impl: BlockedTagRepositoryImpl): BlockedTagRepository

    @Binds
    @Singleton
    abstract fun bindSessionTimerRepository(
        impl: SessionTimerRepositoryImpl,
    ): SessionTimerRepository

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindZoneIdProvider(impl: SystemZoneIdProvider): ZoneIdProvider
}
