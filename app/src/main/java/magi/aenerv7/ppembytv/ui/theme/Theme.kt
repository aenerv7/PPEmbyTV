package magi.aenerv7.ppembytv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 复刻参考 App（柴柴emby）的暖棕深色主题。
 * 颜色取自原版真机/模拟器截图（1920x1080 像素采样）：
 * - 背景渐变：#322317（顶）→ #150F0A（底）
 * - 主色（焦糖橙）：#D39454（按钮/标题/选中高亮）
 * - 输入框容器：#2D221B；二维码卡片：#4B3917
 */
private val WarmBrownColors = darkColorScheme(
    primary = Color(0xFFD39454),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B3514),
    onPrimaryContainer = Color(0xFFF6EFE5),
    secondary = Color(0xFF8C867A),
    onSecondary = Color(0xFF2A1D12),
    secondaryContainer = Color(0xFF3A2E24),
    onSecondaryContainer = Color(0xFFE8DCCB),
    background = Color(0xFF1C140C),
    onBackground = Color(0xFFF2EEE9),
    surface = Color(0xFF2D221B),
    onSurface = Color(0xFFF2EEE9),
    surfaceVariant = Color(0xFF3A2E24),
    onSurfaceVariant = Color(0xFFC0B9B1),
    outline = Color(0xFF5E5546),
    error = Color(0xFFFF6B6B),
)

/** 服务器列表/空状态背景用的竖向渐变（与原版截图一致）。 */
val TvBackgroundTop: Color = Color(0xFF322317)
val TvBackgroundBottom: Color = Color(0xFF150F0A)

/** 输入框容器色（原版聚焦/未聚焦输入框底色）。 */
val TvInputContainer: Color = Color(0xFF2D221B)

/** 二维码卡片底色。 */
val TvQrPanel: Color = Color(0xFF4B3917)

/** 通用焦点高亮边框色（原版为纯白 2dp）。 */
val TvFocusBorder: Color = Color.White

@Composable
fun PpEmbyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmBrownColors,
        content = content,
    )
}
