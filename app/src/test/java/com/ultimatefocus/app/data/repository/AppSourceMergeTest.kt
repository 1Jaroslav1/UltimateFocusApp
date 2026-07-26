/**
 * JVM unit tests for AppRepositoryImpl.mergeAppSources — the pure merge
 * step behind the ULT-86 fix for OEM (e.g. Samsung) apps whose launcher
 * activity is disabled by default and therefore missing from
 * LauncherApps.getActivityList.
 *
 * Covers: ULT-86
 */
package com.ultimatefocus.app.data.repository

import android.content.ComponentName
import com.ultimatefocus.app.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSourceMergeTest {

    private fun appInfo(pkg: String, label: String = pkg) = AppInfo(
        packageName = pkg,
        label = label,
        componentName = ComponentName(pkg, "$pkg.Main"),
    )

    @Test
    fun packageOnlyInEnabled_isIncludedOnce() {
        val enabled = listOf(appInfo("com.alpha"))
        val result = mergeAppSources(enabled, emptyList())
        assertEquals(listOf(appInfo("com.alpha")), result)
    }

    @Test
    fun packageOnlyInDisabledOnly_isAdded() {
        // Regression test for the Samsung bug: an OEM app whose launcher
        // activity is disabled by default must still show up.
        val disabledOnly = listOf(appInfo("com.samsung.android.app.notes", "Samsung Notes"))
        val result = mergeAppSources(emptyList(), disabledOnly)
        assertEquals(disabledOnly, result)
    }

    @Test
    fun packageInBoth_enabledEntryWins() {
        val enabled = listOf(appInfo("com.alpha", "Alpha (enabled)"))
        val disabledOnly = listOf(appInfo("com.alpha", "Alpha (disabled)"))
        val result = mergeAppSources(enabled, disabledOnly)
        assertEquals(1, result.size)
        assertEquals("Alpha (enabled)", result.single().label)
    }

    @Test
    fun duplicatesWithinDisabledOnly_areDeduped() {
        val disabledOnly = listOf(
            appInfo("com.alpha", "Alpha (first)"),
            appInfo("com.alpha", "Alpha (second)"),
        )
        val result = mergeAppSources(emptyList(), disabledOnly)
        assertEquals(1, result.size)
    }

    @Test
    fun emptyDisabledOnly_returnsEnabledUnchanged() {
        val enabled = listOf(appInfo("com.alpha"), appInfo("com.bravo"))
        assertEquals(enabled, mergeAppSources(enabled, emptyList()))
    }

    @Test
    fun emptyEnabled_returnsAllDisabledOnly() {
        val disabledOnly = listOf(appInfo("com.alpha"), appInfo("com.bravo"))
        assertEquals(disabledOnly, mergeAppSources(emptyList(), disabledOnly))
    }
}
