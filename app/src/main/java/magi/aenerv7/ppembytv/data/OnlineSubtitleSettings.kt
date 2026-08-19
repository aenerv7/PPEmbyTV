package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences

/** 在线字幕（Assrt）配置持久化。 */
class OnlineSubtitleSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): OnlineSubtitleConfig = OnlineSubtitleConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        assrtApiToken = prefs.getString(KEY_API_TOKEN, "") ?: "",
        assrtApiProtocol = runCatching {
            AssrtApiProtocol.valueOf(prefs.getString(KEY_PROTOCOL, "HTTPS") ?: "HTTPS")
        }.getOrDefault(AssrtApiProtocol.HTTPS),
    )

    fun save(config: OnlineSubtitleConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_API_TOKEN, config.assrtApiToken)
            .putString(KEY_PROTOCOL, config.assrtApiProtocol.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "online_subtitle_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_API_TOKEN = "assrt_api_token"
        private const val KEY_PROTOCOL = "assrt_api_protocol"
    }
}
