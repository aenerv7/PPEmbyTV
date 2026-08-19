package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.OnlineSubtitleConfig
import java.io.IOException

class OnlineSubtitleConfigServerManager(private val context: Context) {

    private var server: OnlineSubtitleConfigServer? = null

    fun startServer(
        initialConfig: OnlineSubtitleConfig,
        onConfigReceived: (OnlineSubtitleConfig) -> Unit,
    ): String? {
        stopServer()
        var lastError: IOException? = null
        for (port in DEFAULT_PORT until 8800) {
            try {
                val configServer = OnlineSubtitleConfigServer(port, initialConfig) { config ->
                    CoroutineScope(Dispatchers.Main).launch { onConfigReceived(config) }
                }
                server = configServer
                configServer.start()
                return resolveServerUrl(context, port)
            } catch (e: IOException) {
                lastError = e
                server?.stop()
                server = null
                Log.e(TAG, "端口 $port 启动失败", e)
                val message = e.message
                if (message == null || !message.contains("EADDRINUSE")) {
                    break
                }
            }
        }
        if (lastError != null) {
            Log.e(TAG, "所有候选端口均启动失败", lastError)
        }
        return null
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    private companion object {
        const val TAG = "OnlineSubCfgServer"
        const val DEFAULT_PORT = 8770
        const val MAX_PORT = 8799
    }
}
