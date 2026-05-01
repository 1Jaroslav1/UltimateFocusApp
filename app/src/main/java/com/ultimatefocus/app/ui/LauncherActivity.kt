/**
 * The single Activity that hosts the entire Compose UI tree.
 * Declared as the HOME intent handler in AndroidManifest so the system
 * presents it as the device home screen.
 * On first launch, prompts the user to set this app as the default launcher
 * via RoleManager (API 29+) and asks CategorySeeder (ULT-85) to create
 * the default "Social Media" and "Games" tags + assign matching apps.
 *
 * Back-press handling is left to OnBackPressedDispatcher (NavHost registers
 * its own pop callback there, so the OS edge-swipe-back gesture pops the
 * NavHost stack like any other Android app). The "home page is not
 * exitable" rule is enforced by a BackHandler in NavGraph that's enabled
 * only on the pager's home page — not by overriding onBackPressed() here,
 * which would short-circuit the dispatcher and break navigation pop on the
 * pushed Settings / Blocking schedules / editor screens.
 *
 * Covers: ULT-5, ULT-23, ULT-85
 */
package com.ultimatefocus.app.ui

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.ultimatefocus.app.domain.service.CategorySeeder
import com.ultimatefocus.app.ui.navigation.NavGraph
import com.ultimatefocus.app.ui.settings.SettingsViewModel
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var categorySeeder: CategorySeeder

    private var hasRequestedLauncherRole = false

    private val requestHomeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* User made their choice — no further action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ULT-85: best-effort one-shot seeding on first launch. The
        // seeder is internally idempotent (guarded by a DataStore flag),
        // so safely no-ops on every subsequent cold start.
        lifecycleScope.launch { categorySeeder.seedIfNeeded() }
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
            UltimateFocusTheme(darkTheme = preferences.isDarkTheme) {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasRequestedLauncherRole) {
            hasRequestedLauncherRole = true
            requestDefaultLauncherIfNeeded()
        }
    }

    private fun requestDefaultLauncherIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null &&
                    roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    requestHomeLauncher.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                    )
                }
            } catch (_: Exception) {
                // RoleManager may not be available on all devices/emulators
            }
        }
    }
}
