/**
 * Unit tests for TagPickerViewModel. Verifies state derivation in
 * single-app and bulk modes, the 5-tag-cap-disables-row rule, and toggle
 * behavior (single → cycle assigned/unassigned, bulk → cycle AllAssigned ↔
 * NoneAssigned).
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.tags

import com.ultimatefocus.app.data.local.dao.FakeAppTagDao
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.data.local.dao.FakeTagDao
import com.ultimatefocus.app.data.repository.BlockedTagRepositoryImpl
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.MAX_TAGS_PER_APP
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagPickerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var tagDao: FakeTagDao
    private lateinit var appTagDao: FakeAppTagDao
    private lateinit var blockedTagDao: FakeBlockedTagDao
    private lateinit var clock: FakeClock
    private lateinit var repo: TagRepositoryImpl
    private lateinit var blockedTagRepo: BlockedTagRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tagDao = FakeTagDao()
        appTagDao = FakeAppTagDao()
        blockedTagDao = FakeBlockedTagDao()
        clock = FakeClock(0L)
        repo = TagRepositoryImpl(tagDao, appTagDao, blockedTagDao, clock)
        blockedTagRepo = BlockedTagRepositoryImpl(blockedTagDao, clock)
    }

    private fun newVm() = TagPickerViewModel(repo, blockedTagRepo)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Single-app mode ─────────────────────────────────────────────────────

    @Test
    fun `single app shows existing assignments and toggles them`() = runTest(dispatcher) {
        val tagA = repo.createTag("A", TagColor.STEEL_BLUE).getOrThrow()
        val tagB = repo.createTag("B", TagColor.SAGE_GREEN).getOrThrow()
        repo.assignTag("com.app", tagA.id).getOrThrow()

        val vm = newVm()
        vm.setPackages(listOf("com.app"))

        val state = vm.uiState.first { !it.isLoading && it.items.size == 2 }
        val byTag = state.items.associateBy { it.tag.id }
        assertEquals(TagAssignmentState.AllAssigned, byTag[tagA.id]!!.state)
        assertEquals(TagAssignmentState.NoneAssigned, byTag[tagB.id]!!.state)

        // Toggle B on
        vm.toggle(byTag[tagB.id]!!)
        val afterToggleOn = vm.uiState.first { st ->
            !st.isLoading && st.items.firstOrNull { it.tag.id == tagB.id }?.state == TagAssignmentState.AllAssigned
        }
        assertEquals(
            TagAssignmentState.AllAssigned,
            afterToggleOn.items.first { it.tag.id == tagB.id }.state,
        )

        // Toggle A off
        vm.toggle(byTag[tagA.id]!!)
        val afterToggleOff = vm.uiState.first { st ->
            !st.isLoading && st.items.firstOrNull { it.tag.id == tagA.id }?.state == TagAssignmentState.NoneAssigned
        }
        assertEquals(
            TagAssignmentState.NoneAssigned,
            afterToggleOff.items.first { it.tag.id == tagA.id }.state,
        )
    }

    @Test
    fun `single app at 5-tag cap disables unassigned rows`() = runTest(dispatcher) {
        val colors = TagColor.values().take(MAX_TAGS_PER_APP + 1)
        val tagIds = colors.mapIndexed { idx, c ->
            repo.createTag("T$idx", c).getOrThrow().id
        }
        // Assign 5 tags to com.full
        tagIds.take(MAX_TAGS_PER_APP).forEach { id ->
            repo.assignTag("com.full", id).getOrThrow()
        }

        val vm = newVm()
        vm.setPackages(listOf("com.full"))

        val state = vm.uiState.first { !it.isLoading && it.items.size == colors.size }
        val sixthTag = state.items.first { it.tag.id == tagIds.last() }
        assertEquals(TagAssignmentState.NoneAssigned, sixthTag.state)
        assertTrue(sixthTag.disabled)
        // Already-assigned rows are NOT disabled (they can be unassigned freely)
        val firstTag = state.items.first { it.tag.id == tagIds.first() }
        assertFalse(firstTag.disabled)
    }

    // ── Bulk mode ───────────────────────────────────────────────────────────

    @Test
    fun `bulk mode reports tri-state per tag`() = runTest(dispatcher) {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        repo.assignTag("com.a", tag.id).getOrThrow()
        // com.b and com.c don't have it
        val vm = newVm()
        vm.setPackages(listOf("com.a", "com.b", "com.c"))

        val state = vm.uiState.first { !it.isLoading && it.items.size == 1 }
        assertEquals(TagAssignmentState.SomeAssigned, state.items[0].state)
    }

    @Test
    fun `bulk all-assigned toggles to none-assigned`() = runTest(dispatcher) {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        repo.assignTag("com.a", tag.id).getOrThrow()
        repo.assignTag("com.b", tag.id).getOrThrow()
        val vm = newVm()
        vm.setPackages(listOf("com.a", "com.b"))

        val initial = vm.uiState.first { !it.isLoading && it.items.size == 1 }
        assertEquals(TagAssignmentState.AllAssigned, initial.items[0].state)

        vm.toggle(initial.items[0])

        val after = vm.uiState.first { st ->
            !st.isLoading && st.items.firstOrNull()?.state == TagAssignmentState.NoneAssigned
        }
        assertEquals(TagAssignmentState.NoneAssigned, after.items[0].state)
    }

    @Test
    fun `bulk some-assigned toggles to all-assigned`() = runTest(dispatcher) {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        repo.assignTag("com.a", tag.id).getOrThrow()
        val vm = newVm()
        vm.setPackages(listOf("com.a", "com.b"))

        val initial = vm.uiState.first { !it.isLoading && it.items.size == 1 }
        assertEquals(TagAssignmentState.SomeAssigned, initial.items[0].state)

        vm.toggle(initial.items[0])

        val after = vm.uiState.first { st ->
            !st.isLoading && st.items.firstOrNull()?.state == TagAssignmentState.AllAssigned
        }
        assertEquals(TagAssignmentState.AllAssigned, after.items[0].state)
    }

    @Test
    fun `empty packages list emits empty items`() = runTest(dispatcher) {
        repo.createTag("Social", TagColor.STEEL_BLUE)
        val vm = newVm()
        vm.setPackages(emptyList())

        val state = vm.uiState.first { !it.isLoading }
        assertTrue(state.items.isEmpty())
    }

    // ── Blocked-tag rows are uneditable (ULT-75 follow-up) ─────────────────

    @Test
    fun `blocked tag surfaces TagBlocked disabledReason`() = runTest(dispatcher) {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        blockedTagRepo.blockTag(tag.id, BlockDuration.HOUR_1)

        val vm = newVm()
        vm.setPackages(listOf("com.app"))

        val state = vm.uiState.first { st -> !st.isLoading && st.items.size == 1 }
        val item = state.items[0]
        assertTrue(item.disabled)
        assertEquals(TagPickerDisabledReason.TagBlocked, item.disabledReason)
    }

    @Test
    fun `blocked reason wins over the 5-tag-cap reason`() = runTest(dispatcher) {
        // Fill the app with 5 tags so the cap would otherwise apply to a 6th.
        val tags = TagColor.values().take(MAX_TAGS_PER_APP + 1).mapIndexed { idx, c ->
            repo.createTag("T$idx", c).getOrThrow()
        }
        tags.take(MAX_TAGS_PER_APP).forEach { repo.assignTag("com.full", it.id).getOrThrow() }
        // Block the 6th tag.
        blockedTagRepo.blockTag(tags.last().id, BlockDuration.HOUR_1)

        val vm = newVm()
        vm.setPackages(listOf("com.full"))
        val state = vm.uiState.first { st -> !st.isLoading && st.items.size == tags.size }
        val sixth = state.items.first { it.tag.id == tags.last().id }
        // TagBlocked reason wins — even though AppAtMaxTags would also apply.
        assertEquals(TagPickerDisabledReason.TagBlocked, sixth.disabledReason)
    }
}
