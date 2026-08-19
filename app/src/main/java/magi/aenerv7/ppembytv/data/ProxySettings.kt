package magi.aenerv7.ppembytv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.proxyDataStore: DataStore<Preferences> by preferencesDataStore(name = "proxy_settings")

class ProxySettings(private val context: Context) {

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val KEY_PROTOCOL = stringPreferencesKey("proxy_protocol")
        val KEY_HOST = stringPreferencesKey("proxy_host")
        val KEY_PORT = intPreferencesKey("proxy_port")
        val KEY_USERNAME = stringPreferencesKey("proxy_username")
        val KEY_PASSWORD = stringPreferencesKey("proxy_password")
        val KEY_BYPASS_LAN = booleanPreferencesKey("proxy_bypass_lan")
    }

    val proxyConfigFlow: Flow<ProxyConfig> = context.proxyDataStore.data.map { prefs ->
        val protocolName = prefs[KEY_PROTOCOL] ?: ProxyProtocol.HTTP.name
        val protocol = runCatching { ProxyProtocol.valueOf(protocolName) }
            .getOrDefault(ProxyProtocol.HTTP)
        ProxyConfig(
            enabled = prefs[KEY_ENABLED] ?: false,
            protocol = protocol,
            host = prefs[KEY_HOST] ?: "",
            port = prefs[KEY_PORT] ?: 7890,
            username = prefs[KEY_USERNAME] ?: "",
            password = prefs[KEY_PASSWORD] ?: "",
            bypassLan = prefs[KEY_BYPASS_LAN] ?: true,
        )
    }

    suspend fun saveProxyConfig(config: ProxyConfig) {
        context.proxyDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = config.enabled
            prefs[KEY_PROTOCOL] = config.protocol.name
            prefs[KEY_HOST] = config.host
            prefs[KEY_PORT] = config.port
            prefs[KEY_USERNAME] = config.username
            prefs[KEY_PASSWORD] = config.password
            prefs[KEY_BYPASS_LAN] = config.bypassLan
        }
    }
}
