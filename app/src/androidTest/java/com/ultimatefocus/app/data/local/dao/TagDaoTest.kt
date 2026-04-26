/**
 * Integration tests for TagDao + AppTagDao against an in-memory Room
 * database. Verifies: ordered observe by name, color uniqueness via the
 * unique index, count, FK ON DELETE CASCADE removes app↔tag rows, bulk
 * deleteByPackagesForTag, deleteAllForTag.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import com.ultimatefocus.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tagDao: TagDao
    private lateinit var appTagDao: AppTagDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tagDao = db.tagDao()
        appTagDao = db.appTagDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndRetrieveTagById() = runTest {
        val id = tagDao.insert(
            TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = 0L),
        )
        assertNotNull(tagDao.getById(id))
    }

    @Test
    fun observeTagsReturnsRowsOrderedByNameNoCase() = runTest {
        tagDao.insert(TagEntity(name = "beta", colorHex = "#5B8DB8", createdAtEpochMs = 0L))
        tagDao.insert(TagEntity(name = "Alpha", colorHex = "#6AAB72", createdAtEpochMs = 0L))

        val tags = tagDao.observeTags().first()
        assertEquals(listOf("Alpha", "beta"), tags.map { it.name })
    }

    @Test
    fun uniqueColorHexIndexRejectsDuplicate() = runTest {
        tagDao.insert(TagEntity(name = "First", colorHex = "#5B8DB8", createdAtEpochMs = 0L))
        try {
            tagDao.insert(TagEntity(name = "Second", colorHex = "#5B8DB8", createdAtEpochMs = 0L))
            fail("Expected duplicate-color insert to fail")
        } catch (_: Exception) {
            // Expected — Room throws SQLiteConstraintException
        }
    }

    @Test
    fun foreignKeyCascadeRemovesAppTagsOnTagDelete() = runTest {
        val tagId = tagDao.insert(
            TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = 0L),
        )
        appTagDao.insert(AppTagCrossRef(packageName = "com.a", tagId = tagId))
        appTagDao.insert(AppTagCrossRef(packageName = "com.b", tagId = tagId))

        tagDao.deleteById(tagId)

        assertTrue(appTagDao.observePackagesForTag(tagId).first().isEmpty())
    }

    @Test
    fun appTagInsertIsIdempotent() = runTest {
        val tagId = tagDao.insert(
            TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = 0L),
        )
        appTagDao.insert(AppTagCrossRef("com.a", tagId))
        appTagDao.insert(AppTagCrossRef("com.a", tagId))
        assertEquals(listOf("com.a"), appTagDao.observePackagesForTag(tagId).first())
    }

    @Test
    fun deleteAllForTagRemovesAllRefs() = runTest {
        val tagId = tagDao.insert(
            TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = 0L),
        )
        appTagDao.insert(AppTagCrossRef("com.a", tagId))
        appTagDao.insert(AppTagCrossRef("com.b", tagId))

        appTagDao.deleteAllForTag(tagId)
        assertTrue(appTagDao.observePackagesForTag(tagId).first().isEmpty())
    }

    @Test
    fun deleteByPackagesForTagRemovesOnlyTargetedRows() = runTest {
        val tagId = tagDao.insert(
            TagEntity(name = "Social", colorHex = "#5B8DB8", createdAtEpochMs = 0L),
        )
        appTagDao.insert(AppTagCrossRef("com.a", tagId))
        appTagDao.insert(AppTagCrossRef("com.b", tagId))
        appTagDao.insert(AppTagCrossRef("com.c", tagId))

        appTagDao.deleteByPackagesForTag(listOf("com.a", "com.b"), tagId)

        assertEquals(listOf("com.c"), appTagDao.observePackagesForTag(tagId).first())
    }

    @Test
    fun countReturnsTotalTags() = runTest {
        assertEquals(0, tagDao.count())
        tagDao.insert(TagEntity(name = "T1", colorHex = "#5B8DB8", createdAtEpochMs = 0L))
        tagDao.insert(TagEntity(name = "T2", colorHex = "#6AAB72", createdAtEpochMs = 0L))
        assertEquals(2, tagDao.count())
    }

    @Test
    fun getByColorMatchesCaseInsensitiveStorage() = runTest {
        tagDao.insert(TagEntity(name = "T1", colorHex = "#5B8DB8", createdAtEpochMs = 0L))
        // SQLite default is case-sensitive but the column happens to be stored upper-case;
        // we match exactly, since we control hex casing via TagColor enum.
        assertNotNull(tagDao.getByColor("#5B8DB8"))
        assertNull(tagDao.getByColor("#000000"))
    }
}
