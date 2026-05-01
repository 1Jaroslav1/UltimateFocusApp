/**
 * Unit tests for ManageTagsViewModel. Verifies that:
 * - uiState mirrors the underlying tag list
 * - takenColors reflects every tag's color
 * - isAtTagLimit flips when the 10-tag cap is reached
 * - errors flow surfaces TagOperationError values
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import app.cash.turbine.test
import com.ultimatefocus.app.data.local.dao.FakeAppTagDao
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.data.local.dao.FakeTagDao
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.data.repository.TagRepositoryImpl
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.service.AppCategoryResolver
import com.ultimatefocus.app.domain.service.CategorySeeder
import com.ultimatefocus.app.domain.service.FakeClock
import android.content.pm.ApplicationInfo
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
class ManageTagsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var tagDao: FakeTagDao
    private lateinit var appTagDao: FakeAppTagDao
    private lateinit var repo: TagRepositoryImpl
    private lateinit var seeder: CategorySeeder

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tagDao = FakeTagDao()
        appTagDao = FakeAppTagDao()
        repo = TagRepositoryImpl(tagDao, appTagDao, FakeBlockedTagDao(), FakeClock(0L))
        seeder = CategorySeeder(
            tagRepository = repo,
            appRepository = FakeAppRepository(),
            userPreferences = FakeUserPreferencesSource(),
            categoryResolver = object : AppCategoryResolver {
                override fun categoryOf(packageName: String): Int =
                    ApplicationInfo.CATEGORY_UNDEFINED
            },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects new tags as they are created`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)

        vm.uiState.test {
            // Initial empty state
            val empty = awaitItem()
            assertEquals(0, empty.tags.size)
            assertFalse(empty.isAtTagLimit)
            assertTrue(empty.takenColors.isEmpty())

            vm.createTag("Social", TagColor.STEEL_BLUE)

            val withOne = awaitItem()
            assertEquals(1, withOne.tags.size)
            assertEquals("Social", withOne.tags[0].name)
            assertEquals(setOf(TagColor.STEEL_BLUE), withOne.takenColors)
            assertFalse(withOne.isAtTagLimit)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isAtTagLimit becomes true after creating 10 tags`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)
        TagColor.values().forEachIndexed { idx, color ->
            vm.createTag("Tag$idx", color)
        }

        val state = vm.uiState.first { it.tags.size == MAX_TAGS }
        assertTrue(state.isAtTagLimit)
    }

    @Test
    fun `errors flow emits ColorAlreadyUsed when color collides`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)

        vm.createTag("First", TagColor.STEEL_BLUE)
        // Wait for the first tag to land
        vm.uiState.first { it.tags.size == 1 }

        vm.errors.test {
            vm.createTag("Second", TagColor.STEEL_BLUE)
            assertEquals(TagOperationError.ColorAlreadyUsed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `errors flow emits TagLimitReached when creating an 11th tag`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)
        TagColor.values().forEachIndexed { idx, color ->
            vm.createTag("Tag$idx", color)
        }
        vm.uiState.first { it.tags.size == MAX_TAGS }

        vm.errors.test {
            vm.createTag("Eleventh", TagColor.STEEL_BLUE)
            assertEquals(TagOperationError.TagLimitReached, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteTag removes the tag from uiState`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)
        vm.createTag("Doomed", TagColor.SAGE_GREEN)
        val present = vm.uiState.first { it.tags.size == 1 }
        val id = present.tags[0].id

        vm.deleteTag(id)

        vm.uiState.first { it.tags.isEmpty() }
    }

    @Test
    fun `affectedAppCountForTag returns the cross-ref size`() = runTest(dispatcher) {
        val vm = ManageTagsViewModel(repo, seeder)
        vm.createTag("Social", TagColor.STEEL_BLUE)
        val state = vm.uiState.first { it.tags.size == 1 }
        val tagId = state.tags[0].id

        repo.assignTag("com.a", tagId)
        repo.assignTag("com.b", tagId)

        assertEquals(2, vm.affectedAppCountForTag(tagId))
    }
}
