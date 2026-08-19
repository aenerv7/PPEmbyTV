package magi.aenerv7.ppembytv.server

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.model.BackupRouteConfig
import magi.aenerv7.ppembytv.data.model.normalizeBackupRouteConfig
import java.util.Locale
import java.util.UUID

/** 局域网备用线路管理服务器：GET / 展示表单，POST /save 保存线路列表。 */
internal class BackupRouteConfigServer(
    port: Int,
    serverAlias: String,
    initialRoutes: List<BackupRouteConfig>,
    private val onConfigReceived: (List<BackupRouteConfig>) -> Unit,
) : NanoHTTPD(port) {

    private val gson = Gson()
    private val escapedServerAlias: String = escapeHtml(serverAlias)
    private val initialRoutesJson: String = gson.toJson(initialRoutes).replace("</", "<\\/")

    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/") {
            return serveHtml()
        }
        if (session.uri == "/save" && session.method == Method.POST) {
            return handleSave(session)
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun handleSave(session: IHTTPSession): Response {
        return try {
            val params = HashMap<String, String>()
            session.parseBody(params)
            val postData = params["postData"] ?: ""
            if (postData.isBlank()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Missing body")
            }
            val payload = gson.fromJson<BackupRoutePayload>(postData, object : TypeToken<BackupRoutePayload>() {}.type)
                ?: throw IllegalArgumentException("Invalid payload")
            val normalizedRoutes = mutableListOf<BackupRouteConfig>()
            payload.routes.take(MAX_ROUTES).forEachIndexed { index, item ->
                val alias = item.alias.trim()
                val host = item.host.trim()
                val port = item.port
                if (alias.isEmpty()) {
                    throw IllegalArgumentException("第 ${index + 1} 条线路缺少服务器别名")
                }
                if (host.isEmpty()) {
                    throw IllegalArgumentException("第 ${index + 1} 条线路缺少服务器地址")
                }
                if (port < 1 || port >= 65536) {
                    throw IllegalArgumentException("第 ${index + 1} 条线路端口无效")
                }
                val id = item.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val protocol = if (item.protocol.lowercase(Locale.ROOT) == "https") "https" else "http"
                normalizedRoutes.add(
                    normalizeBackupRouteConfig(
                        BackupRouteConfig(
                            id = id,
                            alias = alias,
                            protocol = protocol,
                            host = host,
                            port = port,
                            path = item.path.trim(),
                            directOnly = item.directOnly,
                        )
                    )
                )
            }
            onConfigReceived.invoke(normalizedRoutes)
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", "{\"success\":true}")
        } catch (e: Exception) {
            Log.e("BackupRouteCfgServer", "保存备用线路失败", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", e.message ?: "Invalid payload")
        }
    }

    private fun serveHtml(): Response {
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>备用线路管理</title>
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  min-height: 100vh;
                  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
                  color: #fff;
                  padding: 18px;
                }
                .wrap {
                  max-width: 920px;
                  margin: 0 auto;
                  background: rgba(15, 23, 42, 0.88);
                  border: 1px solid rgba(148, 163, 184, 0.22);
                  border-radius: 18px;
                  padding: 20px;
                  box-shadow: 0 20px 50px rgba(2, 6, 23, 0.45);
                }
                h1 { font-size: 24px; margin-bottom: 8px; }
                .sub {
                  color: #cbd5e1;
                  font-size: 14px;
                  line-height: 1.7;
                  margin-bottom: 16px;
                }
                .toolbar {
                  display: flex;
                  align-items: center;
                  gap: 12px;
                  margin-bottom: 14px;
                  flex-wrap: wrap;
                }
                .pill {
                  padding: 8px 12px;
                  border-radius: 999px;
                  background: rgba(255,255,255,0.08);
                  color: #e2e8f0;
                  font-size: 13px;
                }
                button {
                  border: none;
                  border-radius: 12px;
                  background: #2563eb;
                  color: white;
                  padding: 11px 16px;
                  font-size: 15px;
                  font-weight: 600;
                  cursor: pointer;
                }
                button.secondary { background: rgba(255,255,255,0.12); }
                button.danger { background: #b91c1c; }
                .entry-list {
                  display: grid;
                  gap: 14px;
                }
                .entry {
                  border-radius: 14px;
                  border: 1px solid rgba(148, 163, 184, 0.22);
                  background: rgba(15, 23, 42, 0.76);
                  padding: 14px;
                }
                .entry-head {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 10px;
                  margin-bottom: 12px;
                  flex-wrap: wrap;
                }
                .entry-title {
                  font-size: 16px;
                  font-weight: 700;
                }
                .grid {
                  display: grid;
                  grid-template-columns: 1fr;
                  gap: 10px;
                }
                .field { display: flex; flex-direction: column; gap: 6px; }
                .grid-split {
                  display: grid;
                  grid-template-columns: 132px 1fr;
                  gap: 10px;
                }
                label {
                  color: #cbd5e1;
                  font-size: 13px;
                  font-weight: 600;
                }
                input, select {
                  width: 100%;
                  padding: 11px 12px;
                  border-radius: 10px;
                  border: 1px solid rgba(148, 163, 184, 0.32);
                  background: #0b1220;
                  color: #fff;
                  font-size: 14px;
                  outline: none;
                }
                input:focus, select:focus { border-color: #60a5fa; }
                .switch {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 12px;
                  padding: 12px;
                  border-radius: 10px;
                  background: rgba(255,255,255,0.04);
                }
                .switch input {
                  width: 18px;
                  height: 18px;
                  accent-color: #2563eb;
                }
                .msg {
                  min-height: 20px;
                  margin-top: 14px;
                  font-size: 13px;
                  line-height: 1.5;
                  padding: 10px 12px;
                  border-radius: 12px;
                  background: rgba(255,255,255,0.04);
                  color: #cbd5e1;
                  display: none;
                }
                .msg.show { display: block; }
                .msg.success {
                  color: #bbf7d0;
                  background: rgba(34, 197, 94, 0.14);
                  border: 1px solid rgba(34, 197, 94, 0.28);
                }
                .msg.error {
                  color: #fecaca;
                  background: rgba(239, 68, 68, 0.14);
                  border: 1px solid rgba(239, 68, 68, 0.28);
                }
                .msg.info {
                  color: #bfdbfe;
                  background: rgba(37, 99, 235, 0.14);
                  border: 1px solid rgba(37, 99, 235, 0.28);
                }
                .bottom-actions {
                  margin-top: 16px;
                  display: flex;
                  justify-content: flex-end;
                }
                @media (max-width: 720px) {
                  .grid-split { grid-template-columns: 1fr; }
                  .bottom-actions { justify-content: stretch; }
                  .bottom-actions button { width: 100%; }
                }
              </style>
            </head>
            <body>
              <div class="wrap">
                <h1>备用线路管理</h1>
                <p class="sub">当前服务器：${escapedServerAlias}。可维护最多 50 条备用线路。这里只保存线路信息，不会立刻切换电视当前使用的主线路。</p>

                <div class="toolbar">
                  <div class="pill">已配置 <span id="routeCount">0</span> / 50 条</div>
                </div>

                <div id="routeList" class="entry-list"></div>
                <div id="msg" class="msg"></div>
                <div class="bottom-actions">
                  <button type="button" onclick="saveRoutes()">保存到电视</button>
                </div>
              </div>

              <script>
                const MAX_ROUTES = 50;
                let routes = ${initialRoutesJson};

                function escapeHtml(value) {
                  return String(value || '')
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;');
                }

                function routeTemplate(route, index) {
                  const alias = escapeHtml(route.alias || '');
                  const protocol = escapeHtml(route.protocol || 'http');
                  const host = escapeHtml(route.host || '');
                  const port = Number.isFinite(route.port) ? route.port : 443;
                  const path = escapeHtml(route.path || '');
                  return `
                    <div class="entry">
                      <div class="entry-head">
                        <div class="entry-title">备用线路 ${'$'}{index + 1}</div>
                        <button type="button" class="danger" onclick="removeRoute(${'$'}{index})">删除</button>
                      </div>
                      <div class="grid">
                        <div class="field">
                          <label>服务器别名</label>
                          <input type="text" value="${'$'}{alias}" placeholder="例如：家庭宽带入口" oninput="updateRoute(${'$'}{index}, 'alias', this.value)" />
                        </div>
                        <div class="field">
                          <label>协议</label>
                          <select onchange="updateRoute(${'$'}{index}, 'protocol', this.value)">
                            <option value="http" ${'$'}{protocol === 'http' ? 'selected' : ''}>HTTP</option>
                            <option value="https" ${'$'}{protocol === 'https' ? 'selected' : ''}>HTTPS</option>
                          </select>
                        </div>
                        <div class="field">
                          <label>服务器地址</label>
                          <input type="text" value="${'$'}{host}" placeholder="例如：emby.example.com" oninput="updateRoute(${'$'}{index}, 'host', this.value, this)" />
                        </div>
                        <div class="grid-split">
                          <div class="field">
                            <label>端口</label>
                            <input type="text" inputmode="numeric" pattern="[0-9]*" value="${'$'}{port}" placeholder="443" oninput="updateRoute(${'$'}{index}, 'port', this.value, this)" />
                          </div>
                          <div class="field">
                            <label>路径（可选）</label>
                            <input type="text" value="${'$'}{path}" placeholder="如 emby 或 media" oninput="updateRoute(${'$'}{index}, 'path', this.value)" />
                          </div>
                        </div>
                        <div class="field">
                          <div class="switch">
                            <label>仅直连</label>
                            <input type="checkbox" ${'$'}{route.directOnly ? 'checked' : ''} onchange="updateRoute(${'$'}{index}, 'directOnly', this.checked)" />
                          </div>
                        </div>
                      </div>
                    </div>
                  `;
                }

                function renderRoutes() {
                  const routeList = document.getElementById('routeList');
                  const count = document.getElementById('routeCount');
                  count.textContent = routes.length;
                  const emptyState = !routes.length
                    ? '<div class="entry"><div class="entry-title" style="margin-bottom:8px;">还没有备用线路</div><div class="sub" style="margin:0; color:#cbd5e1;">点击下方“新增线路”，或者直接在手机里批量录入后保存到电视。</div></div>'
                    : '';
                  const addButton = '<button type="button" class="secondary" style="width:100%; margin-top:2px;" onclick="addRoute()">+ 新增线路</button>';
                  routeList.innerHTML = emptyState + routes.map((route, index) => routeTemplate(route, index)).join('') + addButton;
                }

                function setMessage(text, type) {
                  const msg = document.getElementById('msg');
                  msg.textContent = text || '';
                  msg.className = text ? `msg show ${'$'}{type || 'info'}` : 'msg';
                }

                function normalizeHostInput(index, rawValue) {
                  const trimmed = String(rawValue || '').trim();
                  if (!trimmed) {
                    routes[index].host = '';
                    return;
                  }

                  if (/^https?:\/\//i.test(trimmed)) {
                    try {
                      const parsed = new URL(trimmed);
                      routes[index].protocol = parsed.protocol === 'https:' ? 'https' : 'http';
                      routes[index].host = parsed.hostname || '';
                      routes[index].port = parsed.port ? parseInt(parsed.port, 10) || routes[index].port : routes[index].port;
                      routes[index].path = parsed.pathname && parsed.pathname !== '/' ? parsed.pathname.replace(/^\/+/, '') : routes[index].path;
                      return;
                    } catch (e) {
                      routes[index].protocol = trimmed.toLowerCase().startsWith('https://') ? 'https' : 'http';
                      routes[index].host = trimmed.replace(/^https?:\/\//i, '').trim();
                      return;
                    }
                  }

                  routes[index].host = trimmed;
                }

                function addRoute() {
                  if (routes.length >= MAX_ROUTES) {
                    setMessage('最多只能配置 50 条备用线路', 'error');
                    return;
                  }
                  routes.push({
                    id: crypto.randomUUID ? crypto.randomUUID() : `route-${'$'}{Date.now()}-${'$'}{routes.length}`,
                    alias: `备用线路${'$'}{routes.length + 1}`,
                    protocol: 'https',
                    host: '',
                    port: 443,
                    path: '',
                    directOnly: true
                  });
                  setMessage('', '');
                  renderRoutes();
                }

                function removeRoute(index) {
                  routes.splice(index, 1);
                  setMessage('', '');
                  renderRoutes();
                }

                function updateRoute(index, field, value, element) {
                  if (field === 'port') {
                    const digitsOnly = String(value || '').replace(/\D/g, '');
                    routes[index][field] = digitsOnly === '' ? 443 : parseInt(digitsOnly, 10) || 443;
                    if (element) element.value = digitsOnly;
                    return;
                  }
                  if (field === 'host') {
                    const hasScheme = /^https?:\/\//i.test(String(value || '').trim());
                    normalizeHostInput(index, value);
                    if (element) element.value = routes[index].host || '';
                    if (hasScheme) renderRoutes();
                    return;
                  }
                  routes[index][field] = typeof value === 'string' ? value.trim() : value;
                }

                function validateRoutes() {
                  for (let i = 0; i < routes.length; i++) {
                    const route = routes[i];
                    if (!String(route.alias || '').trim()) return `第 ${'$'}{i + 1} 条线路缺少服务器别名`;
                    if (!String(route.host || '').trim()) return `第 ${'$'}{i + 1} 条线路缺少服务器地址`;
                    const port = parseInt(route.port, 10);
                    if (!Number.isFinite(port) || port <= 0 || port > 65535) return `第 ${'$'}{i + 1} 条线路端口无效`;
                  }
                  return '';
                }

                async function saveRoutes() {
                  const validationError = validateRoutes();
                  if (validationError) {
                    setMessage(validationError, 'error');
                    return;
                  }
                  setMessage('正在提交到电视，请稍候...', 'info');
                  try {
                    const res = await fetch('/save', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                      body: JSON.stringify({ routes })
                    });
                    if (!res.ok) {
                      const text = await res.text();
                      setMessage('提交失败：' + text, 'error');
                      return;
                    }
                    setMessage('保存成功，线路信息已同步到电视。现在可以返回电视继续操作。', 'success');
                  } catch (e) {
                    setMessage('提交失败：' + (e && e.message ? e.message : '未知错误'), 'error');
                  }
                }

                renderRoutes();
              </script>
            </body>
            </html>
        """.trimIndent()
        val response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html)
        response.addHeader("Content-Type", "text/html; charset=UTF-8")
        return response
    }

    private data class BackupRoutePayload(val routes: List<BackupRouteItemPayload> = emptyList())

    private data class BackupRouteItemPayload(
        val id: String? = null,
        val alias: String = "",
        val protocol: String = "http",
        val host: String = "",
        val port: Int = 8096,
        val path: String = "",
        val directOnly: Boolean = false,
    )

    private companion object {
        const val MAX_ROUTES = 50
    }
}
