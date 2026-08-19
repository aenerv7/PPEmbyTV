package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import magi.aenerv7.ppembytv.BuildConfig
import magi.aenerv7.ppembytv.PPEmbyTVApp
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.preferences.ServerPreferences
import magi.aenerv7.ppembytv.data.preferences.UserPreferences
import magi.aenerv7.ppembytv.dlna.DlnaConfig
import magi.aenerv7.ppembytv.dlna.DlnaSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * 自实现的 WebDAV 同步执行器。
 *
 * 上传：把「服务器配置 + 应用设置」打包为 JSON（即 sync-config.json）经 [WebDavSyncClient] PUT 到
 * 配置的 WebDAV 服务器 PPEmbyTV/sync-config.json；下载：GET 该 JSON 并写回本地偏好。
 *
 * 同步内容与扫码页描述一致：
 * - 服务器配置：服务器列表、最后使用服务器（含地址/账号/Token/备用线路）
 * - 应用设置：代理、服务器图标库 URL、DLNA、解码器、Trakt 基础配置（不含授权 Token）、剧集/媒体库排序
 */
class WebDavSyncManager(private val context: Context) {

    private val gson = Gson()

    /** 上传本地配置到 WebDAV，返回成功提示。 */
    suspend fun uploadSync(config: WebDavSyncConfig): String = withContext(Dispatchers.IO) {
        val payload = buildPayload(config)
        val client = WebDavSyncClient(config.serverUrl, config.username, config.password)
        client.upload(payload.toString())
        val count = payload.optInt("serverCount", 0)
        Log.i(TAG, "WebDAV 上传完成: servers=$count")
        "上传成功（$count 个服务器）"
    }

    /** 从 WebDAV 下载配置并写回本地，返回成功提示。 */
    suspend fun downloadSync(config: WebDavSyncConfig): String = withContext(Dispatchers.IO) {
        val client = WebDavSyncClient(config.serverUrl, config.username, config.password)
        val jsonText = client.download() ?: throw IOException("WebDAV 上还没有同步文件（请先在其他设备上传）")
        val json = runCatching { JSONObject(jsonText) }
            .getOrElse { throw IOException("同步文件格式无效") }
        applyPayload(json)
        val count = json.optInt("serverCount", 0)
        Log.i(TAG, "WebDAV 下载完成: servers=$count")
        "下载成功（$count 个服务器）"
    }

    // ---- payload 构建 ----------------------------------------------------

    private suspend fun buildPayload(config: WebDavSyncConfig): JSONObject {
        val serverPrefs = ServerPreferences(context)
        val userPrefs = UserPreferences(context)
        val json = JSONObject()
        json.put("schemaVersion", PAYLOAD_VERSION)
        json.put("app", "PPEmbyTV")
        json.put("appVersionName", BuildConfig.VERSION_NAME)
        json.put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        json.put("exportedAtMs", System.currentTimeMillis())

        val servers = serverPrefs.getAllServers()
        val serversArr = JSONArray()
        for (s in servers) {
            runCatching { serversArr.put(JSONObject(gson.toJson(s))) }
        }
        json.put("servers", serversArr)
        json.put("serverCount", servers.size)
        json.put("serverOrder", JSONArray(servers.map { it.id }))
        json.put("lastUsedServerId", serverPrefs.getLastUsedServerId() ?: "")

        val settings = JSONObject()

        val proxy = ProxySettings(context).proxyConfigFlow.first()
        settings.put(
            "proxy",
            JSONObject()
                .put("enabled", proxy.enabled)
                .put("protocol", proxy.protocol.name)
                .put("host", proxy.host)
                .put("port", proxy.port)
                .put("username", proxy.username)
                .put("password", proxy.password)
                .put("bypassLan", proxy.bypassLan),
        )

        settings.put("serverIconLibraryUrl", userPrefs.serverIconLibraryUrl ?: "")

        val dlna = DlnaSettings(context).configFlow.first()
        settings.put(
            "dlna",
            JSONObject()
                .put("enabled", dlna.enabled)
                .put("deviceName", dlna.deviceName)
                .put("autoPlay", dlna.autoPlay)
                .put("useProxyByDefault", dlna.useProxyByDefault)
                .put("trustAllCerts", dlna.trustAllCerts),
        )

        val decoder = DecoderSettings(context).decoderConfigFlow.first()
        settings.put(
            "decoder",
            JSONObject()
                .put("mode", decoder.mode)
                .put("audioMode", decoder.audioMode)
                .put("audioPassthroughPriorityEnabled", decoder.audioPassthroughPriorityEnabled)
                .put("dv7CompatibilityEnabled", decoder.dv7CompatibilityEnabled),
        )

        val trakt = TraktSettings(context)
        settings.put(
            "trakt",
            JSONObject()
                .put("clientId", trakt.clientId)
                .put("clientSecret", trakt.clientSecret)
                .put("enabled", trakt.enabled)
                .put("promptCloudProgress", trakt.promptCloudProgress)
                .put("syncLocalProgressToTrakt", trakt.syncLocalProgressToTrakt),
        )

        putPrefsObject(settings, "episodeSort", PPEmbyTVApp.episodeSortSettings?.all)
        putPrefsObject(settings, "librarySort", PPEmbyTVApp.librarySortSettings?.all)

        json.put("settings", settings)
        return json
    }

