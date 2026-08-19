package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import magi.aenerv7.ppembytv.data.SubtitleFontEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.Locale

/**
 * 局域网字幕字体上传服务器：GET / 展示上传页，POST /upload（multipart：fontfile + fontname）保存字体。
 *
 * 注意：原始 handleUpload 的字节码被 JADX 还原失败（大量重复区块），此处按可辨识的逻辑重建：
 * 取文件名 → 含 % 时 URL 解码 → 取扩展名（ttf/otf）→ 校验 → 复制文件到 filesDir/subtitle_fonts。
 */
internal class SubtitleFontUploadServer(
    port: Int,
    private val context: Context,
    private val onFontUploaded: (SubtitleFontEntry) -> Unit,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/") {
            return serveHtml()
        }
        if (session.uri == "/upload" && session.method == Method.POST) {
            return handleUpload(session)
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun handleUpload(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val fontFilePath = files["fontfile"]
                ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "No file")
            var rawName = session.parameters["fontname"]?.firstOrNull()
            if (rawName.isNullOrBlank()) {
                rawName = session.parameters["fontfile"]?.firstOrNull()
            }
            if (rawName.isNullOrBlank()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Invalid filename")
            }
            var candidate: String? = rawName
            if (rawName.contains("%")) {
                try {
                    candidate = URLDecoder.decode(rawName, "UTF-8")
                } catch (e: Exception) {
                    // 解码失败时保持原始名称
                }
            }
            Log.d(TAG, "filename raw=$rawName | candidate=$candidate | decoded=$candidate")
            val ext = candidate?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
            val safeExt = if (ext in FONT_EXTENSIONS) ext else "ttf"
            if (!candidate.isNullOrBlank() && !candidate.contains('\uFFFD') && !candidate.contains('?')) {
                val finalName = "字体_" + System.currentTimeMillis() + "." + safeExt
                Log.d(TAG, "filename ext=$ext | safeExt=$safeExt | finalName=$finalName")
                if (ext !in FONT_EXTENSIONS) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Unsupported format")
                }
                val fontsDir = File(context.filesDir, "subtitle_fonts")
                if (!fontsDir.exists()) {
                    fontsDir.mkdirs()
                }
                val fontId = "font_" + System.currentTimeMillis()
                val targetFile = File(fontsDir, "$fontId.$safeExt")
                FileInputStream(File(fontFilePath)).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                onFontUploaded.invoke(SubtitleFontEntry(fontId, finalName, targetFile.absolutePath))
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain; charset=UTF-8", "OK")
            }
            NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain; charset=UTF-8", "Invalid filename")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=UTF-8", "Error: " + e.message)
        }
    }

    private fun serveHtml(): Response {
        val response = NanoHTTPD.newFixedLengthResponse(
            Response.Status.OK,
            "text/html; charset=UTF-8",
            """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>上传字幕字体</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background:#0f172a; color:#fff; padding:36px; }
        .card { background:#111827; border-radius:12px; padding:28px; max-width:560px; margin:0 auto; }
        h1 { font-size:20px; margin-bottom:16px; }
        p { color:#cbd5e1; font-size:14px; margin-bottom:16px; }
        input { display:none; }
        .file-btn {
            margin-top:28px; margin-bottom:12px; width:100%; padding:12px 12px;
            background:#1f2937; border-radius:8px; color:#fff; text-align:center;
        }
        .file-name { margin-top:14px; font-size:12px; color:#cbd5e1; }
        button { margin-top:24px; width:100%; padding:14px; background:#3b82f6; color:#fff; border:none; border-radius:8px; font-size:16px; }
        .msg { margin-top:18px; font-size:14px; }
    </style>
</head>
<body>
    <div class="card">
        <h1>上传字幕字体</h1>
        <p>支持 .ttf / .otf。上传后会自动同步到电视。</p>
        <label class="file-btn" for="file">选择字体文件</label>
        <input id="file" type="file" accept=".ttf,.otf" />
        <div id="filename" class="file-name">未选择文件</div>
        <button onclick="upload()">上传字体</button>
        <div id="msg" class="msg"></div>
    </div>
    <script>
        const fileInput = document.getElementById('file');
        fileInput.addEventListener('change', () => {
            const f = fileInput.files[0];
            document.getElementById('filename').innerText = f ? f.name : '未选择文件';
        });
        async function upload() {
            const file = document.getElementById('file').files[0];
            if (!file) {
                document.getElementById('msg').innerText = '请选择字体文件';
                return;
            }
            const form = new FormData();
            form.append('fontfile', file, file.name);
            form.append('fontname', encodeURIComponent(file.name));
            document.getElementById('msg').innerText = '上传中...';
            try {
                const res = await fetch('/upload', { method: 'POST', body: form });
                const text = await res.text();
                document.getElementById('msg').innerText = res.ok ? '上传成功' : ('上传失败：' + text);
            } catch (e) {
                document.getElementById('msg').innerText = '上传失败：' + e.message;
            }
        }
    </script>
</body>
</html>"""
        )
        response.addHeader("Content-Type", "text/html; charset=UTF-8")
        return response
    }

    private companion object {
        const val TAG = "FontUploadServer"
        val FONT_EXTENSIONS = setOf("ttf", "otf")
    }
}
