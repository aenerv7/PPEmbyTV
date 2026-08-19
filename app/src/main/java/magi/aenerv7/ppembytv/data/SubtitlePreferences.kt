package magi.aenerv7.ppembytv.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Per-item subtitle selections and display settings, persisted as Gson JSON
 * arrays in the "subtitle_preferences" file. Movies are keyed by
 * "itemId|mediaSourceId", series by seriesId; each list is capped at 100 entries.
 */
class SubtitlePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun clearMovieSubtitleSelection(itemId: String, mediaSourceId: String) {
        val pref = getMovieSubtitlePref(itemId, mediaSourceId) ?: return
        val list = getMoviePrefs().toMutableList()
        list.removeAll { it.itemId + "|" + it.mediaSourceId == itemId + "|" + mediaSourceId }
        list.add(
            pref.copy(
                currentApiStreamIndex = null,
                currentTrackTitle = null,
                memoryEnabled = false,
                timestamp = System.currentTimeMillis(),
            ),
        )
        val capped = if (list.size > MAX_MOVIE_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_MOVIE_CACHE)
        } else {
            list
        }
        saveMoviePrefs(capped)
        Log.d(TAG, "清除电影字幕选择记忆: itemId=" + itemId + ", mediaSourceId=" + mediaSourceId)
    }

    fun clearSeriesSubtitleSelection(seriesId: String) {
        val pref = getSeriesSubtitlePref(seriesId) ?: return
        val list = getSeriesPrefs().toMutableList()
        list.removeAll { it.seriesId == seriesId }
        list.add(pref.copy(memoryEnabled = false, timestamp = System.currentTimeMillis()))
        val capped = if (list.size > MAX_SERIES_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_SERIES_CACHE)
        } else {
            list
        }
        saveSeriesPrefs(capped)
        Log.d(TAG, "清除剧集字幕选择记忆: seriesId=" + seriesId)
    }

    fun getBitmapSubtitleBrightness(): Int =
        normalizeBitmapSubtitleBrightness(prefs.getInt(KEY_BITMAP_SUBTITLE_BRIGHTNESS, BITMAP_SUBTITLE_BRIGHTNESS_DEFAULT))

    /** 全局字幕字号倍率（相对 Media3 默认字号）。 */
    fun getSubtitleFontScale(): Float =
        prefs.getFloat(KEY_SUBTITLE_FONT_SCALE, 1.0f).coerceIn(SUBTITLE_FONT_SCALE_MIN, SUBTITLE_FONT_SCALE_MAX)

    /** 全局字幕颜色（ASS 增强渲染；未带颜色标签的文字使用该颜色）。 */
    fun getSubtitleFontColor(): SubtitleColor =
        SubtitleColor.fromOrdinal(prefs.getInt(KEY_SUBTITLE_FONT_COLOR, SubtitleColor.WHITE.ordinal))

    fun getMovieDisplaySettings(itemId: String, mediaSourceId: String, trackIndex: Int): SubtitleDisplaySettings {
        val pref = getMovieSubtitlePref(itemId, mediaSourceId) ?: return SubtitleDisplaySettings()
        val settings = pref.trackSettings[trackIndex]
        Log.d(
            TAG,
            "获取电影字幕设置: trackIndex=" + trackIndex + ", 已存储的indexes=" + pref.trackSettings.keys + ", 找到=" + (settings != null),
        )
        return settings ?: SubtitleDisplaySettings()
    }

    fun getMovieSubtitlePref(itemId: String, mediaSourceId: String): MovieSubtitlePref? =
        getMoviePrefs().firstOrNull { it.itemId == itemId && it.mediaSourceId == mediaSourceId }

    fun getSeriesDisplaySettings(seriesId: String, languageType: String, subtitleTitle: String?): SubtitleDisplaySettings {
        val pref = getSeriesSubtitlePref(seriesId) ?: return SubtitleDisplaySettings()
        val trackKey = makeTrackKey(languageType, subtitleTitle)
        val settings = pref.trackSettings[trackKey]
        Log.d(
            TAG,
            "获取剧集字幕设置: trackKey=" + trackKey + ", 已存储的keys=" + pref.trackSettings.keys + ", 找到=" + (settings != null),
        )
        return settings ?: SubtitleDisplaySettings()
    }

    fun getSeriesSubtitlePref(seriesId: String): SeriesSubtitlePref? =
        getSeriesPrefs().firstOrNull { it.seriesId == seriesId }

    fun isBitmapSubtitleBrightnessEnabled(): Boolean =
        prefs.getBoolean(KEY_BITMAP_SUBTITLE_BRIGHTNESS_ENABLED, true)

    fun isSubtitlesEnabled(): Boolean = prefs.getBoolean(KEY_SUBTITLES_ENABLED, true)

    fun saveBitmapSubtitleBrightness(value: Int) {
        val brightness = normalizeBitmapSubtitleBrightness(value)
        prefs.edit().putInt(KEY_BITMAP_SUBTITLE_BRIGHTNESS, brightness).apply()
        Log.d(TAG, "保存全局图形字幕亮度: brightness=" + brightness)
    }

    fun saveSubtitleFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_SUBTITLE_FONT_SCALE, scale.coerceIn(SUBTITLE_FONT_SCALE_MIN, SUBTITLE_FONT_SCALE_MAX)).apply()
        Log.d(TAG, "保存全局字幕字号: scale=" + scale)
    }

    fun saveSubtitleFontColor(color: SubtitleColor) {
        prefs.edit().putInt(KEY_SUBTITLE_FONT_COLOR, color.ordinal).apply()
        Log.d(TAG, "保存全局字幕颜色: color=" + color.displayName)
    }

    fun saveBitmapSubtitleBrightnessEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BITMAP_SUBTITLE_BRIGHTNESS_ENABLED, enabled).apply()
        Log.d(TAG, "保存全局图形字幕亮度增强开关: enabled=" + enabled)
    }

    fun saveMovieDisplaySettings(itemId: String, mediaSourceId: String, trackIndex: Int, settings: SubtitleDisplaySettings) {
        val pref = getMovieSubtitlePref(itemId, mediaSourceId)
        val trackSettings = (pref?.trackSettings ?: emptyMap()).toMutableMap()
        trackSettings[trackIndex] = settings
        val list = getMoviePrefs().toMutableList()
        list.removeAll { it.itemId + "|" + it.mediaSourceId == itemId + "|" + mediaSourceId }
        list.add(
            MovieSubtitlePref(
                itemId = itemId,
                mediaSourceId = mediaSourceId,
                currentTrackIndex = pref?.currentTrackIndex ?: trackIndex,
                currentApiStreamIndex = pref?.currentApiStreamIndex,
                currentTrackTitle = pref?.currentTrackTitle,
                trackSettings = trackSettings,
                memoryEnabled = pref?.memoryEnabled,
            ),
        )
        val capped = if (list.size > MAX_MOVIE_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_MOVIE_CACHE)
        } else {
            list
        }
        saveMoviePrefs(capped)
        Log.d(TAG, "保存电影字幕显示设置: itemId=" + itemId + ", trackIndex=" + trackIndex + ", settings=" + settings)
    }

    fun saveMovieSubtitleSelection(
        itemId: String,
        mediaSourceId: String,
        trackIndex: Int,
        apiStreamIndex: Int? = null,
        trackTitle: String? = null,
    ) {
        val pref = getMovieSubtitlePref(itemId, mediaSourceId)
        val trackSettings = pref?.trackSettings ?: emptyMap()
        val list = getMoviePrefs().toMutableList()
        list.removeAll { it.itemId + "|" + it.mediaSourceId == itemId + "|" + mediaSourceId }
        list.add(
            MovieSubtitlePref(
                itemId = itemId,
                mediaSourceId = mediaSourceId,
                currentTrackIndex = trackIndex,
                currentApiStreamIndex = apiStreamIndex,
                currentTrackTitle = trackTitle,
                trackSettings = trackSettings,
                memoryEnabled = true,
            ),
        )
        val capped = if (list.size > MAX_MOVIE_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_MOVIE_CACHE)
        } else {
            list
        }
        saveMoviePrefs(capped)
        Log.d(
            TAG,
            "保存电影字幕选择: itemId=" + itemId + ", trackIndex=" + trackIndex + ", apiStreamIndex=" + apiStreamIndex + ", title=" + trackTitle,
        )
    }

    fun saveSeriesDisplaySettings(seriesId: String, languageType: String, subtitleTitle: String?, settings: SubtitleDisplaySettings) {
        val pref = getSeriesSubtitlePref(seriesId)
        var currentLanguageType = languageType
        var currentSubtitleTitle = subtitleTitle
        val trackKey = makeTrackKey(currentLanguageType, currentSubtitleTitle)
        val trackSettings = (pref?.trackSettings ?: emptyMap()).toMutableMap()
        trackSettings[trackKey] = settings
        val list = getSeriesPrefs().toMutableList()
        list.removeAll { it.seriesId == seriesId }
        pref?.currentLanguageType?.let { currentLanguageType = it }
        pref?.currentSubtitleTitle?.let { currentSubtitleTitle = it }
        list.add(
            SeriesSubtitlePref(
                seriesId = seriesId,
                currentLanguageType = currentLanguageType,
                currentSubtitleTitle = currentSubtitleTitle,
                trackSettings = trackSettings,
                memoryEnabled = pref?.memoryEnabled,
            ),
        )
        val capped = if (list.size > MAX_SERIES_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_SERIES_CACHE)
        } else {
            list
        }
        saveSeriesPrefs(capped)
        Log.d(TAG, "保存剧集字幕显示设置: seriesId=" + seriesId + ", trackKey=" + trackKey + ", settings=" + settings)
    }

    fun saveSeriesSubtitleSelection(seriesId: String, languageType: String, subtitleTitle: String? = null) {
        val pref = getSeriesSubtitlePref(seriesId)
        val trackSettings = pref?.trackSettings ?: emptyMap()
        val list = getSeriesPrefs().toMutableList()
        list.removeAll { it.seriesId == seriesId }
        list.add(
            SeriesSubtitlePref(
                seriesId = seriesId,
                currentLanguageType = languageType,
                currentSubtitleTitle = subtitleTitle,
                trackSettings = trackSettings,
                memoryEnabled = true,
            ),
        )
        val capped = if (list.size > MAX_SERIES_CACHE) {
            list.sortedBy { it.timestamp }.takeLast(MAX_SERIES_CACHE)
        } else {
            list
        }
        saveSeriesPrefs(capped)
        Log.d(TAG, "保存剧集字幕选择: seriesId=" + seriesId + ", language=" + languageType + ", title=" + subtitleTitle)
    }

    fun saveSubtitlesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUBTITLES_ENABLED, enabled).apply()
        Log.d(TAG, "保存全局字幕开关: enabled=" + enabled)
    }

    // ---- private helpers -------------------------------------------------

    private fun getMoviePrefs(): List<MovieSubtitlePref> {
        val json = prefs.getString(KEY_MOVIE_PREFS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<MovieSubtitlePref>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析电影偏好失败", e)
            emptyList()
        }
    }

    private fun getSeriesPrefs(): List<SeriesSubtitlePref> {
        val json = prefs.getString(KEY_SERIES_PREFS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SeriesSubtitlePref>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析剧集偏好失败", e)
            emptyList()
        }
    }

    private fun makeTrackKey(languageType: String, subtitleTitle: String?): String =
        languageType + "|" + (subtitleTitle ?: "")

    private fun saveMoviePrefs(list: List<MovieSubtitlePref>) {
        prefs.edit().putString(KEY_MOVIE_PREFS, gson.toJson(list)).apply()
    }

    private fun saveSeriesPrefs(list: List<SeriesSubtitlePref>) {
        prefs.edit().putString(KEY_SERIES_PREFS, gson.toJson(list)).apply()
    }

    /** Subtitle colors as (argb color, display name). */
    enum class SubtitleColor(val colorValue: Int, val displayName: String) {
        WHITE(0xFFFFFFFF.toInt(), "白色"),
        YELLOW(0xFFFFFF00.toInt(), "黄色"),
        CYAN(0xFF00FFFF.toInt(), "青色"),
        GREEN(0xFF00FF00.toInt(), "绿色"),
        MAGENTA(0xFFFF00FF.toInt(), "洋红"),
        RED(0xFFFF0000.toInt(), "红色"),
        BLUE(0xFF0000FF.toInt(), "蓝色"),
        ORANGE(0xFFFFA500.toInt(), "橙色");

        companion object {
            fun fromOrdinal(ordinal: Int): SubtitleColor = entries.getOrNull(ordinal) ?: WHITE
        }
    }

    data class SubtitleDisplaySettings(
        val verticalOffset: Float = 0f,
        val scale: Float = 1f,
        val timeOffsetMs: Long = 0L,
        val colorOrdinal: Int = 0,
    ) {
        val color: SubtitleColor
            get() = SubtitleColor.fromOrdinal(colorOrdinal)
    }

    data class MovieSubtitlePref(
        val itemId: String,
        val mediaSourceId: String,
        val currentTrackIndex: Int,
        val currentApiStreamIndex: Int? = null,
        val currentTrackTitle: String? = null,
        val trackSettings: Map<Int, SubtitleDisplaySettings> = emptyMap(),
        val memoryEnabled: Boolean? = null,
        val timestamp: Long = System.currentTimeMillis(),
    )

    data class SeriesSubtitlePref(
        val seriesId: String,
        val currentLanguageType: String,
        val currentSubtitleTitle: String? = null,
        val trackSettings: Map<String, SubtitleDisplaySettings> = emptyMap(),
        val memoryEnabled: Boolean? = null,
        val timestamp: Long = System.currentTimeMillis(),
    )

    companion object {
        const val BITMAP_SUBTITLE_BRIGHTNESS_DEFAULT = 80
        const val BITMAP_SUBTITLE_BRIGHTNESS_MAX = 100
        const val BITMAP_SUBTITLE_BRIGHTNESS_MIN = 5
        const val BITMAP_SUBTITLE_BRIGHTNESS_STEP = 5
        const val SUBTITLE_FONT_SCALE_MIN = 0.6f
        const val SUBTITLE_FONT_SCALE_MAX = 2.0f

        /** Normalizes into 5..100, rounded down to a multiple of 5; non-positive -> default. */
        fun normalizeBitmapSubtitleBrightness(value: Int): Int {
            if (value <= 0) return BITMAP_SUBTITLE_BRIGHTNESS_DEFAULT
            val coerced = value.coerceIn(BITMAP_SUBTITLE_BRIGHTNESS_MIN, BITMAP_SUBTITLE_BRIGHTNESS_MAX)
            return (coerced - coerced % BITMAP_SUBTITLE_BRIGHTNESS_STEP)
                .coerceIn(BITMAP_SUBTITLE_BRIGHTNESS_MIN, BITMAP_SUBTITLE_BRIGHTNESS_MAX)
        }

        private const val KEY_BITMAP_SUBTITLE_BRIGHTNESS = "bitmap_subtitle_brightness"
        private const val KEY_BITMAP_SUBTITLE_BRIGHTNESS_ENABLED = "bitmap_subtitle_brightness_enabled"
        private const val KEY_MOVIE_PREFS = "movie_subtitle_prefs_v2"
        private const val KEY_SERIES_PREFS = "series_subtitle_prefs_v2"
        private const val KEY_SUBTITLES_ENABLED = "subtitles_enabled"
        private const val KEY_SUBTITLE_FONT_SCALE = "subtitle_font_scale"
        private const val KEY_SUBTITLE_FONT_COLOR = "subtitle_font_color"
        private const val MAX_MOVIE_CACHE = 100
        private const val MAX_SERIES_CACHE = 100
        private const val PREFS_NAME = "subtitle_preferences"
        private const val TAG = "SubtitlePrefs"
    }
}
