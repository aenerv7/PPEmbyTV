package magi.aenerv7.ppembytv.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen

/**
 * 左侧导航栏：首页 / 电影 / 电视剧 / 搜索 / 设置。
 */
@Composable
fun NavRail(
    navigator: AppNavigator,
    selected: Screen,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Color(0xFF13161B))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "PP\nTV",
            color = Color(0xFF4DA3FF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        NavItem("🏠", "首页", selected is Screen.Home || selected is Screen.Library, Modifier.weight(1f, fill = false)) {
            navigator.replace(Screen.Home)
        }
        NavItem("🎬", "电影", selected is Screen.Movies, Modifier.weight(1f, fill = false)) {
            navigator.replace(Screen.Movies)
        }
        NavItem("📺", "电视剧", selected is Screen.TvShows, Modifier.weight(1f, fill = false)) {
            navigator.replace(Screen.TvShows)
        }
        NavItem("🔍", "搜索", selected is Screen.Search, Modifier.weight(1f, fill = false)) {
            navigator.replace(Screen.Search)
        }
        Spacer(Modifier.height(12.dp))
        NavItem("⚙️", "设置", selected is Screen.Settings, Modifier.weight(1f, fill = false)) {
            navigator.replace(Screen.Settings)
        }
    }
}

@Composable
private fun NavItem(
    icon: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .graphicsLayer {
                scaleX = if (focused) 1.06f else 1f
                scaleY = if (focused) 1.06f else 1f
            }
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = icon,
            fontSize = 22.sp,
        )
        Text(
            text = label,
            color = if (selected) Color(0xFF4DA3FF) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
