/**
 * Unit tests for BlockTagViewModel. Verifies preview returns the apps
 * assigned to a tag, blockTag persists via the repo, and activeTagBlocks
 * mirrors the underlying repository state.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.ui.tags

import com.ultimatefocus.app.data.local.dao.FakeAppTagDao
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.data.local.dao.FakeTagDao
import com.ultimatefocus.app.data.repository.BlockedTagRepositoryImpl
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
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

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeClock(current = 1_000_000L)
        val tagDao = FakeTagDao()
        val appTagDao = FakeAppTagDao()
        tagRepo = TagRepositoryImpl(tagDao, appTagDao, clock)
        blockTagRepo = BlockedTagRepositoryImpl(FakeBlockedTagDao(), clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preview returns packages assigned to the tag`() = runTest(dispatcher) {
        val tag = tagRepo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        tagRepo.assignTag("com.a", tag.id)
        tagRepo.assignTag("com.b", tag.id)

        val vm = BlockTagViewModel(tagRepo, blockTagRepo)
        val preview = vm.preview(tag.id)
        assertEquals(setOf("com.a", "com.b"), preview.toSet())
    }

    @Test
    fun `blockTag persists a row visible via the repository`() = runTest(dispatcher) {
        val tag = tagRepo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        val vm = BlockTagViewModel(tagRepo, blockTagRepo)

        vm.blockTag(tag.id, BlockDuration.HOUR_1)

        assertTrue(blockTagRepo.isTagBlocked(tag.id))
        val active = vm.activeTagBlocks.first { tag.id in it }
        assertEquals(tag.id, active[tag.id]?.tagId)
        assertEquals("1h", active[tag.id]?.durationLabel)
    }
}
