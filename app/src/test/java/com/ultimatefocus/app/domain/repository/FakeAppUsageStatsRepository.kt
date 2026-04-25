/**
 * In-memory test double for AppUsageStatsRepository so JVM unit tests can
 * drive the schedule editor's picker sort without booting UsageStatsManager.
 * Tests can flip [available] to simulate a missing PACKAGE_USAGE_STATS grant.
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.domain.repository

class FakeAppUsageStatsRepository(
    private val seed: Map<String, Long> = emptyMap(),
    private val available: Boolean = true,
) : AppUsageStatsRepository {

    override fun isAvailable(): Boolean = available

    override suspend fun weeklyUsageMillis(): Map<String, Long> =
        if (available) seed else emptyMap()
}
