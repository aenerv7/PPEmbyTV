package magi.aenerv7.ppembytv.data.preferences

import android.content.Context
import magi.aenerv7.ppembytv.data.model.AggregateResultSortMode
import magi.aenerv7.ppembytv.data.model.AudioLanguagePreference
import magi.aenerv7.ppembytv.data.model.AudioPrioritySortType
import magi.aenerv7.ppembytv.data.model.AudioVersionPrioritySettings
import magi.aenerv7.ppembytv.data.model.PlayerDefaultViewMode
import magi.aenerv7.ppembytv.data.model.PlayerFrameRateMatchingMode
import magi.aenerv7.ppembytv.data.model.PlayerResizeMode
import magi.aenerv7.ppembytv.data.model.SubtitleFormatPriorityType
import magi.aenerv7.ppembytv.data.model.SubtitleLanguagePriorityType
import magi.aenerv7.ppembytv.data.model.SubtitlePrioritySortType
import magi.aenerv7.ppembytv.data.model.SubtitleVersionPrioritySettings
import magi.aenerv7.ppembytv.data.model.SystemNetworkSpeedDisplayMode
import magi.aenerv7.ppembytv.data.model.SystemNetworkSpeedPosition
import magi.aenerv7.ppembytv.data.model.SystemTimeDisplayMode
import magi.aenerv7.ppembytv.data.model.VideoPriorityRule
import magi.aenerv7.ppembytv.data.model.VideoPrioritySortType
import magi.aenerv7.ppembytv.data.model.VideoQualityStandard
import magi.aenerv7.ppembytv.data.model.VideoValueSortDirection
import magi.aenerv7.ppembytv.data.model.VideoVersionPrioritySettings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Global user preferences backed by the "emby_prefs" SharedPreferences file.
 * Complex values (video rules, subtitle/audio priority maps) are stored as JSON
 * strings via [org.json]; enums use their [storageValue] or [name].
 */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class AssSubtitleEnhancementGuard(
        val mediaId: String?,
        val subtitleLabel: String?,
        val armedAtMs: Long,
    )

    fun armAssSubtitleEnhancementGuard(mediaId: String, subtitleLabel: String) {
        prefs.edit()
            .putBoolean(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_ACTIVE, true)
            .putString(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_MEDIA_ID, mediaId)
            .putString(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_SUBTITLE_LABEL, subtitleLabel)
            .putLong(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_TIME_MS, System.currentTimeMillis())
            .commit()
    }

    fun clearAssSubtitleEnhancementGuard() {
        prefs.edit()
            .remove(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_ACTIVE)
            .remove(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_MEDIA_ID)
            .remove(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_SUBTITLE_LABEL)
            .remove(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_TIME_MS)
            .commit()
    }

    fun clearLoginInfo() {
        prefs.edit().clear().apply()
    }

    fun consumeRecentAssSubtitleEnhancementGuard(maxAgeMs: Long = 1_800_000L): AssSubtitleEnhancementGuard? {
        val active = prefs.getBoolean(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_ACTIVE, false)
        val armedAtMs = prefs.getLong(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_TIME_MS, 0L)
        val guard = if (active && armedAtMs > 0) {
            AssSubtitleEnhancementGuard(
                mediaId = prefs.getString(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_MEDIA_ID, null),
                subtitleLabel = prefs.getString(KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_SUBTITLE_LABEL, null),
                armedAtMs = armedAtMs,
            )
        } else {
            null
        }
        if (guard != null) {
            clearAssSubtitleEnhancementGuard()
        }
        if (guard != null && System.currentTimeMillis() - guard.armedAtMs <= maxAgeMs) {
            return guard
        }
        return null
    }

    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    val adminDebugFeaturesUnlocked: Boolean
        get() = prefs.getBoolean(KEY_ADMIN_DEBUG_FEATURES_UNLOCKED, false)

    val aggregateResultSortMode: AggregateResultSortMode
        get() = AggregateResultSortMode.fromStorage(prefs.getString(KEY_AGGREGATE_RESULT_SORT_MODE, null))

    fun getAppUpdateStartupLastCheckTimeMs(currentVersion: String? = null): Long {
        if (currentVersion == null || isAppUpdateStartupCacheForVersion(currentVersion)) {
            return prefs.getLong(KEY_APP_UPDATE_STARTUP_LAST_CHECK_TIME_MS, 0L)
        }
        return 0L
    }

    fun getAppUpdateStartupLastHasUpdate(currentVersion: String? = null): Boolean {
        if (currentVersion == null || isAppUpdateStartupCacheForVersion(currentVersion)) {
            return prefs.getBoolean(KEY_APP_UPDATE_STARTUP_LAST_HAS_UPDATE, false)
        }
        return false
    }

    val audioDelayMs: Int
        get() = normalizeAudioDelayMs(prefs.getInt(KEY_AUDIO_DELAY_MS, 0))

    val audioVersionPrioritySettings: AudioVersionPrioritySettings
        get() = AudioVersionPrioritySettings(
            sortTypes = parseAudioPrioritySortTypes(prefs.getString(KEY_AUDIO_PRIORITY_SORT_TYPES, null)),
            preferredLanguage = AudioLanguagePreference.fromStorageValue(
                prefs.getString(KEY_AUDIO_PREFERRED_LANGUAGE, null),
            ),
            aacPriority = prefs.getInt(KEY_AUDIO_PRIORITY_AAC, 1),
            ac3Priority = prefs.getInt(KEY_AUDIO_PRIORITY_AC3, 1),
            eac3Priority = prefs.getInt(KEY_AUDIO_PRIORITY_EAC3, 1),
            dtsPriority = prefs.getInt(KEY_AUDIO_PRIORITY_DTS, 1),
            truehdPriority = prefs.getInt(KEY_AUDIO_PRIORITY_TRUEHD, 1),
            flacPriority = prefs.getInt(KEY_AUDIO_PRIORITY_FLAC, 1),
        ).normalized()

    val disableTopNavFocusRefresh: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_TOP_NAV_FOCUS_REFRESH, true)

    val enableAggregateSearch: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_AGGREGATE_SEARCH, false)

    val enableAggregateSearchInServerSearch: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_AGGREGATE_SEARCH_IN_SERVER_SEARCH, false)

    val enableAggregateSearchVersionTags: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_AGGREGATE_SEARCH_VERSION_TAGS, false)

    val enableAppUpdateCheckOnStartup: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_APP_UPDATE_CHECK_ON_STARTUP, false)

    val enableAssSubtitleEnhancement: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_ASS_SUBTITLE_ENHANCEMENT, true)

    val enableClickableLibrarySectionTitle: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_CLICKABLE_LIBRARY_SECTION_TITLE, false)

    val enableDetailAggregateVersionRail: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_DETAIL_AGGREGATE_VERSION_RAIL, false)

    val enableHomeBackFocusServerButton: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_HOME_BACK_FOCUS_SERVER_BUTTON, false)

    val enableHomeUnplayedOnly: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_HOME_UNPLAYED_ONLY, false)

    val enableLibraryRowMoreButton: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_LIBRARY_ROW_MORE_BUTTON, false)

    val enablePlaybackStartupPoster: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_PLAYBACK_STARTUP_POSTER, true)

    val enableSpecialFeatures: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_SPECIAL_FEATURES, false)

    val enableTransparentTopNavButtons: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_TRANSPARENT_TOP_NAV_BUTTONS, true)

    val lastUsedAggregateHub: Boolean
        get() = prefs.getBoolean(KEY_LAST_USED_AGGREGATE_HUB, false)

    val playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f).coerceIn(0.5f, 3.0f)

    val playerDefaultViewMode: PlayerDefaultViewMode
        get() = PlayerDefaultViewMode.fromStorageValue(
            prefs.getString(KEY_PLAYER_DEFAULT_VIEW_MODE, PlayerDefaultViewMode.SURFACE.storageValue),
        )

    val playerFrameRateMatchingMode: PlayerFrameRateMatchingMode
        get() = PlayerFrameRateMatchingMode.fromStorageValue(
            prefs.getString(
                KEY_PLAYER_FRAME_RATE_MATCHING_MODE,
                PlayerFrameRateMatchingMode.SEAMLESS_ONLY.storageValue,
            ),
        )

    val playerResizeMode: PlayerResizeMode
        get() = PlayerResizeMode.fromStorageValue(
            prefs.getString(KEY_PLAYER_RESIZE_MODE, PlayerResizeMode.DEFAULT.storageValue),
        )

    val qrNetworkInterfaceName: String?
        get() = prefs.getString(KEY_QR_NETWORK_INTERFACE_NAME, null)

    val seekIntervalSec: Int
        get() = (prefs.getInt(KEY_SEEK_INTERVAL_SEC, 10).coerceIn(5, 60) / 5) * 5

    val serverIconLibraryUrl: String?
        get() {
            val raw = prefs.getString(KEY_SERVER_ICON_LIBRARY_URL, null) ?: return null
            val trimmed = raw.trim()
            return if (trimmed.isEmpty()) null else trimmed
        }

    val serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)

    val subtitleVersionPrioritySettings: SubtitleVersionPrioritySettings
        get() = SubtitleVersionPrioritySettings(
            sortTypes = parseSubtitlePrioritySortTypes(prefs.getString(KEY_SUBTITLE_PRIORITY_SORT_TYPES, null)),
            languagePriorities = parseSubtitleLanguagePriorities(prefs.getString(KEY_SUBTITLE_LANGUAGE_PRIORITIES, null)),
            formatPriorities = parseSubtitleFormatPriorities(prefs.getString(KEY_SUBTITLE_FORMAT_PRIORITIES, null)),
        ).normalized()

    val systemNetworkSpeedDisplayMode: SystemNetworkSpeedDisplayMode
        get() = SystemNetworkSpeedDisplayMode.fromStorageValue(
            prefs.getString(KEY_SYSTEM_NETWORK_SPEED_DISPLAY_MODE, null),
        ) ?: SystemNetworkSpeedDisplayMode.MENU_ONLY

    val systemNetworkSpeedPosition: SystemNetworkSpeedPosition
        get() = SystemNetworkSpeedPosition.fromStorageValue(
            prefs.getString(KEY_SYSTEM_NETWORK_SPEED_POSITION, null),
        ) ?: SystemNetworkSpeedPosition.TOP_RIGHT

    val systemTimeDisplayMode: SystemTimeDisplayMode
        get() = SystemTimeDisplayMode.fromStorageValue(
            prefs.getString(KEY_SYSTEM_TIME_DISPLAY_MODE, null),
        ) ?: SystemTimeDisplayMode.MENU_ONLY

    val userId: String?
        get() = prefs.getString(KEY_USER_ID, null)

    val username: String?
        get() = prefs.getString(KEY_USERNAME, null)

    val videoVersionPrioritySettings: VideoVersionPrioritySettings
        get() = VideoVersionPrioritySettings(
            rules = parseVideoPriorityRules(prefs.getString(KEY_VIDEO_PRIORITY_RULES, null)),
        ).normalized()

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun saveAdminDebugFeaturesUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_ADMIN_DEBUG_FEATURES_UNLOCKED, unlocked).apply()
    }

    fun saveAggregateResultSortMode(mode: AggregateResultSortMode) {
        prefs.edit().putString(KEY_AGGREGATE_RESULT_SORT_MODE, mode.name).apply()
    }

    fun saveAppUpdateStartupCheckCache(checkedAtMs: Long, hasUpdate: Boolean, currentVersion: String) {
        prefs.edit()
            .putLong(KEY_APP_UPDATE_STARTUP_LAST_CHECK_TIME_MS, checkedAtMs)
            .putBoolean(KEY_APP_UPDATE_STARTUP_LAST_HAS_UPDATE, hasUpdate)
            .putString(KEY_APP_UPDATE_STARTUP_LAST_CHECKED_VERSION, currentVersion)
            .apply()
    }

    fun saveAudioDelayMs(delayMs: Int) {
        prefs.edit().putInt(KEY_AUDIO_DELAY_MS, normalizeAudioDelayMs(delayMs)).apply()
    }

    fun saveAudioVersionPrioritySettings(settings: AudioVersionPrioritySettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putString(
                KEY_AUDIO_PRIORITY_SORT_TYPES,
                normalized.sortTypes.joinToString(",") { it.storageValue },
            )
            .putString(KEY_AUDIO_PREFERRED_LANGUAGE, normalized.preferredLanguage.storageValue)
            .putInt(KEY_AUDIO_PRIORITY_AAC, normalized.aacPriority)
            .putInt(KEY_AUDIO_PRIORITY_AC3, normalized.ac3Priority)
            .putInt(KEY_AUDIO_PRIORITY_EAC3, normalized.eac3Priority)
            .putInt(KEY_AUDIO_PRIORITY_DTS, normalized.dtsPriority)
            .putInt(KEY_AUDIO_PRIORITY_TRUEHD, normalized.truehdPriority)
            .putInt(KEY_AUDIO_PRIORITY_FLAC, normalized.flacPriority)
            .apply()
    }

    fun saveDisableTopNavFocusRefresh(disable: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_TOP_NAV_FOCUS_REFRESH, disable).apply()
    }

    fun saveEnableAggregateSearch(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_AGGREGATE_SEARCH, enable).apply()
    }

    fun saveEnableAggregateSearchInServerSearch(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_AGGREGATE_SEARCH_IN_SERVER_SEARCH, enable).apply()
    }

    fun saveEnableAggregateSearchVersionTags(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_AGGREGATE_SEARCH_VERSION_TAGS, enable).apply()
    }

    fun saveEnableAppUpdateCheckOnStartup(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_APP_UPDATE_CHECK_ON_STARTUP, enable).apply()
    }

    fun saveEnableAssSubtitleEnhancement(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_ASS_SUBTITLE_ENHANCEMENT, enable).apply()
    }

    fun saveEnableClickableLibrarySectionTitle(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_CLICKABLE_LIBRARY_SECTION_TITLE, enable).apply()
    }

    fun saveEnableDetailAggregateVersionRail(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_DETAIL_AGGREGATE_VERSION_RAIL, enable).apply()
    }

    fun saveEnableHomeBackFocusServerButton(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_HOME_BACK_FOCUS_SERVER_BUTTON, enable).apply()
    }

    fun saveEnableHomeUnplayedOnly(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_HOME_UNPLAYED_ONLY, enable).apply()
    }

    fun saveEnableLibraryRowMoreButton(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_LIBRARY_ROW_MORE_BUTTON, enable).apply()
    }

    fun saveEnablePlaybackStartupPoster(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_PLAYBACK_STARTUP_POSTER, enable).apply()
    }

    fun saveEnableSpecialFeatures(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_SPECIAL_FEATURES, enable).apply()
    }

    fun saveEnableTransparentTopNavButtons(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_TRANSPARENT_TOP_NAV_BUTTONS, enable).apply()
    }

    fun saveLastUsedAggregateHub(enable: Boolean) {
        prefs.edit().putBoolean(KEY_LAST_USED_AGGREGATE_HUB, enable).apply()
    }

    fun saveLoginInfo(userId: String, username: String, accessToken: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed.coerceIn(0.5f, 3.0f)).apply()
    }

    fun savePlayerDefaultViewMode(mode: PlayerDefaultViewMode) {
        prefs.edit().putString(KEY_PLAYER_DEFAULT_VIEW_MODE, mode.storageValue).apply()
    }

    fun savePlayerFrameRateMatchingMode(mode: PlayerFrameRateMatchingMode) {
        prefs.edit().putString(KEY_PLAYER_FRAME_RATE_MATCHING_MODE, mode.storageValue).apply()
    }

    fun savePlayerResizeMode(mode: PlayerResizeMode) {
        prefs.edit().putString(KEY_PLAYER_RESIZE_MODE, mode.storageValue).apply()
    }

    fun saveQrNetworkInterfaceName(interfaceName: String) {
        prefs.edit().putString(KEY_QR_NETWORK_INTERFACE_NAME, interfaceName).apply()
    }

    fun saveSeekIntervalSec(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_INTERVAL_SEC, (seconds.coerceIn(5, 60) / 5) * 5).apply()
    }

    fun saveServerIconLibraryUrl(url: String?) {
        val trimmed = url?.trim()
        val value = trimmed?.takeIf { it.isNotEmpty() }
        val editor = prefs.edit()
        if (value == null) {
            editor.remove(KEY_SERVER_ICON_LIBRARY_URL)
        } else {
            editor.putString(KEY_SERVER_ICON_LIBRARY_URL, value)
        }
        editor.apply()
    }

    fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun saveSubtitleVersionPrioritySettings(settings: SubtitleVersionPrioritySettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putString(
                KEY_SUBTITLE_PRIORITY_SORT_TYPES,
                normalized.sortTypes.joinToString(",") { it.storageValue },
            )
            .putString(KEY_SUBTITLE_LANGUAGE_PRIORITIES, encodeSubtitleLanguagePriorities(normalized.languagePriorities))
            .putString(KEY_SUBTITLE_FORMAT_PRIORITIES, encodeSubtitleFormatPriorities(normalized.formatPriorities))
            .apply()
    }

    fun saveSystemNetworkSpeedDisplayMode(mode: SystemNetworkSpeedDisplayMode) {
        prefs.edit().putString(KEY_SYSTEM_NETWORK_SPEED_DISPLAY_MODE, mode.storageValue).apply()
    }

    fun saveSystemNetworkSpeedPosition(position: SystemNetworkSpeedPosition) {
        prefs.edit().putString(KEY_SYSTEM_NETWORK_SPEED_POSITION, position.storageValue).apply()
    }

    fun saveSystemTimeDisplayMode(mode: SystemTimeDisplayMode) {
        prefs.edit().putString(KEY_SYSTEM_TIME_DISPLAY_MODE, mode.storageValue).apply()
    }

    fun saveVideoVersionPrioritySettings(settings: VideoVersionPrioritySettings) {
        prefs.edit()
            .putString(KEY_VIDEO_PRIORITY_RULES, toJsonString(settings.normalized().rules))
            .apply()
    }

    // ---- private helpers -------------------------------------------------

    private fun encodeSubtitleFormatPriorities(priorities: Map<SubtitleFormatPriorityType, Int>): String {
        val json = JSONObject()
        for ((type, value) in priorities) {
            json.put(type.storageValue, value)
        }
        return json.toString()
    }

    private fun encodeSubtitleLanguagePriorities(priorities: Map<SubtitleLanguagePriorityType, Int>): String {
        val json = JSONObject()
        for ((type, value) in priorities) {
            json.put(type.storageValue, value)
        }
        return json.toString()
    }

    private fun isAppUpdateStartupCacheForVersion(currentVersion: String): Boolean =
        prefs.getString(KEY_APP_UPDATE_STARTUP_LAST_CHECKED_VERSION, null) == currentVersion

    private fun parseAudioPrioritySortTypes(value: String?): List<AudioPrioritySortType> {
        val split = value?.split(",")
        if (split == null) return AudioVersionPrioritySettings.DEFAULT_SORT_TYPES
        return split.mapNotNull { AudioPrioritySortType.fromStorageValue(it.trim()) }
    }

    private fun parseSubtitleFormatPriorities(value: String?): Map<SubtitleFormatPriorityType, Int> {
        if (value.isNullOrBlank()) return SubtitleVersionPrioritySettings.DEFAULT_FORMAT_PRIORITIES
        return try {
            val json = JSONObject(value)
            SubtitleFormatPriorityType.entries.associateWith { type ->
                json.optInt(type.storageValue, type.defaultPriority)
            }
        } catch (e: Exception) {
            SubtitleVersionPrioritySettings.DEFAULT_FORMAT_PRIORITIES
        }
    }

    private fun parseSubtitleLanguagePriorities(value: String?): Map<SubtitleLanguagePriorityType, Int> {
        if (value.isNullOrBlank()) return SubtitleVersionPrioritySettings.DEFAULT_LANGUAGE_PRIORITIES
        return try {
            val json = JSONObject(value)
            SubtitleLanguagePriorityType.entries.associateWith { type ->
                json.optInt(type.storageValue, type.defaultPriority)
            }
        } catch (e: Exception) {
            SubtitleVersionPrioritySettings.DEFAULT_LANGUAGE_PRIORITIES
        }
    }

    private fun parseSubtitlePrioritySortTypes(value: String?): List<SubtitlePrioritySortType> {
        val split = value?.split(",")
        if (split == null) return SubtitleVersionPrioritySettings.DEFAULT_SORT_TYPES
        return split.mapNotNull { SubtitlePrioritySortType.fromStorageValue(it.trim()) }
    }

    private fun parseVideoPriorityRules(json: String?): List<VideoPriorityRule> {
        if (json.isNullOrBlank()) return listOf(VideoPriorityRule())
        return try {
            val array = JSONArray(json)
            val rules = mutableListOf<VideoPriorityRule>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val sortType = runCatching { VideoPrioritySortType.valueOf(obj.optString("sortType")) }
                    .getOrNull() ?: VideoPrioritySortType.NONE
                val qualityPriorities = obj.optJSONObject("qualityPriorities")?.let { qp ->
                    VideoQualityStandard.entries
                        .filter { qp.has(it.name) }
                        .associateWith { qp.optInt(it.name, 1) }
                }
                val valueSortDirection = runCatching {
                    VideoValueSortDirection.valueOf(obj.optString("valueSortDirection"))
                }.getOrNull()
                rules.add(VideoPriorityRule(sortType, qualityPriorities, valueSortDirection))
            }
            if (rules.isEmpty()) listOf(VideoPriorityRule()) else rules
        } catch (e: Exception) {
            listOf(VideoPriorityRule())
        }
    }

    private fun toJsonString(rules: List<VideoPriorityRule>): String {
        val array = JSONArray()
        for (rule in rules) {
            val obj = JSONObject()
            obj.put("sortType", rule.sortType.name)
            rule.qualityPriorities?.let { priorities ->
                val qualityJson = JSONObject()
                for ((standard, priority) in priorities) {
                    qualityJson.put(standard.name, priority)
                }
                obj.put("qualityPriorities", qualityJson)
            }
            rule.valueSortDirection?.let { direction ->
                obj.put("valueSortDirection", direction.name)
            }
            array.put(obj)
        }
        return array.toString()
    }

    companion object {
        const val AUDIO_DELAY_MAX_MS = 10000
        const val AUDIO_DELAY_MIN_MS = -10000
        const val AUDIO_DELAY_STEP_MS = 100

        /** Rounds to the nearest [AUDIO_DELAY_STEP_MS] multiple and clamps into range. */
        fun normalizeAudioDelayMs(delayMs: Int): Int =
            (Math.round(delayMs / AUDIO_DELAY_STEP_MS.toFloat()) * AUDIO_DELAY_STEP_MS)
                .coerceIn(AUDIO_DELAY_MIN_MS, AUDIO_DELAY_MAX_MS)

        private const val PREFS_NAME = "emby_prefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ADMIN_DEBUG_FEATURES_UNLOCKED = "admin_debug_features_unlocked"
        private const val KEY_AGGREGATE_RESULT_SORT_MODE = "aggregate_result_sort_mode"
        private const val KEY_APP_UPDATE_STARTUP_LAST_CHECKED_VERSION = "app_update_startup_last_checked_version"
        private const val KEY_APP_UPDATE_STARTUP_LAST_CHECK_TIME_MS = "app_update_startup_last_check_time_ms"
        private const val KEY_APP_UPDATE_STARTUP_LAST_HAS_UPDATE = "app_update_startup_last_has_update"
        private const val KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_ACTIVE = "ass_subtitle_enhancement_guard_active"
        private const val KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_MEDIA_ID = "ass_subtitle_enhancement_guard_media_id"
        private const val KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_SUBTITLE_LABEL = "ass_subtitle_enhancement_guard_subtitle_label"
        private const val KEY_ASS_SUBTITLE_ENHANCEMENT_GUARD_TIME_MS = "ass_subtitle_enhancement_guard_time_ms"
        private const val KEY_AUDIO_DELAY_MS = "audio_delay_ms"
        private const val KEY_AUDIO_PREFERRED_LANGUAGE = "audio_preferred_language"
        private const val KEY_AUDIO_PRIORITY_AAC = "audio_priority_aac"
        private const val KEY_AUDIO_PRIORITY_AC3 = "audio_priority_ac3"
        private const val KEY_AUDIO_PRIORITY_DTS = "audio_priority_dts"
        private const val KEY_AUDIO_PRIORITY_EAC3 = "audio_priority_eac3"
        private const val KEY_AUDIO_PRIORITY_FLAC = "audio_priority_flac"
        private const val KEY_AUDIO_PRIORITY_SORT_TYPES = "audio_priority_sort_types"
        private const val KEY_AUDIO_PRIORITY_TRUEHD = "audio_priority_truehd"
        private const val KEY_DISABLE_TOP_NAV_FOCUS_REFRESH = "disable_top_nav_focus_refresh"
        private const val KEY_ENABLE_AGGREGATE_SEARCH = "enable_aggregate_search"
        private const val KEY_ENABLE_AGGREGATE_SEARCH_IN_SERVER_SEARCH = "enable_aggregate_search_in_server_search"
        private const val KEY_ENABLE_AGGREGATE_SEARCH_VERSION_TAGS = "enable_aggregate_search_version_tags"
        private const val KEY_ENABLE_APP_UPDATE_CHECK_ON_STARTUP = "enable_app_update_check_on_startup"
        private const val KEY_ENABLE_ASS_SUBTITLE_ENHANCEMENT = "enable_ass_subtitle_enhancement"
        private const val KEY_ENABLE_CLICKABLE_LIBRARY_SECTION_TITLE = "enable_clickable_library_section_title"
        private const val KEY_ENABLE_DETAIL_AGGREGATE_VERSION_RAIL = "enable_detail_aggregate_version_rail"
        private const val KEY_ENABLE_HOME_BACK_FOCUS_SERVER_BUTTON = "enable_home_back_focus_server_button"
        private const val KEY_ENABLE_HOME_UNPLAYED_ONLY = "enable_home_unplayed_only"
        private const val KEY_ENABLE_LIBRARY_ROW_MORE_BUTTON = "enable_library_row_more_button"
        private const val KEY_ENABLE_PLAYBACK_STARTUP_POSTER = "enable_playback_startup_poster"
        private const val KEY_ENABLE_SPECIAL_FEATURES = "enable_special_features"
        private const val KEY_ENABLE_TRANSPARENT_TOP_NAV_BUTTONS = "enable_transparent_top_nav_buttons"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_USED_AGGREGATE_HUB = "last_used_aggregate_hub"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_PLAYER_DEFAULT_VIEW_MODE = "player_default_view_mode"
        private const val KEY_PLAYER_FRAME_RATE_MATCHING_MODE = "player_frame_rate_matching_mode"
        private const val KEY_PLAYER_RESIZE_MODE = "player_resize_mode"
        private const val KEY_QR_NETWORK_INTERFACE_NAME = "qr_network_interface_name"
        private const val KEY_SEEK_INTERVAL_SEC = "seek_interval_sec"
        private const val KEY_SERVER_ICON_LIBRARY_URL = "server_icon_library_url"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SUBTITLE_FORMAT_PRIORITIES = "subtitle_format_priorities"
        private const val KEY_SUBTITLE_LANGUAGE_PRIORITIES = "subtitle_language_priorities"
        private const val KEY_SUBTITLE_PRIORITY_SORT_TYPES = "subtitle_priority_sort_types"
        private const val KEY_SYSTEM_NETWORK_SPEED_DISPLAY_MODE = "system_network_speed_display_mode"
        private const val KEY_SYSTEM_NETWORK_SPEED_POSITION = "system_network_speed_position"
        private const val KEY_SYSTEM_TIME_DISPLAY_MODE = "system_time_display_mode"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_VIDEO_PRIORITY_RULES = "video_priority_rules"
    }
}
