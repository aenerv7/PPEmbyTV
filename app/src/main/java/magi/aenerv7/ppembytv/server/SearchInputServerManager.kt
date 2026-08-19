package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 聚合搜索输入服务器：GET / 展示搜索页，POST /search 提交关键词到电视搜索。 */
class SearchInputServerManager(private val context: Context) {

    private var server: SearchInputServer? = null
    private var activePort: Int? = null

    fun startServer(onSearchReceived: (String) -> Unit) {
        stopServer()
        var lastError: Exception? = null
        for (port in DEFAULT_PORT until 8800) {
            var candidate: SearchInputServer? = null
            try {
                candidate = SearchInputServer(port, onSearchReceived)
            } catch (e: Exception) {
                lastError = e
            }
            try {
                candidate!!.start()
                server = candidate
                activePort = port
                Log.d(TAG, "搜索输入服务器启动成功，端口: $port")
                return
            } catch (e: Exception) {
                lastError = e
                candidate?.stop()
                server?.stop()
                server = null
                activePort = null
                Log.e(TAG, "搜索输入服务器端口 $port 启动失败", e)
                if (!isPortInUse(e)) {
                    break
                }
            }
        }
        if (lastError != null) {
            Log.e(TAG, "所有候选端口均启动失败", lastError)
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        activePort = null
    }

    fun getServerUrl(): String {
        val port = activePort ?: DEFAULT_PORT
        return resolveServerUrl(context, port) ?: "http://192.168.1.1:$port"
    }

    private fun isPortInUse(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val message = current.message
            if (message != null && message.contains("EADDRINUSE")) {
                return true
            }
            current = current.cause
        }
        return false
    }

    inner class SearchInputServer(
        port: Int,
        private val onSearchReceived: (String) -> Unit,
    ) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            if (uri == "/") {
                return serveHtmlPage()
            }
            if (uri == "/search" && session.method == Method.POST) {
                return handleSearchPost(session)
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
        }

        private fun handleSearchPost(session: IHTTPSession): Response {
            return try {
                val params = HashMap<String, String>()
                session.parseBody(params)
                val postData = params["postData"]
                    ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "No data")
                Log.d(TAG, "Received search data: $postData")
                var keyword = postData
                try {
                    keyword = JSONObject(postData).getString("keyword")
                } catch (e: Exception) {
                    // 非 JSON 时直接使用原始数据
                }
                Log.d(TAG, "Search keyword: $keyword")
                CoroutineScope(Dispatchers.Main).launch {
                    onSearchReceived(keyword)
                }
                NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", "{\"success\":true}")
            } catch (e: Exception) {
                Log.e(TAG, "Handle search post failed", e)
                NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
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
    <title>聚合搜索</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            max-width: 400px;
            width: 100%;
            background: white;
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 { color: #333; margin-bottom: 10px; font-size: 24px; text-align: center; }
        .subtitle { color: #666; margin-bottom: 24px; font-size: 14px; text-align: center; }
        .form-group { margin-bottom: 20px; }
        input[type="text"] {
            width: 100%;
            padding: 14px 16px;
            border: 2px solid #e0e0e0;
            border-radius: 12px;
            font-size: 16px;
            transition: border-color 0.3s;
        }
        input[type="text"]:focus { outline: none; border-color: #667eea; }
        button {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white; border: none; border-radius: 12px;
            font-size: 16px; font-weight: 600; cursor: pointer;
            transition: transform 0.2s, opacity 0.2s;
        }
        button:active { transform: scale(0.98); }
        button:disabled { opacity: 0.6; cursor: not-allowed; }
        .success, .error {
            position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
            padding: 16px 24px; border-radius: 8px; text-align: center;
            display: none; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 1000; min-width: 200px;
        }
        .success { background: #10b981; color: white; }
        .error { background: #ef4444; color: white; }
        .history { margin-top: 20px; }
        .history-title { font-size: 14px; color: #888; margin-bottom: 10px; }
        .history-item {
            padding: 10px 14px;
            background: #f5f5f5;
            border-radius: 8px;
            margin-bottom: 8px;
            cursor: pointer;
            transition: background 0.2s;
        }
        .history-item:hover { background: #e8e8e8; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔍 聚合搜索</h1>
        <p class="subtitle">输入影片名称，同步到电视搜索</p>
        
        <div class="form-group">
            <input type="text" id="searchInput" placeholder="输入影片名称，如：凡人修仙传" autofocus>
        </div>
        
        <button type="button" id="searchBtn">📺 发送到电视</button>
        
        <div class="history" id="historySection" style="display: none;">
            <div class="history-title">搜索历史</div>
            <div id="historyList"></div>
        </div>
    </div>
    
    <div class="success" id="success">✅ 已发送到电视！</div>
    <div class="error" id="error">❌ 发送失败，请重试</div>
    
    <script>
        const searchInput = document.getElementById('searchInput');
        const searchBtn = document.getElementById('searchBtn');
        const historySection = document.getElementById('historySection');
        const historyList = document.getElementById('historyList');
        
        // 加载搜索历史
        function loadHistory() {
            const history = JSON.parse(localStorage.getItem('searchHistory') || '[]');
            if (history.length > 0) {
                historySection.style.display = 'block';
                historyList.innerHTML = history.slice(0, 5).map(item => 
                    '<div class="history-item" onclick="useHistory(\'' + item.replace(/'/g, "\\'") + '\')">' + item + '</div>'
                ).join('');
            }
        }
        
        // 保存搜索历史
        function saveHistory(keyword) {
            let history = JSON.parse(localStorage.getItem('searchHistory') || '[]');
            history = history.filter(item => item !== keyword);
            history.unshift(keyword);
            history = history.slice(0, 10);
            localStorage.setItem('searchHistory', JSON.stringify(history));
            loadHistory();
        }
        
        // 使用历史记录
        window.useHistory = function(keyword) {
            searchInput.value = keyword;
            sendSearch();
        }
        
        // 发送搜索
        async function sendSearch() {
            const keyword = searchInput.value.trim();
            if (!keyword) {
                searchInput.focus();
                return;
            }
            
            searchBtn.disabled = true;
            searchBtn.textContent = '发送中...';
            
            try {
                const response = await fetch('/search', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                    body: JSON.stringify({ keyword: keyword })
                });
                
                if (response.ok) {
                    saveHistory(keyword);
                    document.getElementById('success').style.display = 'block';
                    document.getElementById('error').style.display = 'none';
                    setTimeout(() => document.getElementById('success').style.display = 'none', 2000);
                } else {
                    throw new Error('Failed');
                }
            } catch (error) {
                document.getElementById('error').style.display = 'block';
                document.getElementById('success').style.display = 'none';
                setTimeout(() => document.getElementById('error').style.display = 'none', 3000);
            } finally {
                searchBtn.disabled = false;
                searchBtn.textContent = '📺 发送到电视';
            }
        }
        
        searchBtn.addEventListener('click', sendSearch);
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendSearch();
        });
        
        loadHistory();
    </script>
</body>
</html>"""
            )
            response.addHeader("Content-Type", "text/html; charset=UTF-8")
            return response
        }
    }

    private companion object {
        const val TAG = "SearchInputServer"
        const val DEFAULT_PORT = 8767
        const val MAX_PORT = 8799
    }
}
