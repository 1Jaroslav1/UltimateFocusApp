/**
 * Unit tests for BlockingScheduleEditorViewModel. Verifies the create flow,
 * input validation, edit-mode load-by-id, and the active-window guard that
 * prevents Save / Delete from firing while the schedule is currently active
 * (ULT-25). Uses Fake repositories + FakeClock + FixedZoneIdProvider so the
 * predicate result is deterministic.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.repository.FakeAppUsageStatsRepository
import com.ultimatefocus.app.domain.repository.FakeBlockingScheduleRepository
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.domain.service.FixedZoneIdProvider
import com.ultimatefocus.app.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BlockingScheduleEditorViewModelTest {

    private lateinit var repo: FakeBlockingScheduleRepository
    private lateinit var appRepo: FakeAppRepository
    private lateinit var usageRepo: FakeAppUsageStatsRepository
    private lateinit var clock: FakeClock

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val zoneProvider = FixedZoneIdProvider(zone)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Wednesday 08:00 — before the 9-5 window.
        val nowMs = ZonedDateTime.of(2026, 4, 22, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        clock = FakeClock(current = nowMs)
        repo = FakeBlockingScheduleRepository(clock, zoneProvider)
        appRepo = FakeAppRepository()
        usageRepo = FakeAppUsageStatsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(id: Long? = null): BlockingScheduleEditorViewModel {
        val savedState = SavedStateHandle(
            mapOf(Screen.BlockingScheduleEditor.ARG_ID to (id ?: Screen.BlockingScheduleEditor.NEW_ID)),
        )
        return BlockingScheduleEditorViewModel(savedState, repo, appRepo, usageRepo, clock, zoneProvider)
    }

    private fun appInfo(pkg: String, label: String) = com.ultimatefocus.app.domain.model.AppInfo(
        packageName = pkg,
        label = label,
        componentName = android.content.ComponentName(pkg, "$pkg.MainActivity"),
    )

    // ── Picker ordering ─────────────────────────────────────────────────────

    @Test
    fun pickerItems_sortedByUsageDescending_thenAlphabetically() = runTest {
        appRepo.setApps(
            listOf(
                appInfo("com.b", "Beta"),
                appInfo("com.a", "Alpha"),
                appInfo("com.c", "Charlie"),
                appInfo("com.d", "Delta"),
            ),
        )
        usageRepo = FakeAppUsageStatsRepository(
            seed = mapOf(
                "com.a" to 100L,
                "com.b" to 100L, // same as A → tiebreak by label
                "com.c" to 50L,
                // d has no recorded usage → 0L → last
            ),
        )
        val vm = newViewModel()
        val items = vm.pickerItems.first { it.isNotEmpty() }
        assertEquals(
            listOf("com.a", "com.b", "com.c", "com.d"),
            items.map { it.packageName },
        )
    }

    @Test
    fun pickerItems_fallsBackToAlphabetical_whenUsageStatsUnavailable() = runTest {
        appRepo.setApps(
            listOf(
                appInfo("com.b", "Beta"),
                appInfo("com.a", "Alpha"),
                appInfo("com.c", "Charlie"),
            ),
        )
        usageRepo = FakeAppUsageStatsRepository(available = false)
        val vm = newViewModel()
        val items = vm.pickerItems.first { it.isNotEmpty() }
        assertEquals(listOf("com.a", "com.b", "com.c"), items.map { it.packageName })
        assertTrue(items.all { it.weeklyUsageMillis == 0L })
    }

    // ── Create flow ─────────────────────────────────────────────────────────

    @Test
    fun initialState_create_isNotEditMode_andEmpty() = runTest {
        val vm = newViewModel()
        val state = vm.uiState.first()
        assertFalse(state.isEditMode)
        assertTrue(state.name.isEmpty())
        assertTrue(state.days.isEmpty())
        assertTrue(state.packageNames.isEmpty())
    }

    @Test
    fun setters_updateUiState() = runTest {
        val vm = newViewModel()
        vm.setName("Focus")
        vm.setStart(LocalTime.of(8, 0))
        vm.setEnd(LocalTime.of(12, 0))
        vm.toggleDay(DayOfWeek.MONDAY)
        vm.togglePackage("com.a")
        val state = vm.uiState.first()
        assertEquals("Focus", state.name)
        assertEquals(LocalTime.of(8, 0), state.start)
        assertEquals(LocalTime.of(12, 0), state.end)
        assertEquals(setOf(DayOfWeek.MONDAY), state.days)
        assertEquals(setOf("com.a"), state.packageNames)
    }

    @Test
    fun toggleDay_off_removesDay() = runTest {
        val vm = newViewModel()
        vm.toggleDay(DayOfWeek.MONDAY)
        vm.toggleDay(DayOfWeek.MONDAY)
        assertTrue(vm.uiState.first().days.isEmpty())
    }

    @Test
    fun isValid_requiresAllFields() = runTest {
        val vm = newViewModel()
        assertFalse(vm.uiState.first().isValid)
        vm.setName("Focus")
        vm.toggleDay(DayOfWeek.MONDAY)
        vm.togglePackage("com.a")
        assertTrue(vm.uiState.first().isValid)
    }

    @Test
    fun onSave_create_persistsThroughRepository() = runTest {
        val vm = newViewModel()
        vm.setName("Focus")
        vm.setStart(LocalTime.of(9, 0))
        vm.setEnd(LocalTime.of(17, 0))
        vm.toggleDay(DayOfWeek.WEDNESDAY)
        vm.togglePackage("com.a")

        vm.events.test {
            vm.onSave()
            assertTrue(awaitItem() is EditorEvent.Saved)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.all.size)
        assertEquals("Focus", repo.all[0].name)
    }

    @Test
    fun onSave_invalidInput_doesNotEmit() = runTest {
        val vm = newViewModel()
        // Name + days + apps still empty. onSave should be a no-op.
        vm.events.test {
            vm.onSave()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Edit flow ──────────────────────────────────────────────────────────

    @Test
    fun initialState_edit_loadsExistingSchedule() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Loaded",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val vm = newViewModel(id = 1L)
        val state = vm.uiState.first { it.isLoaded }
        assertTrue(state.isEditMode)
        assertEquals("Loaded", state.name)
        assertEquals(setOf(DayOfWeek.WEDNESDAY), state.days)
        assertEquals(setOf("com.a"), state.packageNames)
    }

    @Test
    fun edit_inactive_save_succeeds() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Old",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val vm = newViewModel(id = 1L)
        vm.uiState.first { it.isLoaded }
        vm.setName("New")
        vm.events.test {
            vm.onSave()
            assertTrue(awaitItem() is EditorEvent.Saved)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("New", repo.all[0].name)
    }

    @Test
    fun edit_inactive_delete_succeeds() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Old",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val vm = newViewModel(id = 1L)
        vm.uiState.first { it.isLoaded }
        vm.events.test {
            vm.onDelete()
            assertTrue(awaitItem() is EditorEvent.Deleted)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repo.all.isEmpty())
    }

    @Test
    fun edit_active_save_emitsErrorAndDoesNotPersist() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Old",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        // Move clock into the active window before the VM ticker fires.
        clock.current = ZonedDateTime.of(2026, 4, 22, 12, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        val vm = newViewModel(id = 1L)
        vm.uiState.first { it.isLoaded && it.isCurrentlyActive }
        vm.setName("New")
        vm.events.test {
            vm.onSave()
            val event = awaitItem()
            assertTrue(event is EditorEvent.Error)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Old", repo.all[0].name)
    }

    @Test
    fun edit_active_delete_emitsErrorAndDoesNotPersist() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Old",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        clock.current = ZonedDateTime.of(2026, 4, 22, 12, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        val vm = newViewModel(id = 1L)
        vm.uiState.first { it.isLoaded && it.isCurrentlyActive }
        vm.events.test {
            vm.onDelete()
            val event = awaitItem()
            assertTrue(event is EditorEvent.Error)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.all.size)
    }

    @Test
    fun create_mode_delete_isNoOp() = runTest {
        val vm = newViewModel()
        vm.events.test {
            vm.onDelete()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
