package magi.aenerv7.ppembytv.data

import kotlinx.serialization.Serializable

/**
 * 代理设置（对应参考项目中的 http/socks5 代理配置）。
 */
@Serializable
data class ProxySettings(
    val enabled: Boolean = false,
    val type: String = "http",          // http | socks
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val bypassLan: Boolean = true,      // 局域网地址不走代理
)

fun ProxySettings.isValid(): Boolean =
    host.isNotBlank() && port in 1..65535
