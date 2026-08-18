package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import magi.aenerv7.ppembytv.data.ServerConfig
import java.io.IOException

/**
 * 配置服务管理（对应参考项目的 ConfigServerManager）：
 * 在 8765 端口启动（占用则回退到 8750..8764），返回局域网访问 URL。
 */
class ConfigServerManager(private val context: Context) {

    private var server: ConfigServer? = null
    private var onConfigReceived: ((ServerConfig) -> Unit)? = null

    fun startServer(
        currentConfig: ServerConfig?,
        onConfigReceived: (ServerConfig) -> Unit,
    ): String? {
        stopServer()
        this.onConfigReceived = onConfigReceived

        val candidates = buildList {
            add(DEFAULT_PORT)
            for (i in 8764 downTo MIN_PORT) add(i)
        }

        var lastError: IOException? = null
        for (port in candidates) {
            try {
                val s = ConfigServer(port, context, currentConfig) { config ->
                    this.onConfigReceived?.invoke(config)
                }
                server = s
                s.start()
                val ip = LocalNetwork.getLocalIpAddress(context)
                if (ip == null) {
                    Log.e(TAG, "无法获取本机 IP")
                    s.stop()
                    server = null
                    return null
                }
                return "http://$ip:$port/"
            } catch (e: IOException) {
                server?.stop()
                server = null
                lastError = e
                val msg = e.message
                if (msg == null || !msg.contains("EADDRINUSE")) {
                    break
                }
                Log.e(TAG, "端口 $port 被占用，尝试下一个")
            }
        }
        Log.e(TAG, "所有候选端口均启动失败", lastError)
        return null
    }

    fun stopServer() {
        server?.stop()
        server = null
        onConfigReceived = null
    }

    companion object {
        private const val TAG = "ConfigServerManager"
        private const val DEFAULT_PORT = 8765
        private const val MIN_PORT = 8750
    }
}
