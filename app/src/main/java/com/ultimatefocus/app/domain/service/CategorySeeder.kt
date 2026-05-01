/**
 * Service that creates default tags from Android's standard application
 * categories ("Social Media", "Games", "Music & Audio", "Video &
 * Streaming", "News") and assigns matching installed apps to them. Has
 * two entry points:
 *
 *   1. seedIfNeeded() — fire-and-forget, gated by
 *      UserPreferencesSource.categoriesSeeded. Triggered from
 *      LauncherActivity.onCreate on first launch (ULT-85).
 *   2. gatherCandidates() + applySelection() — interactive flow used by
 *      Manage Tags' "Sync with system categories" button (ULT-85
 *      follow-up): the user reviews which categories and which apps will
 *      be created/assigned, deselects anything they do not want, and
 *      confirms.
 *
 * Detection strategy is layered:
 *  1. ApplicationInfo.category (API 26+) — apps that opt into one of the
 *     supported CATEGORY_* values via their manifest get tagged.
 *  2. A small curated allowlist of well-known package names per category
 *     — covers the popular apps whose manifests do not declare a
 *     category (or whose declarations are inconsistent across regions).
 *     Kept short and intentional so the seeder does not surprise
 *     privacy-conscious users by auto-tagging ambiguous apps (WhatsApp,
 *     Telegram, etc. are deliberately left off Social Media for that
 *     reason).
 *
 * Idempotency: seedIfNeeded is guarded by [UserPreferencesSource
 * .categoriesSeeded]; once flipped true the seeder no-ops on every
 * subsequent launch — so a user who deletes the seeded tags will not
 * have them re-created. The interactive applySelection() path always
 * runs (the user explicitly opted in by tapping the sync button) and
 * also flips the flag so the auto-seeder will not later double-create.
 *
 * Cap behavior: with 5 default categories and a 10-tag cap, a user who
 * already has many tags may not have room for all of them. The
 * per-category ensureTagAndAssign() defensively re-checks the cap so
 * we create as many as fit and silently skip the rest — better partial
 * success than all-or-nothing.
 *
 * Failure tolerance: every TagRepository call returns a Result. Failures
 * (cap reached, color clash, app cap reached) are silently ignored and
 * the seeder still flips the flag, so an over-cap user does not get
 * pummeled by retries on every cold start.
 *
 * Covers: ULT-85
 */
package com.ultimatefocus.app.domain.service

import android.content.pm.ApplicationInfo
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.MAX_TAGS
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.TagRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One detected app eligible to be assigned to a category tag during a
 * sync operation. [label] is the human-readable app label (used by the
 * sync dialog), [packageName] is the install identifier.
 */
data class SyncAppCandidate(
    val packageName: String,
    val label: String,
)

/**
 * One detected category eligible to be created (or augmented) during a
 * sync operation.
 *  - [name] / [color] are the tag identity.
 *  - [packages] are the installed apps that fall into this category via
 *    either ApplicationInfo.category or the curated allowlist.
 *  - [alreadyExists] is true when a tag with the same name already
 *    exists in the user's tag set — the dialog uses this to label the
 *    category as "(already exists)" and to skip create() in
 *    applySelection.
 */
data class SyncCategoryCandidate(
    val name: String,
    val color: TagColor,
    val packages: List<SyncAppCandidate>,
    val alreadyExists: Boolean,
)

/**
 * User selection collected by the sync dialog.
 *  - [selectedCategories] are the category names the user wants to apply.
 *  - [selectedAppsByCategory] maps each selected category name to the
 *    set of package names the user wants assigned to it.
 */
data class SyncSelection(
    val selectedCategories: Set<String>,
    val selectedAppsByCategory: Map<String, Set<String>>,
)

