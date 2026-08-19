package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Trakt 配置与 OAuth 令牌持久化（对应参考实现里混淆层的 Trakt 配置 + 令牌存储）。
 * 客户端 ID/Secret 取自反编译产物中的字面量（R8 不混淆字符串常量）。
 */
class TraktSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, DEFAULT_CLIENT_ID) ?: DEFAULT_CLIENT_ID
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value).apply()

    var clientSecret: String
        get() = prefs.getString(KEY_CLIENT_SECRET, DEFAULT_CLIENT_SECRET) ?: DEFAULT_CLIENT_SECRET
        set(value) = prefs.edit().putString(KEY_CLIENT_SECRET, value).apply()

    var accessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String
        get() = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var promptCloudProgress: Boolean
        get() = prefs.getBoolean(KEY_PROMPT_CLOUD_PROGRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_PROMPT_CLOUD_PROGRESS, value).apply()

    var syncLocalProgressToTrakt: Boolean
        get() = prefs.getBoolean(KEY_SYNC_LOCAL_PROGRESS, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_LOCAL_PROGRESS, value).apply()

    var minimumProgressDifferenceSeconds: Int
        get() = prefs.getInt(KEY_MIN_PROGRESS_DIFF, 60)
        set(value) = prefs.edit().putInt(KEY_MIN_PROGRESS_DIFF, value).apply()

    val isAuthorized: Boolean
        get() = accessToken.isNotBlank()

    fun saveToken(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun clearAuth() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }

    companion object {
        const val DEFAULT_CLIENT_ID = "1c6390b346287cb8aad251da052645aa6e57f4e2dd67ae9d9ee9c7217cc513e6"
        const val DEFAULT_CLIENT_SECRET = "0adc6e4aa2ddd7858eb346db6467d9678709322badd984c655514c97c36a8847"

        private const val PREFS_NAME = "trakt_settings"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PROMPT_CLOUD_PROGRESS = "prompt_cloud_progress"
        private const val KEY_SYNC_LOCAL_PROGRESS = "sync_local_progress_to_trakt"
        private const val KEY_MIN_PROGRESS_DIFF = "minimum_progress_difference_seconds"
    }
}
