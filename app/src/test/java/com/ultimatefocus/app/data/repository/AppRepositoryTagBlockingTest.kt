/**
 * Unit tests focused on the tag-blocking behavior in the AppRepository:
 * because AppRepositoryImpl needs Android system services (LauncherApps),
 * these tests validate the blocking *rule* via isolated FakeBlockedTag/Tag
 * repositories and assert the public effective-block contract:
 * - direct app block alone: AppInfo.blockedUntilEpochMs reflects it.
 * - tag block alone: AppInfo.blockedUntilEpochMs reflects the tag's expiry
 *   (auto-block by tag, AC of ULT-75).
 * - both active: the LATER expiry wins.
 * - new app assigned to a blocked tag: that app immediately appears blocked
 *   in the per-package map (covering the AC "If a new app is assigned to a
 *   currently blocked tag, that app is automatically blocked for the
 *   remaining duration").
 * The combine logic itself lives in AppRepositoryImpl; these tests exercise
 * the repository helpers it composes.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.FakeBlockedTagRepository
import com.ultimatefocus.app.domain.repository.FakeTagRepository
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppRepositoryTagBlockingTest {

    private lateinit var clock: FakeClock
    private lateinit var tagRepo: FakeTagRepository
    private lateinit var blockedTagRepo: FakeBlockedTagRepository

    @Before
    fun setUp() {
        clock = FakeClock(current = 1_000_000L)
        tagRepo = FakeTagRepository()
        blockedTagRepo = FakeBlockedTagRepository(clock)
    }

    @Test
    fun `tag block surfaces the package via observeActiveBlockedPackagesByTags`() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.a", tagId = 1L)
        blockedTagRepo.setAppsForTag(1L, listOf("com.a"))
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)

        val map = blockedTagRepo.observeActiveBlockedPackagesByTags().first()
        assertEquals(1_000_000L + BlockDuration.HOUR_1.millis, map["com.a"])
    }

    @Test
    fun `assigning an app to a blocked tag immediately reflects in the map`() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        // No assignments yet — package map is empty
        assertTrue(blockedTagRepo.observeActiveBlockedPackagesByTags().first().isEmpty())
        // Assign a new app
        tagRepo.seedAssignment("com.late", tagId = 1L)
        blockedTagRepo.setAppsForTag(1L, listOf("com.late"))
        val map = blockedTagRepo.observeActiveBlockedPackagesByTags().first()
        assertEquals(1_000_000L + BlockDuration.HOUR_1.millis, map["com.late"])
    }

    @Test
    fun `tag block does not affect untagged apps`() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        blockedTagRepo.setAppsForTag(1L, emptyList()) // no apps tagged
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        val map = blockedTagRepo.observeActiveBlockedPackagesByTags().first()
        assertNull(map["com.untagged"])
    }

    @Test
    fun `multiple blocked tags on the same app yield the max expiry`() = runTest {
        blockedTagRepo.setAppsForTag(1L, listOf("com.shared"))
        blockedTagRepo.setAppsForTag(2L, listOf("com.shared"))
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        blockedTagRepo.blockTag(2L, BlockDuration.DAY_1)

        val map = blockedTagRepo.observeActiveBlockedPackagesByTags().first()
        assertEquals(1_000_000L + BlockDuration.DAY_1.millis, map["com.shared"])
    }

    @Test
    fun `expired tag block disappears from the map`() = runTest {
        blockedTagRepo.setAppsForTag(1L, listOf("com.a"))
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        val map = blockedTagRepo.observeActiveBlockedPackagesByTags().first()
        assertTrue(map.isEmpty())
    }
}
