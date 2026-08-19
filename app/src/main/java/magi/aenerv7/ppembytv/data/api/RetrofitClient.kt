package magi.aenerv7.ppembytv.data.api

import android.content.Context
import android.util.Log
import magi.aenerv7.ppembytv.data.ProxyConfig
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.dlna.DlnaSettings
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    private const val CLIENT_NAME = "chaichai"
    private const val TAG = "RetrofitClient"
    private const val VIDEO_READ_TIMEOUT_SECONDS = 10L

    private var baseUrl = "http://127.0.0.1:8096/"
    private var accessToken = ""
    private var userId = ""
    private var deviceId = ""
    private var appVersion = "1.0.0"

    @Volatile
    private var trustAllCerts = false

    @Volatile
    private var currentProxy: Proxy? = null

    @Volatile
    private var currentProxyAuthKey = ProxyAuthKey()

    private var okHttpClientInstance: OkHttpClient? = null
    private var retrofitInstance: Retrofit? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private fun buildAuthHeaderValue(): String =
        "MediaBrowser Token=\"$accessToken\", Client=\"$CLIENT_NAME\", Device=\"AndroidTV\", DeviceId=\"$deviceId\", Version=\"$appVersion\""

    private val embyPrefixInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url
        var encodedPath = url.encodedPath
        val basePath = try {
            URL(baseUrl).path.trimEnd('/')
        } catch (e: Exception) {
            ""
        }
        if (basePath.isNotEmpty() && encodedPath.startsWith(basePath)) {
            encodedPath = encodedPath.substring(basePath.length)
        }
        if (encodedPath.startsWith("/emby/") || encodedPath.startsWith("/emby")) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder()
                    .url(url.newBuilder().encodedPath(basePath + "/emby" + encodedPath).build())
                    .build()
            )
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("User-Agent", buildUserAgent(appVersion))
        if (request.method == "POST" || request.method == "PUT") {
            builder.header("Content-Type", "application/json")
        }
        if (accessToken.isNotEmpty()) {
            builder.header("X-Emby-Token", accessToken)
        }
        builder.header("X-Emby-Authorization", buildAuthHeaderValue())
        chain.proceed(builder.build())
    }

    private val exoPlayerAuthInterceptor = Interceptor { chain ->
        val request = chain.request()
        val host = request.url.host
        val lowerHost = host.lowercase(Locale.ROOT)
        val baseHost = try {
            URL(baseUrl).host.lowercase(Locale.ROOT)
        } catch (e: Exception) {
            ""
        }
        val builder = request.newBuilder()
            .header("User-Agent", buildUserAgent(appVersion))
        if (baseHost.isNotBlank() && lowerHost == baseHost) {
            if (accessToken.isNotEmpty()) {
                builder.header("X-Emby-Token", accessToken)
            }
            builder.header("X-Emby-Authorization", buildAuthHeaderValue())
        }
        chain.proceed(builder.build())
    }

    private val socksProxyDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return if (ProxyManager.currentConfig.bypassLan && ProxyManager.isLanAddress(hostname)) {
                Log.d(TAG, "🔓 局域网不代理 DNS: $hostname 使用系统 DNS")
                Dns.SYSTEM.lookup(hostname)
            } else {
                Log.d(TAG, "SOCKS 代理模式：跳过本地 DNS 解析，主机名 $hostname 将由代理服务器解析")
                listOf(InetAddress.getByAddress(hostname, byteArrayOf(0, 0, 0, 0)))
            }
        }
    }

    private class TrustAllHostnameVerifier : javax.net.ssl.HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean = true
    }

    private class ProxyAuthenticator(
        private val tag: String,
        private val username: String,
        private val password: String,
    ) : okhttp3.Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.header("Proxy-Authorization") != null) {
                Log.w(tag, "代理认证已重试过一次，停止继续发送相同凭据")
                return null
            }
            return response.request.newBuilder()
                .header("Proxy-Authorization", Credentials.basic(username, password))
                .build()
        }
    }

    private fun createUnsafeSslContext(): Pair<SSLSocketFactory, X509TrustManager> {
        val trustAllCertsManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCertsManager), SecureRandom())
        return sslContext.socketFactory to trustAllCertsManager
    }

    private fun buildProxySelector(tag: String, label: String): ProxySelector {
        val proxy = currentProxy
        return object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                val host = uri?.host ?: ""
                val config = ProxyManager.currentConfig
                if (config.enabled && config.bypassLan && ProxyManager.isLanAddress(host)) {
                    Log.d(TAG, "🔓 局域网不代理: $host - 直连（$label）")
                    return mutableListOf(Proxy.NO_PROXY)
                }
                return mutableListOf(proxy ?: Proxy.NO_PROXY)
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Log.e(TAG, "$label 代理连接失败: $uri", ioe)
            }
        }
    }

    private fun applyProxyToBuilder(builder: OkHttpClient.Builder, tag: String, label: String) {
        val proxy = currentProxy
        if (proxy == null) {
            Log.d(TAG, "未设置代理，使用直连")
            return
        }
        builder.proxySelector(buildProxySelector(tag, label))
        if (proxy.type() == Proxy.Type.SOCKS) {
            Log.d(TAG, "✅ 应用 SOCKS 代理专用 DNS 解析器（远程解析）")
            builder.dns(socksProxyDns)
        }
        val config = ProxyManager.currentConfig
        if (config.hasCredentials) {
            Log.d(TAG, "✅ 应用代理认证凭据")
            builder.proxyAuthenticator(ProxyAuthenticator(tag, config.username, config.password))
        }
    }

    private fun applyUnsafeSsl(builder: OkHttpClient.Builder, tag: String) {
        if (trustAllCerts) {
            Log.d(tag, "⚠️ 应用不安全的SSL配置（信任所有证书）")
            val (socketFactory, trustManager) = createUnsafeSslContext()
            builder.sslSocketFactory(socketFactory, trustManager)
            builder.hostnameVerifier(TrustAllHostnameVerifier())
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        okHttpClientInstance?.let { return it }
        Log.d(TAG, "========== 创建新的 OkHttpClient ==========")
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(embyPrefixInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
        applyUnsafeSsl(builder, TAG)
        applyProxyToBuilder(builder, TAG, "API请求")
        okHttpClientInstance = builder.build()
        Log.d(TAG, "OkHttpClient 创建完成")
        return okHttpClientInstance!!
    }

    private fun getRetrofit(): Retrofit {
        retrofitInstance?.let { return it }
        retrofitInstance = Retrofit.Builder()
            .baseUrl(if (baseUrl.isNotEmpty()) baseUrl else "http://127.0.0.1:8096/")
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofitInstance!!
    }

    fun getApiService(): EmbyApiService = getRetrofit().create(EmbyApiService::class.java)

    fun getOkHttpClientForExoPlayer(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(VIDEO_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(exoPlayerAuthInterceptor)
            .addInterceptor(httpLoggingInterceptor)
        applyUnsafeSsl(builder, TAG)
        applyProxyToBuilder(builder, TAG, "视频流")
        return builder.build()
    }

    fun getOkHttpClientForDlna(context: Context?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(VIDEO_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        var trustAll = false
        if (context != null) {
            try {
                trustAll = DlnaSettings(context).configSync.trustAllCerts
            } catch (e: Exception) {
            }
        }
        if (trustAll) {
            Log.d(TAG, "⚠️ DLNA 应用不安全的SSL配置（信任所有证书）")
            val (socketFactory, trustManager) = createUnsafeSslContext()
            builder.sslSocketFactory(socketFactory, trustManager)
            builder.hostnameVerifier(TrustAllHostnameVerifier())
        }
        applyProxyToBuilder(builder, TAG, "DLNA")
        return builder.build()
    }

    fun buildUserAgent(version: String): String = "chaichai/$version"

    fun initialize(serverUrl: String) {
        var url = serverUrl
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        if (baseUrl == url) {
            return
        }
        baseUrl = url
        retrofitInstance = null
    }

    fun setAppVersion(version: String) {
        appVersion = version
        Log.d(TAG, "设置应用版本号: $version")
    }

    fun setDeviceId(id: String) {
        deviceId = id
        Log.d(TAG, "设置DeviceId: $id")
    }

    fun setAuthToken(token: String, uid: String) {
        accessToken = token
        userId = uid
    }

    fun setTrustAllCerts(trust: Boolean) {
        if (trustAllCerts != trust) {
            Log.d(TAG, "设置信任所有SSL证书: $trust")
            trustAllCerts = trust
            okHttpClientInstance = null
            retrofitInstance = null
        }
    }

    fun updateProxy(proxy: Proxy?) {
        val fromCurrentConfig = ProxyAuthKey.fromCurrentConfig()
        if (currentProxy == proxy && currentProxyAuthKey == fromCurrentConfig) {
            Log.d(TAG, "代理配置未改变，无需更新")
            return
        }
        Log.d(TAG, "代理配置已改变，清除 OkHttpClient 和 Retrofit 实例")
        currentProxy = proxy
        currentProxyAuthKey = fromCurrentConfig
        okHttpClientInstance = null
        retrofitInstance = null
    }

    fun getImageUrl(itemId: String, imageType: String, tag: String?, maxWidth: Int?): String {
        val base = "$baseUrl" + "emby/Items/$itemId/Images/$imageType"
        val params = mutableListOf<String>()
        if (tag != null) params.add("tag=$tag")
        if (maxWidth != null) params.add("maxWidth=$maxWidth")
        if (imageType == "Logo") {
            params.add("Format=png")
        } else {
            params.add("Format=jpg")
            params.add("Quality=90")
        }
        return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
    }

    fun getPersonImageUrl(personId: String, tag: String?, maxWidth: Int): String {
        val base = "$baseUrl" + "emby/Items/$personId/Images/Primary"
        val params = mutableListOf<String>()
        if (tag != null) params.add("tag=$tag")
        params.add("maxWidth=$maxWidth")
        params.add("Format=jpg")
        params.add("Quality=90")
        return "$base?${params.joinToString("&")}"
    }

    fun getSubtitleUrl(
        itemId: String,
        mediaSourceId: String,
        subtitleIndex: Int,
        format: String,
        startPositionTicks: Long,
    ): String {
        val params = mutableListOf<String>()
        if (startPositionTicks > 0) params.add("StartPositionTicks=$startPositionTicks")
        if (accessToken.isNotEmpty()) params.add("api_key=$accessToken")
        val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        return "$baseUrl" + "emby/Videos/$itemId/$mediaSourceId/Subtitles/$subtitleIndex/Stream.$format$query"
    }

    fun getVideoUrl(itemId: String, mediaSourceId: String, container: String, startTimeTicks: Long): String {
        var url = "$baseUrl" + "emby/videos/$itemId/stream.$container?"
        if (accessToken.isNotEmpty()) {
            url += "api_key=$accessToken&"
        }
        url += "Static=true&MediaSourceId=$mediaSourceId&DeviceId=$deviceId"
        if (startTimeTicks > 0) {
            url += "&StartTimeTicks=$startTimeTicks"
        }
        return url
    }

    fun getTranscodedVideoUrl(
        itemId: String,
        mediaSourceId: String,
        startTimeTicks: Long,
        videoBitrate: Long?,
        maxWidth: Int?,
        maxHeight: Int?,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
    ): String {
        val params = mutableListOf(
            "MediaSourceId=$mediaSourceId",
            "DeviceId=$deviceId",
            "Container=ts",
        )
        if (startTimeTicks > 0) params.add("StartTimeTicks=$startTimeTicks")
        videoBitrate?.takeIf { it > 0 }?.let { params.add("VideoBitRate=$it") }
        maxWidth?.takeIf { it > 0 }?.let { params.add("MaxWidth=$it") }
        maxHeight?.takeIf { it > 0 }?.let { params.add("MaxHeight=$it") }
        audioStreamIndex?.let { params.add("AudioStreamIndex=$it") }
        subtitleStreamIndex?.let { params.add("SubtitleStreamIndex=$it") }
        if (accessToken.isNotEmpty()) params.add("api_key=$accessToken")
        return "$baseUrl" + "emby/Videos/$itemId/master.m3u8?${params.joinToString("&")}"
    }

    fun remapAbsoluteMediaUrlToBaseUrl(rawUrl: String): String {
        val base = baseUrl.toHttpUrlOrNull() ?: return rawUrl
        val target = rawUrl.toHttpUrlOrNull() ?: return rawUrl
        val basePath = base.encodedPath.trimEnd('/')
        if (basePath.isBlank()) {
            return rawUrl
        }
        var encodedPath = target.encodedPath
        if (!encodedPath.equals("/emby", true) && !encodedPath.startsWith("/emby/", true) &&
            (encodedPath.equals("/videos", true) || encodedPath.startsWith("/videos/", true))
        ) {
            encodedPath = "/emby$encodedPath"
        }
        if (encodedPath != basePath) {
            if (!encodedPath.startsWith("$basePath/")) {
                encodedPath = basePath + encodedPath
            }
        }
        return base.newBuilder()
            .encodedPath(encodedPath)
            .encodedQuery(target.encodedQuery)
            .build()
            .toString()
    }

    fun hasCustomRoutingPath(): Boolean {
        val parsed = baseUrl.toHttpUrlOrNull() ?: return false
        val encodedPath = parsed.encodedPath
        return !(encodedPath.isBlank() || encodedPath == "/")
    }

    fun getBaseUrl(): String = baseUrl
    fun getClientName(): String = CLIENT_NAME
    fun getDeviceId(): String = deviceId
    fun getUserId(): String = userId
    fun getAccessToken(): String = accessToken
    fun getTrustAllCerts(): Boolean = trustAllCerts
    fun getVideoReadTimeoutSeconds(): Long = VIDEO_READ_TIMEOUT_SECONDS

    private data class ProxyAuthKey(
        val hasCredentials: Boolean = false,
        val username: String = "",
        val password: String = "",
    ) {
        companion object {
            fun fromCurrentConfig(): ProxyAuthKey {
                val config = ProxyManager.currentConfig
                return ProxyAuthKey(config.hasCredentials, config.username, config.password)
            }
        }
    }
}
