package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.WebDavSyncConfig
import java.io.IOException

class WebDavSyncConfigServerManager(private val context: Context) {

    private var server: WebDavSyncConfigServer? = null

    fun startServer(
        initialConfig: WebDavSyncConfig,
        onConfigReceived: (WebDavSyncConfig) -> Unit,
    ): String? {
        stopServer()
        var lastError: IOException? = null
        var port = DEFAULT_PORT
        while (port < 8800) {
            try {
                val configServer = WebDavSyncConfigServer(port, initialConfig) { config ->
                    CoroutineScope(Dispatchers.Main).launch { onConfigReceived(config) }
                }
                server = configServer
                configServer.start()
                return resolveServerUrl(context, port)
            } catch (e: IOException) {
                server?.stop()
                server = null
                Log.e(TAG, "端口 $port 启动失败", e)
                lastError = e
                val message = e.message
                if (message == null || !message.contains("EADDRINUSE")) {
                    break
                }
                port++
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
        const val TAG = "WebDavSyncCfgServer"
        const val DEFAULT_PORT = 8772
        const val MAX_PORT = 8799
    }
}
