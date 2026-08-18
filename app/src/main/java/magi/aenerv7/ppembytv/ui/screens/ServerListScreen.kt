package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.data.ServerConfig
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.components.WideCard
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import kotlinx.coroutines.launch

/** 服务器列表页 */
@Composable
fun ServerListScreen(navigator: AppNavigator) {
    val servers by AppGraph.serverRepository.servers.collectAsState(initial = emptyList())
    val activeServer by Session.activeServer.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Text(
            text = "选择服务器",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "皮皮 TV · Android TV Emby 客户端",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(24.dp))

        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有服务器，请添加你的 Emby 服务器",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        isActive = server.id == activeServer?.id,
                        onClick = {
                            if (server.isLoggedIn && server.accessToken.isNotEmpty()) {
                                scope.launch {
                                    AppGraph.serverRepository.setLastUsedServerId(server.id)
                                    Session.setActiveServer(server)
                                    Session.invalidate()
                                    navigator.resetToHome()
                                }
                            } else {
                                navigator.push(Screen.Login(server.id))
                            }
                        },
                        onRemove = {
                            scope.launch {
                                AppGraph.serverRepository.removeServer(server.id)
                                if (Session.activeServer.value?.id == server.id) {
                                    Session.setActiveServer(null)
                                }
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TvButton("➕ 手动添加服务器", { navigator.push(Screen.AddServer) })
            TvButton("📱 扫码配置", { navigator.push(Screen.QrConfig) })
            TvButton("⚙️ 设置", { navigator.push(Screen.Settings) })
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    isActive: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    WideCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(44.dp)
                    .background(
                        if (isActive) Color(0xFF4DA3FF) else Color(0xFF3A3F4A),
                        RoundedCornerShape(5.dp),
                    )
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.alias.ifBlank { server.host },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (server.isLoggedIn) "已登录" else "未登录",
                        color = if (server.isLoggedIn) Color(0xFF4CD964) else Color(0xFFFFB454),
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "${server.getBaseUrl()}${if (server.path.isNotBlank()) "/${server.path}" else ""} · ${server.username.ifBlank { "未设置账号" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TvButton(if (server.isLoggedIn) "进入" else "登录", onClick)
            Spacer(Modifier.width(10.dp))
            TvButton("删除", onRemove)
        }
    }
}
