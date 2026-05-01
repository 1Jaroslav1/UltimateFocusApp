/**
 * One-shot onboarding service that creates two default tags ("Social
 * Media" and "Games") and assigns matching installed apps to them on
 * first launch. Triggered from LauncherActivity.onCreate via
 * lifecycleScope.launch so it does not block startup.
 *
 * Detection strategy is layered:
 *  1. ApplicationInfo.category (API 26+) — apps that opt into
 *     CATEGORY_SOCIAL or CATEGORY_GAME via their manifest get tagged.
 *  2. A small curated allowlist of well-known package names — covers the
 *     popular apps whose manifests do not declare a category (or whose
 *     declarations are inconsistent across regions). Kept short and
 *     intentional so the seeder does not surprise privacy-conscious
 *     users by auto-tagging ambiguous apps (WhatsApp, Telegram, etc.
 *     are deliberately left off).
 *
 * Idempotency: guarded by [UserPreferencesSource.categoriesSeeded]. Once
 * the flag flips true the seeder no-ops on every subsequent launch — so
 * a user who deletes the seeded tags will not have them re-created.
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

@Singleton
class CategorySeeder @Inject constructor(
    private val tagRepository: TagRepository,
    private val appRepository: AppRepository,
    private val userPreferences: UserPreferencesSource,
    private val categoryResolver: AppCategoryResolver,
) {

    suspend fun seedIfNeeded() {
        if (userPreferences.categoriesSeeded.first()) return

        val existingTags = tagRepository.observeTags().first()
        if (existingTags.size + 2 > MAX_TAGS) {
            // Not enough room for both new tags. Bail and mark seeded so
            // we do not retry every cold start.
            userPreferences.markCategoriesSeeded()
            return
        }

        val installed = appRepository.getInstalledApps().first()
        val packageNames = installed.map { it.packageName }

        val socialPkgs = matchingPackages(
            packageNames = packageNames,
            category = ApplicationInfo.CATEGORY_SOCIAL,
            allowlist = SOCIAL_ALLOWLIST,
        )
        val gamePkgs = matchingPackages(
            packageNames = packageNames,
            category = ApplicationInfo.CATEGORY_GAME,
            allowlist = GAME_ALLOWLIST,
        )

        ensureTagAndAssign(SOCIAL_TAG_NAME, TagColor.STEEL_BLUE, socialPkgs, existingTags)
        // Re-read tags before the second create so we account for the row
        // that may have just been added (or pre-existed).
        val refreshedTags = tagRepository.observeTags().first()
        ensureTagAndAssign(GAMES_TAG_NAME, TagColor.AMBER_GOLD, gamePkgs, refreshedTags)

        userPreferences.markCategoriesSeeded()
    }

    private suspend fun ensureTagAndAssign(
        name: String,
        color: TagColor,
        packages: List<String>,
        existingTags: List<Tag>,
    ) {
        // Honor the cap defensively — refreshedTags may have grown.
        val currentTags = tagRepository.observeTags().first()
        if (currentTags.size >= MAX_TAGS &&
            currentTags.none { it.name.equals(name, ignoreCase = true) }
        ) {
            return
        }

        val existing = existingTags.firstOrNull { it.name.equals(name, ignoreCase = true) }
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
            // bulk assign in onboarding.
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

    companion object {
        const val SOCIAL_TAG_NAME: String = "Social Media"
        const val GAMES_TAG_NAME: String = "Games"

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
    }
}
