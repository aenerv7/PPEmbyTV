package magi.aenerv7.ppembytv.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.ProxyConfig
import magi.aenerv7.ppembytv.data.ProxyProtocol
import org.json.JSONObject

/** 局域网代理配置服务器：GET / 展示表单，POST /save 保存代理配置。 */
internal class ProxyConfigServer(
    port: Int,
    private val initialConfig: ProxyConfig,
    private val onConfigReceived: (ProxyConfig) -> Unit,
) : NanoHTTPD(port) {

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
            val json = try {
                JSONObject(postData)
            } catch (t: Throwable) {
                null
            }
            val enabled = json?.optBoolean("enabled", false) ?: false
            var protocolName = "HTTP"
            if (json != null) {
                try {
                    protocolName = json.optString("protocol", "HTTP")
                } catch (e: Exception) {
                    protocolName = "HTTP"
                }
            }
            val protocol = ProxyProtocol.valueOf(protocolName)
            val host = json?.optString("host", "")?.trim() ?: ""
            val port = json?.optInt("port", 7890) ?: 7890
            val username = json?.optString("username", "")?.trim() ?: ""
            val password = json?.optString("password", "") ?: ""
            onConfigReceived.invoke(
                ProxyConfig(
                    enabled = enabled,
                    protocol = protocol,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    bypassLan = json?.optBoolean("bypassLan", true) ?: true,
                )
            )
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", "{\"success\":true}")
        } catch (e: Exception) {
            Log.e(TAG, "保存代理配置失败", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtml(): Response {
        val escapedHost = escapeHtmlAttribute(initialConfig.host)
        val escapedUsername = escapeHtmlAttribute(initialConfig.username)
        val escapedPassword = escapeHtmlAttribute(initialConfig.password)
        val enabledChecked = if (initialConfig.enabled) "checked" else ""
        val bypassLanChecked = if (initialConfig.bypassLan) "checked" else ""
        val httpChecked = if (initialConfig.protocol == ProxyProtocol.HTTP) "checked" else ""
        val httpsChecked = if (initialConfig.protocol == ProxyProtocol.HTTPS) "checked" else ""
        val socks5Checked = if (initialConfig.protocol == ProxyProtocol.SOCKS5) "checked" else ""
        val port = initialConfig.port
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>代理配置</title>
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  min-height: 100vh;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
                  color: #fff;
                  padding: 20px;
                }
                .card {
                  width: 100%;
                  max-width: 520px;
                  background: rgba(15, 23, 42, 0.85);
                  border: 1px solid rgba(148, 163, 184, 0.25);
                  border-radius: 14px;
                  padding: 24px;
                  box-shadow: 0 20px 50px rgba(2, 6, 23, 0.5);
                }
                h1 { font-size: 22px; margin-bottom: 8px; }
                p { color: #cbd5e1; font-size: 14px; margin-bottom: 18px; line-height: 1.6; }
                label.field { display: block; color: #cbd5e1; font-size: 13px; margin: 10px 0 4px; }
                input[type="text"], input[type="password"] {
                  width: 100%;
                  padding: 12px 14px;
                  border: 1px solid rgba(148, 163, 184, 0.45);
                  border-radius: 10px;
                  background: #0b1220;
                  color: #fff;
                  outline: none;
                  font-size: 15px;
                }
                input[type="text"]:focus, input[type="password"]:focus { border-color: #38bdf8; }
                .row2 { display: flex; gap: 10px; }
                .row2 > * { flex: 1; }
                .switch-row {
                  margin: 12px 0 4px;
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 12px;
                  color: #e2e8f0;
                  font-size: 14px;
                }
                .switch-row input[type="checkbox"] {
                  width: 18px;
                  height: 18px;
                  accent-color: #0284c7;
                }
                .protocol-title {
                  margin-top: 16px;
                  margin-bottom: 8px;
                  color: #cbd5e1;
                  font-size: 14px;
                }
                .protocol-row {
                  display: flex;
                  gap: 10px;
                  margin-bottom: 6px;
                }
                .protocol-option {
                  flex: 1;
                  border: 1px solid rgba(148, 163, 184, 0.45);
                  border-radius: 10px;
                  background: #0b1220;
                  color: #fff;
                  padding: 8px 6px;
                  display: flex;
                  align-items: center;
                  gap: 6px;
                  cursor: pointer;
                  font-size: 13px;
                }
                .protocol-option input[type="radio"] {
                  accent-color: #0284c7;
                  width: 14px;
                  height: 14px;
                }
                button {
                  margin-top: 14px;
                  width: 100%;
                  padding: 12px 14px;
                  border: none;
                  border-radius: 10px;
                  background: #0284c7;
                  color: #fff;
                  font-size: 15px;
                  font-weight: 600;
                  cursor: pointer;
                }
                .msg { margin-top: 12px; font-size: 13px; color: #bae6fd; min-height: 20px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>代理配置</h1>
                <p>在手机上填写代理信息后点击保存，配置会自动同步到电视。</p>

                <label class="switch-row">
                  <span>启用代理</span>
                  <input id="enabled" type="checkbox" ${enabledChecked} />
                </label>

                <div class="protocol-title">代理协议</div>
                <div class="protocol-row">
                  <label class="protocol-option">
                    <input type="radio" name="protocol" value="HTTP" ${httpChecked} />
                    <span>HTTP</span>
                  </label>
                  <label class="protocol-option">
                    <input type="radio" name="protocol" value="HTTPS" ${httpsChecked} />
                    <span>HTTPS</span>
                  </label>
                  <label class="protocol-option">
                    <input type="radio" name="protocol" value="SOCKS5" ${socks5Checked} />
                    <span>SOCKS5</span>
                  </label>
                </div>

                <div class="row2">
                  <div>
                    <label class="field">代理服务器</label>
                    <input id="host" type="text" placeholder="例如：192.168.1.100" value="${escapedHost}" />
                  </div>
                  <div>
                    <label class="field">端口</label>
                    <input id="port" type="text" placeholder="例如：7890" value="${port}" />
                  </div>
                </div>

                <div class="row2">
                  <div>
                    <label class="field">用户名（可选）</label>
                    <input id="username" type="text" placeholder="无认证留空" value="${escapedUsername}" />
                  </div>
                  <div>
                    <label class="field">密码（可选）</label>
                    <input id="password" type="password" placeholder="无认证留空" value="${escapedPassword}" />
                  </div>
                </div>

                <label class="switch-row">
                  <span>局域网不代理</span>
                  <input id="bypassLan" type="checkbox" ${bypassLanChecked} />
                </label>

                <button onclick="saveConfig()">保存到电视</button>
                <div id="msg" class="msg"></div>
              </div>
              <script>
                async function saveConfig() {
                  const msg = document.getElementById('msg');
                  const port = parseInt(document.getElementById('port').value.trim(), 10);
                  if (!port || port < 1 || port > 65535) {
                    msg.textContent = '请输入有效的端口号（1-65535）';
                    return;
                  }
                  msg.textContent = '提交中...';
                  try {
                    const res = await fetch('/save', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                      body: JSON.stringify({
                        enabled: document.getElementById('enabled').checked,
                        protocol: document.querySelector('input[name="protocol"]:checked')?.value || 'HTTP',
                        host: document.getElementById('host').value.trim(),
                        port: port,
                        username: document.getElementById('username').value.trim(),
                        password: document.getElementById('password').value,
                        bypassLan: document.getElementById('bypassLan').checked
                      })
                    });
                    if (!res.ok) {
                      msg.textContent = '提交失败：' + (await res.text());
                      return;
                    }
                    msg.textContent = '保存成功，请返回电视确认';
                  } catch (e) {
                    msg.textContent = '提交失败：' + (e && e.message ? e.message : '未知错误');
                  }
                }
              </script>
            </body>
            </html>
        """.trimIndent()
        val response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html)
        response.addHeader("Content-Type", "text/html; charset=UTF-8")
        return response
    }

    private fun escapeHtmlAttribute(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private companion object {
        const val TAG = "ProxyCfgServerInner"
    }
}
