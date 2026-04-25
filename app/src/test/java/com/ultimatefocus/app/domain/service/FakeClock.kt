/**
 * Deterministic Clock for unit tests. Tests mutate `current` to advance
 * wall-clock time without relying on Thread.sleep.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.service

class FakeClock(var current: Long = 0L) : Clock {
    override fun now(): Long = current
}
