package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD

/** 在线字幕搜索输入服务器：GET / 展示搜索页，POST /search（表单 keyword）搜索在线字幕。 */
class OnlineSubtitleSearchInputServerManager(private val context: Context) {

    private var server: OnlineSubtitleSearchInputServer? = null

    fun startServer(onSearchReceived: (String) -> Unit) {
        stopServer()
        val searchServer = OnlineSubtitleSearchInputServer(onSearchReceived)
        server = searchServer
        try {
            searchServer.start()
            Log.d(TAG, "在线字幕搜索服务器启动成功，端口: 8769")
        } catch (e: Exception) {
            Log.e(TAG, "在线字幕搜索服务器启动失败", e)
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    fun getServerUrl(): String =
        resolveServerUrl(context, PORT) ?: "http://192.168.1.1:8769"

    inner class OnlineSubtitleSearchInputServer(
        private val onSearchReceived: (String) -> Unit,
    ) : NanoHTTPD(PORT) {

        override fun serve(session: IHTTPSession): Response {
            if (session.uri == "/") {
                return serveHtmlPage()
            }
            if (session.uri == "/search" && session.method == Method.POST) {
                return handleSearchPost(session)
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
        }

        private fun handleSearchPost(session: IHTTPSession): Response {
            return try {
                session.parseBody(HashMap())
                val keyword = session.parameters["keyword"]?.firstOrNull()?.trim() ?: ""
                if (keyword.isBlank()) {
                    NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Keyword is required")
                } else {
                    onSearchReceived.invoke(keyword)
                    NanoHTTPD.newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Handle search post failed", e)
                NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Server Error")
            }
        }

        private fun serveHtmlPage(): Response {
            val response = NanoHTTPD.newFixedLengthResponse(
                Response.Status.OK,
                "text/html; charset=UTF-8",
                """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>在线字幕搜索</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
            min-height: 100vh;
            padding: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            max-width: 420px;
            width: 100%;
            background: white;
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 { color: #111827; margin-bottom: 10px; font-size: 24px; text-align: center; }
        .subtitle { color: #6b7280; margin-bottom: 24px; font-size: 14px; text-align: center; }
        input[type="text"] {
            width: 100%;
            padding: 14px 16px;
            border: 2px solid #d1d5db;
            border-radius: 12px;
            font-size: 16px;
            transition: border-color 0.3s;
            margin-bottom: 16px;
        }
        input[type="text"]:focus { outline: none; border-color: #2563eb; }
        button {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
            color: white;
            border: none;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
        }
        .tips {
            margin-top: 18px;
            padding: 12px;
            background: #eff6ff;
            border-radius: 8px;
            font-size: 13px;
            color: #1d4ed8;
        }
        .success, .error {
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            padding: 16px 24px;
            border-radius: 8px;
            display: none;
            z-index: 1000;
        }
        .success { background: #10b981; color: white; }
        .error { background: #ef4444; color: white; }
    </style>
</head>
<body>
    <div class="container">
        <h1>字幕搜索</h1>
        <p class="subtitle">输入影片名或剧名，同步到电视搜索在线字幕</p>
        <input type="text" id="searchInput" placeholder="例如：沙丘 / 进击的巨人" autofocus>
        <button type="button" id="searchBtn">发送到电视</button>
        <div class="tips">提示：这里只输入片名或剧名即可，不需要包含分辨率或字幕格式。</div>
    </div>
    <div class="success" id="success">已发送到电视</div>
    <div class="error" id="error">发送失败，请重试</div>
    <script>
        const searchInput = document.getElementById('searchInput');
        const searchBtn = document.getElementById('searchBtn');
        const success = document.getElementById('success');
        const error = document.getElementById('error');

        async function sendSearch() {
            const keyword = searchInput.value.trim();
            if (!keyword) {
                searchInput.focus();
                return;
            }
            searchBtn.disabled = true;
            try {
                const response = await fetch('/search', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'keyword=' + encodeURIComponent(keyword)
                });
                if (response.ok) {
                    success.style.display = 'block';
                    setTimeout(() => success.style.display = 'none', 2000);
                } else {
                    throw new Error('request failed');
                }
            } catch (e) {
                error.style.display = 'block';
                setTimeout(() => error.style.display = 'none', 2000);
            } finally {
                searchBtn.disabled = false;
            }
        }

        searchBtn.addEventListener('click', sendSearch);
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendSearch();
        });
    </script>
</body>
</html>"""
            )
            return response
        }
    }

    private companion object {
        const val TAG = "OnlineSubtitleSearchServer"
        const val PORT = 8769
    }
}
