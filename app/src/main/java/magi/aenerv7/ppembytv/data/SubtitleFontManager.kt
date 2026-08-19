package magi.aenerv7.ppembytv.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 已上传字幕字体的持久化管理：字体列表 + 当前选中字体。
 *
 * 上传的字体文件由 SubtitleFontUploadServer 写入 filesDir/subtitle_fonts，
 * 这里只持久化条目（id/name/path）与选中项；新上传的字体自动生效。
 */
class SubtitleFontManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun listFonts(): List<SubtitleFontEntry> {
        val json = prefs.getString(KEY_FONTS, null) ?: return emptyList()
        val entries: List<SubtitleFontEntry> = try {
            gson.fromJson(json, object : TypeToken<List<SubtitleFontEntry>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析字体列表失败", e)
            emptyList()
        }
        val valid = entries.filter { File(it.path).exists() }
        if (valid.size != entries.size) {
            prefs.edit().putString(KEY_FONTS, gson.toJson(valid)).apply()
        }
        return valid
    }

    /** 记录新上传的字体并自动设为当前字体。 */
    fun addFont(entry: SubtitleFontEntry) {
        val list = listFonts().toMutableList()
        list.removeAll { it.id == entry.id }
        list.add(entry)
        prefs.edit().putString(KEY_FONTS, gson.toJson(list)).apply()
        prefs.edit().putString(KEY_SELECTED_FONT, entry.id).apply()
        Log.d(TAG, "已添加字幕字体: ${entry.name}")
    }

    fun getSelectedFont(): SubtitleFontEntry? {
        val selectedId = prefs.getString(KEY_SELECTED_FONT, null) ?: return null
        return listFonts().firstOrNull { it.id == selectedId }
    }

    fun clearSelectedFont() {
        prefs.edit().remove(KEY_SELECTED_FONT).apply()
        Log.d(TAG, "已清除字幕字体选择，恢复默认字体")
    }

    fun removeFont(id: String) {
        val entry = listFonts().firstOrNull { it.id == id }
        val list = listFonts().toMutableList()
        list.removeAll { it.id == id }
        prefs.edit().putString(KEY_FONTS, gson.toJson(list)).apply()
        if (prefs.getString(KEY_SELECTED_FONT, null) == id) {
            clearSelectedFont()
        }
        entry?.let { runCatching { File(it.path).delete() } }
    }

    /** 加载当前选中字体的 [Typeface]；未选择或加载失败时返回 null（播放器用默认字体）。 */
    fun selectedTypeface(): Typeface? {
        val entry = getSelectedFont() ?: return null
        return runCatching { Typeface.createFromFile(entry.path) }.getOrNull()
    }

    private companion object {
        const val PREFS_NAME = "subtitle_font_manager"
        const val KEY_FONTS = "fonts"
        const val KEY_SELECTED_FONT = "selected_font"
        const val TAG = "SubtitleFontMgr"
    }
}
