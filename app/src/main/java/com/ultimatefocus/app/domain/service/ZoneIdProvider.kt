/**
 * Time-zone abstraction injected wherever schedule predicate math runs.
 * Parallels Clock: tests bind a FixedZoneIdProvider so DST/edge tests are
 * deterministic regardless of the JVM's default zone (CI is typically UTC).
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface ZoneIdProvider {
    fun current(): ZoneId
}

@Singleton
class SystemZoneIdProvider @Inject constructor() : ZoneIdProvider {
    override fun current(): ZoneId = ZoneId.systemDefault()
}
