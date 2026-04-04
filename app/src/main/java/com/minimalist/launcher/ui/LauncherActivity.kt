/**
 * The single Activity that hosts the entire Compose UI tree.
 * Declared as the HOME intent handler in AndroidManifest so the system
 * presents it as the device home screen.
 * Covers: ULT-5
 */
package com.minimalist.launcher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import com.minimalist.launcher.ui.navigation.NavGraph
import com.minimalist.launcher.ui.settings.SettingsViewModel
import com.minimalist.launcher.ui.theme.MinimalistLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
            MinimalistLauncherTheme(darkTheme = preferences.isDarkTheme) {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Home screen — back press intentionally ignored
    }
}
