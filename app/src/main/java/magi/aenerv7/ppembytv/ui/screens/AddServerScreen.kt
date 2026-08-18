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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.data.ServerConfig
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import kotlinx.coroutines.launch

/** 手动添加服务器页（含登录） */
@Composable
fun AddServerScreen(navigator: AppNavigator) {
    var alias by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("http") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8096") }
    var path by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var directOnly by remember { mutableStateOf(false) }
    var trustAllCerts by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun buildServer(): ServerConfig {
        val parsedHost = parseHostInput(host)
        return ServerConfig(
            id = AppGraph.serverRepository.generateServerId(),
            alias = alias.ifBlank { parsedHost.first },
            protocol = parsedHost.second ?: protocol,
            host = parsedHost.first,
            port = parsedHost.third ?: (port.toIntOrNull() ?: 8096),
            path = path.trim().trim('/'),
            username = username,
            password = password,
            directOnly = directOnly,
            trustAllCerts = trustAllCerts,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 56.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "添加服务器",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            TvButton("← 返回", { navigator.pop() })
        }

        TvTextField("服务器名称（别名）", alias) { alias = it }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("协议", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            TvButton(if (protocol == "http") "● HTTP" else "HTTP", { protocol = "http" })
            TvButton(if (protocol == "https") "● HTTPS" else "HTTPS", { protocol = "https" })
        }
        TvTextField("服务器地址（可含协议和端口，如 192.168.1.100 或 https://emby.example.com:8920）", host) { host = it }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvTextField("端口", port, Modifier.weight(1f), KeyboardType.Number) { port = it }
            TvTextField("路径（可选，如 emby）", path, Modifier.weight(1f)) { path = it }
        }
        TvTextField("用户名", username) { username = it }
        TvTextField("密码", password, isPassword = true) { password = it }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TvButton(if (directOnly) "● 仅直连（不走代理）" else "仅直连（不走代理）", { directOnly = !directOnly })
            TvButton(if (trustAllCerts) "● 信任所有证书（不安全）" else "信任所有证书（不安全）", { trustAllCerts = !trustAllCerts })
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(
                text = if (busy) "登录中…" else "连接并登录",
                enabled = !busy && host.isNotBlank(),
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val server = buildServer()
                            val loggedIn = login(server, username, password)
                            AppGraph.serverRepository.addServer(loggedIn)
                            AppGraph.serverRepository.setLastUsedServerId(loggedIn.id)
                            Session.setActiveServer(loggedIn)
                            Session.invalidate()
                            navigator.resetToHome()
                        } catch (e: Exception) {
                            error = e.message ?: "登录失败"
                        } finally {
                            busy = false
                        }
                    }
                },
            )
            TvButton(
                text = "仅保存（稍后登录）",
                enabled = !busy && host.isNotBlank(),
                onClick = {
                    scope.launch {
                        AppGraph.serverRepository.addServer(buildServer())
                        navigator.pop()
                    }
                },
            )
        }
    }
}

/** 解析“可能带协议和端口”的主机输入，返回 (host, protocol?, port?) */
fun parseHostInput(raw: String): Triple<String, String?, Int?> {
    var s = raw.trim()
    var protocol: String? = null
    if (s.startsWith("http://")) { protocol = "http"; s = s.removePrefix("http://") }
    else if (s.startsWith("https://")) { protocol = "https"; s = s.removePrefix("https://") }
    s = s.trimEnd('/')
    val port = Regex("^(.*):(\\d{1,5})$").matchEntire(s)?.let { m ->
        Triple(m.groupValues[1], protocol, m.groupValues[2].toIntOrNull())
    }
    return port ?: Triple(s, protocol, null)
}

/** 认证：AuthenticateByName → 保存 token/userId */
suspend fun login(server: ServerConfig, username: String, password: String): ServerConfig {
    val api = Session.apiFor(server)
    val resp = api.authenticate(mapOf("Username" to username, "Pw" to password))
    if (!resp.isSuccessful) {
        val body = resp.errorBody()?.string().orEmpty()
        throw Exception("登录失败（HTTP ${resp.code()}）${if (body.isNotBlank()) body.take(120) else ""}")
    }
    val result = resp.body() ?: throw Exception("服务器响应为空")
    if (result.accessToken.isEmpty()) throw Exception("未获得访问令牌")
    val deviceId = AppGraph.settingsRepository.getOrCreateDeviceId()
    return server.copy(
        username = username,
        password = password,
        userId = result.User?.id.orEmpty(),
        accessToken = result.accessToken,
        deviceId = deviceId,
        isLoggedIn = true,
        lastLoginTime = System.currentTimeMillis(),
    )
}

@Composable
private fun TvTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .background(Color(0xFF1A1D24), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    )
}
