/**
 * Unit tests for BlockedTagRepositoryImpl. Mirrors BlockedAppRepositoryImplTest:
 * verifies blockTag persists with correct expiry, strict > semantics on
 * isTagBlocked / getActive, re-blocking upserts, pruneExpired removes
 * past rows, and observeActiveBlockedPackagesByTags joins through the
 * provided cross-ref source.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.repository

import app.cash.turbine.test
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockedTagRepositoryImplTest {

    private lateinit var dao: FakeBlockedTagDao
    private lateinit var clock: FakeClock
    private lateinit var repo: BlockedTagRepositoryImpl
    private var appsByTag: Map<Long, List<String>> = emptyMap()

    @Before
    fun setUp() {
        dao = FakeBlockedTagDao { appsByTag }
        clock = FakeClock(current = 1_000_000L)
        repo = BlockedTagRepositoryImpl(dao, clock)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `blockTag inserts row with blockedUntilEpochMs equal to now plus duration`() = runTest {
        repo.blockTag(tagId = 7L, duration = BlockDuration.HOUR_1)
        val row = dao.getByTag(7L)
        assertNotNull(row)
        assertEquals(1_000_000L + BlockDuration.HOUR_1.millis, row!!.blockedUntilEpochMs)
        assertEquals(1_000_000L, row.blockedSinceEpochMs)
        assertEquals("1h", row.durationLabel)
    }

    @Test
    fun `isTagBlocked returns true while clock is before expiry`() = runTest {
        repo.blockTag(7L, BlockDuration.HOUR_1)
        assertTrue(repo.isTagBlocked(7L))
    }

    @Test
    fun `isTagBlocked returns false after clock passes expiry`() = runTest {
        repo.blockTag(7L, BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertFalse(repo.isTagBlocked(7L))
    }

    @Test
    fun `re-blocking an already-blocked tag upserts with extended expiry`() = runTest {
        repo.blockTag(7L, BlockDuration.HOUR_1)
        val firstExpiry = dao.getByTag(7L)!!.blockedUntilEpochMs
        clock.current += 30 * 60 * 1000L
        repo.blockTag(7L, BlockDuration.DAY_1)
        val secondExpiry = dao.getByTag(7L)!!.blockedUntilEpochMs
        assertTrue(secondExpiry > firstExpiry)
        assertEquals(1, dao.size)
    }

    @Test
    fun `pruneExpired removes rows whose blockedUntilEpochMs is not greater than now`() = runTest {
        repo.blockTag(1L, BlockDuration.HOUR_1)
        repo.blockTag(2L, BlockDuration.DAY_7)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        repo.pruneExpired()
        assertNull(dao.getByTag(1L))
        assertNotNull(dao.getByTag(2L))
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `getActive returns null at exact expiry due to strict semantics`() = runTest {
        repo.blockTag(7L, BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis // exactly equal
        assertNull(repo.getActive(7L))
    }

    @Test
    fun `getActive returns null for unknown tag`() = runTest {
        assertNull(repo.getActive(999L))
    }

    // ── Package join ─────────────────────────────────────────────────────────

    @Test
    fun `observeActiveBlockedPackagesByTags reports max expiry per package`() = runTest {
        appsByTag = mapOf(
            1L to listOf("com.shared", "com.only-1"),
            2L to listOf("com.shared", "com.only-2"),
        )
        repo.blockTag(1L, BlockDuration.HOUR_1)
        repo.blockTag(2L, BlockDuration.DAY_1) // longer

        val map = repo.observeActiveBlockedPackagesByTags().first()
        assertEquals(1_000_000L + BlockDuration.HOUR_1.millis, map["com.only-1"])
        assertEquals(1_000_000L + BlockDuration.DAY_1.millis, map["com.only-2"])
        // shared app should reflect the LATER expiry (DAY_1)
        assertEquals(1_000_000L + BlockDuration.DAY_1.millis, map["com.shared"])
    }

    @Test
    fun `observeActiveTagBlocks emits empty after expiry`() = runTest {
        repo.blockTag(1L, BlockDuration.HOUR_1)
        repo.observeActiveTagBlocks().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertEquals(emptyList<Any>(), repo.observeActiveTagBlocks().first())
    }
}
