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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.ui.components.PasswordField
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import kotlinx.coroutines.launch

/** 登录页：为已保存但未登录的服务器补充账号登录 */
@Composable
fun LoginScreen(
    navigator: AppNavigator,
    serverId: String,
) {
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf<magi.aenerv7.ppembytv.data.ServerConfig?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 加载服务器配置
    androidx.compose.runtime.LaunchedEffect(serverId) {
        server = AppGraph.serverRepository.getServer(serverId)
        username = server?.username.orEmpty()
        password = server?.password.orEmpty()
    }

    val srv = server
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(horizontal = 80.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("登录服务器", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = srv?.let { "${it.alias.ifBlank { it.host }} · ${it.getBaseUrl()}" } ?: "加载中…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(26.dp))

        LoginField("用户名", username, Modifier.fillMaxWidth().width(560.dp)) { username = it }
        Spacer(Modifier.height(12.dp))
        PasswordField("密码", password, Modifier.fillMaxWidth().width(560.dp)) { password = it }

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error ?: "", color = Color(0xFFFF6B6B), fontSize = 14.sp)
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(
                text = if (busy) "登录中…" else "登录",
                enabled = !busy && srv != null,
                onClick = {
                    val s = srv ?: return@TvButton
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val loggedIn = login(s, username, password)
                            AppGraph.serverRepository.updateServer(loggedIn)
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
            TvButton("← 返回", { navigator.pop() })
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
        modifier = modifier
            .focusable()
            .background(Color(0xFF1A1D24), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    )
}
