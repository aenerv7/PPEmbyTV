package magi.aenerv7.ppembytv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C8DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2A44),
    onPrimaryContainer = Color(0xFFA8C7FF),
    secondary = Color(0xFF7C8DB5),
    onSecondary = Color(0xFF111827),
    background = Color(0xFF101014),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF18181C),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF232329),
    onSurfaceVariant = Color(0xFFB4B8C0),
    outline = Color(0xFF3A3A42),
)

@Composable
fun PpEmbyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
