package magi.aenerv7.ppembytv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF001E33),
    primaryContainer = Color(0xFF00497A),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFB6C8DC),
    onSecondary = Color(0xFF21323F),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = Color(0xFF8D9199),
    error = Color(0xFFFFB4AB),
)

@Composable
fun PPEmbyTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
