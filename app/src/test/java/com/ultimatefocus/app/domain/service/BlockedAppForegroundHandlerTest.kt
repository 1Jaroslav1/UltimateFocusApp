/**
 * Unit tests for BlockedAppForegroundHandler. Verifies the block/debounce/
 * exclusion rules without booting an AccessibilityService:
 *   - blocked package → ShowBlocked on first observation
 *   - blocked package reopened while the block screen is NOT confirmed
 *     visible for it → ShowBlocked again, regardless of elapsed time
 *     (ULT-86: no time-based debounce can gate the blocked branch, since
 *     that's exactly what let rapid close/reopen bypass the block)
 *   - blocked package reopened while the block screen IS still visible
 *     for it → Ignore (avoids a redundant relaunch)
 *   - unblocked / own / system-UI packages → Ignore
 * Covers: ULT-20, ULT-21, ULT-86
 */
package com.ultimatefocus.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BlockedAppForegroundHandlerTest {

    private lateinit var handler: BlockedAppForegroundHandler
    private val ownPackage = "com.ultimatefocus.app"
    private val activeBlocked = setOf("com.blocked.app")

    @Before
    fun setUp() { handler = BlockedAppForegroundHandler() }

    @Test
    fun blockedPackage_firstObservation_returnsShowBlocked() {
        val action = handler.onForegroundPackage(
            packageName = "com.blocked.app",
            nowMs = 1_000L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
        )
        assertEquals(ForegroundAction.ShowBlocked("com.blocked.app"), action)
    }

    @Test
    fun reopeningBlockedPackage_beforeBlockScreenConfirmedVisible_stillReturnsShowBlocked() {
        // This is the ULT-86 regression test: a rapid close/reopen (well
        // within the old 1500ms debounce window) must never be silently
        // ignored when nothing confirms the block screen is actually up.
        handler.onForegroundPackage("com.blocked.app", 1_000L, activeBlocked, ownPackage)
        val action = handler.onForegroundPackage(
            packageName = "com.blocked.app",
            nowMs = 1_500L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
            blockScreenVisiblePackage = null,
        )
        assertEquals(ForegroundAction.ShowBlocked("com.blocked.app"), action)
    }

    @Test
    fun sameBlockedPackageAfterBlockScreenDismissed_returnsShowBlockedAgain() {
        handler.onForegroundPackage("com.blocked.app", 1_000L, activeBlocked, ownPackage)
        val action = handler.onForegroundPackage(
            packageName = "com.blocked.app",
            nowMs = 1_000L + BlockedAppForegroundHandler.DEFAULT_DEBOUNCE_MS + 1L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
            blockScreenVisiblePackage = null,
        )
        assertEquals(ForegroundAction.ShowBlocked("com.blocked.app"), action)
    }

    @Test
    fun blockedPackage_whileBlockScreenCurrentlyVisibleForIt_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "com.blocked.app",
            nowMs = 1_000L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
            blockScreenVisiblePackage = "com.blocked.app",
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun blockedPackage_blockScreenVisibleForDifferentPackage_stillReturnsShowBlocked() {
        val action = handler.onForegroundPackage(
            packageName = "com.blocked.app",
            nowMs = 1_000L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
            blockScreenVisiblePackage = "com.other.blocked",
        )
        assertEquals(ForegroundAction.ShowBlocked("com.blocked.app"), action)
    }

    @Test
    fun unblockedPackage_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "com.free.app",
            nowMs = 1_000L,
            activeBlocked = activeBlocked,
            ownPackage = ownPackage,
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun ownPackage_returnsIgnoreEvenIfSomehowInBlockedSet() {
        val action = handler.onForegroundPackage(
            packageName = ownPackage,
            nowMs = 1_000L,
            activeBlocked = setOf(ownPackage),
            ownPackage = ownPackage,
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun systemUiPackage_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "com.android.systemui",
            nowMs = 1_000L,
            activeBlocked = setOf("com.android.systemui"),
            ownPackage = ownPackage,
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun androidSystemPackage_returnsIgnore() {
        val action = handler.onForegroundPackage(
            packageName = "android",
            nowMs = 1_000L,
            activeBlocked = setOf("android"),
            ownPackage = ownPackage,
        )
        assertEquals(ForegroundAction.Ignore, action)
    }

    @Test
    fun nullOrBlankPackageName_returnsIgnore() {
        assertEquals(
            ForegroundAction.Ignore,
            handler.onForegroundPackage(null, 1_000L, activeBlocked, ownPackage),
        )
        assertEquals(
            ForegroundAction.Ignore,
            handler.onForegroundPackage("", 1_000L, activeBlocked, ownPackage),
        )
    }

    @Test
    fun differentBlockedPackages_eachHaveIndependentDebounce() {
        val blocked = setOf("com.a", "com.b")
        val a1 = handler.onForegroundPackage("com.a", 1_000L, blocked, ownPackage)
        val b1 = handler.onForegroundPackage("com.b", 1_001L, blocked, ownPackage)
        assertEquals(ForegroundAction.ShowBlocked("com.a"), a1)
        assertEquals(ForegroundAction.ShowBlocked("com.b"), b1)
    }
}
