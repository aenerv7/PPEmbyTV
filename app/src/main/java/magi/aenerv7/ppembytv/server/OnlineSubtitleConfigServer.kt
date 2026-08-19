package magi.aenerv7.ppembytv.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.AssrtApiProtocol
import magi.aenerv7.ppembytv.data.OnlineSubtitleConfig
import org.json.JSONObject

/** 局域网在线字幕配置服务器：GET / 展示表单，POST /save 保存 API Key。 */
internal class OnlineSubtitleConfigServer(
    port: Int,
    private val initialConfig: OnlineSubtitleConfig,
    private val onConfigReceived: (OnlineSubtitleConfig) -> Unit,
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
            val apiKey = json?.optString("apiKey")?.trim() ?: ""
            val enabled = json?.optBoolean("enabled", true) ?: true
            val protocol = AssrtApiProtocol.parse(json?.optString("protocol"))
            onConfigReceived.invoke(OnlineSubtitleConfig(enabled, apiKey, protocol))
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", "{\"success\":true}")
        } catch (e: Exception) {
            Log.e("OnlineSubCfgServer", "保存 API key 失败", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtml(): Response {
        val escapedToken = escapeHtmlAttribute(initialConfig.assrtApiToken)
        val enabledChecked = if (initialConfig.enabled) "checked" else ""
        val httpsChecked = if (initialConfig.assrtApiProtocol == AssrtApiProtocol.HTTPS) "checked" else ""
        val httpChecked = if (initialConfig.assrtApiProtocol == AssrtApiProtocol.HTTP) "checked" else ""
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>在线字幕配置</title>
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
                input[type="text"] {
                  width: 100%;
                  padding: 12px 14px;
                  border: 1px solid rgba(148, 163, 184, 0.45);
                  border-radius: 10px;
                  background: #0b1220;
                  color: #fff;
                  outline: none;
                  font-size: 15px;
                }
                input[type="text"]:focus { border-color: #38bdf8; }
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
                  padding: 12px 14px;
                  display: flex;
                  align-items: center;
                  gap: 10px;
                  cursor: pointer;
                }
                .protocol-option input[type="radio"] {
                  accent-color: #0284c7;
                }
                .protocol-hint {
                  color: #94a3b8;
                  font-size: 12px;
                  margin-bottom: 8px;
                  line-height: 1.5;
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
                <h1>在线字幕配置</h1>
                <p>字幕服务由 assrt.net 提供。可直接启用并使用内置 Key；若你有自己的 API Key，填写后会优先使用你的配置。点击保存后会自动同步到电视。</p>
                <input id="apiKey" type="text" placeholder="例如：xxxxxxxxxxxxxxxx" value="${escapedToken}" />
                <div class="protocol-title">ASSRT 接口协议</div>
                <div class="protocol-row">
                  <label class="protocol-option">
                    <input type="radio" name="protocol" value="https" ${httpsChecked} />
                    <span>HTTPS（默认）</span>
                  </label>
                  <label class="protocol-option">
                    <input type="radio" name="protocol" value="http" ${httpChecked} />
                    <span>HTTP（兼容老电视证书问题）</span>
                  </label>
                </div>
                <div class="protocol-hint">推荐优先使用 HTTPS；如果老电视提示 SSL、证书链或握手失败，再切换到 HTTP。</div>
                <label class="switch-row">
                  <span>启用在线字幕</span>
                  <input id="enabled" type="checkbox" ${enabledChecked} />
                </label>
                <button onclick="saveApiKey()">保存到电视</button>
                <div id="msg" class="msg"></div>
              </div>
              <script>
                async function saveApiKey() {
                  const key = document.getElementById('apiKey').value.trim();
                  const enabled = document.getElementById('enabled').checked;
                  const protocol = document.querySelector('input[name="protocol"]:checked')?.value || 'https';
                  const msg = document.getElementById('msg');
                  msg.textContent = '提交中...';
                  try {
                    const res = await fetch('/save', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                      body: JSON.stringify({ apiKey: key, enabled: enabled, protocol: protocol })
                    });
                    if (!res.ok) {
                      const text = await res.text();
                      msg.textContent = '提交失败：' + text;
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
}
