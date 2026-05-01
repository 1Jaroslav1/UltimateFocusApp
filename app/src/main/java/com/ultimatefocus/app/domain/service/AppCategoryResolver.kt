/**
 * Thin abstraction over PackageManager.getApplicationInfo(pkg).category
 * (API 26+) so CategorySeeder can be unit-tested on the JVM without a
 * real Context / PackageManager. Production uses [PackageManagerCategoryResolver]
 * which delegates to the system PackageManager; unit tests provide an
 * in-memory map.
 *
 * Returns ApplicationInfo.CATEGORY_* constants. ApplicationInfo
 * .CATEGORY_UNDEFINED (-1) means "the manifest did not declare a
 * category" — CategorySeeder treats that as "not a member" and falls
 * back to its curated allowlist.
 *
 * Covers: ULT-85
 */
package com.ultimatefocus.app.domain.service

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppCategoryResolver {
    /** Returns ApplicationInfo.CATEGORY_* for [packageName], or CATEGORY_UNDEFINED. */
    fun categoryOf(packageName: String): Int
}

@Singleton
class PackageManagerCategoryResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppCategoryResolver {
    override fun categoryOf(packageName: String): Int = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).category
    }.getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)
}
