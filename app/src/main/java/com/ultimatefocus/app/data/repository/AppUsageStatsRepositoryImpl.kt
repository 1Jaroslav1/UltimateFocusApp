/**
 * UsageStatsManager-backed implementation. queryAndAggregateUsageStats
 * over the trailing 7 days fans out per-package, and we sum
 * totalTimeInForeground across all returned UsageStats entries (the
 * system can return multiple buckets per package).
 *
 * Reads run on Dispatchers.IO — UsageStatsManager.queryAndAggregateUsageStats
 * does work synchronously and can take noticeable time on devices with many
 * installed apps.
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.ultimatefocus.app.domain.repository.AppUsageStatsRepository
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.util.UsageStatsPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : AppUsageStatsRepository {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override fun isAvailable(): Boolean =
        usageStatsManager != null && UsageStatsPermission.isGranted(context)

    override suspend fun weeklyUsageMillis(): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext emptyMap()
        val manager = usageStatsManager ?: return@withContext emptyMap()
        val end = clock.now()
        val start = end - WEEK_MILLIS
        val stats = runCatching {
            manager.queryAndAggregateUsageStats(start, end)
        }.getOrNull() ?: return@withContext emptyMap()
        // queryAndAggregateUsageStats already returns one entry per package
        // collapsing every bucket in the range; no further aggregation needed.
        stats.mapValues { (_, value) -> value.totalTimeInForeground }
            .filterValues { it > 0L }
    }

    private companion object {
        const val WEEK_MILLIS: Long = 7L * 24L * 60L * 60L * 1000L
    }
}
