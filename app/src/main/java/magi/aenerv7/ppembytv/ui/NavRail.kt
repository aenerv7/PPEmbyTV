package magi.aenerv7.ppembytv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.R
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen

/**
 * 左侧导航栏：图标 Logo + 首页 / 电影 / 电视剧 / 搜索 / 设置。
 * 内容整体居中、可滚动，避免在矮屏（手机横屏）上被截断。
 */
@Composable
fun NavRail(
    navigator: AppNavigator,
    selected: Screen,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Color(0xFF13161B)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(96.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 图标 Logo（深色圆角底 + 白色线条兔）
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = "PP TV",
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 4.dp),
            )
            NavItem("🏠", "首页", selected is Screen.Home || selected is Screen.Library) {
                navigator.replace(Screen.Home)
            }
            NavItem("🎬", "电影", selected is Screen.Movies) {
                navigator.replace(Screen.Movies)
            }
            NavItem("📺", "电视剧", selected is Screen.TvShows) {
                navigator.replace(Screen.TvShows)
            }
            NavItem("🔍", "搜索", selected is Screen.Search) {
                navigator.replace(Screen.Search)
            }
            Spacer(Modifier.height(4.dp))
            NavItem("⚙️", "设置", selected is Screen.Settings) {
                navigator.replace(Screen.Settings)
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> Color(0xFF2A3B52)
            focused -> Color(0xFF252A33)
            else -> Color.Transparent
        },
        label = "navBg",
    )
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .graphicsLayer {
                scaleX = if (focused) 1.06f else 1f
                scaleY = if (focused) 1.06f else 1f
            }
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 固定高度的图标容器：防止 emoji 字形被裁切
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = label,
            color = if (selected) Color(0xFF4DA3FF) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
