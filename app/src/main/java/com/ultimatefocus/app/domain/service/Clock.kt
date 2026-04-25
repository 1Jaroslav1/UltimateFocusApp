/**
 * Wall-clock abstraction injected wherever block-expiry math happens.
 * Required so block-expiry logic is unit-testable without Thread.sleep:
 * tests bind a FakeClock that advances deterministically.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

interface Clock {
    fun now(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
