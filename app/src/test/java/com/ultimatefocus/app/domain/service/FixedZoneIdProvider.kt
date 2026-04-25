/**
 * Deterministic ZoneIdProvider for unit tests. Pinning the zone (typically
 * Europe/Warsaw for the schedule predicate's DST tests) makes results
 * reproducible regardless of the JVM default zone — CI machines are usually
 * UTC, while developer machines vary.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import java.time.ZoneId

class FixedZoneIdProvider(private val zone: ZoneId) : ZoneIdProvider {
    override fun current(): ZoneId = zone
}
