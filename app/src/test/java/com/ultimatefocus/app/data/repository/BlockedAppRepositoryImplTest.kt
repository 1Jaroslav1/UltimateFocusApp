/**
 * Unit tests for BlockedAppRepositoryImpl. Uses FakeBlockedAppDao + FakeClock
 * to exercise the business logic without a real Room instance.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.repository

import app.cash.turbine.test
import com.ultimatefocus.app.data.local.dao.FakeBlockedAppDao
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

class BlockedAppRepositoryImplTest {

    private lateinit var dao: FakeBlockedAppDao
    private lateinit var clock: FakeClock
    private lateinit var repo: BlockedAppRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeBlockedAppDao()
        clock = FakeClock(current = 1_000_000L)
        repo = BlockedAppRepositoryImpl(dao, clock)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `block inserts row with blockedUntilEpochMs equal to now plus duration`() = runTest {
        repo.block("com.test.app", BlockDuration.HOUR_1)

        val row = dao.getByPackage("com.test.app")
        assertNotNull(row)
        assertEquals(1_000_000L + BlockDuration.HOUR_1.millis, row!!.blockedUntilEpochMs)
        assertEquals(1_000_000L, row.blockedSinceEpochMs)
        assertEquals("1h", row.durationLabel)
    }

    @Test
    fun `isBlocked returns true while clock is before expiry`() = runTest {
        repo.block("com.test.app", BlockDuration.HOUR_1)
        assertTrue(repo.isBlocked("com.test.app"))
    }

    @Test
    fun `isBlocked returns false after clock advances past expiry`() = runTest {
        repo.block("com.test.app", BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertFalse(repo.isBlocked("com.test.app"))
    }

    @Test
    fun `getBlock returns the domain model while active`() = runTest {
        repo.block("com.test.app", BlockDuration.DAY_1)
        val block = repo.getBlock("com.test.app")
        assertNotNull(block)
        assertEquals("com.test.app", block!!.packageName)
        assertEquals("24h", block.durationLabel)
    }

    @Test
    fun `re-blocking an already-blocked app upserts with extended expiry`() = runTest {
        repo.block("com.test.app", BlockDuration.HOUR_1)
        val firstExpiry = dao.getByPackage("com.test.app")!!.blockedUntilEpochMs

        clock.current += 30 * 60 * 1000L // +30 min
        repo.block("com.test.app", BlockDuration.DAY_1)
        val secondExpiry = dao.getByPackage("com.test.app")!!.blockedUntilEpochMs

        assertTrue(secondExpiry > firstExpiry)
        assertEquals(1, dao.size)
    }

    @Test
    fun `pruneExpired removes rows whose blockedUntilEpochMs is not greater than now`() = runTest {
        repo.block("com.a", BlockDuration.HOUR_1)
        repo.block("com.b", BlockDuration.DAY_7)

        clock.current += BlockDuration.HOUR_1.millis + 1L
        repo.pruneExpired()

        assertNull(dao.getByPackage("com.a"))
        assertNotNull(dao.getByPackage("com.b"))
    }

    @Test
    fun `observeActivePackageNames emits currently active packages only`() = runTest {
        repo.block("com.a", BlockDuration.HOUR_1)
        repo.block("com.b", BlockDuration.DAY_1)

        repo.observeActivePackageNames().test {
            val active = awaitItem()
            assertEquals(setOf("com.a", "com.b"), active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `getBlock returns null for unknown package`() = runTest {
        assertNull(repo.getBlock("com.never.blocked"))
    }

    @Test
    fun `getBlock returns null at exact expiry due to strict semantics`() = runTest {
        repo.block("com.test.app", BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis // exactly equal, not greater
        assertNull(repo.getBlock("com.test.app"))
    }

    @Test
    fun `observeActiveBlocks hides packages whose blocks have since expired`() = runTest {
        repo.block("com.a", BlockDuration.HOUR_1)
        // Baseline: one active
        assertEquals(1, repo.observeActiveBlocks().first().size)

        clock.current += BlockDuration.HOUR_1.millis + 1L
        // After advancing the clock, a fresh subscription sees the row as expired
        assertEquals(0, repo.observeActiveBlocks().first().size)
    }

    @Test
    fun `pruneExpired on empty table is a no-op`() = runTest {
        repo.pruneExpired() // must not throw
        assertEquals(0, dao.size)
    }
}
