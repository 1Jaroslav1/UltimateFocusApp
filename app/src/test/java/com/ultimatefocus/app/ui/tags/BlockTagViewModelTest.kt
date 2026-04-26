/**
 * Unit tests for BlockTagViewModel. Verifies preview returns the apps
 * assigned to a tag (with labels resolved via AppRepository — falling back
 * to packageName when an app isn't installed), blockTag persists via the
 * repo, and activeTagBlocks mirrors the underlying repository state.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.ui.tags

import android.content.ComponentName
import com.ultimatefocus.app.data.local.dao.FakeAppTagDao
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.data.local.dao.FakeTagDao
import com.ultimatefocus.app.data.repository.BlockedTagRepositoryImpl
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockTagViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var tagRepo: TagRepositoryImpl
    private lateinit var blockTagRepo: BlockedTagRepositoryImpl
    private lateinit var appRepo: FakeAppRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeClock(current = 1_000_000L)
        val tagDao = FakeTagDao()
        val appTagDao = FakeAppTagDao()
        val blockedTagDao = FakeBlockedTagDao()
        tagRepo = TagRepositoryImpl(tagDao, appTagDao, blockedTagDao, clock)
        blockTagRepo = BlockedTagRepositoryImpl(blockedTagDao, clock)
        appRepo = FakeAppRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preview returns packages with resolved labels`() = runTest(dispatcher) {
        val tag = tagRepo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        tagRepo.assignTag("org.telegram.messenger", tag.id)
        tagRepo.assignTag("com.example.unknown", tag.id)
        appRepo.setApps(
            listOf(
                AppInfo(
                    packageName = "org.telegram.messenger",
                    label = "Telegram",
                    componentName = ComponentName("org.telegram.messenger", "Main"),
                ),
            ),
        )

        val vm = BlockTagViewModel(tagRepo, blockTagRepo, appRepo)
        val preview = vm.preview(tag.id)
        // Labels resolved via AppRepository; unknown app falls back to packageName.
        // Sorted alphabetically by label.
        assertEquals(
            listOf(
                BlockTagPreviewApp("com.example.unknown", "com.example.unknown"),
                BlockTagPreviewApp("org.telegram.messenger", "Telegram"),
            ),
            preview,
        )
    }

    @Test
    fun `preview returns empty list when no apps tagged`() = runTest(dispatcher) {
        val tag = tagRepo.createTag("Empty", TagColor.STEEL_BLUE).getOrThrow()
        val vm = BlockTagViewModel(tagRepo, blockTagRepo, appRepo)
        assertTrue(vm.preview(tag.id).isEmpty())
    }

    @Test
    fun `blockTag persists a row visible via the repository`() = runTest(dispatcher) {
        val tag = tagRepo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        val vm = BlockTagViewModel(tagRepo, blockTagRepo, appRepo)

        vm.blockTag(tag.id, BlockDuration.HOUR_1)

        assertTrue(blockTagRepo.isTagBlocked(tag.id))
        val active = vm.activeTagBlocks.first { tag.id in it }
        assertEquals(tag.id, active[tag.id]?.tagId)
        assertEquals("1h", active[tag.id]?.durationLabel)
    }
}
