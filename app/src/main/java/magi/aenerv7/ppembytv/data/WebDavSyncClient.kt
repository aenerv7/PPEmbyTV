package magi.aenerv7.ppembytv.data

import android.util.Log
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 自实现的 WebDAV 同步客户端（OkHttp）：
 * 在配置的服务器上固定使用目录 PPEmbyTV/ 与文件 sync-config.json。
 *
 * - [ensureDirectory]：MKCOL 创建目录（已存在时静默通过）
 * - [exists]：PROPFIND（Depth: 0）探测文件是否存在
 * - [upload]：PUT 上传 JSON
 * - [download]：GET 下载 JSON（404 返回 null）
 *
 * 所有方法均为阻塞调用，应在 Dispatchers.IO 中执行。
 */
class WebDavSyncClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String,
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = normalizeBaseUrl(serverUrl)
    private val directoryUrl: String = baseUrl + DIRECTORY_NAME + "/"
    private val fileUrl: String = baseUrl + DIRECTORY_NAME + "/" + FILE_NAME

    /** 创建 PPEmbyTV 目录；已存在（405/200/301/302/201）时静默通过。 */
    fun ensureDirectory() {
        val request = Request.Builder()
            .url(directoryUrl)
            .method("MKCOL", EMPTY_BODY)
            .header("Authorization", authHeader())
            .build()
        client.newCall(request).execute().use { response ->
            val code = response.code
            if (code in 200..299 || code == 301 || code == 302 || code == 405) {
                Log.d(TAG, "WebDAV 目录就绪: $directoryUrl ($code)")
            } else {
                throw IOException("创建 WebDAV 目录失败: HTTP $code")
            }
        }
    }

    /** 用 PROPFIND（Depth: 0）探测同步文件是否存在。 */
    fun exists(): Boolean {
        val request = Request.Builder()
            .url(fileUrl)
            .method("PROPFIND", EMPTY_BODY)
            .header("Depth", "0")
            .header("Authorization", authHeader())
            .build()
        client.newCall(request).execute().use { response ->
            return when (response.code) {
                200, 207 -> true
                404 -> false
                else -> throw IOException("WebDAV 探测失败: HTTP ${response.code}")
            }
        }
    }

    /** PUT 上传同步文件内容。 */
    fun upload(json: String) {
        ensureDirectory()
        val request = Request.Builder()
            .url(fileUrl)
            .put(json.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", authHeader())
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code !in 200..299) {
                throw IOException("WebDAV 上传失败: HTTP ${response.code} ${response.message}")
            }
            Log.d(TAG, "WebDAV 上传成功: $fileUrl (${json.length} bytes)")
        }
    }

    /** GET 下载同步文件内容；文件不存在时返回 null。 */
    fun download(): String? {
        val request = Request.Builder()
            .url(fileUrl)
            .get()
            .header("Authorization", authHeader())
            .build()
        client.newCall(request).execute().use { response ->
            return when (response.code) {
                200 -> response.body?.string()
                404 -> null
                else -> throw IOException("WebDAV 下载失败: HTTP ${response.code} ${response.message}")
            }
        }
    }

    private fun authHeader(): String = Credentials.basic(username, password)

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private companion object {
        const val TAG = "WebDavSyncClient"
        const val DIRECTORY_NAME = "PPEmbyTV"
        const val FILE_NAME = "sync-config.json"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val EMPTY_BODY = ByteArray(0).toRequestBody(null)
    }
}
