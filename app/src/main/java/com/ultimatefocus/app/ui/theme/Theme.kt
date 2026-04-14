/**
 * MaterialTheme wrapper that applies the minimalist color scheme and typography.
 * Also synchronises the status bar and navigation bar colors with the theme background.
 * Covers: ULT-5
 */
package com.ultimatefocus.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    primary = DarkAccent,
    onPrimary = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    primary = LightAccent,
    onPrimary = LightBackground,
)

@Composable
fun UltimateFocusTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as ComponentActivity
            val barColor = colorScheme.background.toArgb()
            val style = if (darkTheme) {
                SystemBarStyle.dark(barColor)
            } else {
                SystemBarStyle.light(barColor, barColor)
            }
            activity.enableEdgeToEdge(
                statusBarStyle = style,
                navigationBarStyle = style,
            )
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = MinimalistTypography, content = content)
}
