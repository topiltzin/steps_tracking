package app.steptracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Mint80,
    onPrimary = Ink,
    secondary = Cyan80,
    onSecondary = Ink,
    tertiary = Cyan80,
    background = Ink,
    onBackground = InkText,
    surface = InkSurface,
    onSurface = InkText,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainerHigh = InkSurfaceHigh,
    outlineVariant = InkSurfaceHigh,
)

private val LightColors = lightColorScheme(
    primary = Mint40,
    onPrimary = CloudSurface,
    secondary = Cyan40,
    onSecondary = CloudSurface,
    tertiary = Cyan40,
    background = Cloud,
    onBackground = CloudText,
    surface = CloudSurface,
    onSurface = CloudText,
    surfaceVariant = CloudSurfaceHigh,
    onSurfaceVariant = CloudMuted,
    surfaceContainerHigh = CloudSurfaceHigh,
    outlineVariant = CloudSurfaceHigh,
)

/** Fixed brand palette (no dynamic color) so the look stays consistent across devices. */
@Composable
fun StepTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
