package magi.aenerv7.ppembytv.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/** 局域网图标库 URL 输入服务器：GET / 展示表单，POST /save 保存图标库 JSON 地址。 */
internal class ServerIconLibraryInputServer(
    port: Int,
    initialUrl: String,
    private val onUrlReceived: (String) -> Unit,
) : NanoHTTPD(port) {

    private val escapedInitialUrl: String = initialUrl
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

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
            val url = json?.optString("url")?.trim() ?: ""
            if (url.isBlank()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Missing url")
            }
            onUrlReceived.invoke(url)
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=UTF-8", "{\"success\":true}")
        } catch (e: Exception) {
            Log.e("ServerIconInputSrv", "保存图标库 URL 失败", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtml(): Response {
        val html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>图标库 URL</title>
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  min-height: 100vh;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  background: linear-gradient(135deg, #111827 0%, #1f2937 100%);
                  color: #fff;
                  padding: 20px;
                }
                .card {
                  width: 100%;
                  max-width: 560px;
                  background: rgba(17, 24, 39, 0.92);
                  border: 1px solid rgba(148, 163, 184, 0.22);
                  border-radius: 16px;
                  padding: 24px;
                  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
                }
                h1 { font-size: 24px; margin-bottom: 10px; }
                p { color: #cbd5e1; font-size: 14px; line-height: 1.6; margin-bottom: 16px; }
                input {
                  width: 100%;
                  padding: 13px 14px;
                  border-radius: 10px;
                  border: 1px solid rgba(148, 163, 184, 0.45);
                  background: #0f172a;
                  color: #fff;
                  font-size: 15px;
                  outline: none;
                }
                input:focus { border-color: #38bdf8; }
                button {
                  width: 100%;
                  margin-top: 16px;
                  padding: 13px 14px;
                  border: none;
                  border-radius: 10px;
                  background: #0284c7;
                  color: #fff;
                  font-size: 15px;
                  font-weight: 600;
                  cursor: pointer;
                }
                .msg { margin-top: 14px; min-height: 20px; color: #bae6fd; font-size: 13px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>图标库 URL</h1>
                <p>输入图标库 JSON 地址并保存，电视会立即加载图标列表。支持直接粘贴 http/https 链接。</p>
                <input id="url" type="text" value="${escapedInitialUrl}" placeholder="请输入图标库 URL" />
                <button onclick="saveUrl()">保存到电视</button>
                <div id="msg" class="msg"></div>
              </div>
              <script>
                async function saveUrl() {
                  const url = document.getElementById('url').value.trim();
                  const msg = document.getElementById('msg');
                  if (!url) {
                    msg.textContent = '请输入图标库 URL';
                    return;
                  }
                  msg.textContent = '提交中...';
                  try {
                    const res = await fetch('/save', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                      body: JSON.stringify({ url: url })
                    });
                    if (!res.ok) {
                      const text = await res.text();
                      msg.textContent = '提交失败：' + text;
                      return;
                    }
                    msg.textContent = '提交成功，电视正在加载图标库';
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
}
