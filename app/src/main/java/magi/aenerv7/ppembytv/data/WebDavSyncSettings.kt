package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences

/** WebDAV 同步配置持久化。 */
class WebDavSyncSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WebDavSyncConfig = WebDavSyncConfig(
        serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
        username = prefs.getString(KEY_USERNAME, "") ?: "",
        password = prefs.getString(KEY_PASSWORD, "") ?: "",
        syncServerConfigurations = prefs.getBoolean(KEY_SYNC_SERVERS, true),
        syncAppSettings = prefs.getBoolean(KEY_SYNC_SETTINGS, true),
    )

    fun save(config: WebDavSyncConfig) {
        prefs.edit()
            .putString(KEY_SERVER_URL, config.serverUrl)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putBoolean(KEY_SYNC_SERVERS, config.syncServerConfigurations)
            .putBoolean(KEY_SYNC_SETTINGS, config.syncAppSettings)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "webdav_sync_settings"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SYNC_SERVERS = "sync_server_configurations"
        private const val KEY_SYNC_SETTINGS = "sync_app_settings"
    }
}
