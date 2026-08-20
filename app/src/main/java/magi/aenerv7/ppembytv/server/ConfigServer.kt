package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.preferences.ServerPreferences

/** 局域网配置服务器：展示 Emby 服务器配置表单，POST /config 保存。 */
internal class ConfigServer(
    port: Int,
    private val context: Context,
    private val currentConfig: ServerConfig?,
    private val onConfigReceived: (ServerConfig) -> Unit,
) : NanoHTTPD(port) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri == "/") {
            return serveHtmlPage()
        }
        if (uri == "/config" && session.method == Method.POST) {
            return handleConfigPost(session)
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun handleConfigPost(session: IHTTPSession): Response {
        return try {
            val params = HashMap<String, String>()
            session.parseBody(params)
            val postData = params["postData"]
                ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "No data")
            val configData = gson.fromJson(postData, ConfigData::class.java)
            Log.d(TAG, "Received server config: alias=${configData.alias}, host=${configData.host}, port=${configData.port}")
            val config = ServerConfig(
                id = ServerPreferences(context).generateServerId(),
                alias = configData.alias,
                protocol = configData.protocol,
                host = configData.host,
                port = configData.port,
                path = configData.path,
                username = configData.username,
                password = configData.password,
                directOnly = configData.directOnly,
                enableStrmDirectPlay = configData.enableStrmDirectPlay,
                trustAllCerts = configData.trustAllCerts,
                note = configData.note?.trim()?.takeIf { it.isNotEmpty() },
            )
            Log.d(TAG, "Calling onConfigReceived callback...")
            CoroutineScope(Dispatchers.Main).launch {
                onConfigReceived(config)
                Log.d(TAG, "Config received callback executed")
            }
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain; charset=UTF-8", "OK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle config", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtmlPage(): Response {
        val escapedAlias = escapeHtmlAttribute(currentConfig?.alias)
        val escapedNote = escapeHtmlAttribute(currentConfig?.note)
        val httpSelected = if (currentConfig?.protocol == "http") "selected" else ""
        val httpsSelected = if (currentConfig?.protocol == "https") "selected" else ""
        val escapedHost = escapeHtmlAttribute(currentConfig?.host)
        val port = currentConfig?.port ?: 8096
        val escapedPath = escapeHtmlAttribute(currentConfig?.path)
        val escapedUsername = escapeHtmlAttribute(currentConfig?.username)
        val escapedPassword = escapeHtmlAttribute(currentConfig?.password)
        val directOnlyChecked = if (currentConfig?.directOnly == true) "checked" else ""
        val enableStrmDirectPlayChecked = if (currentConfig?.enableStrmDirectPlay == true) "checked" else ""
        val trustAllCertsChecked = if (currentConfig?.trustAllCerts == true) "checked" else ""
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Emby 服务器配置</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 500px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 16px;
                        padding: 30px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 24px;
                    }
                    .subtitle {
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 14px;
                    }
                    .form-group {
                        margin-bottom: 20px;
                    }
                    label {
                        display: block;
                        margin-bottom: 8px;
                        color: #555;
                        font-weight: 500;
                        font-size: 14px;
                    }
                    input, select {
                        width: 100%;
                        padding: 12px;
                        border: 2px solid #e0e0e0;
                        border-radius: 8px;
                        font-size: 16px;
                        transition: border-color 0.3s;
                    }
                    input:focus, select:focus {
                        outline: none;
                        border-color: #667eea;
                    }
                    button {
                        width: 100%;
                        padding: 14px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s;
                    }
                    button:active {
                        transform: scale(0.98);
                    }
                    .success {
                        position: fixed;
                        top: 20px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: #10b981;
                        color: white;
                        padding: 16px 24px;
                        border-radius: 8px;
                        text-align: center;
                        display: none;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                        z-index: 1000;
                        min-width: 200px;
                        animation: slideDown 0.3s ease-out;
                    }
                    .error {
                        position: fixed;
                        top: 20px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: #ef4444;
                        color: white;
                        padding: 16px 24px;
                        border-radius: 8px;
                        text-align: center;
                        display: none;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                        z-index: 1000;
                        min-width: 200px;
                        animation: slideDown 0.3s ease-out;
                    }
                    @keyframes slideDown {
                        from {
                            opacity: 0;
                            transform: translateX(-50%) translateY(-20px);
                        }
                        to {
                            opacity: 1;
                            transform: translateX(-50%) translateY(0);
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📺 Emby 服务器配置</h1>
                    <p class="subtitle">填写您的Emby服务器信息</p>
                    
                    <form id="configForm">
                        <div class="form-group">
                            <label>服务器别名</label>
                            <input type="text" id="alias" placeholder="例如：家庭Emby" value="${escapedAlias}" required>
                        </div>

                        <div class="form-group">
                            <label>备注（可选）</label>
                            <input type="text" id="note" placeholder="例如：家里主服、走专线、朋友共享" value="${escapedNote}">
                        </div>
                        
                        <div class="form-group">
                            <label>协议</label>
                            <select id="protocol">
                                <option value="http" ${httpSelected}>HTTP</option>
                                <option value="https" ${httpsSelected}>HTTPS</option>
                            </select>
                        </div>
                        
                        <div class="form-group">
                            <label>服务器地址</label>
                            <input type="text" id="host" placeholder="192.168.1.100 或 emby.example.com" value="${escapedHost}" required>
                        </div>
                        
                        <div class="form-group">
                            <label>端口</label>
                            <input type="number" id="port" value="${port}" required>
                        </div>
                        
                        <div class="form-group">
                            <label>路径（可选）</label>
                            <input type="text" id="path" placeholder="如 emby" value="${escapedPath}">
                        </div>
                        
                        <div class="form-group">
                            <label>用户名</label>
                            <input type="text" id="username" placeholder="您的Emby用户名" value="${escapedUsername}" required>
                        </div>
                        
                        <div class="form-group">
                            <label>密码</label>
                            <input type="password" id="password" placeholder="您的Emby密码" value="${escapedPassword}" required>
                        </div>
                        
                        <div class="form-group" style="display: flex; align-items: center; gap: 8px;">
                            <input type="checkbox" id="directOnly" ${directOnlyChecked} style="width: auto; margin: 0;">
                            <label for="directOnly" style="margin: 0; cursor: pointer;">仅直连（不使用代理）</label>
                        </div>

                        <div class="form-group" style="display: flex; align-items: center; gap: 8px;">
                            <input type="checkbox" id="enableStrmDirectPlay" ${enableStrmDirectPlayChecked} style="width: auto; margin: 0;">
                            <label for="enableStrmDirectPlay" style="margin: 0; cursor: pointer;">STRM直链播放</label>
                        </div>
                        
                        <div class="form-group" style="display: flex; align-items: center; gap: 8px;">
                            <input type="checkbox" id="trustAllCerts" ${trustAllCertsChecked} style="width: auto; margin: 0;">
                            <label for="trustAllCerts" style="margin: 0; cursor: pointer; color: #ef4444;">⚠️ 信任所有SSL证书（不安全）</label>
                        </div>
                        
                        <button type="button" id="submitBtn">📺 同步到电视</button>
                    </form>
                </div>
                
                <!-- 提示消息浮动在页面顶部 -->
                <div class="success" id="success">✅ 配置已成功发送到电视！</div>
                <div class="error" id="error">❌ 发送失败，请重试</div>
                
                <script>
                    // 智能解析URL
                    function parseUrl(input) {
                        let url = input.trim();
                        let protocol = null;
                        let port = null;
                        
                        // 识别协议
                        const protocolMatch = url.match(/^(https?):\/\//i);
                        if (protocolMatch) {
                            protocol = protocolMatch[1].toLowerCase();
                            url = url.substring(protocolMatch[0].length);
                        }
                        
                        // 去除结尾斜杠
                        url = url.replace(/[\/\\]+$/, '');
                        
                        // 提取端口
                        const portMatch = url.match(/:(\d+)$/);
                        if (portMatch) {
                            port = parseInt(portMatch[1]);
                            url = url.substring(0, portMatch.index);
                        }
                        
                        return { host: url, protocol: protocol, port: port };
                    }
                    
                    // 服务器地址输入框智能识别
                    document.getElementById('host').addEventListener('input', (e) => {
                        const parsed = parseUrl(e.target.value);
                        
                        // 只更新识别到的值
                        if (parsed.protocol) {
                            document.getElementById('protocol').value = parsed.protocol;
                        }
                        if (parsed.port) {
                            document.getElementById('port').value = parsed.port;
                        }
                    });
                    
                    // 提交按钮点击事件
                    const submitBtn = document.getElementById('submitBtn');
                    console.log('Submit button found:', submitBtn);
                    
                    submitBtn.addEventListener('click', async (e) => {
                        e.preventDefault();
                        console.log('Submit button clicked!');
                        
                        const config = {
                            alias: document.getElementById('alias').value.trim(),
                            note: document.getElementById('note').value.trim(),
                            protocol: document.getElementById('protocol').value,
                            host: document.getElementById('host').value.trim(),
                            port: parseInt(document.getElementById('port').value),
                            path: document.getElementById('path').value.trim(),
                            username: document.getElementById('username').value.trim(),
                            password: document.getElementById('password').value.trim(),
                            directOnly: document.getElementById('directOnly').checked,
                            enableStrmDirectPlay: document.getElementById('enableStrmDirectPlay').checked,
                            trustAllCerts: document.getElementById('trustAllCerts').checked
                        };
                        
                        console.log('Sending config:', config);
                        
                        try {
                            console.log('Fetching /config...');
                            const response = await fetch('/config', {
                                method: 'POST',
                                headers: { 
                                    'Content-Type': 'application/json; charset=UTF-8',
                                    'Accept-Charset': 'UTF-8'
                                },
                                body: JSON.stringify(config)
                            });
                            
                            console.log('Response received:', response.status, response.statusText);
                            
                            if (response.ok) {
                                console.log('Config sent successfully!');
                                document.getElementById('success').style.display = 'block';
                                document.getElementById('error').style.display = 'none';
                                // 不再清空表单，保留用户填写的内容
                                // 3秒后隐藏成功提示
                                setTimeout(() => {
                                    document.getElementById('success').style.display = 'none';
                                }, 3000);
                            } else {
                                throw new Error('Failed to send config: ' + response.status);
                            }
                        } catch (error) {
                            console.error('Send config error:', error);
                            document.getElementById('error').style.display = 'block';
                            document.getElementById('success').style.display = 'none';
                            // 5秒后隐藏错误提示
                            setTimeout(() => {
                                document.getElementById('error').style.display = 'none';
                            }, 5000);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        val response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html)
        response.addHeader("Content-Type", "text/html; charset=UTF-8")
        return response
    }

    private fun escapeHtmlAttribute(value: String?): String {
        if (value == null) return ""
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private data class ConfigData(
        val alias: String,
        val note: String? = "",
        val protocol: String,
        val host: String,
        val port: Int,
        val path: String = "",
        val username: String,
        val password: String,
        val directOnly: Boolean = false,
        val enableStrmDirectPlay: Boolean = false,
        val trustAllCerts: Boolean = false,
    )

    private companion object {
        const val TAG = "ConfigServer"
    }
}
