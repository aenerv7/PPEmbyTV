package magi.aenerv7.ppembytv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import magi.aenerv7.ppembytv.api.HttpClients
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import java.util.UUID

private val Context.serverDataStore by preferencesDataStore(name = "servers")

/**
 * 多服务器管理：服务器列表与“上次使用”持久化（DataStore + JSON）。
 */
class ServerRepository(private val context: Context) {

    private object Keys {
        val SERVERS = stringPreferencesKey("servers_json")
        val LAST_USED = stringPreferencesKey("last_used_server_id")
    }

    val servers: Flow<List<ServerConfig>> = context.serverDataStore.data.map { prefs ->
        val raw = prefs[Keys.SERVERS] ?: return@map emptyList()
        try {
            HttpClients.json().decodeFromString<List<ServerConfig>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val lastUsedServerId: Flow<String?> = context.serverDataStore.data.map { it[Keys.LAST_USED] }

    suspend fun getServers(): List<ServerConfig> = servers.first()

    suspend fun getServer(id: String): ServerConfig? =
        servers.first().firstOrNull { it.id == id }

    suspend fun getLastUsedServer(): ServerConfig? {
        val id = lastUsedServerId.first() ?: return null
        return getServer(id)
    }

    suspend fun addServer(config: ServerConfig) {
        val list = getServers().toMutableList()
        list.removeAll { it.id == config.id }
        list.add(config)
        persist(list)
    }

    suspend fun updateServer(config: ServerConfig) = addServer(config)

    suspend fun removeServer(id: String) {
        val list = getServers().toMutableList()
        list.removeAll { it.id == id }
        persist(list)
        if (lastUsedServerId.first() == id) {
            setLastUsedServerId(null)
        }
    }

    suspend fun setLastUsedServerId(id: String?) {
        context.serverDataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.LAST_USED) else prefs[Keys.LAST_USED] = id
        }
    }

    private suspend fun persist(list: List<ServerConfig>) {
        val json = HttpClients.json().encodeToString<List<ServerConfig>>(list)
        context.serverDataStore.edit { prefs -> prefs[Keys.SERVERS] = json }
    }

    fun generateServerId(): String = UUID.randomUUID().toString()
}
