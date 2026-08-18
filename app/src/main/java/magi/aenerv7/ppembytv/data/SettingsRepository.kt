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

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 应用设置（代理、扫码端口等）持久化。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val PROXY = stringPreferencesKey("proxy_json")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    /** 获取或创建持久化的设备 ID（用于 Emby 认证头） */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.settingsDataStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val id = java.util.UUID.randomUUID().toString()
        context.settingsDataStore.edit { prefs -> prefs[Keys.DEVICE_ID] = id }
        return id
    }

    val proxy: Flow<ProxySettings> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[Keys.PROXY] ?: return@map ProxySettings()
        try {
            HttpClients.json().decodeFromString<ProxySettings>(raw)
        } catch (e: Exception) {
            ProxySettings()
        }
    }

    suspend fun getProxy(): ProxySettings = proxy.first()

    suspend fun setProxy(settings: ProxySettings) {
        val json = HttpClients.json().encodeToString<ProxySettings>(settings)
        context.settingsDataStore.edit { prefs -> prefs[Keys.PROXY] = json }
    }
}
