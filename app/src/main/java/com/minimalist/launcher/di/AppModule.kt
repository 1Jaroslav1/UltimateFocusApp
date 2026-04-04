/**
 * Hilt DI modules that wire together the data layer:
 * - DatabaseModule  : provides Room database and DAOs
 * - RepositoryModule: binds AppRepository interface to its implementation
 * Covers: ULT-5
 */
package com.minimalist.launcher.di

import android.content.Context
import androidx.room.Room
import com.minimalist.launcher.data.local.AppDatabase
import com.minimalist.launcher.data.local.dao.FavoriteDao
import com.minimalist.launcher.data.preferences.UserPreferencesDataStore
import com.minimalist.launcher.data.repository.AppRepository
import com.minimalist.launcher.data.repository.AppRepositoryImpl
import com.minimalist.launcher.domain.repository.UserPreferencesSource
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
        Room.databaseBuilder(context, AppDatabase::class.java, "minimalist_launcher.db").build()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()
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
}
