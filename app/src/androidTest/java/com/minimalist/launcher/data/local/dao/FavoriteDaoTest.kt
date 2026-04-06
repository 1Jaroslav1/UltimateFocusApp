/**
 * Integration tests for FavoriteDao using an in-memory Room database.
 * Verifies all CRUD operations and query correctness against the real
 * SQLite engine — no mocks or fakes for the database layer.
 * Covers: ULT-6
 */
package com.minimalist.launcher.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimalist.launcher.data.local.AppDatabase
import com.minimalist.launcher.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteDao()
    }

    @After
    fun tearDown() = db.close()

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun insertFavorite_canBeRetrieved() = runTest {
        val entity = FavoriteEntity("com.test.app", "com.test.app/MainActivity", 0)
        dao.insertFavorite(entity)
        val favorites = dao.getAllFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("com.test.app", favorites[0].packageName)
    }

    @Test
    fun getAllFavorites_returnedInDisplayOrder() = runTest {
        dao.insertFavorite(FavoriteEntity("com.b", "com.b/A", 2))
        dao.insertFavorite(FavoriteEntity("com.a", "com.a/A", 0))
        dao.insertFavorite(FavoriteEntity("com.c", "com.c/A", 1))
        val favorites = dao.getAllFavorites().first()
        assertEquals(listOf("com.a", "com.c", "com.b"), favorites.map { it.packageName })
    }

    @Test
    fun isFavorite_returnsTrueAfterInsert() = runTest {
        dao.insertFavorite(FavoriteEntity("com.test.app", "com.test.app/A", 0))
        assertTrue(dao.isFavorite("com.test.app"))
    }

    @Test
    fun deleteByPackageName_removesEntry() = runTest {
        dao.insertFavorite(FavoriteEntity("com.test.app", "com.test.app/A", 0))
        dao.deleteByPackageName("com.test.app")
        val favorites = dao.getAllFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun getNextOrder_returnsZeroForEmptyTable() = runTest {
        assertEquals(0, dao.getNextOrder())
    }

    @Test
    fun getNextOrder_incrementsAfterEachInsert() = runTest {
        dao.insertFavorite(FavoriteEntity("com.a", "com.a/A", dao.getNextOrder()))
        dao.insertFavorite(FavoriteEntity("com.b", "com.b/A", dao.getNextOrder()))
        assertEquals(2, dao.getNextOrder())
    }

    @Test
    fun insertWithConflict_replacesExistingEntry() = runTest {
        dao.insertFavorite(FavoriteEntity("com.test.app", "com.test.app/A", 0))
        dao.insertFavorite(FavoriteEntity("com.test.app", "com.test.app/B", 5))
        val favorites = dao.getAllFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals(5, favorites[0].displayOrder)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun isFavorite_returnsFalseForNonexistentPackage() = runTest {
        assertFalse(dao.isFavorite("com.nonexistent"))
    }

    @Test
    fun deleteByPackageName_doesNothingForNonexistentPackage() = runTest {
        dao.deleteByPackageName("com.nonexistent") // should not throw
        assertTrue(dao.getAllFavorites().first().isEmpty())
    }

    @Test
    fun getFavoritePackageNames_returnsOnlyPackageNames() = runTest {
        dao.insertFavorite(FavoriteEntity("com.alpha", "com.alpha/A", 0))
        dao.insertFavorite(FavoriteEntity("com.beta", "com.beta/A", 1))
        val packageNames = dao.getFavoritePackageNames().first()
        assertTrue(packageNames.containsAll(listOf("com.alpha", "com.beta")))
    }
}
