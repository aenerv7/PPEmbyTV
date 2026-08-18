package magi.aenerv7.ppembytv.data

import kotlinx.serialization.Serializable

/**
 * 单个 Emby 服务器配置（对应参考项目中 ServerConfig 的简化版）。
 */
@Serializable
data class ServerConfig(
    val id: String,
    val alias: String,
    val protocol: String = "http",      // http | https
    val host: String,
    val port: Int = 8096,
    val path: String = "",              // 可选子路径，如 "emby"
    val username: String = "",
    val password: String = "",
    val userId: String = "",
    val accessToken: String = "",
    val deviceId: String = "",
    val directOnly: Boolean = false,    // 仅直连（不使用代理）
    val trustAllCerts: Boolean = false, // 信任所有 SSL 证书
    val note: String = "",
    val isLoggedIn: Boolean = false,
    val lastLoginTime: Long = 0L,
) {
    /** 形如 http://host:port 的服务器根地址 */
    fun getBaseUrl(): String {
        val p = if (port > 0) ":$port" else ""
        return "$protocol://$host$p"
    }

    /** 形如 http://host:port/path/ 的 Emby API 根地址（末尾带斜杠） */
    fun getFullUrl(): String {
        val base = getBaseUrl()
        val p = path.trim().trim('/')
        return if (p.isEmpty()) "$base/" else "$base/$p/"
    }
}
