package magi.aenerv7.ppembytv.dlna

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * DataStore-backed DLNA settings.
 *
 * Ported from `com.dh.myembyapp.dlna.DlnaSettings`. Keys and default values kept EXACT
 * (default device name "皮皮 TV", all booleans default false). `configSync` blocks on the
 * first emission of [configFlow] and matches the original `getConfigSync()`.
 */
class DlnaSettings(private val context: Context) {

    val configFlow: Flow<DlnaConfig> = context.dataStore.data.map { prefs ->
        DlnaConfig(
            enabled = prefs[KEY_ENABLED] ?: false,
            deviceName = prefs[KEY_DEVICE_NAME] ?: "皮皮 TV",
            autoPlay = prefs[KEY_AUTO_PLAY] ?: false,
            useProxyByDefault = prefs[KEY_USE_PROXY_DEFAULT] ?: false,
            trustAllCerts = prefs[KEY_TRUST_ALL_CERTS] ?: false,
        )
    }

    val configSync: DlnaConfig
        get() = runBlocking { configFlow.first() }

    suspend fun saveConfig(config: DlnaConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = config.enabled
            prefs[KEY_DEVICE_NAME] = config.deviceName
            prefs[KEY_AUTO_PLAY] = config.autoPlay
            prefs[KEY_USE_PROXY_DEFAULT] = config.useProxyByDefault
            prefs[KEY_TRUST_ALL_CERTS] = config.trustAllCerts
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_NAME] = name
        }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "dlna_settings")

        private val KEY_ENABLED = booleanPreferencesKey("dlna_enabled")
        private val KEY_DEVICE_NAME = stringPreferencesKey("dlna_device_name")
        private val KEY_AUTO_PLAY = booleanPreferencesKey("dlna_auto_play")
        private val KEY_USE_PROXY_DEFAULT = booleanPreferencesKey("dlna_use_proxy_default")
        private val KEY_TRUST_ALL_CERTS = booleanPreferencesKey("dlna_trust_all_certs")
    }
}
