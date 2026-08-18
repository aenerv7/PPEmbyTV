package magi.aenerv7.ppembytv.api

import magi.aenerv7.ppembytv.BuildConfig
import magi.aenerv7.ppembytv.data.ProxySettings
import magi.aenerv7.ppembytv.data.ServerConfig
import magi.aenerv7.ppembytv.data.isValid
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json

object HttpClients {

    const val CLIENT_NAME = "ppembytv"
    const val DEVICE_NAME = "AndroidTV"

    private var appVersion: String = "1.0.0"
    fun setAppVersion(v: String) { appVersion = v }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun json(): Json = json

    private fun isLanAddress(host: String): Boolean {
        return host == "localhost" ||
            host.endsWith(".local") ||
            host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) && run {
                val parts = host.split(".").map { it.toIntOrNull() ?: -1 }
                parts[0] == 10 ||
                (parts[0] == 172 && parts[1] in 16..31) ||
                (parts[0] == 192 && parts[1] == 168) ||
                (parts[0] == 127)
            }
    }

    /**
     * 构建带认证头（X-Emby-Token / X-Emby-Authorization）、可选代理、可选 trust-all 证书的 OkHttpClient。
     * 同时被 Retrofit 与 Media3 播放器复用。
     */
    fun buildOkHttpClient(server: ServerConfig, proxy: ProxySettings?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .pingInterval(30, TimeUnit.SECONDS)

        val effectiveProxy = proxy?.takeIf { it.enabled && it.isValid() && !server.directOnly }
        if (effectiveProxy != null) {
            val type = if (effectiveProxy.type == "socks") Proxy.Type.SOCKS else Proxy.Type.HTTP
            builder.proxy(Proxy(type, java.net.InetSocketAddress(effectiveProxy.host, effectiveProxy.port)))
            if (effectiveProxy.type == "socks") {
                // SOCKS 代理模式下跳过本地 DNS 解析，由代理服务器解析域名
                builder.dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        if (effectiveProxy.bypassLan && isLanAddress(hostname)) {
                            return Dns.SYSTEM.lookup(hostname)
                        }
                        return listOf(InetAddress.getByAddress(hostname, byteArrayOf(0, 0, 0, 0)))
                    }
                })
            }
        }

        if (server.trustAllCerts) {
            val (sslSocketFactory, trustManager) = createUnsafeSslContext()
            builder.sslSocketFactory(sslSocketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        builder.addInterceptor(authInterceptor(server))
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(buildLoggingInterceptor())
        }
        return builder.build()
    }

    private fun authInterceptor(server: ServerConfig): Interceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val newBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("User-Agent", buildUserAgent())
        if (original.method == "POST" || original.method == "PUT") {
            newBuilder.header("Content-Type", "application/json")
        }
        val token = server.accessToken
        if (token.isNotEmpty()) {
            newBuilder.header("X-Emby-Token", token)
        }
        val auth = "MediaBrowser Token=\"$token\", Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"${server.deviceId}\", Version=\"$appVersion\""
        newBuilder.header("X-Emby-Authorization", auth)
        chain.proceed(newBuilder.build())
    }

    fun buildUserAgent(): String = "$CLIENT_NAME/$appVersion (Android TV)"

    private fun createUnsafeSslContext(): Pair<SSLSocketFactory, X509TrustManager> {
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
        return sslContext.socketFactory as SSLSocketFactory to trustAllManager
    }

    /** 播放器使用的日志级别（默认 NONE，可开启调试） */
    fun buildLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC
        return interceptor
    }

    /** 相对路径（如 Videos/123/stream.mp4）转完整 URL */
    fun resolveUrl(server: ServerConfig, relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            return relativeOrAbsolute
        }
        return server.getFullUrl() + relativeOrAbsolute.trimStart('/')
    }

    /** 把 host:port 之类的文本规范化为 http 地址，供扫码配置/手动输入解析用 */
    fun normalizeServerUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return try {
            s.toHttpUrl().toString().trimEnd('/')
        } catch (e: Exception) {
            s
        }
    }
}
