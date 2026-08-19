package magi.aenerv7.ppembytv.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import magi.aenerv7.ppembytv.data.model.ServerConfig
import java.util.UUID

/**
 * Persists the list of Emby server configurations (as a Gson JSON array under
 * [KEY_SERVERS]), the last-used server id, and an optional explicit server order.
 */
class ServerPreferences(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        cleanupRemovedFeatureData()
    }

    fun clearAllServers() {
        prefs.edit()
            .remove(KEY_SERVERS)
            .remove(KEY_LAST_USED_SERVER)
            .remove(KEY_SERVER_ORDER)
            .apply()
    }

    fun clearLastUsedServer() {
        prefs.edit().remove(KEY_LAST_USED_SERVER).apply()
    }

    fun createDefaultServer(): ServerConfig = ServerConfig(
        id = generateServerId(),
        alias = "我的Emby服务器",
        protocol = "http",
        host = "192.168.1.1",
        port = 8096,
        username = "admin",
        password = "",
    )

    fun deleteServer(id: String) {
        val servers = readStoredServers().toMutableList()
        servers.removeAll { it.id == id }
        prefs.edit().putString(KEY_SERVERS, gson.toJson(servers)).apply()
        if (prefs.contains(KEY_SERVER_ORDER)) {
            persistServerOrder(buildStoredOrderWithAllServerIds(servers.map { it.id }))
        }
        if (getLastUsedServerId() == id) {
            clearLastUsedServer()
        }
    }

    fun generateServerId(): String = UUID.randomUUID().toString()

    fun getAllServers(): List<ServerConfig> {
        val storedServers = readStoredServers()
        if (storedServers.isEmpty()) return emptyList()
        val storedOrder = readStoredServerOrder()
        if (storedOrder.isNullOrEmpty()) {
            return storedServers.sortedByDescending { it.lastLoginTime }
        }
        val byId = storedServers.associateBy { it.id }
        val ordered = storedOrder.mapNotNull { byId[it] }
        val rest = storedServers
            .filterNot { storedOrder.contains(it.id) }
            .sortedByDescending { it.lastLoginTime }
        return ordered + rest
    }

    fun getLastUsedServer(): ServerConfig? {
        val lastUsedServerId = getLastUsedServerId() ?: return null
        return getServerById(lastUsedServerId)
    }

    fun getLastUsedServerId(): String? = prefs.getString(KEY_LAST_USED_SERVER, null)

    fun getServerById(id: String): ServerConfig? = getAllServers().firstOrNull { it.id == id }

    fun saveServer(config: ServerConfig) {
        val servers = readStoredServers().toMutableList()
        val index = servers.indexOfFirst { it.id == config.id }
        if (index != -1) {
            servers[index] = config
        } else {
            servers.add(config)
        }
        prefs.edit().putString(KEY_SERVERS, gson.toJson(servers)).apply()
        if (prefs.contains(KEY_SERVER_ORDER)) {
            persistServerOrder(buildStoredOrderWithAllServerIds(servers.map { it.id }))
        }
    }

    fun saveServerOrder(orderedServerIds: List<String>) {
        val allIds = readStoredServers().map { it.id }
        if (allIds.isEmpty()) return
        persistServerOrder(buildStoredOrderWithAllServerIds(orderedServerIds, allIds))
    }

    /**
     * 整体替换服务器列表（WebDAV 下载同步用）；可选设置最后使用服务器。
     * 显式排序被清除（会按最后登录时间重排）。
     */
    fun replaceAllServers(servers: List<ServerConfig>, lastUsedServerId: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_SERVERS, gson.toJson(servers))
        if (lastUsedServerId != null) {
            editor.putString(KEY_LAST_USED_SERVER, lastUsedServerId)
        } else {
            editor.remove(KEY_LAST_USED_SERVER)
        }
        editor.remove(KEY_SERVER_ORDER)
        editor.apply()
    }

    fun setLastUsedServerId(serverId: String) {
        prefs.edit().putString(KEY_LAST_USED_SERVER, serverId).apply()
    }

    fun updateServerLoginInfo(serverId: String, userId: String, accessToken: String) {
        val server = getServerById(serverId) ?: return
        server.userId = userId
        server.accessToken = accessToken
        server.lastLoginTime = System.currentTimeMillis()
        server.isVerified = true
        saveServer(server)
        setLastUsedServerId(serverId)
    }

    fun updateServerPlaybackAccess(serverId: String, accessTime: Long = System.currentTimeMillis()) {
        val server = getServerById(serverId) ?: return
        saveServer(server.copy(lastPlaybackAccessTime = accessTime))
    }

    /** Keeps only order entries that still exist, then appends every missing id at the end. */
    private fun buildStoredOrderWithAllServerIds(
        orderedServerIds: List<String>,
        allServerIds: List<String>,
    ): List<String> {
        val existing = orderedServerIds.filter { allServerIds.contains(it) }.distinct()
        val missing = allServerIds.filterNot { existing.contains(it) }
        return existing + missing
    }

    private fun buildStoredOrderWithAllServerIds(allServerIds: List<String>): List<String> {
        val storedOrder = readStoredServerOrder() ?: emptyList()
        return buildStoredOrderWithAllServerIds(storedOrder, allServerIds)
    }

    /**
     * One-time migration: strips the removed "strictRequestMode" field from the
     * stored JSON (if present) and wipes the legacy prefs file of that feature.
     */
    private fun cleanupRemovedFeatureData() {
        val json = prefs.getString(KEY_SERVERS, null)
        if (json != null && json.contains(LEGACY_REMOVED_SERVER_FIELD)) {
            parseStoredServers(json)?.let { parsed ->
                prefs.edit().putString(KEY_SERVERS, gson.toJson(parsed)).apply()
            }
        }
        appContext.getSharedPreferences(LEGACY_REMOVED_FEATURE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /**
     * Parses the stored server JSON; returns null when it cannot be parsed.
     * Also repairs each entry: the active backup-route id is kept only when it
     * still resolves inside that entry's (normalized) backup routes.
     */
    private fun parseStoredServers(json: String): List<ServerConfig>? {
        return try {
            val type = object : TypeToken<List<ServerConfig>>() {}.type
            val list: List<ServerConfig> = gson.fromJson(json, type) ?: emptyList()
            list.map { serverConfig: ServerConfig ->
                val backupRoutesSafe = serverConfig.backupRoutesSafe
                val activeBackupRouteId = serverConfig.activeBackupRouteId
                    ?.takeIf { id -> backupRoutesSafe.any { it.id == id } }
                serverConfig.copy(
                    backupRoutes = backupRoutesSafe,
                    activeBackupRouteId = activeBackupRouteId,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun persistServerOrder(orderedServerIds: List<String>) {
        prefs.edit().putString(KEY_SERVER_ORDER, gson.toJson(orderedServerIds)).apply()
    }

    private fun readStoredServerOrder(): List<String>? {
        val json = prefs.getString(KEY_SERVER_ORDER, null) ?: return null
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(json, type) ?: return null
            list.distinct()
        } catch (e: Exception) {
            null
        }
    }

    private fun readStoredServers(): List<ServerConfig> {
        val json = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return parseStoredServers(json) ?: emptyList()
    }

    companion object {
        private const val KEY_LAST_USED_SERVER = "last_used_server"
        private const val KEY_SERVERS = "servers"
        private const val KEY_SERVER_ORDER = "server_order"
        private const val LEGACY_REMOVED_FEATURE_PREFS_NAME = "strict_request_mode_logs"
        private const val LEGACY_REMOVED_SERVER_FIELD = "\"strictRequestMode\""
        private const val PREFS_NAME = "server_configs"
    }
}
