package magi.aenerv7.ppembytv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.ui.theme.TvFocusBorder

/**
 * 复刻参考 App 的可聚焦点击组件：
 * - D-pad 方向键导航，Enter/确认键触发点击；
 * - **支持触屏**（tap 直接触发，便于在手机上检查）；
 * - onFocusChanged 回调用于绘制白色焦点高亮边框。
 */
fun Modifier.tvClickable(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource? = null,
    onFocusChanged: (Boolean) -> Unit = {},
): Modifier {
    val focusMod = if (interactionSource != null) {
        Modifier.focusable(enabled = true, interactionSource = interactionSource)
    } else {
        Modifier.focusable()
    }
    // The observer must precede the focus target or it will not receive that target's state changes.
    return this
        .onFocusChanged { onFocusChanged(it.isFocused) }
        .then(focusMod)
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
}

/** 通用焦点高亮：聚焦时显示 2dp 白色圆角边框（原版 ClickableSurface 聚焦态）。 */
fun Modifier.tvFocusBorder(focused: Boolean, shape: RoundedCornerShape = RoundedCornerShape(10.dp)): Modifier =
    this.then(
        if (focused) Modifier.border(2.dp, TvFocusBorder, shape) else Modifier
    )

/** PP TV 主按钮：品牌蓝胶囊按钮，聚焦时缩放 1.05 + 白色 2dp 边框。 */
@Composable
fun TvButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    shape: RoundedCornerShape = RoundedCornerShape(50),
    height: Int = 64,
    horizontalPadding: Int = 24,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val interactionFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused || interactionFocused) 1.05f else 1f, label = "btnScale")
    Row(
        modifier = modifier
            .height(height.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvClickable(
                onClick = onClick,
                interactionSource = interactionSource,
                onFocusChanged = { f ->
                    focused = f
                },
            )
            .clip(shape)
            .background(containerColor)
            .tvFocusBorder(focused || interactionFocused, shape)
            .padding(horizontal = horizontalPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
}

/** 图标按钮（如服务器列表左上角设置齿轮），聚焦时白色 2dp 边框。 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    tint: Color = Color.White,
    size: Int = 48,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(size.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .tvFocusBorder(focused, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size((size * 0.62f).toInt().dp))
    }
}

/** 可聚焦卡片（原版 ClickableSurface：未聚焦 1dp 主色边框，聚焦 2dp 白色边框）。 */
@Composable
fun TvCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) TvFocusBorder else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = shape,
            ),
    ) {
        content()
    }
}

/** 复刻原版复选框行：方形勾选框 + 标题（+ 可选说明），聚焦时白色边框。 */
@Composable
fun TvCheckRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    onToggle: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(onClick = onToggle, onFocusChanged = { focused = it })
            .clip(shape)
            .tvFocusBorder(focused, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text("✓", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** 复刻原版海报卡片：聚焦时缩放 + 白色边框高亮。 */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Int = 160,
    height: Int = 240,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "posterScale")
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .width(width.dp)
            .scale(scale)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .tvFocusBorder(focused, shape)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(height.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

fun imageUrl(itemId: String?, imageType: String = "Primary", tag: String?, maxWidth: Int = 320): String? {
    if (itemId == null) return null
    return RetrofitClient.getImageUrl(itemId, imageType, tag, maxWidth)
}

fun backdropUrl(itemId: String?, tag: String?, maxWidth: Int = 1920): String? {
    if (itemId == null) return null
    return RetrofitClient.getImageUrl(itemId, "Backdrop", tag, maxWidth)
}
