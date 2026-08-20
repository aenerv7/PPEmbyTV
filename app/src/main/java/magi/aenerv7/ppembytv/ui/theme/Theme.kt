package magi.aenerv7.ppembytv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PP TV 品牌色板：取自应用图标的近黑底、白色线稿和 banner 蓝色强调线。
 * 深蓝层级负责区分背景、容器和浮层，亮蓝只用于选中与主要操作。
 */
private val BlueBlackColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123B68),
    onPrimaryContainer = Color(0xFFEAF4FF),
    secondary = Color(0xFF91A8C5),
    onSecondary = Color(0xFF07111F),
    secondaryContainer = Color(0xFF1A2A3F),
    onSecondaryContainer = Color(0xFFDCE9F8),
    background = Color(0xFF080B13),
    onBackground = Color(0xFFF4F7FB),
    surface = Color(0xFF111A29),
    onSurface = Color(0xFFF4F7FB),
    surfaceVariant = Color(0xFF1A2638),
    onSurfaceVariant = Color(0xFFB9C8DA),
    outline = Color(0xFF3C506A),
    error = Color(0xFFFF6B72),
)

/** 服务器列表/空状态背景用的蓝黑竖向渐变。 */
val TvBackgroundTop: Color = Color(0xFF10213A)
val TvBackgroundBottom: Color = Color(0xFF070A12)

/** 输入框容器色。 */
val TvInputContainer: Color = Color(0xFF111B2A)

/** 二维码卡片底色。 */
val TvQrPanel: Color = Color(0xFF152943)

/** 通用焦点高亮边框色（原版为纯白 2dp）。 */
val TvFocusBorder: Color = Color.White

@Composable
fun PpEmbyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlueBlackColors,
        content = content,
    )
}
