package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.data.ProxySettings
import magi.aenerv7.ppembytv.ui.components.PasswordField
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.components.WideCard
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import kotlinx.coroutines.launch

/** 设置页：服务器管理 / 代理 / 关于 */
@Composable
fun SettingsScreen(navigator: AppNavigator) {
    val scope = rememberCoroutineScope()
    val servers by AppGraph.serverRepository.servers.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("设置", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // ---- 服务器管理 ----
        WideCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("服务器管理", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                servers.forEach { server ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${server.alias.ifBlank { server.host }}  ${if (server.isLoggedIn) "· 已登录" else "· 未登录"}",
                                color = Color.White,
                                fontSize = 15.sp,
                            )
                            Text(
                                text = server.getFullUrl(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        TvButton("删除", {
                            scope.launch { AppGraph.serverRepository.removeServer(server.id) }
                        })
                    }
                }
                if (servers.isEmpty()) {
                    Text("暂无服务器", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton("添加服务器", { navigator.push(Screen.AddServer) })
                    TvButton("扫码配置", { navigator.push(Screen.QrConfig) })
                }
            }
        }

        // ---- 代理设置 ----
        ProxySettingsCard()

        // ---- 关于 ----
        WideCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("关于", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "皮皮 TV v${magi.aenerv7.ppembytv.BuildConfig.VERSION_NAME}\n" +
                        "Android TV Emby 客户端 · 功能参考 ChaiChaiEmbyTV（不含弹幕）\n" +
                        "仅供学习交流使用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ProxySettingsCard() {
    val scope = rememberCoroutineScope()
    val proxy by AppGraph.settingsRepository.proxy.collectAsState(initial = ProxySettings())
    var enabled by remember { mutableStateOf(proxy.enabled) }
    var type by remember { mutableStateOf(proxy.type) }
    var host by remember { mutableStateOf(proxy.host) }
    var port by remember { mutableStateOf(proxy.port.toString()) }
    var username by remember { mutableStateOf(proxy.username) }
    var password by remember { mutableStateOf(proxy.password) }
    var bypassLan by remember { mutableStateOf(proxy.bypassLan) }
    var saved by remember { mutableStateOf(false) }

    WideCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("代理设置（http / socks5）", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TvButton(if (enabled) "● 已启用" else "已禁用", { enabled = !enabled })
                TvButton(if (type == "http") "● HTTP" else "HTTP", { type = "http" })
                TvButton(if (type == "socks") "● SOCKS5" else "SOCKS5", { type = "socks" })
                TvButton(if (bypassLan) "● 局域网直连" else "局域网直连", { bypassLan = !bypassLan })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTextField("代理地址", host, Modifier.weight(1f)) { host = it }
                SettingsTextField("端口", port, Modifier.weight(1f), KeyboardType.Number) { port = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTextField("用户名（可选）", username, Modifier.weight(1f)) { username = it }
                PasswordField("密码（可选）", password, Modifier.weight(1f)) { password = it }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TvButton("保存代理设置", {
                    scope.launch {
                        AppGraph.settingsRepository.setProxy(
                            ProxySettings(
                                enabled = enabled,
                                type = type,
                                host = host.trim(),
                                port = port.toIntOrNull() ?: 0,
                                username = username.trim(),
                                password = password,
                                bypassLan = bypassLan,
                            )
                        )
                        saved = true
                    }
                })
                if (saved) {
                    Text("✅ 已保存", color = Color(0xFF4CD964), fontSize = 14.sp)
                }
            }
            Text(
                "提示：代理对所有服务器生效；勾选「仅直连」的服务器会绕过代理。SOCKS5 模式下域名由代理服务器解析。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .background(Color(0xFF1A1D24), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    )
}