    // ---- payload 应用 ----------------------------------------------------

    private suspend fun applyPayload(json: JSONObject) {
        val serverPrefs = ServerPreferences(context)
        json.optJSONArray("servers")?.let { serversArr ->
            val list = mutableListOf<ServerConfig>()
            for (i in 0 until serversArr.length()) {
                val obj = serversArr.optJSONObject(i) ?: continue
                runCatching { gson.fromJson(obj.toString(), ServerConfig::class.java) }
                    .onSuccess { list.add(it) }
            }
            val lastUsedId = json.optString("lastUsedServerId", "").takeIf { it.isNotBlank() }
            serverPrefs.replaceAllServers(list, lastUsedId)
            json.optJSONArray("serverOrder")?.let { orderArr ->
                val ids = (0 until orderArr.length()).mapNotNull { orderArr.optString(it, null) }
                if (ids.isNotEmpty()) {
                    serverPrefs.saveServerOrder(ids)
                }
            }
        }

        val settings = json.optJSONObject("settings") ?: return

        settings.optJSONObject("proxy")?.let { p ->
            val protocol = runCatching { ProxyProtocol.valueOf(p.optString("protocol", "HTTP")) }
                .getOrDefault(ProxyProtocol.HTTP)
            val proxyConfig = ProxyConfig(
                enabled = p.optBoolean("enabled", false),
                protocol = protocol,
                host = p.optString("host", ""),
                port = p.optInt("port", 7890),
                username = p.optString("username", ""),
                password = p.optString("password", ""),
                bypassLan = p.optBoolean("bypassLan", true),
            )
            ProxySettings(context).saveProxyConfig(proxyConfig)
            ProxyManager.applyProxyConfig(proxyConfig)
        }

        val iconUrl = settings.optString("serverIconLibraryUrl", "").takeIf { it.isNotBlank() }
        UserPreferences(context).saveServerIconLibraryUrl(iconUrl)

        settings.optJSONObject("dlna")?.let { d ->
            DlnaSettings(context).saveConfig(
                DlnaConfig(
                    enabled = d.optBoolean("enabled", false),
                    deviceName = d.optString("deviceName", "皮皮 TV"),
                    autoPlay = d.optBoolean("autoPlay", false),
                    useProxyByDefault = d.optBoolean("useProxyByDefault", false),
                    trustAllCerts = d.optBoolean("trustAllCerts", false),
                ),
            )
        }

        settings.optJSONObject("decoder")?.let { d ->
            val decoder = DecoderSettings(context)
            decoder.saveDecoderMode(d.optString("mode", DecoderSettings.DECODER_MODE_AUTO))
            decoder.saveAudioDecoderMode(d.optString("audioMode", DecoderSettings.AUDIO_DECODER_AUTO))
            decoder.saveAudioPassthroughPriorityEnabled(d.optBoolean("audioPassthroughPriorityEnabled", false))
            decoder.saveDv7CompatibilityEnabled(d.optBoolean("dv7CompatibilityEnabled", false))
        }

        settings.optJSONObject("trakt")?.let { t ->
            val trakt = TraktSettings(context)
            trakt.clientId = t.optString("clientId", TraktSettings.DEFAULT_CLIENT_ID)
            trakt.clientSecret = t.optString("clientSecret", TraktSettings.DEFAULT_CLIENT_SECRET)
            trakt.enabled = t.optBoolean("enabled", false)
            trakt.promptCloudProgress = t.optBoolean("promptCloudProgress", true)
            trakt.syncLocalProgressToTrakt = t.optBoolean("syncLocalProgressToTrakt", false)
        }

        applyPrefsObject(settings.optJSONObject("episodeSort"), PPEmbyTVApp.episodeSortSettings)
        applyPrefsObject(settings.optJSONObject("librarySort"), PPEmbyTVApp.librarySortSettings)
    }

    // ---- 通用 SharedPreferences <-> JSON 辅助 ------------------------------

    private fun putPrefsObject(parent: JSONObject, key: String, map: Map<String, *>?) {
        if (map == null) return
        val obj = JSONObject()
        for ((k, v) in map) {
            when (v) {
                is String -> obj.put(k, v)
                is Int -> obj.put(k, v)
                is Long -> obj.put(k, v)
                is Float -> obj.put(k, v.toDouble())
                is Boolean -> obj.put(k, v)
                is Set<*> -> obj.put(k, JSONArray(v.filterIsInstance<String>()))
                else -> Unit
            }
        }
        parent.put(key, obj)
    }

    private fun applyPrefsObject(obj: JSONObject?, prefs: SharedPreferences?) {
        if (obj == null || prefs == null) return
        val editor = prefs.edit()
        for (key in obj.keys()) {
            when (val value = obj.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putLong(key, value.toLong())
                is String -> editor.putString(key, value)
                is JSONArray -> editor.putStringSet(
                    key,
                    (0 until value.length()).mapNotNull { value.optString(it, null) }.toSet(),
                )
                else -> Unit
            }
        }
        editor.apply()
    }

    private companion object {
        const val TAG = "WebDavSyncManager"
        const val PAYLOAD_VERSION = 1
    }
}
