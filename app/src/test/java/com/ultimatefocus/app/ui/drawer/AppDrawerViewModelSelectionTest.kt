/**
 * Unit tests for the bulk-select mode of AppDrawerViewModel. Verifies
 * toggleSelection adds/removes packages, selection becomes inactive when
 * empty, and exitSelectionMode clears state.
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.drawer

import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelSelectionTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AppDrawerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AppDrawerViewModel(
            FakeAppRepository(),
            FakeUserPreferencesSource(),
            FakeBlockedAppRepository(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selection starts empty and inactive`() {
        val state = viewModel.selectionState.value
        assertFalse(state.active)
        assertTrue(state.selectedPackages.isEmpty())
    }

    @Test
    fun `toggleSelection adds the package and activates selection mode`() {
        viewModel.toggleSelection("com.a")
        val state = viewModel.selectionState.value
        assertTrue(state.active)
        assertEquals(setOf("com.a"), state.selectedPackages)
    }

    @Test
    fun `toggleSelection on selected package removes it`() {
        viewModel.toggleSelection("com.a")
        viewModel.toggleSelection("com.b")
        viewModel.toggleSelection("com.a")
        val state = viewModel.selectionState.value
        assertEquals(setOf("com.b"), state.selectedPackages)
        assertTrue(state.active)
    }

    @Test
    fun `removing the last selection deactivates selection mode`() {
        viewModel.toggleSelection("com.a")
        viewModel.toggleSelection("com.a") // now empty
        val state = viewModel.selectionState.value
        assertFalse(state.active)
        assertTrue(state.selectedPackages.isEmpty())
    }

    @Test
    fun `exitSelectionMode clears selection`() {
        viewModel.toggleSelection("com.a")
        viewModel.toggleSelection("com.b")
        viewModel.exitSelectionMode()
        val state = viewModel.selectionState.value
        assertFalse(state.active)
        assertTrue(state.selectedPackages.isEmpty())
    }
}
