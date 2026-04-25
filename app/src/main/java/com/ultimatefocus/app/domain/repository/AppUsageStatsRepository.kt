/**
 * Repository contract for per-package weekly foreground-usage totals.
 * The schedule editor (ULT-24) sorts the app picker by these numbers so
 * the apps the user actually distracts themselves with surface first.
 *
 * weeklyUsageMillis() returns total time-in-foreground over the trailing
 * 7 days as a Map<packageName, Long>. Packages with no recorded usage are
 * omitted (callers default to 0L). When the PACKAGE_USAGE_STATS permission
 * has not been granted, isAvailable() returns false and weeklyUsageMillis()
 * returns an empty map — callers fall back to alphabetical sort.
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.domain.repository

interface AppUsageStatsRepository {
    fun isAvailable(): Boolean
    suspend fun weeklyUsageMillis(): Map<String, Long>
}
