/**
 * Unit tests for BlockedAppChecker. Uses FakeBlockedAppRepository + FakeClock
 * to verify the suppression rule is flipped on by an active block and back
 * off by clock advancement past expiry.
 * Covers: ULT-22
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockedAppCheckerTest {

    private lateinit var clock: FakeClock
    private lateinit var repo: FakeBlockedAppRepository
    private lateinit var checker: BlockedAppChecker

    @Before
    fun setUp() {
        clock = FakeClock(current = 1_000_000L)
        repo = FakeBlockedAppRepository(clock)
        checker = BlockedAppChecker(repo)
    }

    @Test
    fun shouldSuppress_returnsTrueForActivelyBlockedPackage() = runTest {
        repo.block("com.blocked", BlockDuration.HOUR_1)
        assertTrue(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_returnsFalseForUnblockedPackage() = runTest {
        assertFalse(checker.shouldSuppress("com.fine"))
    }

    @Test
    fun shouldSuppress_returnsFalseAfterBlockExpires() = runTest {
        repo.block("com.blocked", BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertFalse(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_isPackageScoped() = runTest {
        repo.block("com.blocked", BlockDuration.HOUR_1)
        assertFalse(checker.shouldSuppress("com.other"))
    }
}
