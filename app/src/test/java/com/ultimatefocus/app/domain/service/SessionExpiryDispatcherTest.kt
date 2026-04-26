/**
 * Unit tests for SessionExpiryDispatcher — the pure mapping from a
 * session's chosen expiry mode to the concrete SessionExpiryEffect.
 * Each enum branch maps to exactly one effect type with the right
 * payload (in particular EXIT_AND_BLOCK preserves the original session
 * duration so the auto-block lasts the same amount of time the user
 * spent in the app, per AC).
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryDispatcherTest {

    private fun session(mode: SessionExpiryMode, durationMinutes: Int = 10) = ActiveSession(
        packageName = "com.alpha",
        appLabel = "Alpha",
        startedEpochMs = 1_000L,
        durationMinutes = durationMinutes,
        expiryMode = mode,
    )

    @Test
    fun mindfulExtensionDelay_mapsToOverlayCountdown() {
        val effect = SessionExpiryDispatcher.effectFor(
            session(SessionExpiryMode.MINDFUL_EXTENSION_DELAY),
        )
        assertTrue(effect is SessionExpiryEffect.LaunchOverlayCountdown)
        assertEquals("com.alpha", effect.packageName)
    }

    @Test
    fun exitApp_mapsToGoHomeOnly() {
        val effect = SessionExpiryDispatcher.effectFor(session(SessionExpiryMode.EXIT_APP))
        assertTrue(effect is SessionExpiryEffect.GoHomeOnly)
    }

    @Test
    fun exitAndBlock_mapsToGoHomeAndBlockWithOriginalDuration() {
        val effect = SessionExpiryDispatcher.effectFor(
            session(SessionExpiryMode.EXIT_AND_BLOCK, durationMinutes = 12),
        )
        assertTrue(effect is SessionExpiryEffect.GoHomeAndBlock)
        assertEquals(12, (effect as SessionExpiryEffect.GoHomeAndBlock).blockDurationMinutes)
    }

    @Test
    fun onlyReminder_mapsToShowReminderOnlyWithMinutesSpent() {
        val effect = SessionExpiryDispatcher.effectFor(
            session(SessionExpiryMode.ONLY_REMINDER, durationMinutes = 7),
        )
        assertTrue(effect is SessionExpiryEffect.ShowReminderOnly)
        assertEquals(7, (effect as SessionExpiryEffect.ShowReminderOnly).minutesSpent)
    }

    @Test
    fun allEnumValuesAreCovered() {
        // Sanity: every enum value yields a non-null effect; protects against
        // a future enum addition silently slipping through the pure mapping.
        SessionExpiryMode.entries.forEach { mode ->
            val effect = SessionExpiryDispatcher.effectFor(session(mode))
            assertEquals("com.alpha", effect.packageName)
        }
    }
}
