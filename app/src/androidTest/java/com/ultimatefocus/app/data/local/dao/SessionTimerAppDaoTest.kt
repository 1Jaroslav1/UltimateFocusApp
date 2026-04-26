/**
 * Integration tests for SessionTimerAppDao against an in-memory Room
 * database. Verifies the real SQLite engine handles upsert/delete/observe
 * for the per-app In-App Reminder opt-in table.
 *
 * Covers: ULT-77
 */
package com.ultimatefocus.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.entity.SessionTimerAppEntity
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
class SessionTimerAppDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SessionTimerAppDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sessionTimerAppDao()
    }

    @After
    fun tearDown() = db.close()

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun upsertAndIsEnabled_returnsTrueForInsertedPackage() = runTest {
        dao.upsert(SessionTimerAppEntity("com.alpha", 1_000L))
        assertTrue(dao.isEnabled("com.alpha"))
    }

    @Test
    fun isEnabled_returnsFalseForUnknownPackage() = runTest {
        assertFalse(dao.isEnabled("com.never.added"))
    }

    @Test
    fun observeAll_emitsRowsSortedByPackageName() = runTest {
        dao.upsert(SessionTimerAppEntity("com.zeta", 1_000L))
        dao.upsert(SessionTimerAppEntity("com.alpha", 2_000L))
        dao.upsert(SessionTimerAppEntity("com.middle", 3_000L))
        val rows = dao.observeAll().first()
        assertEquals(
            listOf("com.alpha", "com.middle", "com.zeta"),
            rows.map { it.packageName },
        )
    }

    @Test
    fun observePackages_emitsAllPackageNames() = runTest {
        dao.upsert(SessionTimerAppEntity("com.a", 1L))
        dao.upsert(SessionTimerAppEntity("com.b", 2L))
        assertEquals(setOf("com.a", "com.b"), dao.observePackages().first().toSet())
    }

    @Test
    fun deleteByPackage_removesTheRow() = runTest {
        dao.upsert(SessionTimerAppEntity("com.a", 1L))
        dao.deleteByPackage("com.a")
        assertFalse(dao.isEnabled("com.a"))
        assertTrue(dao.observeAll().first().isEmpty())
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun upsert_isIdempotent_keyedByPackageName() = runTest {
        dao.upsert(SessionTimerAppEntity("com.a", 1L))
        dao.upsert(SessionTimerAppEntity("com.a", 9_999L))
        val rows = dao.observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(9_999L, rows.first().addedEpochMs)
    }

    @Test
    fun deleteByPackage_isNoOpForUnknownPackage() = runTest {
        dao.upsert(SessionTimerAppEntity("com.a", 1L))
        dao.deleteByPackage("com.unknown")
        assertTrue(dao.isEnabled("com.a"))
    }

    @Test
    fun emptyTable_observeReturnsEmptyList() = runTest {
        assertTrue(dao.observeAll().first().isEmpty())
        assertTrue(dao.observePackages().first().isEmpty())
    }
}
