package magi.aenerv7.ppembytv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import magi.aenerv7.ppembytv.api.BaseItemDto
import magi.aenerv7.ppembytv.util.Formatting

/**
 * TV 焦点交互封装：获得焦点时放大并高亮描边。
 */
@Composable
fun rememberFocusState(): Pair<MutableInteractionSource, Boolean> {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    return interactionSource to focused
}

@Composable
fun Modifier.tvFocus(
    interactionSource: MutableInteractionSource,
    focused: Boolean,
    scale: Float = 1.08f,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp,
): Modifier {
    val animScale by animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "focusScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) Color(0xFF4DA3FF) else Color.Transparent,
        label = "focusBorder",
    )
    return this
        .graphicsLayer {
            scaleX = animScale
            scaleY = animScale
            shadowElevation = if (focused) 24f else 0f
        }
        .clip(RoundedCornerShape(cornerRadius))
        .then(
            if (focused) {
                Modifier.background(
                    borderColor.copy(alpha = 0.35f),
                    RoundedCornerShape(cornerRadius),
                )
            } else Modifier
        )
        .focusable(interactionSource = interactionSource)
}

/**
 * 可聚焦点击项（用于按钮、导航项）。
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (interactionSource, focused) = rememberFocusState()
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFF2A2E36)
            focused -> Color(0xFF4DA3FF)
            else -> Color(0xFF23262E)
        },
        label = "btnBg",
    )
    val fg by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFF9AA0AA)
            focused -> Color.White
            else -> Color(0xFFE1E2E9)
        },
        label = "btnFg",
    )
    Box(
        modifier = modifier
            .tvFocus(interactionSource, focused, scale = 1.06f, cornerRadius = 8.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * 密码输入框（带「显示/隐藏」切换按钮，TV 焦点与触摸均可用）。
 */
@Composable
fun PasswordField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    var showPassword by remember { mutableStateOf(false) }
    val toggleInteraction = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        trailingIcon = {
            Box(
                modifier = Modifier
                    .focusable(interactionSource = toggleInteraction)
                    .clickable(
                        interactionSource = toggleInteraction,
                        indication = null,
                    ) { showPassword = !showPassword }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (showPassword) "隐藏" else "显示",
                    color = Color(0xFF4DA3FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        modifier = modifier
            .background(Color(0xFF1A1D24), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    )
}

/**
 * 海报卡片（电影/剧集/剧集集）：海报在上、标题与副标题在下（带合理间距）。
 * 注意必须整体包在一个 Column 内——若海报与文字作为两个根节点直接平铺，
 * Lazy 容器会把它们横向摆放，导致文字紧贴封面右侧、与相邻卡片重叠。
 */
@Composable
fun PosterCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 150.dp,
    height: androidx.compose.ui.unit.Dp = 220.dp,
    showProgress: Float? = null,
    imageLoader: coil.ImageLoader? = null,
) {
    val (interactionSource, focused) = rememberFocusState()
    Column(
        modifier = modifier.width(width),
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .tvFocus(interactionSource, focused)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1F26))
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        imageLoader = imageLoader ?: coil.compose.LocalImageLoader.current,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title.take(1).ifEmpty { "?" },
                            color = Color(0xFF555B66),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // 观看进度条
                if (showProgress != null && showProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(showProgress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(Color(0xFF4DA3FF))
                        )
                    }
                }
            }
            if (focused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Transparent)
                )
            }
        }
        // 标题区：与封面之间保留 10dp 空隙，标题与副标题之间 3dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** 根据 BaseItemDto 生成海报卡片 */
@Composable
fun ItemPosterCard(
    item: BaseItemDto,
    server: magi.aenerv7.ppembytv.data.ServerConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 150.dp,
    height: androidx.compose.ui.unit.Dp = 220.dp,
) {
    val imageUrl = magi.aenerv7.ppembytv.playback.PlaybackUrlBuilder.imageUrl(
        server = server,
        itemId = item.id,
        type = "Primary",
        tag = item.primaryImageTag,
        maxWidth = (width.value * 2).toInt(),
    )
    val imageLoader = rememberServerImageLoader(server)
    val progress = item.userData?.takeIf { it.playbackPositionTicks > 0 && item.runTimeTicks != null && item.runTimeTicks!! > 0 }
        ?.let { (it.playbackPositionTicks.toFloat() / item.runTimeTicks!!.toFloat()) }

    PosterCard(
        title = item.name,
        subtitle = Formatting.itemSubtitle(item),
        imageUrl = imageUrl,
        onClick = onClick,
        modifier = modifier,
        width = width,
        height = height,
        showProgress = progress,
        imageLoader = imageLoader,
    )
}

/**
 * 横向内容行。
 */
@Composable
fun ContentRow(
    title: String,
    itemCount: Int = 0,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    modifier: Modifier = Modifier,
    itemContent: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (itemCount > 0) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$itemCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { itemContent() }
        }
    }
}

/** 分隔线标题（用于详情页等） */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** 带圆角背景的“宽卡片”（用于横幅/按钮组） */
@Composable
fun WideCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val (interactionSource, focused) = rememberFocusState()
    Box(
        modifier = modifier
            .tvFocus(interactionSource, focused, scale = 1.02f, cornerRadius = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color(0xFF2A2F3A) else Color(0xFF1C1F26))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(20.dp)
    ) {
        content()
    }
}
