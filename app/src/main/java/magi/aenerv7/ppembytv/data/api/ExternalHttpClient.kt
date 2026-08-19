package magi.aenerv7.ppembytv.data.api

import android.util.Log
import magi.aenerv7.ppembytv.data.ProxyConfig
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.data.ProxyProtocol
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object ExternalHttpClient {

    private const val TAG = "ExternalHttpClient"

    enum class RouteMode {
        AUTO, FORCE_PROXY, FORCE_DIRECT
    }

    private val socksProxyDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return if (ProxyManager.currentConfig.bypassLan && ProxyManager.isLanAddress(hostname)) {
                Dns.SYSTEM.lookup(hostname)
            } else {
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
    ) : Authenticator {
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

    private fun createProxyFromConfig(config: ProxyConfig): Proxy? {
        if (config.enabled && config.host.isNotBlank()) {
            val type = when (config.protocol) {
                ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> Proxy.Type.HTTP
                ProxyProtocol.SOCKS5 -> Proxy.Type.SOCKS
            }
            return try {
                Proxy(type, InetSocketAddress(config.host, config.port))
            } catch (e: Exception) {
                Log.e(TAG, "根据全局代理配置创建外部请求代理失败", e)
                null
            }
        }
        return null
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

    private fun applyProxyRoute(builder: OkHttpClient.Builder, routeMode: RouteMode, ignoreServerDirectOnly: Boolean) {
        val config = ProxyManager.currentConfig
        val proxy: Proxy? = if (ignoreServerDirectOnly) {
            createProxyFromConfig(config) ?: ProxyManager.currentProxy
        } else {
            ProxyManager.currentProxy
        }
        val proxyEnabled = config.enabled && proxy != null

        when (routeMode) {
            RouteMode.FORCE_DIRECT -> {
                builder.proxy(Proxy.NO_PROXY)
            }
            RouteMode.FORCE_PROXY -> {
                if (!proxyEnabled) {
                    Log.w(TAG, "FORCE_PROXY 但未配置有效代理，回退直连")
                    builder.proxy(Proxy.NO_PROXY)
                } else {
                    builder.proxy(proxy)
                    if (config.hasCredentials) {
                        builder.proxyAuthenticator(ProxyAuthenticator(TAG, config.username, config.password))
                    }
                    if (proxy?.type() == Proxy.Type.SOCKS) {
                        builder.dns(socksProxyDns)
                    }
                }
            }
            RouteMode.AUTO -> {
                if (!proxyEnabled) {
                    builder.proxy(Proxy.NO_PROXY)
                } else {
                    builder.proxySelector(object : ProxySelector() {
                        override fun select(uri: URI?): List<Proxy> {
                            val host = uri?.host ?: ""
                            if (!config.enabled || !config.bypassLan || !ProxyManager.isLanAddress(host)) {
                                return mutableListOf(proxy)
                            }
                            return mutableListOf(Proxy.NO_PROXY)
                        }

                        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                            Log.e(TAG, "外部请求代理连接失败: $uri", ioe)
                        }
                    })
                    if (config.hasCredentials) {
                        builder.proxyAuthenticator(ProxyAuthenticator(TAG, config.username, config.password))
                    }
                    if (proxy?.type() == Proxy.Type.SOCKS) {
                        builder.dns(socksProxyDns)
                    }
                }
            }
        }
    }

    private fun buildClient(
        connectTimeoutSeconds: Long,
        readTimeoutSeconds: Long,
        writeTimeoutSeconds: Long,
        routeMode: RouteMode,
        allowUnsafeSsl: Boolean,
        ignoreServerDirectOnly: Boolean,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
        if (allowUnsafeSsl) {
            val (socketFactory, trustManager) = createUnsafeSslContext()
            builder.sslSocketFactory(socketFactory, trustManager)
            builder.hostnameVerifier(TrustAllHostnameVerifier())
        }
        applyProxyRoute(builder, routeMode, ignoreServerDirectOnly)
        return builder.build()
    }

    fun createApiClient(
        routeMode: RouteMode = RouteMode.AUTO,
        allowUnsafeSsl: Boolean = false,
        ignoreServerDirectOnly: Boolean = false,
    ): OkHttpClient = buildClient(10, 15, 15, routeMode, allowUnsafeSsl, ignoreServerDirectOnly)

    fun createDownloadClient(
        routeMode: RouteMode = RouteMode.AUTO,
        allowUnsafeSsl: Boolean = false,
        ignoreServerDirectOnly: Boolean = false,
    ): OkHttpClient = buildClient(20, 120, 30, routeMode, allowUnsafeSsl, ignoreServerDirectOnly)
}
