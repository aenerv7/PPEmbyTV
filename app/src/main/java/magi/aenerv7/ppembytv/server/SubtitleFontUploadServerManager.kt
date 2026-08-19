package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.SubtitleFontEntry
import java.io.IOException

class SubtitleFontUploadServerManager(private val context: Context) {

    private var server: SubtitleFontUploadServer? = null
    private var onFontUploaded: (suspend (SubtitleFontEntry) -> Unit)? = null

    private fun handleUploaded(entry: SubtitleFontEntry) {
        CoroutineScope(Dispatchers.Main).launch {
            onFontUploaded?.invoke(entry)
        }
    }

    fun startServer(onFontUploaded: suspend (SubtitleFontEntry) -> Unit): String? {
        stopServer()
        this.onFontUploaded = onFontUploaded
        var lastError: IOException? = null
        for (port in DEFAULT_PORT until 8800) {
            try {
                val uploadServer = SubtitleFontUploadServer(port, context) { handleUploaded(it) }
                server = uploadServer
                uploadServer.start()
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
        const val TAG = "FontUploadServer"
        const val DEFAULT_PORT = 8768
        const val MAX_PORT = 8799
    }
}
