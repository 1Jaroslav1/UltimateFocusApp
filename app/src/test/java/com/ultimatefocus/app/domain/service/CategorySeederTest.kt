/**
 * JVM unit tests for the auto-categories seeder. Wires real CategorySeeder
 * against an in-memory test rig: FakeAppRepository for the installed-apps
 * Flow, a stub TagRepository that lets the test simulate cap / color-clash
 * error paths, FakeUserPreferencesSource for the categories_seeded flag,
 * and a stub AppCategoryResolver so the seeder can be tested without
 * Robolectric / a real PackageManager.
 *
 * Covers: ULT-85
 */
package com.ultimatefocus.app.domain.service

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.repository.TagOperationError
import com.ultimatefocus.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorySeederTest {

    private lateinit var apps: FakeAppRepository
    private lateinit var prefs: FakeUserPreferencesSource
    private lateinit var tags: ScriptedTagRepository
    private lateinit var resolver: StubCategoryResolver

    @Before
    fun setUp() {
        apps = FakeAppRepository()
        prefs = FakeUserPreferencesSource()
        tags = ScriptedTagRepository()
        resolver = StubCategoryResolver()
    }

    private fun newSeeder(): CategorySeeder = CategorySeeder(
        tagRepository = tags,
        appRepository = apps,
        userPreferences = prefs,
        categoryResolver = resolver,
    )

    private fun appInfo(pkg: String) = AppInfo(
        packageName = pkg,
        label = pkg,
        componentName = ComponentName(pkg, "$pkg.Main"),
    )

    // ── Happy path: zero tags, allowlist hits ───────────────────────────────

    @Test
    fun seedIfNeeded_withFlagFalseAndZeroTags_createsBothTagsAndFlipsFlag() = runTest {
        apps.setApps(
            listOf(
                appInfo("com.facebook.katana"),
                appInfo("com.king.candycrushsaga"),
                appInfo("com.unrelated.app"),
            ),
        )

        newSeeder().seedIfNeeded()

        val createdNames = tags.created.map { it.name }
        assertTrue(createdNames.contains(CategorySeeder.SOCIAL_TAG_NAME))
        assertTrue(createdNames.contains(CategorySeeder.GAMES_TAG_NAME))
        assertTrue(prefs.categoriesSeededValue())

        val socialTag = tags.allTagsValue.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }
        assertTrue(tags.assignmentsFor(socialTag.id).contains("com.facebook.katana"))
        val gamesTag = tags.allTagsValue.first { it.name == CategorySeeder.GAMES_TAG_NAME }
        assertTrue(tags.assignmentsFor(gamesTag.id).contains("com.king.candycrushsaga"))
        // Unrelated app is not in either allowlist nor declares CATEGORY_*,
        // so it must remain untagged.
        assertTrue("com.unrelated.app" !in tags.assignmentsFor(socialTag.id))
        assertTrue("com.unrelated.app" !in tags.assignmentsFor(gamesTag.id))
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    @Test
    fun seedIfNeeded_whenFlagAlreadyTrue_isNoOp() = runTest {
        prefs.setCategoriesSeeded(true)
        apps.setApps(listOf(appInfo("com.facebook.katana")))

        newSeeder().seedIfNeeded()

        assertEquals(0, tags.created.size)
    }

    // ── Cap guard: not enough tag slots ─────────────────────────────────────

    @Test
    fun seedIfNeeded_whenTagSlotsBelowTwo_skipsCreationAndFlipsFlag() = runTest {
        // Pre-populate so existing.size + 2 > MAX_TAGS.
        repeat(MAX_TAGS - 1) { tags.seedExistingTag(it.toLong() + 100, "Tag $it", TagColor.TEAL) }
        apps.setApps(listOf(appInfo("com.facebook.katana")))

        newSeeder().seedIfNeeded()

        assertEquals(0, tags.created.size)
        assertTrue(prefs.categoriesSeededValue())
    }

    // ── Color clash: only the second create errors ──────────────────────────

    @Test
    fun seedIfNeeded_whenColorAlreadyUsed_skipsThatTagButFlipsFlag() = runTest {
        tags.failCreateForColor = TagColor.STEEL_BLUE // social color clashes
        apps.setApps(
            listOf(appInfo("com.facebook.katana"), appInfo("com.king.candycrushsaga")),
        )

        newSeeder().seedIfNeeded()

        val createdNames = tags.created.map { it.name }
        assertTrue(CategorySeeder.SOCIAL_TAG_NAME !in createdNames)
        assertTrue(CategorySeeder.GAMES_TAG_NAME in createdNames)
        assertTrue(prefs.categoriesSeededValue())
    }

    // ── assignTag failures must not crash the run ───────────────────────────

    @Test
    fun seedIfNeeded_whenAssignFails_continuesOtherAppsAndFlipsFlag() = runTest {
        tags.failAssignFor = setOf("com.facebook.katana") // simulate cap
        apps.setApps(
            listOf(
                appInfo("com.facebook.katana"),
                appInfo("com.instagram.android"),
                appInfo("com.king.candycrushsaga"),
            ),
        )

        newSeeder().seedIfNeeded()

        val socialTagId =
            tags.allTagsValue.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }.id
        assertTrue("com.instagram.android" in tags.assignmentsFor(socialTagId))
        assertTrue(prefs.categoriesSeededValue())
    }

    // ── Category resolver stub ──────────────────────────────────────────────

    private class StubCategoryResolver : AppCategoryResolver {
        private val categories = mutableMapOf<String, Int>()
        fun setCategory(packageName: String, category: Int) { categories[packageName] = category }
        override fun categoryOf(packageName: String): Int =
            categories[packageName] ?: ApplicationInfo.CATEGORY_UNDEFINED
    }

    // ── ApplicationInfo.category-driven detection ───────────────────────────

    @Test
    fun seedIfNeeded_picksUpAppsViaCategoryResolver() = runTest {
        // App not in any allowlist; only the resolver knows it is social.
        apps.setApps(listOf(appInfo("com.brand.networking_app")))
        resolver.setCategory("com.brand.networking_app", ApplicationInfo.CATEGORY_SOCIAL)

        newSeeder().seedIfNeeded()

        val socialTagId =
            tags.allTagsValue.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }.id
        assertTrue("com.brand.networking_app" in tags.assignmentsFor(socialTagId))
    }

    // ── Interactive sync flow (gatherCandidates + applySelection) ───────────

    @Test
    fun gatherCandidates_returnsBothCategoriesWithDetectedApps() = runTest {
        apps.setApps(
            listOf(
                appInfo("com.facebook.katana"),
                appInfo("com.king.candycrushsaga"),
                appInfo("com.unrelated.app"),
            ),
        )

        val candidates = newSeeder().gatherCandidates()

        assertEquals(2, candidates.size)
        val social = candidates.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }
        val games = candidates.first { it.name == CategorySeeder.GAMES_TAG_NAME }
        assertTrue(social.packages.any { it.packageName == "com.facebook.katana" })
        assertTrue(games.packages.any { it.packageName == "com.king.candycrushsaga" })
        assertTrue(social.packages.none { it.packageName == "com.unrelated.app" })
    }

    @Test
    fun gatherCandidates_marksAlreadyExistingTags() = runTest {
        tags.seedExistingTag(99, CategorySeeder.SOCIAL_TAG_NAME, TagColor.STEEL_BLUE)
        apps.setApps(listOf(appInfo("com.facebook.katana")))

        val candidates = newSeeder().gatherCandidates()

        val social = candidates.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }
        assertTrue(social.alreadyExists)
        val games = candidates.first { it.name == CategorySeeder.GAMES_TAG_NAME }
        assertTrue("Games should not be marked alreadyExists", !games.alreadyExists)
    }

    @Test
    fun applySelection_skipsUnselectedCategories() = runTest {
        apps.setApps(
            listOf(appInfo("com.facebook.katana"), appInfo("com.king.candycrushsaga")),
        )
        val seeder = newSeeder()
        val candidates = seeder.gatherCandidates()
        val selection = SyncSelection(
            selectedCategories = setOf(CategorySeeder.SOCIAL_TAG_NAME),
            selectedAppsByCategory = mapOf(
                CategorySeeder.SOCIAL_TAG_NAME to setOf("com.facebook.katana"),
            ),
        )

        seeder.applySelection(candidates, selection)

        assertTrue(tags.allTagsValue.any { it.name == CategorySeeder.SOCIAL_TAG_NAME })
        assertTrue(tags.allTagsValue.none { it.name == CategorySeeder.GAMES_TAG_NAME })
        assertTrue(prefs.categoriesSeededValue())
    }

    @Test
    fun applySelection_skipsUnselectedAppsInsideSelectedCategory() = runTest {
        apps.setApps(
            listOf(
                appInfo("com.facebook.katana"),
                appInfo("com.instagram.android"),
            ),
        )
        val seeder = newSeeder()
        val candidates = seeder.gatherCandidates()
        val selection = SyncSelection(
            selectedCategories = setOf(CategorySeeder.SOCIAL_TAG_NAME),
            selectedAppsByCategory = mapOf(
                CategorySeeder.SOCIAL_TAG_NAME to setOf("com.facebook.katana"),
            ),
        )

        seeder.applySelection(candidates, selection)

        val socialId = tags.allTagsValue.first { it.name == CategorySeeder.SOCIAL_TAG_NAME }.id
        assertTrue("com.facebook.katana" in tags.assignmentsFor(socialId))
        assertTrue("com.instagram.android" !in tags.assignmentsFor(socialId))
    }

    @Test
    fun applySelection_doesNotDuplicateExistingTag() = runTest {
        tags.seedExistingTag(99, CategorySeeder.SOCIAL_TAG_NAME, TagColor.STEEL_BLUE)
        apps.setApps(listOf(appInfo("com.facebook.katana")))
        val seeder = newSeeder()
        val candidates = seeder.gatherCandidates()
        val selection = SyncSelection(
            selectedCategories = setOf(CategorySeeder.SOCIAL_TAG_NAME),
            selectedAppsByCategory = mapOf(
                CategorySeeder.SOCIAL_TAG_NAME to setOf("com.facebook.katana"),
            ),
        )

        seeder.applySelection(candidates, selection)

        // No new tag created; the pre-existing one is augmented.
        assertEquals(1, tags.allTagsValue.count { it.name == CategorySeeder.SOCIAL_TAG_NAME })
        assertTrue("com.facebook.katana" in tags.assignmentsFor(99))
    }

    // ── Test double — minimal scriptable TagRepository ──────────────────────

    private class ScriptedTagRepository : TagRepository {
        private val tagsFlow = MutableStateFlow<List<Tag>>(emptyList())
        private val assignmentsFlow = MutableStateFlow<Map<String, Set<Long>>>(emptyMap())

        val created: MutableList<Tag> = mutableListOf()
        val allTagsValue: List<Tag> get() = tagsFlow.value
        var failCreateForColor: TagColor? = null
        var failAssignFor: Set<String> = emptySet()

        fun seedExistingTag(id: Long, name: String, color: TagColor) {
            tagsFlow.value = tagsFlow.value + Tag(id, name, color)
        }

        fun assignmentsFor(tagId: Long): Set<String> =
            assignmentsFlow.value.filterValues { tagId in it }.keys

        override fun observeTags(): Flow<List<Tag>> = tagsFlow

        override fun observeTagsForApp(packageName: String): Flow<List<Tag>> =
            assignmentsFlow.map { map ->
                val ids = map[packageName].orEmpty()
                tagsFlow.value.filter { it.id in ids }
            }

        override fun observeAppToTagsMap(): Flow<Map<String, List<Tag>>> =
            assignmentsFlow.map { map ->
                val byId = tagsFlow.value.associateBy { it.id }
                map.mapValues { (_, ids) -> ids.mapNotNull { byId[it] } }
            }

        override fun observeAppsForTag(tagId: Long): Flow<List<String>> =
            assignmentsFlow.map { map -> map.filterValues { tagId in it }.keys.sorted() }

        override suspend fun getTag(id: Long): Tag? = tagsFlow.value.firstOrNull { it.id == id }

        override suspend fun createTag(name: String, color: TagColor): Result<Tag> {
            if (color == failCreateForColor) {
                return Result.failure(TagOperationError.ColorAlreadyUsed)
            }
            val id = (tagsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val tag = Tag(id = id, name = name, color = color)
            tagsFlow.value = tagsFlow.value + tag
            created += tag
            return Result.success(tag)
        }

        override suspend fun updateTag(id: Long, name: String, color: TagColor): Result<Unit> {
            tagsFlow.value = tagsFlow.value.map {
                if (it.id == id) it.copy(name = name, color = color) else it
            }
            return Result.success(Unit)
        }

        override suspend fun deleteTag(id: Long): Result<Unit> {
            tagsFlow.value = tagsFlow.value.filterNot { it.id == id }
            assignmentsFlow.value = assignmentsFlow.value.mapValues { (_, ids) -> ids - id }
            return Result.success(Unit)
        }

        override suspend fun assignTag(packageName: String, tagId: Long): Result<Unit> {
            if (packageName in failAssignFor) {
                return Result.failure(TagOperationError.AppTagLimitReached)
            }
            val current = assignmentsFlow.value
            val pkgTags = current[packageName].orEmpty().toMutableSet()
            pkgTags += tagId
            assignmentsFlow.value = current + (packageName to pkgTags)
            return Result.success(Unit)
        }

        override suspend fun unassignTag(packageName: String, tagId: Long) {
            val current = assignmentsFlow.value
            val pkgTags = current[packageName].orEmpty().toMutableSet()
            pkgTags -= tagId
            assignmentsFlow.value = current + (packageName to pkgTags)
        }

        override suspend fun bulkAssignTag(
            packageNames: List<String>,
            tagId: Long,
        ): Result<Unit> {
            packageNames.forEach { assignTag(it, tagId) }
            return Result.success(Unit)
        }

        override suspend fun bulkUnassignTag(packageNames: List<String>, tagId: Long) {
            packageNames.forEach { unassignTag(it, tagId) }
        }
    }
}
