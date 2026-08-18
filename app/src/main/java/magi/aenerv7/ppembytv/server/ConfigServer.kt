package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.ServerConfig
import magi.aenerv7.ppembytv.api.HttpClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 本地配置服务（对应参考项目的 ConfigServer）：
 * 手机扫描电视屏幕上的二维码后，在浏览器中填写 Emby 服务器配置，
 * POST 到 /config，电视端收到后回调保存。
 */
class ConfigServer(
    port: Int,
    private val context: Context,
    private val currentConfig: ServerConfig?,
    private val onConfigReceived: (ServerConfig) -> Unit,
) : NanoHTTPD(port) {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return when {
            uri == "/" -> serveHtmlPage()
            uri == "/config" && session.method == NanoHTTPD.Method.POST -> handleConfigPost(session)
            else -> NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "Not Found"
            )
        }
    }

    private fun handleConfigPost(session: IHTTPSession): Response {
        return try {
            val body = mutableMapOf<String, String>()
            session.parseBody(body)
            val raw = body["postData"] ?: return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "No data"
            )
            Log.d(TAG, "Received config data: $raw")
            val data = HttpClients.json().decodeFromString<ConfigData>(raw)
            val config = ServerConfig(
                id = java.util.UUID.randomUUID().toString(),
                alias = data.alias.ifBlank { data.host },
                protocol = data.protocol.ifBlank { "http" },
                host = data.host,
                port = data.port,
                path = data.path.orEmpty(),
                username = data.username,
                password = data.password,
                directOnly = data.directOnly,
                trustAllCerts = data.trustAllCerts,
                note = data.note.orEmpty(),
            )
            Log.d(TAG, "Parsed config: alias=${config.alias}, host=${config.host}, port=${config.port}")
            scope.launch { onConfigReceived(config) }
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain; charset=UTF-8", "OK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle config", e)
            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "text/plain; charset=UTF-8",
                "Error: ${e.message}"
            )
        }
    }

    private fun serveHtmlPage(): Response {
        val page = buildHtml()
        val resp = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html; charset=UTF-8", page)
        resp.addHeader("Content-Type", "text/html; charset=UTF-8")
        return resp
    }

    private fun buildHtml(): String {
        val alias = escapeHtml(currentConfig?.alias)
        val host = escapeHtml(currentConfig?.host)
        val port = currentConfig?.port ?: 8096
        val path = escapeHtml(currentConfig?.path)
        val username = escapeHtml(currentConfig?.username)
        val password = escapeHtml(currentConfig?.password)
        val httpSel = if (currentConfig?.protocol != "https") "selected" else ""
        val httpsSel = if (currentConfig?.protocol == "https") "selected" else ""
        val directChecked = if (currentConfig?.directOnly == true) "checked" else ""
        val trustChecked = if (currentConfig?.trustAllCerts == true) "checked" else ""

        return """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Emby 服务器配置</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
         min-height: 100vh; padding: 20px; }
  .container { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 16px;
               padding: 30px; box-shadow: 0 20px 60px rgba(0,0,0,0.4); }
  h1 { color: #333; margin-bottom: 10px; font-size: 24px; }
  .subtitle { color: #666; margin-bottom: 24px; font-size: 14px; }
  .form-group { margin-bottom: 18px; }
  label { display: block; margin-bottom: 6px; color: #555; font-weight: 500; font-size: 14px; }
  input, select { width: 100%; padding: 12px; border: 2px solid #e0e0e0; border-radius: 8px;
                  font-size: 15px; }
  input:focus, select:focus { outline: none; border-color: #2c5364; }
  .check-group { display: flex; align-items: center; gap: 8px; }
  .check-group input { width: auto; margin: 0; }
  .check-group label { margin: 0; cursor: pointer; }
  button { width: 100%; padding: 14px; background: #2c5364; color: #fff; border: none;
           border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 6px; }
  button:active { opacity: 0.85; }
  .toast { position: fixed; top: 16px; left: 50%; transform: translateX(-50%);
           padding: 12px 24px; border-radius: 8px; color: #fff; font-size: 14px;
           display: none; z-index: 99; }
  .success { background: #27ae60; }
  .error { background: #e74c3c; }
  .danger { color: #e74c3c; }
</style>
</head>
<body>
<div class="container">
  <h1>📺 Emby 服务器配置</h1>
  <p class="subtitle">在手机上填写你的 Emby 服务器信息，点击同步后电视端自动保存</p>
  <div class="form-group">
    <label>服务器名称（别名）</label>
    <input type="text" id="alias" placeholder="如：我的家庭影院" value="$alias">
  </div>
  <div class="form-group">
    <label>协议</label>
    <select id="protocol">
      <option value="http" $httpSel>HTTP</option>
      <option value="https" $httpsSel>HTTPS</option>
    </select>
  </div>
  <div class="form-group">
    <label>服务器地址</label>
    <input type="text" id="host" placeholder="192.168.1.100 或 emby.example.com" value="$host" required>
  </div>
  <div class="form-group">
    <label>端口</label>
    <input type="number" id="port" value="$port" required>
  </div>
  <div class="form-group">
    <label>路径（可选）</label>
    <input type="text" id="path" placeholder="如 emby" value="$path">
  </div>
  <div class="form-group">
    <label>用户名</label>
    <input type="text" id="username" placeholder="您的Emby用户名" value="$username" required>
  </div>
  <div class="form-group">
    <label>密码</label>
    <input type="password" id="password" placeholder="您的Emby密码" value="$password" required>
  </div>
  <div class="form-group check-group">
    <input type="checkbox" id="directOnly" $directChecked>
    <label for="directOnly">仅直连（不使用代理）</label>
  </div>
  <div class="form-group check-group">
    <input type="checkbox" id="trustAllCerts" $trustChecked>
    <label for="trustAllCerts" class="danger">⚠️ 信任所有SSL证书（不安全）</label>
  </div>
  <button type="button" id="submitBtn">📺 同步到电视</button>
</div>
<div class="toast success" id="success">✅ 配置已成功发送到电视！</div>
<div class="toast error" id="error">❌ 发送失败，请重试</div>
<script>
  function parseUrl(input) {
    let url = input.trim();
    let protocol = null, port = null;
    const pm = url.match(/^(https?):\/\//i);
    if (pm) { protocol = pm[1].toLowerCase(); url = url.substring(pm[0].length); }
    url = url.replace(/[\/\\]+$/, '');
    const pm2 = url.match(/^(.*?):(\d+)$/);
    if (pm2) { port = parseInt(pm2[2]); url = pm2[1]; }
    return { host: url, protocol: protocol, port: port };
  }
  document.getElementById('host').addEventListener('input', (e) => {
    const parsed = parseUrl(e.target.value);
    if (parsed.host) {
      e.target.value = parsed.host;
      if (parsed.protocol) document.getElementById('protocol').value = parsed.protocol;
      if (parsed.port) document.getElementById('port').value = parsed.port;
    }
  });
  document.getElementById('submitBtn').addEventListener('click', async () => {
    const host = document.getElementById('host').value.trim();
    if (!host) { showToast('error', '请填写服务器地址'); return; }
    const payload = {
      alias: document.getElementById('alias').value.trim(),
      protocol: document.getElementById('protocol').value,
      host: host,
      port: parseInt(document.getElementById('port').value) || 8096,
      path: document.getElementById('path').value.trim(),
      username: document.getElementById('username').value.trim(),
      password: document.getElementById('password').value,
      directOnly: document.getElementById('directOnly').checked,
      trustAllCerts: document.getElementById('trustAllCerts').checked
    };
    try {
      const resp = await fetch('/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (resp.ok) showToast('success', '✅ 配置已成功发送到电视！');
      else showToast('error', '❌ 发送失败（' + resp.status + '），请重试');
    } catch (err) {
      showToast('error', '❌ 发送失败，请检查网络后重试');
    }
  });
  let toastTimer = null;
  function showToast(kind, msg) {
    const el = document.getElementById(kind);
    el.textContent = msg;
    el.style.display = 'block';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.style.display = 'none'; }, 3000);
  }
</script>
</body>
</html>"""
    }

    private fun escapeHtml(s: String?): String {
        if (s == null) return ""
        return s.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    companion object {
        private const val TAG = "ConfigServer"
    }
}

/** POST /config 的 JSON 载荷 */
@kotlinx.serialization.Serializable
data class ConfigData(
    val alias: String = "",
    val note: String? = null,
    val protocol: String = "http",
    val host: String = "",
    val port: Int = 8096,
    val path: String? = "",
    val username: String = "",
    val password: String = "",
    val directOnly: Boolean = false,
    val trustAllCerts: Boolean = false,
)
