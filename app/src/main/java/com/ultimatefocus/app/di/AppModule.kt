/**
 * Hilt DI modules that wire together the data layer:
 * - DatabaseModule  : provides Room database and DAOs
 * - RepositoryModule: binds AppRepository + UserPreferencesSource + SystemActions
 *                     + BlockedAppRepository + BlockingScheduleRepository
 *                     + Clock + ZoneIdProvider
 * Database uses fallbackToDestructiveMigration because the app is pre-release
 * MVP — schema bumps (e.g. ULT-20 adding blocked_apps, ULT-24 adding
 * blocking_schedules) wipe prior data.
 * Covers: ULT-5, ULT-16, ULT-17, ULT-18, ULT-20, ULT-24
 */
package com.ultimatefocus.app.di

import android.content.Context
import androidx.room.Room
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.preferences.UserPreferencesDataStore
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.data.repository.AppRepositoryImpl
import com.ultimatefocus.app.data.repository.BlockedAppRepositoryImpl
import com.ultimatefocus.app.data.repository.BlockingScheduleRepositoryImpl
import com.ultimatefocus.app.data.system.SystemActionsImpl
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
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
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindZoneIdProvider(impl: SystemZoneIdProvider): ZoneIdProvider
}
