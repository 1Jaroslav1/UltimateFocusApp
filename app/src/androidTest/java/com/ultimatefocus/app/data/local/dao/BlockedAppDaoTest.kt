/**
 * Integration tests for BlockedAppDao against an in-memory Room database.
 * Verifies the real SQLite engine honors the strict > semantics of
 * observeActiveBlocks / observeActivePackageNames / pruneExpired.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedAppDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BlockedAppDao

    private val now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.blockedAppDao()
    }

    @After
    fun tearDown() = db.close()

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun upsertAndRetrieveRow() = runTest {
        val entity = BlockedAppEntity("com.a", now + 10_000L, now, "1h")
        dao.upsert(entity)
        assertEquals(entity, dao.getByPackage("com.a"))
    }

    @Test
    fun observeActiveBlocks_includesRowsStrictlyInFuture() = runTest {
        dao.upsert(BlockedAppEntity("com.a", now + 1L, now, "1h"))
        val active = dao.observeActiveBlocks(now).first()
        assertEquals(1, active.size)
        assertEquals("com.a", active[0].packageName)
    }

    @Test
    fun observeActiveBlocks_excludesRowsAtOrBeforeNow() = runTest {
        dao.upsert(BlockedAppEntity("com.equal", now, now - 1, "1h"))
        dao.upsert(BlockedAppEntity("com.past", now - 1, now - 2, "1h"))
        val active = dao.observeActiveBlocks(now).first()
        assertTrue(active.isEmpty())
    }

    @Test
    fun upsert_replacesExistingRowWithNewExpiry() = runTest {
        dao.upsert(BlockedAppEntity("com.a", now + 10L, now, "1h"))
        dao.upsert(BlockedAppEntity("com.a", now + 99_999L, now, "24h"))

        val row = dao.getByPackage("com.a")
        assertNotNull(row)
        assertEquals(now + 99_999L, row!!.blockedUntilEpochMs)
        assertEquals("24h", row.durationLabel)
    }

    @Test
    fun pruneExpired_deletesRowsAtOrBeforeNow() = runTest {
        dao.upsert(BlockedAppEntity("com.expired", now - 1, now - 2, "1h"))
        dao.upsert(BlockedAppEntity("com.equal", now, now - 1, "1h"))
        dao.upsert(BlockedAppEntity("com.active", now + 10, now, "1h"))

        dao.pruneExpired(now)

        assertNull(dao.getByPackage("com.expired"))
        assertNull(dao.getByPackage("com.equal"))
        assertNotNull(dao.getByPackage("com.active"))
    }

    @Test
    fun observeActivePackageNames_returnsOnlyActivePackageNames() = runTest {
        dao.upsert(BlockedAppEntity("com.a", now + 1, now, "1h"))
        dao.upsert(BlockedAppEntity("com.past", now - 1, now - 2, "1h"))

        val active = dao.observeActivePackageNames(now).first()
        assertEquals(listOf("com.a"), active)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun pruneExpired_onEmptyTable_doesNotThrow() = runTest {
        dao.pruneExpired(now) // should not throw
        assertTrue(dao.observeActiveBlocks(now).first().isEmpty())
    }

    @Test
    fun getByPackage_returnsNullForUnknownPackage() = runTest {
        assertNull(dao.getByPackage("com.nonexistent"))
    }

    @Test
    fun deleteByPackage_removesActiveBlock() = runTest {
        dao.upsert(BlockedAppEntity("com.a", now + 1, now, "1h"))
        dao.deleteByPackage("com.a")
        assertNull(dao.getByPackage("com.a"))
    }
}
