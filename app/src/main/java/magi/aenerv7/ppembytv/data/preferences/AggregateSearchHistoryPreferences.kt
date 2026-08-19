package magi.aenerv7.ppembytv.data.preferences

import android.content.Context
import android.util.Log
import org.json.JSONArray

/**
 * Stores the aggregate-search keyword history as a JSON string array (max
 * [MAX_HISTORY_COUNT] entries, deduplicated, newest first).
 */
class AggregateSearchHistoryPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(): List<String> = parseHistory(prefs.getString(KEY_HISTORY, null))

    /** Adds a keyword to the front of the history (deduplicated) and returns the new history. */
    fun addKeyword(keyword: String): List<String> {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return getHistory()
        val history = getHistory()
        val result = (listOf(trimmed) + history.filterNot { it.equals(trimmed, true) })
            .take(MAX_HISTORY_COUNT)
        saveHistory(result)
        return result
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun parseHistory(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i).trim()
                if (item.isNotBlank() && result.none { it.equals(item, true) }) {
                    result.add(item)
                }
            }
            result.take(MAX_HISTORY_COUNT)
        } catch (e: Exception) {
            Log.e(TAG, "Parse aggregate search history failed", e)
            emptyList()
        }
    }

    private fun saveHistory(history: List<String>) {
        val array = JSONArray()
        for (item in history.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_HISTORY_COUNT)) {
            array.put(item)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        const val MAX_HISTORY_COUNT = 5
        private const val KEY_HISTORY = "history"
        private const val PREFS_NAME = "aggregate_search_history"
        private const val TAG = "AggregateSearchHistory"
    }
}
