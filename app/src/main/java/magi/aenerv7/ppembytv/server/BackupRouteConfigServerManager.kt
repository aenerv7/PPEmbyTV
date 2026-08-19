package magi.aenerv7.ppembytv.server

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.model.BackupRouteConfig
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.model.normalizeBackupRouteConfig
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.NoSuchElementException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class BackupRouteConfigServerManager(private val context: Context) {

    private var server: BackupRouteConfigServer? = null

    /** 启动服务器；成功返回局域网访问地址（http://<ip>:<port>），失败返回 null。 */
    fun startServer(
        serverAlias: String,
        initialRoutes: List<BackupRouteConfig>,
        onConfigReceived: (List<BackupRouteConfig>) -> Unit,
    ): String? {
        stopServer()
        var lastError: IOException? = null
        for (port in DEFAULT_PORT until 8800) {
            try {
                val configServer = BackupRouteConfigServer(port, serverAlias, initialRoutes.take(MAX_ROUTES)) { routes ->
                    CoroutineScope(Dispatchers.Main).launch { onConfigReceived(routes) }
                }
                server = configServer
                configServer.start()
                return resolveServerUrl(context, port)
            } catch (e: IOException) {
                lastError = e
                server?.stop()
                server = null
                Log.e(TAG, "端口 $port 启动失败", e)
                val message = e.message
                if (message == null || !message.contains("EADDRINUSE")) {
                    break
                }
            }
        }
        if (lastError != null) {
            Log.e(TAG, "所有候选端口均启动失败", lastError)
        }
        return null
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    /** 抓取远端服务器的备用线路域名（Emby /emby/System/Ext/ServerDomains）。 */
    suspend fun fetchServerDomains(server: ServerConfig): Result<RemoteBackupRouteFetchResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = try {
                    server.fullUrl.toHttpUrlOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "获取服务器备用线路失败", e)
                    null
                } ?: throw IllegalArgumentException("Invalid server url")
                val url = baseUrl.newBuilder()
                    .addPathSegments("emby/System/Ext/ServerDomains")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("User-Agent", RetrofitClient.buildUserAgent("0.3.1-alpha2"))
                    .header("X-Emby-Token", server.accessToken ?: "")
                    .header("X-Emby-Authorization", buildEmbyAuthorizationHeader(server))
                    .get()
                    .build()
                val client = createFetchClient(server, url.host)
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        throw IllegalStateException("Empty response body")
                    }
                    val parsed = try {
                        Gson().fromJson(body, ExtServerDomainsResponse::class.java) ?: ExtServerDomainsResponse()
                    } catch (t: Throwable) {
                        Log.e(TAG, "解析备用线路列表失败", t)
                        throw t
                    }
                    if (parsed.ok != true) {
                        throw IllegalStateException("Response ok=false")
                    }
                    val items = parsed.data ?: emptyList()
                    val routes = buildImportableRoutes(server, items)
                    if (routes.isEmpty()) {
                        throw NoSuchElementException("No importable routes")
                    }
                    RemoteBackupRouteFetchResult(routes.size, routes.take(MAX_ROUTES))
                }
            }
        }

    private fun buildEmbyAuthorizationHeader(server: ServerConfig): String {
        val accessToken = server.accessToken ?: ""
        val deviceId = RetrofitClient.getDeviceId()
        return "MediaBrowser Token=\"$accessToken\", Client=\"${RetrofitClient.getClientName()}\", Device=\"AndroidTV\", DeviceId=\"$deviceId\", Version=\"0.3.1-alpha2\""
    }

    private fun buildImportableRoutes(server: ServerConfig, items: List<ExtServerDomainItem>): List<BackupRouteConfig> {
        val alias = server.alias.ifBlank { "主线路" }
        val mainRoute = normalizeBackupRouteConfig(
            BackupRouteConfig(
                alias = alias,
                protocol = server.protocol,
                host = server.host,
                port = server.port,
                path = server.path ?: "",
                directOnly = server.directOnly,
            )
        )
        val seenSignatures = linkedSetOf(toRouteSignature(mainRoute))
        val result = mutableListOf<BackupRouteConfig>()
        items.forEachIndexed { index, item ->
            val url = (item.url ?: "").trim()
            if (!url.isBlank()) {
                var name = (item.name ?: "").trim()
                if (name.isBlank()) {
                    name = "备用线路${index + 1}"
                }
                val route = normalizeBackupRouteConfig(
                    BackupRouteConfig(
                        alias = name,
                        protocol = server.protocol,
                        host = url,
                        port = server.port,
                        path = "",
                        directOnly = true,
                    )
                )
                if (!route.host.isBlank() && route.port >= 1 && route.port < 65536) {
                    if (seenSignatures.add(toRouteSignature(route))) {
                        result.add(route)
                    }
                }
            }
        }
        return result
    }

    private fun createFetchClient(server: ServerConfig, requestHost: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
        if (server.trustAllCerts) {
            val trustAllCertsManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustAllCertsManager), SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCertsManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        if (!server.directOnly) {
            val proxy = ProxyManager.currentProxy
            val proxyConfig = ProxyManager.currentConfig
            var bypass = false
            if (proxyConfig.enabled && proxyConfig.bypassLan && ProxyManager.isLanAddress(requestHost)) {
                bypass = true
            }
            if (proxy != null && !bypass) {
                builder.proxy(proxy)
                if (proxyConfig.hasCredentials) {
                    builder.proxyAuthenticator(
                        object : okhttp3.Authenticator {
                            override fun authenticate(route: Route?, response: Response): Request? {
                                if (response.request.header("Proxy-Authorization") != null) {
                                    Log.w(TAG, "代理认证已重试过一次，停止继续发送相同凭据")
                                    return null
                                }
                                return response.request.newBuilder()
                                    .header("Proxy-Authorization", Credentials.basic(proxyConfig.username, proxyConfig.password))
                                    .build()
                            }
                        }
                    )
                }
                if (proxy.type() == java.net.Proxy.Type.SOCKS) {
                    builder.dns(socksProxyDns)
                }
            }
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "BackupRouteCfgServer"
        const val DEFAULT_PORT = 8771
        const val MAX_PORT = 8799
        const val MAX_ROUTES = 50
    }
}

/** SOCKS 代理下跳过本地 DNS 解析，交由代理服务器解析主机名。 */
private val socksProxyDns = object : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return if (ProxyManager.currentConfig.bypassLan && ProxyManager.isLanAddress(hostname)) {
            Dns.SYSTEM.lookup(hostname)
        } else {
            listOf(InetAddress.getByAddress(hostname, byteArrayOf(0, 0, 0, 0)))
        }
    }
}

/** 线路唯一签名：protocol|host|port|path。 */
private fun toRouteSignature(route: BackupRouteConfig): String =
    listOf(
        route.protocol.lowercase(Locale.ROOT),
        route.host.trim().lowercase(Locale.ROOT),
        route.port.toString(),
        (route.path ?: "").trim().trim('/'),
    ).joinToString("|")

/** HTML 属性转义。 */
internal fun escapeHtml(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
