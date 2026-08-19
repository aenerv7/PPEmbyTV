package magi.aenerv7.ppembytv.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.WebDavSyncConfig
import org.json.JSONObject
import java.util.Locale

/** 局域网 WebDAV 同步配置服务器：GET / 展示表单，POST /save 保存 WebDAV 配置。 */
internal class WebDavSyncConfigServer(
    port: Int,
    initialConfig: WebDavSyncConfig,
    private val onConfigReceived: (WebDavSyncConfig) -> Unit,
) : NanoHTTPD(port) {

    private var currentConfig: WebDavSyncConfig = initialConfig.normalized()

    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/") {
            return serveHtmlPage()
        }
        if (session.uri == "/save" && session.method == Method.POST) {
            return handleSave(session)
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            when (ch) {
                '\u000c' -> sb.append("\\f")
                '\r' -> sb.append("\\r")
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\t' -> sb.append("\\t")
                '\n' -> sb.append("\\n")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
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
            if (json == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Invalid payload")
            }
            val serverUrl = json.optString("serverUrl").trim()
            val username = json.optString("username").trim()
            val password = json.optString("password").trim()
            val syncServerConfigurations = json.optBoolean("syncServerConfigurations", true)
            val syncAppSettings = json.optBoolean("syncAppSettings", true)
            val config = WebDavSyncConfig(serverUrl, username, password, syncServerConfigurations, syncAppSettings).normalized()
            currentConfig = config
            onConfigReceived.invoke(config)
            val responseBody = """
                {
                  "success": true,
                  "serverUrl": "${escapeJson(config.serverUrl)}",
                  "username": "${escapeJson(config.username)}",
                  "password": "${escapeJson(config.password)}",
                  "syncServerConfigurations": ${config.syncServerConfigurations},
                  "syncAppSettings": ${config.syncAppSettings}
                }
            """.trimIndent()
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", responseBody)
        } catch (e: Exception) {
            Log.e("WebDavSyncCfgServer", "保存 WebDAV 配置失败", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtmlPage(): Response {
        val escapedServerUrl = escapeHtmlAttribute(currentConfig.serverUrl)
        val escapedUsername = escapeHtmlAttribute(currentConfig.username)
        val escapedPassword = escapeHtmlAttribute(currentConfig.password)
        val syncServersClass = if (currentConfig.syncServerConfigurations) "on" else ""
        val syncSettingsClass = if (currentConfig.syncAppSettings) "on" else ""
        val syncServersValue = currentConfig.syncServerConfigurations.toString().lowercase(Locale.ROOT)
        val syncSettingsValue = currentConfig.syncAppSettings.toString().lowercase(Locale.ROOT)
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>WebDAV 同步</title>
              <style>
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  min-height: 100vh;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  background: linear-gradient(180deg, #0f172a 0%, #111827 100%);
                  color: #f8fafc;
                  padding: 24px 16px 32px;
                }
                .card {
                  max-width: 560px;
                  margin: 0 auto;
                  background: rgba(15, 23, 42, 0.9);
                  border: 1px solid rgba(148, 163, 184, 0.18);
                  border-radius: 20px;
                  padding: 22px 18px 20px;
                  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.35);
                }
                h1 { font-size: 22px; margin: 0 0 8px; }
                p { color: #cbd5e1; font-size: 14px; margin: 0 0 18px; line-height: 1.6; }
                .hint {
                  padding: 12px 14px;
                  border-radius: 12px;
                  background: rgba(30, 41, 59, 0.95);
                  border: 1px solid rgba(148, 163, 184, 0.16);
                  margin-bottom: 18px;
                  font-size: 13px;
                  line-height: 1.7;
                  color: #dbeafe;
                }
                label {
                  display: block;
                  margin-bottom: 6px;
                  color: #e2e8f0;
                  font-size: 14px;
                }
                .field { margin-bottom: 14px; }
                input {
                  width: 100%;
                  padding: 10px 14px;
                  border-radius: 12px;
                  border: 1px solid rgba(148, 163, 184, 0.22);
                  background: rgba(15, 23, 42, 0.85);
                  color: #f8fafc;
                  font-size: 15px;
                  outline: none;
                }
                input:focus { border-color: #60a5fa; }
                .switch-row {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 12px;
                  padding: 12px 14px;
                  border-radius: 12px;
                  background: rgba(30, 41, 59, 0.95);
                  border: 1px solid rgba(148, 163, 184, 0.16);
                  margin-bottom: 10px;
                }
                .switch-copy {
                  display: flex;
                  flex-direction: column;
                  gap: 4px;
                }
                .switch-copy strong { font-size: 15px; }
                .switch-copy span { font-size: 12px; color: #cbd5e1; line-height: 1.5; }
                .switch {
                  flex: 0 0 52px;
                  width: 52px;
                  min-width: 52px;
                  height: 30px;
                  border-radius: 15px;
                  background: #475569;
                  border: none;
                  padding: 4px;
                  overflow: hidden;
                  appearance: none;
                  -webkit-appearance: none;
                  display: flex;
                  align-items: center;
                  justify-content: flex-start;
                  cursor: pointer;
                }
                .switch::after {
                  content: "";
                  width: 22px;
                  height: 22px;
                  border-radius: 50%;
                  background: white;
                  display: block;
                  transition: transform 0.2s ease;
                }
                .switch.on {
                  background: #22c55e;
                  justify-content: flex-end;
                }
                .switch:focus-visible {
                  outline: 2px solid rgba(255, 255, 255, 0.75);
                  outline-offset: 2px;
                }
                button.primary {
                  width: 100%;
                  margin-top: 16px;
                  padding: 14px 16px;
                  border: none;
                  border-radius: 12px;
                  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
                  color: #f8fafc;
                  font-size: 15px;
                  font-weight: 600;
                }
                .msg { margin-top: 12px; min-height: 20px; color: #bae6fd; font-size: 13px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>WebDAV 同步</h1>
                <p>在手机上填写 WebDAV 信息并提交，电视端会立即回填配置。固定使用目录 <strong>PPEmbyTV</strong>，同步文件名为 <strong>sync-config.json</strong>。</p>

                <div class="field">
                  <label for="serverUrl">服务器地址</label>
                  <input id="serverUrl" type="text" value="${escapedServerUrl}" placeholder="例如：https://dav.example.com/dav/" />
                </div>

                <div class="field">
                  <label for="username">账号</label>
                  <input id="username" type="text" value="${escapedUsername}" placeholder="请输入 WebDAV 账号" />
                </div>

                <div class="field">
                  <label for="password">密码</label>
                  <input id="password" type="password" value="${escapedPassword}" placeholder="请输入 WebDAV 密码" />
                </div>

                <div class="switch-row" onclick="toggle('syncServers')">
                  <div class="switch-copy">
                    <strong>同步服务器配置</strong>
                    <span>包含服务器地址、账号、密码、Token、备用线路和最后使用服务器。</span>
                  </div>
                  <button id="syncServersBtn" type="button" class="switch ${syncServersClass}"></button>
                </div>

                <div class="switch-row" onclick="toggle('syncSettings')">
                  <div class="switch-copy">
                    <strong>同步应用设置</strong>
                    <span>包含弹幕源配置、代理、在线字幕、图标库 URL、主题、缓冲、DLNA、排序和 Trakt 基础配置；不包含字幕字体文件、播放器相关偏好和 Trakt 授权 Token。</span>
                  </div>
                  <button id="syncSettingsBtn" type="button" class="switch ${syncSettingsClass}"></button>
                </div>

                <button class="primary" type="button" onclick="submitForm()">保存到电视</button>
                <div id="msg" class="msg"></div>
              </div>

              <script>
                let syncServers = ${syncServersValue};
                let syncSettings = ${syncSettingsValue};

                function renderSwitches() {
                  document.getElementById('syncServersBtn').className = 'switch' + (syncServers ? ' on' : '');
                  document.getElementById('syncSettingsBtn').className = 'switch' + (syncSettings ? ' on' : '');
                }

                function toggle(key) {
                  if (key === 'syncServers') {
                    syncServers = !syncServers;
                  } else if (key === 'syncSettings') {
                    syncSettings = !syncSettings;
                  }
                  renderSwitches();
                }

                async function submitForm() {
                  const msg = document.getElementById('msg');
                  msg.textContent = '提交中...';

                  const payload = {
                    serverUrl: document.getElementById('serverUrl').value.trim(),
                    username: document.getElementById('username').value.trim(),
                    password: document.getElementById('password').value.trim(),
                    syncServerConfigurations: syncServers,
                    syncAppSettings: syncSettings
                  };

                  try {
                    const res = await fetch('/save', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                      body: JSON.stringify(payload)
                    });
                    if (!res.ok) {
                      const text = await res.text();
                      msg.textContent = '提交失败：' + text;
                      return;
                    }
                    const savedConfig = await res.json();
                    document.getElementById('serverUrl').value = savedConfig.serverUrl || '';
                    document.getElementById('username').value = savedConfig.username || '';
                    document.getElementById('password').value = savedConfig.password || '';
                    syncServers = !!savedConfig.syncServerConfigurations;
                    syncSettings = !!savedConfig.syncAppSettings;
                    renderSwitches();
                    msg.textContent = '保存成功，电视端已更新为最新 WebDAV 配置';
                  } catch (e) {
                    msg.textContent = '提交失败：' + (e && e.message ? e.message : '未知错误');
                  }
                }

                renderSwitches();
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
}
