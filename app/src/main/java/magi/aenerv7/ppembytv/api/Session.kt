package magi.aenerv7.ppembytv.api

import magi.aenerv7.ppembytv.data.ProxySettings
import magi.aenerv7.ppembytv.data.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 当前会话：活动的服务器 + 按需构建的 EmbyApiService 缓存。
 */
object Session {

    private val _activeServer = MutableStateFlow<ServerConfig?>(null)
    val activeServer: StateFlow<ServerConfig?> = _activeServer

    private var currentProxy: ProxySettings? = null

    private val cache = ConcurrentHashMap<String, EmbyApiService>()

    fun setProxy(proxy: ProxySettings?) {
        currentProxy = proxy
    }

    /** 当前生效的代理设置（供播放器等复用） */
    fun currentProxy(): ProxySettings? = currentProxy

    fun setActiveServer(server: ServerConfig?) {
        _activeServer.value = server
    }

    fun api(): EmbyApiService {
        val server = _activeServer.value ?: throw IllegalStateException("No active server")
        return apiFor(server)
    }

    /** 按服务器 + 代理 + 证书配置缓存 Retrofit 服务 */
    fun apiFor(server: ServerConfig): EmbyApiService {
        val key = buildCacheKey(server)
        return cache.getOrPut(key) {
            val client = HttpClients.buildOkHttpClient(server, currentProxy)
            Retrofit.Builder()
                .baseUrl(server.getFullUrl())
                .client(client)
                .addConverterFactory(HttpClients.json().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(EmbyApiService::class.java)
        }
    }

    fun invalidate() {
        cache.clear()
    }

    private fun buildCacheKey(server: ServerConfig): String {
        val p = currentProxy
        return listOf(
            server.id,
            server.accessToken,
            server.trustAllCerts,
            server.directOnly,
            p?.enabled, p?.type, p?.host, p?.port, p?.username, p?.password, p?.bypassLan
        ).joinToString("|")
    }
}