@Singleton
class CategorySeeder @Inject constructor(
    private val tagRepository: TagRepository,
    private val appRepository: AppRepository,
    private val userPreferences: UserPreferencesSource,
    private val categoryResolver: AppCategoryResolver,
) {

    suspend fun seedIfNeeded() {
        if (userPreferences.categoriesSeeded.first()) return

        val candidates = gatherCandidates()
        // Default-apply everything (mirrors the original auto-seed
        // behavior). applySelection also flips the seeded flag, so no
        // explicit mark needed here.
        val selection = SyncSelection(
            selectedCategories = candidates.map { it.name }.toSet(),
            selectedAppsByCategory = candidates.associate { c ->
                c.name to c.packages.map { it.packageName }.toSet()
            },
        )
        applySelection(candidates = candidates, selection = selection)
    }

    /**
     * Computes the categories + matching apps that the sync dialog should
     * surface. Pure read operation: never mutates the tag store.
     */
    suspend fun gatherCandidates(): List<SyncCategoryCandidate> {
        val installed = appRepository.getInstalledApps().first()
        val packageToLabel = installed.associate { it.packageName to it.label }
        val installedSet = packageToLabel.keys
        val existingNames = tagRepository.observeTags().first()
            .map { it.name.lowercase() }
            .toSet()

        return DEFAULT_CATEGORIES.map { spec ->
            val pkgs = matchingPackages(
                packageNames = installedSet.toList(),
                category = spec.systemCategory,
                allowlist = spec.allowlist,
            ).map { pkg ->
                SyncAppCandidate(packageName = pkg, label = packageToLabel[pkg] ?: pkg)
            }.sortedBy { it.label.lowercase() }
            SyncCategoryCandidate(
                name = spec.name,
                color = spec.color,
                packages = pkgs,
                alreadyExists = spec.name.lowercase() in existingNames,
            )
        }
    }

    /**
     * Applies a [selection] gathered against [candidates]. Creates each
     * selected category that does not yet exist, then assigns each
     * selected app to the corresponding tag. Errors from the repository
     * (cap reached, color clash, app cap reached) are silently swallowed
     * so a partial failure does not abort the whole sync.
     *
     * Also flips UserPreferencesSource.categoriesSeeded so the
     * fire-and-forget seedIfNeeded() will not later re-create a tag the
     * user just declined.
     */
    suspend fun applySelection(
        candidates: List<SyncCategoryCandidate>,
        selection: SyncSelection,
    ) {
        for (candidate in candidates) {
            if (candidate.name !in selection.selectedCategories) continue
            val pkgs = selection.selectedAppsByCategory[candidate.name].orEmpty()
            ensureTagAndAssign(
                name = candidate.name,
                color = candidate.color,
                packages = pkgs.toList(),
            )
        }
        userPreferences.markCategoriesSeeded()
    }

    private suspend fun ensureTagAndAssign(
        name: String,
        color: TagColor,
        packages: List<String>,
    ) {
        // Honor the cap defensively — refreshedTags may have grown.
        val currentTags = tagRepository.observeTags().first()
        if (currentTags.size >= MAX_TAGS &&
            currentTags.none { it.name.equals(name, ignoreCase = true) }
        ) {
            return
        }

        val existing = currentTags.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: tagRepository.createTag(name, color).getOrElse {
                // ColorAlreadyUsed / TagLimitReached: try to recover by
                // matching an existing tag with the same name; otherwise
                // bail silently.
                tagRepository.observeTags().first()
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
            }
            ?: return

        for (pkg in packages) {
            // Ignore failures (AppTagLimitReached etc.) — best-effort
            // bulk assign.
            tagRepository.assignTag(pkg, existing.id)
        }
    }

    private fun matchingPackages(
        packageNames: List<String>,
        category: Int,
        allowlist: Set<String>,
    ): List<String> {
        val byCategory = packageNames.filter { categoryResolver.categoryOf(it) == category }
        return (byCategory + allowlist.intersect(packageNames.toSet())).distinct()
    }

    /** Single source of truth for the default categories. */
    internal data class CategorySpec(
        val name: String,
        val color: TagColor,
        val systemCategory: Int,
        val allowlist: Set<String>,
    )

    companion object {
        const val SOCIAL_TAG_NAME: String = "Social Media"
        const val GAMES_TAG_NAME: String = "Games"
        const val MUSIC_TAG_NAME: String = "Music & Audio"
        const val VIDEO_TAG_NAME: String = "Video & Streaming"
        const val NEWS_TAG_NAME: String = "News"

        // Deliberately conservative: limited to apps whose primary purpose
        // is broadcast/feed-style social media. Pure messaging apps
        // (WhatsApp, Signal, Telegram, Messenger) are intentionally
        // omitted — they double as essential communication and would
        // surprise users if auto-tagged "social media".
        internal val SOCIAL_ALLOWLIST: Set<String> = setOf(
            "com.facebook.katana",
            "com.instagram.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",
            "com.linkedin.android",
            "com.tumblr",
        )

        // Curated common gaming launchers / popular titles. ApplicationInfo
        // .CATEGORY_GAME catches most legitimate games; the allowlist
        // backstops popular titles whose manifests miss the declaration.
        internal val GAME_ALLOWLIST: Set<String> = setOf(
            "com.king.candycrushsaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.activision.callofduty.shooter",
            "com.dts.freefireth",
            "com.tencent.ig", // PUBG Mobile
            "com.miHoYo.GenshinImpact",
        )

        // Music streaming + audio apps. Podcast players (Pocket Casts,
        // Audible) are intentionally left off — they sit closer to
        // "spoken word / learning" than the music vibe of this category.
        internal val MUSIC_ALLOWLIST: Set<String> = setOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.apple.android.music",
            "com.amazon.mp3",
            "com.soundcloud.android",
            "com.pandora.android",
            "com.deezer.android.app",
            "tunein.player",
        )

        // Video streaming / live-video clients.
        internal val VIDEO_ALLOWLIST: Set<String> = setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient", // Prime Video
            "com.hulu.plus",
            "com.hbo.hbonow",
            "tv.twitch.android.app",
            "com.peacocktv.peacockandroid",
            "com.cbs.app", // Paramount+
        )

        // News & magazine readers — broadcast publishers and aggregators.
        internal val NEWS_ALLOWLIST: Set<String> = setOf(
            "flipboard.app",
            "com.google.android.apps.magazines", // Google News
            "bbc.mobile.news.ww",
            "com.cnn.mobile.android.phone",
            "com.nytimes.android",
            "com.washingtonpost.rainbow",
            "com.yahoo.mobile.client.android.yahoo",
            "com.smartnews.android",
        )

        internal val DEFAULT_CATEGORIES: List<CategorySpec> = listOf(
            CategorySpec(
                name = SOCIAL_TAG_NAME,
                color = TagColor.STEEL_BLUE,
                systemCategory = ApplicationInfo.CATEGORY_SOCIAL,
                allowlist = SOCIAL_ALLOWLIST,
            ),
            CategorySpec(
                name = GAMES_TAG_NAME,
                color = TagColor.AMBER_GOLD,
                systemCategory = ApplicationInfo.CATEGORY_GAME,
                allowlist = GAME_ALLOWLIST,
            ),
            CategorySpec(
                name = VIDEO_TAG_NAME,
                color = TagColor.TERRACOTTA,
                systemCategory = ApplicationInfo.CATEGORY_VIDEO,
                allowlist = VIDEO_ALLOWLIST,
            ),
            CategorySpec(
                name = MUSIC_TAG_NAME,
                color = TagColor.SAGE_GREEN,
                systemCategory = ApplicationInfo.CATEGORY_AUDIO,
                allowlist = MUSIC_ALLOWLIST,
            ),
            CategorySpec(
                name = NEWS_TAG_NAME,
                color = TagColor.DUSTY_ROSE,
                systemCategory = ApplicationInfo.CATEGORY_NEWS,
                allowlist = NEWS_ALLOWLIST,
            ),
        )
    }
}
