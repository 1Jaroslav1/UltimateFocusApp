/**
 * Unit tests for BlockedScreenState — the ULT-86 replacement for a
 * time-based debounce, tracking whether the block screen is genuinely
 * still visible for a given package.
 *
 * Covers: ULT-86
 */
package com.ultimatefocus.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockedScreenStateTest {

    @Test
    fun onShown_setsVisiblePackage() {
        val state = BlockedScreenState()
        state.onShown("com.blocked.app")
        assertEquals("com.blocked.app", state.visiblePackage)
    }

    @Test
    fun onHidden_clearsOnlyWhenPackageMatches() {
        val state = BlockedScreenState()
        state.onShown("com.alpha")
        state.onHidden("com.bravo")
        assertEquals("com.alpha", state.visiblePackage)
    }

    @Test
    fun onHidden_clearsWhenPackageMatches() {
        val state = BlockedScreenState()
        state.onShown("com.alpha")
        state.onHidden("com.alpha")
        assertNull(state.visiblePackage)
    }

    @Test
    fun onShown_overwritesPreviousPackage() {
        val state = BlockedScreenState()
        state.onShown("com.alpha")
        state.onShown("com.bravo")
        assertEquals("com.bravo", state.visiblePackage)
    }
}
