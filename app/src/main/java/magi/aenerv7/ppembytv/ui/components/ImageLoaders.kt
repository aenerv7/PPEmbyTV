package magi.aenerv7.ppembytv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import magi.aenerv7.ppembytv.api.HttpClients
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.data.ServerConfig

/**
 * 为指定服务器构建 Coil ImageLoader（复用服务器的 OkHttp 客户端：
 * 携带 Emby 认证头、代理与 trust-all 证书配置）。
 */
@Composable
fun rememberServerImageLoader(server: ServerConfig): ImageLoader {
    val context = LocalContext.current
    return remember(server.id, server.trustAllCerts, server.directOnly, server.accessToken) {
        ImageLoader.Builder(context)
            .okHttpClient(HttpClients.buildOkHttpClient(server, Session.currentProxy()))
            .crossfade(true)
            .build()
    }
}
