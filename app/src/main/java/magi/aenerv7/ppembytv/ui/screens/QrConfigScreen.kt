package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.server.ConfigServerManager
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.util.QrCode
import kotlinx.coroutines.launch

/**
 * 扫码配置页：电视端启动本地配置服务并展示二维码，
 * 手机扫码后在浏览器中填写 Emby 服务器配置并同步回电视。
 */
@Composable
fun QrConfigScreen(navigator: AppNavigator) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf<String?>(null) }
    var received by remember { mutableStateOf(false) }
    val manager = remember { ConfigServerManager(context) }

    DisposableEffect(Unit) {
        url = manager.startServer(
            currentConfig = null,
            onConfigReceived = { config ->
                scope.launch {
                    AppGraph.serverRepository.addServer(config)
                    received = true
                }
            },
        )
        onDispose {
            manager.stopServer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val qr = remember(url) { url?.let { QrCode.generate(it, 420) } }
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "配置二维码",
                        modifier = Modifier.size(360.dp),
                    )
                } else {
                    Text(
                        text = "二维码生成失败\n请检查网络/WiFi 连接",
                        color = Color(0xFFFF6B6B),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "用手机扫码，在浏览器中填写 Emby 服务器配置",
                    color = Color.White,
                    fontSize = 16.sp,
                )
                if (url != null) {
                    Text(
                        text = url!!,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Spacer(Modifier.width(60.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (received) "✅ 配置已接收！" else "📱 等待手机同步…",
                    color = if (received) Color(0xFF4CD964) else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "1. 手机连接与电视相同的 WiFi\n" +
                        "2. 扫描左侧二维码（或手动输入上方地址）\n" +
                        "3. 在手机页面填写服务器、账号信息\n" +
                        "4. 点击「同步到电视」",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(10.dp))
                TvButton("← 返回服务器列表", { navigator.pop() })
            }
        }
    }
}
