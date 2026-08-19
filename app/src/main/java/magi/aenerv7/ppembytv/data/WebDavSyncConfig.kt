package magi.aenerv7.ppembytv.data

/**
 * WebDAV 同步配置。
 * 同步文件固定保存在目录 PPEmbyTV 下的 sync-config.json。
 */
data class WebDavSyncConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val syncServerConfigurations: Boolean,
    val syncAppSettings: Boolean,
) {
    fun normalized(): WebDavSyncConfig = WebDavSyncConfig(
        serverUrl = normalizeWebDavUrl(serverUrl),
        username = username.trim(),
        password = password.trim(),
        syncServerConfigurations = syncServerConfigurations,
        syncAppSettings = syncAppSettings,
    )
}

/**
 * 归一化 WebDAV 服务器地址：去除首尾空白与结尾的连续斜杠；
 * 若地址不是单纯的协议头（如 "https://"），则在末尾补上 "/"。
 */
private fun normalizeWebDavUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""
    val withoutTrailingSlashes = Regex("/+$").replace(trimmed, "")
    return if (withoutTrailingSlashes.endsWith("://")) trimmed else "$withoutTrailingSlashes/"
}
