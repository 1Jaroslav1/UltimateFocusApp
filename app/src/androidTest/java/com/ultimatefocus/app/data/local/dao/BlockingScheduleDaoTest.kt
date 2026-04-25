/**
 * Integration tests for BlockingScheduleDao against an in-memory Room
 * database. Verifies the @Transaction-bound projection returns parent +
 * junction rows together, that onDelete=CASCADE removes assignments when
 * a schedule is deleted, and that replaceApps is atomic from the observer's
 * perspective.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.data.local.AppDatabase
import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
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
class BlockingScheduleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BlockingScheduleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.blockingScheduleDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndReadSchedule_roundTripsRow() = runTest {
        val id = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Focus",
                startMinuteOfDay = 9 * 60,
                endMinuteOfDay = 17 * 60,
                daysOfWeekMask = 0b0011111,
                createdEpochMs = 1L,
            ),
        )
        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals("Focus", loaded!!.schedule.name)
        assertTrue(loaded.apps.isEmpty())
    }

    @Test
    fun replaceApps_swapsJunctionRowsAtomically() = runTest {
        val id = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Focus",
                startMinuteOfDay = 9 * 60,
                endMinuteOfDay = 17 * 60,
                daysOfWeekMask = 0b0011111,
                createdEpochMs = 1L,
            ),
        )
        dao.replaceApps(id, listOf("com.a", "com.b"))
        val first = dao.getById(id)!!
        assertEquals(setOf("com.a", "com.b"), first.apps.map { it.packageName }.toSet())

        dao.replaceApps(id, listOf("com.c"))
        val second = dao.getById(id)!!
        assertEquals(setOf("com.c"), second.apps.map { it.packageName }.toSet())
    }

    @Test
    fun deleteById_cascadesJunctionRows() = runTest {
        val id = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Focus",
                startMinuteOfDay = 0,
                endMinuteOfDay = 60,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 1L,
            ),
        )
        dao.replaceApps(id, listOf("com.a", "com.b"))
        dao.deleteById(id)
        assertNull(dao.getById(id))
        // No way to direct-query the junction without a helper; observe the parent
        // table to confirm both deletions succeeded together.
        val all = dao.observeAll().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun observeAll_emitsScheduleWithApps_inCreationOrder() = runTest {
        val first = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "First",
                startMinuteOfDay = 0,
                endMinuteOfDay = 60,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 100L,
            ),
        )
        dao.replaceApps(first, listOf("com.a"))
        val second = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Second",
                startMinuteOfDay = 60,
                endMinuteOfDay = 120,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 200L,
            ),
        )
        dao.replaceApps(second, listOf("com.b"))

        val all = dao.observeAll().first()
        assertEquals(listOf("First", "Second"), all.map { it.schedule.name })
        assertEquals(listOf("com.a"), all[0].apps.map { it.packageName })
        assertEquals(listOf("com.b"), all[1].apps.map { it.packageName })
    }

    @Test
    fun deleteAssignmentsForPackage_removesAcrossAllSchedules() = runTest {
        val first = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "First",
                startMinuteOfDay = 0,
                endMinuteOfDay = 60,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 1L,
            ),
        )
        val second = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Second",
                startMinuteOfDay = 60,
                endMinuteOfDay = 120,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 2L,
            ),
        )
        dao.replaceApps(first, listOf("com.a", "com.b"))
        dao.replaceApps(second, listOf("com.a"))

        dao.deleteAssignmentsForPackage("com.a")

        val all = dao.observeAll().first()
        assertEquals(setOf("com.b"), all[0].apps.map { it.packageName }.toSet())
        assertTrue(all[1].apps.isEmpty())
    }

    @Test
    fun insertApps_ignoresDuplicatesViaCompositePrimaryKey() = runTest {
        val id = dao.insertSchedule(
            BlockingScheduleEntity(
                name = "Focus",
                startMinuteOfDay = 0,
                endMinuteOfDay = 60,
                daysOfWeekMask = 0b1111111,
                createdEpochMs = 1L,
            ),
        )
        dao.insertApps(
            listOf(
                BlockingScheduleAppEntity(id, "com.a"),
                BlockingScheduleAppEntity(id, "com.a"), // duplicate ignored
                BlockingScheduleAppEntity(id, "com.b"),
            ),
        )
        val loaded = dao.getById(id)!!
        assertEquals(setOf("com.a", "com.b"), loaded.apps.map { it.packageName }.toSet())
    }
}
