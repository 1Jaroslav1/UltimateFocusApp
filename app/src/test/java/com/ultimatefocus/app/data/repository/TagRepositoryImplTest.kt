/**
 * Unit tests for TagRepositoryImpl. Uses FakeTagDao + FakeAppTagDao + FakeClock
 * to exercise the business rules: 10-tag cap, color uniqueness on create and
 * edit, name validation, cross-ref cleanup on delete, 5-tags-per-app cap, and
 * bulk-assign partial-failure semantics.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.repository

import app.cash.turbine.test
import com.ultimatefocus.app.data.local.dao.FakeAppTagDao
import com.ultimatefocus.app.data.local.dao.FakeBlockedTagDao
import com.ultimatefocus.app.data.local.dao.FakeTagDao
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.MAX_TAGS_PER_APP
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagRepositoryImplTest {

    private lateinit var tagDao: FakeTagDao
    private lateinit var appTagDao: FakeAppTagDao
    private lateinit var blockedTagDao: FakeBlockedTagDao
    private lateinit var clock: FakeClock
    private lateinit var repo: TagRepositoryImpl

    @Before
    fun setUp() {
        tagDao = FakeTagDao()
        appTagDao = FakeAppTagDao()
        blockedTagDao = FakeBlockedTagDao()
        clock = FakeClock(current = 1_000_000L)
        repo = TagRepositoryImpl(tagDao, appTagDao, blockedTagDao, clock)
    }

    // ── createTag ───────────────────────────────────────────────────────────

    @Test
    fun `createTag inserts a tag with trimmed name and given color`() = runTest {
        val result = repo.createTag("  Social Media  ", TagColor.STEEL_BLUE)
        val tag = result.getOrNull()
        assertNotNull(tag)
        assertEquals("Social Media", tag!!.name)
        assertEquals(TagColor.STEEL_BLUE, tag.color)
        assertEquals(1, tagDao.size)
    }

    @Test
    fun `createTag fails when 10-tag cap is already reached`() = runTest {
        // Fill all 10 colors with distinct tags.
        TagColor.values().forEachIndexed { idx, color ->
            repo.createTag("Tag$idx", color)
        }
        assertEquals(MAX_TAGS, tagDao.size)

        val result = repo.createTag("Eleventh", TagColor.STEEL_BLUE)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagLimitReached, result.exceptionOrNull())
    }

    @Test
    fun `createTag fails when color is already used by another tag`() = runTest {
        repo.createTag("First", TagColor.SAGE_GREEN)
        val second = repo.createTag("Second", TagColor.SAGE_GREEN)
        assertTrue(second.isFailure)
        assertEquals(TagOperationError.ColorAlreadyUsed, second.exceptionOrNull())
        assertEquals(1, tagDao.size)
    }

    @Test
    fun `createTag fails when name is blank`() = runTest {
        val result = repo.createTag("   ", TagColor.TEAL)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TagOperationError.NameBlank)
        assertEquals(0, tagDao.size)
    }

    // ── updateTag ───────────────────────────────────────────────────────────

    @Test
    fun `updateTag changes name and color`() = runTest {
        val created = repo.createTag("Old", TagColor.STEEL_BLUE).getOrThrow()
        val result = repo.updateTag(created.id, "New", TagColor.AMBER_GOLD)
        assertTrue(result.isSuccess)
        val updated = repo.getTag(created.id)!!
        assertEquals("New", updated.name)
        assertEquals(TagColor.AMBER_GOLD, updated.color)
    }

    @Test
    fun `updateTag with the same color it already has succeeds`() = runTest {
        val tag = repo.createTag("Mine", TagColor.BERRY).getOrThrow()
        val result = repo.updateTag(tag.id, "Mine renamed", TagColor.BERRY)
        assertTrue(result.isSuccess)
        assertEquals("Mine renamed", repo.getTag(tag.id)!!.name)
    }

    @Test
    fun `updateTag fails when color collides with a different tag`() = runTest {
        val first = repo.createTag("First", TagColor.STEEL_BLUE).getOrThrow()
        repo.createTag("Second", TagColor.SAGE_GREEN).getOrThrow()
        val result = repo.updateTag(first.id, "First", TagColor.SAGE_GREEN)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.ColorAlreadyUsed, result.exceptionOrNull())
        assertEquals(TagColor.STEEL_BLUE, repo.getTag(first.id)!!.color)
    }

    @Test
    fun `updateTag fails when tag id does not exist`() = runTest {
        val result = repo.updateTag(99999L, "Whatever", TagColor.TEAL)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagNotFound, result.exceptionOrNull())
    }

    @Test
    fun `updateTag fails when name is blank`() = runTest {
        val tag = repo.createTag("Existing", TagColor.STEEL_BLUE).getOrThrow()
        val result = repo.updateTag(tag.id, "", TagColor.STEEL_BLUE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TagOperationError.NameBlank)
    }

    // ── deleteTag ───────────────────────────────────────────────────────────

    @Test
    fun `deleteTag removes the tag and cleans up cross-refs`() = runTest {
        val tag = repo.createTag("Doomed", TagColor.SAGE_GREEN).getOrThrow()
        repo.assignTag("com.a", tag.id).getOrThrow()
        repo.assignTag("com.b", tag.id).getOrThrow()

        val result = repo.deleteTag(tag.id)
        assertTrue(result.isSuccess)
        assertNull(repo.getTag(tag.id))
        assertTrue(appTagDao.snapshot().isEmpty())
    }

    @Test
    fun `deleteTag fails when tag does not exist`() = runTest {
        val result = repo.deleteTag(404L)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagNotFound, result.exceptionOrNull())
    }

    @Test
    fun `deleteTag fails while a tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        // Seed an active block via the FakeBlockedTagDao (60-min into the future).
        blockedTagDao.upsert(
            com.ultimatefocus.app.data.local.entity.BlockedTagEntity(
                tagId = tag.id,
                blockedUntilEpochMs = clock.current + 60 * 60_000L,
                blockedSinceEpochMs = clock.current,
                durationLabel = "1h",
            ),
        )
        val result = repo.deleteTag(tag.id)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagCurrentlyBlocked, result.exceptionOrNull())
        // Tag still exists.
        assertTrue(repo.getTag(tag.id) != null)
    }

    @Test
    fun `deleteTag succeeds after the tag block has expired`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        // Advance past the block.
        clock.current += 60 * 60_000L + 1L

        val result = repo.deleteTag(tag.id)
        assertTrue(result.isSuccess)
        assertNull(repo.getTag(tag.id))
    }

    // ── No write to a blocked tag (ULT-75 follow-up) ───────────────────────

    @Test
    fun `updateTag fails while the tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        val result = repo.updateTag(tag.id, "Renamed", TagColor.AMBER_GOLD)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagCurrentlyBlocked, result.exceptionOrNull())
        // Tag unchanged.
        val unchanged = repo.getTag(tag.id)!!
        assertEquals("Locked", unchanged.name)
        assertEquals(TagColor.SAGE_GREEN, unchanged.color)
    }

    @Test
    fun `assignTag fails while the tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        val result = repo.assignTag("com.app", tag.id)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagCurrentlyBlocked, result.exceptionOrNull())
        // Cross-ref not created.
        assertTrue(appTagDao.snapshot().isEmpty())
    }

    @Test
    fun `bulkAssignTag fails while the tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        val result = repo.bulkAssignTag(listOf("com.a", "com.b"), tag.id)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagCurrentlyBlocked, result.exceptionOrNull())
        assertTrue(appTagDao.snapshot().isEmpty())
    }

    @Test
    fun `unassignTag silently no-ops while the tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        // Pre-existing assignment from before the block began.
        appTagDao.insert(com.ultimatefocus.app.data.local.entity.AppTagCrossRef("com.a", tag.id))
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        repo.unassignTag("com.a", tag.id)
        // Cross-ref still present — block protects the assignment.
        assertEquals(1, appTagDao.snapshot().size)
    }

    @Test
    fun `bulkUnassignTag silently no-ops while the tag block is active`() = runTest {
        val tag = repo.createTag("Locked", TagColor.SAGE_GREEN).getOrThrow()
        appTagDao.insert(com.ultimatefocus.app.data.local.entity.AppTagCrossRef("com.a", tag.id))
        appTagDao.insert(com.ultimatefocus.app.data.local.entity.AppTagCrossRef("com.b", tag.id))
        seedActiveBlock(tag.id, durationMs = 60 * 60_000L)
        repo.bulkUnassignTag(listOf("com.a", "com.b"), tag.id)
        assertEquals(2, appTagDao.snapshot().size)
    }

    private suspend fun seedActiveBlock(tagId: Long, durationMs: Long) {
        blockedTagDao.upsert(
            com.ultimatefocus.app.data.local.entity.BlockedTagEntity(
                tagId = tagId,
                blockedUntilEpochMs = clock.current + durationMs,
                blockedSinceEpochMs = clock.current,
                durationLabel = "1h",
            ),
        )
    }

    // ── observeTags ─────────────────────────────────────────────────────────

    @Test
    fun `observeTags emits ordered list including all created tags`() = runTest {
        repo.createTag("Beta", TagColor.STEEL_BLUE)
        repo.createTag("Alpha", TagColor.SAGE_GREEN)

        repo.observeTags().test {
            val emitted = awaitItem()
            assertEquals(listOf("Alpha", "Beta"), emitted.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── assignTag / unassignTag ─────────────────────────────────────────────

    @Test
    fun `assignTag adds a cross-ref and does not duplicate`() = runTest {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        assertTrue(repo.assignTag("com.app", tag.id).isSuccess)
        // Idempotent: second call still succeeds, count remains 1
        assertTrue(repo.assignTag("com.app", tag.id).isSuccess)
        val tagsForApp = repo.observeTagsForApp("com.app").first()
        assertEquals(listOf(tag), tagsForApp)
    }

    @Test
    fun `assignTag fails when app already has 5 tags`() = runTest {
        val colors = TagColor.values().take(MAX_TAGS_PER_APP + 1)
        val tagIds = colors.mapIndexed { idx, color ->
            repo.createTag("T$idx", color).getOrThrow().id
        }
        // Assign first 5 successfully
        tagIds.take(MAX_TAGS_PER_APP).forEach { id ->
            assertTrue(repo.assignTag("com.app", id).isSuccess)
        }
        // Sixth fails
        val result = repo.assignTag("com.app", tagIds.last())
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.AppTagLimitReached, result.exceptionOrNull())
    }

    @Test
    fun `assignTag fails when tag does not exist`() = runTest {
        val result = repo.assignTag("com.app", 9999L)
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.TagNotFound, result.exceptionOrNull())
    }

    @Test
    fun `unassignTag removes the cross-ref`() = runTest {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        repo.assignTag("com.app", tag.id).getOrThrow()
        repo.unassignTag("com.app", tag.id)
        assertTrue(repo.observeTagsForApp("com.app").first().isEmpty())
    }

    // ── bulkAssignTag / bulkUnassignTag ─────────────────────────────────────

    @Test
    fun `bulkAssignTag assigns the tag to every package without duplicates`() = runTest {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        val packages = listOf("com.a", "com.b", "com.c")
        val result = repo.bulkAssignTag(packages, tag.id)
        assertTrue(result.isSuccess)
        assertEquals(setOf("com.a", "com.b", "com.c"), repo.observeAppsForTag(tag.id).first().toSet())
    }

    @Test
    fun `bulkAssignTag skips apps already at 5-tag cap and reports failure`() = runTest {
        // Create 6 tags
        val tagIds = TagColor.values().take(6).mapIndexed { idx, color ->
            repo.createTag("T$idx", color).getOrThrow().id
        }
        // com.full already has 5 tags
        tagIds.take(5).forEach { repo.assignTag("com.full", it).getOrThrow() }
        // com.empty has none
        // Bulk-assign the 6th tag to both
        val result = repo.bulkAssignTag(listOf("com.full", "com.empty"), tagIds.last())
        assertTrue(result.isFailure)
        assertEquals(TagOperationError.AppTagLimitReached, result.exceptionOrNull())
        // The empty app got the tag, the full one did not
        assertEquals(setOf("com.empty"), repo.observeAppsForTag(tagIds.last()).first().toSet())
    }

    @Test
    fun `bulkUnassignTag removes the tag from all listed packages`() = runTest {
        val tag = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        repo.assignTag("com.a", tag.id).getOrThrow()
        repo.assignTag("com.b", tag.id).getOrThrow()
        repo.assignTag("com.c", tag.id).getOrThrow()

        repo.bulkUnassignTag(listOf("com.a", "com.b"), tag.id)

        assertEquals(listOf("com.c"), repo.observeAppsForTag(tag.id).first())
    }

    // ── observeAppToTagsMap ─────────────────────────────────────────────────

    @Test
    fun `observeAppToTagsMap returns each app to its sorted tag list`() = runTest {
        val social = repo.createTag("Social", TagColor.STEEL_BLUE).getOrThrow()
        val games = repo.createTag("Games", TagColor.AMBER_GOLD).getOrThrow()
        repo.assignTag("com.app", social.id).getOrThrow()
        repo.assignTag("com.app", games.id).getOrThrow()

        val map = repo.observeAppToTagsMap().first()
        val tagsForApp = map["com.app"]!!.map { it.name }
        // Sorted alphabetically: Games, Social
        assertEquals(listOf("Games", "Social"), tagsForApp)
    }
}
