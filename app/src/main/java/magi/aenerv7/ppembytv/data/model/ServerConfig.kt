package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ServerConfig(
    @SerializedName("id") val id: String,
    @SerializedName("alias") val alias: String,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int,
    @SerializedName("path") val path: String? = null,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("userId") var userId: String? = null,
    @SerializedName("accessToken") var accessToken: String? = null,
    @SerializedName("deviceId") var deviceId: String? = null,
    @SerializedName("lastLoginTime") var lastLoginTime: Long = 0L,
    @SerializedName("isVerified") var isVerified: Boolean = false,
    @SerializedName("directOnly") val directOnly: Boolean = false,
    @SerializedName("enableStrmDirectPlay") val enableStrmDirectPlay: Boolean = false,
    @Deprecated("不再使用，保留仅为向后兼容")
    @SerializedName("useEmbyPrefix") var useEmbyPrefix: Boolean = false,
    @SerializedName("trustAllCerts") val trustAllCerts: Boolean = false,
    @SerializedName("backupRoutes") val backupRoutes: List<BackupRouteConfig>? = null,
    @SerializedName("activeBackupRouteId") val activeBackupRouteId: String? = null,
    @SerializedName("customIconPath") val customIconPath: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("lastPlaybackAccessTime") val lastPlaybackAccessTime: Long = 0L,
) {
    val backupRoutesSafe: List<BackupRouteConfig>
        get() = (backupRoutes ?: emptyList()).map { normalizeBackupRouteConfig(it) }

    val activeBackupRoute: BackupRouteConfig?
        get() = activeBackupRouteId?.let { id -> backupRoutesSafe.find { it.id == id } }

    val currentRouteDisplayName: String
        get() {
            val alias = activeBackupRoute?.alias?.trim()
            return if (alias.isNullOrEmpty()) "主线路" else alias
        }

    val displayAddress: String
        get() = "$protocol://$host:$port${normalizeServerPath(path)}"

    val effectiveServerConfig: ServerConfig
        get() {
            val routes = backupRoutesSafe
            val route = activeBackupRouteId?.let { id -> routes.find { it.id == id } }
            return if (route == null) {
                copy(backupRoutes = routes, activeBackupRouteId = null)
            } else {
                copy(
                    protocol = route.protocol,
                    host = route.host,
                    port = route.port,
                    path = route.path,
                    directOnly = route.directOnly,
                    backupRoutes = routes,
                    activeBackupRouteId = route.id,
                )
            }
        }

    val fullUrl: String
        get() = "$protocol://$host:$port${normalizeServerPath(path)}/"

    fun isLoggedIn(): Boolean = userId != null && accessToken != null
}
