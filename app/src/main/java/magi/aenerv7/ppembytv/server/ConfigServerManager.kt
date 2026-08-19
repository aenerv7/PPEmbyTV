package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.model.ServerConfig
import java.io.IOException

class ConfigServerManager(private val context: Context) {

    private var server: ConfigServer? = null
    private var onConfigReceived: (suspend (ServerConfig) -> Unit)? = null

    private fun handleConfig(config: ServerConfig) {
        CoroutineScope(Dispatchers.Main).launch {
            onConfigReceived?.invoke(config)
        }
    }

    fun startServer(
        currentConfig: ServerConfig? = null,
        onConfigReceived: suspend (ServerConfig) -> Unit,
    ): String? {
        stopServer()
        this.onConfigReceived = onConfigReceived
        val ports = buildList {
            add(DEFAULT_PORT)
            for (i in 8764 downTo MIN_PORT) {
                add(i)
            }
        }
        var lastError: IOException? = null
        for (port in ports) {
            try {
                val configServer = ConfigServer(port, context, currentConfig) { handleConfig(it) }
                server = configServer
                configServer.start()
                return resolveServerUrl(context, port)
            } catch (e: IOException) {
                server?.stop()
                server = null
                Log.e(TAG, "Failed to start server on port $port", e)
                lastError = e
                val message = e.message
                if (message == null || !message.contains("EADDRINUSE")) {
                    break
                }
            }
        }
        if (lastError != null) {
            Log.e(TAG, "All candidate ports failed", lastError)
        }
        return null
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    private companion object {
        const val TAG = "ConfigServerManager"
        const val DEFAULT_PORT = 8765
        const val MIN_PORT = 8750
    }
}
