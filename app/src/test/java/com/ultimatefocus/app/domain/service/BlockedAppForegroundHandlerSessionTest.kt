/**
 * Unit tests for the ULT-78 branches of BlockedAppForegroundHandler — the
 * existing block tests live alongside in BlockedAppForegroundHandlerTest;
 * this file exclusively exercises the new "RequireSessionDuration" path
 * and the priority rule (block always wins over picker).
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BlockedAppForegroundHandlerSessionTest {

    private lateinit var handler: BlockedAppForegroundHandler
    private val ownPackage = "com.ultimatefocus.app"

    @Before
    fun setUp() { handler = BlockedAppForegroundHandler() }

    @Test
    fun reminderEnabledPackage_returnsRequireSessionDuration() {
        val action = handler.onForegroundPackage(
            packageName = "com.alpha",
            nowMs = 1_000L,
            activeBlocked = emptySet(),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.alpha"),
            sessionActiveFor = emptySet(),
        )
        assertEquals(ForegroundAction.RequireSessionDuration("com.alpha"), action)
    }

    @Test
    fun reminderEnabledButSessionInProgress_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "com.alpha",
            nowMs = 1_000L,
            activeBlocked = emptySet(),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.alpha"),
            sessionActiveFor = setOf("com.alpha"),
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun blockedAndReminderEnabled_blockWins() {
        val action = handler.onForegroundPackage(
            packageName = "com.alpha",
            nowMs = 1_000L,
            activeBlocked = setOf("com.alpha"),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.alpha"),
            sessionActiveFor = emptySet(),
        )
        assertEquals(ForegroundAction.ShowBlocked("com.alpha"), action)
    }

    @Test
    fun reminderEnabled_systemUiPackage_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "com.android.systemui",
            nowMs = 1_000L,
            activeBlocked = emptySet(),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.android.systemui"),
            sessionActiveFor = emptySet(),
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun reminderEnabled_secondCallWithinDebounce_returnsIgnore() {
        val first = handler.onForegroundPackage(
            packageName = "com.alpha",
            nowMs = 1_000L,
            activeBlocked = emptySet(),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.alpha"),
        )
        val second = handler.onForegroundPackage(
            packageName = "com.alpha",
            nowMs = 1_500L, // within default 1500ms debounce
            activeBlocked = emptySet(),
            ownPackage = ownPackage,
            reminderEnabled = setOf("com.alpha"),
        )
        assertEquals(ForegroundAction.RequireSessionDuration("com.alpha"), first)
        assertEquals(ForegroundAction.Ignore, second)
    }
}
