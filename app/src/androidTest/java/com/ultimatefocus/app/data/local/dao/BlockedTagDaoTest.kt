/**
 * Integration tests for BlockedTagDao against an in-memory Room database.
 * Verifies strict > expiry, prune behavior, the cross-ref join used by
 * observeActivePackages, and FK ON DELETE CASCADE from tags → blocked_tags.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import com.ultimatefocus.app.data.local.entity.BlockedTagEntity
import com.ultimatefocus.app.data.local.entity.TagEntity
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
class BlockedTagDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BlockedTagDao
    private lateinit var tagDao: TagDao
    private lateinit var appTagDao: AppTagDao

    private val now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.blockedTagDao()
        tagDao = db.tagDao()
        appTagDao = db.appTagDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertAndRetrieve() = runTest {
        val tagId = tagDao.insert(TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = now))
        val entity = BlockedTagEntity(
            tagId = tagId,
            blockedUntilEpochMs = now + 1_000L,
            blockedSinceEpochMs = now,
            durationLabel = "1h",
        )
        dao.upsert(entity)
        assertEquals(entity, dao.getByTag(tagId))
    }

    @Test
    fun observeActiveBlocksUsesStrictGreaterThanSemantics() = runTest {
        val tagId = tagDao.insert(TagEntity(name = "T", colorHex = "#5B8DB8", createdAtEpochMs = now))
        dao.upsert(BlockedTagEntity(tagId, now + 1L, now, "1h"))
        assertEquals(1, dao.observeActiveBlocks(now).first().size)
        // At exact expiry, the row is hidden from the active observer.
        assertTrue(dao.observeActiveBlocks(now + 1L).first().isEmpty())
    }

    @Test
    fun pruneExpiredRemovesRowsAtOrBeforeNow() = runTest {
        val a = tagDao.insert(TagEntity(name = "A", colorHex = "#5B8DB8", createdAtEpochMs = now))
        val b = tagDao.insert(TagEntity(name = "B", colorHex = "#6AAB72", createdAtEpochMs = now))
        dao.upsert(BlockedTagEntity(a, now - 1L, now - 2L, "1h"))
        dao.upsert(BlockedTagEntity(b, now + 100L, now, "1h"))

        dao.pruneExpired(now)
        assertNull(dao.getByTag(a))
        assertNotNull(dao.getByTag(b))
    }

    @Test
    fun observeActivePackagesJoinsThroughAppTags() = runTest {
        val tagId = tagDao.insert(TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = now))
        appTagDao.insert(AppTagCrossRef(packageName = "com.a", tagId = tagId))
        appTagDao.insert(AppTagCrossRef(packageName = "com.b", tagId = tagId))
        dao.upsert(BlockedTagEntity(tagId, now + 1_000L, now, "1h"))

        val rows = dao.observeActivePackages(now).first()
        assertEquals(setOf("com.a", "com.b"), rows.map { it.packageName }.toSet())
        rows.forEach { assertEquals(now + 1_000L, it.blockedUntilEpochMs) }
    }

    @Test
    fun deletingTagCascadesBlockedTagsRow() = runTest {
        val tagId = tagDao.insert(TagEntity(name = "Doomed", colorHex = "#5B8DB8", createdAtEpochMs = now))
        dao.upsert(BlockedTagEntity(tagId, now + 1L, now, "1h"))

        tagDao.deleteById(tagId)

        assertNull(dao.getByTag(tagId))
    }
}
