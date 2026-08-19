package magi.aenerv7.ppembytv.data

import android.util.Log
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale

object ProxyManager {

    private const val TAG = "ProxyManager"

    @Volatile
    var currentProxy: Proxy? = null

    @Volatile
    var currentConfig: ProxyConfig = ProxyConfig()

    @Volatile
    var serverDirectOnly: Boolean = false

    @Volatile
    private var socksAuthActive: Boolean = false

    private val socksAuthenticator = object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication? {
            val config = currentConfig
            val proxy = currentProxy
            val requestorType = requestorType
            val requestingProtocol = requestingProtocol ?: ""
            val requestingHost = requestingHost ?: ""
            val requestingPort = requestingPort
            val address = proxy?.address() as? InetSocketAddress
            if ((requestorType != Authenticator.RequestorType.PROXY && !requestingProtocol.startsWith("SOCKS5")) ||
                config.protocol != ProxyProtocol.SOCKS5 ||
                !config.hasCredentials ||
                address == null ||
                !requestingHost.startsWith(address.hostString) ||
                requestingPort != address.port
            ) {
                return null
            }
            Log.d(TAG, "SOCKS5 认证请求: 为当前代理提供凭据")
            return PasswordAuthentication(config.username, config.password.toCharArray())
        }
    }

    fun applyProxyConfig(config: ProxyConfig) {
        currentConfig = config
        Log.d(TAG, "========== 开始应用代理配置 ==========")
        Log.d(TAG, "代理配置: enabled=${config.enabled}, protocol=${config.protocol}, host=${config.host}, port=${config.port}, bypassLan=${config.bypassLan}")

        if (config.enabled) {
            val type = when (config.protocol) {
                ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> Proxy.Type.HTTP
                ProxyProtocol.SOCKS5 -> Proxy.Type.SOCKS
            }
            currentProxy = try {
                Proxy(type, InetSocketAddress(config.host, config.port))
            } catch (e: Exception) {
                Log.e(TAG, "创建代理对象失败", e)
                null
            }
            if (config.protocol == ProxyProtocol.SOCKS5 && config.hasCredentials) {
                Authenticator.setDefault(socksAuthenticator)
                socksAuthActive = true
            } else if (socksAuthActive) {
                Authenticator.setDefault(null)
                socksAuthActive = false
            }
            RetrofitClient.updateProxy(currentProxy)
        } else {
            currentProxy = null
            if (socksAuthActive) {
                Authenticator.setDefault(null)
                socksAuthActive = false
            }
            RetrofitClient.updateProxy(null)
        }
        Log.d(TAG, "========== 代理配置应用完成 ==========")
    }

    fun isLanAddress(host: String): Boolean {
        val h = host.lowercase(Locale.ROOT).trim()
        if (h == "localhost" || h == "127.0.0.1" || h.startsWith("127.") || h == "::1") {
            return true
        }
        return listOf(
            Regex("^192\\.168\\.\\d{1,3}\\.\\d{1,3}$"),
            Regex("^10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"),
            Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d{1,3}\\.\\d{1,3}$"),
        ).any { it.matches(h) }
    }

    fun setDirectOnly(directOnly: Boolean) {
        Log.d(TAG, "设置服务器仅直连模式: $directOnly")
        serverDirectOnly = directOnly
        if (directOnly) {
            currentProxy = null
            if (socksAuthActive) {
                Authenticator.setDefault(null)
                socksAuthActive = false
            }
            RetrofitClient.updateProxy(null)
        } else if (currentConfig.enabled) {
            applyProxyConfig(currentConfig)
        }
    }
}
